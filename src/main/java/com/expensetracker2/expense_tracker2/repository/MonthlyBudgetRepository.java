package com.expensetracker2.expense_tracker2.repository;

import java.util.Optional;


import org.springframework.data.jpa.repository.JpaRepository;
import com.expensetracker2.expense_tracker2.model.MonthlyBudget;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {
	Optional<MonthlyBudget> findByMonthAndYear(int month, int year);
	
	Optional<MonthlyBudget> findByMonthAndYearAndClosedFalse(int month, int year);

}
