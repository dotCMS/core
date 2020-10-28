package com.dotmarketing.util;

import static org.apache.commons.lang.StringUtils.split;

import com.dotcms.repackage.com.google.common.base.CaseFormat;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotcms.repackage.org.jsoup.Jsoup;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    public static final String TRUE = "true";

    private final static char COMMA = ',';
    
    // Pattern is threadsafe
    private static final Pattern camelCaseLowerPattern = Pattern.compile("^[a-z]([a-zA-Z0-9]+)*$");
    private static final Pattern camelCaseUpperPattern = Pattern.compile("^[A-Z]([a-zA-Z0-9]+)*$");

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

    public static boolean isHtml(final String htmlString) {

        if(!htmlString.contains("<") || !htmlString.contains(">")){
            return false;
        }

        try {
            Jsoup.parse(htmlString);
        } catch (Exception e) {
            return false;
        }

        return true;
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
        String var = variable.toLowerCase().replaceAll("[^a-z\\d]", StringPool.DASH);
        while (UtilMethods.isSet(var) && (var.startsWith("-")
                || Character.isDigit(var.charAt(0)))) {
            var = var.substring(1);
        }
        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL,var);
    }
    
    public static String camelCaseUpper(String variable) {
            // are we already camelCase?
        if(camelCaseUpperPattern.matcher(variable).find()){
            return variable;
        }
        String ret = camelCaseLower(variable);
        if (UtilMethods.isSet(ret)) {

            String firstChar = ret.substring(0, 1);
            return firstChar.toUpperCase() + ret.substring(1);
        }

        return ret;
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

    public static <T> T getOrDefault (final T value, final Supplier<T> defaultSupplier) {
        return value != null ? value : defaultSupplier.get();
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

    /**
     * Abbreviate for StringBuilder
     * @return StringBuilder
     */
    public static StringBuilder builder () {

        return new StringBuilder();
    }

    /**
     * Abbreviate for StringBuilder
     * @param sequences (optional array of sequences)
     * @return StringBuilder
     */
    public static StringBuilder builder (final CharSequence... sequences) {

        final StringBuilder builder = new StringBuilder();

        if (null != sequences) {

            for (final CharSequence charSequence: sequences) {

                if (null != charSequence) {
                    builder.append(charSequence);
                }
            }
        }

        return builder;
    } // builder.

    /**
     * Abbreviate for StringBuilder
     * @param objects (optional array of Object, the toString will be append to the builder)
     * @return StringBuilder
     */
    public static StringBuilder builder (final Object... objects) {

        final StringBuilder builder = new StringBuilder();

        if (null != objects) {

            for (final Object object: objects) {

                if (null != object) {
                    builder.append(object.toString());
                }
            }
        }

        return builder;
    } // builder.

    public static String lowercaseStringExceptMatchingTokens(final String query,
            final String regex) {

        final StringTokenizer tokenizer = new StringTokenizer(query, " ", true);
        final StringBuilder loweredString = new StringBuilder();

        while(tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            if(!token.matches(regex)) {
                token = token.toLowerCase();
            }
            loweredString.append(token);
        }

        return loweredString.toString();
    }
    
    
    /**
     * this is just a reference to the FileUtil methos, 
     * added for findability
     * @param fileName
     * @return
     */
    public static String sanitizeFileName(final String fileName) {
      return FileUtil.sanitizeFileName(fileName);
      
    }

    private static final Pattern quotedLiteral = Pattern.compile("(?:^|\\s)*['\"]([a-zA-Z0-9-_]+)['\"](?:$|\\s)*", Pattern.MULTILINE);

    /**
     * This method is useful to isolate quoted literals hence `my-literal` or `12345abc` or `1se34s-23r45-eE8u76-223df` or even "this"
     * Anything enclosed within quotes (single and double) containing an alphanumeric char or dash will be matched.
     * if the enclosed text has a blank it is not considered a literal.
     * Meaning that neither `1234 456` nor `abc def`  will no be matched.
     * @param input a multiline string
     * @return list of matches found.
     */
    public static List<String> quotedLiteral(final String input) {
        final List<String> strings = new ArrayList<>();
        final Matcher matcher = quotedLiteral.matcher(input);
        while (matcher.find()) {
            strings.add(matcher.group(1));
        }
        return strings;
    }
    
    
}