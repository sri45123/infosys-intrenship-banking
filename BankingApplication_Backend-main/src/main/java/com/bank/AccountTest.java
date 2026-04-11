package com.bank;

import java.math.BigDecimal;

import com.bank.exceptions.AccountNotFoundException;
import com.bank.exceptions.InvalidAmountException;
import com.bank.model.Account;
import com.bank.repository.AccountRepository;
import com.bank.service.AccountService;

public class AccountTest {
	
	public static void main(String[] args) {
		
		AccountRepository repo = new AccountRepository();
		AccountService service = new AccountService(repo);
		
		try {
			System.out.println("Created Accounts..!");
			Account account1 = service.createAccount("Hema", "hema@gmail.com", new BigDecimal("5000"));
			Account account2 = service.createAccount("Shiva", "shiva@gmail.com", new BigDecimal("1000"));
			Account account3 = service.createAccount("Hema", "hema@gmail.com", new BigDecimal("5000"));
			Account account4 = service.createAccount("Shiva", "shiva@gmail.com", new BigDecimal("1000"));
			System.out.println(account1);
			System.out.println(account2);
			System.out.println(account3);
			System.out.println(account4);
			System.out.println("========================================================");
			System.out.println("Getting the Details of the Account");
			Account acc = service.getAccount(account2.getAccountNumber());
			System.out.println(acc);
			System.out.println("========================================================");
			System.out.println("All Accounts..!");
			for(Account a : service.listAllAccounts()) {
				System.out.println(a);
			}
		} catch (InvalidAmountException | AccountNotFoundException e) {
			 
			e.printStackTrace();
		}
	}

}
