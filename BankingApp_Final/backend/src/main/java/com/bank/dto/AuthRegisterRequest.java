package com.bank.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRegisterRequest {

	@NotBlank
	public String name;

	@NotBlank
	public String username;

	@NotBlank
	@Email
	public String email;

	@NotBlank
	@Size(min = 4)
	public String password;

	public String pin;

	@DecimalMin("0.0")
	public BigDecimal openingBalance = BigDecimal.ZERO;
}