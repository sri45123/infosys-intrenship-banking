package com.bank.dto;

public class ApiResponse<T> {
	public String status;
	public String message;
	public T data;

	public ApiResponse() {
	}

	public ApiResponse(String status, String message, T data) {
		this.status = status;
		this.message = message;
		this.data = data;
	}
}