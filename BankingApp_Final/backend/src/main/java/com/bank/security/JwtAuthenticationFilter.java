package com.bank.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bank.dto.ApiResponse;
import com.google.gson.Gson;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final List<String> PUBLIC_PATHS = List.of(
		"/auth/register",
		"/auth/login",
		"/auth/forgot-password",
		"/auth/reset-password",
		"/",
		"/index.html",
		"/script.js",
		"/style.css"
	);

	private final JwtService jwtService;
	private final Gson gson = new Gson();

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		return PUBLIC_PATHS.contains(path) || path.startsWith("/public/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = request.getHeader("Authorization");
		if (token == null || token.isBlank()) {
			unauthorized(response, "Missing authorization token");
			return;
		}
		if (token.startsWith("Bearer ")) {
			token = token.substring(7);
		}
		try {
			String email = jwtService.parseSubject(token);
			request.setAttribute("authenticatedEmail", email);
			filterChain.doFilter(request, response);
		} catch (Exception ex) {
			unauthorized(response, "Invalid or expired session");
		}
	}

	private void unauthorized(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(gson.toJson(new ApiResponse<>("ERROR", message, null)));
	}
}