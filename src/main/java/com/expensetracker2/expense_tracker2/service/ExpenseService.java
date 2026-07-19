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

    // ─── PERSON ───────────────────────────────────────────────

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

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    // ─── EXPENSE ──────────────────────────────────────────────

    public Expense saveExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    public Expense getExpenseById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Expense not found with id: " + id));
    }

    public void deleteExpense(Long id) {
        Expense expense = getExpenseById(id);
        reverseImpact(expense);
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

        String owedByName = getName(expense.getOwedByName(), expense.getOwedBy());
        String paidByName = getName(expense.getPaidByName(), expense.getPaidBy());

        boolean maniOwes = owedByName.equalsIgnoreCase(OWNER);
        boolean maniPaid = paidByName.isEmpty() ||
                           paidByName.equalsIgnoreCase(OWNER);

        try {
            FinancialProfile profile = getProfile();
            MonthlyBudget budget = getActiveBudget();

            if (maniOwes) {
                // Mani pays someone back → deduct balance + allowance NOW
                deductBalance(profile, expense);
                deductAllowance(profile, budget,
                                expense.getCategory(), expense.getAmount());
            } else if (maniPaid) {
                // Someone pays Mani back → add balance + allowance back
                addBalance(profile, expense);
                refundAllowance(profile, budget,
                                expense.getCategory(), expense.getAmount());
            }

            financialProfileRepository.save(profile);
            monthlyBudgetRepository.save(budget);
        } catch (Exception ignored) {}

        return expenseRepository.save(expense);
    }

    public BigDecimal getTotalOwedBy(Long personId) {
        Person person = getPersonById(personId);
        return expenseRepository.findAll().stream()
                .filter(e -> !e.isSettled())
                .filter(e -> e.getOwedBy() != null &&
                             e.getOwedBy().getId().equals(person.getId()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalOwedTo(Long personId) {
        Person person = getPersonById(personId);
        return expenseRepository.findAll().stream()
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

    private void reverseImpact(Expense expense) {
        try {
            FinancialProfile profile = getProfile();
            MonthlyBudget budget = getActiveBudget();

            String owedByName = getName(expense.getOwedByName(), expense.getOwedBy());
            String paidByName = getName(expense.getPaidByName(), expense.getPaidBy());

            boolean maniOwes = owedByName.equalsIgnoreCase(OWNER);
            boolean maniPaid = paidByName.isEmpty() ||
                               paidByName.equalsIgnoreCase(OWNER);

            if (expense.isSettled()) {
                // Reverse whatever happened at settle time
                if (maniOwes) {
                    // At settle: deducted from Mani → reverse: add back
                    addBalance(profile, expense);
                    refundAllowance(profile, budget,
                                    expense.getCategory(), expense.getAmount());
                } else if (maniPaid) {
                    // At settle: added to Mani → reverse: deduct back
                    deductBalance(profile, expense);
                    deductAllowance(profile, budget,
                                    expense.getCategory(), expense.getAmount());
                }
            } else {
                // Reverse whatever happened when expense was added
                if (maniPaid && !maniOwes) {
                    // At add: deducted from Mani → reverse: add back
                    addBalance(profile, expense);
                    refundAllowance(profile, budget,
                                    expense.getCategory(), expense.getAmount());
                }
                // If maniOwes + unsettled: nothing was deducted yet → nothing to reverse
            }

            financialProfileRepository.save(profile);
            monthlyBudgetRepository.save(budget);
        } catch (Exception ignored) {}
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
            BigDecimal rem = budget.getRemainingVehicleAllowance();
            if (rem.compareTo(amount) >= 0) {
                budget.setRemainingVehicleAllowance(rem.subtract(amount));
            } else {
                BigDecimal overflow = amount.subtract(rem);
                budget.setRemainingVehicleAllowance(BigDecimal.ZERO);
                profile.setRemainingSalary(
                    profile.getRemainingSalary().subtract(overflow));
            }
        } else if (isGeneralCategory(category)) {
            BigDecimal rem = budget.getRemainingAllowance();
            if (rem.compareTo(amount) >= 0) {
                budget.setRemainingAllowance(rem.subtract(amount));
            } else {
                BigDecimal overflow = amount.subtract(rem);
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
        } else if (isGeneralCategory(category)) {
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

    private boolean isGeneralCategory(Category category) {
        return category == Category.FOOD ||
               category == Category.SPORTS ||
               category == Category.MISCELLANEOUS ||
               category == Category.ONLINE_SHOPPING ||
               category == Category.SUBSCRIPTIONS ||
               category == Category.TRIP;
    }

    private String getName(String nameField, Person person) {
        if (nameField != null && !nameField.isBlank()) return nameField;
        if (person != null) return person.getName();
        return "";
    }

    private FinancialProfile getProfile() {
        return financialProfileRepository.findAll()
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No profile found"));
    }

    private MonthlyBudget getActiveBudget() {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();
        return monthlyBudgetRepository
                .findByMonthAndYearAndClosedFalse(month, year)
                .orElseThrow(() -> new RuntimeException("No active budget"));
    }
}