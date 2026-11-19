package com.dotcms.content.index;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Minimal local HTTPS server for client connection tests. It presents a freshly generated
 * self-signed certificate (created with the JDK {@code keytool}) and answers every request
 * with a fixed JSON body, so a test can tell "the client completed the TLS handshake and got
 * the body" apart from "the client refused the certificate".
 */
public final class TlsTestServer implements AutoCloseable {

    private static final String STORE_PASSWORD = "changeit";

    private final HttpsServer server;
    private final Path directory;
    private final Path certificatePem;

    private TlsTestServer(final HttpsServer server, final Path directory, final Path certificatePem) {
        this.server = server;
        this.directory = directory;
        this.certificatePem = certificatePem;
    }

    /**
     * Starts a server on a free loopback port.
     *
     * @param commonName the subject common name of the generated certificate
     * @param subjectAltNames value for keytool's {@code -ext san=...}, for example
     *        {@code dns:localhost,ip:127.0.0.1}; {@code null} for a certificate without any
     * @param responseBody JSON returned for every request
     */
    public static TlsTestServer start(final String commonName, final String subjectAltNames,
            final String responseBody) throws Exception {
        return start(commonName, subjectAltNames, responseBody, false, false, true);
    }

    /**
     * Starts a server presenting a self-signed certificate that expired two days ago, to exercise
     * {@code OS_TLS_ON_EXPIRED_CERT}.
     *
     * @param commonName the subject common name of the generated certificate
     * @param subjectAltNames value for keytool's {@code -ext san=...}
     * @param responseBody JSON returned for every request
     */
    public static TlsTestServer startExpired(final String commonName, final String subjectAltNames,
            final String responseBody) throws Exception {
        return start(commonName, subjectAltNames, responseBody, false, true, true);
    }

    /**
     * Starts a server whose certificate is signed by a freshly generated CA, so the chain it
     * presents has two certificates and the leaf is not self-signed.
     *
     * @param commonName the subject common name of the server certificate
     * @param subjectAltNames value for keytool's {@code -ext san=...}; must not be {@code null}
     * @param responseBody JSON returned for every request
     */
    public static TlsTestServer startSignedByCa(final String commonName, final String subjectAltNames,
            final String responseBody) throws Exception {
        return start(commonName, subjectAltNames, responseBody, true, false, true);
    }

    /**
     * Starts a server whose certificate is signed by a freshly generated CA but which presents that
     * certificate <em>alone</em>, without the CA — a chain of one that is not self-signed. This is
     * the shape httpclient5's {@code TrustSelfSignedStrategy} used to accept.
     *
     * @param commonName the subject common name of the server certificate
     * @param subjectAltNames value for keytool's {@code -ext san=...}; must not be {@code null}
     * @param responseBody JSON returned for every request
     */
    public static TlsTestServer startSignedByCaWithoutChain(final String commonName,
            final String subjectAltNames, final String responseBody) throws Exception {
        return start(commonName, subjectAltNames, responseBody, true, false, false);
    }

    private static TlsTestServer start(final String commonName, final String subjectAltNames,
            final String responseBody, final boolean signedByCa, final boolean expired,
            final boolean sendChain) throws Exception {
        final Path dir = Files.createTempDirectory("tls-test-server");
        final Path keystore = dir.resolve("server.p12");
        final Path pem = dir.resolve("server.pem");

        final Path keytool = Path.of(System.getProperty("java.home"), "bin", "keytool");
        final String[] generate = subjectAltNames == null
                ? new String[]{keytool.toString(), "-genkeypair", "-alias", "server", "-keyalg", "RSA",
                        "-keysize", "2048", "-dname", "CN=" + commonName, "-validity", "2",
                        "-keystore", keystore.toString(), "-storetype", "PKCS12",
                        "-storepass", STORE_PASSWORD, "-keypass", STORE_PASSWORD}
                : new String[]{keytool.toString(), "-genkeypair", "-alias", "server", "-keyalg", "RSA",
                        "-keysize", "2048", "-dname", "CN=" + commonName, "-ext", "san=" + subjectAltNames,
                        "-validity", "2", "-keystore", keystore.toString(), "-storetype", "PKCS12",
                        "-storepass", STORE_PASSWORD, "-keypass", STORE_PASSWORD};
        run(expired ? withExpiredDates(generate) : generate);
        if (signedByCa) {
            signWithNewCa(keytool, dir, keystore, subjectAltNames, pem);
        } else {
            run(keytool.toString(), "-exportcert", "-rfc", "-alias", "server", "-keystore", keystore.toString(),
                    "-storepass", STORE_PASSWORD, "-file", pem.toString());
        }

        final KeyStore store = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(keystore.toFile())) {
            store.load(in, STORE_PASSWORD.toCharArray());
        }
        if (signedByCa && !sendChain) {
            keepOnlyTheLeaf(store, dir.resolve("server-signed.pem"));
        }
        final KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(store, STORE_PASSWORD.toCharArray());
        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(keys.getKeyManagers(), null, null);

        final HttpsServer https = HttpsServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        https.setHttpsConfigurator(new HttpsConfigurator(context));
        final byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
        https.createContext("/", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        https.start();
        return new TlsTestServer(https, dir, pem);
    }

    /**
     * Rewrites a keytool {@code -genkeypair} command so the certificate it produces was valid from
     * three days ago for one day, which puts its expiry two days in the past.
     *
     * @param command the original keytool invocation
     * @return the same invocation with the back-dated validity window
     */
    private static String[] withExpiredDates(final String[] command) {
        final java.util.List<String> dated = new java.util.ArrayList<>();
        for (int i = 0; i < command.length; i++) {
            if ("-validity".equals(command[i])) {
                i++;
                continue;
            }
            dated.add(command[i]);
        }
        dated.add("-startdate");
        dated.add("-3d");
        dated.add("-validity");
        dated.add("1");
        return dated.toArray(new String[0]);
    }

    /**
     * Rewrites the key entry so its certificate chain holds the server certificate only, which
     * makes the server present a single certificate it did not sign itself.
     *
     * @param store the loaded key store
     * @param signedPem the PEM of the CA-signed server certificate
     */
    private static void keepOnlyTheLeaf(final KeyStore store, final Path signedPem) throws Exception {
        final java.security.Key key = store.getKey("server", STORE_PASSWORD.toCharArray());
        final java.security.cert.Certificate leaf;
        try (java.io.InputStream in = Files.newInputStream(signedPem)) {
            leaf = java.security.cert.CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
        store.setKeyEntry("server", key, STORE_PASSWORD.toCharArray(),
                new java.security.cert.Certificate[]{leaf});
    }

    private static void signWithNewCa(final Path keytool, final Path dir, final Path keystore,
            final String subjectAltNames, final Path caPem) throws Exception {
        final Path caStore = dir.resolve("ca.p12");
        final Path request = dir.resolve("server.csr");
        final Path signed = dir.resolve("server-signed.pem");
        run(keytool.toString(), "-genkeypair", "-alias", "ca", "-keyalg", "RSA", "-keysize", "2048",
                "-dname", "CN=Test CA", "-ext", "bc:c", "-validity", "2", "-keystore", caStore.toString(),
                "-storetype", "PKCS12", "-storepass", STORE_PASSWORD, "-keypass", STORE_PASSWORD);
        run(keytool.toString(), "-exportcert", "-rfc", "-alias", "ca", "-keystore", caStore.toString(),
                "-storepass", STORE_PASSWORD, "-file", caPem.toString());
        run(keytool.toString(), "-certreq", "-alias", "server", "-keystore", keystore.toString(),
                "-storepass", STORE_PASSWORD, "-file", request.toString());
        run(keytool.toString(), "-gencert", "-alias", "ca", "-keystore", caStore.toString(),
                "-storepass", STORE_PASSWORD, "-infile", request.toString(), "-outfile", signed.toString(),
                "-rfc", "-ext", "san=" + subjectAltNames, "-validity", "2");
        run(keytool.toString(), "-importcert", "-noprompt", "-alias", "ca", "-file", caPem.toString(),
                "-keystore", keystore.toString(), "-storepass", STORE_PASSWORD);
        run(keytool.toString(), "-importcert", "-noprompt", "-alias", "server", "-file", signed.toString(),
                "-keystore", keystore.toString(), "-storepass", STORE_PASSWORD);
    }

    /** The loopback port the server listens on. */
    public int port() {
        return server.getAddress().getPort();
    }

    /** The PEM file of the CA to trust: the server certificate itself when self-signed, or the signing CA. */
    public Path certificatePem() {
        return certificatePem;
    }

    @Override
    public void close() throws IOException {
        server.stop(0);
        try (Stream<Path> files = Files.walk(directory)) {
            files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    private static void run(final String... command) throws Exception {
        final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
            throw new IllegalStateException("keytool failed: " + output);
        }
    }
}
