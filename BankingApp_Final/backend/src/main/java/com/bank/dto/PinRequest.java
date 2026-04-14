package com.bank.dto;

import jakarta.validation.constraints.NotBlank;

public class PinRequest {

	@NotBlank
	public String pin;
}