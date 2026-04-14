package com.bank.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TransferRequest {

	@NotBlank
	public String fromAcc;

	@NotBlank
	public String toAcc;

	@NotNull
	@DecimalMin("0.01")
	public BigDecimal amount;
}