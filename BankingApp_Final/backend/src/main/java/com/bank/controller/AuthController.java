package com.bank.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bank.dto.ApiResponse;
import com.bank.dto.AuthLoginRequest;
import com.bank.dto.AuthRegisterRequest;
import com.bank.dto.ForgotPasswordRequest;
import com.bank.dto.LoginResponse;
import com.bank.dto.PinRequest;
import com.bank.dto.ResetPasswordRequest;
import com.bank.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody AuthRegisterRequest request) {
		Map<String, Object> data = authService.register(request);
		return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Registration successful", data));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody AuthLoginRequest request) {
		LoginResponse response = authService.login(request);
		return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Login successful", response));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		authService.forgotPassword(request);
		return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "If this email is registered, a reset code has been sent", Map.of("email", request.email)));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request);
		return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Password reset successful. Please login with your new password", Map.of("email", request.email)));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Map<String, String>>> logout() {
		authService.logout();
		return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Logout successful", Map.of("status", "ok")));
	}

	@GetMapping("/session")
	public ResponseEntity<ApiResponse<Map<String, String>>> session(@RequestAttribute("authenticatedEmail") String authenticatedEmail) {
		String email = authService.getSessionEmail(authenticatedEmail);
		return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "Session active", Map.of("email", email)));
	}

	@PostMapping("/set-pin")
	public ResponseEntity<ApiResponse<Map<String, String>>> setPin(@RequestAttribute("authenticatedEmail") String authenticatedEmail,
			@Valid @RequestBody PinRequest request) {
		authService.setPin(authenticatedEmail, request.pin);
		return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "PIN saved successfully", Map.of("pinConfigured", "true")));
	}

	@PostMapping("/verify-pin")
	public ResponseEntity<ApiResponse<Map<String, String>>> verifyPin(@RequestAttribute("authenticatedEmail") String authenticatedEmail,
			@Valid @RequestBody PinRequest request) {
		authService.verifyPin(authenticatedEmail, request.pin);
		return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "PIN verification successful", Map.of("verified", "true")));
	}
}