package com.dotcms.content.index;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * The {@code yyyyMMddHHmmss} suffix carried by every content index name
 * ({@code cluster_<id>.<type>_<suffix>}, optionally followed by an {@link IndexTag}).
 *
 * <p>The suffix is both written when an index is created and read back later to tell how long a
 * reindex has been running, so writing and reading must agree on what the digits mean. This class
 * is the single place that does both, and it always uses UTC: dotCMS changes the JVM default time
 * zone at runtime, so a suffix tied to the default zone could be written in one zone and read in
 * another (issue #37282).</p>
 */
public final class IndexTimestamp {

    /** Immutable and thread-safe; pinned to UTC so the JVM default zone never leaks in. */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private IndexTimestamp() {
    }

    /**
     * Builds the suffix for an index created right now.
     *
     * @return the current UTC time as a {@code yyyyMMddHHmmss} suffix
     */
    public static String now() {
        return of(Instant.now());
    }

    /**
     * Formats an arbitrary instant the way index-name suffixes are written, for example to build
     * a cutoff that is compared against the suffixes of existing indices.
     *
     * @param instant the instant to format
     * @return the instant as a UTC {@code yyyyMMddHHmmss} suffix
     */
    public static String of(final Instant instant) {
        return FORMATTER.format(instant);
    }

    /**
     * Reads the creation instant encoded in an index name's suffix.
     *
     * @param indexName a physical or logical index name, with or without cluster prefix or tag
     * @return the instant the suffix encodes, read as UTC
     * @throws DateTimeParseException if the name does not end in a {@code yyyyMMddHHmmss} suffix
     */
    public static Instant createdAt(final String indexName) {
        // The parser cannot consume a trailing tag, so strip it before taking the suffix.
        final String base = IndexTag.strip(indexName);
        final String suffix = base.substring(base.lastIndexOf('_') + 1);
        return FORMATTER.parse(suffix, Instant::from);
    }

    /**
     * Tells how long ago the index was created, according to its name.
     *
     * @param indexName a physical or logical index name, with or without cluster prefix or tag
     * @return the time elapsed since the instant encoded in the suffix; negative if that instant
     *         is in the future
     * @throws DateTimeParseException if the name does not end in a {@code yyyyMMddHHmmss} suffix
     */
    public static Duration elapsedSince(final String indexName) {
        return Duration.between(createdAt(indexName), Instant.now());
    }
}
