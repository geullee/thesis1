package kr.geul.thesis1.command;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.text.BadLocationException;

import kr.geul.console.command.Command;
import kr.geul.dataobject.DataClass;
import kr.geul.dataobject.DataClassArray;
import kr.geul.dataobject.DataClassInfoHolder;
import kr.geul.dataobject.Direct;
import kr.geul.dataobject.ObList;
import kr.geul.dataobject.Observation;

public class FilterMissing extends Command {

	boolean doesExist;
	File logFile;
	String fileName_o;
	DataClassArray dayDataClassArray;
	DataClassInfoHolder dayDataClassInfoHolder, exdatesInfoHolder, optionsInfoHolder, shareInfoHolder;
	PrintWriter logPrintWriter;

	public FilterMissing(ArrayList<String> arguments) {
		super(arguments);	
	}

	@Override
	protected String getCommandName() {
		return "filterm";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		int[] numbers = {0};
		return numbers;
	}

	@Override
	protected void runCommand() throws Exception {
		
		initialize();

		for (int year = 100; year < 111; year++) {

			int numberOfParts = getNumberOfParts(year);

			for (int month = 0; month < 12; month++) {

				for (int part = 0; part < numberOfParts; part++) {

					fileName_o = getFileName_o(year, month, part);
					loadClass();

					for (int dayIndex = dayDataClassArray.size() - 1; dayIndex > -1 ; dayIndex--) {
						
						Observation day = dayDataClassArray.get(dayIndex);
						Observation share = day.getObservationVariable
								(dayDataClassInfoHolder, "share");

						if (share == null || 
								share.getStringTypeVariable(shareInfoHolder, "value").equals("")) 
								dayDataClassArray.remove(dayIndex);

						else {

							ObList exdates = day.getObListVariable
									(dayDataClassInfoHolder, "exdates");

							for (int exdatesIndex = exdates.size() - 1; 
									exdatesIndex > -1; exdatesIndex--) {

								Observation exdate = exdates.get(exdatesIndex);
								ObList options = exdate.getObListVariable
										(exdatesInfoHolder, "options");

								for (int optionsIndex = options.size() - 1; 
										optionsIndex > -1; optionsIndex--) {

									Observation option = options.get(optionsIndex);

									if (option.getStringTypeVariable(optionsInfoHolder, "impvol") == null
											|| option.getStringTypeVariable(optionsInfoHolder, "delta") == null
											|| option.getStringTypeVariable(optionsInfoHolder, "impvol").equals("")
											|| option.getStringTypeVariable(optionsInfoHolder, "delta").equals(""))
										options.remove(optionsIndex);

								}

								if (options.size() == 0)
									exdates.remove(exdatesIndex);

							}

							if (exdates.size() == 0)
								dayDataClassArray.remove(dayIndex);

						}

					}

					saveClass();

				}

			}

		}
		
	}

	private String getFileName_o(int year, int month, int part) {

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
	
	private void initialize() throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException, BadLocationException, IOException {

		String initialFileName = "d:/Chapter_1/optionClassData_spi_raw/0001_SP500_1.cl";

		DataClass.clearDataClassHanger();

		dayDataClassArray = Direct.loadDataClass(initialFileName);

		dayDataClassInfoHolder = dayDataClassArray.getInfoHolder(); 
		exdatesInfoHolder = dayDataClassArray.getSubInfoHolder("exdates");
		optionsInfoHolder = dayDataClassArray.getSubInfoHolder("exdates/options");
		shareInfoHolder = dayDataClassArray.getSubInfoHolder("share");

	}
	
	private void loadClass() throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException, FileNotFoundException, IOException, 
	BadLocationException {

		DataClass.clearDataClassHanger();
		
		String fileName = "d:/Chapter_1/optionClassData_spi_raw/" + fileName_o + ".cl";

		DataClassArray dataClassArray = Direct.loadDataClass(fileName);
		DataClass.addDataClass(dataClassArray);
		dayDataClassArray = DataClass.getDataClassArray("day");

	}	

	private void saveClass() throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException, FileNotFoundException, IOException, BadLocationException {
		
		String fileName = "d:/Chapter_1/optionClassData_spi_nonMissing/" + fileName_o + ".cl";
		Direct.saveDataClass(dayDataClassArray, fileName);

	}

}
