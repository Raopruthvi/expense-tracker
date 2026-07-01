package com.expensetracker2.expense_tracker2.exception;

import java.time.LocalDateTime;

public class ErrorResponse {
	private int status;
	private String message;
	private LocalDateTime timestamp;
	 
	public ErrorResponse(int status, String message) {
		this.status=status;
		this.message=message;
		this.timestamp=LocalDateTime.now();
	}

	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}
	
	

}
