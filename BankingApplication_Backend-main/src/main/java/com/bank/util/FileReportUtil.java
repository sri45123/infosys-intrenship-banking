package com.bank.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileReportUtil {
	
	private static final String REPORT_FOLDER = "reports";
	private static final String REPORT_FILE = REPORT_FOLDER+"/transactions_reports.txt";
	private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
	
	static {
		File folder = new File(REPORT_FOLDER);
		if(!folder.exists()) {
			folder.mkdir();
		}
	}
	
	public static void writeLine(String line){
		try(BufferedWriter bw = new BufferedWriter(new FileWriter(REPORT_FILE,true))){
			bw.write(LocalDateTime.now().format(TS_FORMAT) + " | " + line);
			bw.newLine();
		}
		catch(IOException e) {
			System.out.println(e.getMessage());
		}
	}

	public static List<String> readAllLines() {
		try {
			if (!new File(REPORT_FILE).exists()) {
				return new ArrayList<>();
			}
			return Files.readAllLines(Paths.get(REPORT_FILE));
		} catch (IOException e) {
			System.out.println(e.getMessage());
			return new ArrayList<>();
		}
	}
	

}
