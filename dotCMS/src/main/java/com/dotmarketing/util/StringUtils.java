package com.dotmarketing.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

import com.dotcms.repackage.com.google.common.base.CaseFormat;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.liferay.util.StringPool;

import static com.dotcms.repackage.org.apache.commons.lang.StringUtils.*;

public class StringUtils {

    public static final String TRUE = "true";

    private final static char COMMA = ',';
    
    // Pattern is threadsafe
    private static final Pattern camelCaseLowerPattern = Pattern.compile("^[a-z]+([A-Z][a-z0-9]+)+");
    private static final Pattern camelCaseUpperPattern = Pattern.compile("^[A-Z]+([A-Z][a-z0-9]+)+");

    private static Map<String, Pattern> patternCacheMap = new ConcurrentHashMap<>();

    public static String formatPhoneNumber(String phoneNumber) {
        try {
            String s = phoneNumber.replaceAll("\\(|\\)|:|-|\\.", "");

            s = s.replaceAll("(\\d{3})(\\d{3})(\\d{4})(\\d{3})*", "($1) $2-$3x$4");

            if (s.endsWith("x")){
                s = s.substring(0, s.length() - 1);
            }
            return s;
        } catch (Exception ex) {
            return "";
        }
    }




    public static boolean isJson(String jsonString) {
        if(!jsonString.contains("{") || !jsonString.contains("}")){
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
    
    public static String camelCaseLower(String variable) {
        // are we already camelCase?
        if(camelCaseLowerPattern.matcher(variable).find()){
            return variable;
        }
        String var = variable.toLowerCase().replaceAll("[^a-z\\d]", "-");
        while(var.startsWith("-")){
            var =var.substring(1, var.length());
        }
        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL,var);
    }
    
    public static String camelCaseUpper(String variable) {
            // are we already camelCase?
        if(camelCaseUpperPattern.matcher(variable).find()){
            return variable;
        }
        String ret = camelCaseLower(variable);
        String firstChar = ret.substring(0,1);
        
        return firstChar.toUpperCase() + ret.substring(1,ret.length());
    }
    
    public static boolean isSet(String test){
        return UtilMethods.isSet(test);
    }
    
    public static String nullEmptyStr(String test){
        return isSet(test) ? test : null;
    }
    
    /**
     * Split the string by commas
     * Pre: string argument must be not null
     * @param string {@link String}
     * @return String array
     */

    public static String [] splitByCommas (final String string) {

        return split(string, COMMA);
    } // splitByComma.

    // this the pattern for replace variables, such as {xxx} @see interpolate method
    private static final String ALPHA_HYPHEN_VARIABLE_REGEX = "{(.*?)}";

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

        String normalizeMatch;

        final List<RegExMatch> regExMatches =
                RegEX.find(expression, ALPHA_HYPHEN_VARIABLE_REGEX);

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

    /**
     * Retrieve a map with all the expression that successfully found a match in the parametersMap.
     *
     * @param expression {@link String}
     * @param parametersMap {@link Map}
     * @return Map with all matches, key is the variable and value is the match for that variable.
     */
    public static Map<String, Object> getInterpolateMatches(final String expression,
                                                            final Map<String, Object> parametersMap){
        Map<String, Object> interpolateMatches = new HashMap<>();
        // PRECONDITIONS
        if (null != expression && null != parametersMap && parametersMap.size() != 0) {
            final List<RegExMatch> regExMatches = RegEX.find(expression, ALPHA_HYPHEN_VARIABLE_REGEX);

            if ((null != regExMatches) && (regExMatches.size() > 0)) {
                // we need to start replacing from the end, to avoid conflicts with the shift chars.
                Collections.reverse(regExMatches);

                for (RegExMatch regExMatch : regExMatches) {

                    if (null != regExMatch.getMatch() && regExMatch.getMatch().length() > 2) {
                        // removes from the match the curly braces {}
                        String normalizeMatch = regExMatch.getMatch().substring(1, regExMatch.getMatch().length() - 1);

                        if (null != parametersMap.get(normalizeMatch)) {
                            interpolateMatches.put(normalizeMatch, parametersMap.get(normalizeMatch).toString());
                        }
                    }
                }
            }
        }

        return interpolateMatches;
    }

    /**
     * Returns true if the pattern match on the string
     * @param string  String string to search
     * @param pattern String pattern to search
     * @return boolean true if match
     */
    public static boolean matches (final String string, final String pattern) {

        if (!patternCacheMap.containsKey(pattern)) {
            patternCacheMap.put(pattern, Pattern.compile(pattern));
        }

        return patternCacheMap.get(pattern).matcher(string).matches();
    }
    
    
}