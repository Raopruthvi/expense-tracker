package com.expensetracker2.expense_tracker2.service;

import com.expensetracker2.expense_tracker2.exception.ResourceNotFoundException;
import com.expensetracker2.expense_tracker2.repository.FinancialProfileRepository;
import com.expensetracker2.expense_tracker2.repository.MonthlyBudgetRepository;
import com.expensetracker2.expense_tracker2.model.FinancialProfile;
import com.expensetracker2.expense_tracker2.model.MonthlyBudget;
import com.expensetracker2.expense_tracker2.model.Category;
import java.math.BigDecimal;
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
	private final FinancialProfileRepository financialProfileRepository;
	private final MonthlyBudgetRepository monthlyBudgetRepository;

	public ExpenseService(ExpenseRepository expenseRepository,
	                      PersonRepository personRepository,
	                      FinancialProfileRepository financialProfileRepository,
	                      MonthlyBudgetRepository monthlyBudgetRepository) {
	    this.expenseRepository = expenseRepository;
	    this.personRepository = personRepository;
	    this.financialProfileRepository = financialProfileRepository;
	    this.monthlyBudgetRepository = monthlyBudgetRepository;
	}
	
	//---------Person methods------------
	
	public Person savePerson(Person person) {
		return personRepository.save(person);
	}
	
	public List<Person> getAllPeople(){
		return personRepository.findAll();	} 
	
	public Person getPersonById(Long id) {
		return personRepository.findById(id) 
				.orElseThrow(()->new ResourceNotFoundException("Person not found with Id: "+id));
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
				.orElseThrow(()-> new ResourceNotFoundException("Expense not found with id: "+id));
	}
	
	public void deleteExpense(Long id) {
	    Expense expense = getExpenseById(id);

	    if (!expense.isSettled()) {
	        try {
	            FinancialProfile profile = financialProfileRepository.findAll()
	                .stream().findFirst().orElse(null);

	            int currentMonth = java.time.LocalDate.now().getMonthValue();
	            int currentYear = java.time.LocalDate.now().getYear();
	            MonthlyBudget budget = monthlyBudgetRepository
	                .findByMonthAndYearAndClosedFalse(currentMonth, currentYear)
	                .orElse(null);

	            if (profile != null && budget != null) {
	                BigDecimal amount = expense.getAmount();
	                Category category = expense.getCategory();

	                profile.setAccountBalance(profile.getAccountBalance().add(amount));

	                if (category == Category.VEHICLE) {
	                    BigDecimal newVehicle = budget.getRemainingVehicleAllowance().add(amount);
	                    if (newVehicle.compareTo(budget.getVehicleAllowance()) > 0) {
	                        BigDecimal overflow = newVehicle.subtract(budget.getVehicleAllowance());
	                        budget.setRemainingVehicleAllowance(budget.getVehicleAllowance());
	                        profile.setRemainingSalary(profile.getRemainingSalary().add(overflow));
	                    } else {
	                        budget.setRemainingVehicleAllowance(newVehicle);
	                    }
	                } else if (category == Category.FOOD ||
	                           category == Category.SPORTS ||
	                           category == Category.MISCELLANEOUS ||
	                           category == Category.ONLINE_SHOPPING ||
	                           category == Category.SUBSCRIPTIONS ||
	                           category == Category.TRIP) {
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

	                financialProfileRepository.save(profile);
	                monthlyBudgetRepository.save(budget);
	            }
	        } catch (Exception e) {
	            // No profile or budget — just delete
	        }
	    }

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
