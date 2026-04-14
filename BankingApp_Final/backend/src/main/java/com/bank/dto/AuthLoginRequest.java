package com.bank.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthLoginRequest {

	public String username;

	public String email;

	@NotBlank
	public String password;
}