package com.dotcms.telemetry.collectors.sitesearch;

import com.dotcms.content.index.domain.IndexStats;
import com.dotmarketing.exception.DotDataException;

import java.util.Collection;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import com.dotcms.telemetry.MetricsProfile;
import com.dotcms.telemetry.ProfileType;

/**
 * Collect the total size in Mb of all the Site Search indices.
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
