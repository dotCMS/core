package com.dotmarketing.util;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;


public class Calendar {
    static final String TIMEZONE = "EST";
    static final String[] MONTH_NAME = {
        "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November",
        "December"
    };

    public static Map getMap() {
        GregorianCalendar cal = new GregorianCalendar();

        return getMap(cal.get(GregorianCalendar.YEAR), cal.get(GregorianCalendar.MONTH),
            cal.get(GregorianCalendar.DAY_OF_MONTH));
    }

    public static Map getMap(String y, String m) {
        try {
            return getMap(Integer.parseInt(y), Integer.parseInt(m), 1);
        } catch (Exception e) {
            return getMap();
        }
    }

    public static Map getMap(int y, int m, int d) {
        if (y < 100) {
            y = y + 2000;
        }

        Logger.debug(Calendar.class, "Y:" + y);
        Logger.debug(Calendar.class, "m:" + m);
        Logger.debug(Calendar.class, "d:" + d);

        GregorianCalendar cal = new GregorianCalendar(y, m, d);
        GregorianCalendar today = new GregorianCalendar();
        java.util.HashMap modelRoot = new java.util.HashMap();

        int month = cal.get(GregorianCalendar.MONTH);
        int year = cal.get(GregorianCalendar.YEAR);

        int showmonth = month + 1;

        // simple error checking
        if ((month < 0) || (month > 11) || (year < 1995)) {
            month = cal.get(GregorianCalendar.MONTH);
            year = 1995;
        }

        // populate hashtable
        modelRoot.put("year", (new Integer(year).toString()));
        modelRoot.put("month", (new Integer(month).toString()));
        modelRoot.put("data", getCalendarData(month, year));
        modelRoot.put("today",
            today.get(GregorianCalendar.YEAR) + "-" + today.get(GregorianCalendar.MONTH) + "-" +
            today.get(GregorianCalendar.DAY_OF_MONTH));
        modelRoot.put("monthStr", MONTH_NAME[month]);
        modelRoot.put("showmonth", (new Integer(showmonth).toString()));
        modelRoot.put("nextYear", (new Integer(year + 1).toString()));
        modelRoot.put("prevYear", (new Integer(year - 1).toString()));
        modelRoot.put("prevMonth", (new Integer((month + 11) % 12).toString()));
        modelRoot.put("nextMonth", (new Integer((month + 01) % 12).toString()));
        modelRoot.put("timeZone", TIMEZONE);
        modelRoot.put("totalDays",cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH));

        return modelRoot;
    }

    private static List getCalendarData(int month, int year) {
        // initialize ArrayList and fill with spaces
        ArrayList monthData = new ArrayList();

        for (int row = 0; row < 6; row++) {
            ArrayList weekVect = new ArrayList();
            monthData.add(weekVect);

            for (int col = 0; col < 7; col++) {
                weekVect.add("");
            }
        }

        // populate actual data
        GregorianCalendar cal = new GregorianCalendar(year, month, 1);
        TimeZone tz = TimeZone.getTimeZone(TIMEZONE);
        cal.setTimeZone(tz);

        int totalDays = cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
        int prevcol = 0;
        int row = 0;

        for (int i = 1; i <= totalDays; i++) {
            cal.set(GregorianCalendar.DATE, i);

            int dayOfWeek = cal.get(GregorianCalendar.DAY_OF_WEEK);

            //int weekOfMonth = cal.get(GregorianCalendar.WEEK_OF_MONTH);
            int col = dayOfWeek - GregorianCalendar.SUNDAY;

            if (prevcol == 6) {
                row++;
            }

            ArrayList weekVect = ( ArrayList ) monthData.get(row);
            weekVect.set(col, new Integer(i));
            prevcol = col;
        }

        //remove last row if only 5 rows are needed
        if (row == 4) {
            monthData.remove(5);
        }

        ArrayList monthList = new ArrayList();

        for (int i = 0; i < monthData.size(); i++) {
            ArrayList weekList = new ArrayList();

            for (int j = 0; j < (( ArrayList ) monthData.get(i)).size(); j++) {
                weekList.add(( String ) (( ArrayList ) monthData.get(i)).get(j).toString());
            }

            monthList.add(weekList);
        }

        return monthList;
    }
}
