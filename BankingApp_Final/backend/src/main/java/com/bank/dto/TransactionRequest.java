package com.bank.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TransactionRequest {

	@NotBlank
	public String accNo;

	@NotNull
	@DecimalMin("0.01")
	public BigDecimal amount;
}