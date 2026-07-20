package com.expensetracker2.expense_tracker2.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.expensetracker2.expense_tracker2.model.Expense;
import com.expensetracker2.expense_tracker2.service.ExpenseService;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    @GetMapping("/unsettled")
    public ResponseEntity<List<Expense>> getUnsettled() {
        return ResponseEntity.ok(expenseService.getUnsettledExpenses());
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Expense>> getOverdue() {
        return ResponseEntity.ok(expenseService.getOverdueExpenses());
    }
}