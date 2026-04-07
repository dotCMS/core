package com.dotcms.content.index;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.elasticsearch.util.RestHighLevelClientProvider;
import com.dotcms.content.index.opensearch.OSClientProvider;
import com.dotcms.content.index.opensearch.OSIndexProperty;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Startup validator for search-engine client configuration.
 *
 * <p>Called from {@code ContentletIndexAPIImpl.checkAndInitialiazeIndex()} whenever
 * a migration phase that involves OpenSearch is active (phase ≥ 1). Fails fast
 * with a descriptive message so misconfiguration is caught before index creation
 * attempts produce cryptic {@code resource_already_exists_exception} errors.</p>
 *
 * <h2>Checks performed</h2>
 * <ol>
 *   <li><strong>OS version</strong> — the OpenSearch cluster must report a version
 *       that starts with {@code "3."}.  Connecting to an Elasticsearch node or an
 *       older OpenSearch release fails immediately.</li>
 *   <li><strong>Endpoint separation</strong> — the resolved OS endpoints must not
 *       overlap with any ES node.  Overlapping endpoints mean both clients would
 *       write to the same cluster, which causes {@code resource_already_exists}
 *       errors and silent data corruption during dual-write phases.</li>
 * </ol>
 */
public class IndexStartupValidator {

    private static final String REQUIRED_OS_MAJOR = "3.";

    private final OSClientProvider osClientProvider;

    public IndexStartupValidator(final OSClientProvider osClientProvider) {
        this.osClientProvider = osClientProvider;
    }

    /**
     * Runs all startup validations using the CDI-managed {@link OSClientProvider}.
     *
     * <p>Call this once during application startup whenever a migration phase that involves
     * OpenSearch is active. Fails fast with a descriptive {@link DotRuntimeException} so
     * misconfiguration is caught before index creation attempts produce cryptic errors.</p>
     *
     * @throws DotRuntimeException if any validation fails
     */
    public static void validateIndexingConfig() {
        new IndexStartupValidator(CDIUtils.getBeanThrows(OSClientProvider.class)).validate();
    }

    /**
     * Runs all validations. Throws {@link DotRuntimeException} on the first failure
     * so the application startup is aborted with a clear error message.
     */
    public void validate() {
        validateOSVersion();
        validateEndpointSeparation();
    }

    // -------------------------------------------------------------------------
    // OS version check
    // -------------------------------------------------------------------------

    /**
     * Connects to the configured OpenSearch cluster and asserts that its version
     * starts with {@value #REQUIRED_OS_MAJOR}.
     *
     * @throws DotRuntimeException if the cluster version does not match or the
     *                             connection fails
     */
    private void validateOSVersion() {
        try {
            final var info    = osClientProvider.getClient().info();
            final String version = info.version().number();

            if (!version.startsWith(REQUIRED_OS_MAJOR)) {
                throw new DotRuntimeException(
                        "OpenSearch version mismatch: expected " + REQUIRED_OS_MAJOR + "x"
                        + " but connected cluster reports version " + version
                        + ". Check OS_ENDPOINTS configuration.");
            }
            Logger.info(this, "OS version check passed: " + version);
        } catch (DotRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new DotRuntimeException(
                    "Could not verify OpenSearch version — connection failed: " + e.getMessage()
                    + ". Check OS_ENDPOINTS configuration.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Endpoint separation check
    // -------------------------------------------------------------------------

    /**
     * Resolves the effective ES and OS endpoint sets and asserts they do not share
     * any {@code host:port} pair.
     *
     * @throws DotRuntimeException if at least one endpoint is common to both clients
     */
    private void validateEndpointSeparation() {
        final Set<String> esEndpoints = resolveESEndpoints();
        final Set<String> osEndpoints = resolveOSEndpoints();

        final Set<String> overlap = new LinkedHashSet<>(esEndpoints);
        overlap.retainAll(osEndpoints);

        if (!overlap.isEmpty()) {
            throw new DotRuntimeException(
                    "ES and OS clients point to the same endpoint(s): " + overlap
                    + ". ES endpoints: " + esEndpoints
                    + " — OS endpoints: " + osEndpoints
                    + ". Set OS_ENDPOINTS to a separate OpenSearch instance.");
        }
        Logger.info(this, "Endpoint separation check passed."
                + " ES: " + esEndpoints + " — OS: " + osEndpoints);
    }

    /**
     * Extracts the live connected nodes from the ES RestHighLevelClient and
     * normalises each one to {@code "host:port"}.
     */
    private Set<String> resolveESEndpoints() {
        return RestHighLevelClientProvider.getInstance()
                .getClient()
                .getLowLevelClient()
                .getNodes()
                .stream()
                .map(node -> node.getHost().getHostName() + ":" + node.getHost().getPort())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Resolves OS endpoints from configuration with the same fallback chain that
     * {@link IndexConfigHelper} applies:
     * {@code OS_ENDPOINTS} → {@code OS_HOSTNAME}/{@code OS_PORT} →
     * {@code ES_HOSTNAME}/{@code ES_PORT} → {@code localhost:9200}.
     *
     * <p>Uses {@code OS_ENDPOINTS} (full URL array) when present; otherwise
     * synthesises a single entry from the individual host/port/protocol properties.</p>
     */
    private Set<String> resolveOSEndpoints() {
        final String[] rawEndpoints = Config.getStringArrayProperty("OS_ENDPOINTS", null);
        if (rawEndpoints != null && rawEndpoints.length > 0) {
            return Arrays.stream(rawEndpoints)
                    .map(IndexStartupValidator::normalizeEndpoint)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        // Fall back to individual host/port properties (mirrors ConfigurableOpenSearchProvider)
        final String host = IndexConfigHelper.getString(OSIndexProperty.HOSTNAME, "localhost");
        final int    port = IndexConfigHelper.getInt(OSIndexProperty.PORT, 9200);
        return Set.of(host + ":" + port);
    }

    /**
     * Parses a full URL (e.g. {@code "http://localhost:9201"}) and returns
     * {@code "host:port"}.  Falls back to the raw string if parsing fails.
     */
    private static String normalizeEndpoint(final String url) {
        try {
            final URI uri = URI.create(url.trim());
            final int port = uri.getPort() > 0 ? uri.getPort() : 9200;
            return uri.getHost() + ":" + port;
        } catch (Exception e) {
            return url.trim();
        }
    }
}
