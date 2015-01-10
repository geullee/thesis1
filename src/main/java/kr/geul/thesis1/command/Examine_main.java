package kr.geul.thesis1.command;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.text.BadLocationException;

import org.apache.commons.math3.analysis.UnivariateFunction;

import kr.geul.bkm.BKMEstimator;
import kr.geul.console.Console;
import kr.geul.console.PublicWriter;
import kr.geul.console.command.Command;
import kr.geul.dataobject.DataClass;
import kr.geul.dataobject.DataClassArray;
import kr.geul.dataobject.DataClassInfoHolder;
import kr.geul.dataobject.InfoHolder;
import kr.geul.dataobject.ObList;
import kr.geul.dataobject.Observation;
import kr.geul.options.exception.AtTheMoneyException;
import kr.geul.options.exception.DuplicateOptionsException;
import kr.geul.options.exception.InconsistentArgumentLengthException;
import kr.geul.options.exception.InconsistentOptionException;
import kr.geul.options.exception.InvalidArgumentException;
import kr.geul.options.option.CallOption;
import kr.geul.options.option.Option;
import kr.geul.options.option.PutOption;
import kr.geul.options.structure.OptionCurve;
import kr.geul.options.structure.OptionSurface;
import kr.geul.thesis1.Thesis1;

public class Examine_main extends Command {

	static final double extrapolationMultiplier = 3.0,
			strikePriceGap = 2.5,
			precisionMultiplier = 1.0 / strikePriceGap; 
	static final double[] targetTimeToMaturities = {1.0 / 12.0, 2.0 / 12.0, 3.0 / 12.0, 
		4.0 / 12.0, 5.0 / 12.0, 6.0 / 12.0, 7.0 / 12.0, 8.0 / 12.0, 9.0 / 12.0,
		10.0 / 12.0, 11.0 / 12.0, 12.0 / 12.0};
	static final String[] variableNames = {"S", "K", "R", "T", "D", "C", "V", "delta"};

	BKMEstimator bkmEstimator;
	DataClassArray dayDataClassArray, dividendDataClassArray, rfrDataClassArray, tdayDataClassArray;
	DataClassInfoHolder dayInfoHolder, exdateInfoHolder, optionInfoHolder, rfrInfoHolder,
	dividendInfoHolder, rateInfoHolder, shareInfoHolder;
	String fileName;

	/* Variables that are renewed for each day */
	double sharePrice;
	double[] kMin, kMax, timeToMaturity, volume;
	double[][] bkmEstimates, bkmEstimatesExtrapolated, endPoints;
	ArrayList<Integer[]> numberOfOptions;
	
	Calendar dateCalendar;
	ObList exdates;
	Observation day, rfrDay, share;
	OptionSurface surface;
	String date;
	UnivariateFunction kMinFunction, kMaxFunction;	

	/* Variables that are renewed for each time to maturity */
	double rfr, dividend, tau;

	Calendar exdateCalendar;
	Observation exdate;
	ObList options;
	OptionCurve curve;

	public Examine_main(ArrayList<String> arguments) {
		super(arguments);	
	}


	@Override
	protected String getCommandName() {
		return "exam_main";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		return null;
	}

	@Override
	protected void runCommand() throws Exception {

		fileName = arguments.get(0);
		bkmEstimator = new BKMEstimator();

		setupArrayAndInfoHolder();

		tic();

		Console.printMessage("Running function 'bkms' for "
				+ fileName + ".cl...");

		for (int dayIndex = 0; dayIndex < dayDataClassArray.size(); dayIndex++) {

			updateProgress(dayIndex, dayDataClassArray.size());
			setDayVariables(dayIndex);

			if (checkPeriod() == true) {

				System.out.println("Date " + date + " is being processed.");
				numberOfOptions = new ArrayList<Integer[]>();
				
				for (int exdatesIndex = 0; 
						exdatesIndex < exdates.size(); exdatesIndex++) {

					setExdateVariables(exdatesIndex);
					printInfo();
					System.out.println("Exdate " + Thesis1.toDateString(exdateCalendar) + " is being processed.");

				}
				
				toc();
				Console.quit();

			}

			else
				toc();

		}

	}

	private boolean checkPeriod() {

		boolean result = true;

		System.out.println(arguments.get(1) + " : " + date);

		if (arguments.size() >= 2 && 
				arguments.get(1).equals(date) == false)
			result = false;

		return result;

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

	private void printInfo() throws FileNotFoundException, InvalidArgumentException, InconsistentArgumentLengthException, ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, BadLocationException, AtTheMoneyException {
		
		for (int optionsIndex = 0; optionsIndex < options.size(); optionsIndex++) {

			Observation option = options.get(optionsIndex);

			double bid = (double) option.getVariable(optionInfoHolder, "bid");
			double offer = (double) option.getVariable(optionInfoHolder, "offer");
			double strike = (double) option.getVariable(optionInfoHolder, "strike") / 1000.0; 
			double price = (bid + offer) / 2.0;
			double volume = Double.parseDouble(option.getStringTypeVariable(optionInfoHolder, "volume")) ;
			double delta = (double) option.getVariable(optionInfoHolder, "delta");
			String cp = (String) option.getVariable(optionInfoHolder, "cp");

			double bidIV, askIV, midIV;
			
			Option glOption;

			if (cp.equals("C"))
				glOption = new CallOption();
			
			else
				glOption = new PutOption();

			double[] variableValuesBid = 
				{sharePrice, strike, rfr, tau, dividend, bid, volume, delta};
			glOption.set(variableNames, variableValuesBid);
			bidIV = glOption.getBSImpVol();

			double[] variableValuesAsk = 
				{sharePrice, strike, rfr, tau, dividend, offer, volume, delta};
			glOption.set(variableNames, variableValuesAsk);
			askIV = glOption.getBSImpVol();
			
			double[] variableValuesMid = 
				{sharePrice, strike, rfr, tau, dividend, price, volume, delta};
			glOption.set(variableNames, variableValuesMid);
			midIV = glOption.getBSImpVol();
			
//			String line = date + "," + sharePrice + "," + rfr + "," + dividend + "," + tau + "," + strike + "," + 
//					bid + "," + offer + "," + price;
			
			String line = date + "," + sharePrice + "," + tau + "," + strike + "," + 
					bid + "," + offer + "," + price + "," + bidIV + "," + askIV + "," + midIV;
			
			System.out.println(line);
			PublicWriter.write(line);
			
		}
		
	}

	private void setupArrayAndInfoHolder() throws ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException, 
	BadLocationException {

		dayDataClassArray = DataClass.getDataClassArray("day");
		dividendDataClassArray = DataClass.getDataClassArray("dividendRate");
		rfrDataClassArray = DataClass.getDataClassArray("rfrDay");
		tdayDataClassArray = DataClass.getDataClassArray("tday");
		dayInfoHolder = InfoHolder.getInfoHolder("day");
		exdateInfoHolder = InfoHolder.getInfoHolder("exdate");
		optionInfoHolder = InfoHolder.getInfoHolder("option");
		dividendInfoHolder = InfoHolder.getInfoHolder("dividend");
		rfrInfoHolder = InfoHolder.getInfoHolder("rfr");
		rateInfoHolder = InfoHolder.getInfoHolder("rate");
		shareInfoHolder = InfoHolder.getInfoHolder("share");

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

		surface = new OptionSurface();

	}

	private void setExdateVariables(int exdatesIndex) throws ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException, 
	BadLocationException, NoSuchFieldException, FileNotFoundException, IOException {

		curve = new OptionCurve();

		exdate = exdates.get(exdatesIndex);
		exdateCalendar = exdate.getDateVariable(exdateInfoHolder, "exdate");

		rfr = Thesis1.getRfr
				(rfrDay, rfrInfoHolder, rateInfoHolder, dateCalendar, exdateCalendar);
		dividend = getDividendRate(dateCalendar, exdateCalendar);
		tau = Thesis1.getTdayTau(dateCalendar, exdateCalendar, tdayDataClassArray);

		options = 
				exdate.getObListVariable(exdateInfoHolder, "options");	

	}

}
