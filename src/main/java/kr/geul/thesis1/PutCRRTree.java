package kr.geul.thesis1;

import java.util.ArrayList;

public class PutCRRTree extends CRRTree {

	public PutCRRTree(double initialValue, double sigma, double tau,
			double rfr, int size, ArrayList<Double> dividendAmounts, ArrayList<Double> dividendDates) {
		super(initialValue, sigma, tau, rfr, size, dividendAmounts, dividendDates);
	}

	public double getOptionPrice(double strike) {
		
		double[][] lastColumn = treeSpace.get(size - 1);
		
		for (int i = 0; i < lastColumn.length; i++) {
			lastColumn[i][1] = calculateOptionValue(lastColumn[i][0], strike);
		}
		
		for (int i = size - 2; i > -1; i--) {
			
			double[][] column = treeSpace.get(i);
			double[][] rightColumn = treeSpace.get(i + 1);
					
			for (int j = 0; j < i + 1; j++) {
				
				column[j][1] = Math.max(calculateOptionValue(column[j][0], strike), 
						Math.exp(-rfr * delta * tau) * ((upProbability * rightColumn[j][1]) + 
						(downProbability * rightColumn[j + 1][1])));
//				column[j][1] = Math.exp(-rfr * delta * tau) * ((upProbability * rightColumn[j][1]) + 
//						(downProbability * rightColumn[j + 1][1]));
				
			}
			
		}
		
		double[][] firstColumn = treeSpace.get(0);
		return firstColumn[0][1];
		
	}
	
	private double calculateOptionValue(double underlyingPrice, double strike) {
		return Math.max(strike - underlyingPrice, 0);
	}
	
}
