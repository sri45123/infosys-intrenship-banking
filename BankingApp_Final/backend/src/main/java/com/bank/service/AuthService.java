package com.bank.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.dto.AuthLoginRequest;
import com.bank.dto.AuthRegisterRequest;
import com.bank.dto.ForgotPasswordRequest;
import com.bank.dto.LoginResponse;
import com.bank.dto.ResetPasswordRequest;
import com.bank.model.Account;
import com.bank.model.AuthUser;
import com.bank.repository.AuthUserJpaRepository;
import com.bank.security.JwtService;
import com.bank.util.EmailUtil;

@Service
public class AuthService {

	private static final long RESET_CODE_TTL_MILLIS = 10 * 60 * 1000L;
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Map<String, PasswordResetCode> PASSWORD_RESET_CODES = new ConcurrentHashMap<>();

	private final AuthUserJpaRepository authRepo;
	private final AccountService accountService;
	private final JwtService jwtService;

	public AuthService(AuthUserJpaRepository authRepo, AccountService accountService, JwtService jwtService) {
		this.authRepo = authRepo;
		this.accountService = accountService;
		this.jwtService = jwtService;
	}

	@Transactional
	public Map<String, Object> register(AuthRegisterRequest data) {
		String email = data.email.trim().toLowerCase();
		String username = data.username.trim().toLowerCase();

		if (authRepo.existsByEmailIgnoreCase(email)) {
			throw new IllegalArgumentException("User already registered");
		}
		if (authRepo.existsByUsernameIgnoreCase(username)) {
			throw new IllegalArgumentException("Username already in use");
		}

		AuthUser user = new AuthUser(data.name.trim(), username, email, hashPassword(data.password), null);
		if (data.pin != null && !data.pin.isBlank()) {
			validatePin(data.pin);
			user.setPin(data.pin);
		}

		authRepo.save(user);
		Account createdAccount = accountService.getOrCreateAccountByEmail(user.getName(), user.getEmail(),
			data.openingBalance == null ? BigDecimal.ZERO : data.openingBalance);
		EmailUtil.sendEmail(
			user.getEmail(),
			"Welcome to NovaBank - Account Created",
			"Hi " + user.getName() + ",\n\nYour account registration is successful.\n"
				+ "Account Number: " + createdAccount.getAccountNumber() + "\n"
				+ "You can use this account number for deposit, withdraw and transfer transactions.\n\n"
				+ "Thanks,\nNovaBank"
		);

		return Map.of(
			"accountNumber", createdAccount.getAccountNumber(),
			"email", user.getEmail(),
			"name", user.getName()
		);
	}

	@Transactional(readOnly = true)
	public LoginResponse login(AuthLoginRequest data) {
		AuthUser user = null;
		if (data.username != null && !data.username.isBlank()) {
			user = authRepo.findByUsernameIgnoreCase(data.username.trim()).orElse(null);
		}
		if (user == null && data.email != null && !data.email.isBlank()) {
			user = authRepo.findByEmailIgnoreCase(data.email.trim().toLowerCase()).orElse(null);
		}
		if (user == null || !user.getPasswordHash().equals(hashPassword(data.password))) {
			throw new IllegalArgumentException("Invalid username/email or password");
		}

		String token = jwtService.generateToken(user.getEmail());
		return new LoginResponse(token, user.getName(), user.getUsername(), user.getEmail(), user.getPin() != null && !user.getPin().isBlank());
	}

	@Transactional(readOnly = true)
	public String getSessionEmail(String authenticatedEmail) {
		if (authenticatedEmail == null || authenticatedEmail.isBlank()) {
			throw new IllegalArgumentException("Invalid or expired session");
		}
		return authenticatedEmail;
	}

	@Transactional
	public void setPin(String authenticatedEmail, String pin) {
		validatePin(pin);
		AuthUser user = authRepo.findByEmailIgnoreCase(authenticatedEmail).orElseThrow(() -> new IllegalArgumentException("Invalid session"));
		user.setPin(pin);
		authRepo.save(user);
	}

	@Transactional(readOnly = true)
	public void verifyPin(String authenticatedEmail, String pin) {
		AuthUser user = authRepo.findByEmailIgnoreCase(authenticatedEmail).orElseThrow(() -> new IllegalArgumentException("Invalid session"));
		if (user.getPin() == null || user.getPin().isBlank() || !user.getPin().equals(pin)) {
			throw new IllegalArgumentException("Invalid PIN");
		}
	}

	public void logout() {
		// Stateless JWT logout is handled client-side by dropping the token.
	}

	public void forgotPassword(ForgotPasswordRequest data) {
		String email = data.email.trim().toLowerCase();
		AuthUser user = authRepo.findByEmailIgnoreCase(email).orElse(null);
		if (user != null) {
			String code = String.format("%06d", RANDOM.nextInt(1_000_000));
			PASSWORD_RESET_CODES.put(email, new PasswordResetCode(code, System.currentTimeMillis() + RESET_CODE_TTL_MILLIS));
			EmailUtil.sendEmail(
				email,
				"NovaBank Password Reset Code",
				"Hi " + user.getName() + ",\n\nUse this reset code to set a new password: " + code + "\n"
					+ "This code expires in 10 minutes.\n\nIf you did not request this, please ignore this email.\n\nThanks,\nNovaBank"
			);
		}
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest data) {
		String email = data.email.trim().toLowerCase();
		PasswordResetCode storedCode = PASSWORD_RESET_CODES.get(email);
		if (storedCode == null || storedCode.expiresAtMillis < System.currentTimeMillis() || !storedCode.code.equals(data.code)) {
			throw new IllegalArgumentException("Invalid or expired reset code");
		}

		AuthUser user = authRepo.findByEmailIgnoreCase(email).orElseThrow(() -> new IllegalArgumentException("Invalid reset request"));
		user.setPasswordHash(hashPassword(data.newPassword));
		authRepo.save(user);
		PASSWORD_RESET_CODES.remove(email);
	}

	private void validatePin(String pin) {
		if (pin == null || !pin.matches("\\d{4,6}")) {
			throw new IllegalArgumentException("PIN must be 4 to 6 digits");
		}
	}

	private String hashPassword(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception e) {
			throw new IllegalStateException("Unable to hash password", e);
		}
	}

	private static final class PasswordResetCode {
		private final String code;
		private final long expiresAtMillis;

		private PasswordResetCode(String code, long expiresAtMillis) {
			this.code = code;
			this.expiresAtMillis = expiresAtMillis;
		}
	}
}