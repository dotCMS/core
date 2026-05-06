package com.dotcms.cdn.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;

@JsonDeserialize(builder = DotCDNStats.DotStatsBuilder.class)
public class DotCDNStats {

    private final String cdnDomain;
    private final String dateFrom;
    private final String dateTo;
    private final double cacheHitRate;
    private final long totalBandwidthUsed;
    private final long totalRequestsServed;
    private final String bandwidthPretty;
    private final int averageOriginResponseTime;
    private final Map<String, Map<String, Long>> geographicDistribution;
    private final Map<String, Long> bandwidthUsedChart;
    private final Map<String, Long> requestsServedChart;
    private final Map<String, Number> cacheHitRateChart;
    private final Map<String, Number> originResponseTimeChart;
    private final Map<String, Number> error4xxChart;
    private final Map<String, Number> error5xxChart;

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

    public static DotStatsBuilder from(final DotCDNStats dotCDNStats) {
        return new DotStatsBuilder(dotCDNStats);
    }

    public String getCdnDomain() {
        return cdnDomain;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public double getCacheHitRate() {
        return cacheHitRate;
    }

    public long getTotalBandwidthUsed() {
        return totalBandwidthUsed;
    }

    public long getTotalRequestsServed() {
        return totalRequestsServed;
    }

    public String getBandwidthPretty() {
        return bandwidthPretty;
    }

    public int getAverageOriginResponseTime() {
        return averageOriginResponseTime;
    }

    public Map<String, Map<String, Long>> getGeographicDistribution() {
        return geographicDistribution;
    }

    public Map<String, Long> getBandwidthUsedChart() {
        return bandwidthUsedChart;
    }

    public Map<String, Long> getRequestsServedChart() {
        return requestsServedChart;
    }

    public Map<String, Number> getCacheHitRateChart() {
        return cacheHitRateChart;
    }

    public Map<String, Number> getOriginResponseTimeChart() {
        return originResponseTimeChart;
    }

    public Map<String, Number> getError4xxChart() {
        return error4xxChart;
    }

    public Map<String, Number> getError5xxChart() {
        return error5xxChart;
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

        private DotStatsBuilder(final DotCDNStats dotCDNStats) {
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

        public DotStatsBuilder withCDNDomain(@Nonnull final String cdnDomain) {
            this.cdnDomain = cdnDomain;
            return this;
        }

        public DotStatsBuilder withDateFrom(@Nonnull final String dateFrom) {
            this.dateFrom = dateFrom;
            return this;
        }

        public DotStatsBuilder withDateTo(@Nonnull final String dateTo) {
            this.dateTo = dateTo;
            return this;
        }

        public DotStatsBuilder withCacheHitRate(final double cacheHitRate) {
            this.cacheHitRate = cacheHitRate;
            return this;
        }

        public DotStatsBuilder withTotalBandwidthUsed(final long totalBandwidthUsed) {
            this.totalBandwidthUsed = totalBandwidthUsed;
            return this;
        }

        public DotStatsBuilder withTotalRequestsServed(final long totalRequestsServed) {
            this.totalRequestsServed = totalRequestsServed;
            return this;
        }

        public DotStatsBuilder withAverageOriginResponseTime(final int averageOriginResponseTime) {
            this.averageOriginResponseTime = averageOriginResponseTime;
            return this;
        }

        public DotStatsBuilder withGeographicDistribution(
                @Nonnull final Map<String, Map<String, Long>> geographicDistribution) {
            this.geographicDistribution = geographicDistribution;
            return this;
        }

        public DotStatsBuilder withBandwidthUsedChart(
                @Nonnull final Map<String, Long> bandWidthUsed) {
            this.bandwidthUsedChart = bandWidthUsed;
            return this;
        }

        public DotStatsBuilder withRequestsServedChart(
                @Nonnull final Map<String, Long> requestsServedChart) {
            this.requestsServedChart = requestsServedChart;
            return this;
        }

        public DotStatsBuilder withCacheHitRateChart(
                @Nonnull final Map<String, Number> cacheHitRateChart) {
            this.cacheHitRateChart = cacheHitRateChart;
            return this;
        }

        public DotStatsBuilder withOriginResponseTimeChart(
                @Nonnull final Map<String, Number> originResponseTimeChart) {
            this.originResponseTimeChart = originResponseTimeChart;
            return this;
        }

        public DotStatsBuilder withError4xxChart(
                @Nonnull final Map<String, Number> error4xxChart) {
            this.error4xxChart = error4xxChart;
            return this;
        }

        public DotStatsBuilder withError5xxChart(
                @Nonnull final Map<String, Number> error5xxChart) {
            this.error5xxChart = error5xxChart;
            return this;
        }

        public DotCDNStats build() {
            return new DotCDNStats(this);
        }
    }

    private static final int BYTES_PER_DECIMAL_UNIT = 1000;

    private String prettyByteify(final long memory) {
        final NumberFormat numberFormat = new DecimalFormat("#.00");
        final double x = memory;
        if (x > (BYTES_PER_DECIMAL_UNIT * BYTES_PER_DECIMAL_UNIT * BYTES_PER_DECIMAL_UNIT)) {
            return numberFormat.format(x / (BYTES_PER_DECIMAL_UNIT * BYTES_PER_DECIMAL_UNIT
                    * BYTES_PER_DECIMAL_UNIT)) + " GB";
        } else if (x > (BYTES_PER_DECIMAL_UNIT * BYTES_PER_DECIMAL_UNIT)) {
            return numberFormat.format(x / (BYTES_PER_DECIMAL_UNIT * BYTES_PER_DECIMAL_UNIT))
                    + " MB";
        } else if (x > BYTES_PER_DECIMAL_UNIT) {
            return numberFormat.format(x / BYTES_PER_DECIMAL_UNIT) + " KB";
        } else if (x > 1) {
            return numberFormat.format(x) + " B";
        } else {
            return "0 b";
        }
    }
}
