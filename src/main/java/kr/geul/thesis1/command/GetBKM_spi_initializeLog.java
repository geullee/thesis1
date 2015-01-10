package kr.geul.thesis1.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import kr.geul.console.PublicWriter;
import kr.geul.console.command.Command;

public class GetBKM_spi_initializeLog extends Command {

	public GetBKM_spi_initializeLog(ArrayList<String> arguments) {
		super(arguments);	
	}
	
	@Override
	protected String getCommandName() {
		return "bkms_log";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		return null;
	}

	@Override
	protected void runCommand() throws Exception {
		
		File logFile;
		
		if (arguments.get(0).equals("true"))
			logFile = new File("./BKM_log_spi_" + arguments.get(1) + ".csv");
		else
			logFile = new File("D:/Chapter_1/BKM_log_spi_" + arguments.get(1) + ".csv");

		boolean doesExist = false;

		if (logFile.exists() == true)
			doesExist = true;	

		PublicWriter.setWriter(logFile);
//		String logHeader = "date,share,tau,kmin,kmax,calls,puts,leftm,rightm,leftlm,rightlm,vol,skew,kurt,volext,skewext,kurtext";
		String logHeader = "date,share,tau,kmin,kmax,kmin99,kmax99,kmin95,kmax95,kmin90,kmax90,vol,skew,kurt,volext,skewext,kurtext,vol99,skew99,kurt99,vole99,skewe99,kurte99,vol95,skew95,kurt95,vole95,skewe95,kurte95,vol90,skew90,kurt90,vole90,skewe90,kurte90";
		if (doesExist == false)
			PublicWriter.write(logHeader);
		
	}
	
//	private String getFileName() {
//
//		String fileName;
//		int year = Integer.parseInt(arguments.get(1)) - 100;
//		int month = Integer.parseInt(arguments.get(2)) + 1;
//		
//		if (year < 10)
//			fileName = "0" + year;
//		else
//			fileName = "" + year;
//
//		if (month < 10)
//			fileName += "0";
//
//		fileName += month;
//
//		return fileName;
//
//	}
	
}
