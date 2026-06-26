package com.expensetracker2.expense_tracker2.service;

import com.expensetracker2.expense_tracker2.model.*;
import com.expensetracker2.expense_tracker2.repository.*;

import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public class ExpenseService {
	private final ExpenseRepository expenseRepository;
	private final PersonRepository personRepository;
	
	public ExpenseService(ExpenseRepository expenseRepository, PersonRepository personRepository) {
		this.expenseRepository=expenseRepository;
		this.personRepository=personRepository;
	}
	
	//---------Person methods------------
	
	public Person savePerson(Person person) {
		return personRepository.save(person);
	}
	
	public List<Person> getAllPeople(){
		return personRepository.findAll();	} 
	
	public Person getPersonById(Long id) {
		return personRepository.findById(id) 
				.orElseThrow(()->new RuntimeException("Person not found with Id: "+id));
	}
	
	
	//--------Expense methods-----------
	
	public Expense saveExpense(Expense expense) {
		return expenseRepository.save(expense);
	}
	
	public List<Expense> getAllExpenses() {
		return expenseRepository.findAll();
	}
	
	public Expense getExpenseById(Long id) {
		return expenseRepository.findById(id)
				.orElseThrow(()-> new RuntimeException("Expense not found with id: "+id));
	}
	
	public void deleteExpense(Long id) {
		 expenseRepository.deleteById(id);
	}
	
	//------Business Logic--------
	
	public List<Expense> getUnsettledExpenses(){
		return expenseRepository.findAll()
				.stream()
				.filter(e->!e.isSettled())
				.toList();
	}
	
	public List<Expense> getOverdueExpenses(){
		return expenseRepository.findAll()
				.stream()
				.filter(e->!e.isSettled() && e.getDeadline().isBefore(LocalDate.now()))
				.toList();
	}
	
	public Expense settleExpense(Long id) {
		Expense expense=getExpenseById(id);
		expense.setSettled(true);
		return expenseRepository.save(expense);
	}
	
	public BigDecimal getTotalOwedBy(Long personId) {
		Person person=getPersonById(personId);
		return expenseRepository.findAll()
				.stream()
				.filter(e->!e.isSettled())
				.filter(e->e.getOwedBy().getId().equals(person.getId()))
				.map(Expense::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
	
	public BigDecimal getTotalOwedTo(Long personId) {
		Person person=getPersonById(personId);
		return expenseRepository.findAll()
				.stream()
				.filter(e->!e.isSettled())
				.filter(e->e.getPaidBy().getId().equals(person.getId()))
				.map(Expense::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
	
	public BigDecimal getNetBalance(Long personId) {
		BigDecimal owedTo=getTotalOwedTo(personId);
		BigDecimal owedBy=getTotalOwedBy(personId);
		return owedTo.subtract(owedBy);
	}

}
