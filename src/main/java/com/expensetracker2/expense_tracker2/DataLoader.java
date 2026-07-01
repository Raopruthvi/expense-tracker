package com.expensetracker2.expense_tracker2;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.expensetracker2.expense_tracker2.model.*;
import com.expensetracker2.expense_tracker2.repository.*;
import com.expensetracker2.expense_tracker2.model.Category;


@Component
public class DataLoader implements CommandLineRunner{
	private final PersonRepository personRepository;
	private final ExpenseRepository expenseRepository;
	
	public DataLoader( PersonRepository personRepository, ExpenseRepository expenseRepository) {
		this.expenseRepository=expenseRepository;
		this.personRepository=personRepository;
	}
	
	@Override
	public void run(String... args) throws Exception {

        Person alice = new Person("Alice", "alice@email.com");
        Person bob = new Person("Bob", "bob@email.com");

        personRepository.save(alice);
        personRepository.save(bob);

        Expense e1 = new Expense("Dinner", new BigDecimal("450.00"), LocalDate.now().plusDays(7), alice, bob,Category.FOOD);

        Expense e2 = new Expense("Cab fare", new BigDecimal("120.00"), LocalDate.now().plusDays(3), bob, alice,Category.MISCELLANEOUS);

        expenseRepository.save(e1);
        expenseRepository.save(e2);

        System.out.println("=== DATA LOADED ===");
        System.out.println("People saved: " + personRepository.count());
        System.out.println("Expenses saved: " + expenseRepository.count());
    }
}


