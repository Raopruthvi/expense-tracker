package com.expensetracker2.expense_tracker2.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.expensetracker2.expense_tracker2.repository.ExpenseRepository;
import com.expensetracker2.expense_tracker2.repository.FinancialProfileRepository;
import com.expensetracker2.expense_tracker2.repository.MonthlyBudgetRepository;
import com.expensetracker2.expense_tracker2.exception.ResourceNotFoundException;
import com.expensetracker2.expense_tracker2.model.*;

@Service
public class BudgetService {
	private final FinancialProfileRepository profileRepository;
	private final MonthlyBudgetRepository budgetRepository;
	private final ExpenseRepository expenseRepository;

	
	
	public BudgetService(FinancialProfileRepository profileRepository, MonthlyBudgetRepository budgetRepository, ExpenseRepository expenseRepository) {
		this.profileRepository=profileRepository;
		this.budgetRepository=budgetRepository;
		this.expenseRepository=expenseRepository;	
	}
	
	//------Financial Profile--------
	
	public FinancialProfile setupProfile(BigDecimal accountBalance, BigDecimal cashBalance, BigDecimal salaryAmount ) {
		if(!profileRepository.findAll().isEmpty()) {
			throw new IllegalArgumentException(
					"Profile already exists. Use update endpoints to modify it.");
		}
			FinancialProfile profile=new FinancialProfile(
					accountBalance, cashBalance, salaryAmount);
			return profileRepository.save(profile);
		
	}
	
	public FinancialProfile getProfile() {
		return profileRepository.findAll()
				.stream()
				.findFirst()
				.orElseThrow(()->new ResourceNotFoundException("No financial profile found. Please set up your profile first."));
	}
	
	public FinancialProfile updateSalary(BigDecimal newSalary) {
		FinancialProfile profile=getProfile();
		profile.setSalaryAmount(newSalary);
		profile.setRemainingSalary(newSalary);
		return profileRepository.save(profile);
	}

	
	//-------Monthly Budget----------
	
	public MonthlyBudget createMonthlyBudget(BigDecimal totalAllowance, BigDecimal vehicleAllowance) {
		int currentMonth=LocalDate.now().getMonthValue();
		int currentYear=LocalDate.now().getYear();
		
		budgetRepository.findByMonthAndYearAndClosedFalse(currentMonth, currentYear)
		.ifPresent(b->{
			throw new IllegalArgumentException(
					"A budget for this month already exists.");
		});
		MonthlyBudget budget=new MonthlyBudget(currentMonth, currentYear, totalAllowance, vehicleAllowance);
		return budgetRepository.save(budget);
	}
	
	public MonthlyBudget getCurrentBudget() {
		int currentMonth=LocalDate.now().getMonthValue();
		int currentYear=LocalDate.now().getYear();
		return budgetRepository.findByMonthAndYearAndClosedFalse(currentMonth, currentYear)
				.orElseThrow(()->new ResourceNotFoundException(
						"No active budget found for this month"));
	}
	
	//------Record an expense with budget deduction--------
	
	public ExpenseResult recordExpense(Expense expense) {
		FinancialProfile profile=getProfile();
		MonthlyBudget budget=getCurrentBudget();
		String warning=null;
		
		Category category=expense.getCategory();
		
		if(category==Category.VEHICLE) {
			BigDecimal remaining=budget.getRemainingVehicleAllowance();
			if(remaining.compareTo(expense.getAmount())>=0) {
				budget.setRemainingVehicleAllowance(
						remaining.subtract(expense.getAmount()));
			} else {
				BigDecimal overflow=expense.getAmount().subtract(remaining);
				budget.setRemainingVehicleAllowance(BigDecimal.ZERO);
				profile.setRemainingSalary(profile.getRemainingSalary().subtract(overflow));
				warning="Vehicle allowance exhausted. ₹" + overflow +" deducted from salary. Remaining salary: ₹"+profile.getRemainingSalary();
			}
		} else if(category==Category.FOOD ||
				category==Category.SPORTS ||
				category==Category.MISCELLANEOUS ||
				category==Category.ONLINE_SHOPPING ||
				category==Category.SUBSCRIPTIONS ||
				category==Category.TRIP) {
			BigDecimal remaining=budget.getRemainingAllowance();
			if(remaining.compareTo(expense.getAmount())>=0) {
				budget.setRemainingAllowance(remaining.subtract(expense.getAmount()));
			} else {
				BigDecimal overflow=expense.getAmount().subtract(remaining);
				budget.setRemainingAllowance(BigDecimal.ZERO);
				profile.setRemainingSalary(profile.getRemainingSalary().subtract(overflow));
				warning="Monthly allowance exhausted. ₹"+overflow+" deducted from salary. Remaining salary:  ₹"+profile.getRemainingSalary();
			}
		}
		
		//Always deduct from Account Balance regardless of category
		profile.setAccountBalance(
				profile.getAccountBalance().subtract(expense.getAmount()));
		profileRepository.save(profile);
		budgetRepository.save(budget);
		Expense saved=expenseRepository.save(expense);
		
		return new ExpenseResult(saved,warning,profile.getRemainingSalary());
			
		
	}
	
	//---------End of month-------------
	
	public EndOfMonthResult closeMonth() {
		FinancialProfile profile=getProfile();
		MonthlyBudget budget=getCurrentBudget();
		
		BigDecimal allowanceSavings=budget.getRemainingAllowance();
		BigDecimal vehicleSavings=budget.getRemainingVehicleAllowance();
		BigDecimal salarySavings=profile.getRemainingSalary();
		
		BigDecimal totalAddedToSavings=allowanceSavings.add(vehicleSavings).add(salarySavings);
		
		profile.setSavingsAmount(profile.getSavingsAmount().add(totalAddedToSavings));
		profile.setRemainingSalary(profile.getSalaryAmount());
		
		budget.setClosed(true);
		
		profileRepository.save(profile);
		budgetRepository.save(budget);
		
		return new EndOfMonthResult(allowanceSavings, vehicleSavings, salarySavings, totalAddedToSavings, profile.getSavingsAmount());
	}
	
	//-----------Summary------------------
	public FinancialSummary getSummary() {
		FinancialProfile profile=getProfile();
		MonthlyBudget budget=getCurrentBudget();
		return new FinancialSummary(profile,budget);
	}
	
	//----Inner Result Classes--------
	
	public static class ExpenseResult{
		public final Expense expense;
		public final String warning;
		public final BigDecimal remainingSalary;
		
		public ExpenseResult(Expense expense, String warning, BigDecimal remainingSalary) {
			this.expense=expense;
			this.warning=warning;
			this.remainingSalary=remainingSalary;
		}
	}
	
	public static class EndOfMonthResult{
		public final BigDecimal allowanceSaved;
		public final BigDecimal vehicleSaved;
		public final BigDecimal salarySaved;
		public final BigDecimal totalAddedToSavings;
		public final BigDecimal newTotalSavings;
		
		public EndOfMonthResult(BigDecimal allowanceSaved, BigDecimal vehicleSaved,BigDecimal salarySaved,  BigDecimal totalAddedToSavings, BigDecimal newTotalSavings) {
			this.allowanceSaved=allowanceSaved;
			this.vehicleSaved=vehicleSaved;
			this.salarySaved=salarySaved;
			this.totalAddedToSavings=totalAddedToSavings;
			this.newTotalSavings=newTotalSavings;
		}
	}
	
	
	public static class FinancialSummary{
		public final BigDecimal accountBalance;
		public final BigDecimal cashBalance;
		public final BigDecimal salaryAmount;
		public final BigDecimal remainingSalary;
		public final BigDecimal savingsAmount;
		public final BigDecimal remainingAllowance;
		public final BigDecimal remainingVehicleAllowance;
		
		public FinancialSummary(FinancialProfile profile, MonthlyBudget budget) {
			this.accountBalance=profile.getAccountBalance();
			this.cashBalance=profile.getCashBalance();
			this.salaryAmount=profile.getSalaryAmount();
			this.remainingSalary=profile.getRemainingSalary();
			this.savingsAmount=profile.getSavingsAmount();
			this.remainingAllowance=budget.getRemainingAllowance();
			this.remainingVehicleAllowance=budget.getRemainingVehicleAllowance();
		}
	}
	
}
