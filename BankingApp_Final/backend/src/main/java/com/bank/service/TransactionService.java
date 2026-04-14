package com.bank.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.InsufficientBalanceException;
import com.bank.exceptions.InvalidAmountException;
import com.bank.model.Account;
import com.bank.model.TransactionLog;
import com.bank.repository.TransactionLogJpaRepository;
import com.bank.util.FileReportUtil;

@Service
public class TransactionService {

	private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

	private final AccountService accountService;
	private final TransactionLogJpaRepository txRepo;
	private final AlertService alertservice;

	public TransactionService(AccountService accountService, TransactionLogJpaRepository txRepo, AlertService alertservice) {
		this.accountService = accountService;
		this.txRepo = txRepo;
		this.alertservice = alertservice;
	}

	@Transactional
	public void deposite(String accNo, BigDecimal amount) throws AccountNotFoundException, InvalidAmountException {
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidAmountException("Amount must be positive..!");
		}

		Account account = accountService.getAccount(accNo);
		account.credit(amount);
		accountService.saveAccount(account);
		txRepo.save(new TransactionLog("DEPOSITE", accNo, amount.doubleValue(), null));
		FileReportUtil.writeLine("DEPOSITE | Acc: " + accNo + " | Amount: " + amount);
		alertservice.checkBalance(account);
		log.info("Deposited {} to {}", amount, accNo);
	}

	@Transactional
	public void withdraw(String accNo, BigDecimal amount)
			throws AccountNotFoundException, InvalidAmountException, InsufficientBalanceException {
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidAmountException("Amount must be positive..!");
		}

		Account account = accountService.getAccount(accNo);
		if (account.getOpeningBalance().compareTo(amount) < 0) {
			throw new InsufficientBalanceException("Insufficient Balance..!");
		}

		account.debit(amount);
		accountService.saveAccount(account);
		txRepo.save(new TransactionLog("WITHDRAW", accNo, amount.doubleValue(), null));
		FileReportUtil.writeLine("WITHDRAW | Acc: " + accNo + " | Amount: " + amount);
		alertservice.checkBalance(account);
		log.info("Withdraw {} from {}", amount, accNo);
	}

	@Transactional
	public void transfer(String fromAcc, String toAcc, BigDecimal amount)
			throws InvalidAmountException, AccountNotFoundException, InsufficientBalanceException {
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidAmountException("Amount must be positive..!");
		}

		Account sender = accountService.getAccount(fromAcc);
		Account receiver = accountService.getAccount(toAcc);

		if (sender.getOpeningBalance().compareTo(amount) < 0) {
			throw new InsufficientBalanceException("Insufficient Balance..!");
		}

		sender.debit(amount);
		receiver.credit(amount);
		accountService.saveAccount(sender);
		accountService.saveAccount(receiver);
		alertservice.checkBalance(sender);
		alertservice.checkBalance(receiver);

		txRepo.save(new TransactionLog("TRANSFER", fromAcc, amount.doubleValue(), toAcc));
		FileReportUtil.writeLine("TRANSFER | Acc: " + fromAcc + " | Amount: " + amount + " | to " + toAcc);
		log.info("Transfer {} from {} to {}", amount, fromAcc, toAcc);
	}
}





