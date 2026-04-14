package com.bank.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bank.model.Account;
import com.bank.util.EmailUtil;

@Service
public class AlertService {

	private final BigDecimal threshold;

	public AlertService(@Value("${bank.alert.threshold:1000}") BigDecimal threshold) {
		this.threshold = threshold;
	}

	public void checkBalance(Account account) {
		if (account.getOpeningBalance().compareTo(threshold) <= 0) {
			String subject = "Low Balance Alert: " + account.getAccountNumber();
			String message = "Dear " + account.getHolderName() + ", \n\n Your Account Balance is Low: "
				+ account.getOpeningBalance() + "\nPlease maintain the minimum balance.";
			EmailUtil.sendEmail(account.getEmail(), subject, message);
		}
	}
}
