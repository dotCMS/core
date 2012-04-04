/*
 *  UtilMethods.java
 *
 *  Created on March 4, 2002, 2:56 PM
 */
package com.dotmarketing.util;

import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import com.csvreader.CsvReader;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portlets.containers.model.ContainerVersionInfo;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.files.model.FileAssetVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.htmlpages.model.HTMLPageVersionInfo;
import com.dotmarketing.portlets.links.model.LinkVersionInfo;
import com.dotmarketing.portlets.templates.model.TemplateVersionInfo;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;

/**
 * @author will
 * @created April 23, 2002
 */
/**
 * @author Carlos Rivas
 *
 */
public class UtilMethods {
    private static final java.text.SimpleDateFormat DATE_TO_INT_TIME = new java.text.SimpleDateFormat("Hmm");

    private static final java.text.SimpleDateFormat JDBC_TO_DATE = new java.text.SimpleDateFormat("yyyy-M-d H:mm:ss");

    private static final java.text.SimpleDateFormat DATE_TO_HTML_TIME = new java.text.SimpleDateFormat("h:mma");

    private static final java.text.SimpleDateFormat DATE_TO_JS_TIME = new java.text.SimpleDateFormat("H:mm");

    private static final java.text.SimpleDateFormat DATE_TO_JDBC = new java.text.SimpleDateFormat("yyyy-MM-dd H:mm:ss");

    private static final java.text.SimpleDateFormat DATE_TO_SHORT_JDBC = new java.text.SimpleDateFormat("yyyy-MM-dd");

    private static final java.text.SimpleDateFormat DATE_TO_YEAR = new java.text.SimpleDateFormat("yyyy");

    private static final java.text.SimpleDateFormat DATE_TO_HTML_DATE = new java.text.SimpleDateFormat("M/d/yyyy");

    private static final java.text.SimpleDateFormat DATE_TO_DAY_VIEW_DATE = new java.text.SimpleDateFormat("MMMM d");

    private static final java.text.SimpleDateFormat HTML_DATETIME_TO_DATE = new java.text.SimpleDateFormat("M/d/yyyy h:mm a");

    private static final java.text.SimpleDateFormat HTML_DATETIME24_TO_DATE = new java.text.SimpleDateFormat("M/d/yyyy H:mm");

    private static final java.text.SimpleDateFormat HTML_DB_TO_DATE = new java.text.SimpleDateFormat("yyyy-MM-dd H:mm:ss.S");

    private static final java.text.SimpleDateFormat HTML_DB_TO_DATE2 = DATE_TO_JDBC;	// More generic. It ignores millisecond component

    private static final java.text.SimpleDateFormat GOOGLE_DATETIME_TO_DATE = new java.text.SimpleDateFormat("yyyyMMddhhmmss");

    private static final java.text.SimpleDateFormat GOOGLE_DATETIME_TO_HTML = new java.text.SimpleDateFormat("dd/MM/yyyy");

    private static final java.text.SimpleDateFormat DATE_TO_CONTENT_EXPIRES_DATE = new java.text.SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

    private static final java.text.SimpleDateFormat DATE_TO_PRETTY_HTML_DATE = new java.text.SimpleDateFormat("EEE, MMMM d yyyy");

    private static final java.util.Map<String, String> _CC_MAPPINGS = new HashMap<String, String>();

    private static final java.text.SimpleDateFormat PIDMS_TEXT_TO_DATE = new java.text.SimpleDateFormat("yyyy/MM/dd h:mm a");

    private static final java.text.SimpleDateFormat DATE_TO_LONG_PRETTY_HTML_DATE = new java.text.SimpleDateFormat("EEE, d MMM yyyy hh:mm a");

    private static final java.text.SimpleDateFormat DATE_TO_PRETTY_HTML_DATE_2 = new java.text.SimpleDateFormat("MMMM d, yyyy");

    static {
        _CC_MAPPINGS.put("AMEX", "American Express");
        _CC_MAPPINGS.put("VISA", "Master Card / Visa");
        _CC_MAPPINGS.put("CHPW-AMEX", "American Express");
        _CC_MAPPINGS.put("CHPW-VISA-MC", "Master Card / Visa");
    }

    static HashMap<String, String> daysOfWeek = null;

    private static final String UTILMETHODS_DEFAULT_ENCODING = "UTF-8";

    public static final java.util.Date pidmsToDate(String d) {
        java.text.ParsePosition pos = new java.text.ParsePosition(0);

        return PIDMS_TEXT_TO_DATE.parse(d, pos);
    }

    public static final String join(String[] strArray, String separator) {
        StringBuffer strBuff = new StringBuffer();

        for (int k = 0; k < strArray.length; k++) {
            strBuff.append((String) strArray[k]).append(separator);
        }

        return strBuff.toString();
    }

    public static final String getCookieValue(javax.servlet.http.Cookie[] cookies, String cookieName) {
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                javax.servlet.http.Cookie cookie = cookies[i];

                if (cookieName.equals(cookie.getName())) {
                    return (cookie.getValue());
                }
            }
        }

        return null;
    }

    public static final Cookie getCookie(javax.servlet.http.Cookie[] cookies, String cookieName) {
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                javax.servlet.http.Cookie cookie = cookies[i];

                if (cookieName.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }

        return null;
    }

    public static final boolean isDateInRange(Date date, Date fromDate, Date toDate) {

        Calendar cal = Calendar.getInstance();
        if (fromDate != null) {
            cal.setTime(fromDate);
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            fromDate = cal.getTime();
        }

        if (toDate != null) {
            cal.setTime(toDate);
            cal.set(Calendar.HOUR, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            toDate = cal.getTime();
        }

        return (fromDate != null ? date.after(fromDate) : true) && (toDate != null ? date.before(toDate) : true);

    }

    public static final String dateToHTMLTimeRange(java.util.Date x, java.util.Date y) {
        String k = dateToHTMLTime(x);
        String l = dateToHTMLTime(y);

        if (k.equals(l)) {
            return k;
        } else {
            return k + "-" + l;
        }
    }

    public static final boolean isImage(String x) {
        if (x == null)
            return false;
        return (x.toLowerCase().endsWith(".gif") || x.toLowerCase().endsWith(".jpg") || x.toLowerCase().endsWith(".jpe")
                || x.toLowerCase().endsWith(".png") || x.toLowerCase().endsWith(".png") || x.toLowerCase().endsWith(".jpeg"));
    }

    public static final String getMonthFromNow() {
        java.util.GregorianCalendar cal = new java.util.GregorianCalendar();
        cal.roll(java.util.Calendar.MONTH, 1);

        return DATE_TO_CONTENT_EXPIRES_DATE.format(cal.getTime());
    }

    public static String escapeSingleQuotes(String fixme) {
        fixme = fixme.replaceAll("'", "\\\\'");
        return fixme;
    }

    public static String escapeDoubleQuotes(String fixme) {
        fixme = fixme.replaceAll("\"", "'");
        return fixme;
    }

    public static final String getMonthName(int x) {
        if ((x < 1) || (x > 12)) {
            x = 1;
        }

        String[] arr = { "", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };

        return arr[x];
    }

    public static final String getNextMonthName(int month) {
        if ((month < 1) || (month > 12)) {
            month = 1;
        }

        String[] arr = { "", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December", "January" };

        return arr[month];
    }

    public static final int getNextMonthNumber(int month) {
        return ((month < 1) || (month > 11)) ? (month = 1) : (month + 1);
    }

    public static final String getPreviousMonthName(int x) {
        if ((x < 1) || (x > 12)) {
            x = 1;
        }

        String[] arr = { "", "December", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November" };

        return arr[x];
    }

    public static final int getPreviousMonthNumber(int month) {
        return ((month < 2) || (month > 12)) ? (month = 12) : (month - 1);
    }

    public static final boolean isSet(String x) {
        if (x == null) {
            return false;
        }

        x = x.toLowerCase();

        if (x.indexOf("null") > -1) {
            x = x.replaceAll("null", "");
        }

        return (x.trim().length() > 0);
    }

    public static final boolean isSet(java.util.Date x) {
        return ((x != null) && (x.getTime() > 0));
    }

    public static final boolean isSet(Float x) {
        return (x != null);
    }

    public static final boolean isSet(Object x) {
        return (x != null);
    }

    public static final boolean isSetCrumb(String x) {
        return (isSet(x) && !x.equals("index"));
    }

    public static final boolean isSetHTML(String x) {
        if (x == null) {
            return false;
        }

        x = x.toLowerCase();
        x = x.replaceAll("null", "");
        x = x.replaceAll("<[^>]*>", "");
        x = x.replace('\0', ' ');
        Logger.debug(UtilMethods.class, "X:" + x + ":X");

        return (x.trim().length() > 1);
    }

    public static final boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }

        return java.util.regex.Pattern.matches("^[\\w-\\.]{1,}\\@([\\da-zA-Z-]{1,}\\.){1,}[\\da-zA-Z-]{2,4}$", email);
    }

    public static final boolean isValidURL(String url) {
        if (url == null) {
            return false;
        }

        return java.util.regex.Pattern
                .matches(
                        "((http|ftp|https):\\/\\/w{3}[\\d]*.|(http|ftp|https):\\/\\/|w{3}[\\d]*.)([\\w\\d\\._\\-#\\(\\)\\[\\]\\\\,;:]+@[\\w\\d\\._\\-#\\(\\)\\[\\]\\\\,;:])?([a-z0-9]+.)*[a-z\\-0-9]+.([a-z]{2,3})?[a-z]{2,6}(:[0-9]+)?(\\/[\\/a-z0-9\\._\\-,]+)*[a-z0-9\\-_\\.\\s\\%]+(\\?[a-z0-9=%&\\.\\-,#]+)?",
                        url);
        // UrlValidator val = new UrlValidator();
        // return val.isValid(url);

    }

    public static final boolean isValidEmail(Object email) {
        if (email == null) {
            return false;
        }

        return isValidEmail((String) email);
    }

    /**
     * Description of the Method
     * 
     * @param cmdline
     *            Description of the Parameter
     * @return Description of the Return Value
     * 
     * public static final String CmdExec(String cmdline) { StringBuffer sb =
     * new StringBuffer();
     * 
     * try { String line; Process p = Runtime.getRuntime().exec(cmdline);
     * BufferedReader input = new BufferedReader(new InputStreamReader(p
     * .getInputStream()));
     * 
     * while ((line = input.readLine()) != null) { sb.append(line); }
     * 
     * input.close(); } catch (Exception err) { sb.append(err); }
     * 
     * return sb.toString(); }
     */
    public static final String dateToDayViewDate(java.util.Date x) {
        if (x == null) {
            return "";
        }

        return DATE_TO_DAY_VIEW_DATE.format(x);
    }

    public static final String dateToHTMLDateTimeRange(java.util.Date x, java.util.Date y, TimeZone tz) {
        String i = dateToHTMLDate(x, tz);
        String j = dateToHTMLDate(y, tz);
        String k = dateToHTMLTime(x, tz);
        String l = dateToHTMLTime(y, tz);

        if (i.equals(j) && k.equals(l)) {
            return i + " &nbsp; " + k;
        } else if (i.equals(j)) {
            return i + " &nbsp; " + k + "-" + l;
        } else {
            return i + " - " + j;
        }
    }

    public static final String dateToHTMLDate(java.util.Date x) {
        if (x == null) {
            return "";
        }
        return DATE_TO_HTML_DATE.format(x);
    }

    public static final String dateToHTMLDate(java.util.Date x, String format) {
        if (x == null) {
            return "";
        }
        java.text.SimpleDateFormat mySimpleFormat = new java.text.SimpleDateFormat(format);
        return mySimpleFormat.format(x);
    }

    public static final String dateToHTMLDate(java.util.Date x, TimeZone tz) {
        if (x == null) {
            return "";
        }
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("M/d/yyyy");
        formatter.setTimeZone(tz);
        return formatter.format(x);
    }

    public static final String dateToHTMLDateRange(java.util.Date x, java.util.Date y) {
        String i = dateToHTMLDate(x);
        String j = dateToHTMLDate(y);

        if (i.equals(j)) {
            return i;
        } else {
            return i + " - " + j;
        }
    }

    public static final String dateToHTMLDateRange(java.util.Date x, java.util.Date y, TimeZone tz) {
        String i = dateToHTMLDate(x, tz);
        String j = dateToHTMLDate(y, tz);

        if (i.equals(j)) {
            return i;
        } else {
            return i + " - " + j;
        }
    }

    public static final String dateToHTMLDateTimeRange(java.util.Date x, java.util.Date y) {
        String i = dateToHTMLDate(x);
        String j = dateToHTMLDate(y);
        String k = dateToHTMLTime(x);
        String l = dateToHTMLTime(y);

        if (i.equals(j) && k.equals(l)) {
            return i + " &nbsp; " + k;
        } else if (i.equals(j)) {
            return i + " &nbsp; " + k + "-" + l;
        } else {
            return i + " - " + j;
        }
    }

    public static final String dateToHTMLTime(java.util.Date x) {
        if (x == null) {
            return "";
        }
        DATE_TO_HTML_TIME.setTimeZone(Calendar.getInstance().getTimeZone());
        return DATE_TO_HTML_TIME.format(x);
    }

    public static final String dateToHTMLTime(java.util.Date x, TimeZone tz) {
        if (x == null) {
            return "";
        }

        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("h:mm a");
        formatter.setTimeZone(tz);
        return formatter.format(x);
    }

    public static final int dateToIntTime(java.util.Date x) {
        return Integer.parseInt(DATE_TO_INT_TIME.format(x));
    }

    public static final String dateToJDBC(java.util.Date x) {
        return DATE_TO_JDBC.format(x);
    }

    public static final String dateToShortJDBC(java.util.Date x) {
        return DATE_TO_SHORT_JDBC.format(x);
    }

    public static final String dateToShortJDBCForQuery(java.util.Date x) {
    	if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)) {
    		return "TO_DATE('" + DATE_TO_SHORT_JDBC.format(x) + "','YYYY-MM-DD')";
    	} else {
    		return "'" + DATE_TO_SHORT_JDBC.format(x) + "'";
    	}
    }

    public static final String dateToJSTime(java.util.Date x) {
        return DATE_TO_JS_TIME.format(x);
    }

    public static final String dateToPrettyHTMLDate(java.util.Date x) {
        if (x == null) {
            return "";
        }

        return DATE_TO_PRETTY_HTML_DATE.format(x);
    }

    /**
     * Takes a date and return a string with the date formatted as DD/MM/YYYY
     * 
     * @param x
     *            Date to format
     * @return
     */
    public static final String dateToGoogleDate(java.util.Date x) {
        if (x == null) {
            return "";
        }
        return GOOGLE_DATETIME_TO_HTML.format(x);
    }

    public static final String dateToPrettyHTMLDate(java.util.Date x, TimeZone tz) {
        if (x == null) {
            return "";
        }
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("EEE, MMMM d yyyy");
        formatter.setTimeZone(tz);
        return formatter.format(x);
    }

    public static final String dateToYear(java.util.Date x) {
        return DATE_TO_YEAR.format(x);
    }

    public static final String dollarFormat(float f) {
        if (f == 0) {
            return "0.00";
        }

        java.text.DecimalFormat cf = new java.text.DecimalFormat("########.00");

        return cf.format(f);
    }

    /*
     * public final static String capitalize(String var) { if (var != null &&
     * var.length() > 1) { return var.substring(0, 1).toUpperCase() +
     * var.substring(1); } else { return var; } }
     */
    public static final String formatter(String original, String from, String to) {
        return replace(original, from, to);
    }

    public static final StringBuffer formatterStringBuffer(StringBuffer original, String from, String to) {
        return replaceStringBuffer(original, from, to);
    }

    public static final StringBuffer replaceStringBuffer(StringBuffer original, String from, String to) {
        StringBuffer finished = new StringBuffer();

        if (original == null) {
            return finished;
        }

        // This method takes a string and replaces the line feed with an
        // html line feed
        int start = 0;
        int index = original.indexOf(from);

        while (index != -1) {
            finished.append(original.substring(start, index));
            finished.append(to);
            start = index + from.length();
            index = original.indexOf(from, start);
        }

        finished.append(original.substring(start));

        return finished;
    }

    public static final java.util.Date htmlDateTimeToDate(String d) {
        java.util.Date rDate = null;
        java.text.ParsePosition pos = new java.text.ParsePosition(0);
        TimeZone tz = Calendar.getInstance().getTimeZone();
        HTML_DATETIME_TO_DATE.setTimeZone(tz);
        rDate = HTML_DATETIME_TO_DATE.parse(d, pos);

        if (rDate == null) {
        	HTML_DATETIME24_TO_DATE.setTimeZone(tz);
            rDate = HTML_DATETIME24_TO_DATE.parse(d, pos);
        }

        if (rDate == null) {
        	HTML_DB_TO_DATE.setTimeZone(tz);
            rDate = HTML_DB_TO_DATE.parse(d, pos);
        }

        if (rDate == null) {
        	HTML_DB_TO_DATE2.setTimeZone(tz);
            rDate = HTML_DB_TO_DATE2.parse(d, pos);	// Try to parse a db date without millisecond component 
        }

        if (rDate == null) {
            rDate = new java.util.Date();
        }

        return rDate;
    }

    public static final String htmlLineBreak(String original) {
        // This method takes a string and replaces the line feed with an
        // html line feed
        if (original == null) {
            return "";
        }

        return original.replaceAll("\r", "").replaceAll("\n\n", "<br/>&nbsp;<br/>").replaceAll("\n", "<br/>");

        // return original;
    }

    public static final java.util.Date htmlToDate(String d) {
        java.util.Date rDate = null;
        java.text.ParsePosition pos = new java.text.ParsePosition(0);
        DATE_TO_HTML_DATE.setLenient(true);
        rDate = (java.util.Date) DATE_TO_HTML_DATE.parse(d, pos);

        // need to find non deprecaited method to do this, but it works
        // rDate.setHours(12);
        return rDate;
    }

    /**
     * Take a string and converts it to Date using the google mini search format
     * 
     * @param d
     *            string date to converts
     * @return
     */
    public static final java.util.Date googleDateToDate(String d) {
        java.util.Date rDate = null;
        java.text.ParsePosition pos = new java.text.ParsePosition(0);
        GOOGLE_DATETIME_TO_DATE.setLenient(true);
        rDate = (java.util.Date) GOOGLE_DATETIME_TO_DATE.parse(d, pos);
        return rDate;
    }

    public static final java.util.Date htmlToDate(java.util.Date rDate) {
        return rDate;
    }

    public static final java.util.Date jdbcToDate(String d) {
        java.util.Date rDate = null;
        if (!isSet(d)) {
            return rDate;
        }
        java.text.ParsePosition pos = new java.text.ParsePosition(0);
        rDate = JDBC_TO_DATE.parse(d, pos);

        if (rDate == null) {
            rDate = new java.util.Date();
        }

        return rDate;
    }

    /*
     * Expecting a string like yyyy-MM-dd hh:mm:ss
     * 
     */
    public static String jdbcDateToHtml(String jdbcdate) {
        if (!isSet(jdbcdate)) {
            return "";
        }

        if (jdbcdate.indexOf("/") > 0) {
            return jdbcdate;
        }

        StringBuffer out = new StringBuffer("");

        if ((jdbcdate.indexOf("-") > 0) && (jdbcdate.lastIndexOf("-") > jdbcdate.indexOf("-"))) {
            // dirty but let's make suer we have 2 dashes
            String[] parts = jdbcdate.split("-");
            String day = parts[2];

            if (day.indexOf(" ") > 0) {
                day = day.substring(0, day.indexOf(" "));
            }

            out.append(parts[1]);
            out.append("/");
            out.append(day);
            out.append("/");
            out.append(parts[0]);
        }

        return out.toString();
    }

    public static final String obfuscateEmail(String email) {
        // This method takes a string and replaces the line feed with an
        // html line feed
        if (email == null) {
            return "";
        }

        StringTokenizer st = new StringTokenizer(email, "@");

        if (st.countTokens() < 2) {
            return "";
        }

        StringBuffer sb = new StringBuffer();
        sb.append("<script>document.write('" + st.nextToken() + "');");
        sb.append("document.write('@');");
        sb.append("document.write('" + st.nextToken() + "');</script>");

        return sb.toString();
    }

    /**
     * Use 
     * @deprecated Use obfuscateEmail and create the <a> in your html
     * @param email
     * @return
     */
    public static final String obfuscateEmailHref(String email) {
        // This method takes a string and replaces the line feed with an
        // html line feed
        if (email == null) {
            return "";
        }

        StringTokenizer st = new StringTokenizer(email, "@");

        if (st.countTokens() < 2) {
            return "";
        }

        StringBuffer sb = new StringBuffer();
        sb.append("<script>document.write('<a href=\"mailto:" + st.nextToken() + "');");
        sb.append("document.write('@');");
        sb.append("document.write('" + st.nextToken() + "\">');</script>");

        return sb.toString();
    }

    public static final String obfuscateEmailHrefClass(String email, String className) {
        // This method takes a string and replaces the line feed with an
        // html line feed
        if (email == null) {
            return "";
        }

        StringTokenizer st = new StringTokenizer(email, "@");

        if (st.countTokens() < 2) {
            return "";
        }

        StringBuffer sb = new StringBuffer();
        sb.append("<script>document.write('<a href=\"mailto:" + st.nextToken() + "');");
        sb.append("document.write('@');");
        sb.append("document.write('" + st.nextToken() + "\" class=\"" + className + "\">');</script>");

        return sb.toString();
    }

    public static String prettyString(String text) {
        if (text == null)
            return "";
        return text;
    }

    
    public static String prettyMemory(long memory) {
        return prettyByteify(memory);
    }
    
    
    
    
    
    
    public static String prettyString(String text, String alternateText) {
        if (text == null)
            return alternateText;
        return text;
    }

    public static String prettyShortenString(String text, int maxLength) {
        if (text == null) {
            return "";

        }
        StringBuffer sb = new StringBuffer();
        StringTokenizer st = new StringTokenizer(text, " ,-\n&()=;_", true);

        while (st.hasMoreTokens()) {
            String token = st.nextToken();

            if ((sb.length() + token.length()) > maxLength) {
                sb.append("...");

                break;
            }

            sb.append(token);
        }

        return sb.toString();
    }

    public static final String replace(String original, String from, String to) {
        if (original == null) {
            return "";
        }

        // This method takes a string and replaces the line feed with an
        // html line feed
        StringBuffer finished = new StringBuffer();
        int start = 0;
        int index = original.indexOf(from);

        while (index != -1) {
            finished.append(original.substring(start, index));
            finished.append(to);
            start = index + from.length();
            index = original.indexOf(from, start);
        }

        finished.append(original.substring(start));

        return finished.toString();
    }

    public static final String shortenString(String s, int l) {
        s = webifyString(s);

        return (s.length() < l) ? s : (s.substring(0, l) + "...");
    }

    public static final boolean similarStrings(String a, String b) {
        if (a == null) {
            a = "";
        }

        if (b == null) {
            b = "";
        }

        a = ((a.length() < 15) ? a : a.substring(0, 15)).replaceAll("\\W", "");
        b = ((b.length() < 15) ? b : b.substring(0, 15)).replaceAll("\\W", "");

        return a.equals(b);
    }

    public static final String webifyString(String x) {
        if ((x == null) || x.trim().equals("") || x.trim().equals("null")) {
            return "";
        } else {
            x = x.replaceAll("\"", "&quot;");
        }

        return x.trim();
    }

    public static final String xmlifyString(String x) {
        if ((x == null) || x.trim().equals("") || x.trim().equals("null")) {
            return "";
        } else {
            x = x.replaceAll("&", "and");
            x = x.replaceAll("<", "");
            x = x.replaceAll(">", "");
            x = x.replaceAll("\"", "");
            x = x.replaceAll("'", "");
        }

        return x.trim();
    }

    public static final String wrapLines(String original, int wrap) {
        // This method takes a string and replaces the line feed with an
        // html line feed
        if (original == null) {
            return "";
        }

        StringBuffer sb = new StringBuffer();
        String[] words = original.split(" ");
        int charCount = 0;

        for (int i = 0; i < words.length; i++) {
            if (words[i].indexOf("\r\n\r\n") > -1) {
                sb.append("<BR>&nbsp;<BR>");
            }

            sb.append(words[i] + " ");
            charCount = charCount + words[i].length() + 1;

            if (charCount > wrap) {
                sb.append("<BR>");
                charCount = 0;
            }
        }

        return sb.toString();
    }

    public static String getFileExtension(String x) {
    	String r = "";
        try {
            if (x.lastIndexOf(".") != -1) {
                return x.substring(x.lastIndexOf(".") + 1).toLowerCase();
            } else {
                return r;
            }
        } catch (Exception e) {
            return "ukn";
        }
    }

    public static String getFileName(String x) {
        try {
            if (x.lastIndexOf("/") > -1 || x.lastIndexOf("\\") > -1) {
                int idx = (x.lastIndexOf("/") > x.lastIndexOf("\\") ? x.lastIndexOf("/") : x.lastIndexOf("\\")) + 1;
                x = x.substring(idx, x.length());
            }
            if (x.lastIndexOf(".") != -1) {
                int begin = 0;
                if (x.lastIndexOf("/") != -1) {
                    begin = x.lastIndexOf("/") + 1;
                }
                return x.substring(begin, x.lastIndexOf("."));
            } else {
                return x;
            }
        } catch (Exception e) {
            return "";
        }
    }

    public static java.util.List getTimeList(int start, int duration) {
        java.util.Calendar cal = new java.util.GregorianCalendar();
        List<String> out = new ArrayList<String>();
        cal.set(java.util.Calendar.HOUR, start);
        cal.set(java.util.Calendar.MINUTE, 0);

        if (start >= 12) {
            cal.set(java.util.Calendar.AM_PM, java.util.Calendar.PM);
        } else {
            cal.set(java.util.Calendar.AM_PM, java.util.Calendar.AM);
        }

        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("hh:mm a");

        for (int i = 0; i < ((duration * 4) + 1); i++) {
            out.add(format.format(cal.getTime()));
            cal.add(java.util.Calendar.MINUTE, 15);
        }

        return out;
    }

    public static StringBuffer getURL(String URI) throws java.net.ConnectException{
        StringBuffer html = new StringBuffer();
        html.append("");

        try {
        	System.setProperty("sun.net.client.defaultReadTimeout","20000"); 
    		System.setProperty("sun.net.client.defaultConnectTimeout","10000"); 
			 java.net.URL pointer = new java.net.URL(URI);
			 
			 java.net.URLConnection conn = pointer.openConnection();
			 conn.setUseCaches(false);
			 conn.setConnectTimeout(10000);
			 if(conn instanceof java.net.HttpURLConnection){
				 java.net.HttpURLConnection myConn = (java.net.HttpURLConnection)conn;
				 myConn.setRequestMethod("POST");
				 if(myConn.getResponseCode() != HttpServletResponse.SC_OK){
					 return null;
				 }
			 }

			 BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			 String inputLine;

			 while ((inputLine = in.readLine()) != null) {
				 html.append(inputLine + "\n");
			 }

			 in.close();
		 } catch (Exception e) {
			 Logger.debug(UtilMethods.class, "Browser class failed to get page: " + URI + " - " + e, e);
			 Logger.warn(UtilMethods.class, "Browser class failed to get page: " + URI + " - " + e);
		 }

        return html;
    }

    public static String capitalize(String s) {
        if (s == null) {
            return "";
        }

        if (s.indexOf(".") > -1) {
            s = s.substring(0, s.lastIndexOf("."));
        }

        char[] chars = s.toLowerCase().toCharArray();

        // java.util.ArrayList al = new java.util.ArrayList();
        boolean capitalNext = true;

        for (int i = 0; i < chars.length; i++) {
            char x = chars[i];

            if (capitalNext) {
                x = Character.toUpperCase(chars[i]);
                capitalNext = false;
            }

            if (!Character.isLetterOrDigit(x)) {
                x = ' ';
                capitalNext = true;
            }

            chars[i] = x;
        }

        return new String(chars);
    }

    public static String capitalize(Object s) {
        try {
            return capitalize(s.toString());
        } catch (Exception e) {
            return "";
        }
    }

    public static String csvifyString(String x) {
        return webifyString(x).replace(',', ' ');
    }

    public static String htmlifyString(String x) {
        return webifyString(x).replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    /**
     * This method will take a url and make sure it has the protocol (http://)
     * portion set. Pass it www.dotcms.org and it will return
     * http://www.dotcms.org. Pass it http://www.dotcms.org and it will return
     * http://www.dotcms.org
     * 
     * @param x
     *            The string to check to make sure it starts with http
     * @return The string with http:// prepended if needed
     */
    public static String httpifyString(String x) {
        if (x == null) {
            return null;
        }

        String testString = x.trim().toLowerCase();

        if (testString.startsWith("http://") || testString.startsWith("https://") || testString.startsWith("mailto:")
                || testString.startsWith("ftp://")) {
            return x;
        } else {
            return ("http://" + x);
        }
    }

    public static String javaScriptify(String x) {
        if (x == null) {
            return "";
        } else {
            x = x.replaceAll("'", "\\\\'" ).replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
            return x;
        }
    }


    public static String javaScriptifyVariable(String x) {
        if (x == null) {
            return "";
        } else {
            x = x.replaceAll("[^A-Za-z0-9_]", "");
            return x;
        }
    }

    public static String truncatify(String x) {
    	if(!isSet(x)){
    		return "";
    	}
        if (x.length() > 15) {
            java.util.StringTokenizer st2 = new java.util.StringTokenizer(x, "_- ;,.", true);
            StringBuffer buffer = new StringBuffer();

            while (st2.hasMoreTokens()) {
                buffer.append(st2.nextToken());

                if (buffer.length() > 15) {
                    if (st2.hasMoreTokens()) {
                        buffer.append("...");
                    }

                    break;
                }
            }

            return buffer.toString();
        } else {
            return x;
        }
    }

    public static String truncatify(String x, int len) {
    	if(!isSet(x)){
    		return "";
    	}
        if (x.length() > len) {
            java.util.StringTokenizer st2 = new java.util.StringTokenizer(x, "_- ;,.", true);
            StringBuffer buffer = new StringBuffer();

            while (st2.hasMoreTokens()) {
                buffer.append(st2.nextToken());

                if (buffer.length() > len) {
                    if (st2.hasMoreTokens()) {
                        buffer.append("...");
                    }

                    break;
                }
            }

            return buffer.toString();
        } else {
            return x;
        }
    }

    public static String prettyByteify(long memory) {
    	Double x = new Double(memory);
    	NumberFormat nf = new DecimalFormat("#.0");
        String myBytes = null;
        if (x > (1024 * 1024 * 1024)) {
        	myBytes = nf.format(((x / (1024 * 1024 * 1024)) )) + " G";
        }
        else if (x > (1024 * 1024)) {
            myBytes = nf.format(((x / (1024 * 1024)) )) + " M";
        } else if (x > 1024) {
            myBytes = nf.format(((x / (1024)) )) + " K";
        } else if (x > 1) {
            myBytes =nf.format( x ) + " B";
        }
        else  {
            myBytes ="0 b";
        }
        return myBytes;
    }

    public static String cleanURI(String uri) {
        // if we are looking for an index page
        if (uri.indexOf(".") < 0) {
            if (!uri.endsWith("/")) {
                uri += "/";
            }

            uri += "index.html";
        }

        return uri;
    }

    public static String cleanFileSystemPathURI(String path) {
        if (path.indexOf("..") != -1 && path.indexOf("WEB-INF") != -1 && path.indexOf("META-INF") != -1 && path.indexOf("!") != -1
                && path.indexOf(":") != -1 && path.indexOf(";") != -1 && path.indexOf(";") != -1 && path.indexOf("&") != -1
                && path.indexOf("?") != -1 && path.indexOf("$") != -1 && path.indexOf("*") != -1 && path.indexOf("\"") != -1
                && path.indexOf("/") != -1 && path.indexOf("[") != -1 && path.indexOf("]") != -1 && path.indexOf("=") != -1
                && path.indexOf("|") != -1 && path.indexOf(",") != -1) {
            return null;

        } else {
            return path;

        }

    }

    
    
    public static String validateFileName(String fileName) throws IllegalArgumentException{
    	
        if (!isSet(fileName)  || 
        		fileName.indexOf("..") != -1 || fileName.indexOf("WEB-INF") != -1 || fileName.indexOf("META-INF") != -1 || fileName.indexOf("!") != -1
                || fileName.indexOf(":") != -1 || fileName.indexOf(";") != -1 || fileName.indexOf(";") != -1 || fileName.indexOf("&") != -1
                || fileName.indexOf("?") != -1 || fileName.indexOf("$") != -1 || fileName.indexOf("*") != -1 || fileName.indexOf("\"") != -1
                || fileName.indexOf("/") != -1 || fileName.indexOf("[") != -1 || fileName.indexOf("]") != -1 || fileName.indexOf("=") != -1
                || fileName.indexOf("|") != -1 || fileName.indexOf(",") != -1) {
            throw new IllegalArgumentException("Invalid Filename passed in: " + fileName);

        } else {
            return fileName;

        }

    }
    public static String getPageChannel(String uri) {
        java.util.StringTokenizer st = new java.util.StringTokenizer(String.valueOf(uri), "/");
        String pageChannel = null;

        if (st.hasMoreTokens()) {
            pageChannel = st.nextToken();
        }

        return pageChannel;
    }

    public static String espaceForVelocity(String text) {
        if (isSet(text)) {
            text = replace(text, "\"", "${quote}");
            text = replace(text, "##", "${pounds}");
            text = replace(text, "�", "'");
            text = replace(text, "�", "-");
            text = replace(text, "�", "\\�");
            return text.trim();
        }

        return "";
    }

    // Uses by the code generated in the contentletmapservices
    public static String espaceVariableForVelocity(String text) {
        if (isSet(text)) {
            text = replace(text, "'", "${singleQuote}");
            text = replace(text, "##", "${pounds}");
            text = replace(text, "\\", "${backSlash}");
            return text;
        }

        return "";
    }

    // Uses by the code generated in the contentletmapservices
    public static String restoreVariableForVelocity(String text) {
        if (isSet(text)) {
            text = text.replaceAll("\\$\\{singleQuote}", "\'");
            text = text.replaceAll("\\$\\{pounds}", "##");
            text = text.replaceAll("\\$\\{backSlash}", "\\\\");
        }

        return text;
    }

    // Used by the code generated in the contentletmapservices
    public static String evaluateVelocity(String vtl, Context ctx) {
        try {
            StringWriter sw = new StringWriter();
            //Was put in to fix DOTCMS-995 but it caused DOTCMS-1210.  
//            I actually think it should be fine passed the ctx which is a chained context here
//            VelocityContext vc = pushVelocityContext(ctx);
            VelocityEngine ve = VelocityUtil.getEngine();
            boolean success = ve.evaluate(ctx, sw, "RenderTool.eval()", vtl);
            if (success)
                return sw.toString();
            else
                return null;
        } catch (Exception e) {
            Logger.debug(UtilMethods.class, "Error evaluating velocity code: " + vtl, e);
            return "Syntax Error: " + UtilMethods.htmlLineBreak(e.getMessage());
        }
    }

    public static Context pushVelocityContext(Context ctx) {
//    	/Was put in to fix DOTCMS-995 but it caused DOTCMS-1210.  
//      I actually think it should be fine passed the ctx which is a chained context here
//    	return new VelocityContext(ctx);
    	return ctx;
    }
    public static Context popVelocityContext(VelocityContext vctx) {
    	return vctx.getChainedContext();
    }

    public static String escapeUnicodeCharsForHTML(String valueSt) {
    	
        // inverted exclamation mark �
        valueSt = valueSt.replaceAll("\u00A1", "&iexcl;");
        // cent sign �
        valueSt = valueSt.replaceAll("\u00A2", "&cent;");
        // pound sign �
        valueSt = valueSt.replaceAll("\u00A3", "&pound;");
        // currency sign �
        valueSt = valueSt.replaceAll("\u00A4", "&curren;");
        // yen sign �
        valueSt = valueSt.replaceAll("\u00A5", "&yen;");
        // broken bar �
        valueSt = valueSt.replaceAll("\u00A6", "&brvbar;");
        // section sign �
        valueSt = valueSt.replaceAll("\u00A7", "&sect;");
        // diaeresis �
        valueSt = valueSt.replaceAll("\u00A8", "&uml;");
        // copyright sign �
        valueSt = valueSt.replaceAll("\u00A9", "&copy;");
        // feminine ordinal indicator �
        valueSt = valueSt.replaceAll("\u00AA", "&ordf;");
        // left-pointing double angle quotation mark �
        valueSt = valueSt.replaceAll("\u00AB", "&laquo;");
        // not sign �
        valueSt = valueSt.replaceAll("\u00AC", "&not;");
        // soft hyphen �
        valueSt = valueSt.replaceAll("\u00AD", "&shy;");
        // registered sign �
        valueSt = valueSt.replaceAll("\u00AE", "&reg;");
        // macron �
        valueSt = valueSt.replaceAll("\u00AF", "&macr;");
        // degree sign �
        valueSt = valueSt.replaceAll("\u00B0", "&deg;");
        // plus-minus sign �
        valueSt = valueSt.replaceAll("\u00B1", "&plusmn;");
        // superscript two �
        valueSt = valueSt.replaceAll("\u00B2", "&sup2;");
        // superscript three �
        valueSt = valueSt.replaceAll("\u00B3", "&sup3;");
        // acute accent �
        valueSt = valueSt.replaceAll("\u00B4", "&acute;");
        // micro sign �
        valueSt = valueSt.replaceAll("\u00B5", "&micro;");
        // pilcrow sign �
        valueSt = valueSt.replaceAll("\u00B6", "&para;");
        // middle dot �
        valueSt = valueSt.replaceAll("\u00B7", "&middot;");
        // cedilla �
        valueSt = valueSt.replaceAll("\u00B8", "&cedil;");
        // superscript one �
        valueSt = valueSt.replaceAll("\u00B9", "&sup1;");
        // masculine ordinal indicator �
        valueSt = valueSt.replaceAll("\u00BA", "&ordm;");
        // right-pointing double angle quotation mark �
        valueSt = valueSt.replaceAll("\u00BB", "&raquo;");
        // vulgar fraction one quarter �
        valueSt = valueSt.replaceAll("\u00BC", "&frac14;");
        // vulgar fraction one half �
        valueSt = valueSt.replaceAll("\u00BD", "&frac12;");
        // vulgar fraction three quarters �
        valueSt = valueSt.replaceAll("\u00BE", "&frac34;");
        // inverted question mark �
        valueSt = valueSt.replaceAll("\u00BF", "&iquest;");
        // latin capital letter A with grave �
        valueSt = valueSt.replaceAll("\u00C0", "&Agrave;");
        // latin capital letter A with acute �
        valueSt = valueSt.replaceAll("\u00C1", "&Aacute;");
        // latin capital letter A with circumflex �
        valueSt = valueSt.replaceAll("\u00C2", "&Acirc;");
        // latin capital letter A with tilde �
        valueSt = valueSt.replaceAll("\u00C3", "&Atilde;");
        // latin capital letter A with diaeresis �
        valueSt = valueSt.replaceAll("\u00C4", "&Auml;");
        // latin capital letter A with ring above �
        valueSt = valueSt.replaceAll("\u00C5", "&Aring;");
        // latin capital letter AE �
        valueSt = valueSt.replaceAll("\u00C6", "&AElig;");
        // latin capital letter C with cedilla �
        valueSt = valueSt.replaceAll("\u00C7", "&Ccedil;");
        // latin capital letter E with grave �
        valueSt = valueSt.replaceAll("\u00C8", "&Egrave;");
        // latin capital letter E with acute �
        valueSt = valueSt.replaceAll("\u00C9", "&Eacute;");
        // latin capital letter E with circumflex �
        valueSt = valueSt.replaceAll("\u00CA", "&Ecirc;");
        // latin capital letter E with diaeresis �
        valueSt = valueSt.replaceAll("\u00CB", "&Euml;");
        // latin capital letter I with grave �
        valueSt = valueSt.replaceAll("\u00CC", "&Igrave;");
        // latin capital letter I with acute �
        valueSt = valueSt.replaceAll("\u00CD", "&Iacute;");
        // latin capital letter I with circumflex �
        valueSt = valueSt.replaceAll("\u00CE", "&Icirc;");
        // latin capital letter I with diaeresis �
        valueSt = valueSt.replaceAll("\u00CF", "&Iuml;");
        // latin capital letter ETH �
        valueSt = valueSt.replaceAll("\u00D0", "&ETH;");
        // latin capital letter N with tilde �
        valueSt = valueSt.replaceAll("\u00D1", "&Ntilde;");
        // latin capital letter O with grave �
        valueSt = valueSt.replaceAll("\u00D2", "&Ograve;");
        // latin capital letter O with acute �
        valueSt = valueSt.replaceAll("\u00D3", "&Oacute;");
        // latin capital letter O with circumflex �
        valueSt = valueSt.replaceAll("\u00D4", "&Ocirc;");
        // latin capital letter O with tilde �
        valueSt = valueSt.replaceAll("\u00D5", "&Otilde;");
        // latin capital letter O with diaeresis �
        valueSt = valueSt.replaceAll("\u00D6", "&Ouml;");
        // multiplication sign �
        valueSt = valueSt.replaceAll("\u00D7", "&times;");
        // latin capital letter O with stroke �
        valueSt = valueSt.replaceAll("\u00D8", "&Oslash;");
        // latin capital letter U with grave �
        valueSt = valueSt.replaceAll("\u00D9", "&Ugrave;");
        // latin capital letter U with acute �
        valueSt = valueSt.replaceAll("\u00DA", "&Uacute;");
        // latin capital letter U with circumflex �
        valueSt = valueSt.replaceAll("\u00DB", "&Ucirc;");
        // latin capital letter U with diaeresis �
        valueSt = valueSt.replaceAll("\u00DC", "&Uuml;");
        // latin capital letter Y with acute �
        valueSt = valueSt.replaceAll("\u00DD", "&Yacute;");
        // latin capital letter THORN �
        valueSt = valueSt.replaceAll("\u00DE", "&THORN;");
        // latin small letter sharp s �
        valueSt = valueSt.replaceAll("\u00DF", "&szlig;");
        // latin small letter a with grave �
        valueSt = valueSt.replaceAll("\u00E0", "&agrave;");
        // latin small letter a with acute �
        valueSt = valueSt.replaceAll("\u00E1", "&aacute;");
        // latin small letter a with circumflex �
        valueSt = valueSt.replaceAll("\u00E2", "&acirc;");
        // latin small letter a with tilde �
        valueSt = valueSt.replaceAll("\u00E3", "&atilde;");
        // latin small letter a with diaeresis �
        valueSt = valueSt.replaceAll("\u00E4", "&auml;");
        // latin small letter a with ring above �
        valueSt = valueSt.replaceAll("\u00E5", "&aring;");
        // latin small letter ae �
        valueSt = valueSt.replaceAll("\u00E6", "&aelig;");
        // latin small letter c with cedilla �
        valueSt = valueSt.replaceAll("\u00E7", "&ccedil;");
        // latin small letter e with grave �
        valueSt = valueSt.replaceAll("\u00E8", "&egrave;");
        // latin small letter e with acute �
        valueSt = valueSt.replaceAll("\u00E9", "&eacute;");
        // latin small letter e with circumflex �
        valueSt = valueSt.replaceAll("\u00EA", "&ecirc;");
        // latin small letter e with diaeresis �
        valueSt = valueSt.replaceAll("\u00EB", "&euml;");
        // latin small letter i with grave �
        valueSt = valueSt.replaceAll("\u00EC", "&igrave;");
        // latin small letter i with acute �
        valueSt = valueSt.replaceAll("\u00ED", "&iacute;");
        // latin small letter i with circumflex �
        valueSt = valueSt.replaceAll("\u00EE", "&icirc;");
        // latin small letter i with diaeresis �
        valueSt = valueSt.replaceAll("\u00EF", "&iuml;");
        // latin small letter eth �
        valueSt = valueSt.replaceAll("\u00F0", "&eth;");
        // latin small letter n with tilde �
        valueSt = valueSt.replaceAll("\u00F1", "&ntilde;");
        // latin small letter o with grave �
        valueSt = valueSt.replaceAll("\u00F2", "&ograve;");
        // latin small letter o with acute �
        valueSt = valueSt.replaceAll("\u00F3", "&oacute;");
        // latin small letter o with circumflex �
        valueSt = valueSt.replaceAll("\u00F4", "&ocirc;");
        // latin small letter o with tilde �
        valueSt = valueSt.replaceAll("\u00F5", "&otilde;");
        // latin small letter o with diaeresis �
        valueSt = valueSt.replaceAll("\u00F6", "&ouml;");
        // division sign �
        valueSt = valueSt.replaceAll("\u00F7", "&divide;");
        // latin small letter o with stroke, �
        valueSt = valueSt.replaceAll("\u00F8", "&oslash;");
        // latin small letter u with grave �
        valueSt = valueSt.replaceAll("\u00F9", "&ugrave;");
        // latin small letter u with acute �
        valueSt = valueSt.replaceAll("\u00FA", "&uacute;");
        // latin small letter u with circumflex �
        valueSt = valueSt.replaceAll("\u00FB", "&ucirc;");
        // latin small letter u with diaeresis �
        valueSt = valueSt.replaceAll("\u00FC", "&uuml;");
        // latin small letter y with acute �
        valueSt = valueSt.replaceAll("\u00FD", "&yacute;");
        // latin small letter thorn �
        valueSt = valueSt.replaceAll("\u00FE", "&thorn;");
        // latin small letter y with diaeresis �
        valueSt = valueSt.replaceAll("\u00FF", "&yuml;");

        // non standards but supported by IE and Mozilla

        // non-standard, use &sbquo; �
        valueSt = valueSt.replaceAll("\u201A", "&sbquo;");
        // non-standard, use &fnof; �
        valueSt = valueSt.replaceAll("\u0192", "&fnof;");
        // non-standard, use &bdquo; �
        valueSt = valueSt.replaceAll("\u201E", "&dbquo;");
        // non-standard, use &hellip; �
        valueSt = valueSt.replaceAll("\u2026", "&hellip;");
        // non-standard, use &dagger; �
        valueSt = valueSt.replaceAll("\u2020", "&dagger;");
        // non-standard, use &Dagger �
        valueSt = valueSt.replaceAll("\u2021", "&Dagger;");
        // non-standard, use &Scaron; �
        valueSt = valueSt.replaceAll("\u0160", "&Scaron;");
        // non-standard, use &OElig; �
        valueSt = valueSt.replaceAll("\u0152", "&OElig;");
        // unused ?
        valueSt = valueSt.replaceAll("\u008D", "");
        // non-standard �
        valueSt = valueSt.replaceAll("\u008E", "");
        // unused ?
        valueSt = valueSt.replaceAll("\u008F", "");
        // unused ?
        valueSt = valueSt.replaceAll("\u0090", "");
        // non-standard, use &lsquo; �
        valueSt = valueSt.replaceAll("\u2018", "&lsquo;");
        // non-standard, use &rsquo; �
        valueSt = valueSt.replaceAll("\u2019", "&rsquo;");
        // non-standard, use &ldquo; �
        valueSt = valueSt.replaceAll("\u201C", "&ldquo;");
        // non-standard, use &rdquo; �
        valueSt = valueSt.replaceAll("\u201D", "&rdquo;");
        // non-standard, use &bull; �
        valueSt = valueSt.replaceAll("\u2022", "&bull;");
        // non-standard, use &ndash; �
        valueSt = valueSt.replaceAll("\u2013", "&ndash;");
        // non-standard, use &mdash; �
        valueSt = valueSt.replaceAll("\u2014", "&mdash;");
        // non-standard, use &tilde; �
        valueSt = valueSt.replaceAll("\u007E", "&tilde;");
        // non-standard, use &trade; �
        valueSt = valueSt.replaceAll("\u2122", "&trade;");
        // non-standard, use &scaron; �
        valueSt = valueSt.replaceAll("\u0161", "&scaron;");
        // non-standard, use &oelig; �
        valueSt = valueSt.replaceAll("\u0153", "&oelig;");
        // unused ?
        valueSt = valueSt.replaceAll("\u009D", "");
        // unused �
        valueSt = valueSt.replaceAll("\u009E", "");
        // non-standard, use &Yuml; �
        valueSt = valueSt.replaceAll("\u0178", "&Yuml;");

        return valueSt;    	
    }
    
    public static String escapeHTMLSpecialChars(String valueSt) {
    	if(valueSt ==null){
    		return null;
    	}
        // Standard chars
    	valueSt = valueSt.replaceAll("&amp;", "_DOTCMS_AMP_");
		valueSt = valueSt.replaceAll("&", "&amp;");
		valueSt = valueSt.replaceAll("_DOTCMS_AMP_", "&amp;");
		
		valueSt = valueSt.replaceAll("&lt;", "_DOTCMS_LT_");
		valueSt = valueSt.replaceAll("<", "&lt;");
		valueSt = valueSt.replaceAll("_DOTCMS_LT_","&lt;");
		
		valueSt = valueSt.replaceAll("&gt;", "_DOTCMS_GT_");
		valueSt = valueSt.replaceAll(">", "&gt;");
		valueSt = valueSt.replaceAll("_DOTCMS_GT_","&gt;");
		
        // inverted exclamation mark �
        valueSt = escapeUnicodeCharsForHTML(valueSt);

        return valueSt;
    }

    public static String fixBreaks(String fixme) {
        if (isSet(fixme)) {
            fixme = replace(fixme, "${return}", "\n");
            return fixme;
        }
        return "";
    }

    /**
     * Escape quotation marks so they work in javascript fields
     */
    public static String escapeQuotes(String fixme) {
        String doubleQuote = "\"";

        String singleQuote = "'";
        String escapedSingleQuote = "\\'";

        if (fixme != null) {
            fixme = fixme.trim();

            try {
                // first replace double quotes with single quotes
                fixme = fixme.replaceAll(doubleQuote, doubleQuote);

                // now escape all the single quotes
                fixme = fixme.replaceAll(singleQuote, escapedSingleQuote);

                return fixme;
            } catch (Exception e) {
                Logger.error(UtilMethods.class, "Could not parse string [" + fixme + "] for escaping quotes: " + e.toString(), e);
                return "";
            }
        } else {
            return "";
        }
    }

    /**
     * Escape quotation marks so they work in javascript fields
     */
    public static String sqlify(String fixme) {
        String singleQuote = "'";
        String escapedSingleQuote = "''";

        if (fixme != null) {
            fixme = fixme.trim();

            try {
                // now escape all the single quotes
                fixme = fixme.replaceAll(singleQuote, escapedSingleQuote);

                return fixme;
            } catch (Exception e) {
                Logger.error(UtilMethods.class, "Could not parse string [" + fixme + "] for escaping single quotes: " + e.toString(), e);
                return "";
            }
        } else {
            return "";
        }
    }

    public static boolean inString(String haystack, String needle) {
        if ((haystack == null) || (needle == null)) {
            return false;
        }

        return haystack.startsWith(needle);
    }

    public static String dayify(String x) {
        StringBuffer sb = new StringBuffer();

        if (daysOfWeek == null) {
            daysOfWeek = new HashMap<String, String>();
            daysOfWeek.put("M", "Monday");
            daysOfWeek.put("T", "Tuesday");
            daysOfWeek.put("W", "Wednesday");
            daysOfWeek.put("R", "Thursday");
            daysOfWeek.put("F", "Friday");
            daysOfWeek.put("S", "Saturday");
            daysOfWeek.put("&", "Sunday");
        }

        if (x.indexOf("ARR") > -1) {
            return "to be arranged";
        }

        char[] chrs = x.toCharArray();

        for (int i = 0; i < chrs.length; i++) {
            if (daysOfWeek.get(String.valueOf(chrs[i])) != null) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(daysOfWeek.get(String.valueOf(chrs[i])));
            }
        }

        return sb.toString();
    }

    static final String[] MONTH_NAME = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    public static String getShortMonthName(int month) {
        try {
            return MONTH_NAME[month];
        } catch (Exception e) {
            return "";
        }
    }

    public static String getShortMonthName(String month) {
        try {
            return MONTH_NAME[Integer.parseInt(month)];
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isUrlLive(String url, Host host) throws Exception {
        return isUrlLive(url, host.getIdentifier());
    }

    public static boolean isUrlLive(String url, String hostId) throws Exception {
        return (LiveCache.getPathFromCache(url, hostId) != null);
    }

    public static boolean isUrlPreview(String url, Host host) throws Exception {
        return isUrlPreview(url, host.getIdentifier());
    }

    public static boolean isUrlPreview(String url, String hostId) throws Exception {
        return (WorkingCache.getPathFromCache(url, hostId) != null);
    }

    public static String stripUnicode(String x) {
        return (x == null) ? x : x.replaceAll("([^\000-\177��������������])", " ");

    }

    public static String obfuscateCreditCard(String ccnum) {
        return obfuscateString(ccnum,4);
    }
    
    public static String obfuscateString(String toOfuscate,int size) {
        if (toOfuscate != null && toOfuscate.length() > size) 
        {
        	int ofuscateSize = toOfuscate.length() - size;
        	toOfuscate = toOfuscate.substring(ofuscateSize,toOfuscate.length());
        	StringBuffer sb = new StringBuffer();
        	for(int i = 0; i < ofuscateSize; i++)
        	{
        		sb.append("*");
        	}
        	toOfuscate = sb.toString() + toOfuscate;
            return toOfuscate;
        }
        return "";
    }

    /**
     * Special split function, to split csv files exported from access
     * 
     * @param reader
     *            The file reader
     * @param delim
     *            The columns delimiter
     * @param textQualifier
     *            The text qualifier string
     * @return A list of list with the list of lines splitted on columns
     * @throws IOException
     */
    public static String[] specialSplit(String text, String delim, String textQualifier) throws IOException {

        ArrayList<String> tokens = new ArrayList<String>();
        StringTokenizer tok = new StringTokenizer(text, delim, true);
        boolean delimFound = false;
        String lastToken = "";
        try {
            while (true) {
                String nt = tok.nextToken(delim);
                lastToken = nt;
                if (nt.equals(textQualifier)) {
                    delimFound = false;
                    continue;
                }
                if (nt.equals(",") && delimFound) {
                    tokens.add("");
                    continue;
                } else if (nt.equals(",")) {
                    delimFound = true;
                    continue;
                } else {
                    delimFound = false;
                }
                if (nt.trim().startsWith(textQualifier) && (!nt.trim().endsWith(textQualifier))) {
                    boolean endFound = false;
                    while (!endFound) {
                        try {
                            nt += tok.nextToken(textQualifier);
                            if (nt.trim().endsWith("\""))
                                endFound = true;
                        } catch (NoSuchElementException e) {

                        }
                    }
                }
                nt = nt.trim();
                if (nt.startsWith(textQualifier))
                    nt = nt.substring(textQualifier.length(), nt.length());
                if (nt.endsWith(textQualifier))
                    nt = nt.substring(0, nt.length() - textQualifier.length());
                nt = nt.trim();
                tokens.add(nt);
            }
        } catch (NoSuchElementException e) {
            if (lastToken.equals(delim))
                tokens.add("");
        }
        String[] values = (String[]) tokens.toArray(new String[0]);

        return values;
    }

    /**
     * Extracts the character set that has been configured by the 
     * admin, for this installation of dotCMS
     * e.g. "text/html;charset=UTF-8" => "UTF-8" 
     * @return the configured character set
     * 
     * @author Dimitris Zavaliadis
     * @version 1.0
     */
    public static String getCharsetConfiguration() {
    	// CHARSET key in properties file specifies both content type and charset
    	String charsetWithContentType = Config.getStringProperty("CHARSET");	
    	
    	if (isSet(charsetWithContentType)) {
    		// We are only interested in charset
    		if(charsetWithContentType.indexOf("charset") > 0) {
    			return charsetWithContentType.substring(
    	    			charsetWithContentType.indexOf("=") + 1).trim();
    		}
        }
    	// Default to UTF-8
    	return UTILMETHODS_DEFAULT_ENCODING;
    }

    public static final String dateToLongPrettyHTMLDate(java.util.Date x) {
        if (x == null) {
            return "";
        }
        return DATE_TO_LONG_PRETTY_HTML_DATE.format(x);
    }

    public static final boolean hasValue(String selectedValues, String value) {
        if (UtilMethods.isSet(selectedValues)) {
            String[] values = selectedValues.split(",");
            for (String val : values) {
                if (val.trim().equals(value))
                    return true;
            }
        }
        return false;
    }

    public static final String convertToNumbers(String st) {
        String result = "";
        for (int i = 0; i < st.length(); i++) {
            if (Character.isDigit(st.charAt(i)))
                result += st.charAt(i);
        }
        return result;
    }

    public static final String convertToFolderName(String st) {
        return st.replaceAll("\\/", "_");
    }

    public static String encodeURL(String url) {
        try {
            return URLEncoder.encode(url, getCharsetConfiguration());
        } catch (Exception e) {
        }
        return "";
    }

    public static String decodeURL(String url) {
        try {
            return URLDecoder.decode(url, getCharsetConfiguration());
        } catch (Exception e) {
        }
        return "";
    }

    /**
     * It is mainly suitable for converting i18n URIs which will be used in html anchors/simple actions.
     * <p>
     * <em><strong>Note:</strong> It only keeps unchanged the following characters: "/" (Solidus) and ":" (Colon). Also, it
     * does not work exactly as java.net.URLEncoder.encode(java.lang.String uri, java.lang.String enc) nor Javascript's encodeURIComponent.</em>
     *
     * @param uri
     * @return A new percent-encoded string.
     * @since 1.6
     * @author Carlos Rivas
     * @see URLEncoder#encode(java.lang.String, java.lang.String)
     */
    public static String encodeURIComponent(String uri) {
    	String result = uri; 
        try {
        	result = URLEncoder.encode(result, getCharsetConfiguration());
        	result = result.replaceAll("%2F", "/").replaceAll("%3A", ":");
            return result;
        } catch (Exception e) {
        	Logger.error(UtilMethods.class, "encodeURIComponent failed for URI: " + uri);
        }
        return uri;
    }

    public static boolean revomeDir(String path) {
        File dir = new File(path);
        if (!dir.isDirectory()) {
            return false;
        }
        File[] children = dir.listFiles();
        for (File child : children) {
            boolean ok = true;
            if (child.isDirectory())
                ok = revomeDir(child.getAbsolutePath());
            if (ok)
                ok = child.delete();
            if (!ok)
                return ok;
        }

        return dir.delete();
    }

    // Liferay users utility methods
    public static String getUserFullName(String userId) {
    	User usr = null;
		try {
			usr = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e1) {
			Logger.debug(UtilMethods.class,e1.getMessage(), e1);
		}
        if (usr == null)
            return userId;
        if (!isSet(usr.getFirstName()) && !isSet(usr.getLastName()))
        	if(usr.getEmailAddress() != null)
        		return usr.getEmailAddress();
        	else
        		return usr.getUserId();
        return usr.getFullName();
    }

    public static String getUserEmailAddress(String userId) {
    	User usr = null;
		try {
			usr = APILocator.getUserAPI().loadUserById(userId,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e1) {
			Logger.error(UtilMethods.class,e1.getMessage(), e1);
		}
        if (usr == null)
            return "none";
        return usr.getEmailAddress();
    }

    public static String getValidDirectoryName(String phrase) {
        if (phrase == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        char[] chars = phrase.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char x = chars[i];

            if ((x > 64 && x < 91) || (x > 96 && x < 123) || (x > 47 && x < 58)) {
                sb.append(x);
            }
            if (x == 32) {
                sb.append('_');
            }

        }
        return sb.toString().toLowerCase();
    }

    public static boolean isInt(String intString) {
        try {
            Integer.parseInt(intString);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static Date addDate(Date date, int amount, int field) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        gc.add(field, amount);
        return gc.getTime();
    }

    public static Date addDays(Date date, int amount) {
        int field = GregorianCalendar.DAY_OF_MONTH;
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        gc.add(field, amount);
        return gc.getTime();
    }

    public static String elapsedTimeToString(long milliseconds) {

        String time = "";

        long seconds = milliseconds / 1000;
        if (seconds > 0) {
            String sec;
            sec = "" + (seconds % 60) + "s";
            String min;
            if (seconds > 60) {
                min = "" + (seconds / 60 % 60) + "m";
            } else {
                min = "";
            }
            String hours;
            if (seconds / 60 > 60) {
                hours = "" + (seconds / 60 / 60) + "h";
            } else {
                hours = "";
            }

            time = "" + hours + " " + min + " " + sec;

        } else {
            time = "0 s";
        }
        return time;
    }

    /**
     * Special split function, to split csv files exported from access, excel,
     * ...
     * 
     * @param reader
     * @param delim
     * @param textQualifier
     * @return
     * @throws IOException
     */
    public static List<String[]> specialSplit(Reader reader, char delim, char textQualifier) throws IOException {
		List<String[]> records = new ArrayList<String[]>();
		CsvReader csvReader = new CsvReader (reader);
		csvReader.setDelimiter(delim);
		csvReader.setTextQualifier(textQualifier);
		csvReader.readHeaders();
		records.add(csvReader.getHeaders());
		while(csvReader.readRecord()) {
			records.add(csvReader.getValues());
		}
		return records;
    }

    public static Folder getParentFolder(String childPath, Host host) {
    	Folder folder = new Folder();
    	try {
			Folder childFolder = APILocator.getFolderAPI().findFolderByPath(childPath, host,APILocator.getUserAPI().getSystemUser(),false);
			folder = APILocator.getFolderAPI().findParentFolder(childFolder,APILocator.getUserAPI().getSystemUser(),false);
		} catch (Exception e) {
			Logger.error(UtilMethods.class,e.getMessage(), e);
		}
		return folder;
    }

    /**
     * Compares if two dates (yyyy/MM/dd)) have equals values
     * 
     * @param date1
     *            Actual date
     * @param date2
     *            Date to be Compared
     * @return int 0 if but dates are equals, less than 0 if date1 is before
     *         date2, and more than 0 if date1 id after date2
     */
    public static int compareDates(Date date1, Date date2) {

        if (date1 == null && date2 == null)
            return 0;

        if (date1 == null)
            return -1;
        if (date2 == null)
            return 1;

        GregorianCalendar cal1 = new GregorianCalendar();
        cal1.setTime(date1);
        cal1.set(GregorianCalendar.HOUR_OF_DAY, 0);
        cal1.set(GregorianCalendar.MINUTE, 0);
        cal1.set(GregorianCalendar.SECOND, 0);
        cal1.set(GregorianCalendar.MILLISECOND, 0);

        GregorianCalendar cal2 = new GregorianCalendar();
        cal2.setTime(date2);
        cal2.set(GregorianCalendar.HOUR_OF_DAY, 0);
        cal2.set(GregorianCalendar.MINUTE, 0);
        cal2.set(GregorianCalendar.SECOND, 0);
        cal2.set(GregorianCalendar.MILLISECOND, 0);
        Logger.debug(UtilMethods.class, "cal1:" + cal1.getTime().toString() + " - cal2:" + cal2.getTime().toString());
        return cal1.compareTo(cal2);
    }
    
    
    /**
     * This method takes two strings as input and tries to
     * parse and format them to the correct format needed
     * by compareDates(Date,Date), this way we are not bound 
     * to use Date data types only. 
     * @param stringDate1 
     *        String representation of the actual date
     * @param stringDate2 
     *        String representation of the date to be compared
     * @return see compareDates(Date, Date)
     */
    
    public static int compareDates(String stringDate1, String stringDate2){
    	
        SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date1 = null;
        Date date2 = null;
        try{
              date1 = simpleFormat.parse(stringDate1);
              date2 = simpleFormat.parse(stringDate2);
        }catch(ParseException e){
        	
        }      
    	
    	return(compareDates(date1,date2));
   	
    }

    /**
     * Get the modelu of x % y
     * 
     * @param x
     *            dividend
     * @param y
     *            divisor
     * @return module
     */
    public static int mod(Integer x, Integer y) {

        return x % y;
    }

    public static final String dateToPrettyHTMLDate2(java.util.Date x) {
        if (x == null) {
            return "";
        }

        return DATE_TO_PRETTY_HTML_DATE_2.format(x);
    }

    public static final String dateToLongHTMLDateRange(java.util.Date x, java.util.Date y) {

        String i = dateToPrettyHTMLDate2(x);
        String j = dateToPrettyHTMLDate2(y);

        if (i.equals(j)) {
            return i;
        } else {
            return i + " - " + j;
        }
    }

    public static boolean isLong(String longString) {
        try {
            Long.parseLong(longString);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static String toPriceFormat(double price) {
        int decimals = 2;
        if (Math.abs(price) == 0) {
            price = Math.abs(price);
        }
        return toXNumberFormat(price, decimals);
    }

    public static String toPriceFormat(float price) {
        if (Math.abs(price) == 0) {
            price = Math.abs(price);
        }
        return toPriceFormat((double) price);
    }

    public static String toXNumberFormat(double number, int decimals) {
        try {
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMinimumFractionDigits(decimals);
            nf.setMaximumFractionDigits(decimals);
            String numberS = nf.format(number);
            return numberS;
        } catch (Exception ex) {
            Logger.debug(UtilMethods.class, ex.toString());
            return "NAN";
        }
    }

    public static String getShippingTypeName(int shippingType) {

        String typeName = "";
        switch (shippingType) {
        case 0:
            typeName = "Ground";
            break;
        case 1:
            typeName = "Second Day";
            break;
        case 2:
            typeName = "Next Day";
            break;

        }

        return typeName;
    }

    public static String getPaymentTypeName(int paymentType) {

        String typeName = "";
        switch (paymentType) {
        case 1:
            typeName = "Credit Card";
            break;
        case 2:
            typeName = "Check";
            break;
        case 3:
            typeName = "Purchase Order";
            break;
        }
        return typeName;
    }
    
    public static String getUserEmail(User user) {
        String email = "";
        if (user != null) {
            email = user.getEmailAddress();
        }
        return email;
    }

    public static String toPercentageFormat(double weight) {
        int decimals = 3;
        return toXNumberFormat(weight, decimals);
    }

    public static String toPercentageFormat(float weight) {
        return toWeightFormat((double) weight);
    }

    public static String toWeightFormat(double weight) {
        int decimals = 3;
        return toXNumberFormat(weight, decimals);
    }

    public static String toWeightFormat(float weight) {
        return toWeightFormat((double) weight);
    }

    public static String getActualYear() {
        GregorianCalendar calendar = new GregorianCalendar();
        return String.valueOf(calendar.get(GregorianCalendar.YEAR));
    }

    public static String getEventDateRange(Date date1, Date date2) {
        if (date1 == null)
            return "";
        if (date2 == null)
            date2 = date1;

        String dateRet = "";
        GregorianCalendar cal = new GregorianCalendar();
        GregorianCalendar cal2 = new GregorianCalendar();
        cal.setTime(date1);
        cal2.setTime(date2);
        if (cal.get(GregorianCalendar.MONTH) == cal2.get(GregorianCalendar.MONTH)
                && cal.get(GregorianCalendar.YEAR) == cal2.get(GregorianCalendar.YEAR)) {
            dateRet += new SimpleDateFormat("MMMM").format(date1) + " ";
            if (cal.get(GregorianCalendar.DATE) == cal2.get(GregorianCalendar.DATE)) {
                dateRet += cal.get(GregorianCalendar.DATE) + " ";
            } else {
                dateRet += cal.get(GregorianCalendar.DATE) + " - " + cal2.get(GregorianCalendar.DATE) + " ";
            }
            dateRet += cal.get(GregorianCalendar.YEAR);
        } else {
            if (cal.get(GregorianCalendar.YEAR) == cal2.get(GregorianCalendar.YEAR)) {
                dateRet += new SimpleDateFormat("MMMM").format(date1) + " ";
                dateRet += cal.get(GregorianCalendar.DATE) + " - ";
                dateRet += new SimpleDateFormat("MMMM").format(date2) + " ";
                dateRet += cal2.get(GregorianCalendar.DATE) + " ";
                dateRet += cal.get(GregorianCalendar.YEAR);
            } else {
                dateRet += new SimpleDateFormat("MMMM").format(date1) + " ";
                dateRet += cal.get(GregorianCalendar.DATE) + " ";
                dateRet += cal.get(GregorianCalendar.YEAR) + " - ";
                dateRet += new SimpleDateFormat("MMMM").format(date2) + " ";
                dateRet += cal2.get(GregorianCalendar.DATE) + " ";
                dateRet += cal2.get(GregorianCalendar.YEAR);
            }
        }
        return dateRet;
    }

    public static final String htmlDateToHTMLTime(java.util.Date x) {
        if (x == null) {
            return "";
        }

        return HTML_DATETIME_TO_DATE.format(x);
    }

    public static List<Object> randomList(List<Object> list) {
        return randomList(list, list.size());
    }

    public static List<Object> randomList(List<Object> list, int number) {
    	List<Object> randomList = new ArrayList<Object>();
    	
    	if(list.size() > 0) {
	    	int done = 0;
	    	int i = 0;
	    	
	    	// Randomize the list
	    	Collections.shuffle(list);
	    	
	    	// Build the random list with number elements
	    	while(done < number) {		    	
	    		// number can be > list.size() 
	    		if(i >= list.size()) {
	    			// in this case resuffle the list and start over
	    			Collections.shuffle(list);
	    			i = 0;
	    		}
	    		
	    		randomList.add(list.get(i));
	    		
	    		done++;
	    		i++;
	    	}
    	}
    	
        return randomList;
    }

    public String toString(Object obj) {
        return obj.toString();
    }

    public String toString(int num) {
        return Integer.toString(num);
    }

    public String toString(long num) {
        return Long.toString(num);
    }

    /**
     * This methods receives an object and builds a map based on the object
     * simple properties (integers, longs, strings, floats, doubles, dates,
     * chars, booleans) of the object
     * 
     * @param obj
     * @return
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(Object obj) {

        HashMap<String, Object> map = new HashMap<String, Object>();

        // Getting object properties
        PropertyDescriptor[] descs = PropertyUtils.getPropertyDescriptors(obj);
        try {
            for (PropertyDescriptor desc : descs) {
                Class propertyType = desc.getPropertyType();
                if (propertyType != null
                        && (propertyType.equals(Integer.class) || propertyType.equals(int.class) || propertyType.equals(Long.class)
                                || propertyType.equals(long.class) || propertyType.equals(String.class) || propertyType.equals(Float.class)
                                || propertyType.equals(float.class) || propertyType.equals(Double.class) || propertyType.equals(double.class)
                                || propertyType.equals(Character.class) || propertyType.equals(char.class) || propertyType.equals(Date.class)
                                || propertyType.equals(boolean.class) || propertyType.equals(Boolean.class))) {
                    try {
                        map.put(desc.getName(), PropertyUtils.getSimpleProperty(obj, desc.getName()));
                    } catch (Exception e) {
                        Logger.error(UtilMethods.class, "An error as ocurred trying to copy the properpy: " + desc.getName() + " from the object: " + obj, e);
                    }
                }
            }
        } catch (Exception e) {
            Logger.error(UtilMethods.class,"Unable to read object properties", e);
        }

        return map;
    }

    /**
     * This method create a list of element for the
     * getLuceneDocumentForContentlet to facilitate the search of multiple
     * elements the contentlet search
     * 
     * @param stringList
     * @return a list of elements for the getLuceneDocumentForContentlet
     */
    public static String listToString(String stringList) {
        StringBuffer result = new StringBuffer();
        StringTokenizer token = new StringTokenizer(stringList, ",");
        while (token.hasMoreElements()) {
            result.append(token.nextToken() + " ");
        }
        return result.toString();
    }

    /**
     * Generate a ramdom number between 0 and maxRanger number
     * 
     * @param maxRange
     * @return int
     */
    public static int getRandomNumber(int maxRange) {

        Random r = new Random();
        int randInt = Math.abs(r.nextInt()) % (maxRange + 1);
        return randInt;

    }

    /**
     * get the velocity template from the liveUrl, if the file is not publish,
     * automatically is published
     * 
     * @param liveUrl
     * @return Velocity Template
     * @throws DotDataException 
     * @throws DotSecurityException 
     */
    public static Template getVelocityTemplate(String liveUrl) throws WebAssetException, DotDataException, DotSecurityException {

        try {
            VelocityEngine ve = VelocityUtil.getEngine();
            Template template = ve.getTemplate(liveUrl);
            return template;
        } catch (Exception e) {
            /* Get the htmlpage a publish */
            String idStr = liveUrl.substring(liveUrl.indexOf("/") + 1, liveUrl.indexOf("."));
            //long idInode = Long.parseLong(idStr);
            //HTMLPage htmlPage = (HTMLPage) APILocator.getVersionableAPI().findLiveVersion(InodeFactory.getInode(idStr, Identifier.class), HTMLPage.class);
            HTMLPage htmlPage = (HTMLPage) APILocator.getVersionableAPI().findLiveVersion(APILocator.getIdentifierAPI().find(idStr), APILocator.getUserAPI().getSystemUser(),false);
            if(htmlPage != null && InodeUtils.isSet(htmlPage.getInode())){
            	PublishFactory.publishAsset(htmlPage, APILocator.getUserAPI().getSystemUser(), false);
            	return getVelocityTemplate(liveUrl);
            }
            
            
        }
        return null;
    }

    /**
     * Return is a date is equals or before the actual date
     * 
     * @param date
     * @return
     */
    public static Date getCurrentDate() {

        try {
            GregorianCalendar cal = new GregorianCalendar();
            return cal.getTime();

        } catch (Exception e) {

        }
        return null;
    }

    public static String truncFull(String x, int len) {
        len -= 3;
        if (x.length() >= len) {
            x = x.substring(0, len);
            x += "...";
        }
        return x;
    }

    public static String concat(String string1, String string2) {
        return string1 + string2;
    }

    public static String toCamelCase(String fieldName) {
        if (fieldName == null)
            return null;
        boolean upperCase = false;
        String newString = "";
        for (int i = 0; i < fieldName.length(); i++) {
            Character c = fieldName.charAt(i);
            if (upperCase) {
                c = Character.toUpperCase(c);
            } else {
                c = Character.toLowerCase(c);
            }
            if (c == ' ') {
                upperCase = true;
            } else {
                upperCase = false;
                newString += c;
            }
        }
        newString = newString.replaceAll("[^a-zA-Z0-9]+", "");
        return newString;
    }

    public static Date now() {
        return new Date();
    }

    public static String xmlEscape(String description) {
        return XMLUtils.xmlEscape(description);
    }

    /**
     * An optimized routine for concatenating String objects together.
     * 
     * @param objects
     *            variable arity list of Objects. Java 1.5 auto-boxing allows
     *            this method to accept primitive values to be concatenated
     *            together. Empty String will be used in place of null values.
     * @return String
     */
    public static String concatenate(Object... objects) {
        StringBuffer string = new StringBuffer(50);
        for (int i = 0; i < objects.length; i++) {
            string.append((objects[i] != null) ? objects[i] : "");
        }
        return string.toString();
    }

    /**
     * An optimized routine for concatenating String objects together.
     * 
     * @param objects
     *            variable arity list of Objects. Java 1.5 auto-boxing allows
     *            this method to accept primitive values to be concatenated
     *            together. The text "null" will be used in place of null
     *            values.
     * @return String
     */
    public static String concatenateWithNulls(Object... objects) {
        StringBuffer string = new StringBuffer(50);
        for (int i = 0; i < objects.length; i++) {
            string.append((objects[i] != null) ? objects[i] : "null");
        }
        return string.toString();
    }

    /**
     * @param integer
     *            Integer to be analyzed
     * @param fallThroughValue
     *            default value in case of NullPointerException or
     *            NumberFormatException
     * @return the int value of an Integer. If the Integer cannot be parsed,
     *         fallThroughValue is returned.
     */
    public static int getInt(Integer integer, int fallThroughValue) {
        try {
            return integer.intValue();
        } catch (Exception ex) {
            return fallThroughValue;
        }
    }

    /**
     * 
     * @param <E>
     * @param arli
     * @return
     */
    public static <E> String toCommaDelimitedString(List<E> arli) {
        StringBuffer result = new StringBuffer();
        for (E e : arli) {
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(e.toString());
        }
        return result.toString();
    }

    public static final int COMPARE_LESS_THAN = -1;
    public static final int COMPARE_EQUAL_TO = 0;
    public static final int COMPARE_GREATER_THAN = 1;

    /**
     * returns the simple class name of the object
     * (&quot;edu.bju.asq.action.LoginAction&quot converts to
     * &quot;LoginAction&quot;) null input object returns UnknownClass
     */
    public static String getClassName(Object source) {
        String className = "UnknownClass";
        if (source != null) {
            if (source instanceof Class) {
                className = ((Class) source).getName();
            } else {
                className = source.getClass().getName();
            }
            if (className != null && className.indexOf('.') >= 0) {
                className = className.substring(className.lastIndexOf('.') + 1);
            }
        }
        return className;
    }

    public static String makeXmlSafe(String unsafeString) {
        return makeHtmlSafe(unsafeString, true);
    }

    public static String makeHtmlSafe(String unsafeString) {
        return makeHtmlSafe(unsafeString, true);
    }

    /**
     * new lines stay as single character '\n's -- they are not translated into
     * <br>
     * s
     */
    public static String makeHtmlSafeDontConvertNewLines(String unsafeString) {
        return makeHtmlSafe(unsafeString, false);
    }

    private static String makeHtmlSafe(String unsafeString, boolean convertNewLines) {
        if (unsafeString == null) {
            return "";
        }

        StringBuffer safeText = null;
        char[] text = unsafeString.toCharArray();
        safeText = new StringBuffer(unsafeString.length() + 50);

        // StringBuffer attributedInput = new StringBuffer();

        for (int i = 0; i < text.length; i++) {
            // attributedInput.append(text[i]);
            // if ((text[i] < 'A' || text[i] > 'Z') &&
            // (text[i] < 'a' || text[i] > 'z') &&
            // text[i] != ' ') {
            // attributedInput.append('[');
            // attributedInput.append(Integer.toHexString(text[i]));
            // attributedInput.append(']');
            // }

            switch (text[i]) {
            case '<':
                safeText.append("&lt;");
                break;
            case '>':
                safeText.append("&gt;");
                break;
            case '&':
                safeText.append("&amp;");
                break;
            case '"':
                safeText.append("&quot;");
                break;
            case '\'':
                safeText.append("&#39;");
                break;
            case 0x1a:
                break;
            case '\n':
                safeText.append(convertNewLines ? "<br>" : "\n");
                break;
            case '\r':
                if (!convertNewLines) {
                    safeText.append(text[i]);
                }
                break;
            default:
                if (text[i] < 128) {
                    safeText.append(text[i]);
                }
            }
        }

        // print("made html safe \"" + attributedInput + "\" -> \"" + safeText +
        // "\"");

        return safeText.toString();
    }

    /**
     * removes Microsoft Word's unusual characters and replaces them with their
     * simple equivalents
     */
    public static String simplifyExtendedAsciiCharacters(String unsafeString) {
        if (unsafeString == null) {
            return "";
        }

        boolean someCharactersChanged = false;

        StringBuffer safeText = null;
        char[] text = unsafeString.toCharArray();
        safeText = new StringBuffer(unsafeString.length() + 50);

        StringBuffer attributedInput = new StringBuffer();

        for (int i = 0; i < text.length; i++) {
            attributedInput.append(text[i]);
            if ((text[i] < ' ' || text[i] > '~')) {
                attributedInput.append("[x");
                attributedInput.append(Integer.toHexString(text[i]));
                attributedInput.append(']');
            }

            // if the character is not a "substitute" character, add it to the
            // string
            if (text[i] != 0x1a) {
                if (text[i] < 128) {
                    safeText.append(text[i]);
                } else {
                    someCharactersChanged = true;
                    switch (text[i]) {
                    // single quote "single quote ?[91]hello?[92]"
                    case 0x91:
                        safeText.append('\'');
                        break;
                    case 0x92:
                        safeText.append('\'');
                        break;
                    // double quote "quote ?[93]hello?[94]"
                    case 0x93:
                        safeText.append('\"');
                        break;
                    case 0x94:
                        safeText.append('\"');
                        break;
                    // short dash "short ?[96] dash"
                    case 0x96:
                        safeText.append('-');
                        break;
                    // long dash "long?[97]dash"
                    case 0x97:
                        safeText.append('-');
                        break;
                    // elipsis "hello?[85]goodbye"
                    case 0x85:
                        safeText.append("...");
                        break;
                    default:
                        safeText.append('?');
                        // log(
                    }
                }
            }
        }

        if (someCharactersChanged) {
            String attributedInputString = attributedInput.toString();
            attributedInputString = attributedInputString.replace('\r', 'r');
            attributedInputString = attributedInputString.replace('\n', 'n');
            String safeTextString = safeText.toString();
            safeTextString = safeTextString.replace('\r', 'r');
            safeTextString = safeTextString.replace('\n', 'n');

        }
        return (someCharactersChanged) ? safeText.toString() : unsafeString;
    }

    public static String makePdfSafe(String unsafeString) {
        if (unsafeString == null) {
            return "";
        }

        // refer to
        // http://msdn.microsoft.com/library/default.asp?url=/library/en-us/xmlsdk/htm/xsl_whitespace_4o1f.asp
        // for more information if needed
        StringBuffer safeText = null;
        char[] text = unsafeString.toCharArray();
        safeText = new StringBuffer(unsafeString.length() + 50);

        for (int i = 0; i < text.length; i++) {
            switch (text[i]) {
            case '<':
                safeText.append("&lt;");
                break;
            case '>':
                safeText.append("&gt;");
                break;
            case '&':
                safeText.append("&amp;");
                break;
            case '"':
                safeText.append("&quot;");
                break;
            case '\n':
                safeText.append("&#10;");
                break;
            case '\r':
                break;
            case '\t':
                safeText.append("&#9;");
                break;
            default:
                if (text[i] >= 0x20) {
                    safeText.append(text[i]);
                }
            }
        }
        return safeText.toString();
    }

    // used when sending a database string to a javascript string
    public static String makeJavaSafe(String unsafeString) {
        if (unsafeString == null) {
            return "";
        }

        StringBuffer safeText = null;
        char[] text = unsafeString.toCharArray();
        safeText = new StringBuffer(unsafeString.length() + 50);

        for (int i = 0; i < text.length; i++) {
            switch (text[i]) {
            case '\'':
                safeText.append("\\\'");
                break;
            case '\\':
                safeText.append("\\\\");
                break;
            case '"':
                safeText.append("\\\"");
                break;
            case '\n':
                safeText.append("\\n");
                break;
            case '\r':
                break;
            default:
                safeText.append(text[i]);
            }
        }

        return safeText.toString();
    }

    // used when sending a database string to a javascript string
    public static String makeUnixSafe(String unsafeString) {
        if (unsafeString == null) {
            return "";
        }

        StringBuffer safeText = null;
        char[] text = unsafeString.toCharArray();
        safeText = new StringBuffer(unsafeString.length() + 50);

        for (int i = 0; i < text.length; i++) {
            if (text[i] != '\r') {
                safeText.append(text[i]);
            }
        }

        return safeText.toString();
    }

    public static String removeCharacters(String unsafeString, String charactersToRemove) {
        if (unsafeString != null) {
            StringTokenizer safeTokens = new StringTokenizer(unsafeString, charactersToRemove);
            // if there is more than one token, then an offending character was
            // found and must be removed
            if (safeTokens.countTokens() > 1) {
                StringBuffer safeResult = new StringBuffer(unsafeString.length());
                while (safeTokens.hasMoreTokens()) {
                    safeResult.append(safeTokens.nextToken());
                }
                return safeResult.toString();
            } else {
                return unsafeString;
            }
        } else {
            return "";
        }
    }

    /**
     * padToLength("bob", 6) -> "bob " padToLength("bob", 6, "&nbsp;") ->
     * "bob&nbsp;&nbsp;&nbsp;" padToLengthL("bob", 6) -> " bob"
     * padToLengthL("bob", 6, "&nbsp;") -> "&nbsp;&nbsp;&nbsp;bob"
     * 
     * padToLengthL("bob", 8, ".") -> ".....bob"
     */
    public static String padToLength(String baseString, int finalLength) {
        if (baseString == null) {
            baseString = "";
        }
        while (baseString.length() < finalLength) {
            baseString += ' ';
        }
        return baseString;
    }

    public static String padToLength(String baseString, int finalLength, String padString) {
        if (baseString == null) {
            baseString = "";
        }
        if (padString == null) {
            padString = "";
        }

        String append = "";
        for (int numCharsToAdd = finalLength - baseString.length(); numCharsToAdd > 0; numCharsToAdd--) {
            append += padString;
        }
        return baseString + append;
    }

    public static String padToLengthL(String baseString, int finalLength) {
        if (baseString == null) {
            baseString = "";
        }
        String prepend = "";
        for (int numCharsToAdd = finalLength - baseString.length(); numCharsToAdd > 0; numCharsToAdd--) {
            prepend += ' ';
        }
        return prepend + baseString;
    }

    public static String padToLengthL(String baseString, int finalLength, String padString) {
        if (baseString == null) {
            baseString = "";
        }
        if (padString == null) {
            padString = "";
        }
        String prepend = "";
        for (int numCharsToAdd = finalLength - baseString.length(); numCharsToAdd > 0; numCharsToAdd--) {
            prepend += padString;
        }
        return prepend + baseString;
    }

    /**
     * Takes a string of text and trims it to the maxNumberOfChars (null input
     * string yields an empty string). Example: String sample = "I like dogs and
     * cats" shortenText(sample, 14, true) // gives "I like dogs..." instead of
     * "I like dogs an"
     * 
     * maxNumberOfChars does not count the three periods if includeEllipsis
     * always leaves at least ten of the original characters
     */
    public static String shortstring(String text, int maxNumberOfChars, boolean includeEllipsis) {
        if (text == null) {
            return "";
        }

        String sampledText;
        boolean mustChop = (text.length() > maxNumberOfChars);

        // if the string is originally too long, chop it to last complete word,
        // and include ellipsis
        if (mustChop) {
            // string is too long, must chop
            // get raw string up to endIndex
            sampledText = text.substring(0, maxNumberOfChars);
            // chop off incomplete word (always leave at least ten chars)
            int indexOfLastSpace = sampledText.lastIndexOf(' ');
            if (indexOfLastSpace > 10) {
                sampledText = sampledText.substring(0, indexOfLastSpace);
            }
            sampledText += ((includeEllipsis) ? "..." : "");
        } else {
            sampledText = text;
        }
        // stem.out.println("shortstring: " + sampledText);
        return sampledText;
    }

    @SuppressWarnings("unchecked")
    public static ArrayList arrayToArrayList(Object[] oldArray) {
        ArrayList newArray = null;
        if (oldArray != null) {
            newArray = new ArrayList(oldArray.length);
            for (int i = 0; i < oldArray.length; i++) {
                newArray.add(i, oldArray[i]);
            }
        } else {
            newArray = new ArrayList(0);
        }
        return newArray;
    }

    /**
     * formats an array { 1, 2, 3 } as &quot 1 2 3 &quot
     */
    public static String arrayToString(long[] array) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuffer string = new StringBuffer();
        for (int i = 0; i < array.length; i++) {
            string.append(' ');
            string.append(array[i]);
        }
        if (array.length > 0) {
            string.append(' ');
        }
        return string.toString();
    }

    public static String arrayToString(ArrayList array) {
        if (array != null) {
            return arrayToString(array.toArray());
        } else {
            return arrayToString(new Object[0]);
        }
    }

    /**
     * formats an array { 1, 2, 3 } as &quot thingy1, thingy,
     * edu.bju.app.entity@123456 &quot
     */
    public static String arrayToString(Object[] array) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuffer string = new StringBuffer();
        // print the first element
        string.append(' ');
        string.append(array[0]);

        // print all subsequent elements
        for (int i = 1; i < array.length; i++) {
            string.append(", ");
            string.append(array[i]);
        }

        // print termination character
        if (array.length > 0) {
            string.append(' ');
        }
        return string.toString();
    }

    /**
     * formats an enumeration as &quot thingy1, thingy,
     * edu.bju.app.entity@123456 &quot
     */
    public static String arrayToString(Enumeration array) {
        if (array == null || !array.hasMoreElements()) {
            return "";
        }
        StringBuffer string = new StringBuffer();
        // print the first element
        string.append(' ');
        string.append(array.nextElement());

        // print all subsequent elements
        while (array.hasMoreElements()) {
            string.append(", ");
            string.append(array.nextElement());
        }

        // print termination character
        string.append(' ');

        return string.toString();
    }

    public static final int compare(int first, int second) {
        if (first < second) {
            return COMPARE_LESS_THAN;
        } else if (first == second) {
            return COMPARE_EQUAL_TO;
        } else {
            return COMPARE_GREATER_THAN;
        }
    }

    public static final int compare(long first, long second) {
        if (first < second) {
            return COMPARE_LESS_THAN;
        } else if (first == second) {
            return COMPARE_EQUAL_TO;
        } else {
            return COMPARE_GREATER_THAN;
        }
    }

    @SuppressWarnings("unchecked")
    public static final int compare(Comparable first, Comparable second) {
        if (first != null) {
            return first.compareTo(second);
        } else {
            return (second == null) ? COMPARE_EQUAL_TO : COMPARE_GREATER_THAN;
        }
    }

    public static final int compareIgnoreCase(String first, String second) {
        if (first != null) {
            return first.compareToIgnoreCase(second);
        } else {
            return (second == null) ? COMPARE_EQUAL_TO : COMPARE_GREATER_THAN;
        }
    }

    /**
     * "null safe" comparison of the two objects.
     * 
     * @param first
     * @param second
     * @return True if objects are both null or first.equals(second); false
     *         otherwise.
     */
    public static final boolean equal(Object first, Object second) {
        // if the first is not null,
        // then they are equal if the first.equals(second)
        if (first != null) {
            return first.equals(second);

            // if the first is null,
            // then they are equal if the second is also null
        } else {
            return second == null;
        }
    }

    public static final boolean notEqual(Object first, Object second) {
        return !equal(first, second);
    }

    public static final boolean different(Object first, Object second) {
        return !equal(first, second);
    }

    public static boolean isNumeric(String str) {
        boolean isNumeric = true;
        char c;
        for (int i = 0; i < str.length() && isNumeric; i++) {
            c = str.substring(i).charAt(0);
            isNumeric = Character.isDigit(c) || c == '-';
        }
        return isNumeric;
    }

    public static String formatId(long bjuId) {
        String id = Long.toString(bjuId);
        while (id.length() < 6) {
            id = "0" + id;
        }
        return id;
    }

    public static int random(int maxValue) {
        return (int) (Math.random() * maxValue);
    }

    public static int random(int minValue, int maxValue) {
        int range = (maxValue - minValue) + 1;
        return ((int) (Math.random() * range)) + minValue;
    }

    /**
     * takes a string <possibleLong> and attempts to parse it. if it is not
     * parseable the <backupValue> will be returned
     */
    public static long parseLong(String possibleLong, long backupValue) {
        long returnValue = 0;
        try {
            if (possibleLong != null && possibleLong.trim().length() > 0) {
                returnValue = Long.parseLong(possibleLong);
            }
            return returnValue;

        } catch (Exception e) {
            return backupValue;
        }
    }

    /**
     * takes a string <possibleDate> and attempts to parse it using the given
     * format. if it is not parseable null will be returned
     */
    public static Date parseDate(String possibleDate, String format) {
        Date returnValue = null;
        try {
            SimpleDateFormat datef = new SimpleDateFormat(format);
            if (possibleDate != null) {
                returnValue = datef.parse(possibleDate);
            }
            return returnValue;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * takes a string <possibleDouble> and attempts to parse it. if it is not
     * parseable the <backupValue> will be returned
     */
    public static double parseDouble(String possibleDouble, double backupValue) {
        double returnValue = 0;
        try {
            if (possibleDouble != null && possibleDouble.trim().length() > 0) {
                returnValue = Double.parseDouble(possibleDouble);
            }
            return returnValue;

        } catch (Exception e) {

            return backupValue;
        }
    }

    /**
     * Accepts any format string (that includes digits) and creates a double out
     * of it (primarily used for currency).
     * 
     * @param num
     *            the string to be parsed.
     * @return the double value represented by the string argument.
     */
    public static double parseDirtyDouble(String num) {
        double value = -1.0;

        if (num != null && num.length() > 0) {
            StringBuffer cleanNum = new StringBuffer(num.length());
            char[] chars = num.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if ((chars[i] >= '0' && chars[i] <= '9') || chars[i] == '.' || chars[i] == '-') {
                    cleanNum.append(chars[i]);
                }
            }

            if (cleanNum.length() > 0) {
                value = Double.parseDouble(cleanNum.toString());
            }
        }
        return value;
    }

    /**
     * Multiplies two numbers of types double, float, long or integer (the
     * number objects do not have to be of the same type)
     */
    public static Number multiply(Number num1, Number num2) {
        Number product = null;
        if (num1 instanceof Double || num2 instanceof Double) {
            product = new Double(((num1 instanceof Double) ? ((Double) num1).doubleValue() : (num1 instanceof Float) ? ((Float) num1).floatValue()
                    : (num1 instanceof Long) ? ((Long) num1).longValue() : (num1 instanceof Integer) ? ((Integer) num1).intValue() : -1)
                    * ((num2 instanceof Double) ? ((Double) num2).doubleValue() : (num2 instanceof Float) ? ((Float) num2).floatValue()
                            : (num2 instanceof Long) ? ((Long) num2).longValue() : (num2 instanceof Integer) ? ((Integer) num2).intValue() : -1));

        } else if (num1 instanceof Float || num2 instanceof Float) {
            product = new Float(((num1 instanceof Float) ? ((Float) num1).floatValue() : (num1 instanceof Long) ? ((Long) num1).longValue()
                    : (num1 instanceof Integer) ? ((Integer) num1).intValue() : -1)
                    * ((num2 instanceof Float) ? ((Float) num2).floatValue() : (num2 instanceof Long) ? ((Long) num2).longValue()
                            : (num2 instanceof Integer) ? ((Integer) num2).intValue() : -1));

        } else if (num1 instanceof Long || num2 instanceof Long) {
            product = new Long(((num1 instanceof Long) ? ((Long) num1).longValue() : (num1 instanceof Integer) ? ((Integer) num1).intValue() : -1)
                    * ((num2 instanceof Long) ? ((Long) num2).longValue() : (num2 instanceof Integer) ? ((Integer) num2).intValue() : -1));

        } else if (num1 instanceof Integer || num2 instanceof Integer) {
            product = new Integer(((num1 instanceof Integer) ? ((Integer) num1).intValue() : -1)
                    * ((num2 instanceof Integer) ? ((Integer) num2).intValue() : -1));

        }
        return product;
    }

    // public static String formatPercent(int selection, int total) {
    // return ((float) selection / (float) total) * Util.TWO_DECIMAL_PLACES +
    // "%";
    // }
    //	    
    // public static String formatPercent(long selection, long total) {
    // return ((float) selection / (float) total) * Util.TWO_DECIMAL_PLACES +
    // "%";
    // }

    /**
     * pluralize(1, hour) => hour pluralize(2, hour) => 2 hours
     */
    public static String pluralize(long num, String word) {
        return (num == 1 ? word : num + " " + word + "s");
    }

    public static Object[] expandArray(Object[] small, int numNewSlots) {
        if (small != null && numNewSlots > 0) {
            Object[] large = new Object[small.length + numNewSlots];
            for (int i = 0; i < small.length; i++) {
                large[i] = small[i];
            }
            return large;
        } else if (small == null) {
            return new Object[1];
        } else {
            return small;
        }
    }

    public static long[] expandArray(long[] small, int numNewSlots) {
        if (small != null && numNewSlots > 0) {
            long[] large = new long[small.length + numNewSlots];
            for (int i = 0; i < small.length; i++) {
                large[i] = small[i];
            }
            return large;
        } else if (small == null) {
            return new long[1];
        } else {
            return small;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T, E> HashMap<T, E> convertListToHashMap(List<E> arli, String methodToInvoke, Class T) throws Exception {
        HashMap<T, E> hashi = new HashMap<T, E>();
        for (E e : arli) {
            Method m = e.getClass().getMethod(methodToInvoke);
            if (m.getReturnType() != T) {
                throw new ClassCastException();
            }
            T t = (T) m.invoke(e);
            hashi.put(t, e);
        }
        return hashi;
    }

    /*
     * this method lets you to get a word enclosed in double quotes, method
     * created in order to be called from velocity code @author martin amaris
     * @param word word to be enclosed on double quote @return returns a word
     * enclosed on double quote
     */
    public static String doubleQuoteIt(String word) {
        return "\"" + word + "\"";
    }

    public static String getTemporaryDirPath() {
        String tempdir = System.getProperty("java.io.tmpdir");
        if (tempdir == null)
            tempdir = "temp";
        if (!tempdir.endsWith(File.separator))
            tempdir = tempdir + File.separator;
        File tempDirFile = new File(tempdir);
        if (!tempDirFile.exists())
            tempDirFile.mkdirs();
        else if (tempDirFile.exists() && tempDirFile.isFile()) {
            tempDirFile.delete();
            tempDirFile.mkdirs();
        }
        return tempdir;
    }
    
    public static Company getDefaultCompany()
    {
    	return CompanyUtils.getDefaultCompany();
    }
    
    public static String getDotCMSStackTrace() {
    	StringBuilder strB = new StringBuilder ();
//    	StackTraceElement[] elems = Thread.currentThread().getStackTrace();
//    	for(StackTraceElement el : elems) {
//    		if(el.getClassName().startsWith("com.dotmarketing")) {
//    			strB.append(el.toString() + "\n");
//    		}
//    	}
    	return strB.toString();
    
    }
    
    public static boolean contains(String string1, String string2){
    	return(string1.contains(string2));
    }
    
    
    public static boolean compareVersions(String v1, String v2){
    	String[] v1Arr = v1.split("\\.");
    	String[] v2Arr = v2.split("\\.");
    	boolean isMajor = false;
    	String version1Str = "";
    	String version2Str = "";
    	if(v1Arr.length > v2Arr.length){
    		version1Str = v1.replaceAll("\\.", "");
    		version2Str = v2.replaceAll("\\.", "");
    		for(int i=v2Arr.length; i<v1Arr.length;i++){
    			version2Str+="0";
    		}
    		if(Long.parseLong(version1Str)>Long.parseLong(version2Str)){
    			isMajor = true;
    		}
    	}else if(v2Arr.length > v1Arr.length){
    		version1Str = v1.replaceAll("\\.", "");
    		version2Str = v2.replaceAll("\\.", "");
    		for(int i=v1Arr.length; i<v2Arr.length;i++){
    			version1Str+="0";
    		}
    		if(Long.parseLong(version1Str)>Long.parseLong(version2Str)){
    			isMajor = true;
    		}
    	}else{
    		long version1 = Long.parseLong(v1.replaceAll("\\.",""));
    		long version2 = Long.parseLong(v2.replaceAll("\\.",""));
    		if(version1>version2){
    			isMajor = true;
    		}
    	}
    	return isMajor;
    }
    
    public static String getStringFromReader(Reader rd) throws IOException {  
    	StringBuilder sb = new StringBuilder();
    	int cp;
    	while ((cp = rd.read()) != -1) {
    		sb.append((char) cp);
    	}
    	return sb.toString();
    }
    
    public static String getVersionInfoTableName(String asset_type) {
        if(asset_type.equals("links"))
            return "link_version_info";
        else if(asset_type.equals("contentlet"))
            return "contentlet_version_info";
        else if(asset_type.equals("containers"))
            return "container_version_info";
        else if(asset_type.equals("template"))
            return "template_version_info";
        else if(asset_type.equals("file_asset"))
            return "fileasset_version_info";
        else if(asset_type.equals("htmlpage"))
            return "htmlpage_version_info";
        else return null;
    }
    
    public static Class getVersionInfoType(String type) {
        if(type.equals("links"))
            return LinkVersionInfo.class;
        else if(type.equals("contentlet"))
            return ContentletVersionInfo.class;
        else if(type.equals("containers"))
            return ContainerVersionInfo.class;
        else if(type.equals("template"))
            return TemplateVersionInfo.class;
        else if(type.equals("file_asset"))
            return FileAssetVersionInfo.class;
        else if(type.equals("htmlpage"))
            return HTMLPageVersionInfo.class;
        else return null;
    }
}