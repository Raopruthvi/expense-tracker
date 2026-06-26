package com.expensetracker2.expense_tracker2.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.expensetracker2.expense_tracker2.model.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

}
  //JpaRepository auto-generates the implementation(save(), findById(), findAll(), deleteById()) for you at runtime