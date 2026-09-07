package com.dotcms.telemetry.collectors.sitesearch;

import com.dotcms.content.index.domain.IndexStats;
import com.dotmarketing.exception.DotDataException;

import java.util.Collection;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;

/**
 * Telemetry metric that reports the combined storage size of all Site Search indices.
 *
 * <p>The value is returned as a human-readable string with the largest applicable unit
 * (b / kb / mb / gb / tb) and at most one decimal place — for example {@code "1.5gb"} or
 * {@code "128mb"}. Trailing {@code .0} decimals are dropped so exact multiples render
 * without noise (e.g. {@code "1gb"}, not {@code "1.0gb"}).
 */
@MetricsProfile(ProfileType.FULL)
@ApplicationScoped
public class TotalSizeSiteSearchIndicesMetricType extends IndicesSiteSearchMetricType {

    @Override
    public String getName() {
        return "TOTAL_INDICES_SIZE";
    }

    @Override
    public String getDescription() {
        return "Total size of indexes";
    }

    @Override
    public Optional<Object> getValue(final Collection<IndexStats> indices) throws DotDataException {
        final long total = indices.stream().mapToLong(IndexStats::sizeRaw).sum();
        return Optional.of(formatBytes(total));
    }

    /**
     * Formats a byte count as a human-readable string using binary units (1 kb = 1024 b).
     * At most one decimal place is shown; trailing {@code .0} is omitted.
     */
    static String formatBytes(final long bytes) {
        if (bytes >= 1L << 40) return format1Decimal(bytes / (double) (1L << 40)) + "tb";
        if (bytes >= 1L << 30) return format1Decimal(bytes / (double) (1L << 30)) + "gb";
        if (bytes >= 1L << 20) return format1Decimal(bytes / (double) (1L << 20)) + "mb";
        if (bytes >= 1L << 10) return format1Decimal(bytes / (double) (1L << 10)) + "kb";
        return bytes + "b";
    }

    private static String format1Decimal(final double value) {
        final String s = String.valueOf(value);
        final int dot = s.indexOf('.');
        if (dot == -1) return s;
        return s.substring(dot + 1).equals("0") ? s.substring(0, dot) : s.substring(0, dot + 2);
    }
}
