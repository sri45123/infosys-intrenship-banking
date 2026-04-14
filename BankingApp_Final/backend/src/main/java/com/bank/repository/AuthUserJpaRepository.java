package com.bank.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.model.AuthUser;

public interface AuthUserJpaRepository extends JpaRepository<AuthUser, Long> {
	boolean existsByEmailIgnoreCase(String email);

	boolean existsByUsernameIgnoreCase(String username);

	Optional<AuthUser> findByEmailIgnoreCase(String email);

	Optional<AuthUser> findByUsernameIgnoreCase(String username);
}