package com.dotcms.content.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The index-name timestamp suffix must mean the same instant when it is read back as when it was
 * written, whatever the JVM default time zone is at either moment. dotCMS changes that default at
 * runtime (company time zone at startup, DB time-zone probe, upgrade tasks), and a suffix written
 * in one zone and read in another made the reindex switchover see a negative elapsed time and
 * stay blocked (issue #37282).
 */
public class IndexTimestampTest {

    private static final List<String> ZONES = List.of("UTC", "Asia/Tokyo", "Pacific/Honolulu");

    private TimeZone originalDefault;

    @Before
    public void rememberDefaultZone() {
        originalDefault = TimeZone.getDefault();
    }

    @After
    public void restoreDefaultZone() {
        TimeZone.setDefault(originalDefault);
    }

    /**
     * A name created just now reads back as created just now — not hours in the future or past —
     * even when the JVM default zone changes between writing and reading the name.
     */
    @Test
    public void elapsedSince_justCreatedName_isNearZeroWhateverTheDefaultZones() {
        for (final String writeZone : ZONES) {
            for (final String readZone : ZONES) {
                TimeZone.setDefault(TimeZone.getTimeZone(writeZone));
                final String indexName = "cluster_abc.working_" + IndexTimestamp.now();

                TimeZone.setDefault(TimeZone.getTimeZone(readZone));
                final Duration elapsed = IndexTimestamp.elapsedSince(indexName);

                assertTrue("written in " + writeZone + ", read in " + readZone
                                + ": expected ~0 elapsed but was " + elapsed,
                        !elapsed.isNegative() && elapsed.compareTo(Duration.ofSeconds(5)) < 0);
            }
        }
    }

    /**
     * The suffix is UTC: the same digits always decode to the same instant, independently of the
     * JVM default zone.
     */
    @Test
    public void createdAt_suffixIsInterpretedAsUtc() {
        final Instant expected = Instant.parse("2026-08-20T13:20:11Z");
        for (final String zone : ZONES) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            assertEquals("default zone " + zone, expected,
                    IndexTimestamp.createdAt("cluster_abc.working_20260820132011"));
        }
    }

    /**
     * Names carrying the OpenSearch tag decode to the same instant as the untagged name.
     */
    @Test
    public void createdAt_taggedName_ignoresTheTag() {
        assertEquals(IndexTimestamp.createdAt("cluster_abc.working_20260820132011"),
                IndexTimestamp.createdAt("cluster_abc.working_20260820132011.os"));
    }

    /**
     * Any instant formats as its UTC digits, whatever the JVM default zone — so a cutoff such as
     * "24 hours ago" compares correctly against suffixes read back as UTC.
     */
    @Test
    public void of_formatsTheInstantInUtc() {
        final Instant instant = Instant.parse("2026-08-20T13:20:11Z");
        for (final String zone : ZONES) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            assertEquals("default zone " + zone, "20260820132011", IndexTimestamp.of(instant));
        }
    }

    /**
     * An index created at 8:21 PM New York time is already the next day in UTC. The Created column
     * must show the New York date and time together (Aug 26, 8:21), not the UTC date next to the
     * New York time, whatever the JVM default zone is (issue #37253).
     */
    @Test
    public void formatCreatedForDisplay_eveningIndex_dateAndTimeAgreeInDisplayZone() {
        final String indexName = "cluster_abc.working_20260827002116";
        final TimeZone newYork = TimeZone.getTimeZone("America/New_York");
        for (final String zone : ZONES) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            final String displayed = IndexTimestamp.formatCreatedForDisplay(indexName, newYork);

            assertTrue("default zone " + zone + ": " + displayed, displayed.contains(" 26 2026 "));
            assertTrue("default zone " + zone + ": " + displayed, displayed.contains(" 8:21"));
        }
        // Positive control: the same name shown in UTC is on the 27th, so the test can tell them apart.
        assertTrue(IndexTimestamp.formatCreatedForDisplay(indexName, TimeZone.getTimeZone("UTC"))
                .contains(" 27 2026 "));
    }

    /**
     * A daytime index is on the same calendar day in UTC and New York, so nothing changes for it.
     */
    @Test
    public void formatCreatedForDisplay_daytimeIndex_keepsTheSameDay() {
        final String displayed = IndexTimestamp.formatCreatedForDisplay(
                "cluster_abc.working_20260826140000.os", TimeZone.getTimeZone("America/New_York"));

        assertTrue(displayed, displayed.contains(" 26 2026 "));
        assertTrue(displayed, displayed.contains(" 10:00"));
    }

    /**
     * A name without a valid suffix is a fault the caller can see, not a silent zero.
     */
    @Test
    public void createdAt_nameWithoutTimestamp_throws() {
        assertThrows(DateTimeParseException.class,
                () -> IndexTimestamp.createdAt("cluster_abc.working_notatimestamp"));
    }
}
