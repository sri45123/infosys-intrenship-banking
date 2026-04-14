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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileReportUtil {
	private static final Logger log = LoggerFactory.getLogger(FileReportUtil.class);
	
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
			log.warn("Unable to write transaction report", e);
		}
	}

	public static List<String> readAllLines() {
		try {
			if (!new File(REPORT_FILE).exists()) {
				return new ArrayList<>();
			}
			return Files.readAllLines(Paths.get(REPORT_FILE));
		} catch (IOException e) {
			log.warn("Unable to read transaction report", e);
			return new ArrayList<>();
		}
	}
	

}
