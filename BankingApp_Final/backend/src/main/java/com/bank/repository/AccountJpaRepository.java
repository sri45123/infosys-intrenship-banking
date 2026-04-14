package com.bank.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.model.Account;

public interface AccountJpaRepository extends JpaRepository<Account, String> {
	Optional<Account> findByEmailIgnoreCase(String email);

	List<Account> findByHolderNameContainingIgnoreCase(String holderName);
}