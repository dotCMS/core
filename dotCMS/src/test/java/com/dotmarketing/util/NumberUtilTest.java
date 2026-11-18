package com.dotmarketing.util;

import com.dotcms.UnitTestBase;
import org.junit.Test;

import java.text.ParseException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for {@link DateUtil}
 * @author jsanca
 */
public class NumberUtilTest extends UnitTestBase {

    @Test
    public void testToIntNull () throws ParseException {

        final int i = NumberUtil.toInt(null, ()-> -1);
        assertEquals(-1, i);
    }

    @Test
    public void testToIntEmpty () throws ParseException {

        final int i = NumberUtil.toInt("", ()-> -1);
        assertEquals(-1, i);
    }

    @Test
    public void testToIntEmpty2 () throws ParseException {

        final int i = NumberUtil.toInt("        ", ()-> -1);
        assertEquals(-1, i);
    }

    @Test
    public void testToIntInvalid () throws ParseException {

        final int i = NumberUtil.toInt("123abc", ()-> -1);
        assertEquals(-1, i);
    }

    @Test
    public void testToInt () throws ParseException {

        final int i = NumberUtil.toInt("123", ()-> -1);
        assertEquals(123, i);
    }

    /////
    @Test
    public void testToLongNull () throws ParseException {

        final long i = NumberUtil.toLong(null, ()-> -1l);
        assertEquals(-1l, i);
    }

    @Test
    public void testToLongEmpty () throws ParseException {

        final long i = NumberUtil.toLong("", ()-> -1l);
        assertEquals(-1l, i);
    }

    @Test
    public void testToLongEmpty2 () throws ParseException {

        final long i = NumberUtil.toLong("        ", ()-> -1l);
        assertEquals(-1l, i);
    }

    @Test
    public void testToLongInvalid () throws ParseException {

        final long i = NumberUtil.toLong("123abc", ()-> -1l);
        assertEquals(-1l, i);
    }

    @Test
    public void testLongInt () throws ParseException {

        final long i = NumberUtil.toLong("123", ()-> -1l);
        assertEquals(123l, i);
    }

    // ------------------------------------------------------------------------------------------
    // toLongOrEmpty / toFloatOrEmpty — issue #37272
    //
    // These report failure instead of substituting a default, because the caller
    // (ESMappingAPIImpl.loadFields) must be able to OMIT the value rather than fabricate one.
    // See specs/37272-textfield-numeric-column/spec.md, Resolved Decisions C-3.
    // ------------------------------------------------------------------------------------------

    @Test
    public void test_toLongOrEmpty_numericString_converts () {

        final Optional<Long> result = NumberUtil.toLongOrEmpty("54");
        assertTrue("a numeric String must convert", result.isPresent());
        assertEquals(Long.valueOf(54L), result.get());
    }

    /**
     * The emitted value has to be the SAME runtime class the natively-stored path produces, or
     * the index document for a converted value differs from one that was stored correctly.
     * {@code Integer 54} is not {@code equals} to {@code Long 54} (AC-009).
     */
    @Test
    public void test_toLongOrEmpty_returnsLong_notInteger () {

        final Object result = NumberUtil.toLongOrEmpty("54").orElseThrow(AssertionError::new);
        assertEquals("emitted class must match the native path", Long.class, result.getClass());
    }

    @Test
    public void test_toLongOrEmpty_negativeAndSigned_convert () {

        assertEquals(Long.valueOf(-54L), NumberUtil.toLongOrEmpty("-54").orElseThrow(AssertionError::new));
        assertEquals(Long.valueOf(54L), NumberUtil.toLongOrEmpty("+54").orElseThrow(AssertionError::new));
    }

    @Test
    public void test_toLongOrEmpty_surroundingWhitespace_converts () {

        assertEquals(Long.valueOf(54L), NumberUtil.toLongOrEmpty("  54  ").orElseThrow(AssertionError::new));
    }

    @Test
    public void test_toLongOrEmpty_alreadyANumber_convertsToLong () {

        assertEquals(Long.valueOf(54L), NumberUtil.toLongOrEmpty(Integer.valueOf(54)).orElseThrow(AssertionError::new));
        assertEquals(Long.valueOf(54L), NumberUtil.toLongOrEmpty(Long.valueOf(54L)).orElseThrow(AssertionError::new));
    }

    @Test
    public void test_toLongOrEmpty_nonNumericText_isEmpty () {

        assertFalse("\"N/A\" is the real-world case from #37272", NumberUtil.toLongOrEmpty("N/A").isPresent());
        assertFalse(NumberUtil.toLongOrEmpty("123abc").isPresent());
    }

    @Test
    public void test_toLongOrEmpty_nullAndBlank_areEmpty () {

        assertFalse(NumberUtil.toLongOrEmpty(null).isPresent());
        assertFalse(NumberUtil.toLongOrEmpty("").isPresent());
        assertFalse(NumberUtil.toLongOrEmpty("        ").isPresent());
    }

    /**
     * Deliberately strict: truncating "54.3" to 54 for an integer-backed column would change the
     * value silently. Empty means the caller omits the field, which is an honest absence rather
     * than a quiet edit. Flagged for the developer at the approval gate — reversible.
     */
    @Test
    public void test_toLongOrEmpty_fractionalValue_isEmpty_notTruncated () {

        assertFalse("a fractional value must not be silently truncated",
                NumberUtil.toLongOrEmpty("54.3").isPresent());
    }

    @Test
    public void test_toLongOrEmpty_overflow_isEmpty () {

        assertFalse("beyond Long.MAX_VALUE cannot be represented",
                NumberUtil.toLongOrEmpty("99999999999999999999").isPresent());
    }

    /////

    @Test
    public void test_toFloatOrEmpty_numericString_converts () {

        final Optional<Float> result = NumberUtil.toFloatOrEmpty("54.3");
        assertTrue("a decimal String must convert", result.isPresent());
        assertEquals(Float.valueOf(54.3f), result.get());
    }

    /** Float, not Double — matching what a float-backed column natively produces (AC-009). */
    @Test
    public void test_toFloatOrEmpty_returnsFloat_notDouble () {

        final Object result = NumberUtil.toFloatOrEmpty("54.3").orElseThrow(AssertionError::new);
        assertEquals("emitted class must match the native path", Float.class, result.getClass());
    }

    @Test
    public void test_toFloatOrEmpty_wholeNumberString_converts () {

        assertEquals(Float.valueOf(54f), NumberUtil.toFloatOrEmpty("54").orElseThrow(AssertionError::new));
    }

    @Test
    public void test_toFloatOrEmpty_alreadyANumber_convertsToFloat () {

        assertEquals(Float.valueOf(54f), NumberUtil.toFloatOrEmpty(Integer.valueOf(54)).orElseThrow(AssertionError::new));
        assertEquals(Float.valueOf(54.3f), NumberUtil.toFloatOrEmpty(Float.valueOf(54.3f)).orElseThrow(AssertionError::new));
    }

    @Test
    public void test_toFloatOrEmpty_nonNumericText_isEmpty () {

        assertFalse(NumberUtil.toFloatOrEmpty("N/A").isPresent());
        assertFalse(NumberUtil.toFloatOrEmpty("123abc").isPresent());
    }

    @Test
    public void test_toFloatOrEmpty_nullAndBlank_areEmpty () {

        assertFalse(NumberUtil.toFloatOrEmpty(null).isPresent());
        assertFalse(NumberUtil.toFloatOrEmpty("").isPresent());
        assertFalse(NumberUtil.toFloatOrEmpty("        ").isPresent());
    }

    /**
     * Float.parseFloat accepts "Infinity" and "NaN". Neither can be indexed — a non-finite value
     * breaks JSON serialization (the class of bug already seen in #36478/#36480), so they must be
     * reported as unconvertible, not passed through.
     */
    @Test
    public void test_toFloatOrEmpty_nonFiniteValues_areEmpty () {

        assertFalse("Infinity must not reach the index", NumberUtil.toFloatOrEmpty("Infinity").isPresent());
        assertFalse(NumberUtil.toFloatOrEmpty("-Infinity").isPresent());
        assertFalse("NaN must not reach the index", NumberUtil.toFloatOrEmpty("NaN").isPresent());
        assertFalse(NumberUtil.toFloatOrEmpty(Float.NaN).isPresent());
        assertFalse(NumberUtil.toFloatOrEmpty(Float.POSITIVE_INFINITY).isPresent());
    }

}
