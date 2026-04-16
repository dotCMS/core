package com.dotcms.cdn.api;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonDeserialize(builder = DotCDNStats.DotStatsBuilder.class)
public class DotCDNStats {

    @JsonSerialize
    final String cdnDomain;

    @JsonSerialize
    final String dateFrom;

    @JsonSerialize
    final String dateTo;

    @JsonSerialize
    final double cacheHitRate;

    @JsonSerialize
    final long totalBandwidthUsed;

    @JsonSerialize
    final long totalRequestsServed;

    @JsonSerialize
    final String bandwidthPretty;

    @JsonSerialize
    final Map<String, Map<String, Long>> geographicDistribution;

    @JsonSerialize
    final Map<String, Long> bandwidthUsedChart;

    @JsonSerialize
    final Map<String, Long> requestsServedChart;

    private DotCDNStats(DotStatsBuilder builder) {
        this.cdnDomain = builder.cdnDomain;
        this.dateFrom = builder.dateFrom.toString();
        this.dateTo = builder.dateTo.toString();
        this.cacheHitRate = builder.cacheHitRate;
        this.totalBandwidthUsed = builder.totalBandwidthUsed;
        this.totalRequestsServed = builder.totalRequestsServed;
        this.geographicDistribution = builder.geographicDistribution;
        this.bandwidthPretty = prettyByteify(builder.totalBandwidthUsed);
        this.bandwidthUsedChart = builder.bandwidthUsedChart;
        this.requestsServedChart = builder.requestsServedChart;
    }

    /**
     * Creates builder to build {@link DotCDNStats}.
     * @return created builder
     */
    public static DotStatsBuilder builder() {
        return new DotStatsBuilder();
    }

    /**
     * Creates a builder to build {@link DotCDNStats} and initialize it with the given object.
     * @param dotCDNStats to initialize the builder with
     * @return created builder
     */
    public static DotStatsBuilder from(DotCDNStats dotCDNStats) {
        return new DotStatsBuilder(dotCDNStats);
    }

    /**
     * Builder to build {@link DotCDNStats}.
     */
    public static final class DotStatsBuilder {

        private String cdnDomain;
        private String dateFrom;
        private String dateTo;
        private double cacheHitRate;
        private long totalBandwidthUsed;
        private long totalRequestsServed;
        private Map<String, Map<String, Long>> geographicDistribution = Collections.emptyMap();
        private Map<String, Long> bandwidthUsedChart;
        private Map<String, Long> requestsServedChart;

        private DotStatsBuilder() {}

        private DotStatsBuilder(DotCDNStats dotCDNStats) {
            this.cdnDomain = dotCDNStats.cdnDomain;
            this.dateFrom = dotCDNStats.dateFrom;
            this.dateTo = dotCDNStats.dateTo;
            this.cacheHitRate = dotCDNStats.cacheHitRate;
            this.totalBandwidthUsed = dotCDNStats.totalBandwidthUsed;
            this.totalRequestsServed = dotCDNStats.totalRequestsServed;
            this.geographicDistribution = dotCDNStats.geographicDistribution;
            this.bandwidthUsedChart = dotCDNStats.bandwidthUsedChart;
            this.requestsServedChart = dotCDNStats.requestsServedChart;
        }

        public DotStatsBuilder withCDNDomain(@Nonnull String cdnDomain) {
            this.cdnDomain = cdnDomain;
            return this;
        }

        public DotStatsBuilder withDateFrom(@Nonnull String dateFrom) {
            this.dateFrom = dateFrom;
            return this;
        }

        public DotStatsBuilder withDateTo(@Nonnull String dateTo) {
            this.dateTo = dateTo;
            return this;
        }

        public DotStatsBuilder withCacheHitRate(@Nonnull double cacheHitRate) {
            this.cacheHitRate = cacheHitRate;
            return this;
        }

        public DotStatsBuilder withTotalBandwidthUsed(@Nonnull long totalBandwidthUsed) {
            this.totalBandwidthUsed = totalBandwidthUsed;
            return this;
        }

        public DotStatsBuilder withTotalRequestsServed(@Nonnull long totalRequestsServed) {
            this.totalRequestsServed = totalRequestsServed;
            return this;
        }

        public DotStatsBuilder withGeographicDistribution(
                @Nonnull Map<String, Map<String, Long>> geographicDistribution) {
            this.geographicDistribution = geographicDistribution;
            return this;
        }

        public DotStatsBuilder withBandwidthUsedChart(@Nonnull Map<String, Long> bandWidthUsed) {
            this.bandwidthUsedChart = bandWidthUsed;
            return this;
        }

        public DotStatsBuilder withRequestsServedChart(
                @Nonnull Map<String, Long> requestsServedChart) {
            this.requestsServedChart = requestsServedChart;
            return this;
        }

        public DotCDNStats build() {
            return new DotCDNStats(this);
        }
    }

    private static final NumberFormat NF = new DecimalFormat("#.00");
    private static final int DIVIDE_BY = 1000;

    private String prettyByteify(long memory) {
        double x = memory;
        if (x > (DIVIDE_BY * DIVIDE_BY * DIVIDE_BY)) {
            return NF.format(x / (DIVIDE_BY * DIVIDE_BY * DIVIDE_BY)) + " GB";
        } else if (x > (DIVIDE_BY * DIVIDE_BY)) {
            return NF.format(x / (DIVIDE_BY * DIVIDE_BY)) + " MB";
        } else if (x > DIVIDE_BY) {
            return NF.format(x / DIVIDE_BY) + " KB";
        } else if (x > 1) {
            return NF.format(x) + " B";
        } else {
            return "0 b";
        }
    }
}
