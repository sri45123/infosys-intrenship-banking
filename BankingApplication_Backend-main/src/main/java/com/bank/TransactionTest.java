package com.bank;

import java.math.BigDecimal;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.InsufficientBalanceException;
import com.bank.exceptions.InvalidAmountException;
import com.bank.model.Account;
import com.bank.repository.AccountRepository;
import com.bank.repository.TransactionRepository;
import com.bank.service.AccountService;
import com.bank.service.AlertService;
import com.bank.service.TransactionService;

public class TransactionTest {

	public static void main(String[] args) {
		
		AccountRepository accRepo = new AccountRepository();
		TransactionRepository trxRepo = new TransactionRepository();
		
		AccountService accountService = new AccountService(accRepo);
		AlertService alertService = new AlertService(new BigDecimal("1000"));
		TransactionService trxService = new TransactionService(accountService,trxRepo,alertService);
		
		try {
			
			Account account1 = accountService.createAccount("Krish", "chinnikrishna82277@gmail.com", new BigDecimal("5000"));
			Account account2 = accountService.createAccount("Madhu", "madhu@gmail.com", new BigDecimal("2000"));
			//trxService.deposite(account1.getAccountNumber(), new BigDecimal("500"));
			//trxService.withdraw(account1.getAccountNumber(), new BigDecimal("4500"));
			
			trxService.transfer(account1.getAccountNumber(), account2.getAccountNumber(), new BigDecimal("4500"));
			
			System.out.println(account1.getOpeningBalance());//4000
			System.out.println(account2.getOpeningBalance());//3000
			
		} catch (InvalidAmountException | AccountNotFoundException | InsufficientBalanceException  e) {
			 
			e.printStackTrace();
		}
		
		
	}

}
