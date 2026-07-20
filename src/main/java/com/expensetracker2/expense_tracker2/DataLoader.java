package com.expensetracker2.expense_tracker2;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.expensetracker2.expense_tracker2.repository.*;

@Component
public class DataLoader implements CommandLineRunner {

    private final PersonRepository personRepository;

    public DataLoader(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // No sample data — fresh start
        System.out.println("=== Expense Tracker Started ===");
    }
}