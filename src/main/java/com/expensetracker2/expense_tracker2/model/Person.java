package com.expensetracker2.expense_tracker2.model;
import jakarta.persistence.*; //how java should talk to relational databases.
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


  @Entity         //This class represents a table                              // You write @Entity (Jakarta annotation)
  public class Person {                                                        //        ↓
	                                                                           //Hibernate reads it and generates SQL
	@Id   //This field is primary key                                          //        ↓
                                                                               //SQL talks to your actual database (H2 right now)
	@GeneratedValue(strategy=GenerationType.IDENTITY) //let the database auto-genetate it                      
	private Long id;
	
	
	@NotBlank(message="Name cannot be blank")
	private String name;
	
	@NotBlank(message="Email cannot be blank")
	@Email(message="Email must be valid")
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
