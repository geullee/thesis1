package kr.geul.thesis1.command;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.text.BadLocationException;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.exception.TooManyEvaluationsException;

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
import kr.geul.thesis1.IVEstimator;
import kr.geul.thesis1.Thesis1;

public class GetBKM_spi_estimate extends Command {

	static final double extrapolationMultiplier = 3.0,
			strikePriceGap = 0.1,
			precisionMultiplier = 1.0 / strikePriceGap; 
	static final double[] targetTimeToMaturities = {2.0 / 12.0, 4.0 / 12.0};

	static final String[] variableNames = {"S", "K", "R", "T", "D", "C", "V", "delta"};

	BKMEstimator bkmEstimator;
	DataClassArray dayDataClassArray, dividendDataClassArray, rfrDataClassArray, tdayDataClassArray;
	DataClassInfoHolder dayInfoHolder, exdateInfoHolder, optionInfoHolder, rfrInfoHolder,
	dividendInfoHolder, rateInfoHolder, shareInfoHolder;
	String fileName;

	/* Variables that are renewed for each day */
	double sharePrice;
	double[] kMin, kMax, timeToMaturity, volume;
	double[][] bkmEstimates, bkmEstimatesExtrapolated, bkmEstimatesTrim99, bkmEstimatesExtrapolatedTrim99, 
	bkmEstimatesTrim95, bkmEstimatesExtrapolatedTrim95, bkmEstimatesTrim90, bkmEstimatesExtrapolatedTrim90, 
	endPoints, endPoints99, endPoints95, endPoints90;
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

	public GetBKM_spi_estimate(ArrayList<String> arguments) {
		super(arguments);	
	}


	@Override
	protected String getCommandName() {
		return "bkms_estimate";
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
					setOptions(exdatesIndex);
					//					setOptions_numberEqualized(exdatesIndex);
					System.out.println("Exdate " + Thesis1.toDateString(exdateCalendar) + " is being processed.");

				}

				writeLog("An implied volatility surface is constructed.");

				getTruncationLocations();
				fixTimeToMaturities();
				interpolate(true);
				bkmEstimates = getEstimate();
				endPoints = getEndPoints();
				extrapolate();
				bkmEstimatesExtrapolated = getEstimate();
				surface.trim(getTrimmingLocations(-2.5822, 1.1159, -2.1783, 1.0615));
				endPoints90 = getEndPoints();
				bkmEstimatesTrim90 = getEstimate();
				extrapolate();
				bkmEstimatesExtrapolatedTrim90 = getEstimate();
				surface.trim(getTrimmingLocations(-2.1867, 1.0496, -1.8775, 0.9770));
				endPoints95 = getEndPoints();
				bkmEstimatesTrim95 = getEstimate();
				extrapolate();
				bkmEstimatesExtrapolatedTrim95 = getEstimate();
				surface.trim(getTrimmingLocations(-1.6823, 0.8925, -1.1734, 0.6911));
				endPoints99 = getEndPoints();
				bkmEstimatesTrim99 = getEstimate();
				extrapolate();
				bkmEstimatesExtrapolatedTrim99 = getEstimate();
				printResults();

				toc();
				Console.quit();
			}

			else
				toc();

		}

	}

	private double[][] getTrimmingLocations(double kMin2Multiplier, double kMax2Multiplier,
			double kMin4Multiplier, double kMax4Multiplier) {
		
		ArrayList<OptionCurve> curves = surface.getCurves();
		double[][] locations = new double[curves.size()][2];
		
		for (int i = 0; i < curves.size(); i++) {
			OptionCurve curve = curves.get(i);
			double sharePrice = curve.getVariableArray()[0];
			double maturity = curve.getVariableArray()[1];
			double vol = bkmEstimatesExtrapolated[i][0];
			
			if (i == 0) {
				locations[i][0] = sharePrice * Math.exp(kMin2Multiplier * vol * Math.sqrt(maturity));
				locations[i][1] = sharePrice * Math.exp(kMax2Multiplier * vol * Math.sqrt(maturity));
			}
			
			else {
				locations[i][0] = sharePrice * Math.exp(kMin4Multiplier * vol * Math.sqrt(maturity));
				locations[i][1] = sharePrice * Math.exp(kMax4Multiplier * vol * Math.sqrt(maturity));				
			}
			
		}
		
		return locations;
		
	}


	private boolean checkPeriod() {

		boolean result = true;

		System.out.println(arguments.get(1) + " : " + date);

		if (arguments.size() >= 2 && 
				arguments.get(1).equals(date) == false)
			result = false;

		return result;

	}


	private double[][] getEstimate() throws DuplicateOptionsException, InconsistentOptionException {

		ArrayList<OptionCurve> curves = surface.getCurves();

		double[][] estimates = new double[curves.size()][3];

		for (int i = 0; i < estimates.length; i++) {
			OptionCurve curve = curves.get(i);
			bkmEstimator.setOptions(curve);
			estimates[i] = bkmEstimator.getEstimates();
		}

		return estimates;

	}


	private void extrapolate() throws TooManyEvaluationsException, InvalidArgumentException, 
	InconsistentArgumentLengthException, DuplicateOptionsException, InconsistentOptionException, 
	AtTheMoneyException, FileNotFoundException {

		double kMin = 
				Math.round(sharePrice / extrapolationMultiplier * precisionMultiplier) / 
				precisionMultiplier, 
				kMax = 
				Math.round(sharePrice * extrapolationMultiplier * precisionMultiplier) / 
				precisionMultiplier;

		surface.setStrikePriceGap(strikePriceGap);
		surface.setExtrapolationRange(kMin, kMax);
		surface.extrapolate(precisionMultiplier);

		writeLog("Extrapolation has been completed.");
		ArrayList<OptionCurve> curves = surface.getCurves();

		for (int i = 0; i < curves.size(); i++) {
			OptionCurve curve = curves.get(i);
			double[] strikePrices = curve.getStrikePrices();
			writeLog("Volatility curve for T = " + curve.getVariableArray()[1] + ": " +
					strikePrices.length + " options, K_min: " + strikePrices[0] +
					", K_max: " + strikePrices[strikePrices.length - 1]);

		}

	}

	private void fixTimeToMaturities() throws FileNotFoundException, InvalidArgumentException, InconsistentArgumentLengthException, AtTheMoneyException, DuplicateOptionsException, InconsistentOptionException {

		surface.fixTimeToMaturities(targetTimeToMaturities, kMinFunction, kMaxFunction,
				precisionMultiplier);

		writeLog("Time to maturities have been fixed.");
		ArrayList<OptionCurve> curves = surface.getCurves();

		for (int i = 0; i < curves.size(); i++) {

			OptionCurve curve = curves.get(i);
			double[] strikePrices = curve.getStrikePrices();
			writeLog("Volatility curve for T = " + curve.getVariableArray()[1] + ": " +
					strikePrices.length + " options, K_min: " + strikePrices[0] +
					", K_max: " + strikePrices[strikePrices.length - 1]);

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

	private double[][] getEndPoints() {

		ArrayList<OptionCurve> curves = surface.getCurves();
		double[] timeToMaturity = getTimeToMaturities(curves);
		double[][] endPoints = new double[timeToMaturity.length][2];

		for (int i = 0; i < timeToMaturity.length; i++) {

			double[] strikePrices = curves.get(i).getStrikePrices(); 
			endPoints[i][0] = strikePrices[0];
			endPoints[i][1] = strikePrices[strikePrices.length - 1];

		}

		return endPoints;

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

	private double[] getTimeToMaturities(ArrayList<OptionCurve> curves) {

		ArrayList<Double> ttmArray = new ArrayList<Double>();

		for (int i = 0; i < curves.size(); i++) {
			OptionCurve curve = curves.get(i);
			ttmArray.add(curve.getVariableArray()[1]);
		}

		double[] ttm = new double[ttmArray.size()];

		for (int i = 0; i < ttm.length; i++) {
			ttm[i] = ttmArray.get(i);
		}

		return ttm;

	}

	private void getTruncationLocations() throws FileNotFoundException, InvalidArgumentException {

		ArrayList<OptionCurve> curves = surface.getCurves();
		timeToMaturity = getTimeToMaturities(curves);
		kMin = new double[curves.size()];
		kMax = new double[curves.size()];
		volume = new double[curves.size()]; 

		for (int i = 0; i < curves.size(); i++) {

			OptionCurve curve = curves.get(i);

			// Getting truncation locations
			double[] strikePrices = curve.getStrikePrices();
			kMin[i] = Math.round((double) strikePrices[0] * precisionMultiplier) / precisionMultiplier;
			kMax[i] = Math.round((double) strikePrices[strikePrices.length - 1] * precisionMultiplier) / precisionMultiplier;
			System.out.println("Volatility curve for T = " + curve.getVariableArray()[1] + ": " +
					strikePrices.length + " options, K_min: " + kMin[i] +
					", K_max: " + kMax[i]);

		}

		UnivariateInterpolator interpolator = new LinearInterpolator();
		kMinFunction = interpolator.interpolate(timeToMaturity, kMin);
		kMaxFunction = interpolator.interpolate(timeToMaturity, kMax);

	}

	private void interpolate(boolean isATMOptionsGenerated) throws InvalidArgumentException, InconsistentArgumentLengthException, 
	AtTheMoneyException, DuplicateOptionsException, InconsistentOptionException, 
	FileNotFoundException {

		surface.setStrikePriceGap(strikePriceGap);
		surface.interpolate(precisionMultiplier, isATMOptionsGenerated);

	}

	private void printResults() throws FileNotFoundException {

		ArrayList<OptionCurve> curves = surface.getCurves();
		timeToMaturity = getTimeToMaturities(curves);

		for (int i = 0; i < timeToMaturity.length; i++) {

			double tau = timeToMaturity[i];

			PublicWriter.write(date + "," + sharePrice + "," + tau + "," + endPoints[i][0] + "," +
					endPoints[i][1] + "," + endPoints99[i][0] + "," +
					endPoints99[i][1] + "," + endPoints95[i][0] + "," +
					endPoints95[i][1] + "," + endPoints90[i][0] + "," +
					endPoints90[i][1] + "," + bkmEstimates[i][0] + "," + bkmEstimates[i][1] + "," + 
					bkmEstimates[i][2] + "," + bkmEstimatesExtrapolated[i][0] + "," +
					bkmEstimatesExtrapolated[i][1] + "," + bkmEstimatesExtrapolated[i][2] + 
					"," + bkmEstimatesTrim99[i][0] + "," + bkmEstimatesTrim99[i][1] + "," + 
					bkmEstimatesTrim99[i][2] + "," + bkmEstimatesExtrapolatedTrim99[i][0] + "," +
					bkmEstimatesExtrapolatedTrim99[i][1] + "," + bkmEstimatesExtrapolatedTrim99[i][2] + 
					"," + bkmEstimatesTrim95[i][0] + "," + bkmEstimatesTrim95[i][1] + "," + 
					bkmEstimatesTrim95[i][2] + "," + bkmEstimatesExtrapolatedTrim95[i][0] + "," +
					bkmEstimatesExtrapolatedTrim95[i][1] + "," + bkmEstimatesExtrapolatedTrim95[i][2] + 
					"," + bkmEstimatesTrim90[i][0] + "," + bkmEstimatesTrim90[i][1] + "," + 
					bkmEstimatesTrim90[i][2] + "," + bkmEstimatesExtrapolatedTrim90[i][0] + "," +
					bkmEstimatesExtrapolatedTrim90[i][1] + "," + bkmEstimatesExtrapolatedTrim90[i][2]);

		}

	}

	private void printResults_simple() throws FileNotFoundException {

		ArrayList<OptionCurve> curves = surface.getCurves();
		timeToMaturity = getTimeToMaturities(curves);

		for (int i = 0; i < timeToMaturity.length; i++) {

			double[] strikePrices = curves.get(i).getStrikePrices(); 
			double tau = timeToMaturity[i];

			PublicWriter.write(date + "," + sharePrice + "," + tau + "," + kMin + "," +
					kMax + "," + (endPoints[i][0] / sharePrice) + "," + (endPoints[i][1] / sharePrice) + "," +
					Math.log(endPoints[i][0] / sharePrice) + "," + Math.log(endPoints[i][1] / sharePrice) + 
					"," + strikePrices.length);

		}

	}

	private void printResultsToLog() throws InvalidArgumentException, InconsistentArgumentLengthException, FileNotFoundException {

		ArrayList<OptionCurve> curves = surface.getCurves();
		double[] strikes = curves.get(0).getStrikePrices();
		ArrayList<Double> ttmArray = new ArrayList<Double>();

		for (int i = 0; i < curves.size(); i++) {
			ttmArray.add(curves.get(i).getVariableArray()[1]);
		}

		double[] ttm = new double[ttmArray.size()]; 

		for (int i = 0; i < ttm.length; i++) {
			ttm[i] = ttmArray.get(i);
		}

		double[][] ivs = new double[strikes.length][ttm.length];
		double[][] prices = new double[strikes.length][ttm.length];

		for (int i = 0; i < curves.size(); i++) {

			ArrayList<Option> options = curves.get(i).getOptions();

			for (int j = 0; j < options.size(); j++) {
				ivs[j][i] = options.get(j).get("sigma");
				prices[j][i] = options.get(j).getOptionPrice();
			}

		}

		writeLog("<STRIKES>");

		String strikeStr = "";

		for (int i = 0; i < strikes.length; i++) {
			strikeStr += ("," + strikes[i]);
		}

		writeLog(strikeStr);

		writeLog("<TTMS>");
		String ttmStr = "";

		for (int i = 0; i < ttm.length; i++) {
			ttmStr += ("," + ttm[i]);
		}

		writeLog(ttmStr);

		writeLog("<IVS>");

		for (int i = 0; i < ivs.length; i++) {

			String ivsStr = "";

			for (int j = 0; j < ivs[0].length; j++) {
				ivsStr += ("," + (ivs[i][j] > 0 ? ivs[i][j] : "NaN"));
			}

			writeLog(ivsStr);

		}

		writeLog("<PRICES>");

		for (int i = 0; i < prices.length; i++) {

			String priceStr = "";

			for (int j = 0; j < prices[0].length; j++) {
				priceStr += ("," + (ivs[i][j] > 0 ? prices[i][j] : "NaN"));
			}

			writeLog(priceStr);

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

	private void setOptions(int exdatesIndex) throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException, BadLocationException, InvalidArgumentException, 
	InconsistentArgumentLengthException, AtTheMoneyException, InconsistentOptionException, 
	DuplicateOptionsException {

		int numberOfCalls = 0, numberOfPuts = 0;
		Integer[] numbersArray = new Integer[2];

		for (int optionsIndex = 0; optionsIndex < options.size(); optionsIndex++) {

			Observation option = options.get(optionsIndex);

			double bid = (double) option.getVariable(optionInfoHolder, "bid");
			double offer = (double) option.getVariable(optionInfoHolder, "offer");
			double strike = (double) option.getVariable(optionInfoHolder, "strike") / 1000.0; 
			double price = (bid + offer) / 2.0;
			double volume = Double.parseDouble(option.getStringTypeVariable(optionInfoHolder, "volume")) ;
			double delta = (double) option.getVariable(optionInfoHolder, "delta");
			String cp = (String) option.getVariable(optionInfoHolder, "cp");

			//			System.out.println(bid + "," + offer
			//					+ "," + strike + "," + price + "," + cp);

			Option glOption;

			if (cp.equals("C")) {
				glOption = new CallOption();
				numberOfCalls++;
			}

			else {
				glOption = new PutOption();
				numberOfPuts++;
			}

			double[] variableValues = 
				{sharePrice, strike, rfr, tau, dividend, price, volume, delta};

			glOption.set(variableNames, variableValues);
			curve.add(glOption);

		}

		if (numberOfCalls > 2 && numberOfPuts > 2) {
			numbersArray[0] = numberOfCalls;
			numbersArray[1] = numberOfPuts;
			numberOfOptions.add(numbersArray);
			surface.add(curve);
			System.out.println("calls: " + numberOfCalls + ", puts: "
					+ numberOfPuts + " [INCLUDED - SURFACE SIZE " + numberOfOptions.size() + "]");
		}

		else 
			System.out.println("calls: " + numberOfCalls + ", puts: "
					+ numberOfPuts + " [EXCLUDED] - SURFACE SIZE " + numberOfOptions.size() + "]");

	}

	private void setOptions_numberEqualized(int exdatesIndex) throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException, BadLocationException, InvalidArgumentException, 
	InconsistentArgumentLengthException, AtTheMoneyException, InconsistentOptionException, 
	DuplicateOptionsException {

		int numberOfCalls = 0, numberOfPuts = 0;
		OptionCurve temporaryCurve = new OptionCurve();
		Integer[] numbersArray = new Integer[2];

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
				glOption = new CallOption();

			else 
				glOption = new PutOption();

			double[] variableValues = 
				{sharePrice, strike, rfr, tau, dividend, price, volume, delta};

			glOption.set(variableNames, variableValues);
			temporaryCurve.add(glOption);

		}

		OptionCurve temporaryCallCurve = temporaryCurve.getCallCurve(),
				temporaryPutCurve = temporaryCurve.getPutCurve();

		int smallerNumber = Math.min(temporaryCallCurve.size(), temporaryPutCurve.size());

		for (int i = 0; i < smallerNumber; i++) {
			curve.add(temporaryCallCurve.get(i));
			curve.add(temporaryPutCurve.get(temporaryPutCurve.size() - (i + 1)));
			numberOfCalls++;
			numberOfPuts++;
		}

		if (numberOfCalls > 2 && numberOfPuts > 2) {
			numbersArray[0] = numberOfCalls;
			numbersArray[1] = numberOfPuts;
			numberOfOptions.add(numbersArray);
			surface.add(curve);
			System.out.println("calls: " + curve.getCallCurve().size() + ", puts: "
					+ curve.getPutCurve().size() + " [INCLUDED - SURFACE SIZE " + numberOfOptions.size() + "]");
		}

		else 
			System.out.println("calls: " + curve.getCallCurve().size() + ", puts: "
					+ curve.getPutCurve().size() + " [EXCLUDED] - SURFACE SIZE " + numberOfOptions.size() + "]");

	}
	
}
