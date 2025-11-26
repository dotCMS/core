package com.dotcms.telemetry.collectors.api;

import com.dotcms.telemetry.Metric;

import java.time.Instant;

/**
 * Represent a request to a Endpoint that we wish to track
 *
 * @see ApiMetricFactory
 */
public class ApiMetricRequest {

    private final Metric metric;
    private final Instant time;
    private final String hash;

    public ApiMetricRequest(final Builder builder) {
        this.metric = builder.metric;
        this.time = builder.time;
        this.hash = builder.hash;
    }

    public Metric getMetric() {
        return metric;
    }

    public Instant getTime() {
        return time;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public String toString() {
        return "ApiMetricRequest{" +
                "metric=" + metric +
                ", time=" + time +
                ", hash='" + hash + '\'' +
                '}';
    }

    public static class Builder {
        private Metric metric;
        private Instant time;
        private String hash;

        public Builder setMetric(final Metric metric) {
            this.metric = metric;
            return this;
        }

        public Builder setTime(final Instant time) {
            this.time = time;
            return this;
        }

        public Builder setHash(final String hash) {
            this.hash = hash;
            return this;
        }

        public ApiMetricRequest build() {
            return new ApiMetricRequest(this);
        }
    }

}
