package com.expensetracker2.expense_tracker2.service;

import com.expensetracker2.expense_tracker2.exception.ResourceNotFoundException;
import com.expensetracker2.expense_tracker2.model.*;
import com.expensetracker2.expense_tracker2.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final PersonRepository personRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          PersonRepository personRepository) {
        this.expenseRepository = expenseRepository;
        this.personRepository = personRepository;
    }

    public Person savePerson(Person person) {
        return personRepository.save(person);
    }

    public List<Person> getAllPeople() {
        return personRepository.findAll();
    }

    public Person getPersonById(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Person not found: " + id));
    }

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    public Expense getExpenseById(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Expense not found: " + id));
    }

    public List<Expense> getUnsettledExpenses() {
        return expenseRepository.findAll().stream()
                .filter(e -> !e.isSettled()).toList();
    }

    public List<Expense> getOverdueExpenses() {
        return expenseRepository.findAll().stream()
                .filter(e -> !e.isSettled()
                        && e.getDeadline() != null
                        && e.getDeadline().isBefore(LocalDate.now()))
                .toList();
    }

    public BigDecimal getTotalOwedBy(Long personId) {
        Person p = getPersonById(personId);
        return expenseRepository.findAll().stream()
                .filter(e -> !e.isSettled())
                .filter(e -> e.getOwedBy() != null &&
                             e.getOwedBy().getId().equals(p.getId()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalOwedTo(Long personId) {
        Person p = getPersonById(personId);
        return expenseRepository.findAll().stream()
                .filter(e -> !e.isSettled())
                .filter(e -> e.getPaidBy() != null &&
                             e.getPaidBy().getId().equals(p.getId()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getNetBalance(Long personId) {
        return getTotalOwedTo(personId).subtract(getTotalOwedBy(personId));
    }
}