package com.expensetracker2.expense_tracker2.model;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
public class FinancialProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal accountBalance;
    private BigDecimal cashBalance;
    private BigDecimal salaryAmount;
    private BigDecimal remainingSalary;
    private BigDecimal savingsAmount;

    public FinancialProfile() {}

    public FinancialProfile(BigDecimal accountBalance, BigDecimal cashBalance,
                            BigDecimal salaryAmount, BigDecimal initialSavings) {
        this.accountBalance = accountBalance;
        this.cashBalance = cashBalance;
        this.salaryAmount = salaryAmount;
        this.remainingSalary = salaryAmount;
        this.savingsAmount = initialSavings != null ? initialSavings : BigDecimal.ZERO;
    }

    public Long getId() { return id; }

    public BigDecimal getAccountBalance() { return accountBalance; }
    public void setAccountBalance(BigDecimal v) { this.accountBalance = v; }

    public BigDecimal getCashBalance() { return cashBalance; }
    public void setCashBalance(BigDecimal v) { this.cashBalance = v; }

    public BigDecimal getSalaryAmount() { return salaryAmount; }
    public void setSalaryAmount(BigDecimal v) { this.salaryAmount = v; }

    public BigDecimal getRemainingSalary() { return remainingSalary; }
    public void setRemainingSalary(BigDecimal v) { this.remainingSalary = v; }

    public BigDecimal getSavingsAmount() { return savingsAmount; }
    public void setSavingsAmount(BigDecimal v) { this.savingsAmount = v; }
}