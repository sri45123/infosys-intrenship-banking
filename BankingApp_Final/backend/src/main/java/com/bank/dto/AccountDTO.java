package com.bank.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AccountDTO {

	@NotBlank
	public String name;

	@NotBlank
	@Email
	public String email;

	@NotNull
	@DecimalMin("0.0")
	public BigDecimal balance;
}