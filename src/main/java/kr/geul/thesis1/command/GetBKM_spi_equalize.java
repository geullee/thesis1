package kr.geul.thesis1.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.text.BadLocationException;

import kr.geul.console.command.Command;
import kr.geul.dataobject.DataClass;
import kr.geul.dataobject.DataClassArray;
import kr.geul.dataobject.DataClassInfoHolder;
import kr.geul.dataobject.InfoHolder;
import kr.geul.dataobject.ObList;
import kr.geul.dataobject.Observation;

public class GetBKM_spi_equalize extends Command {

	DataClassArray dayDataClassArray;
	DataClassInfoHolder dayInfoHolder, exdateInfoHolder, optionInfoHolder, shareInfoHolder;
	
	final double leftBaseMoneyness = 1.5;
	final double rightBaseMoneyness = 1.5;
	
	public GetBKM_spi_equalize(ArrayList<String> arguments) {
		super(arguments);	
	}

	@Override
	protected String getCommandName() {
		return "bkms_eq";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		return null;
	}

	@Override
	protected void runCommand() throws Exception {
		
		dayDataClassArray = DataClass.getDataClassArray("day");
		dayInfoHolder = InfoHolder.getInfoHolder("day");
		exdateInfoHolder = InfoHolder.getInfoHolder("exdate");
		optionInfoHolder = InfoHolder.getInfoHolder("option");
		shareInfoHolder = InfoHolder.getInfoHolder("share");
		
		for (int dayIndex = dayDataClassArray.size() - 1; dayIndex > -1 ; dayIndex--) {
			
			Observation day = dayDataClassArray.get(dayIndex);
			Observation share = day.getObservationVariable(dayInfoHolder, "share");
			ObList exdates = 
					day.getObListVariable(dayInfoHolder, "exdates");
			double sharePrice = 
					Math.abs((double) share.getVariable(shareInfoHolder, "value"));		
			
			for (int exdatesIndex = exdates.size() - 1; 
					exdatesIndex > -1; exdatesIndex--) {
				
				double leftEndMoneyness = 0.0, rightEndMoneyness = 0.0; 
				
				Observation exdate = exdates.get(exdatesIndex);
				ObList options = 
						exdate.getObListVariable(exdateInfoHolder, "options");	
				
				boolean areGapsEqual = true;	
				
				for (int optionsIndex = 0; optionsIndex < options.size(); optionsIndex++) {
					
					Observation option = options.get(optionsIndex);
					double strike = (double) option.getVariable(optionInfoHolder, "strike") / 1000.0; 
					
					if (strike > sharePrice) {
					
						double moneyness = Math.log(strike / sharePrice);
						if (moneyness > rightEndMoneyness)
							rightEndMoneyness = moneyness;
						
					}
					
					else {
						
						double moneyness = Math.log(strike / sharePrice) * -1.0;
						if (moneyness > leftEndMoneyness)
							leftEndMoneyness = moneyness;
						
					}
					
//					if (optionsIndex >= 2) {
//						
//						if ((double) options.get(optionsIndex).getVariable(optionInfoHolder, "strike") -
//								(double) options.get(optionsIndex - 1).getVariable(optionInfoHolder, "strike") !=
//								(double) options.get(optionsIndex - 1).getVariable(optionInfoHolder, "strike") -
//								(double) options.get(optionsIndex - 2).getVariable(optionInfoHolder, "strike"))
//							areGapsEqual = false;
//						
//					}
					
				}
				
				int numberOfCalls = getCallPutAmount("C", options);
				int numberOfPuts = getCallPutAmount("P", options);
				int smallerNumber = Math.min(numberOfCalls, numberOfPuts);

				if (smallerNumber < 2 || areGapsEqual == false) 	
					exdates.remove(exdatesIndex);
				
//				if (smallerNumber < 2 || leftEndMoneyness < leftBaseMoneyness || 
//						rightEndMoneyness < rightBaseMoneyness || areGapsEqual == false) 	
//					exdates.remove(exdatesIndex);
				
			}
			
		}
		
	}
	
	private int getCallPutAmount(String flag, ObList options) 
			throws BadLocationException, ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {

		int amount = 0;

		for (int i = 0; i < options.size(); i++) {

			Observation option = options.get(i);
			String cpFlag = (String) option.getVariable(optionInfoHolder, "cp");

			if (flag.equals(cpFlag))
				amount++;

		}

		return amount;

	}
	
}
