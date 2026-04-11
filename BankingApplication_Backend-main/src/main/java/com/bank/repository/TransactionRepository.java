package com.bank.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class TransactionRepository {
	
	public void logTransaction(String type,String accNo, double amount,String targetAcc){
		
		String query = "INSERT INTO transactions(type,account_number,amount,target_account)VALUES(?,?,?,?)";
		String createTableQuery = "CREATE TABLE IF NOT EXISTS transactions ("
				+ "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
				+ "type VARCHAR(20) NOT NULL,"
				+ "account_number VARCHAR(20) NOT NULL,"
				+ "amount DOUBLE NOT NULL,"
				+ "target_account VARCHAR(20),"
				+ "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
				+ ")";
		
		try(Connection con = DBConnection.getConnection())
		{	
			 if (con == null) {
			 	return;
			 }

			 try (Statement stmt = con.createStatement()) {
			 	stmt.execute(createTableQuery);
			 }

			 try (PreparedStatement pstmt = con.prepareStatement(query)) {
			 pstmt.setString(1, type);
			 pstmt.setString(2, accNo);
			 pstmt.setDouble(3, amount);
			 pstmt.setString(4, targetAcc);
			
			 pstmt.executeUpdate();
			 }
		}
		catch(Exception e) {
			System.out.println("Failed to persist transaction log: " + e.getMessage());
		}
	}

}
