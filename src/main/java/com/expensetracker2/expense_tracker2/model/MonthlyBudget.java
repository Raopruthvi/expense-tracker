package com.expensetracker2.expense_tracker2.model;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
public class MonthlyBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public MonthlyBudget() {}

    public MonthlyBudget(int month, int year,
                         BigDecimal totalAllowance, BigDecimal vehicleAllowance) {
        this.month = month;
        this.year = year;
        this.totalAllowance = totalAllowance;
        this.remainingAllowance = totalAllowance;
        this.vehicleAllowance = vehicleAllowance;
        this.remainingVehicleAllowance = vehicleAllowance;
        this.closed = false;
    }

    public Long getId() { return id; }
    public int getMonth() { return month; }
    public int getYear() { return year; }

    public BigDecimal getTotalAllowance() { return totalAllowance; }
    public void setTotalAllowance(BigDecimal v) { this.totalAllowance = v; }

    public BigDecimal getRemainingAllowance() { return remainingAllowance; }
    public void setRemainingAllowance(BigDecimal v) { this.remainingAllowance = v; }

    public BigDecimal getVehicleAllowance() { return vehicleAllowance; }
    public void setVehicleAllowance(BigDecimal v) { this.vehicleAllowance = v; }

    public BigDecimal getRemainingVehicleAllowance() { return remainingVehicleAllowance; }
    public void setRemainingVehicleAllowance(BigDecimal v) { this.remainingVehicleAllowance = v; }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean v) { this.closed = v; }
}