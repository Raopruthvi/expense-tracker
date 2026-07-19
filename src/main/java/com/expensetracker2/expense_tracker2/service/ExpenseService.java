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

    private static final String OWNER = "Mani";

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

    public Person findOrCreatePerson(String name) {
        return personRepository.findAll()
                .stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> personRepository.save(new Person(name)));
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
        // Auto-create people from names
        if (expense.getPaidByName() != null && !expense.getPaidByName().isBlank()) {
            Person p = findOrCreatePerson(expense.getPaidByName());
            expense.setPaidBy(p);
        }
        if (expense.getOwedByName() != null && !expense.getOwedByName().isBlank()) {
            Person p = findOrCreatePerson(expense.getOwedByName());
            expense.setOwedBy(p);
        }
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
        reverseExpenseImpact(expense);
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

    public Expense settleExpense(Long id) {
        Expense expense = getExpenseById(id);
        expense.setSettled(true);

        String owedByName = getPersonName(expense.getOwedByName(), expense.getOwedBy());
        String paidByName = getPersonName(expense.getPaidByName(), expense.getPaidBy());

        boolean maniOwes = owedByName.equalsIgnoreCase(OWNER);
        boolean maniIsPaidBy = paidByName.isEmpty() ||
                               paidByName.equalsIgnoreCase(OWNER);

        try {
            FinancialProfile profile = getProfile();
            MonthlyBudget budget = getCurrentBudget();

            if (maniOwes) {
                // Mani pays someone back → deduct balance + allowance
                deductBalance(profile, expense);
                deductAllowance(profile, budget, expense.getCategory(),
                                expense.getAmount());
            } else if (maniIsPaidBy) {
                // Someone pays Mani back → add balance + allowance
                addBalance(profile, expense);
                refundAllowance(profile, budget, expense.getCategory(),
                                expense.getAmount());
            }

            financialProfileRepository.save(profile);
            monthlyBudgetRepository.save(budget);
        } catch (Exception e) {
            // Skip if no profile/budget
        }

        return expenseRepository.save(expense);
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

    // ─── RESET ────────────────────────────────────────────────

    public void resetAll() {
        expenseRepository.deleteAll();
        personRepository.deleteAll();
        financialProfileRepository.deleteAll();
        monthlyBudgetRepository.deleteAll();
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────

    private void reverseExpenseImpact(Expense expense) {
        try {
            FinancialProfile profile = getProfile();
            MonthlyBudget budget = getCurrentBudget();

            String owedByName = getPersonName(expense.getOwedByName(),
                                              expense.getOwedBy());
            String paidByName = getPersonName(expense.getPaidByName(),
                                              expense.getPaidBy());

            boolean maniOwes = owedByName.equalsIgnoreCase(OWNER);
            boolean maniIsPaidBy = paidByName.isEmpty() ||
                                   paidByName.equalsIgnoreCase(OWNER);

            if (expense.isSettled()) {
                // Settled expense: reverse the settle action
                if (maniOwes) {
                    // Mani had paid back → refund his balance + allowance
                    addBalance(profile, expense);
                    refundAllowance(profile, budget, expense.getCategory(),
                                    expense.getAmount());
                } else if (maniIsPaidBy) {
                    // Someone had paid Mani → deduct back
                    deductBalance(profile, expense);
                    deductAllowance(profile, budget, expense.getCategory(),
                                    expense.getAmount());
                }
            } else {
                // Unsettled expense: reverse the original add action
                if (maniIsPaidBy && !maniOwes) {
                    // Mani had paid → refund his balance + allowance
                    addBalance(profile, expense);
                    refundAllowance(profile, budget, expense.getCategory(),
                                    expense.getAmount());
                }
                // If maniOwes and unsettled → nothing was deducted yet, nothing to reverse
            }

            financialProfileRepository.save(profile);
            monthlyBudgetRepository.save(budget);

        } catch (Exception e) {
            // Skip if no profile/budget
        }
    }

    private void deductBalance(FinancialProfile profile, Expense expense) {
        if (expense.isCash()) {
            profile.setCashBalance(
                profile.getCashBalance().subtract(expense.getAmount()));
            profile.setAccountBalance(
                profile.getAccountBalance().subtract(expense.getAmount()));
        } else {
            profile.setAccountBalance(
                profile.getAccountBalance().subtract(expense.getAmount()));
        }
    }

    private void addBalance(FinancialProfile profile, Expense expense) {
        if (expense.isCash()) {
            profile.setCashBalance(
                profile.getCashBalance().add(expense.getAmount()));
            profile.setAccountBalance(
                profile.getAccountBalance().add(expense.getAmount()));
        } else {
            profile.setAccountBalance(
                profile.getAccountBalance().add(expense.getAmount()));
        }
    }

    private void deductAllowance(FinancialProfile profile, MonthlyBudget budget,
                                  Category category, BigDecimal amount) {
        if (category == Category.VEHICLE) {
            BigDecimal remaining = budget.getRemainingVehicleAllowance();
            if (remaining.compareTo(amount) >= 0) {
                budget.setRemainingVehicleAllowance(remaining.subtract(amount));
            } else {
                BigDecimal overflow = amount.subtract(remaining);
                budget.setRemainingVehicleAllowance(BigDecimal.ZERO);
                profile.setRemainingSalary(
                    profile.getRemainingSalary().subtract(overflow));
            }
        } else if (isGeneralAllowanceCategory(category)) {
            BigDecimal remaining = budget.getRemainingAllowance();
            if (remaining.compareTo(amount) >= 0) {
                budget.setRemainingAllowance(remaining.subtract(amount));
            } else {
                BigDecimal overflow = amount.subtract(remaining);
                budget.setRemainingAllowance(BigDecimal.ZERO);
                profile.setRemainingSalary(
                    profile.getRemainingSalary().subtract(overflow));
            }
        } else {
            profile.setRemainingSalary(
                profile.getRemainingSalary().subtract(amount));
        }
    }

    private void refundAllowance(FinancialProfile profile, MonthlyBudget budget,
                                  Category category, BigDecimal amount) {
        if (category == Category.VEHICLE) {
            BigDecimal newVehicle = budget.getRemainingVehicleAllowance().add(amount);
            if (newVehicle.compareTo(budget.getVehicleAllowance()) > 0) {
                BigDecimal overflow = newVehicle.subtract(budget.getVehicleAllowance());
                budget.setRemainingVehicleAllowance(budget.getVehicleAllowance());
                profile.setRemainingSalary(
                    profile.getRemainingSalary().add(overflow));
            } else {
                budget.setRemainingVehicleAllowance(newVehicle);
            }
        } else if (isGeneralAllowanceCategory(category)) {
            BigDecimal newAllowance = budget.getRemainingAllowance().add(amount);
            if (newAllowance.compareTo(budget.getTotalAllowance()) > 0) {
                BigDecimal overflow = newAllowance.subtract(budget.getTotalAllowance());
                budget.setRemainingAllowance(budget.getTotalAllowance());
                profile.setRemainingSalary(
                    profile.getRemainingSalary().add(overflow));
            } else {
                budget.setRemainingAllowance(newAllowance);
            }
        } else {
            profile.setRemainingSalary(
                profile.getRemainingSalary().add(amount));
        }
    }

    private boolean isGeneralAllowanceCategory(Category category) {
        return category == Category.FOOD ||
               category == Category.SPORTS ||
               category == Category.MISCELLANEOUS ||
               category == Category.ONLINE_SHOPPING ||
               category == Category.SUBSCRIPTIONS ||
               category == Category.TRIP;
    }

    private String getPersonName(String name, Person person) {
        if (name != null && !name.isBlank()) return name;
        if (person != null) return person.getName();
        return "";
    }

    private FinancialProfile getProfile() {
        return financialProfileRepository.findAll()
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No financial profile found"));
    }

    private MonthlyBudget getCurrentBudget() {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        return monthlyBudgetRepository
                .findByMonthAndYearAndClosedFalse(month, year)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No active budget for this month"));
    }
}