package com.bank.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class TransactionLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String type;

	@Column(name = "account_number", nullable = false, length = 20)
	private String accountNumber;

	@Column(nullable = false)
	private double amount;

	@Column(name = "target_account", length = 20)
	private String targetAccount;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	public TransactionLog() {
	}

	public TransactionLog(String type, String accountNumber, double amount, String targetAccount) {
		this.type = type;
		this.accountNumber = accountNumber;
		this.amount = amount;
		this.targetAccount = targetAccount;
	}

	public Long getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public double getAmount() {
		return amount;
	}

	public String getTargetAccount() {
		return targetAccount;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}