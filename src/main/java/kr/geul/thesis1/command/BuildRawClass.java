package kr.geul.thesis1.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import kr.geul.console.command.Command;

public class BuildRawClass extends Command {

	String fileName;
	
	public BuildRawClass(ArrayList<String> arguments) {
		super(arguments);	
	}
	
	@Override
	protected String getCommandName() {
		return "buildr";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		int[] numbers = {0};
		return numbers;
	}

	@Override
	protected void runCommand() throws Exception {
		
		defineAliases();
		
		for (int year = 100; year < 111; year++) {
			
			int numberOfParts = getNumberOfParts(year);
			
			for (int month = 0; month < 12; month++) {
				
				for (int part = 0; part < numberOfParts; part++) {
					
					fileName = getFileName(year, month, part);
					initializeDataClasses();
					buildDataClasses();
					
				}
				
			}
			
		}
		
	}

	private void buildDataClasses() throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException {

		enterCommand("loadcsv d:/Chapter_1/csv_cut_sp500/" + fileName + ".csv rawVariableNames;");
		enterCommand("sort RawObservation criteria directions;");
		enterCommand("setobs day dayReadingVariableNames dayRawVariableNames sorted;");
		enterCommand("setobs dayExdate dayExdateReadingVariableNames dayExdateRawVariableNames sorted;");
		enterCommand("setobs option optionReadingVariableNames optionRawVariableNames nofilter;");
		enterCommand("getlist dayExdate/options option optionGetlistVariableNames asortedrep;");
		enterCommand("getlist day/exdates dayExdate dayExdateGetlistVariableNames asortedrep;");
		enterCommand("getobs day/share share shareGetobsVariableNames asortedrep;");
		enterCommand("saveclass day d:/Chapter_1/optionClassData_spi_raw/" + fileName + ".cl;");
		
	}

	private void defineAliases() throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException {
		
		enterCommand("alias rawVariableNames cusip date exdate index_flag exercise_style cp_flag strike_price best_bid best_offer volume open_interest impl_volatility delta;");
		enterCommand("alias optionVariableNames cusip date exdate cp strike bid offer volume ointerest impvol delta;");
		enterCommand("alias optionVariableTypes string date date string double double double int int double double;");
		enterCommand("alias optionReadingVariableNames cusip date exdate cp strike bid offer volume ointerest impvol delta;");
		enterCommand("alias optionRawVariableNames cusip date exdate cp_flag strike_price best_bid best_offer volume open_interest impl_volatility delta;");
		enterCommand("alias dayVariableNames cusip date exchange index exercise exdates share firm;");
		enterCommand("alias dayVariableTypes string date int int string oblist obs obs;");
		enterCommand("alias dayReadingVariableNames cusip date index exercise;");
		enterCommand("alias dayRawVariableNames cusip date index_flag exercise_style;");
		enterCommand("alias dayExdateVariableNames cusip date exdate options rnms horizon;");
		enterCommand("alias dayExdateVariableTypes string date date oblist oblist int;");
		enterCommand("alias dayExdateReadingVariableNames cusip date exdate;");
		enterCommand("alias dayExdateRawVariableNames cusip date exdate;");
		enterCommand("alias dayExdateGetlistVariableNames cusip date;");
		
		enterCommand("alias optionGetlistVariableNames cusip date exdate;");
		enterCommand("alias shareGetobsVariableNames date;");
		
		enterCommand("alias criteria cusip date exdate strike_price;");
		enterCommand("alias directions a a a a;");
		
	}

	private String getFileName(int year, int month, int part) {
		
		String yearString = String.valueOf(year);
		String monthString = String.valueOf(month + 1);
		String partString = String.valueOf(part + 1);
		
		if (yearString.length() == 3)
			yearString = yearString.substring(1, 3);
		if (monthString.length() == 1)
			monthString = "0" + monthString;
		
		return yearString + monthString + "_SP500_" + partString;
		
	}
	
	private int getNumberOfParts(int year) {
		
		if (year < 100)
			return 6;
		else if (year < 105)
			return 10;
		else
			return 15;
		
	}	

	private void initializeDataClasses() throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException {
		
		enterCommand("class *;");
		enterCommand("class option optionVariableNames optionVariableTypes;");
		enterCommand("class day dayVariableNames dayVariableTypes;");
		enterCommand("class dayExdate dayExdateVariableNames dayExdateVariableTypes;");
		enterCommand("loadclass d:/Chapter_1/indexClassData/spi.cl;");
		
	}
	
}
