package com.dotmarketing.util;

import java.text.DecimalFormat;
import java.util.Optional;
import java.util.function.Supplier;

public class NumberUtil {

	private static final DecimalFormat formatter = new DecimalFormat("0000000000000000000.000000000000000000"); 

	/**
	 * Will pad a number/decimal to 64bit meaning 19 characters.
	 * @param n
	 * @return
	 */
	public static String pad(Number n) { 
		return formatter.format(n); 			
	}

	/**
	 * try to convert to integer the string, if any error will return the defaultOne.
	 * @param sInt {@link String}
	 * @param defaultOne supplier int
	 * @return int
	 */
	public static int toInt (final String sInt, final Supplier<Integer> defaultOne) {

		try {
			return (UtilMethods.isSet(sInt))?
				Integer.parseInt(sInt):defaultOne.get();
		} catch(NumberFormatException e) {
			return defaultOne.get();
		}
	} // toInt.

	/**
	 * try to convert to long the string, if any error will return the defaultOne.
	 * @param sLong {@link String}
	 * @param defaultOne supplier long
	 * @return long
	 */
	public static long toLong (final String sLong, final Supplier<Long> defaultOne) {

		try {
			return (UtilMethods.isSet(sLong))?
					Long.parseLong(sLong):defaultOne.get();
		} catch(NumberFormatException e) {
			return defaultOne.get();
		}
	} // toLong.

    /**
     * try to convert to boolean the string, if any error will return the defaultOne.
     * @param sBoolean {@link String}
     * @param defaultOne supplier boolean
     * @return Boolean
     */
    public static Boolean toBoolean (final String sBoolean, final Supplier<Boolean> defaultOne) {

        try {
            return UtilMethods.isSet(sBoolean)?
                    Boolean.valueOf(sBoolean):defaultOne.get();
        } catch(NumberFormatException e) {
            return defaultOne.get();
        }
    } // toBoolean.

	/**
	 * Coerces a value that may be a {@link Number} or a numeric {@link String} (e.g. a column
	 * value returned from a SQL result row) to an int. Returns 0 when the value is null.
	 *
	 * @param value the value to coerce
	 * @return the int value, or 0 when null
	 */
	public static int asInt(final Object value) {
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		return value != null ? Integer.parseInt(value.toString()) : 0;
	} // asInt.

	/**
	 * Best-effort coercion to a {@link Long} of a value that may already be a {@link Number} or a
	 * numeric {@link String}.
	 *
	 * <p>Unlike {@link #toLong(String, Supplier)} this reports failure instead of substituting a
	 * default: an empty result means "this value is not a long", which lets the caller omit the
	 * value entirely rather than fabricate one. Parsing is strict — a value carrying a fractional
	 * part is <em>not</em> silently truncated.</p>
	 *
	 * @param value the value to coerce; may be null
	 * @return the coerced value, or empty when it cannot be represented as a long
	 */
	public static Optional<Long> toLongOrEmpty(final Object value) {

		if (value instanceof Number) {
			final Number number = (Number) value;
			// Reject a value carrying a fractional part rather than truncating it silently.
			// Also rejects NaN and the infinities, whose longValue() is a meaningless clamp.
			if (number.longValue() != number.doubleValue()) {
				return Optional.empty();
			}
			return Optional.of(number.longValue());
		}
		if (!(value instanceof String)) {
			return Optional.empty();
		}
		final String candidate = ((String) value).trim();
		if (!UtilMethods.isSet(candidate)) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.parseLong(candidate));
		} catch (final NumberFormatException e) {
			// Not a long: non-numeric text, a fractional value, or beyond Long's range.
			return Optional.empty();
		}
	} // toLongOrEmpty.

	/**
	 * Best-effort coercion to a {@link Float} of a value that may already be a {@link Number} or a
	 * numeric {@link String}. Same reporting semantics as {@link #toLongOrEmpty(Object)}.
	 *
	 * @param value the value to coerce; may be null
	 * @return the coerced value, or empty when it cannot be represented as a finite float
	 */
	public static Optional<Float> toFloatOrEmpty(final Object value) {

		if (value instanceof Number) {
			return finiteOrEmpty(((Number) value).floatValue());
		}
		if (!(value instanceof String)) {
			return Optional.empty();
		}
		final String candidate = ((String) value).trim();
		if (!UtilMethods.isSet(candidate)) {
			return Optional.empty();
		}
		try {
			return finiteOrEmpty(Float.parseFloat(candidate));
		} catch (final NumberFormatException e) {
			return Optional.empty();
		}
	} // toFloatOrEmpty.

	/**
	 * Guards against the non-finite values {@link Float#parseFloat} accepts. "Infinity" and "NaN"
	 * parse successfully but cannot be serialized to JSON, so letting them through would push the
	 * failure downstream into the index write instead of reporting it here.
	 */
	private static Optional<Float> finiteOrEmpty(final float candidate) {
		return Float.isFinite(candidate) ? Optional.of(candidate) : Optional.empty();
	} // finiteOrEmpty.

} // E:O:F:NumberUtil
