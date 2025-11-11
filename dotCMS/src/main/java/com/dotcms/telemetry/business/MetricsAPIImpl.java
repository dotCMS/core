package com.dotcms.telemetry.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.telemetry.MetricsSnapshot;
import com.dotcms.telemetry.util.JsonUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This class provides the default implementation of the {@link MetricsAPI} interface.
 */
@ApplicationScoped
public class MetricsAPIImpl implements MetricsAPI {

    private final Lazy<String> endPointUrl = Lazy.of(() -> Config.getStringProperty(
            "TELEMETRY_PERSISTENCE_ENDPOINT", null));

    private final Lazy<String> customCategory = Lazy.of(() -> Config.getStringProperty(
            "TELEMETRY_CLIENT_CATEGORY", null));

    private final Lazy<String> clientNameFromConfig = Lazy.of(() -> Config.getStringProperty(
            "TELEMETRY_CLIENT_NAME", null));

    private final Lazy<Integer> clientEnvVersionFromConfig = Lazy.of(() -> Config.getIntProperty(
            "TELEMETRY_CLIENT_VERSION", -1));

    private final Lazy<String> clientEnvFromConfig = Lazy.of(() -> Config.getStringProperty(
            "TELEMETRY_CLIENT_ENV", null));

    private final Lazy<Integer> maxAttemptsToFail = Lazy.of(() -> Config.getIntProperty(
            "TELEMETRY_MAX_ATTEMPTS_TO_FAIL", 3));
    private final Lazy<Integer> tryAgainDelay = Lazy.of(() -> Config.getIntProperty(
            "TELEMETRY_TRY_AGAIN_DELAY", 30));
    private final Lazy<Integer> requestTimeout = Lazy.of(() -> Config.getIntProperty(
            "TELEMETRY_REQUEST_TIMEOUT", 4000));

    private final MetricsFactory metricsFactory;

    @Inject
    public MetricsAPIImpl(final MetricsFactory metricsFactory) {
        this.metricsFactory = metricsFactory;
    }

    @WrapInTransaction
    private int getSchemaDBVersion() throws DotDataException {
        try {
            return metricsFactory.getSchemaDBVersion();
        } catch (final DotDataException e) {
            Logger.debug(this, "Error getting the Metrics schema version from the database", e);
            throw e;
        } catch (final Exception e) {
            Logger.debug(this, "Generic error getting the Metrics schema version", e);
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public void persistMetricsSnapshot(final MetricsSnapshot metricsSnapshot) throws DotDataException {
        Logger.debug(this, "Persisting the snapshot");
        final Client client = getClient();
        Logger.debug(this, "Client Info: " + client.toString());
        Logger.debug(this, "MetricsSnapshot Info: " + metricsSnapshot.toString());
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

    @Override
    @CloseDBIfOpened
    public List<String> getList(final String sqlQuery) {
        try {
            return metricsFactory.getList(sqlQuery).orElse(Collections.emptyList());
        } catch (final Exception e) {
            Logger.debug(this, "Error getting the Metrics list from the database", e);
            throw new DotRuntimeException(e);
        }
    }

    @Override
    @CloseDBIfOpened
    public Optional<Object> getValue(final String sqlQuery) {
        try {
            return metricsFactory.getValue(sqlQuery);
        } catch (final Exception e) {
            Logger.debug(this, "Error getting the Metrics value from the database", e);
            throw new DotRuntimeException(e);
        }
    }

    @Override
    public Client getClient() throws DotDataException {
        final Client client = this.getClientMetaDataFromHostName();
        return new Client.Builder()
                .clientName(clientNameFromConfig.get() != null ? clientNameFromConfig.get() :
                        client.getClientName())
                .category(customCategory.get() != null ? customCategory.get() : client.getCategory())
                .version(clientEnvVersionFromConfig.get() != -1 ? clientEnvVersionFromConfig.get() :
                        client.getVersion())
                .environment(clientEnvFromConfig.get() != null ? clientEnvFromConfig.get() :
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

        this.getCategoryFromHostName(hostname).map(ClientCategory::name).ifPresent(builder::category);
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
        Logger.debug(this, "Sending the metric");
        Logger.debug(this, "MetricEndpointPayload: " + metricEndpointPayload.toString());
        final CircuitBreakerUrl circuitBreakerUrl = CircuitBreakerUrl.builder()
                .setMethod(CircuitBreakerUrl.Method.POST)
                .setUrl(endPointUrl.get())
                .setHeaders(Map.of("Content-Type", "application/json"))
                .setRawData(JsonUtil.INSTANCE.getAsJson(metricEndpointPayload))
                .setFailAfter(maxAttemptsToFail.get())
                .setTryAgainAfterDelaySeconds(tryAgainDelay.get())
                .setTimeout(requestTimeout.get())
                .build();
        try {
            circuitBreakerUrl.doString();
            final int response = circuitBreakerUrl.response();
            if (response != HttpServletResponse.SC_CREATED) {
                Logger.debug(this, "ERROR: Unable to send the Metric. HTTP error code: " + response);
            }
        } catch (final Exception e) {
            Logger.debug(this, "Failed to send the Metric to Telemetry persistence endpoint", e);
        }
    }

}
