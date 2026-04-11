package com.bank.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.bank.model.AuthUser;

public class AuthRepository {

	private final Map<String, AuthUser> memoryByEmail = new ConcurrentHashMap<>();
	private final Map<String, String> memoryByUsername = new ConcurrentHashMap<>();
	private volatile boolean dbReady = false;

	public AuthRepository() {
		ensureTable();
	}

	private synchronized void ensureTable() {
		String query = "CREATE TABLE IF NOT EXISTS app_users ("
			+ "id INT AUTO_INCREMENT PRIMARY KEY,"
			+ "name VARCHAR(120) NOT NULL,"
			+ "username VARCHAR(80) NOT NULL UNIQUE,"
			+ "email VARCHAR(150) NOT NULL UNIQUE,"
			+ "password_hash VARCHAR(255) NOT NULL,"
			+ "pin VARCHAR(10) NULL,"
			+ "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
			+ ")";
		Connection con = DBConnection.getConnection();
		if (con == null) {
			dbReady = false;
			return;
		}

		try (PreparedStatement pstmt = con.prepareStatement(query)) {
			pstmt.execute();
			dbReady = true;
		} catch (Exception e) {
			dbReady = false;
			System.out.println("Auth table setup failed: " + e.getMessage());
		} finally {
			try {
				con.close();
			} catch (Exception ignored) {
			}
		}
	}

	private boolean useDatabase() {
		if (dbReady) {
			return true;
		}
		ensureTable();
		return dbReady;
	}

	public boolean existsByEmail(String email) {
		if (!useDatabase()) {
			return memoryByEmail.containsKey(email);
		}
		String query = "SELECT 1 FROM app_users WHERE email = ? LIMIT 1";
		return existsByField(query, email);
	}

	public boolean existsByUsername(String username) {
		if (!useDatabase()) {
			return memoryByUsername.containsKey(username);
		}
		String query = "SELECT 1 FROM app_users WHERE username = ? LIMIT 1";
		return existsByField(query, username);
	}

	private boolean existsByField(String query, String value) {
		Connection con = DBConnection.getConnection();
		if (con == null) {
			dbReady = false;
			return false;
		}

		try (PreparedStatement pstmt = con.prepareStatement(query)) {
			pstmt.setString(1, value);
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			dbReady = false;
			throw new RuntimeException("User lookup failed", e);
		} finally {
			try {
				con.close();
			} catch (Exception ignored) {
			}
		}
	}

	public void save(AuthUser user) {
		if (!useDatabase()) {
			memoryByEmail.put(user.getEmail(), user);
			memoryByUsername.put(user.getUsername(), user.getEmail());
			return;
		}

		String query = "INSERT INTO app_users(name,username,email,password_hash,pin) VALUES(?,?,?,?,?)";
		Connection con = DBConnection.getConnection();
		if (con == null) {
			dbReady = false;
			memoryByEmail.put(user.getEmail(), user);
			memoryByUsername.put(user.getUsername(), user.getEmail());
			return;
		}

		try (PreparedStatement pstmt = con.prepareStatement(query)) {
			pstmt.setString(1, user.getName());
			pstmt.setString(2, user.getUsername());
			pstmt.setString(3, user.getEmail());
			pstmt.setString(4, user.getPasswordHash());
			if (user.getPin() == null || user.getPin().isBlank()) {
				pstmt.setNull(5, java.sql.Types.VARCHAR);
			} else {
				pstmt.setString(5, user.getPin());
			}
			pstmt.executeUpdate();
		} catch (SQLException e) {
			dbReady = false;
			throw new RuntimeException("Unable to save user", e);
		} finally {
			try {
				con.close();
			} catch (Exception ignored) {
			}
		}
	}

	public AuthUser findByEmail(String email) {
		if (!useDatabase()) {
			return memoryByEmail.get(email);
		}
		String query = "SELECT name, username, email, password_hash, pin FROM app_users WHERE email = ? LIMIT 1";
		return findOne(query, email);
	}

	public AuthUser findByUsername(String username) {
		if (!useDatabase()) {
			String email = memoryByUsername.get(username);
			return email == null ? null : memoryByEmail.get(email);
		}
		String query = "SELECT name, username, email, password_hash, pin FROM app_users WHERE username = ? LIMIT 1";
		return findOne(query, username);
	}

	private AuthUser findOne(String query, String value) {
		Connection con = DBConnection.getConnection();
		if (con == null) {
			dbReady = false;
			return null;
		}

		try (PreparedStatement pstmt = con.prepareStatement(query)) {
			pstmt.setString(1, value);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return new AuthUser(
					rs.getString("name"),
					rs.getString("username"),
					rs.getString("email"),
					rs.getString("password_hash"),
					rs.getString("pin")
				);
			}
		} catch (SQLException e) {
			dbReady = false;
			throw new RuntimeException("Unable to fetch user", e);
		} finally {
			try {
				con.close();
			} catch (Exception ignored) {
			}
		}
	}

	public void updatePin(String email, String pin) {
		if (!useDatabase()) {
			AuthUser user = memoryByEmail.get(email);
			if (user != null) {
				user.setPin(pin);
			}
			return;
		}

		String query = "UPDATE app_users SET pin = ? WHERE email = ?";
		Connection con = DBConnection.getConnection();
		if (con == null) {
			dbReady = false;
			return;
		}

		try (PreparedStatement pstmt = con.prepareStatement(query)) {
			pstmt.setString(1, pin);
			pstmt.setString(2, email);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			dbReady = false;
			throw new RuntimeException("Unable to update PIN", e);
		} finally {
			try {
				con.close();
			} catch (Exception ignored) {
			}
		}
	}

	public void updatePasswordHash(String email, String passwordHash) {
		if (!useDatabase()) {
			AuthUser user = memoryByEmail.get(email);
			if (user != null) {
				AuthUser updated = new AuthUser(user.getName(), user.getUsername(), user.getEmail(), passwordHash, user.getPin());
				memoryByEmail.put(email, updated);
				memoryByUsername.put(updated.getUsername(), email);
			}
			return;
		}

		String query = "UPDATE app_users SET password_hash = ? WHERE email = ?";
		Connection con = DBConnection.getConnection();
		if (con == null) {
			dbReady = false;
			return;
		}

		try (PreparedStatement pstmt = con.prepareStatement(query)) {
			pstmt.setString(1, passwordHash);
			pstmt.setString(2, email);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			dbReady = false;
			throw new RuntimeException("Unable to update password", e);
		} finally {
			try {
				con.close();
			} catch (Exception ignored) {
			}
		}
	}
}
