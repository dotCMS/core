package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import io.vavr.control.Try;

import javax.net.ssl.SSLException;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Collects log files from all servers in a dotCMS cluster and assembles them
 * into a single ZIP archive. Uses the Java native {@link HttpClient} to call
 * each peer server's {@code _downloadLog} endpoint in parallel, authenticating
 * with a short-lived JWT token.
 *
 * @since Feb 2026
 */
public class ClusterLogCollector {

    private static final String CONFIG_TIMEOUT_SECONDS = "CLUSTER_LOG_DOWNLOAD_TIMEOUT_SECONDS";
    private static final String CONFIG_CONNECT_TIMEOUT_SECONDS = "CLUSTER_LOG_DOWNLOAD_CONNECT_TIMEOUT_SECONDS";
    private static final String CONFIG_SCHEME = "CLUSTER_LOG_DOWNLOAD_SCHEME";
    private static final String CONFIG_PORT = "CLUSTER_LOG_DOWNLOAD_PORT";

    private static final int DEFAULT_TIMEOUT_SECONDS = 300;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_HTTPS_PORT = 8443;
    private static final int MAX_PORT = 65535;
    private static final String HTTP_SCHEME = "http";
    private static final String HTTPS_SCHEME = "https";
    private static final String HTTPS_FAILURE_HINT = " (peers are reached over HTTPS on the port"
            + " given by " + CONFIG_PORT + ", 8443 by default, and must present a certificate"
            + " trusted by the JVM and valid for their address; set " + CONFIG_PORT + " to the port"
            + " each peer serves HTTPS on, or " + CONFIG_SCHEME + "=http to reach them over plain"
            + " HTTP instead)";

    private final List<Server> peerServers;
    private final String fileName;
    private final String jwt;
    private final int localPort;
    private final File localLogFile;
    private final Server localServer;

    /**
     * Creates a new collector.
     *
     * @param peerServers  alive servers excluding the local one
     * @param fileName     the log file name requested
     * @param jwt          short-lived JWT for authenticating against peers
     * @param localPort    the HTTP port of the local server, used as the peer
     *                     port over plain HTTP when
     *                     {@code CLUSTER_LOG_DOWNLOAD_PORT} is not set
     * @param localLogFile the local log file to include in the ZIP
     * @param localServer  the local server
     */
    public ClusterLogCollector(final List<Server> peerServers,
                               final String fileName,
                               final String jwt,
                               final int localPort,
                               final File localLogFile,
                               final Server localServer) {
        this.peerServers = peerServers;
        this.fileName = fileName;
        this.jwt = jwt;
        this.localPort = localPort;
        this.localLogFile = localLogFile;
        this.localServer = localServer;
    }

    /**
     * Fetches log files from all peer servers in parallel, then returns a
     * {@link StreamingOutput} that writes a ZIP containing every collected log
     * plus the local server's log.
     *
     * <p>The scheme used for peer requests is controlled by the
     * {@code CLUSTER_LOG_DOWNLOAD_SCHEME} configuration property:
     * <ul>
     *   <li>{@code "http"}: plain HTTP</li>
     *   <li>any other value, or none (default): HTTPS</li>
     * </ul>
     *
     * <p>Peer requests go to the port given by {@code CLUSTER_LOG_DOWNLOAD_PORT}
     * when it is set to a valid port; otherwise to dotCMS's default HTTPS port,
     * 8443, under HTTPS, or to the local server's own port under plain HTTP. A
     * value that is not a port from 1 to 65535 is logged, noted in
     * {@code _errors.txt}, and the default port is used.
     *
     * <p>Over HTTPS, each peer's certificate and host name are verified against
     * the JVM's default trust store, so every peer must present a certificate
     * trusted by the JVM and valid for the address it is reached at. A peer
     * request that fails, or that cannot be built from the peer's address, is
     * reported in {@code _errors.txt} with its reason and is not retried with
     * another scheme.
     *
     * @return a {@link StreamingOutput} streaming the assembled ZIP
     */
    public StreamingOutput collect() {

        final int connectTimeout = Config.getIntProperty(
                CONFIG_CONNECT_TIMEOUT_SECONDS, DEFAULT_CONNECT_TIMEOUT_SECONDS);
        final int readTimeout = Config.getIntProperty(
                CONFIG_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);
        final String scheme = resolveScheme();
        final boolean httpsScheme = HTTPS_SCHEME.equals(scheme);
        final List<String> errors = new ArrayList<>();
        final int peerPort = resolvePeerPort(scheme, localPort, errors);

        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .build();

        // Fire off all peer requests in parallel
        final Map<String, CompletableFuture<PeerResponse>> futures = new LinkedHashMap<>();

        for (final Server peer : peerServers) {
            final String host = resolvePeerHost(peer);
            if (!UtilMethods.isSet(host)) {
                Logger.warn(this, "Skipping peer server " + peer.serverId
                        + " — no host or IP address available");
                continue;
            }

            final String url = scheme + "://" + urlHost(host) + ":" + peerPort
                    + "/api/v1/maintenance/_downloadLog/" + fileName;

            futures.put(labelForServer(peer), fetchLog(httpClient, url, readTimeout, peer));
        }

        // Wait for all responses
        final Map<String, byte[]> peerLogs = new LinkedHashMap<>();

        for (final Map.Entry<String, CompletableFuture<PeerResponse>> entry : futures.entrySet()) {
            final String label = entry.getKey();
            try {
                final PeerResponse response = entry.getValue().join();
                if (response.body != null) {
                    peerLogs.put(label, response.body);
                    Logger.info(this, "Received log from " + label
                            + " (" + response.body.length + " bytes)");
                } else {
                    final String hint = httpsScheme && response.possiblyTls
                            ? HTTPS_FAILURE_HINT : "";
                    final String msg = "Server " + label + " — " + response.failure + hint;
                    Logger.warn(this, msg);
                    errors.add(msg);
                }
            } catch (Exception e) {
                final String msg = "Failed to retrieve log from " + label + ": " + e.getMessage();
                Logger.warn(this, msg, e);
                errors.add(msg);
            }
        }

        // Build the streaming ZIP response
        return outputStream -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                // Local log
                addFileToZip(zipOut, labelForLocalServer() + "_" + fileName, localLogFile);

                // Peer logs
                for (final Map.Entry<String, byte[]> entry : peerLogs.entrySet()) {
                    addBytesToZip(zipOut, entry.getKey() + "_" + fileName, entry.getValue());
                }

                // Error summary if any nodes failed
                if (!errors.isEmpty()) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("The following errors occurred while collecting cluster logs:\n\n");
                    for (final String error : errors) {
                        sb.append("- ").append(error).append("\n");
                    }
                    addBytesToZip(zipOut, "_errors.txt", sb.toString().getBytes());
                }

                zipOut.finish();
            }
        };
    }

    /**
     * Returns the scheme for peer requests: {@code http} when
     * {@code CLUSTER_LOG_DOWNLOAD_SCHEME} is {@code http} (case and surrounding
     * spaces ignored), otherwise {@code https}.
     *
     * @return {@code "http"} or {@code "https"}
     */
    private static String resolveScheme() {
        final String configured = Config.getStringProperty(CONFIG_SCHEME, HTTPS_SCHEME);
        return configured != null && HTTP_SCHEME.equalsIgnoreCase(configured.trim())
                ? HTTP_SCHEME
                : HTTPS_SCHEME;
    }

    /**
     * Returns the port peer requests go to: the {@code CLUSTER_LOG_DOWNLOAD_PORT}
     * configuration property when it is set to a port from 1 to 65535, otherwise
     * dotCMS's default HTTPS port, 8443, under HTTPS, or the local server's own
     * port under plain HTTP. A value that is set but is not such a port is logged
     * and added to {@code notes}, and the default port is returned.
     *
     * @param scheme    the scheme of the peer requests, {@code http} or {@code https}
     * @param localPort the port the local server listens on
     * @param notes     the lines written to {@code _errors.txt}; receives the note
     *                  about an invalid configured value
     * @return the peer port
     */
    static int resolvePeerPort(final String scheme, final int localPort, final List<String> notes) {
        final int defaultPort = HTTPS_SCHEME.equals(scheme) ? DEFAULT_HTTPS_PORT : localPort;
        final String configured = Config.getStringProperty(CONFIG_PORT, null);
        if (!UtilMethods.isSet(configured)) {
            return defaultPort;
        }

        final Integer port = Try.of(() -> Integer.parseInt(configured.trim())).getOrNull();
        if (port != null && port >= 1 && port <= MAX_PORT) {
            return port;
        }

        final String note = CONFIG_PORT + " is set to '" + configured
                + "', which is not a valid port; the default port " + defaultPort + " was used";
        Logger.warn(ClusterLogCollector.class, note);
        notes.add(note);
        return defaultPort;
    }

    /**
     * Attempts to fetch a log file from the given URL. The returned response holds
     * the body on success (HTTP 200); otherwise it holds a short reason: the HTTP
     * status the peer answered with, or the kind of I/O failure ({@code could not
     * connect}, {@code TLS handshake failed}, {@code connect timed out}, {@code timed
     * out}, {@code connection failed}) whose details are logged at INFO, or
     * {@code invalid peer address} when no request can be built from the URL.
     *
     * @param client      the HTTP client shared by all peer requests
     * @param url         the peer's {@code _downloadLog} URL
     * @param readTimeout the request timeout, in seconds
     * @param peer        the peer server, for logging
     * @return a future that always completes normally with the peer's response
     */
    private CompletableFuture<PeerResponse> fetchLog(final HttpClient client,
                                                      final String url,
                                                      final int readTimeout,
                                                      final Server peer) {

        Logger.info(this, "Requesting log from peer server " + peer.serverId + ": " + url);

        final HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(readTimeout))
                    .header("Authorization", "Bearer " + jwt)
                    .GET()
                    .build();
        } catch (final IllegalArgumentException e) {
            Logger.warn(this, "Cannot build the request for peer server " + peer.serverId
                    + ": " + e.getMessage());
            return CompletableFuture.completedFuture(
                    PeerResponse.failure("invalid peer address", false));
        }

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(resp -> {
                    if (resp.statusCode() == 200) {
                        return PeerResponse.success(resp.body());
                    }
                    Logger.warn(this, "Server " + peer.serverId + " returned HTTP "
                            + resp.statusCode() + " for " + url);
                    return PeerResponse.failure("HTTP " + resp.statusCode(), false);
                })
                .exceptionally(ex -> {
                    // Unwrap CompletionException to get the real cause
                    final Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (!(cause instanceof IOException)) {
                        Logger.warn(this, "Error fetching " + url + ": " + cause.getMessage(), cause);
                        return PeerResponse.failure("request failed", false);
                    }
                    Logger.info(this, "Connection to " + url + " failed: " + cause.getMessage());
                    if (cause instanceof SSLException) {
                        return PeerResponse.failure("TLS handshake failed", true);
                    }
                    if (cause instanceof HttpConnectTimeoutException) {
                        // Covers the TLS handshake too, so a peer that does not complete it ends here
                        return PeerResponse.failure("connect timed out", true);
                    }
                    if (cause instanceof HttpTimeoutException) {
                        // Raised only once the connection, TLS included, was established
                        return PeerResponse.failure("timed out", false);
                    }
                    if (cause instanceof ConnectException) {
                        // Under HTTPS the peer may serve HTTPS on another port than the one used
                        return PeerResponse.failure("could not connect", true);
                    }
                    return PeerResponse.failure("connection failed", true);
                });
    }

    /**
     * Result of one peer request: the log content, or a short reason why there is none.
     */
    private static final class PeerResponse {

        private final byte[] body;
        private final String failure;
        private final boolean possiblyTls;

        private PeerResponse(final byte[] body, final String failure, final boolean possiblyTls) {
            this.body = body;
            this.failure = failure;
            this.possiblyTls = possiblyTls;
        }

        /**
         * A successful response.
         *
         * @param body the log file content
         * @return the response
         */
        private static PeerResponse success(final byte[] body) {
            return new PeerResponse(body, null, false);
        }

        /**
         * A failed request.
         *
         * @param reason      a short reason, written to {@code _errors.txt}
         * @param possiblyTls {@code true} when the failure may come from the HTTPS setup (a
         *                    handshake error, a refused connection or a connect timeout, or
         *                    a peer that does not speak TLS on the port), so the port and
         *                    scheme setting hint applies under HTTPS
         * @return the response
         */
        private static PeerResponse failure(final String reason, final boolean possiblyTls) {
            return new PeerResponse(null, reason, possiblyTls);
        }
    }

    /**
     * Resolves the best address for a peer server. Prefers the
     * {@code ipAddress} field since hostnames may not be resolvable
     * between cluster nodes, falling back to {@code host} then {@code name}.
     */
    private String resolvePeerHost(final Server peer) {
        if (UtilMethods.isSet(peer.ipAddress)) {
            return peer.ipAddress;
        }
        if (UtilMethods.isSet(peer.host)) {
            return peer.host;
        }
        return peer.name;
    }

    /**
     * Returns the host as written in a peer URL: an IPv6 address is enclosed in
     * brackets, unless it already is; any other host is returned as it is.
     *
     * @param host the peer's address or host name
     * @return the host part of the peer URL
     */
    static String urlHost(final String host) {
        return host.contains(":") && !host.startsWith("[") ? "[" + host + "]" : host;
    }

    private String labelForServer(final Server server) {
        final String hostPart = UtilMethods.isSet(server.name)
                ? server.name
                : UtilMethods.isSet(server.host) ? server.host : "";

        if (UtilMethods.isSet(hostPart)) {
            return APILocator.getShortyAPI().shortify(server.serverId) + "_" + hostPart;
        }
        return server.serverId;
    }

    private String labelForLocalServer() {
        return labelForServer(localServer);
    }

    private void addFileToZip(final ZipOutputStream zipOut,
                              final String entryName,
                              final File file) throws IOException {
        zipOut.putNextEntry(new ZipEntry(entryName));
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.transferTo(zipOut);
        }
        zipOut.closeEntry();
    }

    private void addBytesToZip(final ZipOutputStream zipOut,
                               final String entryName,
                               final byte[] data) throws IOException {
        zipOut.putNextEntry(new ZipEntry(entryName));
        zipOut.write(data);
        zipOut.closeEntry();
    }

}
