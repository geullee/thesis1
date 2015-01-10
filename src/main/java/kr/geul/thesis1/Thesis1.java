package kr.geul.thesis1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;

import javax.swing.text.BadLocationException;

import kr.geul.console.CommandLibrary;
import kr.geul.console.ExpansionPack;
import kr.geul.dataobject.DataClassArray;
import kr.geul.dataobject.DataClassInfoHolder;
import kr.geul.dataobject.ObList;
import kr.geul.dataobject.Observation;

public class Thesis1 extends ExpansionPack {

	protected CommandLibrary getCommandLibrary() {

		CommandLibrary library = new CommandLibrary("Thesis1");
		library.addCommand("bkms", "kr.geul.thesis1.command.GetBKM_spi");
		library.addCommand("bkms_eq", "kr.geul.thesis1.command.GetBKM_spi_equalize");
		library.addCommand("bkms_estimate", "kr.geul.thesis1.command.GetBKM_spi_estimate");
		library.addCommand("bkms_filter", "kr.geul.thesis1.command.GetBKM_spi_filter");
		library.addCommand("bkms_log", "kr.geul.thesis1.command.GetBKM_spi_initializeLog");
		library.addCommand("buildr", "kr.geul.thesis1.command.BuildRawClass");
		library.addCommand("exam", "kr.geul.thesis1.command.Examine");
		library.addCommand("exam_main", "kr.geul.thesis1.command.Examine_main");
		library.addCommand("filterm", "kr.geul.thesis1.command.FilterMissing");
		library.addCommand("getb", "kr.geul.thesis1.command.GetBasicProperties");
		library.addCommand("getb_log", "kr.geul.thesis1.command.GetBasicProperties_initializeLog");
		library.addCommand("getb_filter", "kr.geul.thesis1.command.GetBasicProperties_filter");
		library.addCommand("getb_get", "kr.geul.thesis1.command.GetBasicProperties_get");
		library.addCommand("getf", "kr.geul.thesis1.command.GetFiltrationDetails");
		library.addCommand("getf_log", "kr.geul.thesis1.command.GetFiltrationDetails_initializeLog");
		library.addCommand("getf_filter", "kr.geul.thesis1.command.GetFiltrationDetails_filter");
		library.addCommand("gett", "kr.geul.thesis1.command.GetTradingDays");
		library.addCommand("ped", "kr.geul.thesis1.command.GetParameterEstimates_daily");
		library.addCommand("ped_log", "kr.geul.thesis1.command.GetParameterEstimates_daily_initializeLog");
		library.addCommand("ped_filter", "kr.geul.thesis1.command.GetParameterEstimates_daily_filter");
		library.addCommand("ped_estimate", "kr.geul.thesis1.command.GetParameterEstimates_daily_estimate");
		library.addCommand("ped_bs", "kr.geul.thesis1.command.GetParameterEstimates_daily_bs");
		library.addCommand("ped_log_bs", "kr.geul.thesis1.command.GetParameterEstimates_daily_initializeLog_bs");
		library.addCommand("ped_estimate_bs", "kr.geul.thesis1.command.GetParameterEstimates_daily_estimate_bs");
		return library;

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

		if ((int) rates.get(0).getVariable(rfrRatesInfoHolder, "days") > days)
			finalRate = (double) rates.get(0).getVariable(rfrRatesInfoHolder, "rate") / 100.0;

		else {

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
			finalRate = (leftRate + (rightRate - leftRate) * dayRatio) / 100.0;

		}

		return finalRate;

	}

	public static double getRfr_fixed(Observation rfrDay,
			DataClassInfoHolder rfrDayInfoHolder,
			DataClassInfoHolder rfrRatesInfoHolder, double tau) 
			throws NoSuchFieldException, SecurityException, ClassNotFoundException, 
			InstantiationException, IllegalAccessException, IllegalArgumentException, 
			InvocationTargetException, NoSuchMethodException, BadLocationException {

		int leftDay = 0, rightDay = 0;
		double leftRate = 0, rightRate = 0;
		double finalRate;
		long tauInMillis = getTauInMillis(tau);
		Calendar tauCalendar = Calendar.getInstance();
		tauCalendar.setTimeInMillis(tauInMillis);
		int days = (tauCalendar.get(Calendar.YEAR) - 1970) * 365 + 
				tauCalendar.get(Calendar.DAY_OF_YEAR) - 1;

		ObList rates = rfrDay.getObListVariable(rfrDayInfoHolder, "rates");

		if ((int) rates.get(0).getVariable(rfrRatesInfoHolder, "days") > days)
			finalRate = (double) rates.get(0).getVariable(rfrRatesInfoHolder, "rate") / 100.0;

		else {

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
			finalRate = (leftRate + (rightRate - leftRate) * dayRatio) / 100.0;

		}

		return finalRate;

	}

	public static long getTauInMillis(double tau) {
		
		double oneYearInMillis = 31536000000.0;
		return Math.round(oneYearInMillis * tau);
		
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

	public static Calendar toCalendar(String string) {

		if (string != null && string.length() == 8) {

			Calendar result = Calendar.getInstance();

			int year = Integer.parseInt(string.substring(0, 4));
			int month = Integer.parseInt(string.substring(4, 6));
			int day = Integer.parseInt(string.substring(6, 8));

			result.set(year, month - 1, day);

			return result;

		}

		else
			return null;

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
