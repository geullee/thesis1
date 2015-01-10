package kr.geul.thesis1.command;

import java.io.File;
import java.util.ArrayList;

import kr.geul.console.PublicWriter;
import kr.geul.console.command.Command;

public class GetBasicProperties_initializeLog extends Command {

	public GetBasicProperties_initializeLog(ArrayList<String> arguments) {
		super(arguments);	
	}
	
	@Override
	protected String getCommandName() {
		return "getb_log";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		return null;
	}

	@Override
	protected void runCommand() throws Exception {
		
		File logFile;
		
		if (arguments.get(0).equals("true"))
			logFile = new File("./GetBasicProperties.csv");
		else
			logFile = new File("D:/Chapter_1/GetBasicProperties.csv");

		boolean doesExist = false;

		if (logFile.exists() == true)
			doesExist = true;	

		PublicWriter.setWriter(logFile);
		
		String logHeader = "date,";

		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 6; j++) {
				logHeader += ("prices" + i + "_" + j + ",");
			}
		}
		
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 6; j++) {
				logHeader += ("spreads" + i + "_" + j + ",");
			}
		}
		
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 6; j++) {
				logHeader += ("numbers" + i + "_" + j + ",");
			}
		}
		
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 6; j++) {
				logHeader += ("ivs" + i + "_" + j);
				if (i < 5 || j < 5)
					logHeader += ",";
			}
		}		
		
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
