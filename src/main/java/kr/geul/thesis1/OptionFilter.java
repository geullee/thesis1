package kr.geul.thesis1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.text.BadLocationException;

import kr.geul.dataobject.DataClassArray;
import kr.geul.dataobject.DataClassInfoHolder;
import kr.geul.dataobject.ObList;
import kr.geul.dataobject.Observation;

public class OptionFilter {

	static final long ONEYEARINMILLIS = 31536000000L;
	
	public static boolean checkExistence(int checkedInt,
			ArrayList<Integer> targetArray) {

		boolean doesExist = false;

		for (int j = 0; j < targetArray.size(); j++) {

			int targetElement = targetArray.get(j);

			if (checkedInt == targetElement)
				doesExist = true;

		}

		return doesExist;

	}
	
	public static boolean doesViolateArbitrageRestriction(Observation option,
			double sharePrice, DataClassInfoHolder optionsInfoHolder) 
					throws BadLocationException, ClassNotFoundException, 
					InstantiationException, IllegalAccessException, IllegalArgumentException, 
					InvocationTargetException, NoSuchMethodException, SecurityException {

		String cpFlag = option.getStringTypeVariable(optionsInfoHolder, "cp");
		double bid = (double) option.getVariable(optionsInfoHolder, "bid");
		double offer = (double) option.getVariable(optionsInfoHolder, "offer");
		double strike = (double) option.getVariable(optionsInfoHolder, "strike") / 1000.0; 
		double price = (bid + offer) / 2.0; 

		if (bid == 0.0 || bid > offer)
			return true;

		else {

			if (cpFlag.equals("C")) {

				if (price > sharePrice || price < sharePrice - strike)
					return true;
				else
					return false;

			}

			else {

				if (price > sharePrice || price < strike - sharePrice)
					return true;
				else
					return false;

			}

		}

	}

	public static boolean doesViolateEuropeanArbitrageRestriction(
			Observation option, double sharePrice, double dividendRate,
			double rfr, double tau, DataClassInfoHolder optionInfoHolder) throws ClassNotFoundException, 
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
	
	public static ObList equalize(ObList options, double sharePrice, DataClassInfoHolder optionsInfoHolder) 
			throws BadLocationException, ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {

		ObList result = new ObList();

		ArrayList<Integer> callLocations = new ArrayList<Integer>();
		ArrayList<Integer> putLocations = new ArrayList<Integer>();
		ArrayList<Double> callStrikes = new ArrayList<Double>();
		ArrayList<Double> putStrikes = new ArrayList<Double>();

		for (int i = options.size() - 1; i > -1; i--) {

			Observation option = options.get(i);
			double strike = (double) option.getVariable(optionsInfoHolder, "strike") / 1000.0;
			String cpFlag = option.getStringTypeVariable(optionsInfoHolder, "cp");

			if (cpFlag.equals("C")) {
				callLocations.add(i);
				callStrikes.add(strike);
			}

			else {
				putLocations.add(i);
				putStrikes.add(strike);				
			}

		}

		int[] callOrders = getAscendingOrder(callStrikes);
		int[] putOrders = getDescendingOrder(putStrikes);

		if (callStrikes.size() > putStrikes.size()) {

			for (int i = putOrders.length - 1; i > -1 ; i--) {
				result.add(options.get(putLocations.get(putOrders[i])));
			}

			ArrayList<Integer> includedCandidateOrderLocations = new ArrayList<Integer>();

			for (int i = 0; i < putOrders.length; i++) {

				double targetPriceDistance = Math.log(sharePrice / putStrikes.get(putOrders[i]));
				double minimumDistanceGap = 99999.9;
				int closestCandidateOrderLocation = 99999;

				for (int j = 0; j < callOrders.length; j++) {

					double candidatePriceDistance = 
							Math.log(callStrikes.get(callOrders[j]) / sharePrice);

					if (Math.abs(targetPriceDistance - candidatePriceDistance) < minimumDistanceGap &&
							checkExistence(callOrders[j], includedCandidateOrderLocations) == false) {	
						minimumDistanceGap = Math.abs(targetPriceDistance - candidatePriceDistance);
						closestCandidateOrderLocation = callOrders[j];
					}

				}

				result.add(options.get(callLocations.get(closestCandidateOrderLocation)));
				includedCandidateOrderLocations.add(closestCandidateOrderLocation);

			}

		}

		else {

			ArrayList<Integer> includedCandidateOrderLocations = new ArrayList<Integer>();

			for (int i = 0; i < callOrders.length; i++) {

				double targetPriceDistance = Math.log(callStrikes.get(callOrders[i]) / sharePrice);
				double minimumDistanceGap = 99999.9;
				int closestCandidateOrderLocation = 99999;

				for (int j = 0; j < putOrders.length; j++) {

					double candidatePriceDistance = 
							Math.log(sharePrice / putStrikes.get(putOrders[j]));

					if (Math.abs(targetPriceDistance - candidatePriceDistance) < minimumDistanceGap &&
							checkExistence(putOrders[j], includedCandidateOrderLocations) == false) {	
						minimumDistanceGap = Math.abs(targetPriceDistance - candidatePriceDistance);
						closestCandidateOrderLocation = putOrders[j];
					}

				}

				result.add(options.get(putLocations.get(closestCandidateOrderLocation)));
				includedCandidateOrderLocations.add(closestCandidateOrderLocation);

			}

			for (int i = 0; i < callOrders.length; i++) {
				result.add(options.get(callLocations.get(callOrders[i])));
			}

		}

		return result;

	}

	public static ObList filterPCP(
			Observation exdate, double sharePrice, Observation rfr, Calendar date,
			DataClassInfoHolder exdatesInfoHolder, DataClassInfoHolder optionsInfoHolder,
			DataClassInfoHolder rfrDataClassInfoHolder, DataClassInfoHolder rfrRateDataClassInfoHolder) 
					throws BadLocationException, NoSuchFieldException, SecurityException, 
					ClassNotFoundException, InstantiationException, IllegalAccessException, 
					IllegalArgumentException, InvocationTargetException, NoSuchMethodException {

		ObList result = new ObList();
		ArrayList<PutCallPair> putCallPairs = new ArrayList<PutCallPair>();

		ObList options = exdate.getObListVariable(exdatesInfoHolder, "options");	
		Calendar expiry = exdate.getDateVariable(exdatesInfoHolder, "exdate");	
		long gap = expiry.getTimeInMillis() - date.getTimeInMillis();
		double r = getRfr(rfr, rfrDataClassInfoHolder, rfrRateDataClassInfoHolder, date, expiry);
		double tau = (double) gap / (double) ONEYEARINMILLIS;

		for (int i = 0; i < options.size(); i++) {

			Observation option = options.get(i);
			double strike = (double) option.getVariable(optionsInfoHolder, "strike") / 1000.0;

			if (i == 0) {				
				PutCallPair putCallPair = new PutCallPair(option, sharePrice, optionsInfoHolder);
				putCallPairs.add(putCallPair);				
			}

			else {

				boolean doesAlreadyExist = false;

				for (int j = 0; j < putCallPairs.size(); j++) {

					PutCallPair existingPair = putCallPairs.get(j);

					if (existingPair.getStrike() == strike) {
						existingPair.add(option);
						doesAlreadyExist = true;
					}

				}

				if (doesAlreadyExist == false) {
					PutCallPair putCallPair = new PutCallPair(option, sharePrice, optionsInfoHolder);				
					putCallPairs.add(putCallPair);			
				}			

			}

		}

		for (int i = 0; i < putCallPairs.size(); i++) {

			PutCallPair putCallPair = putCallPairs.get(i);

			if (putCallPair.checkPCP(r, tau) == true) {

				if (putCallPair.getCall() != null)
					result.add(putCallPair.getCall());
				if (putCallPair.getPut() != null)
					result.add(putCallPair.getPut());

			}

			else {
			}

		}

		return result;

	}
	
	public static int[] getAscendingOrder(ArrayList<Double> values) {
		
		int[] order = new int[values.size()];	

		for (int i = 0; i < order.length; i++) {
			order[i] = i;
		}

		boolean needNextPass = true;

		for (int i = 1; i < order.length && needNextPass; i++) {

			needNextPass = false;

			for (int j = 0; j < order.length - i; j++) {

				if (values.get(order[j]) > values.get(order[j + 1])) {

					int temp = order[j];
					order[j] = order[j + 1];
					order[j + 1] = temp;
					needNextPass = true;

				}

			}

		}

		return order;
		
	}

	public static int[] getDescendingOrder(ArrayList<Double> values) {
		
		int[] order = new int[values.size()];	

		for (int i = 0; i < order.length; i++) {
			order[i] = i;
		}

		boolean needNextPass = true;

		for (int i = 1; i < order.length && needNextPass; i++) {

			needNextPass = false;

			for (int j = 0; j < order.length - i; j++) {

				if (values.get(order[j]) < values.get(order[j + 1])) {

					int temp = order[j];
					order[j] = order[j + 1];
					order[j + 1] = temp;
					needNextPass = true;

				}

			}

		}

		return order;
		
	}
	
	public static double getRfr(Observation rfrDay, DataClassInfoHolder rfrDayInfoHolder, 
			DataClassInfoHolder rfrRatesInfoHolder, Calendar fromDate, Calendar toDate) 
					throws NoSuchFieldException, SecurityException, ClassNotFoundException, 
					InstantiationException, IllegalAccessException, IllegalArgumentException, 
					InvocationTargetException, NoSuchMethodException, BadLocationException {
		
		int leftDay = 0, rightDay = 0;
		double leftRate = 0, rightRate = 0;
		double finalRate;
		
		long gap = toDate.getTimeInMillis() - fromDate.getTimeInMillis();
		Calendar gapCalendar = Calendar.getInstance();
		gapCalendar.setTimeInMillis(gap);
		int days = (gapCalendar.get(Calendar.YEAR) - 1970) * 365 + gapCalendar.get(Calendar.DAY_OF_YEAR) - 1;
		
		ObList rates = rfrDay.getObListVariable(rfrDayInfoHolder, "rates");
		
		for (int i = 0; i < rates.size() - 1; i++) {
			
			int leftDays = (int) rates.get(i).getVariable(rfrRatesInfoHolder, "days");
			int rightDays = (int) rates.get(i + 1).getVariable(rfrRatesInfoHolder, "days");
			
			if (leftDays <= days && rightDays >= days) {
				
				leftDay = leftDays;
				rightDay = rightDays;
				leftRate = (double) rates.get(i).getVariable(rfrRatesInfoHolder, "rate");
				rightRate = (double) rates.get(i + 1).getVariable(rfrRatesInfoHolder, "rate");
				break;
				
			}
				
		}
		
		double dayRatio = (double) (days - leftDay) / (double) (rightDay - leftDay);
		finalRate = (leftRate + (rightRate - leftRate) * dayRatio) / 100;
		
		return finalRate;
		
	}

	public static double getTdayTau(Calendar dayFrom, Calendar dayTo, DataClassArray tdayArray) 
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, 
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, 
			SecurityException, FileNotFoundException, IOException, BadLocationException {
		
		DataClassInfoHolder tdayInfoHolder = tdayArray.getInfoHolder();
		int dayFromInt = Integer.parseInt(toDateString(dayFrom));
		int dayToInt = Integer.parseInt(toDateString(dayTo));
		int dayFromLocation = 0, dayToLocation = 0;
		
		for (int i = 0; i < tdayArray.size(); i++) {
			
			Observation tday = tdayArray.get(i);
			int tdayDateInt = Integer.parseInt(tday.getStringTypeVariable(tdayInfoHolder, "date"));

			if (tdayDateInt == dayFromInt)
				dayFromLocation = i;
			
			else if (tdayDateInt == dayToInt) {
				dayToLocation = i;
				break;
			}
			
			else if (tdayDateInt > dayToInt) {
				dayToLocation = i - 1;
				break;
			}
			
		}

		return (double) (dayToLocation - dayFromLocation) / 252.0;
		
	}
	
	public static boolean isExpensiveEnough(Observation option, DataClassInfoHolder optionsInfoHolder) 
			throws BadLocationException, ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {

		boolean isExpensiveEnough;
		double bid = Double.parseDouble(option.getStringTypeVariable(optionsInfoHolder, "bid"));
		double offer = Double.parseDouble(option.getStringTypeVariable(optionsInfoHolder, "offer"));
		double closingPrice = (bid + offer) / 2.0;

		if (closingPrice < 0.375)
			isExpensiveEnough = false;

		else
			isExpensiveEnough = true;

		return isExpensiveEnough;

	}

	public static boolean isOTM(double sharePrice, Observation option, DataClassInfoHolder optionsInfoHolder) 
			throws BadLocationException, ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {

		boolean isOTM;
		String cpFlag = option.getStringTypeVariable(optionsInfoHolder, "cp");
		double strike = (double) option.getVariable(optionsInfoHolder, "strike") / 1000.0; 

		if (cpFlag.equals("C") && strike > sharePrice)
			isOTM = true;

		else if (cpFlag.equals("P") && strike < sharePrice)
			isOTM = true;

		else
			isOTM = false;

		return isOTM;

	}
	
	public static boolean isTraded(Observation option, DataClassInfoHolder optionsInfoHolder) 
			throws BadLocationException, ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {

		boolean isTraded;
		int volume = Integer.parseInt(option.getStringTypeVariable(optionsInfoHolder, "volume"));

		if (volume == 0)
			isTraded = false;

		else
			isTraded = true;

		return isTraded;

	}

	public static boolean isTTMLongEnough(Observation exdate, Calendar dateCalendar, 
			DataClassInfoHolder exdatesInfoHolder, DataClassInfoHolder optionsInfoHolder) 
			throws BadLocationException, ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {

		Calendar exdateCalendar = exdate.getDateVariable(exdatesInfoHolder, "exdate");
		Calendar oneWeek = Calendar.getInstance();
		oneWeek.set(1970, 0, 8);

		Calendar gap = Calendar.getInstance();
		gap.setTimeInMillis(exdateCalendar.getTimeInMillis() - dateCalendar.getTimeInMillis());

		if (gap.getTimeInMillis() > oneWeek.getTimeInMillis() && gap.getTimeInMillis() < ONEYEARINMILLIS)
			return true;
		else		
			return false;

	}
	
	public static String toDateString(Calendar calendar) {

		if (calendar != null) {
			
			String result = Integer.toString(calendar.get(Calendar.YEAR));

			if (Integer.toString(calendar.get(Calendar.MONTH) + 1).length() == 1)
				result += "0";

			result += calendar.get(Calendar.MONTH) + 1;

			if (Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)).length() == 1)
				result += "0";

			result += calendar.get(Calendar.DAY_OF_MONTH);

			return result;
			
		}
		
		else
			return null;

	}
	
}
