package kr.geul.thesis1.command;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.text.BadLocationException;

import kr.geul.console.PublicWriter;
import kr.geul.console.command.Command;
import kr.geul.dataobject.DataClass;
import kr.geul.dataobject.DataClassArray;
import kr.geul.dataobject.DataClassInfoHolder;
import kr.geul.dataobject.InfoHolder;
import kr.geul.dataobject.ObList;
import kr.geul.dataobject.Observation;
import kr.geul.thesis1.Thesis1;

public class GetFiltrationDetails_filter extends Command {

	static final double annualTradingDays = 252.0, minimumOptionPrice = 0.375, 
			minimumTimeToMaturityInTradingDays = 5.0,
			maximumTimeToMaturityInTradingDays = 252.0;

	DataClassArray dayDataClassArray, dividendDataClassArray, rfrDataClassArray, tdayDataClassArray;
	DataClassInfoHolder dayInfoHolder, dividendInfoHolder, exdateInfoHolder, optionInfoHolder, 
	rfrInfoHolder, rateInfoHolder, shareInfoHolder;

	public GetFiltrationDetails_filter(ArrayList<String> arguments) {
		super(arguments);	
	}

	@Override
	protected String getCommandName() {
		return "getf_filter";
	}

	@Override
	protected int[] getValidNumberOfArguments() {
		return null;
	}

	@Override
	protected void runCommand() throws Exception {

		setupArrayAndInfoHolder();

		for (int dayIndex = dayDataClassArray.size() - 1; dayIndex > -1 ; dayIndex--) {

			Observation day = dayDataClassArray.get(dayIndex);
			String date = day.getStringTypeVariable(dayInfoHolder, "date");

			Calendar dateCalendar = day.getDateVariable(dayInfoHolder, "date");
			Observation rfrDay = getRfrDay(date);
			Observation share = day.getObservationVariable(dayInfoHolder, "share");

			if (isEntryMissing(share, rfrDay) == true)
				dayDataClassArray.remove(dayIndex);

			else {

				int[] totalNumberOfOptions_day = new int[13], arbitrage = new int[13],
						itm = new int[13], noVolume = new int[13], tooCheap = new int[13],
						tooShortMaturity = new int[13], tooLongMaturity = new int[13],
						zeroBidPrice = new int[13], tooHighBidPrice = new int[13];

				for (int i = 0; i < 12; i++) {
					
					totalNumberOfOptions_day[i] = 0;
					arbitrage[i] = 0;
					itm[i] = 0;
					noVolume[i] = 0;
					tooCheap[i] = 0;
					tooShortMaturity[i] = 0;
					tooLongMaturity[i] = 0;
					zeroBidPrice[i] = 0;
					tooHighBidPrice[i] = 0;
					
				}
						
				double sharePrice = 
						Math.abs((double) share.getVariable(shareInfoHolder, "value"));

				ObList exdates = 
						day.getObListVariable(dayInfoHolder, "exdates");

				for (int exdatesIndex = exdates.size() - 1; 
						exdatesIndex > -1; exdatesIndex--) {

					Observation exdate = exdates.get(exdatesIndex);
					Calendar exdateCalendar = exdate.getDateVariable(exdateInfoHolder, "exdate");

					boolean isThisMaturityTraded = false;
					double dividendRate = getDividendRate(dateCalendar, exdateCalendar);
					double tau = Thesis1.getTdayTau(dateCalendar, exdateCalendar, tdayDataClassArray);

					Double rfrRate = Thesis1.getRfr
							(rfrDay, rfrInfoHolder, rateInfoHolder, 
									dateCalendar, exdateCalendar);									
					ObList options = 
							exdate.getObListVariable(exdateInfoHolder, "options");

					int[] totalNumberOfOptions_exdate = new int[13];
					
					for (int i = 0; i < 12; i++) {
						totalNumberOfOptions_exdate[i] = 0;
					}
					
					for (int i = 0; i < options.size(); i++) {
						Observation option = options.get(i);
						totalNumberOfOptions_exdate[getColumnNumber(option, sharePrice)]++;
						totalNumberOfOptions_exdate[12]++;
					}
					
					
					for (int i = 0; i < 12; i++) {
						totalNumberOfOptions_day[i] += totalNumberOfOptions_exdate[i];
					}

					if (tau * annualTradingDays < minimumTimeToMaturityInTradingDays){
						for (int i = 0; i < 12; i++) {
							tooShortMaturity[i] += totalNumberOfOptions_exdate[i];
						}
					}

					else if (tau * annualTradingDays >= maximumTimeToMaturityInTradingDays) {
						for (int i = 0; i < 12; i++) {
							tooLongMaturity[i] += totalNumberOfOptions_exdate[i];
						}
					}
					
					else {

						for (int optionsIndex = options.size() - 1; 
								optionsIndex > -1; optionsIndex--) {

							Observation option = options.get(optionsIndex);

							if (isTraded(option) == true)	
								isThisMaturityTraded = true;

						}

						if (isThisMaturityTraded == false) {
							for (int i = 0; i < 12; i++) {
								noVolume[i] += totalNumberOfOptions_exdate[i];
							}
						}

						else {

							for (int optionsIndex = options.size() - 1; 
									optionsIndex > -1; optionsIndex--) {

								Observation option = options.get(optionsIndex);
								int columnNumber = getColumnNumber(option, sharePrice);
								boolean[] arbitrageBoolean = doesViolateEuropeanArbitrageRestriction
										(option, sharePrice, dividendRate, rfrRate, tau); 
								
								if (isBidPriceZero(option)) {
									zeroBidPrice[columnNumber]++;
									zeroBidPrice[12]++;
								}	
								
								else if (isBidPriceHigherThanOfferPrice(option)) {
									tooHighBidPrice[columnNumber]++;
									tooHighBidPrice[12]++;
								}
								
								else if (arbitrageBoolean[0] == true) {
									arbitrage[columnNumber]++;
									arbitrage[12]++;
								}

								else if (isExpensiveEnough(option) == false) {
									tooCheap[columnNumber]++;
									tooCheap[12]++;
								}
								
								else if (isOTM(sharePrice, option) == false){
									itm[columnNumber]++;
									itm[12]++;
								}

							}

						}

					}

				}

				String result = date + ",";
				
				for (int i = 0; i < 13; i++) {
					result += totalNumberOfOptions_day[i] + ",";
				}
				
				for (int i = 0; i < 13; i++) {
					result += tooShortMaturity[i] + ",";
				}
				
				for (int i = 0; i < 13; i++) {
					result += tooLongMaturity[i] + ",";
				}

				for (int i = 0; i < 13; i++) {
					result += noVolume[i] + ",";
				}
				
				for (int i = 0; i < 13; i++) {
					result += zeroBidPrice[i] + ",";
				}
				
				for (int i = 0; i < 13; i++) {
					result += tooHighBidPrice[i] + ",";
				}

				for (int i = 0; i < 13; i++) {
					result += arbitrage[i] + ",";
				}

				for (int i = 0; i < 13; i++) {
					result += tooCheap[i] + ",";
				}
				
				for (int i = 0; i < 13; i++) {
					result += itm[i] + ",";
				}
				
				PublicWriter.write(result);

			}

		}

		DataClass.resetDataClass(dayDataClassArray);
		
	}

	public boolean[] doesViolateEuropeanArbitrageRestriction(
			Observation option, double sharePrice, double dividendRate,
			double rfr, double tau) throws ClassNotFoundException, 
			InstantiationException, IllegalAccessException, IllegalArgumentException, 
			InvocationTargetException, NoSuchMethodException, SecurityException, 
			BadLocationException {

		String cpFlag = option.getStringTypeVariable(optionInfoHolder, "cp");
		double bid = (double) option.getVariable(optionInfoHolder, "bid");
		double offer = (double) option.getVariable(optionInfoHolder, "offer");
		double strike = (double) option.getVariable(optionInfoHolder, "strike") / 1000.0; 
		double price = (bid + offer) / 2.0; 
		boolean[] result = {false, false};
		
		if (cpFlag.equals("C")) {

			if (price > sharePrice * Math.exp(-1.0 * dividendRate)) {
				result[0] = true;
				result[1] = true;
			}
		
			else if (price < Math.max(0, sharePrice * Math.exp(-1.0 * dividendRate * tau) - (strike * Math.exp(-1.0 * rfr * tau)))) {
				result[0] = true;
			}

		}

		else {

			if (price > sharePrice * Math.exp(-1.0 * dividendRate)) {
				result[0] = true;
				result[1] = false;
			}
				
			else if (price < Math.max(0, (strike * Math.exp(-1.0 * rfr * tau))) - sharePrice * Math.exp(-1.0 * dividendRate * tau))
				result[0] = true;

		}
		
		return result;

	}


	private int getColumnNumber(Observation option, double sharePrice) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, BadLocationException {
		
		String cpFlag = option.getStringTypeVariable(optionInfoHolder, "cp");
		double strike = (double) option.getVariable(optionInfoHolder, "strike") / 1000.0;
		double moneyness = strike / sharePrice;
		
		if (moneyness < 0.7) {
			if (cpFlag.equals("C"))
				return 0;
			else
				return 6;
		}
			 
		else if (moneyness < 0.85) {
			if (cpFlag.equals("C"))
				return 1;
			else
				return 7;
		}
		
		else if (moneyness < 1.0) {
			if (cpFlag.equals("C"))
				return 2;
			else
				return 8;
		}
		
		else if (moneyness < 1.15) {
			if (cpFlag.equals("C"))
				return 3;
			else
				return 9;
		}
		
		else if (moneyness < 1.3) {
			if (cpFlag.equals("C"))
				return 4;
			else
				return 10;
		}
		
		else {
			if (cpFlag.equals("C"))
				return 5;
			else
				return 11;			
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

	public boolean isBidPriceHigherThanOfferPrice(Observation option) throws ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException, 
	BadLocationException {

		double bid = (double) option.getVariable(optionInfoHolder, "bid");
		double offer = (double) option.getVariable(optionInfoHolder, "offer");

		if (bid > offer)
			return true;
		else 
			return false;

	}

	public boolean isBidPriceZero(Observation option) throws ClassNotFoundException, 
	InstantiationException, IllegalAccessException, IllegalArgumentException, 
	InvocationTargetException, NoSuchMethodException, SecurityException, 
	BadLocationException {

		double bid = (double) option.getVariable(optionInfoHolder, "bid");

		if (bid == 0.0)
			return true;
		else 
			return false;

	}


	private boolean isEntryMissing(Observation share, Observation rfr) 
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, 
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, 
			SecurityException, BadLocationException, NoSuchFieldException {

		String sharePriceString = share.getStringTypeVariable(shareInfoHolder, "value");

		if (sharePriceString.equals("") || rfr == null ||
				rfr.getObListVariable(rfrInfoHolder, "rates") == null) 
			return true;
		else 
			return false;

	}

	private boolean isExpensiveEnough(Observation option) 
			throws BadLocationException, ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {

		boolean isExpensiveEnough;
		double bid = Double.parseDouble(option.getStringTypeVariable(optionInfoHolder, "bid"));
		double offer = Double.parseDouble(option.getStringTypeVariable(optionInfoHolder, "offer"));
		double closingPrice = (bid + offer) / 2.0;

		if (closingPrice < minimumOptionPrice)
			isExpensiveEnough = false;

		else
			isExpensiveEnough = true;

		return isExpensiveEnough;

	}

	private boolean isOTM(double sharePrice, Observation option) 
			throws BadLocationException, ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {

		boolean isOTM;
		String cpFlag = option.getStringTypeVariable(optionInfoHolder, "cp");
		double strike = (double) option.getVariable(optionInfoHolder, "strike") / 1000.0; 

		if (cpFlag.equals("C") && strike > sharePrice)
			isOTM = true;

		else if (cpFlag.equals("P") && strike < sharePrice)
			isOTM = true;

		else
			isOTM = false;

		return isOTM;

	}

	public boolean isTraded(Observation option) 
			throws BadLocationException, ClassNotFoundException, InstantiationException, 
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
			NoSuchMethodException, SecurityException {

		boolean isTraded;
		int volume = Integer.parseInt(option.getStringTypeVariable(optionInfoHolder, "volume"));

		if (volume == 0)
			isTraded = false;

		else
			isTraded = true;

		return isTraded;

	}

	private void setupArrayAndInfoHolder() throws ClassNotFoundException, InstantiationException, 
	IllegalAccessException, IllegalArgumentException, InvocationTargetException, 
	NoSuchMethodException, SecurityException, BadLocationException {

		dayDataClassArray = DataClass.getDataClassArray("day");
		dividendDataClassArray = DataClass.getDataClassArray("dividendRate");
		rfrDataClassArray = DataClass.getDataClassArray("rfrDay");
		tdayDataClassArray = DataClass.getDataClassArray("tday");
		dividendInfoHolder = InfoHolder.getInfoHolder("dividend");
		dayInfoHolder = InfoHolder.getInfoHolder("day");
		exdateInfoHolder = InfoHolder.getInfoHolder("exdate");
		optionInfoHolder = InfoHolder.getInfoHolder("option");
		rfrInfoHolder = InfoHolder.getInfoHolder("rfr");
		rateInfoHolder = InfoHolder.getInfoHolder("rate");
		shareInfoHolder = InfoHolder.getInfoHolder("share");

	}

}
