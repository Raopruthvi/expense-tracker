package com.expensetracker2.expense_tracker2.model;
import jakarta.persistence.*; //how java should talk to relational databases.

  @Entity         //This class represents a table                              // You write @Entity (Jakarta annotation)
  public class Person {                                                        //        ↓
	                                                                           //Hibernate reads it and generates SQL
	@Id   //This field is primary key                                          //        ↓
                                                                               //SQL talks to your actual database (H2 right now)
	@GeneratedValue(strategy=GenerationType.IDENTITY) //let the database auto-genetate it                      
	private Long id;
	
	private String name;
	private String email;
	
	public Person() {
		
	}
	
	public Person(String name, String email) {
		this.name=name;
		this.email=email;
	}

	public Long getId() {
		return id;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	

}
