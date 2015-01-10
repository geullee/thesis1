package kr.geul.thesis1.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.text.BadLocationException;

import kr.geul.console.command.Command;
import kr.geul.dataobject.DataClass;
import kr.geul.dataobject.DataClassArray;
import kr.geul.dataobject.DataClassInfoHolder;
import kr.geul.dataobject.InfoHolder;
import kr.geul.dataobject.ObList;
import kr.geul.dataobject.Observation;
import kr.geul.thesis1.Thesis1;

public class GetBKM_spi_filter extends Command {

	static final double annualTradingDays = 252.0, minimumOptionPrice = 0.375, 
						minimumTimeToMaturityInTradingDays = 5.0,
						maximumTimeToMaturityInTradingDays = 252.0;
		
	DataClassArray dayDataClassArray, dividendDataClassArray, rfrDataClassArray, tdayDataClassArray;
	DataClassInfoHolder dayInfoHolder, dividendInfoHolder, exdateInfoHolder, optionInfoHolder, 
	rfrInfoHolder, rateInfoHolder, shareInfoHolder;
	
	public GetBKM_spi_filter(ArrayList<String> arguments) {
		super(arguments);	
	}
	
	@Override
	protected String getCommandName() {
		return "bkms_filter";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		return null;
	}

	@Override
	protected void runCommand() throws Exception {
		
		setupArrayAndInfoHolder();
							
		for (int dayIndex = dayDataClassArray.size() - 1; dayIndex > -1 ; dayIndex--) {
		
			Observation day = dayDataClassArray.get(dayIndex);
			String date = day.getStringTypeVariable(dayInfoHolder, "date");
			
			Calendar dateCalendar = day.getDateVariable(dayInfoHolder, "date");
			Observation rfrDay = getRfrDay(date);
			Observation share = day.getObservationVariable(dayInfoHolder, "share");
			
			if (isEntryMissing(share, rfrDay) == true)
				dayDataClassArray.remove(dayIndex);
			
			else {
				
				double sharePrice = 
						Math.abs((double) share.getVariable(shareInfoHolder, "value"));

				ObList exdates = 
						day.getObListVariable(dayInfoHolder, "exdates");
				
				for (int exdatesIndex = exdates.size() - 1; 
						exdatesIndex > -1; exdatesIndex--) {
					
					Observation exdate = exdates.get(exdatesIndex);
					Calendar exdateCalendar = exdate.getDateVariable(exdateInfoHolder, "exdate");
					
					boolean isThisMaturityTraded = false;
					double dividendRate = getDividendRate(dateCalendar, exdateCalendar);
					double tau = Thesis1.getTdayTau(dateCalendar, exdateCalendar, tdayDataClassArray);
					
					Double rfrRate = Thesis1.getRfr
							(rfrDay, rfrInfoHolder, rateInfoHolder, 
									dateCalendar, exdateCalendar);									
					ObList options = 
							exdate.getObListVariable(exdateInfoHolder, "options");
					
					for (int optionsIndex = options.size() - 1; 
							optionsIndex > -1; optionsIndex--) {

						Observation option = options.get(optionsIndex);
						
						if (isOTM(sharePrice, option, tau, dividendRate) == false ||
								isExpensiveEnough(option) == false ||
								isSpreadNarrowEnough(option) == false ||
								doesViolateEuropeanArbitrageRestriction
								(option, sharePrice, dividendRate, rfrRate, tau) == true)
							options.remove(optionsIndex);
							
						else if (isTraded(option) == true)	
								isThisMaturityTraded = true;
						
					}
					
					if (isThisMaturityTraded == false || rfrRate.isNaN() == true ||
						tau * annualTradingDays < minimumTimeToMaturityInTradingDays || 
						tau * annualTradingDays >= maximumTimeToMaturityInTradingDays) 
						exdates.remove(exdatesIndex);
					
				}
				
				if (exdates.size() == 0)
					dayDataClassArray.remove(dayIndex);
				
			}
			
		}
	
		DataClass.resetDataClass(dayDataClassArray);
		
	}

	public boolean doesViolateEuropeanArbitrageRestriction(
			Observation option, double sharePrice, double dividendRate,
			double rfr, double tau) throws ClassNotFoundException, 
			InstantiationException, IllegalAccessException, IllegalArgumentException, 
			InvocationTargetException, NoSuchMethodException, SecurityException, 
			BadLocationException {
		
		String cpFlag = option.getStringTypeVariable(optionInfoHolder, "cp");
		double bid = (double) option.getVariable(optionInfoHolder, "bid");
		double offer = (double) option.getVariable(optionInfoHolder, "offer");
		double strike = (double) option.getVariable(optionInfoHolder, "strike") / 1000.0; 
		double price = (bid + offer) / 2.0; 

		if (bid == 0.0 || bid > offer)
			return true;
		
		else {

			if (cpFlag.equals("C")) {

				if (price > sharePrice * Math.exp(-1.0 * dividendRate) || 
						price < Math.max(0, sharePrice * Math.exp(-1.0 * dividendRate) - (strike * Math.exp(-1.0 * rfr * tau))))
					return true;
				else
					return false;

			}

			else {

				if (price > sharePrice * Math.exp(-1.0 * dividendRate) || 
						price < Math.max(0, (strike * Math.exp(-1.0 * rfr * tau))) - sharePrice * Math.exp(-1.0 * dividendRate))
					return true;
				else
					return false;

			}

		}
		
	}
	
	private double getDividendRate(Calendar dateCalendar,
			Calendar exdateCalendar) throws ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException, BadLocationException {

		ArrayList<Double> rates = new ArrayList<Double>();
		double dividendRate = 1.0;

		for (int dividendIndex = 0; dividendIndex < dividendDataClassArray.size(); dividendIndex++) {

			Observation dividend = dividendDataClassArray.get(dividendIndex);

			String dividendDateString = dividend.getStringTypeVariable(dividendInfoHolder, "date");
			Calendar dividendCalendar = Thesis1.toCalendar(dividendDateString);

			if (dividendCalendar.getTimeInMillis() >= dateCalendar.getTimeInMillis() &&
					dividendCalendar.getTimeInMillis() <= exdateCalendar.getTimeInMillis()) {

				double rate = (double) dividend.getVariable(dividendInfoHolder, "rate") / 100.0;
				rates.add(rate);

			}

		}

		for (int i = 0; i < rates.size(); i++) {

			dividendRate *= ((rates.get(i) / annualTradingDays) + 1.0);

		}

		return (dividendRate - 1.0) * (annualTradingDays / (double) rates.size());

	}
	
	private Observation getRfrDay(String date) throws BadLocationException,
	ClassNotFoundException, InstantiationException, IllegalAccessException, 
	IllegalArgumentException, InvocationTargetException, NoSuchMethodException, 
	SecurityException {

		for (int i = 0; i < rfrDataClassArray.size(); i++) {

			Observation rfrDay = rfrDataClassArray.get(i);
			String rfrDate = rfrDay.getStringTypeVariable(rfrInfoHolder, "date");

			if (date.equals(rfrDate))
				return rfrDay;

		}

		return null;

	}
	

	private boolean isEntryMissing(Observation share, Observation rfr) 
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, 
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, 
			SecurityException, BadLocationException, NoSuchFieldException {
		
		String sharePriceString = share.getStringTypeVariable(shareInfoHolder, "value");
		
		if (sharePriceString.equals("") || rfr == null ||
				rfr.getObListVariable(rfrInfoHolder, "rates") == null) 
			return true;
		else 
			return false;
		
	}
	
	private boolean isExpensiveEnough(Observation option) 
			throws BadLocationException, ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {

		boolean isExpensiveEnough;
		double bid = Double.parseDouble(option.getStringTypeVariable(optionInfoHolder, "bid"));
		double offer = Double.parseDouble(option.getStringTypeVariable(optionInfoHolder, "offer"));
		double closingPrice = (bid + offer) / 2.0;

		if (closingPrice < minimumOptionPrice)
			isExpensiveEnough = false;

		else
			isExpensiveEnough = true;

		return isExpensiveEnough;

	}
	
	private boolean isOTM(double sharePrice, Observation option, double tau, double dividendRate) 
			throws BadLocationException, ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {

		boolean isOTM;
		String cpFlag = option.getStringTypeVariable(optionInfoHolder, "cp");
		double strike = (double) option.getVariable(optionInfoHolder, "strike") / 1000.0; 
		double discountedSharePrice = sharePrice * Math.exp(-tau * dividendRate);
		
		if (cpFlag.equals("C") && strike > discountedSharePrice)
			isOTM = true;

		else if (cpFlag.equals("P") && strike < discountedSharePrice)
			isOTM = true;

		else
			isOTM = false;

		return isOTM;

	}
	
	private boolean isSpreadNarrowEnough(Observation option) throws NumberFormatException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, BadLocationException {
		
		double bid = Double.parseDouble(option.getStringTypeVariable(optionInfoHolder, "bid"));
		double offer = Double.parseDouble(option.getStringTypeVariable(optionInfoHolder, "offer"));
		
		if ((offer - bid) > (bid + offer) / 2.0)
			return false;
		else
			return true;
		
	}
	
	public boolean isTraded(Observation option) 
			throws BadLocationException, ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {

		boolean isTraded;
		int volume = Integer.parseInt(option.getStringTypeVariable(optionInfoHolder, "volume"));

		if (volume == 0)
			isTraded = false;

		else
			isTraded = true;

		return isTraded;

	}
	
	private void setupArrayAndInfoHolder() throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException, BadLocationException {
		
		dayDataClassArray = DataClass.getDataClassArray("day");
		dividendDataClassArray = DataClass.getDataClassArray("dividendRate");
		rfrDataClassArray = DataClass.getDataClassArray("rfrDay");
		tdayDataClassArray = DataClass.getDataClassArray("tday");
		dividendInfoHolder = InfoHolder.getInfoHolder("dividend");
		dayInfoHolder = InfoHolder.getInfoHolder("day");
		exdateInfoHolder = InfoHolder.getInfoHolder("exdate");
		optionInfoHolder = InfoHolder.getInfoHolder("option");
		rfrInfoHolder = InfoHolder.getInfoHolder("rfr");
		rateInfoHolder = InfoHolder.getInfoHolder("rate");
		shareInfoHolder = InfoHolder.getInfoHolder("share");
		
	}
	
}
