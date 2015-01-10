package kr.geul.thesis1;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.swing.text.BadLocationException;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;

import kr.geul.console.RMI;
import kr.geul.dataobject.VarList;
import kr.geul.dataobject.Variable;

public class BKMEstimator_lib {

	public static VarList getSampleProperty(
			ArrayList<double[]> calls, ArrayList<double[]> puts, ArrayList<double[]> options,
			double sharePrice, int option) {

		VarList sampleProperty = new VarList();

		double leftEndMoneynessNominal = (double) (puts.get(0)[0] / sharePrice);
		double rightEndMoneynessNominal = (double) (calls.get(calls.size() - 1)[0] / sharePrice);
		double leftEndMoneynessLog = -1.0 * Math.log((double) (puts.get(0)[0] / sharePrice));
		double rightEndMoneynessLog = Math.log((double) (calls.get(calls.size() - 1)[0] / sharePrice));
		double moneynessRatioNominal = Math.log((rightEndMoneynessNominal - 1) / (1 - leftEndMoneynessNominal));
		double moneynessSumNominal = rightEndMoneynessNominal - leftEndMoneynessNominal;
		double moneynessRatioLog = Math.log(rightEndMoneynessLog / leftEndMoneynessLog);
		double moneynessSumLog = leftEndMoneynessLog + rightEndMoneynessLog;
		double leftImpVol = options.get(0)[2];
		double rightImpVol = options.get(options.size() - 1)[2];
		
		sampleProperty.add(new Variable("options", options.size()));
		sampleProperty.add(new Variable("leftEndNom", leftEndMoneynessNominal));
		sampleProperty.add(new Variable("rightEndNom", rightEndMoneynessNominal));
		sampleProperty.add(new Variable("leftEndLog", leftEndMoneynessLog));
		sampleProperty.add(new Variable("rightEndLog", rightEndMoneynessLog));
		sampleProperty.add(new Variable("leftImpVol", leftImpVol));
		sampleProperty.add(new Variable("rightImpVol", rightImpVol));
		sampleProperty.add(new Variable("ratioNom", moneynessRatioNominal));
		sampleProperty.add(new Variable("sumNom", moneynessSumNominal));
		sampleProperty.add(new Variable("ratioLog", moneynessRatioLog));
		sampleProperty.add(new Variable("sumLog", moneynessSumLog));

		if (option == 1) {
			
			double[] volume = getVolume(calls, puts);
			double volumeRatio = Math.log(volume[0] / volume[1]);
			double volumeSum = volume[0] + volume[1];
			sampleProperty.add(new Variable("vratio", volumeRatio));
			sampleProperty.add(new Variable("vsum", volumeSum));
			
		}
		
		return sampleProperty;
		
	}

	public static VarList getEstimates(ArrayList<double[]> options,
			double sharePrice, double r, double tau) throws BadLocationException {

		VarList estimates = new VarList();

		double v, w, x, mu, vol, skew, kurt;

		v = getV(options, sharePrice);
		w = getW(options, sharePrice);
		x = getX(options, sharePrice);
		mu = getMu(r, tau, v, w, x);
		vol = getVol(r, tau, mu, v, w, x);
		skew = getSkew(r, tau, mu, v, w, x);
		kurt = getKurt(r, tau, mu, v, w, x);

		estimates.add(new Variable("v", v));
		estimates.add(new Variable("w", w));
		estimates.add(new Variable("x", x));
		estimates.add(new Variable("mu", mu));
		estimates.add(new Variable("vol", vol));
		estimates.add(new Variable("skew", skew));
		estimates.add(new Variable("kurt", kurt));

		return estimates;

	}

	private static double getKurt(double r, double tau, double mu, double v, double w,
			double x) {

		double ert = Math.exp(r * tau);
		double kurt = ((ert * x) - (4 * mu * ert * w) + (6 * ert * Math.pow(mu, 2) * v) - (3 * Math.pow(mu, 4))) /
				Math.pow((ert * v) - Math.pow(mu, 2), 2); 
		return kurt;

	}

	private static double getMu(double r, double tau, double v, double w, double x) {

		double ert = Math.exp(r * tau);
		double mu = ert - 1 - (ert * v / 2) - (ert * w / 6) - (ert * x / 24);
		return mu;

	}

	private static double getSkew(double r, double tau, double mu, double v, double w,
			double x) {

		double ert = Math.exp(r * tau);
		double skew = ((ert * w) - (3 * mu * ert * v) + (2 * Math.pow(mu, 3))) / 
				Math.pow((ert * v) - Math.pow(mu, 2), 1.5); 
		return skew;

	}

	public static VarList getSlopeEstimates(ArrayList<double[]> options,
			double sharePrice) {

		double constant, linear, quadratic;
		VarList estimates = new VarList();
		ArrayList<Double> moneyness = new ArrayList<Double>();
		ArrayList<Double> moneynessSquared = new ArrayList<Double>();
		ArrayList<Double> impVol = new ArrayList<Double>();
		
		
		for (int i = 0; i < options.size(); i++) {
			moneyness.add(options.get(i)[0] / sharePrice - 1);
			moneynessSquared.add(Math.pow(options.get(i)[0] / sharePrice - 1, 2));
			impVol.add(options.get(i)[2]);
		}
		
		RMI.addVariable(moneyness, "moneyness");
		RMI.addVariable(moneynessSquared, "moneynessSquared");
		RMI.addVariable(impVol, "impVol");
		RMI.run("estimate <- coef(lm(impVol ~ moneyness + moneynessSquared))");
		
		constant = RMI.getDouble("estimate[\"(Intercept)\"]");
		linear = RMI.getDouble("estimate[\"moneyness\"]");
		quadratic = RMI.getDouble("estimate[\"moneynessSquared\"]");
		
		estimates.add(new Variable("constant", constant));
		estimates.add(new Variable("linear", linear));
		estimates.add(new Variable("quadratic", quadratic));
		
		return estimates;
		
	}
	
	public static VarList getSlopeEstimates_Backus(ArrayList<double[]> options,
			double sharePrice, double rfr, double dividend, double tau, double vol) {

		double constant, linear, quadratic;
		VarList estimates = new VarList();
		ArrayList<Double> moneyness = new ArrayList<Double>();
		ArrayList<Double> moneynessSquared = new ArrayList<Double>();
		ArrayList<Double> impVol = new ArrayList<Double>();
		
		for (int i = 0; i < options.size(); i++) {
			double d = (Math.log(sharePrice * Math.exp((rfr - dividend) * tau) / options.get(i)[0])
					+ 0.5 * Math.pow(vol, 2) * tau) / vol * tau; 
			moneyness.add(d);
			moneynessSquared.add(Math.pow(d, 2));
			impVol.add(options.get(i)[2]);
		}
		
		RMI.addVariable(moneyness, "moneyness");
		RMI.addVariable(moneynessSquared, "moneynessSquared");
		RMI.addVariable(impVol, "impVol");
		RMI.run("estimate <- coef(lm(impVol ~ moneyness + moneynessSquared))");
		
		constant = RMI.getDouble("estimate[\"(Intercept)\"]");
		linear = RMI.getDouble("estimate[\"moneyness\"]");
		quadratic = RMI.getDouble("estimate[\"moneynessSquared\"]");
		
		estimates.add(new Variable("constant", constant));
		estimates.add(new Variable("linear", linear));
		estimates.add(new Variable("quadratic", quadratic));
		
		return estimates;
		
	}
	
	public static VarList getSlopeEstimates_noQuad(ArrayList<double[]> options,
			double sharePrice) {

		double constant, linear;
		VarList estimates = new VarList();
		ArrayList<Double> moneyness = new ArrayList<Double>();
		ArrayList<Double> impVol = new ArrayList<Double>();
		
		
		for (int i = 0; i < options.size(); i++) {
			moneyness.add(Math.log(options.get(i)[0] / sharePrice));
			impVol.add(Math.log(options.get(i)[2]));
		}
		
		RMI.addVariable(moneyness, "moneyness");
		RMI.addVariable(impVol, "impVol");
		RMI.run("estimate <- coef(lm(impVol ~ moneyness))");
		
		constant = RMI.getDouble("estimate[\"(Intercept)\"]");
		linear = RMI.getDouble("estimate[\"moneyness\"]");
		
		estimates.add(new Variable("constant", constant));
		estimates.add(new Variable("linear", linear));
		
		return estimates;
		
	}
	
	private static double getTrapezoidalApproximationValue(ArrayList<double[]> xValues,
			ArrayList<Double> yValues) throws BadLocationException {

		double totalArea = 0.0;

		for (int i = 1; i < xValues.size(); i++) {	

			try {

				double xWidth = xValues.get(i)[0] - xValues.get(i - 1)[0];
				double yValueLow = Math.min(yValues.get(i), yValues.get(i - 1));
				double yValueHigh = Math.max(yValues.get(i), yValues.get(i - 1));
				double area = xWidth * ((yValueHigh + yValueLow) * 0.5); 
				totalArea += area;

			}

			catch (NullPointerException e) {
				e.printStackTrace();		
			}

		}

		return totalArea;

	}	

	private static double getV(ArrayList<double[]> options, double sharePrice) throws BadLocationException {

		ArrayList<Double> vElements = new ArrayList<Double>();

		for (int i = 0; i < options.size(); i++) {
			vElements.add(getVElement(options.get(i)[0], sharePrice, options.get(i)[1]));
		}

		return getTrapezoidalApproximationValue(options, vElements);

	}

	private static double getVElement(double k, double s, double o) {
		return (2.0 * (1.0 - Math.log(k / s))) / (Math.pow(k, 2.0)) * o;		
	}

	private static double getVol(double r, double tau, double mu, double v, double w,
			double x) {

		double ert = Math.exp(r * tau);
		double vol = Math.sqrt(((ert * v) - Math.pow(mu, 2)) / tau);
		return vol;

	}

	private static double[] getVolume(ArrayList<double[]> calls,
			ArrayList<double[]> puts) {
		
		double[] volume = new double[2];
		double callVolume = 0, putVolume = 0;

		for (int i = 0; i < calls.size(); i++) {
			double[] call = calls.get(i);
			callVolume += (call[3] * call[4]);
		}
		
		for (int i = 0; i < puts.size(); i++) {		
			double[] put = puts.get(i);
			putVolume += (-1.0 * put[3] * put[4]);
		}
		
		volume[0] = callVolume;
		volume[1] = putVolume;
		
		return volume;
		
	}
	
	private static double getW(ArrayList<double[]> options, double sharePrice) throws BadLocationException {

		ArrayList<Double> wElements = new ArrayList<Double>();

		for (int i = 0; i < options.size(); i++) {
			wElements.add(getWElement(options.get(i)[0], sharePrice, options.get(i)[1]));
		}

		return getTrapezoidalApproximationValue(options, wElements); 

	}

	private static double getWElement(double k, double s, double o) {
		return ((6.0 * Math.log(k / s)) - (3.0 * Math.pow(Math.log(k / s), 2.0))) / (Math.pow(k, 2.0)) * o;		
	}

	private static double getX(ArrayList<double[]> options, double sharePrice) throws BadLocationException {

		ArrayList<Double> xElements = new ArrayList<Double>();

		for (int i = 0; i < options.size(); i++) {
			xElements.add(getXElement(options.get(i)[0], sharePrice, options.get(i)[1]));
		}

		return getTrapezoidalApproximationValue(options, xElements);

	}

	private static double getXElement(double k, double s, double o) {
		return ((12.0 * Math.pow(Math.log(k / s), 2.0)) - 
				(4.0 * Math.pow(Math.log(k / s), 3.0))) 
				/ (Math.pow(k, 2.0)) * o;		
	}

	public static ArrayList<double[]> interpolateAmerican
	(ArrayList<double[]> options, double s, double r, double tau, double moneynessGap, 
			ArrayList<Double> convertedDividendAmounts, ArrayList<Double> convertedDividendDates) {

		ArrayList<double[]> newOptions = new ArrayList<double[]>();

		double leftMoneyness = s / options.get(0)[0];
		double rightMoneyness = options.get(options.size() - 1)[0] / s;
		
		int numberOfLeftPoints = (int) Math.floor((leftMoneyness - 1.0 - (moneynessGap / 2.0)) / moneynessGap);
		int numberOfRightPoints = (int) Math.floor((rightMoneyness - 1.0 - (moneynessGap / 2.0)) / moneynessGap);
		
		for (int i = numberOfLeftPoints; i > -1; i--) {
			
			double moneyness = 1.0 / (1.0 + ((double) i + 0.5) * moneynessGap);
			double strike = s * moneyness;
			double impVol = 0.0;
			
			for (int j = 1; j < options.size(); j++) {
				
				if (options.get(j - 1)[0] <= strike && options.get(j)[0] >= strike) {
					
					double[] leftOption = options.get(j - 1);
					double[] rightOption = options.get(j);
					impVol = leftOption[2] + (rightOption[2] - leftOption[2]) *
							((strike - leftOption[0]) / (rightOption[0] - leftOption[0]));	
					PutCRRTree putCRRTree = new PutCRRTree(s, impVol, tau, r, 1000,
							convertedDividendAmounts, convertedDividendDates);
					double price = putCRRTree.getOptionPrice(strike);
					double[] option = {strike, price, impVol};
//					System.out.print("(p) " + (s / strike) + "(" + impVol + ", " + price + ") ");
					newOptions.add(option);
					
				}
				
			}
			
		}
		
		for (int i = 0; i < numberOfRightPoints + 1; i++) {
			
			double moneyness = (1.0 + ((double) i + 0.5) * moneynessGap);
			double strike = s * moneyness;
			double impVol = 0.0;
			
			for (int j = 1; j < options.size(); j++) {
				
				if (options.get(j - 1)[0] <= strike && options.get(j)[0] >= strike) {
					
					double[] leftOption = options.get(j - 1);
					double[] rightOption = options.get(j);
					impVol = leftOption[2] + (rightOption[2] - leftOption[2]) *
							((strike - leftOption[0]) / (rightOption[0] - leftOption[0]));	
					CallCRRTree callCRRTree = new CallCRRTree(s, impVol, tau, r, 1000,
							convertedDividendAmounts, convertedDividendDates);
					double price = callCRRTree.getOptionPrice(strike);
					double[] option = {strike, price, impVol};
//					System.out.print("(c) " + (strike / s) + "(" + impVol + ", " + price + ") ");
					newOptions.add(option);
					
				}
				
			}
			
		}
		
//		System.out.println("");
//		
//		newOptions.add(options.get(0));		
//
//		for (int i = 0; i < options.size() - 1; i++) {
//
//			double[] leftOption = options.get(i);
//			double[] rightOption = options.get(i + 1);
//
//			if (convertedDividendAmounts.size() > 0) {
//			
//				System.out.println("s: " + s + ", rfr: " + r + ", tau: " + tau);
//				System.out.println("left: " + leftOption[0] + "(" + leftOption[1] + ", " + leftOption[2] + "), right: " + 
//				rightOption[0] + "(" + rightOption[1] + ", " + rightOption[2] + ")");
//				
//			}
//
//			for (int j = 1; j < moneynessGap; j++) {
//
//				double k = 
//						leftOption[0] + (double) j * (rightOption[0] - leftOption[0]) / moneynessGap;
//				double impVol = 
//						leftOption[2] + (double) j * (rightOption[2] - leftOption[2]) / moneynessGap;
//				double price;
//
//				if (k > s) {
//					if (convertedDividendAmounts.size() > 0) 
//						System.out.print("(c) ");
//					CallCRRTree callCRRTree = new CallCRRTree(s, impVol, tau, r, 1000, 
//							convertedDividendAmounts, convertedDividendDates);
//					price = callCRRTree.getOptionPrice(k);
//					//					price = BlackScholes.getCallPrice(s, k, r, impVol, tau);
//				}
//
//				else {
//					if (convertedDividendAmounts.size() > 0) 
//						System.out.print("(p) ");
//					PutCRRTree putCRRTree = new PutCRRTree(s, impVol, tau, r, 1000,
//							convertedDividendAmounts, convertedDividendDates);
//					price = putCRRTree.getOptionPrice(k);
//					//					price = BlackScholes.getPutPrice(s, k, r, impVol, tau);
//				}
//
//				double[] option = {k, price, impVol};
//				newOptions.add(option);
//
//				if (convertedDividendAmounts.size() > 0) 
//					System.out.print(k + "(" + impVol + ", " + price + ") ");
//
//			}
//
//			newOptions.add(rightOption);
//			if (convertedDividendAmounts.size() > 0) 
//				System.out.println("");	
//
//		}

		return newOptions;

	}

	public static ArrayList<double[]> interpolateEuropean(
			ArrayList<double[]> options, double s, double r,
			double dividendRate, double tau, double moneynessGap) {
		
		ArrayList<double[]> newOptions = new ArrayList<double[]>();
		
		double leftMoneyness = -1.0 * Math.log((double) (options.get(0)[0] / s)); 
		double rightMoneyness = Math.log((double) (options.get(options.size() - 1)[0] / s));
		
		int numberOfLeftPoints = (int) Math.floor((leftMoneyness - (moneynessGap / 2.0)) / moneynessGap);
		int numberOfRightPoints = (int) Math.floor((rightMoneyness - (moneynessGap / 2.0)) / moneynessGap);
		
		for (int i = numberOfLeftPoints; i > -1; i--) {
			
			double moneyness = -1.0 * ((double) i + 0.5) * moneynessGap;
			double strike = s * Math.exp(moneyness);
			double impVol = 0.0;
			
			for (int j = 1; j < options.size(); j++) {
				
				if (options.get(j - 1)[0] <= strike && options.get(j)[0] >= strike) {
					
					double[] leftOption = options.get(j - 1);
					double[] rightOption = options.get(j);
					impVol = leftOption[2] + (rightOption[2] - leftOption[2]) *
							((strike - leftOption[0]) / (rightOption[0] - leftOption[0]));	
					double price = BlackScholes.getPutPriceWithDividend
							(s, strike, r, dividendRate, impVol, tau);
					double[] option = {strike, price, impVol};
//					System.out.print("(p) " + (s / strike) + "(" + impVol + ", " + price + ") ");
					newOptions.add(option);
					
				}
				
			}
			
		}
		
		for (int i = 0; i < numberOfRightPoints + 1; i++) {
			
			double moneyness = (((double) i + 0.5) * moneynessGap);
			double strike = s * Math.exp(moneyness);
			double impVol = 0.0;
			
			for (int j = 1; j < options.size(); j++) {
				
				if (options.get(j - 1)[0] <= strike && options.get(j)[0] >= strike) {
					
					double[] leftOption = options.get(j - 1);
					double[] rightOption = options.get(j);
					impVol = leftOption[2] + (rightOption[2] - leftOption[2]) *
							((strike - leftOption[0]) / (rightOption[0] - leftOption[0]));	
					double price = BlackScholes.getCallPriceWithDividend
							(s, strike, r, dividendRate, impVol, tau);
					double[] option = {strike, price, impVol};
//					System.out.print("(c) " + (strike / s) + "(" + impVol + ", " + price + ") ");
					newOptions.add(option);
					
				}
				
			}
		}
		
		return newOptions;
		
	}

	public static ArrayList<double[]> interpolateEuropeanNominalCubicSpline(
			ArrayList<double[]> options, double s, double r,
			double dividendRate, double tau, double gap) {
		
		ArrayList<double[]> newOptions = new ArrayList<double[]>();
		double[] strikes = new double[options.size()];
		double[] ivs = new double[options.size()];
		
		for (int i = 0; i < options.size(); i++) {
			strikes[i] = options.get(i)[0];
			ivs[i] = options.get(i)[2];
		}
		
		UnivariateInterpolator interpolator = new SplineInterpolator();
		UnivariateFunction function = interpolator.interpolate(strikes, ivs);
		
		double leftEndStrike = options.get(0)[0]; 
		double rightEndStrike = options.get(options.size() - 1)[0];
		
		int numberOfLeftPoints = (int) Math.floor((s - leftEndStrike) / gap);
		int numberOfRightPoints = (int) Math.floor((rightEndStrike - s - gap) / gap);
		
		double strike, impVol, price;
		
		for (int i = numberOfLeftPoints; i > -1; i--) {
			
			strike = s - (double) i * gap;
			
			if (strike >= leftEndStrike && strike <= s) {
				
				impVol = function.value(strike);
				price = BlackScholes.getPutPriceWithDividend
						(s, strike, r, dividendRate, impVol, tau);
				double[] option = {strike, price, impVol};
				newOptions.add(option);
				
//				System.out.println("(p) " + strike + "(" + impVol + ") ");
				
			}
				
		}
				
		for (int i = 1; i < numberOfRightPoints + 1; i++) {
			
			strike = s + (double) i * gap;
			if (strike >= s && s <= rightEndStrike) {
			
				impVol = function.value(strike);
				price = BlackScholes.getCallPriceWithDividend
						(s, strike, r, dividendRate, impVol, tau);
				double[] option = {strike, price, impVol};
				newOptions.add(option);
				
//				System.out.println("(c) " + strike + "(" + impVol + ") ");
				
			}
					
		}
		
		return newOptions;
		
	}
	
	public static ArrayList<double[]> extrapolateEuropean(
			ArrayList<double[]> options, double s, double r,
			double dividendRate, double tau, double moneynessGap) {
		
		ArrayList<double[]> newOptions = new ArrayList<double[]>();
		
		double leftMoneyness = -1.0 * Math.log((double) (options.get(0)[0] / s));
		double rightMoneyness = Math.log((double) (options.get(options.size() - 1)[0] / s));;
		double leftImpVol = options.get(0)[2];
		double rightImpVol = options.get(options.size() - 1)[2];
		
		int numberOfLeftPoints = (int) Math.floor((0.5 - leftMoneyness) / moneynessGap);
		int numberOfRightPoints = (int) Math.floor((0.2 - rightMoneyness) / moneynessGap);
		
		for (int i = 0; i < numberOfLeftPoints; i++) {
			
			double moneyness = -0.5 + ((double) i * moneynessGap);
			double strike = s * Math.exp(moneyness);
			double price = BlackScholes.getPutPriceWithDividend
					(s, strike, r, dividendRate, leftImpVol, tau);
			double[] option = {strike, price, leftImpVol};
//			System.out.print("(p) " + (s / strike) + "(" + leftImpVol + ", " + price + ") ");
			newOptions.add(option);
			
		}
		
		for (int i = 0; i < options.size(); i++) {
			double[] option = options.get(i);
			newOptions.add(option);
		}
		
		for (int i = 0; i < numberOfRightPoints; i++) {
			
			double moneyness = (rightMoneyness + (double) (i + 1) * moneynessGap);
			double strike = s * Math.exp(moneyness);
			double price = BlackScholes.getCallPriceWithDividend
					(s, strike, r, dividendRate, rightImpVol, tau);
			double[] option = {strike, price, rightImpVol};
//			System.out.print("(c) " + (strike / s) + "(" + rightImpVol + ", " + price + ") ");
			newOptions.add(option);
			
		}
		
		return newOptions;
		
	}

	public static ArrayList<double[]> extrapolateEuropean(
			ArrayList<double[]> options, double s, double r,
			double dividendRate, double tau, double moneynessGap,
			double leftBaseMoneyness, double rightBaseMoneyness) {
		
		ArrayList<double[]> newOptions = new ArrayList<double[]>();
		
		double leftMoneyness = -1.0 * Math.log((double) (options.get(0)[0] / s));
		double rightMoneyness = Math.log((double) (options.get(options.size() - 1)[0] / s));;
		
		if (leftMoneyness < leftBaseMoneyness) {
			
			double leftImpVol = options.get(0)[2];
			int numberOfLeftPoints = 
					(int) Math.floor((leftBaseMoneyness - leftMoneyness) / moneynessGap);
			
			for (int i = 0; i < numberOfLeftPoints; i++) {
				
				double moneyness = (-1.0 * leftBaseMoneyness) + ((double) i * moneynessGap);
				double strike = s * Math.exp(moneyness);
				double price = BlackScholes.getPutPriceWithDividend
						(s, strike, r, dividendRate, leftImpVol, tau);
				double[] option = {strike, price, leftImpVol};
//				System.out.print("(p) " + (s / strike) + "(" + leftImpVol + ", " + price + ") ");
				newOptions.add(option);
				
			}
			
		}
		
		for (int i = 0; i < options.size(); i++) {
			double[] option = options.get(i);
			newOptions.add(option);
		}
		
		if (rightMoneyness < rightBaseMoneyness) {
			
			double rightImpVol = options.get(options.size() - 1)[2];
			int numberOfRightPoints = 
					(int) Math.floor((rightBaseMoneyness - rightMoneyness) / moneynessGap);
			
			for (int i = 0; i < numberOfRightPoints; i++) {
				
				double moneyness = (rightMoneyness + (double) (i + 1) * moneynessGap);
				double strike = s * Math.exp(moneyness);
				double price = BlackScholes.getCallPriceWithDividend
						(s, strike, r, dividendRate, rightImpVol, tau);
				double[] option = {strike, price, rightImpVol};
//				System.out.print("(c) " + (strike / s) + "(" + rightImpVol + ", " + price + ") ");
				newOptions.add(option);
				
			}
			
		}
		
		return newOptions;
		
	}

	public static ArrayList<double[]> extrapolateEuropeanNominally(
			ArrayList<double[]> options, double s, double r,
			double dividendRate, double tau, double moneynessGap,
			double leftBaseMoneyness, double rightBaseMoneyness) {
		
ArrayList<double[]> newOptions = new ArrayList<double[]>();
		
		double leftMoneyness = (double) (options.get(0)[0] / s);
		double rightMoneyness = (double) (options.get(options.size() - 1)[0] / s);
		
		if (leftMoneyness > leftBaseMoneyness) {
			
			double leftImpVol = options.get(0)[2];
			int numberOfLeftPoints = 
					(int) Math.floor((leftMoneyness - leftBaseMoneyness) / moneynessGap);
			
			for (int i = 0; i < numberOfLeftPoints; i++) {
				
				double moneyness = leftBaseMoneyness + ((double) i * moneynessGap);
				double strike = s * moneyness;
				double price = BlackScholes.getPutPriceWithDividend
						(s, strike, r, dividendRate, leftImpVol, tau);
				double[] option = {strike, price, leftImpVol};
//				System.out.print("(p) " + (s / strike) + "(" + leftImpVol + ", " + price + ") ");
				newOptions.add(option);
				
			}
			
		}
		
		for (int i = 0; i < options.size(); i++) {
			double[] option = options.get(i);
			newOptions.add(option);
		}
		
		if (rightMoneyness < rightBaseMoneyness) {
			
			double rightImpVol = options.get(options.size() - 1)[2];
			int numberOfRightPoints = 
					(int) Math.floor((rightBaseMoneyness - rightMoneyness) / moneynessGap);
			
			for (int i = 0; i < numberOfRightPoints; i++) {
				
				double moneyness = (rightMoneyness + (double) (i + 1) * moneynessGap);
				double strike = s * moneyness;
				double price = BlackScholes.getCallPriceWithDividend
						(s, strike, r, dividendRate, rightImpVol, tau);
				double[] option = {strike, price, rightImpVol};
//				System.out.print("(c) " + (strike / s) + "(" + rightImpVol + ", " + price + ") ");
				newOptions.add(option);
				
			}
			
		}
		
		return newOptions;
		
	}

	public static ArrayList<double[]> trackDerivatives_call(
			ArrayList<double[]> calls, ArrayList<double[]> puts,
			double sharePrice, double rfr, double tau) throws BadLocationException, 
			ClassNotFoundException, InstantiationException, IllegalAccessException, 
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, 
			SecurityException {
		
		ArrayList<double[]> results = new ArrayList<double[]>();
		calls.add(0, puts.get(puts.size() - 1));
		double[] vols = new double[puts.size()];
		double[] dVols = new double[puts.size()];
		double[] skews = new double[puts.size()];
		double[] dSkews = new double[puts.size()];
		
		for (int i = 2; i < calls.size(); i++) {
			
			ArrayList<double[]> subOptions = new ArrayList<double[]>();
			
			for (int j = 0; j < i; j++) {
				subOptions.add(calls.get(j));
			}
			
			VarList estimates = getEstimates(subOptions, sharePrice, rfr, tau);
			double V = estimates.get("v").getDoubleValue();
			double W = estimates.get("w").getDoubleValue();
			double mu = estimates.get("mu").getDoubleValue();
			double vol = estimates.get("vol").getDoubleValue();
			double skew = estimates.get("skew").getDoubleValue();
			
            double endPrice = subOptions.get(subOptions.size() - 1)[1];
            double endStrike = subOptions.get(subOptions.size() - 1)[0];
            double endLogMoneyness = 
            		Math.log(subOptions.get(subOptions.size() - 1)[0] / sharePrice);
			double ert = Math.exp(rfr * tau);
            
			double dV = (2 * (1 - endLogMoneyness)) / 
					Math.pow(endStrike, 2) * endPrice;
            double dW = ((6 * endLogMoneyness) - (3 * Math.pow(endLogMoneyness, 2))) / 
            		Math.pow(endStrike, 2) * endPrice;
            double dX = (4 * (Math.pow(endLogMoneyness, 2)) * (3 - endLogMoneyness)) / 
            		Math.pow(endStrike, 2) * endPrice;
            
            double dMu = (-ert / 2) * dV + (-ert / 6) * dW + (-ert / 24) * dX;
            
            double gamma = ert * W - 3 * mu * ert * V + 2 * Math.pow(mu, 3);
            double theta = Math.pow((ert * V - Math.pow(mu, 2)), 1.5);
            
            double dGamma = (ert * dW) - (3 * ert * (V * dMu + mu * dV)) + 6 * Math.pow(mu, 2) * dMu;
            double dTheta = 1.5 * Math.pow(ert * V - Math.pow(mu, 2), 0.5) * (ert * dV - 2 * mu * dMu);
            
            double dVol = 0.5 * Math.pow((ert * V - Math.pow(mu, 2)), -0.5) * (ert * dV - 2 * mu * dMu);
            double dSkew = ((theta * dGamma) - (gamma * dTheta)) / Math.pow(theta, 2);
            
			System.out.println(vol + "," + dVol + "," + skew + "," + dSkew); 
			
		}
		
		results.add(vols);
		results.add(dVols);
		results.add(skews);
		results.add(dSkews);
		
		return results;
		
	}
	
	public static ArrayList<double[]> trackDerivatives_put(
			ArrayList<double[]> calls, ArrayList<double[]> puts,
			double sharePrice, double rfr, double tau) throws BadLocationException, 
			ClassNotFoundException, InstantiationException, IllegalAccessException, 
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, 
			SecurityException {
		
		ArrayList<double[]> results = new ArrayList<double[]>();
		puts.add(calls.get(0));
		double[] vols = new double[puts.size()];
		double[] dVols = new double[puts.size()];
		double[] skews = new double[puts.size()];
		double[] dSkews = new double[puts.size()];
		
		for (int i = 2; i < puts.size(); i++) {
			
			ArrayList<double[]> subOptions = new ArrayList<double[]>();
			
			for (int j = 0; j < i; j++) {
				subOptions.add(0, puts.get(puts.size() - (j + 1)));
			}
			
			VarList estimates = getEstimates(subOptions, sharePrice, rfr, tau);
			double V = estimates.get("v").getDoubleValue();
			double W = estimates.get("w").getDoubleValue();
			double mu = estimates.get("mu").getDoubleValue();
			double vol = estimates.get("vol").getDoubleValue();
			double skew = estimates.get("skew").getDoubleValue();
			
            double endPrice = subOptions.get(0)[1];
            double endStrike = subOptions.get(0)[0];
            double endLogMoneyness = Math.abs(Math.log(subOptions.get(0)[0] / sharePrice));
			double ert = Math.exp(rfr * tau);
            
			double dV = (2 * (1 + endLogMoneyness)) / 
					Math.pow(endStrike, 2) * endPrice;
            double dW = ((-6 * endLogMoneyness) - (3 * Math.pow(endLogMoneyness, 2))) / 
            		Math.pow(endStrike, 2) * endPrice;
            double dX = (4 * (Math.pow(endLogMoneyness, 2)) * (3 + endLogMoneyness)) / 
            		Math.pow(endStrike, 2) * endPrice;
            
            double dMu = (-ert / 2) * dV + (-ert / 6) * dW + (-ert / 24) * dX;
            
            double gamma = ert * W - 3 * mu * ert * V + 2 * Math.pow(mu, 3);
            double theta = Math.pow((ert * V - Math.pow(mu, 2)), 1.5);
            
            double dGamma = (ert * dW) - (3 * ert * (V * dMu + mu * dV)) + 6 * Math.pow(mu, 2) * dMu;
            double dTheta = 1.5 * Math.pow(ert * V - Math.pow(mu, 2), 0.5) * (ert * dV - 2 * mu * dMu);
            
            double dVol = 0.5 * Math.pow((ert * V - Math.pow(mu, 2)), -0.5) * (ert * dV - 2 * mu * dMu);
            double dSkew = ((theta * dGamma) - (gamma * dTheta)) / Math.pow(theta, 2);
            
			System.out.println(vol + "," + dVol + "," + skew + "," + dSkew); 
			
		}
		
		results.add(vols);
		results.add(dVols);
		results.add(skews);
		results.add(dSkews);
		
		return results;
		
	}

}
