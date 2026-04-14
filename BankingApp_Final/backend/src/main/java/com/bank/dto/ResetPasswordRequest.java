package com.bank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

	@NotBlank
	@Email
	public String email;

	@NotBlank
	@Size(min = 6, max = 6)
	public String code;

	@NotBlank
	@Size(min = 4)
	public String newPassword;
}