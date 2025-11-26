package com.dotcms.telemetry.business;

import com.dotcms.telemetry.MetricsSnapshot;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Payload to be sent to the Metric Endpoint
 */
public class MetricEndpointPayload {

    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm" +
                    ":ss.S'Z'")
            .withZone(ZoneId.systemDefault());
    private final String clientName;
    private final String clientEnv;
    private final int clientVersion;
    private final String clientCategory;
    private final long schemaVersion;
    private final String insertDate;
    private final MetricsSnapshot snapshot;

    private MetricEndpointPayload(final Builder builder) {
        this.clientName = builder.clientName;
        this.clientEnv = builder.clientEnv;
        this.clientVersion = builder.clientVersion;
        this.clientCategory = builder.clientCategory;
        this.schemaVersion = builder.schemaVersion;
        this.insertDate = DATE_FORMAT.format(builder.insertDate);
        this.snapshot = builder.snapshot;
    }

    public String getClientName() {
        return clientName;
    }

    public String getClientEnv() {
        return clientEnv;
    }

    public int getClientVersion() {
        return clientVersion;
    }

    public String getClientCategory() {
        return clientCategory;
    }

    public long getSchemaVersion() {
        return schemaVersion;
    }

    public String getInsertDate() {
        return insertDate;
    }

    public MetricsSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public String toString() {
        return "MetricEndpointPayload{" +
                "clientName='" + clientName + '\'' +
                ", clientEnv='" + clientEnv + '\'' +
                ", clientVersion=" + clientVersion +
                ", clientCategory='" + clientCategory + '\'' +
                ", schemaVersion=" + schemaVersion +
                ", insertDate='" + insertDate + '\'' +
                ", snapshot=" + snapshot +
                '}';
    }

    public static class Builder {
        private String clientName;
        private String clientEnv;
        private int clientVersion;
        private String clientCategory;
        private long schemaVersion;
        private Instant insertDate;
        private MetricsSnapshot snapshot;

        public Builder clientName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public Builder clientEnv(String clientEnv) {
            this.clientEnv = clientEnv;
            return this;
        }

        public Builder clientVersion(int version) {
            this.clientVersion = version;
            return this;
        }

        public Builder clientCategory(String clientCategory) {
            this.clientCategory = clientCategory;
            return this;
        }

        public Builder schemaVersion(long schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder insertDate(Instant insertDate) {
            this.insertDate = insertDate;
            return this;
        }

        public Builder snapshot(MetricsSnapshot snapshot) {
            this.snapshot = snapshot;
            return this;
        }

        public MetricEndpointPayload build() {
            return new MetricEndpointPayload(this);
        }
    }

}
