package kr.geul.thesis1.command;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.text.BadLocationException;

import kr.geul.console.Console;
import kr.geul.console.PublicWriter;
import kr.geul.console.command.Command;
import kr.geul.dataobject.DataClass;
import kr.geul.dataobject.DataClassArray;
import kr.geul.dataobject.DataClassInfoHolder;
import kr.geul.dataobject.InfoHolder;
import kr.geul.dataobject.ObList;
import kr.geul.dataobject.Observation;
import kr.geul.options.option.CallOption;
import kr.geul.options.option.Option;
import kr.geul.options.option.PutOption;
import kr.geul.thesis1.Thesis1;

public class GetBasicProperties_get extends Command {

	static final double annualTradingDays = 252.0, minimumOptionPrice = 0.375, 
			minimumTimeToMaturityInTradingDays = 5.0,
			maximumTimeToMaturityInTradingDays = 252.0;
	static final String[] variableNames = {"S", "K", "R", "T", "D", "C", "V", "delta"};

	DataClassArray dayDataClassArray, dividendDataClassArray, rfrDataClassArray, tdayDataClassArray;
	DataClassInfoHolder dayInfoHolder, dividendInfoHolder, exdateInfoHolder, optionInfoHolder, 
	rfrInfoHolder, rateInfoHolder, shareInfoHolder;
	String fileName;

	/* Variables that are renewed for each day */
	double sharePrice;
	double[][] prices, spreads, ivs;
	int[][] numbers;

	Calendar dateCalendar;
	ObList exdates;
	Observation day, rfrDay, share;
	String date;

	/* Variables that are renewed for each time to maturity */
	double rfr, dividend, tau;

	Calendar exdateCalendar;
	Observation exdate;
	ObList options;

	public GetBasicProperties_get(ArrayList<String> arguments) {
		super(arguments);	
	}

	@Override
	protected String getCommandName() {
		return "getb_get";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		return null;
	}

	@Override
	protected void runCommand() throws Exception {

		setupArrayAndInfoHolder();

		tic();

		for (int dayIndex = 0; dayIndex < dayDataClassArray.size(); dayIndex++) {

			setResultArray();
			
			updateProgress(dayIndex, dayDataClassArray.size());
			setDayVariables(dayIndex);

			if (checkPeriod() == true) {
			
				for (int exdatesIndex = 0; 
						exdatesIndex < exdates.size(); exdatesIndex++) {

					setExdateVariables(exdatesIndex);

					for (int optionsIndex = 0; optionsIndex < options.size(); optionsIndex++) {

						Observation option = options.get(optionsIndex);

						double bid = (double) option.getVariable(optionInfoHolder, "bid");
						double offer = (double) option.getVariable(optionInfoHolder, "offer");
						double strike = (double) option.getVariable(optionInfoHolder, "strike") / 1000.0; 
						double price = (bid + offer) / 2.0;
						double volume = Double.parseDouble(option.getStringTypeVariable(optionInfoHolder, "volume")) ;
						double delta = (double) option.getVariable(optionInfoHolder, "delta");
						String cp = (String) option.getVariable(optionInfoHolder, "cp");

						Option glOption;

						if (cp.equals("C")) 
							glOption = new CallOption("BS");
						else
							glOption = new PutOption("BS");

						double[] variableValues = 
							{sharePrice, strike, rfr, tau, dividend, price, volume, delta};

						glOption.set(variableNames, variableValues);

						double spread = offer - price;
						double moneyness = strike / sharePrice;
						double impVol = glOption.getBSImpVol();
						
						System.out.println(price + ", " + spread); 
						
						int rowNumber = getRowNumber(moneyness);
						int columnNumber = getColumnNumber(tau, cp);

						prices[rowNumber][columnNumber] += price;
						spreads[rowNumber][columnNumber] += spread;
						numbers[rowNumber][columnNumber] ++;
						ivs[rowNumber][columnNumber] += impVol;

					}

				}

				PublicWriter.write(getResult());
				Console.quit();
				
			}
	
			else
				System.out.println("Not the chosen day");
			
		}

		toc();
		
	}

	private boolean checkPeriod() {
		
		boolean result = true;
		
		if (arguments.size() >= 1 && 
				arguments.get(0).equals(date) == false)
			result = false;
		
		return result;
		
	}
	
	private int getColumnNumber(double tau, String cp) {

		if (tau * 12.0 < 2.0) {
			if (cp.equals("C")) 
				return 0;
			else
				return 1;
		}

		else if (tau * 12.0 < 6.0) {
			if (cp.equals("C")) 
				return 2;
			else
				return 3;
		}

		else {
			if (cp.equals("C")) 
				return 4;
			else
				return 5;
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

			dividendRate *= (1 + rates.get(i)) ;

		}

		return Math.pow(dividendRate, 1.0 / (double) rates.size()) - 1.0;

	}

	private String getResult() {

		String result = date + ",";
		
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 6; j++) {
				result += (prices[i][j] + ",");
			}
		}
		
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 6; j++) {
				result += (spreads[i][j] + ",");
			}
		}
		
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 6; j++) {
				result += (numbers[i][j] + ",");
			}
		}
		
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 6; j++) {
				result += ivs[i][j];
				if (i < 5 || j < 5)
					result += ",";
			}
		}
		
		return result;
		
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

	private int getRowNumber(double moneyness) {

		if (moneyness < 0.7)
			return 0;
		else if (moneyness < 0.85)
			return 1;
		else if (moneyness < 1.0)
			return 2;
		else if (moneyness < 1.15)
			return 3;
		else if (moneyness < 1.3)
			return 4;
		else 
			return 5;

	}

	private void setDayVariables(int dayIndex) throws ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException, 
	BadLocationException, NoSuchFieldException {

		day = dayDataClassArray.get(dayIndex);
		date = day.getStringTypeVariable(dayInfoHolder, "date");

		dateCalendar = day.getDateVariable(dayInfoHolder, "date");
		rfrDay = getRfrDay(date);
		share = day.getObservationVariable(dayInfoHolder, "share");
		exdates = day.getObListVariable(dayInfoHolder, "exdates");

		sharePrice = 
				Math.abs((double) share.getVariable(shareInfoHolder, "value"));

	}

	private void setExdateVariables(int exdatesIndex) throws ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException, 
	BadLocationException, NoSuchFieldException, FileNotFoundException, IOException {

		exdate = exdates.get(exdatesIndex);
		exdateCalendar = exdate.getDateVariable(exdateInfoHolder, "exdate");

		rfr = Thesis1.getRfr
				(rfrDay, rfrInfoHolder, rateInfoHolder, dateCalendar, exdateCalendar);
		dividend = getDividendRate(dateCalendar, exdateCalendar);
		tau = Thesis1.getTdayTau(dateCalendar, exdateCalendar, tdayDataClassArray);

		options = 
				exdate.getObListVariable(exdateInfoHolder, "options");	

	}

	private void setResultArray() {
		
		prices = new double[6][6];
		spreads = new double[6][6];
		numbers = new int[6][6];
		ivs = new double[6][6];
		
		for (int i = 0; i < 6; i++) {
		
			for (int j = 0; j < 6; j++) {
				
				prices[i][j] = 0;
				spreads[i][j] = 0;
				numbers[i][j] = 0;
				ivs[i][j] = 0;
				
			}
			
		}
		
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
