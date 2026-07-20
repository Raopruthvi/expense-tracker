package com.expensetracker2.expense_tracker2.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.expensetracker2.expense_tracker2.model.*;
import com.expensetracker2.expense_tracker2.service.BudgetService;
import com.expensetracker2.expense_tracker2.service.BudgetService.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping("/profile/setup")
    public ResponseEntity<FinancialProfile> setupProfile(
            @RequestBody Map<String, BigDecimal> req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            budgetService.setupProfile(
                req.get("accountBalance"), req.get("cashBalance"),
                req.get("salaryAmount"), req.get("initialSavings")));
    }

    @GetMapping("/profile")
    public ResponseEntity<FinancialProfile> getProfile() {
        return ResponseEntity.ok(budgetService.getProfile());
    }

    @PutMapping("/profile/update")
    public ResponseEntity<FinancialProfile> updateProfile(
            @RequestBody Map<String, BigDecimal> req) {
        return ResponseEntity.ok(budgetService.updateProfile(
            req.get("accountBalance"), req.get("cashBalance"),
            req.get("salaryAmount"), req.get("savingsAmount")));
    }

    @DeleteMapping("/profile/reset")
    public ResponseEntity<Void> resetProfile() {
        budgetService.resetProfile();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/monthly")
    public ResponseEntity<MonthlyBudget> createBudget(
            @RequestBody Map<String, BigDecimal> req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            budgetService.createMonthlyBudget(
                req.get("totalAllowance"), req.get("vehicleAllowance")));
    }

    @GetMapping("/monthly/current")
    public ResponseEntity<MonthlyBudget> getCurrentBudget() {
        return ResponseEntity.ok(budgetService.getCurrentBudget());
    }

    @PutMapping("/monthly/update")
    public ResponseEntity<MonthlyBudget> updateBudget(
            @RequestBody Map<String, BigDecimal> req) {
        return ResponseEntity.ok(budgetService.updateBudget(
            req.get("totalAllowance"), req.get("vehicleAllowance")));
    }

    @DeleteMapping("/monthly/reset")
    public ResponseEntity<Void> resetBudget() {
        budgetService.resetBudget();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/expense")
    public ResponseEntity<Map<String, Object>> recordExpense(
            @Valid @RequestBody Expense expense) {
        ExpenseResult result = budgetService.recordExpense(expense);
        Map<String, Object> response = new HashMap<>();
        response.put("expense", result.expense);
        response.put("remainingSalary", result.remainingSalary);
        if (result.warning != null) response.put("warning", result.warning);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/expense/{id}/settle")
    public ResponseEntity<Expense> settleExpense(@PathVariable Long id) {
        return ResponseEntity.ok(budgetService.settleExpense(id));
    }

    @DeleteMapping("/expense/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        budgetService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/month-end")
    public ResponseEntity<EndOfMonthResult> closeMonth() {
        return ResponseEntity.ok(budgetService.closeMonth());
    }

    @GetMapping("/summary")
    public ResponseEntity<FinancialSummary> getSummary() {
        return ResponseEntity.ok(budgetService.getSummary());
    }

    @DeleteMapping("/reset")
    public ResponseEntity<Void> resetAll() {
        budgetService.resetAll();
        return ResponseEntity.noContent().build();
    }
}