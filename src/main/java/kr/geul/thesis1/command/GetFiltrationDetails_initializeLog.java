package kr.geul.thesis1.command;

import java.io.File;
import java.util.ArrayList;

import kr.geul.console.PublicWriter;
import kr.geul.console.command.Command;

public class GetFiltrationDetails_initializeLog extends Command {

	public GetFiltrationDetails_initializeLog(ArrayList<String> arguments) {
		super(arguments);	
	}
	
	@Override
	protected String getCommandName() {
		return "getf_log";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		return null;
	}

	@Override
	protected void runCommand() throws Exception {
		
		File logFile;
		
		if (arguments.get(0).equals("true"))
			logFile = new File("./GetFiltrationDetail.csv");
		else
			logFile = new File("D:/Chapter_1/GetFiltrationDetail.csv");

		boolean doesExist = false;

		if (logFile.exists() == true)
			doesExist = true;	

		PublicWriter.setWriter(logFile);
		String logHeader = "date,";
		
		for (int i = 0; i < 13; i++) {
			String category = getCategory(i);
			logHeader += "total_" + category + ",";
		}
		
		for (int i = 0; i < 13; i++) {
			String category = getCategory(i);
			logHeader += "tooShortM_" + category + ",";
		}
		
		for (int i = 0; i < 13; i++) {
			String category = getCategory(i);
			logHeader += "tooLongM_" + category + ",";
		}

		for (int i = 0; i < 13; i++) {
			String category = getCategory(i);
			logHeader += "noVol_" + category + ",";
		}
		
		for (int i = 0; i < 13; i++) {
			String category = getCategory(i);
			logHeader += "zeroB_" + category + ",";
		}
		
		for (int i = 0; i < 13; i++) {
			String category = getCategory(i);
			logHeader += "tooHighB_" + category + ",";
		}

		for (int i = 0; i < 13; i++) {
			String category = getCategory(i);
			logHeader += "arbitrage_" + category + ",";
		}

		for (int i = 0; i < 13; i++) {
			String category = getCategory(i);
			logHeader += "tooCheap_" + category + ",";
		}
		
		for (int i = 0; i < 13; i++) {
			String category = getCategory(i);
			logHeader += "ITM_" + category + ",";
		}
		
		if (doesExist == false)
			PublicWriter.write(logHeader);
		
	}
	
	private String getCategory(int i) {
		
		switch (i) {
		
		case 0:
			return "ITMC1";
		case 1:
			return "ITMC2";
		case 2:
			return "ITMC3";
		case 3:
			return "OTMC3";
		case 4:
			return "OTMC2";
		case 5:
			return "OTMC1";
		case 6:
			return "OTMP1";
		case 7:
			return "OTMP2";
		case 8:
			return "OTMP3";
		case 9:
			return "ITMP3";
		case 10:
			return "ITMP2";
		case 11:
			return "ITMP1";
		default:
			return "all";
			
		}
		
	}
	
}
