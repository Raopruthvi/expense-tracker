package com.expensetracker2.expense_tracker2.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;

import java.math.BigDecimal;
import java.time.Month;



@Entity
public class MonthlyBudget {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "budget_month")
	private int month;
	
	@Column(name = "budget_year")
	private int year;
	
	private BigDecimal totalAllowance;
	private BigDecimal remainingAllowance;
	private BigDecimal vehicleAllowance;
	private BigDecimal remainingVehicleAllowance;
	
	private boolean closed;
	
	public MonthlyBudget() {
		
	}
	
	public MonthlyBudget(int month, int year, BigDecimal totalAllowance, BigDecimal vehicleAllowance) {
		
		this.month=month;
		this.year=year;
		this.totalAllowance=totalAllowance;
		this.remainingAllowance=totalAllowance;
		this.vehicleAllowance=vehicleAllowance;
		this.remainingVehicleAllowance=vehicleAllowance;
		this.closed=false;
		
	}

	public Long getId() {
		return id;
	}

	public int getMonth() {
		return month;
	}

	public int getYear() {
		return year;
	}

	public BigDecimal getTotalAllowance() {
		return totalAllowance;
	}

	public void setTotalAllowance(BigDecimal totalAllowance) {
		this.totalAllowance = totalAllowance;
	}

	public BigDecimal getRemainingAllowance() {
		return remainingAllowance;
	}

	public void setRemainingAllowance(BigDecimal remainingAllowance) {
		this.remainingAllowance = remainingAllowance;
	}

	public BigDecimal getVehicleAllowance() {
		return vehicleAllowance;
	}

	public BigDecimal getRemainingVehicleAllowance() {
		return remainingVehicleAllowance;
	}

	public void setRemainingVehicleAllowance(BigDecimal remainingVehicleAllowance) {
		this.remainingVehicleAllowance = remainingVehicleAllowance;
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}
	
	

}
