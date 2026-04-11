package com.bank.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

	private static final String DB_HOST = System.getenv().getOrDefault("BANK_DB_HOST", "localhost");
	private static final String DB_PORT = System.getenv().getOrDefault("BANK_DB_PORT", "3306");
	private static final String DB_NAME = System.getenv().getOrDefault("BANK_DB_NAME", "BankingApplication");
	private static final String DB_USER = System.getenv().getOrDefault("BANK_DB_USER", "root");
	private static final String DB_PASS = System.getenv().getOrDefault("BANK_DB_PASS", "root");
	private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
		+ "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
	private static volatile boolean warningPrinted = false;
	
public static Connection getConnection(){
		
		Connection con = null;
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
			
		} catch (ClassNotFoundException | SQLException e) {
			if (!warningPrinted) {
				System.out.println("DB connection unavailable. Transaction DB logging will be skipped.");
				warningPrinted = true;
			}
		}
		
		return con;
		
	}

}
