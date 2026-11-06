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
        run(generate);
        run(keytool.toString(), "-exportcert", "-rfc", "-alias", "server", "-keystore", keystore.toString(),
                "-storepass", STORE_PASSWORD, "-file", pem.toString());

        final KeyStore store = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(keystore.toFile())) {
            store.load(in, STORE_PASSWORD.toCharArray());
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

    /** The loopback port the server listens on. */
    public int port() {
        return server.getAddress().getPort();
    }

    /** The server certificate as a PEM file. Being self-signed, it is also its own CA. */
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
