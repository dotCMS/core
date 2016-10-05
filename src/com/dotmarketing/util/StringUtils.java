package com.dotmarketing.util;

import java.util.regex.Pattern;

import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;

import static com.dotcms.repackage.org.apache.commons.lang.StringUtils.*;

public class StringUtils {

	public static final char COMMA = ',';


	public static String formatPhoneNumber(String phoneNumber) {
		try {
			String s = phoneNumber.replaceAll("\\(|\\)|:|-|\\.", "");
			;
			s = s.replaceAll("(\\d{3})(\\d{3})(\\d{4})(\\d{3})*", "($1) $2-$3x$4");

			if (s.endsWith("x"))
				s = s.substring(0, s.length() - 1);
			return s;
		} catch (Exception ex) {
			return "";
		}
	}

	public static String sanitizeCamelCase(String variable, boolean firstLetterUppercase) {

		Boolean upperCase = firstLetterUppercase;
		String velocityvar = "";
		String re = "[^a-zA-Z0-9]+";
		Pattern p = Pattern.compile(re);

		for (int i = 0; i < variable.length(); i++) {
			Character c = variable.charAt(i);
			if (upperCase) {
				c = Character.toUpperCase(c);
			} else {
				c = Character.toLowerCase(c);
			}
			if (p.matcher(c.toString()).matches()) {
				upperCase = true;
			} else {
				upperCase = false;
				velocityvar += c;
			}
		}
		velocityvar = velocityvar.replaceAll(re, "");
		return velocityvar;

	}

	public static String sanitizeCamelCase(String variable) {

		return sanitizeCamelCase(variable, false);

	}

	public static boolean isJson(String jsonString) {
		if(jsonString.indexOf("{") <0 || jsonString.indexOf("}") <0){
			return false;
		}
		try {
			if (jsonString.startsWith("{"))
				new JSONObject(jsonString);
			else if (jsonString.startsWith("["))
				new JSONArray(jsonString);

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Split the string by commans
	 * Pre: string argument must be not null
	 * @param string {@link String}
	 * @return String array
     */
	public static String [] splitByCommas (final String string) {

		return split(string, COMMA);
	} // splitByComma.

}
