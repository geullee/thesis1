package kr.geul.thesis1.command;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.text.BadLocationException;

import kr.geul.calibration.BSCalibrator;
import kr.geul.calibration.Calibrator;
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
import kr.geul.options.exception.InconsistentComponentException;
import kr.geul.options.exception.InconsistentOptionException;
import kr.geul.options.exception.InvalidArgumentException;
import kr.geul.options.option.CallOption;
import kr.geul.options.option.Option;
import kr.geul.options.option.PutOption;
import kr.geul.options.parameterset.BSParameterSet;
import kr.geul.options.parameterset.ParameterSet;
import kr.geul.options.structure.OptionCurve;
import kr.geul.options.structure.OptionSurface;
import kr.geul.thesis1.Thesis1;

@SuppressWarnings("unused")
public class GetParameterEstimates_daily_estimate_bs extends Command {

	static final double calibrationPrecision = -1.0;
	static final int calibrationPopulationMultiplier = 1;
	static final int calibrationIterationMultiplier = 100;
							
	static final double[] calibrationSigma = {0.2};
	static final String[] variableNames = {"S", "K", "R", "T", "D", "C"};
	static final String[] parameterNames = {"sigma"};
	
	static double[] parameterValues = {0.3};

	Calibrator calibrator;
	DataClassArray dayDataClassArray, dividendDataClassArray, rfrDataClassArray, tdayDataClassArray;
	DataClassInfoHolder dayInfoHolder, exdateInfoHolder, optionInfoHolder, rfrInfoHolder,
	dividendInfoHolder, rateInfoHolder, shareInfoHolder;
	OptionSurface surface;
	OptionCurve curve;
	ParameterSet parameterSet;
	PrintWriter printWriter;
	
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
	
	/* Variables that are renewed for each option */
	double bid, offer, strike, price;	
	Observation option;
	Option glOption;
	String cp;
	
	public GetParameterEstimates_daily_estimate_bs(ArrayList<String> arguments) 
			throws InconsistentComponentException {
		super(arguments);	
	}


	@Override
	protected String getCommandName() {
		return "ped_bs_estimate";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		return null;
	}

	@Override
	protected void runCommand() throws Exception {
		
		tic();
		
		setup();
		applyArguments();			
		
		for (int dayIndex = 0; dayIndex < dayDataClassArray.size(); dayIndex++) {

			updateProgress(dayIndex, dayDataClassArray.size());
			setDayVariables(dayIndex);

			for (int exdatesIndex = 0; 
					exdatesIndex < exdates.size(); exdatesIndex++) {		

				setExdateVariables(exdatesIndex);
				
				for (int optionsIndex = 0; optionsIndex < options.size(); optionsIndex++) {
					setOption(optionsIndex);					
				}
				
				surface.add(curve);

			}
			
			if (checkPeriod() == true) {
				
				printCalibrationResults(getCalibrationResults());
				toc();
				Console.quit();
				
			}
			
			else
				toc();
		
		}

		System.out.println("DONE ");
		
		
	}

	private void printCalibrationResults(double[] calibrationResults) 
			throws FileNotFoundException, InvalidArgumentException, InconsistentArgumentLengthException {
		
		int numberOfOptions = getNumberOfOptions();
		String output = date + "," + numberOfOptions + ",";

		for (int i = 0; i < calibrationResults.length; i++) {

			output += calibrationResults[i];
			if (i < calibrationResults.length - 1)
				output += ",";	

		}

		PublicWriter.write(output);
		PublicWriter.closeWriter();
		
	}

	private void applyArguments() {
		if (arguments.size() == 3) 
			parameterValues[0] = Double.parseDouble(arguments.get(2));
	}

	private boolean checkPeriod() {
		
		boolean result = true;
		
		if (arguments.size() >= 2 && 
				arguments.get(1).equals(date) == false)
			result = false;
		
		return result;
		
	}

	private double[] getCalibrationResults() throws InvalidArgumentException, 
	InconsistentArgumentLengthException, InconsistentComponentException {
			
		calibrator.setSurface(surface);
		calibrator.setOptimizerSigma(calibrationSigma);
		
		return calibrator.optimizeCMAES
				(calibrationPopulationMultiplier * calibrationIterationMultiplier, 
						calibrationPopulationMultiplier, -1.0, calibrationPrecision);
		
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

	private int getNumberOfOptions() throws InvalidArgumentException, InconsistentArgumentLengthException {
		
		int result = 0;
		ArrayList<OptionCurve> curves = surface.getCurves();
		
		for (int i = 0; i < curves.size(); i++) {
			result += curves.get(i).getOptions().size();
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

	private void setDayVariables(int dayIndex) throws ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException, 
	BadLocationException, NoSuchFieldException {
		
		day = dayDataClassArray.get(dayIndex);
		date = day.getStringTypeVariable(dayInfoHolder, "date");
		dateCalendar = day.getDateVariable(dayInfoHolder, "date");
		rfrDay = getRfrDay(date);
		share = day.getObservationVariable(dayInfoHolder, "share");
		exdates = 
				day.getObListVariable(dayInfoHolder, "exdates");

		sharePrice = 
				Math.abs((double) share.getVariable(shareInfoHolder, "value"));
		
		surface = new OptionSurface();
		
	}
	
	private void setExdateVariables(int exdatesIndex) throws NoSuchFieldException, 
	SecurityException, ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, BadLocationException, FileNotFoundException, IOException {
		
		curve = new OptionCurve();
		exdate = exdates.get(exdatesIndex);
		options = 
				exdate.getObListVariable(exdateInfoHolder, "options");	
		exdateCalendar = exdate.getDateVariable(exdateInfoHolder, "exdate");
		rfr = Thesis1.getRfr
				(rfrDay, rfrInfoHolder, rateInfoHolder, dateCalendar, exdateCalendar);
		dividend = getDividendRate(dateCalendar, exdateCalendar);
		tau = Thesis1.getTdayTau(dateCalendar, exdateCalendar, tdayDataClassArray);
		
	}

	private void setOption(int optionsIndex) throws ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException, 
	BadLocationException, InvalidArgumentException, InconsistentArgumentLengthException, 
	AtTheMoneyException, DuplicateOptionsException, InconsistentOptionException {
		
		option = options.get(optionsIndex);

		cp = (String) option.getVariable(optionInfoHolder, "cp");
		bid = (double) option.getVariable(optionInfoHolder, "bid");
		offer = (double) option.getVariable(optionInfoHolder, "offer");
		strike = (double) option.getVariable(optionInfoHolder, "strike") / 1000.0; 
		price = (bid + offer) / 2.0;

		if (cp.equals("C")) 
			glOption = new CallOption();
		else
			glOption = new PutOption();

		double[] variableValues = 
			{sharePrice, strike, rfr, tau, dividend, price};

		glOption.set(variableNames, variableValues);
		curve.add(glOption);	
		
	}
	
	private void setup() throws ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException, 
	BadLocationException, InconsistentComponentException, InvalidArgumentException, 
	InconsistentArgumentLengthException {
		
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

		calibrator = new BSCalibrator();
		parameterSet = new BSParameterSet();
		parameterSet.set(parameterNames, parameterValues);
		calibrator.setParameterSet(parameterSet);
		
	}
	
}
