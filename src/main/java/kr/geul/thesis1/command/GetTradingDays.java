package kr.geul.thesis1.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import kr.geul.console.command.Command;

public class GetTradingDays extends Command {

	String fileName;
	
	public GetTradingDays(ArrayList<String> arguments) {
		super(arguments);	
	}

	@Override
	protected String getCommandName() {
		return "gett";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		int[] numbers = {0};
		return numbers;
	}

	@Override
	protected void runCommand() throws Exception {
		
		defineAliases();

		for (int year = 100; year < 113; year++) {

			int numberOfParts = getNumberOfParts(year);

			for (int month = 0; month < 12; month++) {

				for (int part = 0; part < numberOfParts; part++) {

					fileName = getFileName(year, month, part);
					buildDataClass();

				}

			}

		}

		saveDataClass();
		
	}

	private void buildDataClass() throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException {

		enterCommand("loadcsv d:/Thesis/csv_cut_s/" + fileName + ".csv tdayRawVariableNames;");
		enterCommand("sort RawObservation criteria directions;");
		enterCommand("setobs tday tdayReadingVariableNames tdayRawVariableNames sorted;");
		
	}
	
	private void defineAliases() throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException {

		enterCommand("alias tdayVariableNames date;");
		enterCommand("alias tdayVariableTypes date;");
		enterCommand("alias tdayReadingVariableNames date;");
		enterCommand("alias tdayRawVariableNames date;");

		enterCommand("alias criteria date;");
		enterCommand("alias directions a;");

		enterCommand("class tday tdayVariableNames tdayVariableTypes;");
		
	}

	private String getFileName(int year, int month, int part) {
		
		String yearString = String.valueOf(year);
		String monthString = String.valueOf(month + 1);
		String partString = String.valueOf(part + 1);
		
		if (yearString.length() == 3)
			yearString = yearString.substring(1, 3);
		if (monthString.length() == 1)
			monthString = "0" + monthString;
		
		return yearString + monthString + "_s_" + partString;
		
	}
	
	private int getNumberOfParts(int year) {

		if (year < 100)
			return 6;
		else if (year < 105)
			return 10;
		else
			return 15;

	}	
	
	private void saveDataClass() throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException {
		enterCommand("sort tday criteria directions;");
		enterCommand("saveclass tday d:/Chapter_1/tdayClassData_raw/tday.cl;");		
	}
	
}
