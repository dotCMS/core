package com.dotcms.content.elasticsearch.util;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static javax.crypto.Cipher.DECRYPT_MODE;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import io.vavr.control.Try;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.SSLContext;
import javax.security.auth.x500.X500Principal;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.jetbrains.annotations.NotNull;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

/**
 * Default OpenSearch client provider to handle API requests in OpenSearch
 */
public class DotOpenSearchClientProvider extends OpenSearchClientProvider {

    private static final String OPENSEARCH_CONFIG_DIR = "config";
    private static final String OPENSEARCH_PATH_HOME = "opensearch.path.home";
    private static final String OPENSEARCH_PATH_HOME_DEFAULT_VALUE = "WEB-INF/opensearch";

    private static final String BASIC_AUTH_TYPE = "BASIC";
    private static final String JWT_AUTH_TYPE = "JWT";

    private OpenSearchClient client;
    private OpenSearchTransport transport;

    private static SSLContext sslContextFromPem;
    private static final String HTTPS_PROTOCOL = "https";

    DotOpenSearchClientProvider() {
        buildClient();
    }

    private void buildClient() {
        try {
            final BasicCredentialsProvider credentialsProvider = getCredentialsProvider();
            final HttpHost[] hosts = getEndpoints();

            // Create the transport using Apache HttpClient5
            ApacheHttpClient5TransportBuilder transportBuilder = ApacheHttpClient5TransportBuilder
                .builder(hosts)
                .setMapper(new JacksonJsonpMapper());

            // Configure authentication
            if (credentialsProvider.getCredentials(new AuthScope(null, -1), null) != null) {
                transportBuilder.setHttpClientConfigCallback(clientBuilder -> {
                    clientBuilder.setDefaultCredentialsProvider(credentialsProvider);

                    // Configure SSL if needed
                    if (sslContextFromPem != null) {
                        clientBuilder.setConnectionManager(
                            PoolingAsyncClientConnectionManagerBuilder.create()
                                .setTlsStrategy(ClientTlsStrategyBuilder.create()
                                    .setSslContext(sslContextFromPem)
                                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                    .build())
                                .build()
                        );
                    } else if (isSecuredConnection(hosts)) {
                        try {
                            SSLContext sslContext = SSLContexts.custom()
                                .loadTrustMaterial((chain, authType) -> true)
                                .build();

                            clientBuilder.setConnectionManager(
                                PoolingAsyncClientConnectionManagerBuilder.create()
                                    .setTlsStrategy(ClientTlsStrategyBuilder.create()
                                        .setSslContext(sslContext)
                                        .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                        .build())
                                    .build()
                            );
                        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                            Logger.error(this.getClass(),
                                    "Error setting TrustAllStrategy for OpenSearch Client", e);
                            throw new DotRuntimeException(e);
                        }
                    }

                    return clientBuilder;
                });
            }

            // Add JWT token if configured
            final String opensearchAuthType = getOpenSearchAuthType();
            if (opensearchAuthType.equals(JWT_AUTH_TYPE)) {
                String token = Config.getStringProperty("OPENSEARCH_AUTH_JWT_TOKEN", null);
                if (token != null) {
                    Header authHeader = new BasicHeader("Authorization", "Bearer " + token);
                    transportBuilder.setDefaultHeaders(new Header[]{authHeader});
                    Logger.info(DotOpenSearchClientProvider.class,
                            "Initializing OpenSearch Client using JWT authentication");
                }
            }

            transport = transportBuilder.build();
            client = new OpenSearchClient(transport);

            logConnectionInfo(hosts);

        } catch (IOException | GeneralSecurityException e) {
            Logger.error(DotOpenSearchClientProvider.class,
                    "Error creating OpenSearch Client", e);
            throw new DotRuntimeException("Failed to create OpenSearch client", e);
        }
    }

    private BasicCredentialsProvider getCredentialsProvider() throws IOException, GeneralSecurityException {
        final String opensearchAuthType = getOpenSearchAuthType();
        final boolean tlsEnabled = Config.getBooleanProperty("OPENSEARCH_TLS_ENABLED", false);

        //Loading TLS certificates
        if (tlsEnabled && HTTPS_PROTOCOL
                .equalsIgnoreCase(Config.getStringProperty("OPENSEARCH_PROTOCOL", HTTPS_PROTOCOL))) {
            loadTLSCertificates();
        } else if (!tlsEnabled){
            Logger.warn(this.getClass(),
                    "OpenSearch Client will be initialized without certificates");
        }

        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        //Loading basic credentials if set
        if (opensearchAuthType.equals(BASIC_AUTH_TYPE)) {
            String username = Config.getStringProperty("OPENSEARCH_AUTH_BASIC_USER", null);
            String password = Config.getStringProperty("OPENSEARCH_AUTH_BASIC_PASSWORD", null);

            if (username != null && password != null) {
                credentialsProvider.setCredentials(
                    new AuthScope(null, -1),
                    new UsernamePasswordCredentials(username, password.toCharArray())
                );
                Logger.info(DotOpenSearchClientProvider.class,
                        "Initializing OpenSearch Client using Basic authentication");
            }
        }

        return credentialsProvider;
    }

    private HttpHost[] getEndpoints() {
        final String[] endpoints = Config.getStringArrayProperty("OPENSEARCH_ENDPOINTS", getDefaultEndpoint());
        return Arrays.stream(endpoints)
            .map(h -> {
                URL url = Try.of(() -> new URL(h)).getOrElseThrow(e -> new DotRuntimeException(e));
                return new HttpHost(url.getProtocol(), url.getHost(), url.getPort());
            })
            .toArray(HttpHost[]::new);
    }

    private String[] getDefaultEndpoint() {
        String OPENSEARCH_HOSTNAME = Config.getStringProperty("OPENSEARCH_HOSTNAME", "localhost");
        String OPENSEARCH_PROTOCOL = Config.getStringProperty("OPENSEARCH_PROTOCOL", HTTPS_PROTOCOL);
        int OPENSEARCH_PORT = Config.getIntProperty("OPENSEARCH_PORT", 9200);

        return new String[] {OPENSEARCH_PROTOCOL + "://" + OPENSEARCH_HOSTNAME + ":" + OPENSEARCH_PORT};
    }

    private String getOpenSearchAuthType() {
        return Config.getStringProperty("OPENSEARCH_AUTH_TYPE", BASIC_AUTH_TYPE);
    }

    private boolean isSecuredConnection(HttpHost[] hosts) {
        return hosts.length > 0 && HTTPS_PROTOCOL.equalsIgnoreCase(hosts[0].getSchemeName());
    }

    private void logConnectionInfo(HttpHost[] hosts) {
        Logger.info(this.getClass(), "Initializing OpenSearch Client using endpoints [");
        for(HttpHost host : hosts) {
            Logger.info(this.getClass(), "  - " + host);
        }
        Logger.info(this.getClass(), "]");

        if (isSecuredConnection(hosts)){
            Logger.info(this.getClass(),
                    "Initializing OpenSearch Client using a secured https connection");
        } else{
            Logger.warn(this.getClass(),
                    "Initializing OpenSearch Client using an unsecured http connection");
        }
    }

    private void loadTLSCertificates() throws IOException, GeneralSecurityException {
        String clientCertPath = getCertPath("OPENSEARCH_AUTH_TLS_CLIENT_CERT", "opensearch.pem");
        String clientKeyPath = getCertPath("OPENSEARCH_AUTH_TLS_CLIENT_KEY", "opensearch.key");
        String serverCertPath = getCertPath("OPENSEARCH_AUTH_TLS_CA_CERT", "root-ca.pem");

        sslContextFromPem = SSLContexts
                .custom()
                .loadKeyMaterial(PemReader
                        .loadKeyStore(
                                Paths.get(clientCertPath).toFile(),
                                Paths.get(clientKeyPath).toFile(),
                                Optional.empty()), "".toCharArray())
                .loadTrustMaterial(PemReader.loadTrustStore(
                        Paths.get(serverCertPath).toFile()), null)
                .build();
        Logger.info(DotOpenSearchClientProvider.class,
                "Initializing OpenSearch Client using TLS certificates");
    }

    @NotNull
    private String getCertPath(final String propertyName, final String fileName) throws IOException {
        String clientCertPath = Config.getStringProperty(propertyName, null);
        if (clientCertPath == null || !Files.exists(Paths.get(clientCertPath))) {

            String assetsRealPath = Config.getStringProperty("ASSET_REAL_PATH",
                    FileUtil.getRealPath(Config.getStringProperty("ASSET_PATH", "/assets")));

            clientCertPath = assetsRealPath + File.separator + "certs" + File.separator + fileName;

            if (!Files.exists(Paths.get(clientCertPath))) {
                File directory = new File(assetsRealPath + File.separator + "certs");
                if (!directory.exists()){
                    directory.mkdirs();
                }

                Files.copy(Paths.get(getOpenSearchPathHome() + File.separator + OPENSEARCH_CONFIG_DIR
                                + File.separator + fileName), Paths.get(clientCertPath),
                        StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
        return clientCertPath;
    }

    private String getOpenSearchPathHome() {
        String opensearchPathHome = Config
                .getStringProperty(OPENSEARCH_PATH_HOME, OPENSEARCH_PATH_HOME_DEFAULT_VALUE);

        opensearchPathHome =
                !new File(opensearchPathHome).isAbsolute() ? FileUtil.getRealPath(opensearchPathHome) : opensearchPathHome;

        return opensearchPathHome;
    }

    public OpenSearchClient getClient() {
        return client;
    }

    @Override
    public void rebuildClient() {
        Logger.warn(this, "Rebuilding OpenSearch Client");
        close();
        buildClient();
    }

    public void setClient(final OpenSearchClient theClient) {
        if(theClient != null) {
            // Close previous client if exists
            close();
            client = theClient;
        }
    }

    public void close() {
        try {
            if (transport != null) {
                transport.close();
                transport = null;
            }
            client = null;
        } catch (IOException e) {
            Logger.error(this.getClass(), "Error closing OpenSearch transport", e);
        }
    }

    /*
     * Copyright 2014 The Netty Project
     *
     * The Netty Project licenses this file to you under the Apache License,
     * version 2.0 (the "License"); you may not use this file except in compliance
     * with the License. You may obtain a copy of the License at:
     *
     *   http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
     * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
     * License for the specific language governing permissions and limitations
     * under the License.
     */
    private static final class PemReader {

        private static final Pattern CERT_PATTERN = Pattern.compile(
                "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                        "([a-z0-9+/=\\r\\n]+)" +                    // Base64 text
                        "-+END\\s+.*CERTIFICATE[^-]*-+",            // Footer
                CASE_INSENSITIVE);

        private static final Pattern KEY_PATTERN = Pattern.compile(
                "-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                        "([a-z0-9+/=\\r\\n]+)" +                       // Base64 text
                        "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+",            // Footer
                CASE_INSENSITIVE);

        private PemReader() {

        }

        private static KeyStore loadTrustStore(File certificateChainFile)
                throws IOException, GeneralSecurityException {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);

            List<X509Certificate> certificateChain = readCertificateChain(certificateChainFile);
            for (X509Certificate certificate : certificateChain) {
                X500Principal principal = certificate.getSubjectX500Principal();
                keyStore.setCertificateEntry(principal.getName("RFC2253"), certificate);
            }
            return keyStore;
        }

        private static KeyStore loadKeyStore(File certificateChainFile, File privateKeyFile,
                Optional<String> keyPassword)
                throws IOException, GeneralSecurityException {
            PKCS8EncodedKeySpec encodedKeySpec = readPrivateKey(privateKeyFile, keyPassword);
            PrivateKey key;
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                key = keyFactory.generatePrivate(encodedKeySpec);
            } catch (InvalidKeySpecException ignore) {
                KeyFactory keyFactory = KeyFactory.getInstance("DSA");
                key = keyFactory.generatePrivate(encodedKeySpec);
            }

            List<X509Certificate> certificateChain = readCertificateChain(certificateChainFile);
            if (certificateChain.isEmpty()) {
                throw new CertificateException(
                        "Certificate file does not contain any certificates: "
                                + certificateChainFile);
            }

            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            keyStore.setKeyEntry("key", key, keyPassword.orElse("").toCharArray(),
                    certificateChain.stream().toArray(
                            Certificate[]::new));
            return keyStore;
        }

        private static PKCS8EncodedKeySpec readPrivateKey(File keyFile,
                Optional<String> keyPassword)
                throws IOException, GeneralSecurityException {
            String content = readFile(keyFile);

            Matcher matcher = KEY_PATTERN.matcher(content);
            if (!matcher.find()) {
                throw new KeyStoreException("found no private key: " + keyFile);
            }
            byte[] encodedKey = base64Decode(matcher.group(1));

            if (keyPassword.isEmpty()) {
                return new PKCS8EncodedKeySpec(encodedKey);
            }

            EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(
                    encodedKey);
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance(encryptedPrivateKeyInfo.getAlgName());
            SecretKey secretKey = keyFactory
                    .generateSecret(new PBEKeySpec(keyPassword.get().toCharArray()));

            Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
            cipher.init(DECRYPT_MODE, secretKey, encryptedPrivateKeyInfo.getAlgParameters());

            return encryptedPrivateKeyInfo.getKeySpec(cipher);
        }

        private static List<X509Certificate> readCertificateChain(File certificateChainFile)
                throws IOException, GeneralSecurityException {
            String contents = readFile(certificateChainFile);

            Matcher matcher = CERT_PATTERN.matcher(contents);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            List<X509Certificate> certificates = new ArrayList<>();

            int start = 0;
            while (matcher.find(start)) {
                byte[] buffer = base64Decode(matcher.group(1));
                certificates.add((X509Certificate) certificateFactory
                        .generateCertificate(new ByteArrayInputStream(buffer)));
                start = matcher.end();
            }

            return certificates;
        }

        private static byte[] base64Decode(String base64) {
            return Base64.getMimeDecoder().decode(base64.getBytes(US_ASCII));
        }

        private static String readFile(File file)
                throws IOException {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), US_ASCII)) {
                StringBuilder stringBuilder = new StringBuilder();

                CharBuffer buffer = CharBuffer.allocate(2048);
                while (reader.read(buffer) != -1) {
                    buffer.flip();
                    stringBuilder.append(buffer);
                    buffer.clear();
                }
                return stringBuilder.toString();
            }
        }
    }
}