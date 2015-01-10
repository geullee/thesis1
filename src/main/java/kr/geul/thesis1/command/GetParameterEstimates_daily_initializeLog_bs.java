package kr.geul.thesis1.command;

import java.io.File;
import java.util.ArrayList;

import kr.geul.console.PublicWriter;
import kr.geul.console.command.Command;

public class GetParameterEstimates_daily_initializeLog_bs extends Command {

	public GetParameterEstimates_daily_initializeLog_bs(ArrayList<String> arguments) {
		super(arguments);	
	}

	@Override
	protected String getCommandName() {
		return "ped_log_bs";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		return null;
	}

	@Override
	protected void runCommand() throws Exception {
		
		File logFile;
		
		if (arguments.get(0).equals("true"))
			logFile = new File("./PED_bs.csv");
		else
			logFile = new File("D:/Chapter_1/PED_bs.csv");
		
		boolean doesExist = false;
		
		if (logFile.exists() == true)
			doesExist = true;	
		
		PublicWriter.setWriter(logFile);
		
		String logHeader = "date,options,sigma_bs";
		
		if (doesExist == false)
			PublicWriter.write(logHeader);
		
	}
	
	private String getFileName() {

		String fileName;
		int year = Integer.parseInt(arguments.get(1)) - 100;
		int month = Integer.parseInt(arguments.get(2)) + 1;
		
		if (year < 10)
			fileName = "0" + year;
		else
			fileName = "" + year;

		if (month < 10)
			fileName += "0";

		fileName += month;

		return fileName;

	}
	
}
