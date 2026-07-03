package com.dotcms.content.index.opensearch;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.content.index.IndexConfigHelper;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
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
 *       that starts with {@code "3."}.  Connecting to an Elasticsearch node, an
 *       older OpenSearch release, or an unreachable cluster all hard-fail with a
 *       {@link DotRuntimeException}.  The caller ({@code ContentletIndexAPIImpl})
 *       is expected to call {@code haltMigration()} on failure, resetting the active
 *       phase to {@code PHASE_0_MIGRATION_NOT_STARTED} so dotCMS falls back to
 *       ES-only traffic.</li>
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
     * OpenSearch is active. Returns {@code false} — and logs a FATAL message — on any failure:
     * version mismatch, endpoint overlap, or connectivity error. Returns {@code true} only when
     * all checks pass. Any failure must be resolved before dotCMS is restarted.</p>
     *
     * @return {@code true} if the configuration is valid, {@code false} if a fatal
     *         configuration error was detected and dotCMS must not continue booting
     */
    public static boolean validateIndexingConfig() {
        try {
            new IndexStartupValidator(CDIUtils.getBeanThrows(OSClientProvider.class)).validate();
            Logger.info(IndexStartupValidator.class,
                    "OpenSearch startup validation PASSED — connected to OS successfully; migration phase "
                    + IndexConfigHelper.MigrationPhase.current().name() + " is active.");
            return true;
        } catch (Exception e) {
            // Catch broadly (not only DotRuntimeException): any failure here MUST funnel through
            // this single FAILED-and-fallback path so the outcome is always logged and the caller
            // reaches haltMigration()/Phase-3 abort, instead of an unexpected exception escaping to
            // a generic FATAL that skips the ES-only fallback.
            Logger.error(IndexStartupValidator.class,
                    "OpenSearch startup validation FAILED — halting OS migration; dotCMS falls back to"
                    + " ES-only (PHASE_0_MIGRATION_NOT_STARTED): " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Runs all validations. Throws {@link DotRuntimeException} on the first failure
     * so the application startup is aborted with a clear error message.
     *
     * <p>Begins by logging an easy-to-locate banner with the resolved OpenSearch migration
     * parameters (phase, endpoints, effective authentication mode, TLS), so the configuration and
     * the success/fallback outcome appear together as a single startup story under one logger.</p>
     */
    public void validate() {
        final OSClientConfig config = resolveConfig();
        logConfigSummary(config);
        validateOSVersion();
        validateEndpointSeparation(config);
    }

    // -------------------------------------------------------------------------
    // Configuration summary banner
    // -------------------------------------------------------------------------

    /**
     * Re-resolves the OpenSearch configuration from properties through the same package-private
     * resolution path the client is built from ({@link ConfigurableOpenSearchProvider#configFromProperties()}).
     *
     * <p><strong>This is a fresh re-resolution, not the live client's stored config.</strong> It is
     * deterministic from the same properties, so at startup it matches what the client was built
     * with; it would only diverge if properties changed at runtime without the client being rebuilt.
     * The banner is therefore a faithful report of the resolved configuration, not a readback of the
     * connected client's state.</p>
     *
     * <p>A resolution failure (e.g. an invalid/conflicting auth combination) is surfaced as a
     * {@link DotRuntimeException} so it flows through the same uniform halt path as every other
     * startup failure.</p>
     */
    private OSClientConfig resolveConfig() {
        try {
            return ConfigurableOpenSearchProvider.configFromProperties();
        } catch (Exception e) {
            throw new DotRuntimeException("Invalid OpenSearch configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Emits an easy-to-locate, single-block summary of the OpenSearch migration parameters that
     * were resolved — migration phase, endpoints, the effective authentication mode, and the TLS
     * flags. Sensitive values (password, JWT token) are masked via
     * {@link StringUtils#maskSecret(String)} so the banner is safe to leave in the logs.
     *
     * <p>Two things this banner makes explicit on purpose, because their absence was confusing
     * during QA:</p>
     * <ul>
     *   <li>When no credentials are resolved (e.g. only an endpoint URL was provided) the auth mode
     *       is reported as {@code NONE — connecting ANONYMOUSLY}. dotCMS will still connect if the
     *       OpenSearch cluster does not enforce security, so this line is the signal that no
     *       credentials were applied — it is not an error by itself.</li>
     *   <li>This banner reflects the resolved configuration; reachability and the OS version are
     *       verified by the checks that follow — a printed config is not yet a confirmed
     *       connection.</li>
     * </ul>
     */
    private void logConfigSummary(final OSClientConfig config) {
        final String phase = IndexConfigHelper.MigrationPhase.current().name();

        final String authSummary;
        if (config.jwtToken().isPresent()) {
            authSummary = "JWT (token=" + StringUtils.maskSecret(config.jwtToken().get()) + ")";
        } else if (config.clientCertPath().isPresent() || config.clientKeyPath().isPresent()) {
            authSummary = "CERT (clientCert=" + config.clientCertPath().orElse("(not set)")
                    + ", clientKey=" + config.clientKeyPath().orElse("(not set)") + ")";
        } else if (config.username().isPresent() || config.password().isPresent()) {
            authSummary = "BASIC (user=" + config.username().orElse("(not set)")
                    + ", password=" + StringUtils.maskSecret(config.password().orElse(null)) + ")";
        } else {
            authSummary = "NONE — connecting ANONYMOUSLY (no username/password/token resolved)";
        }

        Logger.info(this, System.lineSeparator() + String.join(System.lineSeparator(),
                "========== OpenSearch Migration — client configuration ==========",
                "  Migration phase   : " + phase,
                "  OS endpoints      : " + config.endpoints(),
                "  Authentication    : " + authSummary,
                "  TLS enabled       : " + config.tlsEnabled(),
                "  TLS cert required : " + config.certRequired(),
                "  TLS trust selfsign: " + config.trustSelfSigned(),
                "  TLS CA cert       : " + config.caCertPath().orElse("(not set)"),
                "  (connectivity + OS version are verified by the checks that follow)",
                "================================================================="));
    }

    // -------------------------------------------------------------------------
    // OS version check
    // -------------------------------------------------------------------------

    /**
     * Connects to the configured OpenSearch cluster and asserts that its version
     * starts with {@value #REQUIRED_OS_MAJOR}.
     *
     * <p>Both a version mismatch and a connectivity failure are treated as hard errors:
     * a {@link DotRuntimeException} is thrown in either case so the caller's uniform
     * halt path ({@code haltMigration()} → {@code MigrationPhase.reset()}) handles
     * all OS failures consistently, falling back to ES-only without a JVM exit.</p>
     *
     * @throws DotRuntimeException if the cluster reports the wrong version or is unreachable
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
            throw new DotRuntimeException(
                    "OpenSearch cluster is not reachable: " + e.getMessage()
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
     * <p>The OS side comes from the already-resolved {@link OSClientConfig#endpoints()} (the very
     * endpoints the client is built with), and the ES side is read from config; both are passed
     * through the same {@link #normalizeEndpoint} path so the comparison is consistent.</p>
     *
     * <p><strong>Best-effort:</strong> two configs that refer to the same host via different forms
     * (e.g. {@code "127.0.0.1"} vs {@code "localhost"}) will not be detected as overlapping.</p>
     *
     * @throws DotRuntimeException if at least one endpoint is common to both clients
     */
    private void validateEndpointSeparation(final OSClientConfig config) {
        assertEndpointsSeparate(config);
        Logger.info(this, "Endpoint separation check passed.");
    }

    /**
     * Asserts that the ES and OS clients do not share any {@code host:port}. Shared by the
     * startup validator ({@link #validate()}) and the config-only OS index-creation gate
     * ({@link #endpointsAreSeparate()}) so both enforce separation with identical logic.
     *
     * @throws DotRuntimeException if at least one endpoint is common to both clients
     */
    static void assertEndpointsSeparate(final OSClientConfig config) {
        final Set<String> esEndpoints = resolveESEndpoints();
        final Set<String> osEndpoints = config.endpoints().stream()
                .map(IndexStartupValidator::normalizeEndpoint)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        final Set<String> overlap = new LinkedHashSet<>(esEndpoints);
        overlap.retainAll(osEndpoints);

        if (!overlap.isEmpty()) {
            throw new DotRuntimeException(
                    "ES and OS clients point to the same endpoint(s): " + overlap
                    + ". ES endpoints: " + esEndpoints
                    + " — OS endpoints: " + osEndpoints
                    + ". Set OS_ENDPOINTS to a separate OpenSearch instance.");
        }
    }

    /**
     * Config-only endpoint-separation gate for the OS index-creation chokepoint
     * ({@code ContentletIndexAPIImpl.bootstrapAndPointOS}, issue #36419). Unlike
     * {@link #validate()} this performs no network I/O — it only compares the resolved ES and OS
     * endpoints — so it is cheap enough to run before every OS bootstrap on any startup path
     * (empty-DB starter-load or populated-DB InitServlet), closing the window where the
     * starter-load path created {@code .os} indices before the late startup validation ran.
     *
     * @return {@code true} when ES and OS point to distinct clusters (safe to create OS indices);
     *         {@code false} when they overlap (caller must skip the OS bootstrap and halt the migration)
     */
    public static boolean endpointsAreSeparate() {
        final OSClientConfig config;
        try {
            config = ConfigurableOpenSearchProvider.configFromProperties();
        } catch (Exception e) {
            // Config could not be resolved — this is NOT an ES/OS overlap. Log it distinctly so the
            // operator is not sent chasing a same-endpoint misconfiguration that does not exist.
            Logger.error(IndexStartupValidator.class,
                    "Cannot resolve OpenSearch configuration for the endpoint-separation check: "
                    + e.getMessage());
            return false;
        }
        try {
            assertEndpointsSeparate(config);
            return true;
        } catch (DotRuntimeException e) {
            // The overlap message (with the actionable "Set OS_ENDPOINTS to a separate instance").
            Logger.error(IndexStartupValidator.class, e.getMessage());
            return false;
        }
    }

    /**
     * Resolves ES endpoints from configuration, passing them through the same
     * {@link #normalizeEndpoint} path used for the OS side so both can be compared consistently.
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
