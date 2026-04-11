package com.bank.model;

public class AuthUser {

	private String name;
	private String username;
	private String email;
	private String passwordHash;
	private String pin;

	public AuthUser(String name, String username, String email, String passwordHash, String pin) {
		this.name = name;
		this.username = username;
		this.email = email;
		this.passwordHash = passwordHash;
		this.pin = pin;
	}

	public String getName() {
		return name;
	}

	public String getUsername() {
		return username;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getPin() {
		return pin;
	}

	public void setPin(String pin) {
		this.pin = pin;
	}
}
