package com.expensetracker2.expense_tracker2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.expensetracker2.expense_tracker2.model.FinancialProfile;

public interface FinancialProfileRepository extends JpaRepository<FinancialProfile, Long>{

}
