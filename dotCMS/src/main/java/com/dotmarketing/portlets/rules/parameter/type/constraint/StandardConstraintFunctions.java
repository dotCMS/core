package com.dotmarketing.portlets.rules.parameter.type.constraint;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotmarketing.portlets.rules.parameter.comparison.MatcherCheck;
import java.util.function.Function;
import org.hamcrest.Matchers;

/**
 * @author Geoff M. Granum
 */
public final class StandardConstraintFunctions {


    public static final Function<String, Boolean> required = v -> MatcherCheck.verifyThat(v, Matchers.notNullValue());

    /**
     * Minimum string length, inclusive.
     */
    public static Function<String, Boolean> minLength(int min){
        return v ->  v == null || MatcherCheck.verifyThat(v.length(), Matchers.greaterThanOrEqualTo(min));
    }

    /**
     * Maximum string length, inclusive.
     */
    public static Function<String, Boolean> maxLength(int max) {
        return v -> v == null || MatcherCheck.verifyThat(v.length(), Matchers.lessThanOrEqualTo(max));
    }

    /**
     * Is a string represnting a numeric (decimal or integral) value. Does not enforce 'Required' state; this constraint will return true if the value is null or empty.
     * Be aware that this will parse numbers according to the server's Locale. For example, "12,00" won't
     * be valid for the US locale, but will be valid for FR.
     */
    public static Function<String, Boolean> numeric() {
        return v -> {
            boolean valid = StringUtils.isEmpty(v);
            if(!valid){
                try {
                    //noinspection ResultOfMethodCallIgnored
                    Double.parseDouble(v);
                    valid = true;
                } catch (NumberFormatException e) {
                    valid = false;
                }
            }
            return valid;
        };
    }

    /**
     * Is string representing an integer value.
     */
    public static Function<String, Boolean> integral() {
        return v -> {
            boolean valid = StringUtils.isEmpty(v);
            if(!valid) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    Integer.parseInt(v, 10);
                    valid = true;
                } catch (NumberFormatException e) {
                    valid = false;
                }
            }
            return valid;
        };
    }

    /**
     * Is a numeric string that is less than or equal to <code>max</code>.
     * An empty string value is considered valid.
     * @param max the maximum value, inclusive.
     */
    public static Function<String, Boolean> max(double max) {
        return StandardConstraintFunctions.max(max, true );
    }

    /**
     * Is a numeric string that is less than, or less than or equal to <code>max</code>,
     * depending on the value of <code>inclusive</code>.
     * An empty string value is considered valid.
     */
    public static Function<String, Boolean> max(double max, boolean inclusive) {
        return v -> {
            boolean valid = StringUtils.isEmpty(v);
            if(!valid) {
                try {
                    double number = Double.parseDouble(v);
                    valid = inclusive ? (number <= max) : (number < max);
                } catch (NumberFormatException e) {
                    valid = false;
                }
            }
            return valid;
        };
    }

    public static Function<String, Boolean> min(double min) {
        return StandardConstraintFunctions.min(min, true);
    }

    public static Function<String, Boolean> min(double min, boolean inclusive) {
        return v -> {
            boolean valid = StringUtils.isEmpty(v);
            if(!valid) {
                try {
                    double number = Double.parseDouble(v);
                    valid = inclusive ? (number >= min) : (number > min);
                } catch (NumberFormatException e) {
                    valid = false;
                }
            }
            return valid;
        };
    }

    public static Function<String, Boolean> max(int max) {
        return StandardConstraintFunctions.max(max, true);
    }

    public static Function<String, Boolean> max(int max, boolean inclusive) {
        return v -> {
            boolean valid = StringUtils.isEmpty(v);
            if(!valid) {
                try {
                    int number = Integer.parseInt(v);
                    valid = inclusive ? (number <= max) : (number < max);
                } catch (NumberFormatException e) {
                    valid = false;
                }
            }
            return valid;
        };
    }

    public static Function<String, Boolean> min(int min) {
        return StandardConstraintFunctions.min(min, true);
    }

    public static Function<String, Boolean> min(int min, boolean inclusive) {
        return v -> {
            boolean valid = StringUtils.isEmpty(v);
            if(!valid) {
                try {
                    int number = Integer.parseInt(v);
                    valid = inclusive ? (number >= min) : (number > min);
                } catch (NumberFormatException e) {
                    valid = false;
                }
            }
            return valid;
        };
    }

}
 
