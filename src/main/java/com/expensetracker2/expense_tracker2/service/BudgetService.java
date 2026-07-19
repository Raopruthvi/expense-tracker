package com.expensetracker2.expense_tracker2.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.expensetracker2.expense_tracker2.exception.ResourceNotFoundException;
import com.expensetracker2.expense_tracker2.model.*;
import com.expensetracker2.expense_tracker2.repository.*;

@Service
public class BudgetService {

    private static final String OWNER = "Mani";

    private final FinancialProfileRepository profileRepository;
    private final MonthlyBudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final PersonRepository personRepository;

    public BudgetService(FinancialProfileRepository profileRepository,
                         MonthlyBudgetRepository budgetRepository,
                         ExpenseRepository expenseRepository,
                         PersonRepository personRepository) {
        this.profileRepository = profileRepository;
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.personRepository = personRepository;
    }

    // ─── FINANCIAL PROFILE ────────────────────────────────────

    public FinancialProfile setupProfile(BigDecimal accountBalance,
                                          BigDecimal cashBalance,
                                          BigDecimal salaryAmount,
                                          BigDecimal initialSavings) {
        if (!profileRepository.findAll().isEmpty()) {
            throw new IllegalArgumentException(
                "Profile already exists. Use update endpoint.");
        }
        FinancialProfile profile = new FinancialProfile(
            accountBalance, cashBalance, salaryAmount,
            initialSavings != null ? initialSavings : BigDecimal.ZERO);
        return profileRepository.save(profile);
    }

    public FinancialProfile getProfile() {
        return profileRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No financial profile found. Please set up your profile first."));
    }

    public FinancialProfile updateProfile(BigDecimal accountBalance,
                                           BigDecimal cashBalance,
                                           BigDecimal salaryAmount,
                                           BigDecimal savingsAmount) {
        FinancialProfile profile = getProfile();
        if (accountBalance != null) profile.setAccountBalance(accountBalance);
        if (cashBalance != null) profile.setCashBalance(cashBalance);
        if (salaryAmount != null) {
            profile.setSalaryAmount(salaryAmount);
            profile.setRemainingSalary(salaryAmount);
        }
        if (savingsAmount != null) profile.setSavingsAmount(savingsAmount);
        return profileRepository.save(profile);
    }

    public FinancialProfile saveProfile(FinancialProfile profile) {
        return profileRepository.save(profile);
    }

    // ─── MONTHLY BUDGET ───────────────────────────────────────

    public MonthlyBudget createMonthlyBudget(BigDecimal totalAllowance,
                                              BigDecimal vehicleAllowance) {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        budgetRepository.findByMonthAndYearAndClosedFalse(currentMonth, currentYear)
                .ifPresent(b -> {
                    throw new IllegalArgumentException(
                        "A budget for this month already exists. Use update.");
                });

        MonthlyBudget budget = new MonthlyBudget(
            currentMonth, currentYear, totalAllowance, vehicleAllowance);
        return budgetRepository.save(budget);
    }

    public MonthlyBudget getCurrentBudget() {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        return budgetRepository
                .findByMonthAndYearAndClosedFalse(currentMonth, currentYear)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No active budget found for this month."));
    }

    public MonthlyBudget updateBudget(BigDecimal totalAllowance,
                                       BigDecimal vehicleAllowance) {
        MonthlyBudget budget = getCurrentBudget();
        if (totalAllowance != null) {
            BigDecimal diff = totalAllowance.subtract(budget.getTotalAllowance());
            budget.setTotalAllowance(totalAllowance);
            budget.setRemainingAllowance(budget.getRemainingAllowance().add(diff));
        }
        if (vehicleAllowance != null) {
            BigDecimal diff = vehicleAllowance.subtract(budget.getVehicleAllowance());
            budget.setVehicleAllowance(vehicleAllowance);
            budget.setRemainingVehicleAllowance(
                budget.getRemainingVehicleAllowance().add(diff));
        }
        return budgetRepository.save(budget);
    }

    public MonthlyBudget saveBudget(MonthlyBudget budget) {
        return budgetRepository.save(budget);
    }

    // ─── RECORD EXPENSE ───────────────────────────────────────

    public ExpenseResult recordExpense(Expense expense) {
        FinancialProfile profile = getProfile();
        MonthlyBudget budget = getCurrentBudget();
        String warning = null;

        String paidByName = expense.getPaidByName() != null ?
                expense.getPaidByName().trim() : "";
        String owedByName = expense.getOwedByName() != null ?
                expense.getOwedByName().trim() : "";

        // Auto-create people from names
        if (!paidByName.isEmpty()) {
            Person p = findOrCreatePerson(paidByName);
            expense.setPaidBy(p);
        }
        if (!owedByName.isEmpty()) {
            Person p = findOrCreatePerson(owedByName);
            expense.setOwedBy(p);
        }

        // Mani is paying if no paidBy specified or paidBy is Mani
        boolean maniIsPaying = paidByName.isEmpty() ||
                               paidByName.equalsIgnoreCase(OWNER);
        // Mani owes someone else (someone else paid, Mani needs to pay back)
        boolean maniOwes = owedByName.equalsIgnoreCase(OWNER);

        // Only deduct NOW if Mani is the one paying and he doesn't owe
        // If maniOwes → deduction happens when settle is pressed
        if (maniIsPaying && !maniOwes) {
            if (expense.isCash()) {
                // Cash: deduct from both cash balance and account balance
                profile.setCashBalance(
                    profile.getCashBalance().subtract(expense.getAmount()));
                profile.setAccountBalance(
                    profile.getAccountBalance().subtract(expense.getAmount()));
            } else {
                // Non-cash: deduct only from account balance
                profile.setAccountBalance(
                    profile.getAccountBalance().subtract(expense.getAmount()));
            }
            // Deduct from allowance pool
            warning = deductFromAllowance(profile, budget,
                      expense.getCategory(), expense.getAmount());
        }

        profileRepository.save(profile);
        budgetRepository.save(budget);
        Expense saved = expenseRepository.save(expense);

        return new ExpenseResult(saved, warning, profile.getRemainingSalary());
    }

    private Person findOrCreatePerson(String name) {
        return personRepository.findAll()
                .stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> personRepository.save(new Person(name)));
    }

    private String deductFromAllowance(FinancialProfile profile,
                                        MonthlyBudget budget,
                                        Category category,
                                        BigDecimal amount) {
        String warning = null;
        if (category == Category.VEHICLE) {
            BigDecimal remaining = budget.getRemainingVehicleAllowance();
            if (remaining.compareTo(amount) >= 0) {
                budget.setRemainingVehicleAllowance(remaining.subtract(amount));
            } else {
                BigDecimal overflow = amount.subtract(remaining);
                budget.setRemainingVehicleAllowance(BigDecimal.ZERO);
                profile.setRemainingSalary(
                    profile.getRemainingSalary().subtract(overflow));
                warning = "Vehicle allowance exhausted. ₹" + overflow +
                          " deducted from salary. Remaining salary: ₹" +
                          profile.getRemainingSalary();
            }
        } else if (isGeneralCategory(category)) {
            BigDecimal remaining = budget.getRemainingAllowance();
            if (remaining.compareTo(amount) >= 0) {
                budget.setRemainingAllowance(remaining.subtract(amount));
            } else {
                BigDecimal overflow = amount.subtract(remaining);
                budget.setRemainingAllowance(BigDecimal.ZERO);
                profile.setRemainingSalary(
                    profile.getRemainingSalary().subtract(overflow));
                warning = "Monthly allowance exhausted. ₹" + overflow +
                          " deducted from salary. Remaining salary: ₹" +
                          profile.getRemainingSalary();
            }
        } else {
            // GENERAL: deduct from salary
            profile.setRemainingSalary(
                profile.getRemainingSalary().subtract(amount));
        }
        return warning;
    }

    private void refundToAllowance(FinancialProfile profile,
                                    MonthlyBudget budget,
                                    Category category,
                                    BigDecimal amount) {
        if (category == Category.VEHICLE) {
            BigDecimal newVehicle = budget.getRemainingVehicleAllowance().add(amount);
            if (newVehicle.compareTo(budget.getVehicleAllowance()) > 0) {
                BigDecimal overflow = newVehicle.subtract(budget.getVehicleAllowance());
                budget.setRemainingVehicleAllowance(budget.getVehicleAllowance());
                profile.setRemainingSalary(profile.getRemainingSalary().add(overflow));
            } else {
                budget.setRemainingVehicleAllowance(newVehicle);
            }
        } else if (isGeneralCategory(category)) {
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

    private boolean isGeneralCategory(Category category) {
        return category == Category.FOOD ||
               category == Category.SPORTS ||
               category == Category.MISCELLANEOUS ||
               category == Category.ONLINE_SHOPPING ||
               category == Category.SUBSCRIPTIONS ||
               category == Category.TRIP;
    }

    // ─── END OF MONTH ─────────────────────────────────────────

    public EndOfMonthResult closeMonth() {
        FinancialProfile profile = getProfile();
        MonthlyBudget budget = getCurrentBudget();

        BigDecimal allowanceSavings = budget.getRemainingAllowance();
        BigDecimal vehicleSavings = budget.getRemainingVehicleAllowance();
        BigDecimal salarySavings = profile.getRemainingSalary();
        BigDecimal total = allowanceSavings.add(vehicleSavings).add(salarySavings);

        profile.setSavingsAmount(profile.getSavingsAmount().add(total));
        profile.setRemainingSalary(profile.getSalaryAmount());
        budget.setClosed(true);

        profileRepository.save(profile);
        budgetRepository.save(budget);

        return new EndOfMonthResult(allowanceSavings, vehicleSavings,
                                    salarySavings, total,
                                    profile.getSavingsAmount());
    }

    // ─── SUMMARY ──────────────────────────────────────────────

    public FinancialSummary getSummary() {
        FinancialProfile profile = getProfile();
        MonthlyBudget budget = getCurrentBudget();
        return new FinancialSummary(profile, budget);
    }

    // ─── RESET ────────────────────────────────────────────────

    public void resetAll() {
        expenseRepository.deleteAll();
        personRepository.deleteAll();
        profileRepository.deleteAll();
        budgetRepository.deleteAll();
    }

    public void resetProfile() {
        profileRepository.deleteAll();
    }

    public void resetBudget() {
        budgetRepository.deleteAll();
    }

    // ─── INNER CLASSES ────────────────────────────────────────

    public static class ExpenseResult {
        public final Expense expense;
        public final String warning;
        public final BigDecimal remainingSalary;

        public ExpenseResult(Expense expense, String warning,
                             BigDecimal remainingSalary) {
            this.expense = expense;
            this.warning = warning;
            this.remainingSalary = remainingSalary;
        }
    }

    public static class EndOfMonthResult {
        public final BigDecimal allowanceSaved;
        public final BigDecimal vehicleSaved;
        public final BigDecimal salarySaved;
        public final BigDecimal totalAddedToSavings;
        public final BigDecimal newTotalSavings;

        public EndOfMonthResult(BigDecimal allowanceSaved,
                                BigDecimal vehicleSaved,
                                BigDecimal salarySaved,
                                BigDecimal totalAddedToSavings,
                                BigDecimal newTotalSavings) {
            this.allowanceSaved = allowanceSaved;
            this.vehicleSaved = vehicleSaved;
            this.salarySaved = salarySaved;
            this.totalAddedToSavings = totalAddedToSavings;
            this.newTotalSavings = newTotalSavings;
        }
    }

    public static class FinancialSummary {
        public final BigDecimal accountBalance;
        public final BigDecimal cashBalance;
        public final BigDecimal salaryAmount;
        public final BigDecimal remainingSalary;
        public final BigDecimal savingsAmount;
        public final BigDecimal remainingAllowance;
        public final BigDecimal remainingVehicleAllowance;

        public FinancialSummary(FinancialProfile profile, MonthlyBudget budget) {
            this.accountBalance = profile.getAccountBalance();
            this.cashBalance = profile.getCashBalance();
            this.salaryAmount = profile.getSalaryAmount();
            this.remainingSalary = profile.getRemainingSalary();
            this.savingsAmount = profile.getSavingsAmount();
            this.remainingAllowance = budget.getRemainingAllowance();
            this.remainingVehicleAllowance = budget.getRemainingVehicleAllowance();
        }
    }
}