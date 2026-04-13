package com.dotcms.content.index;

import com.dotcms.cdi.CDIUtils;
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
 * a migration phase that involves OpenSearch is active (phase ≥ 1).</p>
 *
 * <h2>Checks performed</h2>
 * <ol>
 *   <li><strong>OS version</strong> — the OpenSearch cluster must report a version
 *       that starts with {@code "3."}.  Connecting to an Elasticsearch node or an
 *       older OpenSearch release hard-fails with a {@link DotRuntimeException}.
 *       A transient connectivity failure (OS still starting up, network hiccup,
 *       or rollback while OS is offline) is <em>demoted to a warning</em> — dotCMS
 *       will start, but writes may fail until OS becomes reachable.</li>
 *   <li><strong>Endpoint separation</strong> — the configured OS endpoints must not
 *       overlap with the configured ES endpoints.  Both sides are resolved from
 *       config strings through the same normalisation path, making the comparison
 *       consistent.  Note that this check is <strong>best-effort</strong>: two configs
 *       that refer to the same host using different forms (e.g. {@code "127.0.0.1"}
 *       vs {@code "localhost"}) will not be detected as overlapping.</li>
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
     * <p><strong>Version mismatch</strong> (wrong OS release or accidentally pointing at an
     * ES cluster) is a configuration error and hard-fails with a {@link DotRuntimeException}.</p>
     *
     * <p><strong>Connectivity failure</strong> (OS still starting, transient network hiccup,
     * or rollback while OS is offline) is demoted to a {@code Logger.warn} so dotCMS can
     * complete startup. Writes will fail until OS is reachable, but the application will not
     * be permanently blocked.</p>
     *
     * @throws DotRuntimeException if the connected cluster reports the wrong version
     */
    private void validateOSVersion() {
        try {
            final var info       = osClientProvider.getClient().info();
            final String version = info.version().number();

            if (!version.startsWith(REQUIRED_OS_MAJOR)) {
                throw new DotRuntimeException(
                        "OpenSearch version mismatch: expected " + REQUIRED_OS_MAJOR + "x"
                        + " but connected cluster reports version " + version
                        + ". Check OS_ENDPOINTS configuration.");
            }
            Logger.info(this, "OS version check passed: " + version);
        } catch (DotRuntimeException e) {
            throw e;  // version mismatch — re-throw, this is a hard configuration error
        } catch (Exception e) {
            // Transient connectivity failure — warn but allow startup to continue.
            // OS writes will fail until the cluster is reachable; this is preferable
            // to permanently blocking startup after a rollback or slow OS restart.
            Logger.warn(this,
                    "Could not verify OpenSearch version — connection failed: " + e.getMessage()
                    + ". dotCMS will start, but OS writes may fail until the cluster is available."
                    + " Check OS_ENDPOINTS configuration.");
        }
    }

    // -------------------------------------------------------------------------
    // Endpoint separation check
    // -------------------------------------------------------------------------

    /**
     * Resolves the effective ES and OS endpoint sets and asserts they do not share
     * any {@code host:port} pair.
     *
     * <p><strong>Best-effort:</strong> both sides are resolved from config strings through
     * the same {@link #normalizeEndpoint} path, making the comparison consistent.
     * Two configs that refer to the same host via different forms (e.g. {@code "127.0.0.1"}
     * vs {@code "localhost"}) will not be detected as overlapping.</p>
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
     * Resolves ES endpoints from configuration, using the same normalisation path as
     * {@link #resolveOSEndpoints()} so both sides can be compared consistently.
     *
     * <p>Reads {@code ES_ENDPOINTS} (full URL array) when present; otherwise synthesises
     * a single entry from {@code ES_HOSTNAME} / {@code ES_PORT}, defaulting to
     * {@code localhost:9200}.</p>
     */
    private static Set<String> resolveESEndpoints() {
        final String[] rawEndpoints = Config.getStringArrayProperty("ES_ENDPOINTS", null);
        if (rawEndpoints != null && rawEndpoints.length > 0) {
            return Arrays.stream(rawEndpoints)
                    .map(IndexStartupValidator::normalizeEndpoint)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        final String host = Config.getStringProperty("ES_HOSTNAME", "localhost");
        final int    port = Config.getIntProperty("ES_PORT", 9200);
        return Set.of(host + ":" + port);
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
     * Parses a URL (e.g. {@code "http://localhost:9201"} or {@code "localhost:9201"}) and
     * returns {@code "host:port"}.  Falls back to the raw trimmed string if parsing fails.
     *
     * <p>Scheme-less strings like {@code "localhost:9201"} are treated by {@link URI} as
     * <em>opaque</em> URIs, where {@link URI#getHost()} returns {@code null}. To avoid the
     * silent {@code "null:9201"} concatenation, a dummy {@code http://} scheme is prepended
     * when no scheme is present, forcing hierarchical-URI parsing.</p>
     */
    private static String normalizeEndpoint(final String url) {
        try {
            final String trimmed = url.trim();
            // URI.create() treats scheme-less strings as opaque URIs where getHost() == null.
            // Prepend http:// to force hierarchical parsing so getHost() is always populated.
            final URI uri = URI.create(trimmed.contains("://") ? trimmed : "http://" + trimmed);
            final String host = uri.getHost();
            if (host == null) {
                return trimmed;  // safety net — should not happen after the scheme prepend
            }
            final int port = uri.getPort() > 0 ? uri.getPort() : 9200;
            return host + ":" + port;
        } catch (Exception e) {
            return url.trim();
        }
    }
}
