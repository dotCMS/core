package com.dotcms.telemetry.business;

import com.dotcms.telemetry.MetricsSnapshot;
import com.dotmarketing.exception.DotDataException;

import java.util.List;
import java.util.Optional;

/**
 * API for all the Metrics related operations
 */
public interface MetricsAPI {

    enum ClientCategory {
        DOTCMS,
        CLIENT
    }

    /**
     * Saves all the information collected in the {@link MetricsSnapshot} object to a specified
     * server.
     *
     * @param metricsSnapshot The {@link MetricsSnapshot} containing the  snapshot of the metrics to
     *                        be persisted.
     *
     * @throws DotDataException An error occurred while persisting the metrics snapshot.
     */
    void persistMetricsSnapshot(final MetricsSnapshot metricsSnapshot) throws DotDataException;

    /**
     * Use the {@link MetricsFactory#getList(String)} method to execute a Query and return a
     * Collection of String
     *
     * @param sqlQuery the query to be executed
     *
     * @return a Collection of Strings with the values returned by the query
     *
     * @see MetricsFactory#getList(String)
     */
    List<String> getList(final String sqlQuery);

    Optional<Object> getValue(final String sqlQuery);

    Client getClient() throws DotDataException;

    class Client {

        final String clientName;
        final String environment;
        final int version;
        final String category;

        public Client(final Client.Builder builder) {
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

            Client.Builder clientName(final String clientName) {
                this.clientName = clientName;
                return this;
            }

            Client.Builder environment(final String environment) {
                this.environment = environment;
                return this;
            }

            Client.Builder version(final int version) {
                this.version = version;
                return this;
            }

            Client.Builder category(final String category) {
                this.category = category;
                return this;
            }

            Client build() {
                return new Client(this);
            }

        }

    }

}
