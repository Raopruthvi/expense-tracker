package com.expensetracker2.expense_tracker2.controller;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.expensetracker2.expense_tracker2.model.Person;
import com.expensetracker2.expense_tracker2.service.ExpenseService;

@RestController
@RequestMapping("/api/people")
public class PersonController {

    private final ExpenseService expenseService;

    public PersonController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public ResponseEntity<List<Person>> getAllPeople() {
        return ResponseEntity.ok(expenseService.getAllPeople());
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<String> getBalance(@PathVariable Long id) {
        BigDecimal owedTo = expenseService.getTotalOwedTo(id);
        BigDecimal owedBy = expenseService.getTotalOwedBy(id);
        BigDecimal net = expenseService.getNetBalance(id);
        String summary = String.format(
            "Others owe this person: ₹%s | This person owes others: ₹%s | Net: ₹%s",
            owedTo, owedBy, net);
        return ResponseEntity.ok(summary);
    }
}