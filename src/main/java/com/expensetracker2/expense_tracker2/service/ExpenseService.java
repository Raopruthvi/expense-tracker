package com.expensetracker2.expense_tracker2.service;

import com.expensetracker2.expense_tracker2.exception.ResourceNotFoundException;
import com.expensetracker2.expense_tracker2.model.*;
import com.expensetracker2.expense_tracker2.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final PersonRepository personRepository;
    private final FinancialProfileRepository financialProfileRepository;
    private final MonthlyBudgetRepository monthlyBudgetRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          PersonRepository personRepository,
                          FinancialProfileRepository financialProfileRepository,
                          MonthlyBudgetRepository monthlyBudgetRepository) {
        this.expenseRepository = expenseRepository;
        this.personRepository = personRepository;
        this.financialProfileRepository = financialProfileRepository;
        this.monthlyBudgetRepository = monthlyBudgetRepository;
    }

    // ─── PERSON METHODS ───────────────────────────────────────

    public Person savePerson(Person person) {
        return personRepository.save(person);
    }

    public List<Person> getAllPeople() {
        return personRepository.findAll();
    }

    public Person getPersonById(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Person not found with id: " + id));
    }

    public void deletePerson(Long id) {
        personRepository.deleteById(id);
    }

    // ─── EXPENSE METHODS ──────────────────────────────────────

    public Expense saveExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    public Expense getExpenseById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Expense not found with id: " + id));
    }

    public void deleteExpense(Long id) {
        Expense expense = getExpenseById(id);

        // Refund regardless of settled status
        // Only refund allowance if expense was not settled
        // (settled expenses already had their allowance refunded at settle time)
        if (!expense.isSettled()) {
            refundAllowance(expense);
        }

        // Always refund account/cash balance on delete
        // If settled: the money was already added back at settle time
        // so don't double-refund account balance
        if (!expense.isSettled()) {
            refundBalance(expense);
        }

        expenseRepository.deleteById(id);
    }
    // ─── BUSINESS LOGIC ───────────────────────────────────────

    public List<Expense> getUnsettledExpenses() {
        return expenseRepository.findAll()
                .stream()
                .filter(e -> !e.isSettled())
                .toList();
    }

    public List<Expense> getOverdueExpenses() {
        return expenseRepository.findAll()
                .stream()
                .filter(e -> !e.isSettled()
                        && e.getDeadline() != null
                        && e.getDeadline().isBefore(LocalDate.now()))
                .toList();
    }

    private static final String OWNER_NAME = "Mani";

    public Expense settleExpense(Long id) {
        Expense expense = getExpenseById(id);
        expense.setSettled(true);

        try {
            FinancialProfile profile = financialProfileRepository.findAll()
                    .stream().findFirst().orElse(null);
            int month = LocalDate.now().getMonthValue();
            int year = LocalDate.now().getYear();
            MonthlyBudget budget = monthlyBudgetRepository
                    .findByMonthAndYearAndClosedFalse(month, year)
                    .orElse(null);

            if (profile == null || budget == null) {
                return expenseRepository.save(expense);
            }

            BigDecimal amount = expense.getAmount();
            Category category = expense.getCategory();
            boolean isCash = expense.isCash();

            String paidByName = expense.getPaidByName() != null ?
                    expense.getPaidByName() :
                    (expense.getPaidBy() != null ? expense.getPaidBy().getName() : "");
            String owedByName = expense.getOwedByName() != null ?
                    expense.getOwedByName() :
                    (expense.getOwedBy() != null ? expense.getOwedBy().getName() : "");

            boolean maniPaid = paidByName.equalsIgnoreCase(OWNER_NAME) || paidByName.isEmpty();
            boolean maniOwes = owedByName.equalsIgnoreCase(OWNER_NAME);

            if (maniPaid && !maniOwes) {
                // Someone paid Mani back → add to balance and allowance
                if (isCash) {
                    profile.setCashBalance(profile.getCashBalance().add(amount));
                } else {
                    profile.setAccountBalance(profile.getAccountBalance().add(amount));
                }
                refundAllowance(expense, profile, budget);
            } else if (maniOwes) {
                // Mani pays someone back → deduct from balance and allowance
                if (isCash) {
                    profile.setCashBalance(profile.getCashBalance().subtract(amount));
                } else {
                    profile.setAccountBalance(profile.getAccountBalance().subtract(amount));
                }
                deductAllowance(expense, profile, budget);
            }

            financialProfileRepository.save(profile);
            monthlyBudgetRepository.save(budget);

        } catch (Exception e) {
            // Skip financial updates on error
        }

        return expenseRepository.save(expense);
    }

    private void deductAllowance(Expense expense, FinancialProfile profile,
                                  MonthlyBudget budget) {
        BigDecimal amount = expense.getAmount();
        Category category = expense.getCategory();

        if (category == Category.VEHICLE) {
            BigDecimal remaining = budget.getRemainingVehicleAllowance();
            if (remaining.compareTo(amount) >= 0) {
                budget.setRemainingVehicleAllowance(remaining.subtract(amount));
            } else {
                BigDecimal overflow = amount.subtract(remaining);
                budget.setRemainingVehicleAllowance(BigDecimal.ZERO);
                profile.setRemainingSalary(profile.getRemainingSalary().subtract(overflow));
            }
        } else if (category == Category.FOOD ||
                   category == Category.SPORTS ||
                   category == Category.MISCELLANEOUS ||
                   category == Category.ONLINE_SHOPPING ||
                   category == Category.SUBSCRIPTIONS ||
                   category == Category.TRIP) {
            BigDecimal remaining = budget.getRemainingAllowance();
            if (remaining.compareTo(amount) >= 0) {
                budget.setRemainingAllowance(remaining.subtract(amount));
            } else {
                BigDecimal overflow = amount.subtract(remaining);
                budget.setRemainingAllowance(BigDecimal.ZERO);
                profile.setRemainingSalary(profile.getRemainingSalary().subtract(overflow));
            }
        } else {
            profile.setRemainingSalary(profile.getRemainingSalary().subtract(amount));
        }
    }

    private void refundAllowance(Expense expense, FinancialProfile profile,
                                  MonthlyBudget budget) {
        BigDecimal amount = expense.getAmount();
        Category category = expense.getCategory();

        if (category == Category.VEHICLE) {
            BigDecimal newVehicle = budget.getRemainingVehicleAllowance().add(amount);
            if (newVehicle.compareTo(budget.getVehicleAllowance()) > 0) {
                BigDecimal overflow = newVehicle.subtract(budget.getVehicleAllowance());
                budget.setRemainingVehicleAllowance(budget.getVehicleAllowance());
                profile.setRemainingSalary(profile.getRemainingSalary().add(overflow));
            } else {
                budget.setRemainingVehicleAllowance(newVehicle);
            }
        } else if (category == Category.FOOD ||
                   category == Category.SPORTS ||
                   category == Category.MISCELLANEOUS ||
                   category == Category.ONLINE_SHOPPING ||
                   category == Category.SUBSCRIPTIONS ||
                   category == Category.TRIP) {
            BigDecimal newAllowance = budget.getRemainingAllowance().add(amount);
            if (newAllowance.compareTo(budget.getTotalAllowance()) > 0) {
                BigDecimal overflow = newAllowance.subtract(budget.getTotalAllowance());
                budget.setRemainingAllowance(budget.getTotalAllowance());
                profile.setRemainingSalary(profile.getRemainingSalary().add(overflow));
            } else {
                budget.setRemainingAllowance(newAllowance);
            }
        } else {
            profile.setRemainingSalary(profile.getRemainingSalary().add(amount));
        }
    }

    public BigDecimal getTotalOwedBy(Long personId) {
        Person person = getPersonById(personId);
        return expenseRepository.findAll()
                .stream()
                .filter(e -> !e.isSettled())
                .filter(e -> e.getOwedBy() != null &&
                             e.getOwedBy().getId().equals(person.getId()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalOwedTo(Long personId) {
        Person person = getPersonById(personId);
        return expenseRepository.findAll()
                .stream()
                .filter(e -> !e.isSettled())
                .filter(e -> e.getPaidBy() != null &&
                             e.getPaidBy().getId().equals(person.getId()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getNetBalance(Long personId) {
        return getTotalOwedTo(personId).subtract(getTotalOwedBy(personId));
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────

    private void refundAllowance(Expense expense) {
        try {
            FinancialProfile profile = financialProfileRepository.findAll()
                    .stream().findFirst().orElse(null);
            int month = LocalDate.now().getMonthValue();
            int year = LocalDate.now().getYear();
            MonthlyBudget budget = monthlyBudgetRepository
                    .findByMonthAndYearAndClosedFalse(month, year)
                    .orElse(null);

            if (profile == null || budget == null) return;

            BigDecimal amount = expense.getAmount();
            Category category = expense.getCategory();

            if (category == Category.VEHICLE) {
                BigDecimal newVehicle = budget.getRemainingVehicleAllowance().add(amount);
                if (newVehicle.compareTo(budget.getVehicleAllowance()) > 0) {
                    BigDecimal overflow = newVehicle.subtract(budget.getVehicleAllowance());
                    budget.setRemainingVehicleAllowance(budget.getVehicleAllowance());
                    profile.setRemainingSalary(profile.getRemainingSalary().add(overflow));
                } else {
                    budget.setRemainingVehicleAllowance(newVehicle);
                }
            } else if (category == Category.FOOD ||
                       category == Category.SPORTS ||
                       category == Category.MISCELLANEOUS ||
                       category == Category.ONLINE_SHOPPING ||
                       category == Category.SUBSCRIPTIONS ||
                       category == Category.TRIP) {
                BigDecimal newAllowance = budget.getRemainingAllowance().add(amount);
                if (newAllowance.compareTo(budget.getTotalAllowance()) > 0) {
                    BigDecimal overflow = newAllowance.subtract(budget.getTotalAllowance());
                    budget.setRemainingAllowance(budget.getTotalAllowance());
                    profile.setRemainingSalary(profile.getRemainingSalary().add(overflow));
                } else {
                    budget.setRemainingAllowance(newAllowance);
                }
            } else {
                profile.setRemainingSalary(profile.getRemainingSalary().add(amount));
            }

            financialProfileRepository.save(profile);
            monthlyBudgetRepository.save(budget);

        } catch (Exception e) {
            // Skip refund if no profile/budget
        }
    }

    private void refundBalance(Expense expense) {
        try {
            FinancialProfile profile = financialProfileRepository.findAll()
                    .stream().findFirst().orElse(null);
            if (profile == null) return;

            // Only refund if Mani was the one who paid
            String paidByName = expense.getPaidByName() != null ?
                    expense.getPaidByName() :
                    (expense.getPaidBy() != null ? expense.getPaidBy().getName() : "");
            String owedByName = expense.getOwedByName() != null ?
                    expense.getOwedByName() :
                    (expense.getOwedBy() != null ? expense.getOwedBy().getName() : "");

            boolean maniPaid = paidByName.isEmpty() ||
                               paidByName.equalsIgnoreCase("Mani");
            boolean maniOwes = owedByName.equalsIgnoreCase("Mani");

            if (maniPaid && !maniOwes) {
                if (expense.isCash()) {
                    profile.setCashBalance(
                        profile.getCashBalance().add(expense.getAmount()));
                } else {
                    profile.setAccountBalance(
                        profile.getAccountBalance().add(expense.getAmount()));
                }
                financialProfileRepository.save(profile);
            }
        } catch (Exception e) {
            // Skip
        }
    }
}