package com.expensetracker2.expense_tracker2.model;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class FinancialProfile {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	private BigDecimal accountBalance;
	private BigDecimal salaryAmount;
	private BigDecimal savingsAmount;
	private BigDecimal cashBalance;
	private BigDecimal remainingSalary;
	
	public FinancialProfile() {
		
	}
	
	public FinancialProfile(BigDecimal accountBalance, BigDecimal cashBalance, BigDecimal  salaryAmount, BigDecimal initialSavings) {
		this.accountBalance=accountBalance;
		this.cashBalance=cashBalance;
		this.salaryAmount=salaryAmount;
		this.remainingSalary=salaryAmount;
		this.savingsAmount=initialSavings != null ? initialSavings : BigDecimal.ZERO;
	}

	public Long getId() {
		return id;
	}


	public BigDecimal getAccountBalance() {
		return accountBalance;
	}

	public void setAccountBalance(BigDecimal accountBalance) {
		this.accountBalance = accountBalance;
	}

	public BigDecimal getSalaryAmount() {
		return salaryAmount;
	}

	public void setSalaryAmount(BigDecimal salaryAmount) {
		this.salaryAmount = salaryAmount;
	}

	public BigDecimal getSavingsAmount() {
		return savingsAmount;
	}

	public void setSavingsAmount(BigDecimal savingsAmount) {
		this.savingsAmount = savingsAmount;
	}

	public BigDecimal getCashBalance() {
		return cashBalance;
	}

	public void setCashBalance(BigDecimal cashBalance) {
		this.cashBalance = cashBalance;
	}

	public BigDecimal getRemainingSalary() {
		return remainingSalary;
	}

	public void setRemainingSalary(BigDecimal remainingSalary) {
		this.remainingSalary = remainingSalary;
	}
	
	
	
	

}
