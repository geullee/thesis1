package kr.geul.thesis1;

import java.util.ArrayList;

public class CRRTree {

	protected int size;
	protected double delta, downFactor, downProbability, initialValue, rfr, sigma, 
	tau, upFactor, upProbability;
	protected ArrayList<Double> dividendAmounts;
	protected ArrayList<Integer> dividendDates; 
	protected ArrayList<double[][]> treeSpace;

	public CRRTree (double initialValue, double sigma, double tau, double rfr, int size, 
			ArrayList<Double> dividendAmounts, ArrayList<Double> dividendDates) {

		this.initialValue = initialValue;
		this.sigma = sigma;
		this.tau = tau;
		this.rfr = rfr;
		this.size = size;
		
		this.dividendAmounts = dividendAmounts;
		this.dividendDates = convertDividendDates(dividendDates);
		
		treeSpace = new ArrayList<double[][]>();
		
		construct();	

	}

	private void construct() {

		delta = 1.0 / (double) size;
		//		double alpha = Math.exp(rfr * delta * tau);
		double alpha = 0.5 * (Math.exp(-1.0 * rfr * delta * tau) + 
				Math.exp((rfr + Math.pow(sigma, 2.0)) * delta * tau));

		//		upFactor = Math.exp(sigma * Math.sqrt(delta * tau));
		upFactor = alpha + Math.sqrt(Math.pow(alpha, 2.0) - 1.0);
		downFactor = 1.0 / upFactor;
		//		upProbability = (alpha - downFactor) / (upFactor - downFactor);
		upProbability = (Math.exp(rfr * delta * tau) - downFactor) / (upFactor - downFactor);
		downProbability = 1 - upProbability;

		for (int i = 0; i < size; i++) {			
			double[][] column = new double[i + 1][2];

			if (i == 0) 				
				column[0][0] = initialValue;					
			
			else {

				for (int j = 0; j < i + 1; j++) {

					double[][] leftColumn = treeSpace.get(i - 1);

					if (j == 0) 	  
						column[j][0] = leftColumn[0][0] * upFactor;
					else
						column[j][0] = leftColumn[j - 1][0] * downFactor;				
					
				}

			}

			treeSpace.add(column);

		}

		
		for (int i = 0; i < treeSpace.size(); i++) {
			
			double[][] column = treeSpace.get(i);
			double dividendValue = 0;
			
			for (int j = 0; j < dividendDates.size(); j++) {
				
				int dividendDate = dividendDates.get(j);
				if (dividendDate < i) {
					
					double dividendAmount = dividendAmounts.get(j);
					double timeToDividendPayment = (double) (dividendDate - i) / (double) size * tau;
					dividendValue -= dividendAmount * Math.exp(-rfr * timeToDividendPayment);
					
				}
				
			}
			
			for (int j = 0; j < column.length; j++) {
				column[j][0] += dividendValue;
			}
			
		}
		
	}

	private ArrayList<Integer> convertDividendDates(ArrayList<Double> dates) {
		
		ArrayList<Integer> convertedDates = new ArrayList<Integer>(); 
		
		for (int i = 0; i < dates.size(); i++) {
			
			double date = dates.get(i);
			double locationRatio = date / tau;
			convertedDates.add((int) Math.floor((double) size * locationRatio)); 
			
		}
		
		return convertedDates;
		
	}
	
	public ArrayList<double[][]> getSpace() {
		return treeSpace;	
	}

}