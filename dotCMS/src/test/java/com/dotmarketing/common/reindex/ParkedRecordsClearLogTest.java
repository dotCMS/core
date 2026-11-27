package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;

/**
 * Unit tests for the log lines of the operator's clear of parked reindex records (#37270).
 *
 * <p>Clearing parked records deletes the only evidence that content is missing from the index, so
 * the clear says who did it, how many records it dropped and which identifiers. After a 429 storm
 * during a migration reindex there can be tens of thousands of them: one line per identifier, or
 * one line holding all of them, would flood the log, so the identifiers go out in chunks.</p>
 *
 * <pre>
 *   ./mvnw test -pl :dotcms-core -Dmaven.build.cache.enabled=false -Dtest=ParkedRecordsClearLogTest
 * </pre>
 */
public class ParkedRecordsClearLogTest {

    /**
     * Given Scenario: 1,201 identifiers were cleared by user {@code dotcms.org.1}.
     * Expected Result: A first line naming the user and the count, then the identifiers in 3
     * lines of at most 500 each, every identifier exactly once.
     */
    @Test
    public void chunksIdentifiersAfterAHeaderLine() {
        final List<String> identifiers = IntStream.range(0, 1_201).mapToObj(i -> "id-" + i)
                .collect(Collectors.toList());

        final List<String> lines = ReindexQueueAPIImpl.clearLogLines("dotcms.org.1", identifiers);

        assertEquals(4, lines.size());
        assertTrue(lines.get(0), lines.get(0).contains("dotcms.org.1"));
        assertTrue(lines.get(0), lines.get(0).contains("1201"));
        final List<String> listed = new ArrayList<>();
        for (final String line : lines.subList(1, lines.size())) {
            // Each identifier line is "<label>: id, id, ...": parse it rather than search it.
            final List<String> ids = Arrays.asList(
                    line.substring(line.indexOf("): ") + 3).split(", "));
            assertTrue("at most 500 identifiers per line, got " + ids.size(), ids.size() <= 500);
            listed.addAll(ids);
        }
        assertEquals("every identifier is logged once, in order", identifiers, listed);
    }

    /**
     * Given Scenario: Nothing was parked when the operator cleared.
     * Expected Result: Only the line naming the user and the count (0).
     */
    @Test
    public void noIdentifiersOnlyTheHeaderLine() {
        final List<String> lines = ReindexQueueAPIImpl.clearLogLines("dotcms.org.1", List.of());

        assertEquals(1, lines.size());
        assertTrue(lines.get(0), lines.get(0).contains(" 0 "));
    }
}
