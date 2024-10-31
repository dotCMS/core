package com.dotcms.telemetry.business;

import com.dotcms.telemetry.MetricsSnapshot;
import com.dotcms.telemetry.util.JsonUtil;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * API for all the Metrics related operations
 */
public enum MetricsAPI {

    INSTANCE;

    final String endPointUrl = Config.getStringProperty("TELEMETRY_PERSISTENCE_ENDPOINT",
            "https://dotcms-prod-1.analytics.dotcms.cloud/m");

    final String customCategory = Config.getStringProperty("TELEMETRY_CLIENT_CATEGORY",
            null);

    final String clientNameFromConfig = Config.getStringProperty("TELEMETRY_CLIENT_NAME",
            null);

    final int clientEnvVersionFromConfig = Config.getIntProperty("TELEMETRY_CLIENT_VERSION",
            -1);

    final String clientEnvFromConfig = Config.getStringProperty("TELEMETRY_CLIENT_ENV",
            null);

    final int maxAttemptsToFail = Config.getIntProperty("TELEMETRY_MAX_ATTEMPTS_TO_FAIL", 3);
    final int tryAgainDelay = Config.getIntProperty("TELEMETRY_TRY_AGAIN_DELAY", 30);
    final int requestTimeout = Config.getIntProperty("TELEMETRY_REQUEST_TIMEOUT", 4000);

    private static int getSchemaDBVersion() throws DotDataException {
        try {
            return LocalTransaction.wrapReturn(MetricFactory.INSTANCE::getSchemaDBVersion);
        } catch (DotDataException e) {
            throw e;
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }

    }

    /**
     * Persists the given {@link MetricsSnapshot}
     *
     * @param metricsSnapshot the snapshot to persist
     */
    public void persistMetricsSnapshot(final MetricsSnapshot metricsSnapshot) throws DotDataException {
        Logger.debug(this, "Persisting the snapshot");

        final Client client = getClient();

        sendMetric(new MetricEndpointPayload.Builder()
                .clientName(client.getClientName())
                .clientEnv(client.getEnvironment())
                .clientVersion(client.getVersion())
                .clientCategory(client.getCategory())
                .schemaVersion(getSchemaDBVersion())
                .insertDate(Instant.now())
                .snapshot(metricsSnapshot)
                .build()
        );
    }

    /**
     * Use the {@link MetricFactory#getList(String)} method to execute a Query and return a
     * Collection of String
     *
     * @param sqlQuery the query to be executed
     *
     * @return a Collection of Strings with the values returned by the query
     *
     * @see MetricFactory#getList(String)
     */
    public List<String> getList(final String sqlQuery) {
        try {
            return LocalTransaction.wrapReturn(() -> MetricFactory.INSTANCE.getList(sqlQuery))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public Optional<Object> getValue(final String sqlQuery) {
        try {
            return LocalTransaction.wrapReturn(() -> MetricFactory.INSTANCE.getValue(sqlQuery));
        } catch (Exception e) {
            throw new DotRuntimeException(e);
        }
    }

    public Client getClient() throws DotDataException {

        Client client = getClientMetaDataFromHostName();

        return new Client.Builder()
                .clientName(clientNameFromConfig != null ? clientNameFromConfig :
                        client.getClientName())
                .category(customCategory != null ? customCategory : client.getCategory())
                .version(clientEnvVersionFromConfig != -1 ? clientEnvVersionFromConfig :
                        client.getVersion())
                .environment(clientEnvFromConfig != null ? clientEnvFromConfig :
                        client.getEnvironment())
                .build();
    }

    private Client getClientMetaDataFromHostName() throws DotDataException {
        final String hostname = APILocator.getServerAPI().getCurrentServer().getName();
        final String[] split = hostname.split("-");

        final Client.Builder builder = new Client.Builder();

        if (split.length < 4) {
            builder.clientName(hostname)
                    .environment(hostname)
                    .version(0);
        } else {
            final String clientName = String.join("-", Arrays.copyOfRange(split, 1,
                    split.length - 2));

            builder.clientName(clientName)
                    .environment(split[split.length - 2])
                    .version(Integer.parseInt(split[split.length - 1]));

        }

        getCategoryFromHostName(hostname).map(ClientCategory::name).ifPresent(builder::category);

        return builder.build();
    }

    private Optional<ClientCategory> getCategoryFromHostName(final String hostname) {
        if (hostname.startsWith("dotcms-corpsites")) {
            return Optional.of(ClientCategory.DOTCMS);
        } else if (hostname.startsWith("dotcms-")) {
            return Optional.of(ClientCategory.CLIENT);
        }

        return Optional.empty();
    }


    private void sendMetric(final MetricEndpointPayload metricEndpointPayload) {

        final CircuitBreakerUrl circuitBreakerUrl = CircuitBreakerUrl.builder()
                .setMethod(CircuitBreakerUrl.Method.POST)
                .setUrl(endPointUrl)
                .setHeaders(Map.of("Content-Type", "application/json"))
                .setRawData(JsonUtil.INSTANCE.getAsJson(metricEndpointPayload))
                .setFailAfter(maxAttemptsToFail)
                .setTryAgainAfterDelaySeconds(tryAgainDelay)
                .setTimeout(requestTimeout)
                .build();

        try {
            circuitBreakerUrl.doString();
            final int response = circuitBreakerUrl.response();
            if (response != 201) {
                Logger.debug(this,
                        "ERROR: Unable to save the Metric. HTTP error code: " + response);
            }
        } catch (Exception e) {
            Logger.debug(this, "ERROR: Unable to save the Metric.");
        }
    }

    private enum ClientCategory {
        DOTCMS,
        CLIENT
    }

    public static class Client {
        final String clientName;
        final String environment;
        final int version;

        final String category;

        public Client(final Builder builder) {
            this.clientName = builder.clientName;
            this.environment = builder.environment;
            this.version = builder.version;
            this.category = builder.category;
        }

        @Override
        public String toString() {
            return "Client{" +
                    "clientName='" + clientName + '\'' +
                    ", environment='" + environment + '\'' +
                    ", version=" + version +
                    ", category='" + category + '\'' +
                    '}';
        }

        public String getClientName() {
            return clientName;
        }

        public String getEnvironment() {
            return environment;
        }

        public int getVersion() {
            return version;
        }

        public String getCategory() {
            return category;
        }

        public static class Builder {
            String clientName;
            String environment;
            int version;

            String category;

            Builder clientName(final String clientName) {
                this.clientName = clientName;
                return this;
            }

            Builder environment(final String environment) {
                this.environment = environment;
                return this;
            }

            Builder version(final int version) {
                this.version = version;
                return this;
            }

            Builder category(final String category) {
                this.category = category;
                return this;
            }

            Client build() {
                return new Client(this);
            }
        }
    }

}
