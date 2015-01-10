package kr.geul.thesis1.command;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.text.BadLocationException;

import kr.geul.console.command.Command;

public class GetParameterEstimates_daily_bs extends Command {

	static boolean isRunOnServer = true;
	
	String fileName;
	int yearBegin = 100, yearEnd = 101, monthBegin = 0, monthEnd = 12;

	public GetParameterEstimates_daily_bs(ArrayList<String> arguments) {
		super(arguments);	
	}

	@Override
	protected String getCommandName() {
		return "ped_bs";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		int[] numbers = {0, 1, 2};
		return numbers;
	}

	@Override
	protected void runCommand() throws Exception {

		initialize();

		if (arguments.size() >= 1)
			setPeriod(arguments);

		for (int year = yearBegin; year < yearEnd; year++) {

			int numberOfParts = getNumberOfParts(year);

			for (int month = monthBegin; month < monthEnd; month++) {

				enterCommand("ped_log_bs " + isRunOnServer + " " + year + " " + month + ";");						

				for (int part = 0; part < numberOfParts; part++) {

					fileName = getFileName(year, month, part);

					enterCommand("class day;");
					
					if (isRunOnServer == true)
						enterCommand("loadclass ./spi/" + fileName + ".cl;");
					else 
						enterCommand("loadclass D:/Chapter_1/optionClassData_spi_nonMissing/" + 
								fileName + ".cl;");
					
					enterCommand("ped_filter;");

					if (arguments.size() >= 1) {

						if (arguments.size() == 1)
							enterCommand("ped_estimate_bs " + fileName + " 20" + arguments.get(0) + ";");

						else if (arguments.size() == 2)
							enterCommand("ped_estimate_bs " + fileName + ";");

						else {
							System.out
							.println("Wrong argument combination: running in default setting");
							enterCommand("ped_estimate_bs " + fileName + ";");
						}

					}

					else
						enterCommand("ped_estimate_bs " + fileName + ";");

				}

			}

		}

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

	private void initialize() 
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, 
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, 
			SecurityException, BadLocationException, IOException {

		if (isRunOnServer == true) {
			
			enterCommand("loadclass ./spi/0001_SP500_1.cl;");
			enterCommand("loadclass ./div/spd.cl;");
			enterCommand("loadclass ./rfr/rfrDay.cl;");
			enterCommand("loadclass ./tday/tday.cl;");
			
		}
		
		else {
			
			enterCommand("loadclass D:/Chapter_1/optionClassData_spi_nonMissing/0001_SP500_1.cl;");
			enterCommand("loadclass D:/Chapter_1/dividendClassData/spd.cl;");
			enterCommand("loadclass D:/Chapter_1/rfrClassData/rfrDay.cl;");
			enterCommand("loadclass D:/Chapter_1/tdayClassData_raw/tday.cl;");
			
		}
		
		enterCommand("infoholder day day;");
		enterCommand("infoholder day/exdates exdate;");
		enterCommand("infoholder day/exdates/options option;");
		enterCommand("infoholder day/share share;");
		enterCommand("infoholder dividendRate dividend;");
		enterCommand("infoholder rfrDay rfr;");
		enterCommand("infoholder rfrDay/rates rate;");
		enterCommand("infoholder tday tday;");

	}

	private void setPeriod(ArrayList<String> arguments) {

		String date = arguments.get(0);

		if (date.length() >= 2) {
			yearBegin = Integer.parseInt(date.substring(0, 2)) + 100;
			yearEnd = yearBegin + 1;
		}

		if (date.length() >= 4) {
			monthBegin = Integer.parseInt(date.substring(2, 4)) - 1;
			monthEnd = monthBegin + 1;
		}

	}

}
