package com.expensetracker2.expense_tracker2.service;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    // ─── PROFILE ──────────────────────────────────────────────

    public FinancialProfile setupProfile(BigDecimal accountBalance,
                                          BigDecimal cashBalance,
                                          BigDecimal salaryAmount,
                                          BigDecimal initialSavings) {
        if (!profileRepository.findAll().isEmpty()) {
            throw new IllegalArgumentException("Profile already exists.");
        }
        return profileRepository.save(new FinancialProfile(
            accountBalance, cashBalance, salaryAmount,
            initialSavings != null ? initialSavings : BigDecimal.ZERO));
    }

    public FinancialProfile getProfile() {
        return profileRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No profile found. Please set up your profile."));
    }

    public FinancialProfile updateProfile(BigDecimal accountBalance,
                                           BigDecimal cashBalance,
                                           BigDecimal salaryAmount,
                                           BigDecimal savingsAmount) {
        FinancialProfile p = getProfile();
        if (accountBalance != null) p.setAccountBalance(accountBalance);
        if (cashBalance != null) p.setCashBalance(cashBalance);
        if (salaryAmount != null) {
            p.setSalaryAmount(salaryAmount);
            p.setRemainingSalary(salaryAmount);
        }
        if (savingsAmount != null) p.setSavingsAmount(savingsAmount);
        return profileRepository.save(p);
    }

    // ─── BUDGET ───────────────────────────────────────────────

    public MonthlyBudget createMonthlyBudget(BigDecimal totalAllowance,
                                              BigDecimal vehicleAllowance) {
        int m = LocalDate.now().getMonthValue();
        int y = LocalDate.now().getYear();
        budgetRepository.findByMonthAndYearAndClosedFalse(m, y).ifPresent(b -> {
            throw new IllegalArgumentException("Budget for this month already exists.");
        });
        return budgetRepository.save(new MonthlyBudget(m, y, totalAllowance, vehicleAllowance));
    }

    public MonthlyBudget getCurrentBudget() {
        int m = LocalDate.now().getMonthValue();
        int y = LocalDate.now().getYear();
        return budgetRepository.findByMonthAndYearAndClosedFalse(m, y)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No active budget for this month."));
    }

    public MonthlyBudget updateBudget(BigDecimal totalAllowance,
                                       BigDecimal vehicleAllowance) {
        MonthlyBudget b = getCurrentBudget();
        if (totalAllowance != null) {
            BigDecimal diff = totalAllowance.subtract(b.getTotalAllowance());
            b.setTotalAllowance(totalAllowance);
            b.setRemainingAllowance(b.getRemainingAllowance().add(diff));
        }
        if (vehicleAllowance != null) {
            BigDecimal diff = vehicleAllowance.subtract(b.getVehicleAllowance());
            b.setVehicleAllowance(vehicleAllowance);
            b.setRemainingVehicleAllowance(b.getRemainingVehicleAllowance().add(diff));
        }
        return budgetRepository.save(b);
    }

    // ─── RECORD EXPENSE ───────────────────────────────────────

    public ExpenseResult recordExpense(Expense expense) {
        FinancialProfile profile = getProfile();
        MonthlyBudget budget = getCurrentBudget();
        String warning = null;

        String paidByName = trim(expense.getPaidByName());
        String owedByName = trim(expense.getOwedByName());

        if (!paidByName.isEmpty()) expense.setPaidBy(findOrCreate(paidByName));
        if (!owedByName.isEmpty()) expense.setOwedBy(findOrCreate(owedByName));

        boolean maniIsPaying = paidByName.isEmpty() ||
                               paidByName.equalsIgnoreCase(OWNER);
        boolean maniOwes = owedByName.equalsIgnoreCase(OWNER);

        if (maniIsPaying && !maniOwes) {
            deductBalance(profile, expense);
            warning = deductAllowance(profile, budget,
                      expense.getCategory(), expense.getAmount());
        }

        profileRepository.save(profile);
        budgetRepository.save(budget);
        Expense saved = expenseRepository.save(expense);
        return new ExpenseResult(saved, warning, profile.getRemainingSalary());
    }

    // ─── SETTLE ───────────────────────────────────────────────

    public Expense settleExpense(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));

        String paidByName = trim(expense.getPaidByName() != null ? expense.getPaidByName() :
                (expense.getPaidBy() != null ? expense.getPaidBy().getName() : ""));
        String owedByName = trim(expense.getOwedByName() != null ? expense.getOwedByName() :
                (expense.getOwedBy() != null ? expense.getOwedBy().getName() : ""));

        boolean maniOwes = owedByName.equalsIgnoreCase(OWNER);
        boolean maniPaid = paidByName.isEmpty() || paidByName.equalsIgnoreCase(OWNER);

        try {
            FinancialProfile profile = getProfile();
            MonthlyBudget budget = getCurrentBudget();

            if (maniOwes) {
                deductBalance(profile, expense);
                deductAllowance(profile, budget, expense.getCategory(), expense.getAmount());
            } else if (maniPaid) {
                addBalance(profile, expense);
                refundAllowance(profile, budget, expense.getCategory(), expense.getAmount());
            }

            profileRepository.save(profile);
            budgetRepository.save(budget);
        } catch (Exception ignored) {}

        expense.setSettled(true);
        return expenseRepository.save(expense);
    }

    // ─── DELETE ───────────────────────────────────────────────

    public void deleteExpense(Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found: " + id));

        String paidByName = trim(expense.getPaidByName() != null ? expense.getPaidByName() :
                (expense.getPaidBy() != null ? expense.getPaidBy().getName() : ""));
        String owedByName = trim(expense.getOwedByName() != null ? expense.getOwedByName() :
                (expense.getOwedBy() != null ? expense.getOwedBy().getName() : ""));

        boolean maniOwes = owedByName.equalsIgnoreCase(OWNER);
        boolean maniPaid = paidByName.isEmpty() || paidByName.equalsIgnoreCase(OWNER);

        try {
            FinancialProfile profile = getProfile();
            MonthlyBudget budget = getCurrentBudget();

            if (!expense.isSettled()) {
                if (maniPaid && !maniOwes) {
                    // Mani paid, not yet collected → refund
                    addBalance(profile, expense);
                    refundAllowance(profile, budget,
                                    expense.getCategory(), expense.getAmount());
                }
                // maniOwes + unsettled → nothing deducted yet → do nothing
            } else {
                if (maniOwes) {
                    // Mani paid back at settle → reverse that payback → refund
                    addBalance(profile, expense);
                    refundAllowance(profile, budget,
                                    expense.getCategory(), expense.getAmount());
                }
                // maniPaid + settled (Collect) → already refunded at collect → do nothing
            }

            profileRepository.save(profile);
            budgetRepository.save(budget);
        } catch (Exception ignored) {}
        expenseRepository.deleteById(id);
    }

    // ─── MONTH END ────────────────────────────────────────────

    public EndOfMonthResult closeMonth() {
        FinancialProfile profile = getProfile();
        MonthlyBudget budget = getCurrentBudget();

        BigDecimal aLeft = budget.getRemainingAllowance();
        BigDecimal vLeft = budget.getRemainingVehicleAllowance();
        BigDecimal sLeft = profile.getRemainingSalary();
        BigDecimal total = aLeft.add(vLeft).add(sLeft);

        profile.setSavingsAmount(profile.getSavingsAmount().add(total));
        profile.setRemainingSalary(profile.getSalaryAmount());
        budget.setClosed(true);

        profileRepository.save(profile);
        budgetRepository.save(budget);

        return new EndOfMonthResult(aLeft, vLeft, sLeft, total, profile.getSavingsAmount());
    }

    // ─── SUMMARY ──────────────────────────────────────────────

    public FinancialSummary getSummary() {
        return new FinancialSummary(getProfile(), getCurrentBudget());
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

    // ─── HELPERS ──────────────────────────────────────────────

    private Person findOrCreate(String name) {
        return personRepository.findAll().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> personRepository.save(new Person(name)));
    }

    private void deductBalance(FinancialProfile profile, Expense expense) {
        if (expense.isCash()) {
            profile.setCashBalance(
                profile.getCashBalance().subtract(expense.getAmount()));
        } else {
            profile.setAccountBalance(
                profile.getAccountBalance().subtract(expense.getAmount()));
        }
    }

    private void addBalance(FinancialProfile profile, Expense expense) {
        if (expense.isCash()) {
            profile.setCashBalance(
                profile.getCashBalance().add(expense.getAmount()));
        } else {
            profile.setAccountBalance(
                profile.getAccountBalance().add(expense.getAmount()));
        }
    }

    private String deductAllowance(FinancialProfile profile, MonthlyBudget budget,
                                    Category category, BigDecimal amount) {
        String warning = null;
        if (category == Category.VEHICLE) {
            BigDecimal rem = budget.getRemainingVehicleAllowance();
            if (rem.compareTo(amount) >= 0) {
                budget.setRemainingVehicleAllowance(rem.subtract(amount));
            } else {
                BigDecimal overflow = amount.subtract(rem);
                budget.setRemainingVehicleAllowance(BigDecimal.ZERO);
                profile.setRemainingSalary(
                    profile.getRemainingSalary().subtract(overflow));
                warning = "Vehicle allowance exhausted. ₹" + overflow +
                          " deducted from salary.";
            }
        } else {
            BigDecimal rem = budget.getRemainingAllowance();
            if (rem.compareTo(amount) >= 0) {
                budget.setRemainingAllowance(rem.subtract(amount));
            } else {
                BigDecimal overflow = amount.subtract(rem);
                budget.setRemainingAllowance(BigDecimal.ZERO);
                profile.setRemainingSalary(
                    profile.getRemainingSalary().subtract(overflow));
                warning = "General allowance exhausted. ₹" + overflow +
                          " deducted from salary.";
            }
        }
        return warning;
    }

    private void refundAllowance(FinancialProfile profile, MonthlyBudget budget,
            Category category, BigDecimal amount) {
if (category == Category.VEHICLE) {
BigDecimal current = budget.getRemainingVehicleAllowance();
BigDecimal max = budget.getVehicleAllowance();
BigDecimal space = max.subtract(current); // how much room in allowance

if (amount.compareTo(space) <= 0) {
// Entire refund fits in allowance
budget.setRemainingVehicleAllowance(current.add(amount));
} else {
// Fill allowance to max, rest goes to salary
budget.setRemainingVehicleAllowance(max);
BigDecimal toSalary = amount.subtract(space);
profile.setRemainingSalary(profile.getRemainingSalary().add(toSalary));
}
} else {
BigDecimal current = budget.getRemainingAllowance();
BigDecimal max = budget.getTotalAllowance();
BigDecimal space = max.subtract(current); // how much room in allowance

if (amount.compareTo(space) <= 0) {
// Entire refund fits in allowance
budget.setRemainingAllowance(current.add(amount));
} else {
// Fill allowance to max, rest goes to salary
budget.setRemainingAllowance(max);
BigDecimal toSalary = amount.subtract(space);
profile.setRemainingSalary(profile.getRemainingSalary().add(toSalary));
}
}
}

    private String trim(String s) {
        return s != null ? s.trim() : "";
    }

    // ─── INNER CLASSES ────────────────────────────────────────

    public static class ExpenseResult {
        public final Expense expense;
        public final String warning;
        public final BigDecimal remainingSalary;
        public ExpenseResult(Expense e, String w, BigDecimal r) {
            this.expense = e; this.warning = w; this.remainingSalary = r;
        }
    }

    public static class EndOfMonthResult {
        public final BigDecimal allowanceSaved;
        public final BigDecimal vehicleSaved;
        public final BigDecimal salarySaved;
        public final BigDecimal totalAddedToSavings;
        public final BigDecimal newTotalSavings;
        public EndOfMonthResult(BigDecimal a, BigDecimal v, BigDecimal s,
                                BigDecimal t, BigDecimal n) {
            allowanceSaved = a; vehicleSaved = v; salarySaved = s;
            totalAddedToSavings = t; newTotalSavings = n;
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
        public FinancialSummary(FinancialProfile p, MonthlyBudget b) {
            accountBalance = p.getAccountBalance();
            cashBalance = p.getCashBalance();
            salaryAmount = p.getSalaryAmount();
            remainingSalary = p.getRemainingSalary();
            savingsAmount = p.getSavingsAmount();
            remainingAllowance = b.getRemainingAllowance();
            remainingVehicleAllowance = b.getRemainingVehicleAllowance();
        }
    }
}