package com.bank;

import static spark.Spark.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

 
import com.bank.model.Account;
import com.bank.model.AuthUser;
import com.bank.repository.AccountRepository;
import com.bank.repository.AuthRepository;
import com.bank.repository.TransactionRepository;
import com.bank.service.AccountService;
import com.bank.service.AlertService;
import com.bank.service.TransactionService;
import com.bank.util.EmailUtil;
import com.bank.util.FileReportUtil;
import com.google.gson.Gson;

public class ApiServer {

	private static final Map<String, String> TOKENS = new ConcurrentHashMap<>();
	private static final Set<String> PROTECTED_PATHS = Collections.unmodifiableSet(new LinkedHashSet<>(List.of(
		"/auth/session",
		"/auth/logout",
		"/auth/set-pin",
		"/auth/verify-pin",
		"/accounts/create",
		"/transactions/deposite",
		"/transactions/deposit",
		"/tranctions/withdraw",
		"/transactions/withdraw",
		"/transactions/transfer",
		"/accounts/all",
		"/accounts/:accNo",
		"/transactions/history",
		"/transactions/history/:accNo",
		"/transactions/query-history",
		"/analytics/overview"
	)));
	private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
	private static final long RESET_CODE_TTL_MILLIS = 10 * 60 * 1000L;
	private static final Map<String, PasswordResetCode> PASSWORD_RESET_CODES = new ConcurrentHashMap<>();
	

	public static void main(String[] args) {
		
		port(8080);
		staticFiles.location("/public");
		enableCORS();
		Gson gson = new Gson();
		
		AccountRepository accRepo = new AccountRepository();
		AuthRepository authRepo = new AuthRepository();
		AccountService accService = new AccountService(accRepo);
		TransactionRepository trxRepo = new TransactionRepository();
		AlertService alertService = new AlertService(new BigDecimal("1000"));
		TransactionService trxService = new TransactionService(accService, trxRepo, alertService);

		exception(Exception.class, (e, req, res) -> {
			res.status(400);
			res.type("application/json");
			Map<String, String> error = new LinkedHashMap<>();
			error.put("status", "error");
			error.put("message", e.getMessage() == null ? "Request failed" : e.getMessage());
			res.body(gson.toJson(error));
		});

		before((req, res) -> {
			if (requiresAuth(req.pathInfo())) {
				String token = req.headers("Authorization");
				if (token == null || token.isBlank()) {
					halt(401, gson.toJson(errorResponse("Missing authorization token")));
				}
				if (token.startsWith("Bearer ")) {
					token = token.substring(7);
				}
				if (!TOKENS.containsKey(token)) {
					halt(401, gson.toJson(errorResponse("Invalid or expired session")));
				}
			}
		});

		post("/auth/register", (req, res) -> {
			res.type("application/json");
			AuthRegisterRequest data = gson.fromJson(req.body(), AuthRegisterRequest.class);

			if (data == null || isBlank(data.name) || isBlank(data.username) || isBlank(data.email) || isBlank(data.password)) {
				res.status(400);
				return gson.toJson(errorResponse("Name, username, email and password are required"));
			}

			String email = data.email.trim().toLowerCase();
			String username = data.username.trim().toLowerCase();
			if (authRepo.existsByEmail(email)) {
				res.status(400);
				return gson.toJson(errorResponse("User already registered"));
			}
			if (authRepo.existsByUsername(username)) {
				res.status(400);
				return gson.toJson(errorResponse("Username already in use"));
			}

			if (data.password.length() < 4) {
				res.status(400);
				return gson.toJson(errorResponse("Password must be at least 4 characters"));
			}

			AuthUser user = new AuthUser(data.name.trim(), username, email, hashPassword(data.password), null);
			if (!isBlank(data.pin)) {
				if (!data.pin.matches("\\d{4,6}")) {
					res.status(400);
					return gson.toJson(errorResponse("PIN must be 4 to 6 digits"));
				}
				user.setPin(data.pin);
			}

			authRepo.save(user);
			Account createdAccount = accService.getOrCreateAccountByEmail(user.getName(), user.getEmail(), data.openingBalance);
			EmailUtil.sendEmail(
				user.getEmail(),
				"Welcome to NovaBank - Account Created",
				"Hi " + user.getName() + ",\n\nYour account registration is successful.\n"
					+ "Account Number: " + createdAccount.getAccountNumber() + "\n"
					+ "You can use this account number for deposit, withdraw and transfer transactions.\n\n"
					+ "Thanks,\nNovaBank"
			);

			Map<String, String> payload = new LinkedHashMap<>();
			payload.put("status", "success");
			payload.put("message", "Registration successful");
			payload.put("accountNumber", createdAccount.getAccountNumber());
			return gson.toJson(payload);
		});

		post("/auth/login", (req, res) -> {
			res.type("application/json");
			AuthLoginRequest data = gson.fromJson(req.body(), AuthLoginRequest.class);

			if (data == null || (isBlank(data.email) && isBlank(data.username)) || isBlank(data.password)) {
				res.status(400);
				return gson.toJson(errorResponse("Username/email and password are required"));
			}

			String email = null;
			AuthUser user = null;
			if (!isBlank(data.username)) {
				user = authRepo.findByUsername(data.username.trim().toLowerCase());
			}
			if (user == null && !isBlank(data.email)) {
				user = authRepo.findByEmail(data.email.trim().toLowerCase());
			}

			if (user == null || !user.getPasswordHash().equals(hashPassword(data.password))) {
				res.status(401);
				return gson.toJson(errorResponse("Invalid username/email or password"));
			}
			email = user.getEmail();

			String token = UUID.randomUUID().toString();
			TOKENS.put(token, email);

			Map<String, String> payload = new LinkedHashMap<>();
			payload.put("status", "success");
			payload.put("message", "Login successful");
			payload.put("token", token);
			payload.put("name", user.getName());
			payload.put("username", user.getUsername());
			payload.put("email", user.getEmail());
			payload.put("pinConfigured", String.valueOf(!isBlank(user.getPin())));
			return gson.toJson(payload);
		});

		post("/auth/forgot-password", (req, res) -> {
			res.type("application/json");
			ForgotPasswordRequest data = gson.fromJson(req.body(), ForgotPasswordRequest.class);
			if (data == null || isBlank(data.email)) {
				res.status(400);
				return gson.toJson(errorResponse("Email is required"));
			}

			String email = data.email.trim().toLowerCase();
			AuthUser user = authRepo.findByEmail(email);

			if (user != null) {
				String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
				PASSWORD_RESET_CODES.put(email, new PasswordResetCode(code, System.currentTimeMillis() + RESET_CODE_TTL_MILLIS));
				EmailUtil.sendEmail(
					email,
					"NovaBank Password Reset Code",
					"Hi " + user.getName() + ",\n\nUse this reset code to set a new password: " + code + "\n"
						+ "This code expires in 10 minutes.\n\nIf you did not request this, please ignore this email.\n\nThanks,\nNovaBank"
				);
			}

			return gson.toJson(successResponse("If this email is registered, a reset code has been sent"));
		});

		post("/auth/reset-password", (req, res) -> {
			res.type("application/json");
			ResetPasswordRequest data = gson.fromJson(req.body(), ResetPasswordRequest.class);
			if (data == null || isBlank(data.email) || isBlank(data.code) || isBlank(data.newPassword)) {
				res.status(400);
				return gson.toJson(errorResponse("Email, reset code and new password are required"));
			}

			if (!data.code.matches("\\d{6}")) {
				res.status(400);
				return gson.toJson(errorResponse("Reset code must be 6 digits"));
			}

			if (data.newPassword.length() < 4) {
				res.status(400);
				return gson.toJson(errorResponse("Password must be at least 4 characters"));
			}

			String email = data.email.trim().toLowerCase();
			PasswordResetCode storedCode = PASSWORD_RESET_CODES.get(email);
			if (storedCode == null || storedCode.expiresAtMillis < System.currentTimeMillis() || !storedCode.code.equals(data.code)) {
				res.status(400);
				return gson.toJson(errorResponse("Invalid or expired reset code"));
			}

			AuthUser user = authRepo.findByEmail(email);
			if (user == null) {
				PASSWORD_RESET_CODES.remove(email);
				res.status(400);
				return gson.toJson(errorResponse("Invalid reset request"));
			}

			authRepo.updatePasswordHash(email, hashPassword(data.newPassword));
			PASSWORD_RESET_CODES.remove(email);
			TOKENS.entrySet().removeIf(entry -> email.equals(entry.getValue()));

			EmailUtil.sendEmail(
				email,
				"NovaBank Password Changed",
				"Hi " + user.getName() + ",\n\nYour password has been changed successfully.\n"
					+ "If this was not you, contact support immediately.\n\nThanks,\nNovaBank"
			);

			return gson.toJson(successResponse("Password reset successful. Please login with your new password"));
		});

		post("/auth/logout", (req, res) -> {
			res.type("application/json");
			String token = sanitizeToken(req.headers("Authorization"));
			TOKENS.remove(token);
			return gson.toJson(successResponse("Logout successful"));
		});

		get("/auth/session", (req, res) -> {
			res.type("application/json");
			String email = getUserEmailFromToken(req.headers("Authorization"));
			if (isBlank(email)) {
				res.status(401);
				return gson.toJson(errorResponse("Invalid or expired session"));
			}

			Map<String, String> payload = new LinkedHashMap<>();
			payload.put("status", "success");
			payload.put("email", email);
			return gson.toJson(payload);
		});

		post("/auth/set-pin", (req, res) -> {
			res.type("application/json");
			PinRequest data = gson.fromJson(req.body(), PinRequest.class);
			if (data == null || isBlank(data.pin) || !data.pin.matches("\\d{4,6}")) {
				res.status(400);
				return gson.toJson(errorResponse("PIN must be 4 to 6 digits"));
			}

			String email = getUserEmailFromToken(req.headers("Authorization"));
			AuthUser user = authRepo.findByEmail(email);
			if (user == null || isBlank(email)) {
				res.status(401);
				return gson.toJson(errorResponse("Invalid session"));
			}
			authRepo.updatePin(email, data.pin);
			return gson.toJson(successResponse("PIN saved successfully"));
		});

		post("/auth/verify-pin", (req, res) -> {
			res.type("application/json");
			PinRequest data = gson.fromJson(req.body(), PinRequest.class);
			if (data == null || isBlank(data.pin)) {
				res.status(400);
				return gson.toJson(errorResponse("PIN is required"));
			}

			String email = getUserEmailFromToken(req.headers("Authorization"));
			AuthUser user = authRepo.findByEmail(email);
			if (user == null || isBlank(user.getPin()) || !user.getPin().equals(data.pin)) {
				res.status(401);
				return gson.toJson(errorResponse("Invalid PIN"));
			}
			return gson.toJson(successResponse("PIN verification successful"));
		});
		
		post("/accounts/create",(req,res) -> {
			res.type("application/json");
			System.out.println("/accounts/create API Called.." );
			AccountRequest data = gson.fromJson(req.body(),AccountRequest.class);
			Account acc = accService.createAccount(data.name, data.email, data.balance);
			return gson.toJson(acc);
			
		});
		
		 post("/transactions/deposite", (req, res) -> {
	            System.out.println("/transactions/deposite api is called");

	            TxRequest data = gson.fromJson(req.body(), TxRequest.class);
	            trxService.deposite(data.accNo, data.amount);

	            return "Deposite successfully..!";
	        });

		post("/transactions/deposit", (req, res) -> {
			System.out.println("/transactions/deposit api is called");
			TxRequest data = gson.fromJson(req.body(), TxRequest.class);
			trxService.deposite(data.accNo, data.amount);
			return "Deposit successful..!";
		});
		
		post("/tranctions/withdraw",(req,res) ->{
			System.out.println("/tranctions/withdraw API Called.." );
			TxRequest data = gson.fromJson(req.body(), TxRequest.class);
			trxService.withdraw(data.accNo, data.amount);
			return "Withdraw Successfully..!";
		});

		post("/transactions/withdraw", (req, res) -> {
			System.out.println("/transactions/withdraw API Called..");
			TxRequest data = gson.fromJson(req.body(), TxRequest.class);
			trxService.withdraw(data.accNo, data.amount);
			return "Withdraw successful..!";
		});
		
		post("/transactions/transfer",(req,res) ->{
			System.out.println("/tranctions/transfer API Called.." );
			TransferRequest data = gson.fromJson(req.body(), TransferRequest.class);
			trxService.transfer(data.fromAcc, data.toAcc,data.amount);
			return "Transfer Transaction Successfull..!";
		});
		
		get("/accounts/all",(req,res) -> {
			System.out.println("/accounts/all API is called");
			res.type("application/json");
			
			return gson.toJson(accService.listAllAccounts());
		});
		
		get("/accounts/:accNo",(req,res) -> {
			String accNo = req.params("accNo");
			Account account = accService.getAccount(accNo);
			return gson.toJson(account);
		});

		get("/transactions/history", (req, res) -> {
			res.type("application/json");
			List<String> all = FileReportUtil.readAllLines();
			return gson.toJson(all);
		});

		get("/transactions/history/:accNo", (req, res) -> {
			res.type("application/json");
			String accNo = req.params("accNo");
			List<String> filtered = FileReportUtil.readAllLines()
				.stream()
				.filter(line -> line.contains("Acc: " + accNo) || line.contains("to " + accNo))
				.collect(Collectors.toList());
			return gson.toJson(filtered);
		});

		get("/transactions/query-history", (req, res) -> {
			res.type("application/json");
			String accNo = req.queryParams("accNo");
			String type = req.queryParams("type");
			String minAmount = req.queryParams("minAmount");
			String maxAmount = req.queryParams("maxAmount");
			String from = req.queryParams("from");
			String to = req.queryParams("to");

			Double min = parseDouble(minAmount);
			Double max = parseDouble(maxAmount);
			LocalDate fromDate = parseDate(from);
			LocalDate toDate = parseDate(to);

			List<String> filtered = FileReportUtil.readAllLines().stream()
				.map(ApiServer::parseHistoryLine)
				.filter(item -> item != null)
				.filter(item -> isBlank(accNo) || item.accNo.equals(accNo) || (!isBlank(item.targetAcc) && item.targetAcc.equals(accNo)))
				.filter(item -> isBlank(type) || item.type.equalsIgnoreCase(type))
				.filter(item -> min == null || item.amount >= min)
				.filter(item -> max == null || item.amount <= max)
				.filter(item -> {
					if (fromDate == null && toDate == null) {
						return true;
					}
					if (item.timestamp == null) {
						return false;
					}
					LocalDate d = item.timestamp.toLocalDate();
					if (fromDate != null && d.isBefore(fromDate)) {
						return false;
					}
					if (toDate != null && d.isAfter(toDate)) {
						return false;
					}
					return true;
				})
				.map(item -> item.raw)
				.collect(Collectors.toList());

			return gson.toJson(filtered);
		});

		get("/analytics/overview", (req, res) -> {
			res.type("application/json");
			String accNo = req.queryParams("accNo");
			List<HistoryItem> items = FileReportUtil.readAllLines().stream()
				.map(ApiServer::parseHistoryLine)
				.filter(item -> item != null)
				.filter(item -> isBlank(accNo) || item.accNo.equals(accNo) || (!isBlank(item.targetAcc) && item.targetAcc.equals(accNo)))
				.collect(Collectors.toList());

			double totalDeposit = items.stream().filter(i -> "DEPOSITE".equalsIgnoreCase(i.type)).mapToDouble(i -> i.amount).sum();
			double totalWithdraw = items.stream().filter(i -> "WITHDRAW".equalsIgnoreCase(i.type)).mapToDouble(i -> i.amount).sum();
			double totalTransfer = items.stream().filter(i -> "TRANSFER".equalsIgnoreCase(i.type)).mapToDouble(i -> i.amount).sum();

			Map<String, Double> monthlySpending = new LinkedHashMap<>();
			for (HistoryItem item : items) {
				if (item.timestamp == null) {
					continue;
				}
				if (!"WITHDRAW".equalsIgnoreCase(item.type) && !"TRANSFER".equalsIgnoreCase(item.type)) {
					continue;
				}
				String month = YearMonth.from(item.timestamp).toString();
				monthlySpending.put(month, monthlySpending.getOrDefault(month, 0.0) + item.amount);
			}

			List<String> recent = items.stream()
				.sorted(Comparator.comparing((HistoryItem i) -> i.timestamp == null ? LocalDateTime.MIN : i.timestamp).reversed())
				.limit(8)
				.map(i -> i.raw)
				.collect(Collectors.toList());

			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("totalDeposit", totalDeposit);
			payload.put("totalWithdraw", totalWithdraw);
			payload.put("totalTransfer", totalTransfer);
			payload.put("monthlySpending", monthlySpending);
			payload.put("recentTransactions", recent);
			return gson.toJson(payload);
		});
		
	}

	private static boolean requiresAuth(String path) {
		if (path.equals("/auth/set-pin") || path.equals("/auth/verify-pin") || path.equals("/auth/logout")) {
			return true;
		}

		for (String protectedPath : PROTECTED_PATHS) {
			if (protectedPath.endsWith(":accNo") && path.startsWith("/accounts/")) {
				return true;
			}
			if (protectedPath.endsWith(":accNo") && path.startsWith("/transactions/history/")) {
				return true;
			}
			if (protectedPath.equals("/transactions/query-history") && path.startsWith("/transactions/query-history")) {
				return true;
			}
			if (protectedPath.equals(path)) {
				return true;
			}
		}
		return false;
	}

	private static String sanitizeToken(String raw) {
		if (raw == null) {
			return "";
		}
		if (raw.startsWith("Bearer ")) {
			return raw.substring(7);
		}
		return raw;
	}

	private static String getUserEmailFromToken(String rawToken) {
		return TOKENS.get(sanitizeToken(rawToken));
	}

	private static String hashPassword(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new RuntimeException("Password hashing failed", e);
		}
	}

	private static Double parseDouble(String value) {
		if (isBlank(value)) {
			return null;
		}
		try {
			return Double.parseDouble(value);
		} catch (Exception e) {
			return null;
		}
	}

	private static LocalDate parseDate(String value) {
		if (isBlank(value)) {
			return null;
		}
		try {
			return LocalDate.parse(value);
		} catch (Exception e) {
			return null;
		}
	}

	private static HistoryItem parseHistoryLine(String line) {
		if (isBlank(line)) {
			return null;
		}

		String[] parts = line.split(" \\| ");
		int idx = 0;
		LocalDateTime ts = null;
		if (parts.length > 1 && parts[0].contains("T")) {
			try {
				ts = LocalDateTime.parse(parts[0], TS_FORMAT);
				idx = 1;
			} catch (Exception ignored) {
			}
		}

		if (parts.length <= idx + 2) {
			return null;
		}

		String type = parts[idx].trim();
		String accNo = parts[idx + 1].replace("Acc:", "").trim();
		String amountPart = parts[idx + 2].replace("Amount:", "").trim();
		double amount;
		try {
			amount = Double.parseDouble(amountPart);
		} catch (Exception e) {
			return null;
		}

		String target = "";
		if (parts.length > idx + 3 && parts[idx + 3].startsWith("to ")) {
			target = parts[idx + 3].replace("to", "").trim();
		}

		return new HistoryItem(ts, type, accNo, target, amount, line);
	}

	private static Map<String, String> errorResponse(String message) {
		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("status", "error");
		payload.put("message", message);
		return payload;
	}

	private static Map<String, String> successResponse(String message) {
		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("status", "success");
		payload.put("message", message);
		return payload;
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
	
	public static void enableCORS() {
		options("/*",(request,response) -> {
			String reqheaders = request.headers("Access-Control-Request-Headers");
			if(reqheaders != null) {
				response.header("Access-Control-Allow-Headers", reqheaders);
			}
			
			String reqMethod = request.headers("Access-Control-Request-Method");
			if(reqMethod != null) {
				response.header("Access-Control-Allow-Methods", reqMethod);
			}
			
			return "OK";
			
		});
		
		before((req,res) -> {
			res.header("Access-Control-Allow-Origin","*");
			res.header("Access-Control-Allow-Methods","GET,POST,PUT,DELETE,OPTIONS");
			res.header("Access-Control-Allow-Headers","Content-Type,Authorization");
		});
		
	}
	
	static class AccountRequest{
		String name;
		String email;
		BigDecimal balance;
	}
	
	static class TxRequest{
		String accNo;
		BigDecimal amount;
	}
	
	static class TransferRequest{
		String fromAcc;
		String toAcc;
		BigDecimal amount;
		
	}

	static class AuthRegisterRequest {
		String name;
		String username;
		String email;
		String password;
		String pin;
		BigDecimal openingBalance;
	}

	static class AuthLoginRequest {
		String username;
		String email;
		String password;
	}

	static class PinRequest {
		String pin;
	}

	static class ForgotPasswordRequest {
		String email;
	}

	static class ResetPasswordRequest {
		String email;
		String code;
		String newPassword;
	}

	static class PasswordResetCode {
		String code;
		long expiresAtMillis;

		PasswordResetCode(String code, long expiresAtMillis) {
			this.code = code;
			this.expiresAtMillis = expiresAtMillis;
		}
	}

	static class HistoryItem {
		LocalDateTime timestamp;
		String type;
		String accNo;
		String targetAcc;
		double amount;
		String raw;

		HistoryItem(LocalDateTime timestamp, String type, String accNo, String targetAcc, double amount, String raw) {
			this.timestamp = timestamp;
			this.type = type;
			this.accNo = accNo;
			this.targetAcc = targetAcc;
			this.amount = amount;
			this.raw = raw;
		}
	}

}



















