package com.expensetracker2.expense_tracker2.controller;

import com.expensetracker2.expense_tracker2.model.Expense;
import com.expensetracker2.expense_tracker2.model.Person;

import com.expensetracker2.expense_tracker2.service.*;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
//This annotation does two things in one:
//Tells Spring: "this class handles incoming HTTP requests"
//Tells Spring: "whatever these methods return, convert it to JSON automatically and send it as the response body"
@RequestMapping("/api/people")
public class PersonController {
	private final ExpenseService expenseService;
	 
	public PersonController(ExpenseService expenseService) {
		this.expenseService=expenseService;
	}
	
	@PostMapping   //   POST/api/people
	public ResponseEntity<Person> createPerson(@Valid @RequestBody Person person){   //ResponseEntity gives you control over the entire HTTP response — not just the data but also the status code.
		Person saved=expenseService.savePerson(person);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}
	
	@GetMapping
	public ResponseEntity<List<Person>> getAllPeople(){
		return ResponseEntity.ok(expenseService.getAllPeople());
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<Person> getPersonById(@PathVariable Long id){
		return ResponseEntity.ok(expenseService.getPersonById(id));
	}
	
	@GetMapping("/{id}/balance")
	public ResponseEntity<String> getBalance(@PathVariable Long id){
		var owedTo=expenseService.getTotalOwedTo(id);
		var owedBy=expenseService.getTotalOwedBy(id);
		var net=expenseService.getNetBalance(id);
		
		String summary=String.format(
				"Others owe this person: ₹%s | This person owes others: ₹%s | Net balance: ₹%s",owedTo, owedBy, net
				);
		return ResponseEntity.ok(summary);
	}
	
	@GetMapping("/{id}/expenses")
	public ResponseEntity<List<Map<String, Object>>> getPersonExpenses(
	        @PathVariable Long id) {
	    Person person = expenseService.getPersonById(id);
	    List<Expense> allExpenses = expenseService.getAllExpenses();

	    List<Map<String, Object>> result = allExpenses.stream()
	        .filter(e -> !e.isSettled())
	        .filter(e -> (e.getPaidBy() != null && e.getPaidBy().getId().equals(id)) ||
	                     (e.getOwedBy() != null && e.getOwedBy().getId().equals(id)))
	        .map(e -> {
	            Map<String, Object> item = new java.util.HashMap<>();
	            item.put("description", e.getDescription());
	            item.put("amount", e.getAmount());
	            item.put("category", e.getCategory());
	            item.put("owedBy", e.getOwedBy() != null ? e.getOwedBy().getName() :
	                               e.getOwedByName());
	            item.put("paidBy", e.getPaidBy() != null ? e.getPaidBy().getName() :
	                               e.getPaidByName());
	            item.put("deadline", e.getDeadline());
	            return item;
	        })
	        .toList();

	    return ResponseEntity.ok(result);
	}
	

}
