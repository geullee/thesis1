package kr.geul.thesis1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.text.BadLocationException;

import kr.geul.console.Console;
import kr.geul.dataobject.DataClassArray;
import kr.geul.dataobject.VarList;

public class BKMEstimator {

	private static Calendar today, exdate;
	private static double sharePrice, rfr, tau, dividendRate;	
	private static ArrayList<Calendar> dividendDates;
	private static ArrayList<Double> dividendAmounts, convertedDividendAmounts, convertedDividendDates;
	private static ArrayList<double[]> options, calls, puts;
	private static boolean isInInitialState;
	private final static double moneynessGap = 0.001;
	private final static double nominalGap = 0.01;
	
	public static void addOption(double[] option) throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException {

		if (option.length == 5) {

			if (option[0] <= 0) 
				Console.printErrorMessage("Strike price must be positive.", 
						Thread.currentThread().getStackTrace()[1].getClassName());

			else if (option[1] <= 0) 
				Console.printErrorMessage("Option price must be positive.", 
						Thread.currentThread().getStackTrace()[1].getClassName());

			else if (option[2] <= 0) 
				Console.printErrorMessage("Implied volatility must be positive.", 
						Thread.currentThread().getStackTrace()[1].getClassName());

			else if (option[3] < 0) 
				Console.printErrorMessage("Trading volume must be nonnegative.", 
						Thread.currentThread().getStackTrace()[1].getClassName());
			
			else {

				int addingLocation = 0; 
				double strikePrice = option[0];

				for (int i = 0; i < options.size(); i++) {

					double[] optionCompared = options.get(i);

					if (strikePrice > optionCompared[0])
						addingLocation++;

				}

				options.add(addingLocation, option);
				isInInitialState = false;

			}

		}

		else
			Console.printErrorMessage("Option data being passed to BKMEstimator must be fivefold.", 
					Thread.currentThread().getStackTrace()[1].getClassName());

	}

	public static void convertDates(DataClassArray tdayDataClassArray) 
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException, FileNotFoundException, IOException, 
			BadLocationException {

		//		long gap = exdate.getTimeInMillis() - today.getTimeInMillis();			
		//		tau = (double) gap / (double) Function.ONEYEARINMILLIS;

		tau = OptionFilter.getTdayTau(today, exdate, tdayDataClassArray);

		for (int i = 0; i < dividendDates.size(); i++) {

			Calendar dividendDate = dividendDates.get(i);
			double dividendAmount = dividendAmounts.get(i);

			if (dividendDate.getTimeInMillis() < exdate.getTimeInMillis()) {

				double convertedDividendDate =
						(double) (dividendDate.getTimeInMillis() - today.getTimeInMillis()) / 
						(double) OptionFilter.ONEYEARINMILLIS;		

				convertedDividendAmounts.add(dividendAmount);
				convertedDividendDates.add(convertedDividendDate);

			}

		}

	}

	public static void divideOptionArray() {

		for (int i = 0; i < options.size(); i++) {

			double[] option = options.get(i);

			if (option[0] >= sharePrice)
				calls.add(option);
			else
				puts.add(option);

		}

	}

	public static void extrapolateEuropean() {
		
		options = BKMEstimator_lib.extrapolateEuropean(options, sharePrice, rfr, dividendRate, 
				tau, moneynessGap);
		
	}
	
	public static void extrapolateEuropean(double leftBaseMoneyness,
			double rightBaseMoneyness) {
		
		options = BKMEstimator_lib.extrapolateEuropean(options, sharePrice, rfr, dividendRate, 
				tau, moneynessGap, leftBaseMoneyness, rightBaseMoneyness);
		
	}
	

	public static void extrapolateEuropeanNominally(double leftBaseMoneyness,
	double rightBaseMoneyness) {
		
		options = BKMEstimator_lib.extrapolateEuropeanNominally
				(options, sharePrice, rfr, dividendRate, tau, moneynessGap, 
				leftBaseMoneyness, rightBaseMoneyness);
		
	}
	
	public static VarList getEstimates() throws BadLocationException {

		VarList estimates = 
				BKMEstimator_lib.getEstimates
				(options, sharePrice, rfr, tau);
		return estimates; 

	}

	public static int[] getOptionAmount() {
		
		int[] result = new int[3];
		result[0] = options.size();
		result[1] = puts.size();
		result[2] = calls.size();
		
		return result;
		
	}
	
	public static VarList getSampleProperty(int option) {

		VarList sampleProperty = 
				BKMEstimator_lib.getSampleProperty(calls, puts, options, sharePrice, option);
		return sampleProperty; 

	}

	public static VarList getSlopeEstimates() {
		
		VarList estimates = BKMEstimator_lib.getSlopeEstimates(options, sharePrice);
		return estimates;
		
	}
	
	public static VarList getSlopeEstimates_Backus(double vol) {
		
		VarList estimates = BKMEstimator_lib.getSlopeEstimates_Backus
				(options, sharePrice, rfr, dividendRate, tau, vol);
		return estimates;
		
	}
	
	public static VarList getSlopeEstimates_noQuad() {
		
		VarList estimates = BKMEstimator_lib.getSlopeEstimates_noQuad(options, sharePrice);
		return estimates;
		
	}
	
	public static void initializeDayData() {

		today = null;
		dividendDates = null;
		dividendAmounts = null;

	}

	public static void initializeExdateData() {

		convertedDividendAmounts = new ArrayList<Double>();
		convertedDividendDates = new ArrayList<Double>();		
		exdate = null;
		sharePrice = 0.0;
		rfr = 0.0;
		tau = 0.0;
		options = new ArrayList<double[]>();
		calls = new ArrayList<double[]>();
		puts = new ArrayList<double[]>();
		isInInitialState = true;

	}

	public static void interpolateAmerican() {
		options = BKMEstimator_lib.interpolateAmerican(options, sharePrice, rfr, tau, moneynessGap,
				convertedDividendAmounts, convertedDividendDates);
	} 

	public static void interpolateEuropean() {
		options = BKMEstimator_lib.interpolateEuropean(options, sharePrice, rfr, dividendRate, 
				tau, moneynessGap);
	}
	
	public static void interpolateEuropeanNominalCubicSpline() {
		options = BKMEstimator_lib.interpolateEuropeanNominalCubicSpline
				(options, sharePrice, rfr, dividendRate, tau, nominalGap);
	}
	
	public static boolean isInInitialState() {
		return isInInitialState;
	}

	public static void setDividendAmounts(ArrayList<Double> amounts) {
		dividendAmounts = amounts;
	}

	public static void setDividendDates(ArrayList<Calendar> dates) {
		dividendDates = dates;
	}

	public static void setDividendRate(double rate) {
		dividendRate = rate;
	}
	
	public static void setExdate(Calendar calendar) {
		exdate = calendar;
	}

	public static void setRfr(double rfrEntered) {

		rfr = rfrEntered;
		isInInitialState = false;

	}

	public static void setSharePrice(double sharePriceEntered) {

		sharePrice = sharePriceEntered;
		isInInitialState = false;

	}

	public static void setTau(double tauEntered) {
		tau = tauEntered;	
	}
	
	public static void setToday(Calendar calendar) {
		today = calendar;
	}

	public static void symmetrize(double leftBaseMoneyness, double rightBaseMoneyness) {
		
		for (int optionsIndex = options.size() - 1; optionsIndex > -1; optionsIndex--) {
			
			double[] option = options.get(optionsIndex);
			
			if ((option[0] > sharePrice && Math.log(option[0] / sharePrice) > rightBaseMoneyness) ||
					(option[0] < sharePrice && Math.log(option[0] / sharePrice) * -1.0 > leftBaseMoneyness)) 
						options.remove(optionsIndex);
									
		}
		
//		for (int optionsIndex = 0; optionsIndex < options.size(); optionsIndex++) {
//			
//			double[] option = options.get(optionsIndex);
//			
//			if (option[0] > sharePrice)
//				System.out.print("(c) " + (option[0] / sharePrice) + "(" + option[2] + ", " + option[1] + ") ");
//			else
//				System.out.print("(p) " + (sharePrice / option[0]) + "(" + option[2] + ", " + option[1] + ") ");
//			
//		}
		
	}

	public static void symmetrizeInNumber() {
		
		if (calls.size() > puts.size()) {
		
			for (int i = 0; i < calls.size() - puts.size(); i++) {
				options.remove(options.size() - 1);
			}
			
		}
		
		else if (puts.size() > calls.size()) {
			
			for (int i = 0; i < puts.size() - calls.size(); i++) {
				options.remove(0);
			}			
			
		}
		
	}

	public static void symmetrizeNominally(double leftBaseMoneyness, double rightBaseMoneyness) {
		
		for (int optionsIndex = options.size() - 1; optionsIndex > -1; optionsIndex--) {
			
			double[] option = options.get(optionsIndex);
			
			if ((option[0] > sharePrice && option[0] / sharePrice > rightBaseMoneyness) ||
					(option[0] < sharePrice && option[0] / sharePrice < leftBaseMoneyness)) 
						options.remove(optionsIndex);
									
		}
		
	}

	public static ArrayList<double[]> trackDerivatives_call() throws ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException, 
	BadLocationException {
		
		ArrayList<double[]> derivatives = 
				BKMEstimator_lib.trackDerivatives_call(calls, puts, sharePrice, rfr, tau);
		return derivatives;
		
	}
	
	public static ArrayList<double[]> trackDerivatives_put() throws ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException, 
	BadLocationException {
		
		ArrayList<double[]> derivatives = 
				BKMEstimator_lib.trackDerivatives_put(calls, puts, sharePrice, rfr, tau);
		return derivatives;
		
	}

}
