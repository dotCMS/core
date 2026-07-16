package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.cluster.bean.Server;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
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

    private static final int DEFAULT_TIMEOUT_SECONDS = 300;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    private static final String DEFAULT_SCHEME = "auto";

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
     * @param localPort    the HTTP port of the local server (assumed same on peers)
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
     *   <li>{@code "auto"} (default) — tries HTTPS first, falls back to HTTP</li>
     *   <li>{@code "https"} — HTTPS only</li>
     *   <li>{@code "http"} — HTTP only</li>
     * </ul>
     *
     * <p>When HTTPS is used, self-signed certificates are accepted because
     * cluster-internal communication is trusted.
     *
     * @return a {@link StreamingOutput} streaming the assembled ZIP
     */
    public StreamingOutput collect() {

        final int connectTimeout = Config.getIntProperty(
                CONFIG_CONNECT_TIMEOUT_SECONDS, DEFAULT_CONNECT_TIMEOUT_SECONDS);
        final int readTimeout = Config.getIntProperty(
                CONFIG_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS);
        final String scheme = Config.getStringProperty(CONFIG_SCHEME, DEFAULT_SCHEME);

        final HttpClient httpsClient = buildHttpClient(connectTimeout, true);
        final HttpClient httpClient = buildHttpClient(connectTimeout, false);

        // Fire off all peer requests in parallel
        final Map<String, CompletableFuture<byte[]>> futures = new LinkedHashMap<>();

        for (final Server peer : peerServers) {
            final String host = resolvePeerHost(peer);
            if (!UtilMethods.isSet(host)) {
                Logger.warn(this, "Skipping peer server " + peer.serverId
                        + " — no host or IP address available");
                continue;
            }

            final String basePath = host + ":" + localPort
                    + "/api/v1/maintenance/_downloadLog/" + fileName;

            final CompletableFuture<byte[]> future;

            if ("auto".equalsIgnoreCase(scheme)) {
                // Try HTTPS first, fall back to HTTP on connection failure
                future = fetchLog(httpsClient, "https://" + basePath, readTimeout, peer)
                        .thenCompose(resp -> {
                            if (resp != null) {
                                return CompletableFuture.completedFuture(resp);
                            }
                            Logger.info(this, "HTTPS failed for peer " + peer.serverId
                                    + ", falling back to HTTP");
                            return fetchLog(httpClient, "http://" + basePath, readTimeout, peer);
                        });
            } else if ("https".equalsIgnoreCase(scheme)) {
                future = fetchLog(httpsClient, "https://" + basePath, readTimeout, peer);
            } else {
                future = fetchLog(httpClient, "http://" + basePath, readTimeout, peer);
            }

            futures.put(labelForServer(peer), future);
        }

        // Wait for all responses
        final Map<String, byte[]> peerLogs = new LinkedHashMap<>();
        final List<String> errors = new ArrayList<>();

        for (final Map.Entry<String, CompletableFuture<byte[]>> entry : futures.entrySet()) {
            final String label = entry.getKey();
            try {
                final byte[] body = entry.getValue().join();
                if (body != null) {
                    peerLogs.put(label, body);
                    Logger.info(this, "Received log from " + label
                            + " (" + body.length + " bytes)");
                } else {
                    final String msg = "Server " + label + " — no successful response";
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
     * Attempts to fetch a log file from the given URL. Returns the response
     * body bytes on success (HTTP 200), or {@code null} on failure. Connection
     * errors (e.g. HTTPS not available) return null without logging an error
     * so the caller can fall back to another scheme.
     */
    private CompletableFuture<byte[]> fetchLog(final HttpClient client,
                                                final String url,
                                                final int readTimeout,
                                                final Server peer) {

        Logger.info(this, "Requesting log from peer server " + peer.serverId + ": " + url);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(readTimeout))
                .header("Authorization", "Bearer " + jwt)
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(resp -> {
                    if (resp.statusCode() == 200) {
                        return resp.body();
                    }
                    Logger.warn(this, "Server " + peer.serverId + " returned HTTP "
                            + resp.statusCode() + " for " + url);
                    return null;
                })
                .exceptionally(ex -> {
                    // Unwrap CompletionException to get the real cause
                    final Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof IOException) {
                        // Connection refused, SSL handshake failure, timeouts, etc.
                        Logger.info(this, "Connection to " + url + " failed: " + cause.getMessage());
                    } else {
                        Logger.warn(this, "Error fetching " + url + ": " + cause.getMessage(), cause);
                    }
                    return null;
                });
    }

    /**
     * Builds an {@link HttpClient} with the given connect timeout. When
     * {@code trustAllCerts} is {@code true}, the client accepts any TLS
     * certificate — this is appropriate for cluster-internal communication
     * where peers may use self-signed certificates.
     */
    private HttpClient buildHttpClient(final int connectTimeoutSeconds,
                                        final boolean trustAllCerts) {
        final HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds));

        if (trustAllCerts) {
            try {
                final SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{TRUST_ALL_MANAGER}, null);
                builder.sslContext(sslContext);

                // Java's HttpClient internally forces the endpoint identification
                // algorithm to "HTTPS", ignoring any SSLParameters we set.
                // The only way to disable hostname/SAN verification is this
                // system property. Cluster peers often use self-signed certs
                // without SAN entries for every possible container IP address.
                System.setProperty(
                        "jdk.internal.httpclient.disableHostnameVerification", "true");
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                Logger.error(this, "Failed to create trust-all SSL context, "
                        + "HTTPS with self-signed certs will not work: " + e.getMessage(), e);
            }
        }

        return builder.build();
    }

    /**
     * A trust manager that accepts all certificates. Used only for
     * cluster-internal peer communication where self-signed certs are expected.
     */
    private static final X509TrustManager TRUST_ALL_MANAGER = new X509TrustManager() {
        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
            // Trust all — cluster-internal
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType) {
            // Trust all — cluster-internal
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

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
