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
    final int averageOriginResponseTime;

    @JsonSerialize
    final Map<String, Map<String, Long>> geographicDistribution;

    @JsonSerialize
    final Map<String, Long> bandwidthUsedChart;

    @JsonSerialize
    final Map<String, Long> requestsServedChart;

    @JsonSerialize
    final Map<String, Number> cacheHitRateChart;

    @JsonSerialize
    final Map<String, Number> originResponseTimeChart;

    @JsonSerialize
    final Map<String, Number> error4xxChart;

    @JsonSerialize
    final Map<String, Number> error5xxChart;

    private DotCDNStats(DotStatsBuilder builder) {
        this.cdnDomain = builder.cdnDomain;
        this.dateFrom = builder.dateFrom.toString();
        this.dateTo = builder.dateTo.toString();
        this.cacheHitRate = builder.cacheHitRate;
        this.totalBandwidthUsed = builder.totalBandwidthUsed;
        this.totalRequestsServed = builder.totalRequestsServed;
        this.averageOriginResponseTime = builder.averageOriginResponseTime;
        this.geographicDistribution = builder.geographicDistribution;
        this.bandwidthPretty = prettyByteify(builder.totalBandwidthUsed);
        this.bandwidthUsedChart = builder.bandwidthUsedChart;
        this.requestsServedChart = builder.requestsServedChart;
        this.cacheHitRateChart = builder.cacheHitRateChart;
        this.originResponseTimeChart = builder.originResponseTimeChart;
        this.error4xxChart = builder.error4xxChart;
        this.error5xxChart = builder.error5xxChart;
    }

    public static DotStatsBuilder builder() {
        return new DotStatsBuilder();
    }

    public static DotStatsBuilder from(DotCDNStats dotCDNStats) {
        return new DotStatsBuilder(dotCDNStats);
    }

    public static final class DotStatsBuilder {

        private String cdnDomain;
        private String dateFrom;
        private String dateTo;
        private double cacheHitRate;
        private long totalBandwidthUsed;
        private long totalRequestsServed;
        private int averageOriginResponseTime;
        private Map<String, Map<String, Long>> geographicDistribution = Collections.emptyMap();
        private Map<String, Long> bandwidthUsedChart;
        private Map<String, Long> requestsServedChart;
        private Map<String, Number> cacheHitRateChart = Collections.emptyMap();
        private Map<String, Number> originResponseTimeChart = Collections.emptyMap();
        private Map<String, Number> error4xxChart = Collections.emptyMap();
        private Map<String, Number> error5xxChart = Collections.emptyMap();

        private DotStatsBuilder() {}

        private DotStatsBuilder(DotCDNStats dotCDNStats) {
            this.cdnDomain = dotCDNStats.cdnDomain;
            this.dateFrom = dotCDNStats.dateFrom;
            this.dateTo = dotCDNStats.dateTo;
            this.cacheHitRate = dotCDNStats.cacheHitRate;
            this.totalBandwidthUsed = dotCDNStats.totalBandwidthUsed;
            this.totalRequestsServed = dotCDNStats.totalRequestsServed;
            this.averageOriginResponseTime = dotCDNStats.averageOriginResponseTime;
            this.geographicDistribution = dotCDNStats.geographicDistribution;
            this.bandwidthUsedChart = dotCDNStats.bandwidthUsedChart;
            this.requestsServedChart = dotCDNStats.requestsServedChart;
            this.cacheHitRateChart = dotCDNStats.cacheHitRateChart;
            this.originResponseTimeChart = dotCDNStats.originResponseTimeChart;
            this.error4xxChart = dotCDNStats.error4xxChart;
            this.error5xxChart = dotCDNStats.error5xxChart;
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

        public DotStatsBuilder withAverageOriginResponseTime(int averageOriginResponseTime) {
            this.averageOriginResponseTime = averageOriginResponseTime;
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

        public DotStatsBuilder withCacheHitRateChart(
                @Nonnull Map<String, Number> cacheHitRateChart) {
            this.cacheHitRateChart = cacheHitRateChart;
            return this;
        }

        public DotStatsBuilder withOriginResponseTimeChart(
                @Nonnull Map<String, Number> originResponseTimeChart) {
            this.originResponseTimeChart = originResponseTimeChart;
            return this;
        }

        public DotStatsBuilder withError4xxChart(@Nonnull Map<String, Number> error4xxChart) {
            this.error4xxChart = error4xxChart;
            return this;
        }

        public DotStatsBuilder withError5xxChart(@Nonnull Map<String, Number> error5xxChart) {
            this.error5xxChart = error5xxChart;
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
