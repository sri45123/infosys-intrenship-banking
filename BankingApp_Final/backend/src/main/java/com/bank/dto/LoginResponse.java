package com.bank.dto;

public class LoginResponse {

	public String token;
	public String name;
	public String username;
	public String email;
	public boolean pinConfigured;

	public LoginResponse() {
	}

	public LoginResponse(String token, String name, String username, String email, boolean pinConfigured) {
		this.token = token;
		this.name = name;
		this.username = username;
		this.email = email;
		this.pinConfigured = pinConfigured;
	}
}