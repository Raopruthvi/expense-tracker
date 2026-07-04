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
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long id;
	
	@NotBlank(message="Description cannot be null")
	private String description;
	
	@NotBlank(message="Amount cannot be null")
	@DecimalMin(value="0.01", message="amount must be greater than zero")
	private BigDecimal amount;
	
	@NotNull(message="Deadline cannot be null")
	private LocalDate deadline;
	private boolean settled;
	
	@ManyToOne
	@JoinColumn(name="paid_by_id") //Tells hibernate to name the columns as that
	private Person paidBy;
	
	@ManyToOne
	@JoinColumn(name="owed_by_id")
	private Person owedBy;
	
	@Enumerated(EnumType.STRING)
	@NotNull(message="Category cannot be null")
	private Category category;
	
	public Expense() {
		
	}
	
	public Expense(String description, BigDecimal amount, LocalDate deadline, Person paidBy, Person owedBy, Category category) {
		this.description=description;
		this.amount=amount;
		this.deadline=deadline;
		this.settled=false;
		this.paidBy=paidBy;
		this.owedBy=owedBy;
		this.category=category;
	}

	public long getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public LocalDate getDeadline() {
		return deadline;
	}

	public void setDeadline(LocalDate deadline) {
		this.deadline = deadline;
	}

	public boolean isSettled() {
		return settled;
	}

	public void setSettled(boolean settled) {
		this.settled = settled;
	}

	public Person getPaidBy() {
		return paidBy;
	}

	public void setPaidBy(Person paidBy) {
		this.paidBy = paidBy;
	}

	public Person getOwedBy() {
		return owedBy;
	}

	public void setOwedBy(Person owedBy) {
		this.owedBy = owedBy;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}
	
	
	
	
	
	

}
