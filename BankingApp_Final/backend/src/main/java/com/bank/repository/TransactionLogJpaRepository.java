package com.bank.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.model.TransactionLog;

public interface TransactionLogJpaRepository extends JpaRepository<TransactionLog, Long> {
}