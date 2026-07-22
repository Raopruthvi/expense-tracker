package com.expensetracker2.expense_tracker2.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Description cannot be blank")
    private String description;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Spending date cannot be null")
    private LocalDate spendingDate;

    private LocalDate deadline;
    private boolean settled;
    private boolean cash;

    private String paidByName;
    private String owedByName;

    @ManyToOne
    @JoinColumn(name = "paid_by_id")
    private Person paidBy;

    @ManyToOne
    @JoinColumn(name = "owed_by_id")
    private Person owedBy;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Category cannot be null")
    private Category category;
    
    private BigDecimal salaryDeducted = BigDecimal.ZERO;
    
    private boolean fromSavings;

   

    public Expense() {}

    public Long getId() { return id; }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }

    public LocalDate getSpendingDate() { return spendingDate; }
    public void setSpendingDate(LocalDate v) { this.spendingDate = v; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate v) { this.deadline = v; }

    public boolean isSettled() { return settled; }
    public void setSettled(boolean v) { this.settled = v; }

    public boolean isCash() { return cash; }
    public void setCash(boolean v) { this.cash = v; }

    public String getPaidByName() { return paidByName; }
    public void setPaidByName(String v) { this.paidByName = v; }

    public String getOwedByName() { return owedByName; }
    public void setOwedByName(String v) { this.owedByName = v; }

    public Person getPaidBy() { return paidBy; }
    public void setPaidBy(Person v) { this.paidBy = v; }

    public Person getOwedBy() { return owedBy; }
    public void setOwedBy(Person v) { this.owedBy = v; }

    public Category getCategory() { return category; }
    public void setCategory(Category v) { this.category = v; }
    
    public BigDecimal getSalaryDeducted() { return salaryDeducted; }
    public void setSalaryDeducted(BigDecimal v) { this.salaryDeducted = v; }
    
    public boolean isFromSavings() { return fromSavings; }
    public void setFromSavings(boolean v) { this.fromSavings = v; }
}