
package com.expensetracker2.expense_tracker2.controller;
import java.math.BigDecimal;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
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
		this.budgetService=budgetService;
	}
	
	//-----Profile Endpoint-----------
	
@PostMapping("/profile/setup")
public ResponseEntity<FinancialProfile> setupProfile(@RequestBody Map<String, BigDecimal> request){
	BigDecimal accountBalance=request.get("accountBalance");
	BigDecimal cashBalance=request.get("cashBalance");
	BigDecimal salaryAmount=request.get("salaryAmount");
	
	FinancialProfile profile=budgetService.setupProfile(
			accountBalance, cashBalance, salaryAmount);
	
	return ResponseEntity.status(HttpStatus.CREATED).body(profile);
}

@GetMapping("/profile")
public ResponseEntity<FinancialProfile> getProfile(){
	return ResponseEntity.ok(budgetService.getProfile());
}

@PutMapping("/profile/salary")
public ResponseEntity<FinancialProfile> updateSalary(
		@RequestBody Map<String, BigDecimal> request){
	BigDecimal newSalary=request.get("salaryAmount");
	return ResponseEntity.ok(budgetService.updateSalary(newSalary));
	
}

//-------Monthly Budget Endpoints--------

@PostMapping("/monthly")

public ResponseEntity<MonthlyBudget> createMonthlyBudget(
		@RequestBody Map<String, BigDecimal> request){
	BigDecimal totalAllowance=request.get("totalAllowance");
	BigDecimal vehicleAllowance=request.get("vehicleAllowance");
	
	MonthlyBudget budget= budgetService.createMonthlyBudget(totalAllowance, vehicleAllowance);
	
	return ResponseEntity.status(HttpStatus.CREATED).body(budget);
}

@GetMapping("/monthly/current")
public ResponseEntity<MonthlyBudget> getCurrentBudget(){
	return ResponseEntity.ok(budgetService.getCurrentBudget());
}

//--------Record expense with budget deduction-------
@PostMapping("/expense")
public ResponseEntity<Map<String, Object>> recordExpense(@Valid @RequestBody Expense expense){
	ExpenseResult result=budgetService.recordExpense(expense);
	
	Map<String, Object> response=new HashMap<>();
	response.put("expense", result.expense);
	response.put("remainingSalary", result.remainingSalary);
	
	if(result.warning!=null) {
		response.put("warning", result.warning);
	}
	return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

//-------End of month------

@PostMapping("/month-end")
public ResponseEntity<EndOfMonthResult> closeMonth(){
	return ResponseEntity.ok(budgetService.closeMonth());
}

//-----------Summary---------

@GetMapping("/summary")
public ResponseEntity<FinancialSummary> getSummary(){
	return ResponseEntity.ok(budgetService.getSummary());
}

}
