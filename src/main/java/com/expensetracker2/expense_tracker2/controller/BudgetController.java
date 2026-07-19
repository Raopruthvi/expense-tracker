package com.expensetracker2.expense_tracker2.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.expensetracker2.expense_tracker2.model.Expense;
import com.expensetracker2.expense_tracker2.model.FinancialProfile;
import com.expensetracker2.expense_tracker2.model.MonthlyBudget;
import com.expensetracker2.expense_tracker2.service.BudgetService;
import com.expensetracker2.expense_tracker2.service.BudgetService.EndOfMonthResult;
import com.expensetracker2.expense_tracker2.service.BudgetService.ExpenseResult;
import com.expensetracker2.expense_tracker2.service.BudgetService.FinancialSummary;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    // ─── PROFILE ──────────────────────────────────────────────

    @PostMapping("/profile/setup")
    public ResponseEntity<FinancialProfile> setupProfile(
            @RequestBody Map<String, BigDecimal> request) {
        FinancialProfile profile = budgetService.setupProfile(
            request.get("accountBalance"),
            request.get("cashBalance"),
            request.get("salaryAmount"),
            request.get("initialSavings")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @GetMapping("/profile")
    public ResponseEntity<FinancialProfile> getProfile() {
        return ResponseEntity.ok(budgetService.getProfile());
    }

    @PutMapping("/profile/update")
    public ResponseEntity<FinancialProfile> updateProfile(
            @RequestBody Map<String, BigDecimal> request) {
        return ResponseEntity.ok(budgetService.updateProfile(
            request.get("accountBalance"),
            request.get("cashBalance"),
            request.get("salaryAmount"),
            request.get("savingsAmount")
        ));
    }

    @DeleteMapping("/profile/reset")
    public ResponseEntity<Void> resetProfile() {
        budgetService.resetProfile();
        return ResponseEntity.noContent().build();
    }

    // ─── MONTHLY BUDGET ───────────────────────────────────────

    @PostMapping("/monthly")
    public ResponseEntity<MonthlyBudget> createMonthlyBudget(
            @RequestBody Map<String, BigDecimal> request) {
        MonthlyBudget budget = budgetService.createMonthlyBudget(
            request.get("totalAllowance"),
            request.get("vehicleAllowance")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(budget);
    }

    @GetMapping("/monthly/current")
    public ResponseEntity<MonthlyBudget> getCurrentBudget() {
        return ResponseEntity.ok(budgetService.getCurrentBudget());
    }

    @PutMapping("/monthly/update")
    public ResponseEntity<MonthlyBudget> updateBudget(
            @RequestBody Map<String, BigDecimal> request) {
        return ResponseEntity.ok(budgetService.updateBudget(
            request.get("totalAllowance"),
            request.get("vehicleAllowance")
        ));
    }

    @DeleteMapping("/monthly/reset")
    public ResponseEntity<Void> resetBudget() {
        budgetService.resetBudget();
        return ResponseEntity.noContent().build();
    }

    // ─── EXPENSE ──────────────────────────────────────────────

    @PostMapping("/expense")
    public ResponseEntity<Map<String, Object>> recordExpense(
            @Valid @RequestBody Expense expense) {
        ExpenseResult result = budgetService.recordExpense(expense);
        Map<String, Object> response = new HashMap<>();
        response.put("expense", result.expense);
        response.put("remainingSalary", result.remainingSalary);
        if (result.warning != null) {
            response.put("warning", result.warning);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── MONTH END ────────────────────────────────────────────

    @PostMapping("/month-end")
    public ResponseEntity<EndOfMonthResult> closeMonth() {
        return ResponseEntity.ok(budgetService.closeMonth());
    }

    // ─── SUMMARY ──────────────────────────────────────────────

    @GetMapping("/summary")
    public ResponseEntity<FinancialSummary> getSummary() {
        return ResponseEntity.ok(budgetService.getSummary());
    }

    // ─── RESET ALL ────────────────────────────────────────────

    @DeleteMapping("/reset")
    public ResponseEntity<Void> resetAll() {
        budgetService.resetAll();
        return ResponseEntity.noContent().build();
    }
}