package com.dotmarketing.util;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.dotcms.repackage.edu.emory.mathcs.backport.java.util.Collections;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.liferay.util.StringPool;

import static com.dotmarketing.util.RegEX.find;
import static javafx.scene.input.KeyCode.V;

public class StringUtils {

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

	// this the pattern for replace variables, such as {xxx} @see interpolate method
	private static final String ALPHA_VARIABLE_REGEX = "(\\{\\w+\\})";

	/**
	 * Replace any expression with {?} by the right value in the context Map (interpolation)
	 * The objects inside the Map values will be called by the toString method.
	 * @param expression {@link String}
	 * @param parametersMap {@link Map}
	 * @return String
	 */
	public  static String interpolate (final String expression,
									 final Map<String, Object> parametersMap) {

		// PRECONDITIONS
		if (null == expression) {

			return StringPool.BLANK;
		}

		if (null == parametersMap || parametersMap.size() == 0) {

			return expression;
		}

		final StringBuilder interpolatedBuilder =
				new StringBuilder(expression);
		String normalizeMatch = null;
		final List<RegExMatch> regExMatches =
				RegEX.find(expression, ALPHA_VARIABLE_REGEX);

		if ((null != regExMatches) && (regExMatches.size() > 0)) {

			// we need to start replacing from the end, to avoid conflicts with the shift chars.
			Collections.reverse(regExMatches);

			for (RegExMatch regExMatch : regExMatches) {

				if (null != regExMatch.getMatch() && regExMatch.getMatch().length() > 2) {

					// removes from the match the curly braces {}
					normalizeMatch = regExMatch.getMatch().substring
							(1, regExMatch.getMatch().length() - 1);

					if (null != parametersMap.get(normalizeMatch)) {

						interpolatedBuilder.replace(regExMatch.getBegin(),
								regExMatch.getEnd(), parametersMap.get(normalizeMatch).toString());
					}
				}
			}
		}

		return interpolatedBuilder.toString();
	} // interpolate.




}
