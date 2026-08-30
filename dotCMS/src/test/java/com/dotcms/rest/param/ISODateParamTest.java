package com.dotcms.rest.param;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeParseException;
import java.util.Date;
import org.junit.Test;

/**
 * Unit tests for {@link ISODateParam}, the JAX-RS parameter binder used by
 * {@code BundleResource.deleteBundlesOlderThan} to turn a path segment into a {@link Date}.
 *
 * <p>The class delegates to {@code DateUtil.parseISO}, which returns {@code null} — not an
 * exception — whenever {@code UtilMethods.isSet(String)} rejects the input. {@code isSet} rejects
 * {@code null}, blank strings <b>and the literal text {@code "null"}</b>, so a client that
 * interpolates an unset variable into the URL (producing {@code /olderthan/null}) reaches that
 * path. Before Java 25 the constructor could not guard against it: nothing was allowed to precede
 * {@code super(...)}, so {@code parseISO(...).getTime()} dereferenced the null inside the argument
 * list of the superclass call.</p>
 *
 * <p>These tests pin the contract that the constructor rejects unusable input with a
 * {@link ParseException} that names the offending value.</p>
 */
public class ISODateParamTest {

    /**
     * The realistic case: a client interpolates an unset variable and sends the four characters
     * {@code null}. {@code UtilMethods.isSet} treats that text as unset, so {@code parseISO}
     * returns {@code null}.
     */
    @Test
    public void test_literalNullText_throwsParseException() {
        assertRejected("null");
    }

    /**
     * {@code UtilMethods.isSet} trims before measuring, so a whitespace-only segment is unset.
     */
    @Test
    public void test_blankString_throwsParseException() {
        assertRejected(" ");
    }

    @Test
    public void test_emptyString_throwsParseException() {
        assertRejected("");
    }

    @Test
    public void test_nullString_throwsParseException() {
        assertRejected(null);
    }

    /**
     * The rejection message must name the value, otherwise the caller cannot tell which path
     * segment was wrong.
     */
    @Test
    public void test_rejectionMessageNamesTheOffendingValue() throws Exception {
        try {
            new ISODateParam("null");
            fail("Expected a ParseException for the literal text 'null'");
        } catch (final ParseException e) {
            assertNotNull("ParseException must carry a message", e.getMessage());
            if (!e.getMessage().contains("null")) {
                fail("Message must name the offending value, was: " + e.getMessage());
            }
        }
    }

    /**
     * A short ISO date must still parse to the same instant as the plain formatter, so the fix
     * cannot have changed the happy path.
     */
    @Test
    public void test_shortIsoDate_parses() throws Exception {
        final ISODateParam param = new ISODateParam("2026-08-18");
        final Date expected = new SimpleDateFormat("yyyy-MM-dd").parse("2026-08-18");
        assertEquals("Short ISO date must parse to the same instant", expected.getTime(),
                param.getTime());
    }

    /**
     * A full offset date-time must parse as well: this is the form the endpoint documents.
     */
    @Test
    public void test_offsetDateTime_parses() throws Exception {
        final ISODateParam param = new ISODateParam("2026-08-18T10:15:30+00:00");
        assertEquals("Offset date-time must parse to the expected epoch milli",
                java.time.OffsetDateTime.parse("2026-08-18T10:15:30+00:00").toInstant()
                        .toEpochMilli(),
                param.getTime());
    }

    /**
     * Garbage that is short enough to reach {@code SimpleDateFormat} must surface as a
     * {@link ParseException}. This already worked before the fix and must keep working.
     */
    @Test
    public void test_shortGarbage_throwsParseException() {
        assertRejected("not-a-date");
    }

    /**
     * Garbage long enough to reach {@code OffsetDateTime.parse} surfaces as an unchecked
     * {@link DateTimeParseException}. Documented rather than changed: normalising it would alter
     * the behaviour of a live endpoint, which is outside the scope of this fix.
     */
    @Test
    public void test_longGarbage_throwsDateTimeParseException() {
        try {
            new ISODateParam("this-is-definitely-not-a-date");
            fail("Expected the long-garbage branch to throw");
        } catch (final DateTimeParseException expected) {
            // Documented current behaviour of DateUtil.parseISO for inputs longer than 10 chars.
        } catch (final ParseException e) {
            fail("Long garbage is routed to OffsetDateTime.parse, expected DateTimeParseException"
                    + " but got ParseException: " + e.getMessage());
        }
    }

    /**
     * Asserts that the constructor rejects the value with a {@link ParseException} rather than a
     * {@link NullPointerException}.
     *
     * @param value the path-segment text under test
     */
    private void assertRejected(final String value) {
        try {
            new ISODateParam(value);
            fail("Expected a ParseException for input: " + value);
        } catch (final ParseException expected) {
            // The contract under test.
        } catch (final NullPointerException e) {
            fail("Input '" + value + "' produced a NullPointerException instead of a"
                    + " ParseException — the constructor is not validating before super(...)");
        }
    }
}
