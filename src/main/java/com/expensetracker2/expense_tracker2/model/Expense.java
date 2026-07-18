package com.expensetracker2.expense_tracker2.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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

    public Expense() {}

    public Long getId() { return id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getSpendingDate() { return spendingDate; }
    public void setSpendingDate(LocalDate spendingDate) { this.spendingDate = spendingDate; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public boolean isSettled() { return settled; }
    public void setSettled(boolean settled) { this.settled = settled; }

    public String getPaidByName() { return paidByName; }
    public void setPaidByName(String paidByName) { this.paidByName = paidByName; }

    public String getOwedByName() { return owedByName; }
    public void setOwedByName(String owedByName) { this.owedByName = owedByName; }

    public Person getPaidBy() { return paidBy; }
    public void setPaidBy(Person paidBy) { this.paidBy = paidBy; }

    public Person getOwedBy() { return owedBy; }
    public void setOwedBy(Person owedBy) { this.owedBy = owedBy; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
}