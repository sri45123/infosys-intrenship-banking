package com.bank.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

	private final SecretKey key;
	private final long expirationMillis;

	public JwtService(@Value("${bank.jwt.secret}") String secret, @Value("${bank.jwt.expiration-ms}") long expirationMillis) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMillis = expirationMillis;
	}

	public String generateToken(String subject) {
		Date now = new Date();
		Date expiration = new Date(now.getTime() + expirationMillis);
		return Jwts.builder()
			.subject(subject)
			.issuedAt(now)
			.expiration(expiration)
			.signWith(key)
			.compact();
	}

	public String parseSubject(String token) {
		return Jwts.parser()
			.verifyWith(key)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.getSubject();
	}
}