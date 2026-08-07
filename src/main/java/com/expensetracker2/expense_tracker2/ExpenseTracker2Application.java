package com.expensetracker2.expense_tracker2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication //its three annotations combined into one(@Configuration, @EnableAutoConfiguration, @ComponentScan)
public class ExpenseTracker2Application {

	public static void main(String[] args) {
		SpringApplication.run(ExpenseTracker2Application.class, args);
	}

}
