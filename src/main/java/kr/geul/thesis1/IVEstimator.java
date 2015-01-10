package kr.geul.thesis1;

import java.util.ArrayList;

public class IVEstimator {

	static double initialIV = 0.5;
	static double initialStepSize = 0.5;
	static double tolerance = 0.00000001;

	public static double estimate(double sharePrice, double strikePrice, double tau, double rfr,
			double targetPrice) {

//		long startTime = System.currentTimeMillis();
		double iv = initialIV;
		double stepSize = initialStepSize;
		double price_CRR = 99999.99;

		if (sharePrice > strikePrice) {

			while (Math.abs(price_CRR - targetPrice) >= tolerance) {

				PutCRRTree putCRRTree = new PutCRRTree(sharePrice, iv, tau, rfr, 1000, 
						new ArrayList<Double>(), new ArrayList<Double>());
				price_CRR = putCRRTree.getOptionPrice(strikePrice);

//				System.out.println("iv: " + iv + ", price: " + price_CRR
//						+ ", gap: " + Math.abs(price_CRR - targetPrice));

				if (Math.abs(price_CRR - targetPrice) >= tolerance) {

					if ((price_CRR > targetPrice && stepSize > 0) || (price_CRR < targetPrice && stepSize < 0)) 
						stepSize *= -0.4;

					iv += stepSize;

				}

			}
			
		}

		else {
			
			while (Math.abs(price_CRR - targetPrice) >= tolerance) {

				CallCRRTree callCRRTree = new CallCRRTree(sharePrice, iv, tau, rfr, 1000, 
						new ArrayList<Double>(), new ArrayList<Double>());
				price_CRR = callCRRTree.getOptionPrice(strikePrice);

//				System.out.println("iv: " + iv + ", price: " + price_CRR
//						+ ", gap: " + Math.abs(price_CRR - targetPrice));

				if (Math.abs(price_CRR - targetPrice) >= tolerance) {

					if ((price_CRR > targetPrice && stepSize > 0) || (price_CRR < targetPrice && stepSize < 0)) 
						stepSize *= -0.4;

					iv += stepSize;

				}

			}
			
		}
//		
//		System.out.println("final iv: " + iv);
//		System.out
//		.println("time elapsed: "
//				+ ((double) (System.currentTimeMillis() - startTime) / 1000.00));

		return iv;

	}

	public static double estimateEuropeanWithDividend(double sharePrice,
			double strikePrice, double tau, double dividend, double rfr, double targetPrice) {
		
		double iv = initialIV;
		double stepSize = initialStepSize;
		double price_BS = 99999.99;
		
//		System.out.println(sharePrice + " : " + strikePrice + " : " + tau
//				+ " : " + dividend + " : " + rfr + " : " + targetPrice);
		
		if (sharePrice > strikePrice) {

			while (Math.abs(price_BS - targetPrice) >= tolerance  && iv > 0 && iv <= 3.0) {
				
				price_BS = BlackScholes.getPutPriceWithDividend(sharePrice, strikePrice, rfr, 
						dividend, iv, tau);

//				System.out.println("iv: " + iv + ", price: " + price_CRR
//						+ ", gap: " + Math.abs(price_CRR - targetPrice));

				if (Math.abs(price_BS - targetPrice) >= tolerance) {

					if ((price_BS > targetPrice && stepSize > 0) || (price_BS < targetPrice && stepSize < 0)) 
						stepSize *= -0.4;

					iv += stepSize;

				}

			}
			
		}
		
		else {
			
			while (Math.abs(price_BS - targetPrice) >= tolerance && iv > 0 && iv <= 3.0) {

				price_BS = BlackScholes.getCallPriceWithDividend(sharePrice, strikePrice, rfr, 
						dividend, iv, tau);

//				System.out.println("iv: " + iv + ", price: " + price_CRR
//						+ ", gap: " + Math.abs(price_CRR - targetPrice));

				if (Math.abs(price_BS - targetPrice) >= tolerance) {

					if ((price_BS > targetPrice && stepSize > 0) || (price_BS < targetPrice && stepSize < 0)) 
						stepSize *= -0.4;

					iv += stepSize;

				}

			}
			
		}
		
		if (iv <= 0 || iv > 3.0)
			iv = -99.99;
		
		return iv;
		
	}

	public static double estimateEuropeanWithDividend(double sharePrice,
			double strikePrice, double tau, double dividend, double rfr,
			double targetPrice, String cp) {
		
		double iv = initialIV;
		double stepSize = initialStepSize;
		double price_BS = 99999.99;
		
//		System.out.println(sharePrice + " : " + strikePrice + " : " + tau
//				+ " : " + dividend + " : " + rfr + " : " + targetPrice);
		
		if (cp.equals("P")) {

			while (Math.abs(price_BS - targetPrice) >= tolerance  && iv > 0 && iv <= 3.0) {
				
				price_BS = BlackScholes.getPutPriceWithDividend(sharePrice, strikePrice, rfr, 
						dividend, iv, tau);

//				System.out.println("iv: " + iv + ", price: " + price_CRR
//						+ ", gap: " + Math.abs(price_CRR - targetPrice));

				if (Math.abs(price_BS - targetPrice) >= tolerance) {

					if ((price_BS > targetPrice && stepSize > 0) || (price_BS < targetPrice && stepSize < 0)) 
						stepSize *= -0.4;

					iv += stepSize;

				}

			}
			
		}
		
		else {
			
			while (Math.abs(price_BS - targetPrice) >= tolerance && iv > 0 && iv <= 3.0) {

				price_BS = BlackScholes.getCallPriceWithDividend(sharePrice, strikePrice, rfr, 
						dividend, iv, tau);

//				System.out.println("iv: " + iv + ", price: " + price_BS
//						+ ", gap: " + Math.abs(price_BS - targetPrice));

				if (Math.abs(price_BS - targetPrice) >= tolerance) {

					if ((price_BS > targetPrice && stepSize > 0) || (price_BS < targetPrice && stepSize < 0)) 
						stepSize *= -0.4;

					iv += stepSize;

				}

			}
			
		}
		
		if (iv <= 0 || iv > 3.0)
			iv = -99.99;
		
		return iv;
		
	}

}

