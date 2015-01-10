package kr.geul.thesis1;

import java.lang.reflect.InvocationTargetException;

import javax.swing.text.BadLocationException;

import kr.geul.console.Console;
import kr.geul.dataobject.DataClassInfoHolder;
import kr.geul.dataobject.Observation;

public class PutCallPair {

	private Observation call, put;
	private double strike, sharePrice;
	private DataClassInfoHolder optionsInfoHolder;
	
	PutCallPair(Observation option, double sharePrice, DataClassInfoHolder optionsInfoHolder) 
			throws BadLocationException, ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {

		this.sharePrice = sharePrice;
		this.optionsInfoHolder = optionsInfoHolder;
		add(option);

	}

	public void add(Observation option) throws BadLocationException, ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException {

		double strike = (double) option.getVariable(optionsInfoHolder, "strike") / 1000.0;
		
		if (strike >= sharePrice) 
			setCall(option);
		else
			setPut(option);

	}

	public boolean checkPCP(double r, double tau) throws BadLocationException, 
	ClassNotFoundException, InstantiationException, IllegalAccessException, 
	IllegalArgumentException, InvocationTargetException, NoSuchMethodException, 
	SecurityException {

		if (call == null || put == null)
			return true;

		else {

			boolean isPCPViolated = false;

			double callBidPrice = (double) call.getVariable(optionsInfoHolder, "bid");
			double callOfferPrice = (double) call.getVariable(optionsInfoHolder,"offer");
			double callMidPrice = (callBidPrice + callOfferPrice) / 2.0;
			double putBidPrice = (double) put.getVariable(optionsInfoHolder,"bid");
			double putOfferPrice = (double) put.getVariable(optionsInfoHolder,"offer");
			double putMidPrice = (putBidPrice + putOfferPrice) / 2.0;
			double strike = (double) call.getVariable(optionsInfoHolder, "strike") / 1000.0;

			if (doesViolateArbitrageRestriction(call, callMidPrice) == true ||
					doesViolateArbitrageRestriction(put, putMidPrice) == true)
				return true;

			if (callMidPrice - putMidPrice < sharePrice - strike || 
					callMidPrice - putMidPrice > sharePrice - (sharePrice * Math.exp(-r * tau)))
				isPCPViolated = true;	

			if (isPCPViolated == true && Math.abs(callMidPrice - putMidPrice) > 0.00000001) 
				return false;
			else
				return true;

		}

	}

	private Observation chooseBetterOne(Observation option_1,
			Observation option_2) throws BadLocationException, ClassNotFoundException, 
			InstantiationException, IllegalAccessException, IllegalArgumentException, 
			InvocationTargetException, NoSuchMethodException, SecurityException {

		int volume_1 = (int) option_1.getVariable(optionsInfoHolder, "volume");
		int volume_2 = (int) option_2.getVariable(optionsInfoHolder, "volume");

		if (volume_1 > volume_2)
			return option_1;
		else if (volume_1 < volume_2)
			return option_2;

		else {

			int ointerset_1 = (int) option_1.getVariable(optionsInfoHolder, "ointerest");
			int ointerset_2 = (int) option_2.getVariable(optionsInfoHolder, "ointerest");

			if (ointerset_1 > ointerset_2)
				return option_1;
			else if (ointerset_1 < ointerset_2)
				return option_2;	

			else {

				double bidPrice_1 = (double) option_1.getVariable(optionsInfoHolder, "bid");
				double offerPrice_1 = (double) option_1.getVariable(optionsInfoHolder, "offer");
				double midPrice_1 = (bidPrice_1 + offerPrice_1) / 2.0;
				double bidPrice_2 = (double) option_2.getVariable(optionsInfoHolder, "bid");
				double offerPrice_2 = (double) option_2.getVariable(optionsInfoHolder, "offer");
				double midPrice_2 = (bidPrice_2 + offerPrice_2) / 2.0;

				if (doesViolateArbitrageRestriction(option_1, midPrice_1) == true && 
						doesViolateArbitrageRestriction(option_2, midPrice_2) == true)
					return null;
				else if (doesViolateArbitrageRestriction(option_1, midPrice_1) == false && 
						doesViolateArbitrageRestriction(option_2, midPrice_2) == true)
					return option_1;
				else if (doesViolateArbitrageRestriction(option_1, midPrice_1) == true && 
						doesViolateArbitrageRestriction(option_2, midPrice_2) == false)
					return option_2;						
				else 
					return option_1;

			}

		}

	}

	private boolean doesViolateArbitrageRestriction(Observation option,
			double sharePrice) throws BadLocationException, ClassNotFoundException, 
			InstantiationException, IllegalAccessException, IllegalArgumentException, 
			InvocationTargetException, NoSuchMethodException, SecurityException {

		String cpFlag = option.getStringTypeVariable(optionsInfoHolder, "cp");
		double bid = (double) option.getVariable(optionsInfoHolder, "bid");
		double offer = (double) option.getVariable(optionsInfoHolder, "offer");
		double strike = (double) option.getVariable(optionsInfoHolder, "strike") / 1000.0; 
		double price = (bid + offer) / 2.0; 

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

	Observation getCall() {
		return call;
	}

	Observation getPut() {
		return put;
	}

	double getStrike() {
		return strike;
	}

	void setCall(Observation option) throws BadLocationException, ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException {

		if (call != null) 	
			call = chooseBetterOne(call, option);

		else {

			call = option;

			if (strike == 0.0)
				strike = (double) option.getVariable(optionsInfoHolder, "strike") / 1000.0;
			else if (strike != (double) option.getVariable(optionsInfoHolder, "strike") / 1000.0) 				
				Console.printErrorMessage("Strike prices do not match.", "PutCallPair");

		}			

	}

	void setPut(Observation option) throws BadLocationException, ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException {

		if (put != null)
			put = chooseBetterOne(put, option);

		else {

			put = option;	

			if (strike == 0.0)
				strike = (double) option.getVariable(optionsInfoHolder, "strike") / 1000.0;
			else if (strike != (double) option.getVariable(optionsInfoHolder, "strike") / 1000.0) 				
				Console.printErrorMessage("Strike prices do not match.", "PutCallPair");

		}		

	}

}
