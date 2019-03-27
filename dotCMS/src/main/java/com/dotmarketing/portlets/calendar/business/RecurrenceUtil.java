package com.dotmarketing.portlets.calendar.business;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.calendar.model.Event.Occurrency;
import com.dotmarketing.util.UtilMethods;

public class RecurrenceUtil {
	
	private static final String RECURRENCE_PREFIX = "-recurrence";
	private static final String RECURRENCE_SEPARATOR = "@";
	
	/**
	 * 
	 * @param recurrentIdentifier
	 * @return
	 */
	public static String getBaseEventIdentifier(String recurrentIdentifier){
		if(recurrentIdentifier.contains(RECURRENCE_PREFIX)){
			return recurrentIdentifier.substring(0, recurrentIdentifier.indexOf(RECURRENCE_PREFIX));
		}
		return recurrentIdentifier;
	}
	
	/**
	 * 
	 * @param recurrentEvent
	 * @return
	 */
	public static String getRecurrentEventIdentifier(Event recurrentEvent){
		return recurrentEvent.getIdentifier()+RECURRENCE_PREFIX
	       +RECURRENCE_SEPARATOR+recurrentEvent.getStartDate().getTime()
	       +RECURRENCE_SEPARATOR+recurrentEvent.getEndDate().getTime();
	}
	
	/**
	 * 
	 * @param recurrentEventIdentifier
	 * @return
	 */
	public static String[] getRecurrenceDates(String recurrentEventIdentifier){
		String[] recDates = null;
		if(recurrentEventIdentifier.contains(RECURRENCE_PREFIX) && 
				recurrentEventIdentifier.contains(RECURRENCE_SEPARATOR)){
				String idAux = "";
				try{
					idAux = recurrentEventIdentifier.substring(recurrentEventIdentifier.indexOf(RECURRENCE_SEPARATOR)+1);
				}catch(IndexOutOfBoundsException e){}
				if(UtilMethods.isSet(idAux)){
					recDates = idAux.split(RECURRENCE_SEPARATOR);
				}
			}
		return recDates;
		
	}
	
	/**
	 * 
	 * @param eventStartDate
	 * @param interval
	 * @param occurency
	 * @param recurrenceDaysOfWeeek
	 * @param recurrenceWeekOfMonth
	 * @param recurrenceDayOfWeek
	 * @param recurrenceMonthOfYear
	 * @param recurrenceDayOfMonth
	 * @return
	 */
	public static Calendar calculateFirstOccurence(Date eventStartDate, 
			Integer interval, Occurrency occurency, String recurrenceDaysOfWeeek,
			Integer recurrenceWeekOfMonth, Integer recurrenceDayOfWeek, 
			Integer recurrenceMonthOfYear, Integer recurrenceDayOfMonth ){

		GregorianCalendar auxFo = new GregorianCalendar();
		if(occurency!=null){
			switch(occurency) {
			case DAILY:
				interval = interval>1?interval:0;
				auxFo.setTime(eventStartDate);
				auxFo.add(Calendar.DAY_OF_MONTH, interval);
				break;
			case WEEKLY:
				auxFo.setTime(eventStartDate);
				for (int j = 1; j < 8; j++) {
					int x = auxFo.get(Calendar.DAY_OF_WEEK);
					if (recurrenceDaysOfWeeek.contains(String.valueOf(x))) {
						break;	
					}
					auxFo.add(Calendar.DAY_OF_MONTH, 1);
				}
				break;
			case MONTHLY:	
				GregorianCalendar fo = new GregorianCalendar();
				fo.setTime(eventStartDate);	
				int weekOfMonth = recurrenceWeekOfMonth;
				int dayOfWeek = recurrenceDayOfWeek;
				if(UtilMethods.isSet(recurrenceDayOfMonth) && (recurrenceDayOfMonth > 0)){
					GregorianCalendar baseCal = new GregorianCalendar();
					baseCal.set(Calendar.MONTH, fo.get(Calendar.MONTH));
					baseCal.set(Calendar.YEAR, fo.get(Calendar.YEAR));
					GregorianCalendar baseCal2 = new GregorianCalendar();
					baseCal2.set(Calendar.MONTH, fo.get(Calendar.MONTH));
					baseCal2.set(Calendar.YEAR, fo.get(Calendar.YEAR));

					int dayOfMonth = recurrenceDayOfMonth;
					baseCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
					while(baseCal.get(Calendar.MONTH) != baseCal2.get(Calendar.MONTH)){
						baseCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
						dayOfMonth--;
					}

					weekOfMonth = baseCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
					dayOfWeek = baseCal.get(Calendar.DAY_OF_WEEK);
				}

				auxFo = new GregorianCalendar();
				auxFo.set(Calendar.MONTH, fo.get(Calendar.MONTH));
				auxFo.set(Calendar.YEAR, fo.get(Calendar.YEAR));
				auxFo.set(Calendar.DAY_OF_WEEK, dayOfWeek); 
				if(weekOfMonth==1){
					auxFo.set(Calendar.DAY_OF_WEEK_IN_MONTH, 1);      
				}else{
					if(weekOfMonth<=4){
						auxFo.set(Calendar.DAY_OF_WEEK_IN_MONTH, weekOfMonth);
					}else{
						auxFo.set(Calendar.DAY_OF_WEEK_IN_MONTH, -1);                       	
					}
				}
				break;
			case ANNUALLY:
				fo = new GregorianCalendar();
				fo.setTime(eventStartDate);		
				weekOfMonth = recurrenceWeekOfMonth;
				dayOfWeek = recurrenceDayOfWeek;
				int monthOfYear = recurrenceMonthOfYear>0?recurrenceMonthOfYear-1:0;
				if(UtilMethods.isSet(recurrenceDayOfMonth) && (recurrenceDayOfMonth > 0)){
					GregorianCalendar baseCal = new GregorianCalendar();
					baseCal.set(Calendar.MONTH, monthOfYear);
					baseCal.set(Calendar.YEAR, fo.get(Calendar.YEAR));
					GregorianCalendar baseCal2 = new GregorianCalendar();
					baseCal2.set(Calendar.MONTH, monthOfYear);
					baseCal2.set(Calendar.YEAR, fo.get(Calendar.YEAR));

					int dayOfMonth = recurrenceDayOfMonth;
					baseCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
					while(baseCal.get(Calendar.MONTH) != baseCal2.get(Calendar.MONTH)){
						baseCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
						dayOfMonth--;
					}

					weekOfMonth = baseCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
					dayOfWeek = baseCal.get(Calendar.DAY_OF_WEEK);
				}
				auxFo = new GregorianCalendar();
				auxFo.set(Calendar.MONTH, monthOfYear);
				auxFo.set(Calendar.YEAR, fo.get(Calendar.YEAR));
				auxFo.set(Calendar.DAY_OF_WEEK, dayOfWeek); 
				if(weekOfMonth==1){
					auxFo.set(Calendar.DAY_OF_WEEK_IN_MONTH, 1);      
				}else{
					if(weekOfMonth<=4){
						auxFo.set(Calendar.DAY_OF_WEEK_IN_MONTH, weekOfMonth);
					}else{
						auxFo.set(Calendar.DAY_OF_WEEK_IN_MONTH, -1);                       	
					}
				}
				break;
			}
		}

		auxFo.set(Calendar.HOUR, 0);
		auxFo.set(Calendar.MINUTE, 0);
		auxFo.set(Calendar.SECOND,0); 
		auxFo.set(Calendar.MILLISECOND, 0); 
		return auxFo;
		
	}
	

}
