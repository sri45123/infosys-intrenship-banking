package com.bank.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AuthUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(length = 10)
	private String pin;

	public AuthUser() {
	}

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
    
	public void setName(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}
    
	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}
    
	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}
    
	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getPin() {
		return pin;
	}

	public void setPin(String pin) {
		this.pin = pin;
	}
}
