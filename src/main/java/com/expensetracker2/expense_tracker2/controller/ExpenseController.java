package com.expensetracker2.expense_tracker2.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.expensetracker2.expense_tracker2.service.ExpenseService;
import com.expensetracker2.expense_tracker2.model.Expense;

import java.util.List;


@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
	
	private final ExpenseService expenseService;
	
	public ExpenseController(ExpenseService expenseService) {
		this.expenseService=expenseService;
	}
	
	@GetMapping
	public ResponseEntity<List<Expense>> getAllExpenses(){
		return ResponseEntity.ok(expenseService.getAllExpenses());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<Expense> getExpenseById(@PathVariable Long id){
		return ResponseEntity.ok(expenseService.getExpenseById(id));
		
	}
	
	@PostMapping
	public ResponseEntity<Expense> createExpense(@RequestBody Expense expense){
		Expense saved=expenseService.saveExpense(expense);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
		
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteExpense(@PathVariable Long id){
		expenseService.deleteExpense(id);
		return ResponseEntity.noContent().build();
	}
	
	@PutMapping("/{id}/settle")
	public ResponseEntity<Expense> settleExpense(@PathVariable Long id){
		return ResponseEntity.ok(expenseService.settleExpense(id));
	}
	
	@GetMapping("/unsettled")
	public ResponseEntity<List<Expense>> getUnsettled(){
		return ResponseEntity.ok(expenseService.getUnsettledExpenses());
	}
	
	@GetMapping("/overdue")
	public ResponseEntity<List<Expense>> getOverdue(){
		return ResponseEntity.ok(expenseService.getOverdueExpenses());
	}

}
