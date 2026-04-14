package com.bank.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account {

	@Id
	@Column(name = "account_number", length = 20)
	private String accountNumber;

	@Column(name = "holder_name", nullable = false)
	private String holderName;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "opening_balance", nullable = false, precision = 19, scale = 2)
	private BigDecimal openingBalance;

	public Account() {
	}
	
	public Account(String holderName, String email,BigDecimal openingBalance) {
		this.holderName = holderName;
		this.email = email;
		this.openingBalance = openingBalance == null ? BigDecimal.ZERO : openingBalance;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	public String getHolderName() {
		return holderName;
	}

	public void setHolderName(String holderName) {
		this.holderName = holderName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public BigDecimal getOpeningBalance() {
		return openingBalance;
	}

	public void credit(BigDecimal balance){
		this.openingBalance = this.openingBalance.add(balance);
	}
	
	public void debit(BigDecimal balance){
		this.openingBalance = this.openingBalance.subtract(balance);
	}
	
	
	public String toString(){
		return accountNumber+" "+holderName+" "+email+" "+openingBalance;
	}
	
	

}
