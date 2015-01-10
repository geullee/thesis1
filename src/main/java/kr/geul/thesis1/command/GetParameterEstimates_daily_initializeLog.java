package kr.geul.thesis1.command;

import java.io.File;
import java.util.ArrayList;

import kr.geul.console.PublicWriter;
import kr.geul.console.command.Command;

public class GetParameterEstimates_daily_initializeLog extends Command {

	public GetParameterEstimates_daily_initializeLog(ArrayList<String> arguments) {
		super(arguments);	
	}

	@Override
	protected String getCommandName() {
		return "ped_log";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		return null;
	}

	@Override
	protected void runCommand() throws Exception {
		
		File logFile;
		
		if (arguments.get(0).equals("true"))
			logFile = new File("./PED_svj.csv");
		else
			logFile = new File("D:/Chapter_1/PED_svj.csv");
		
		boolean doesExist = false;
		
		if (logFile.exists() == true)
			doesExist = true;	
		
		PublicWriter.setWriter(logFile);
		
		String logHeader = "date,kappav,thetav,v0,sigmav,muj,sigmaj,rho,lambda,sse";
		
		if (doesExist == false)
			PublicWriter.write(logHeader);
		
	}
	
}
