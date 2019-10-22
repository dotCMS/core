package com.dotcms.content.elasticsearch.util;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static javax.crypto.Cipher.DECRYPT_MODE;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.util.FileUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
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
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.jetbrains.annotations.NotNull;

/**
 * Default high level client to handle API requests in Elastic
 */
public class DotRestHighLevelClientProvider extends RestHighLevelClientProvider {

    private static final String ES_CONFIG_DIR = "config";
    private static final String ES_PATH_HOME = "es.path.home";
    private static final String ES_PATH_HOME_DEFAULT_VALUE = "WEB-INF/elasticsearch";

    private static final String BASIC_AUTH_TYPE = "BASIC";

    private static final String JWT_AUTH_TYPE = "JWT";

    private RestHighLevelClient client;

    private static SSLContext sslContextFromPem;

    DotRestHighLevelClientProvider() {
        try {
            final RestClientBuilder clientBuilder = getClientBuilder(getCredentialsProvider());
            client = new RestHighLevelClient(clientBuilder);
        } catch (IOException | GeneralSecurityException e) {
            Logger.error(DotRestHighLevelClientProvider.class,
                    "Error setting credentials for Elastic RestHighLevel Client", e);
        }
    }

    private BasicCredentialsProvider getCredentialsProvider() throws IOException, GeneralSecurityException {
        final String esAuthType = getESAuthType();

        //Loading TLS certificates
        if (Config.getBooleanProperty("ES_TLS_ENABLED", true)) {
            loadTLSCertificates();
        }

        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        //Loading basic credentials if set
        if (esAuthType.equals(BASIC_AUTH_TYPE)) {
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(
                            Config.getStringProperty("ES_AUTH_BASIC_USER", null),
                            Config.getStringProperty("ES_AUTH_BASIC_PASSWORD", null)));
            Logger.info(DotRestHighLevelClientProvider.class,
                    "Initializing Elastic RestHighLevelClient using Basic authentication");
        }

        return credentialsProvider;
    }

    private RestClientBuilder getClientBuilder(final BasicCredentialsProvider credentialsProvider) {
        final String esAuthType = getESAuthType();

        final RestClientBuilder clientBuilder = RestClient
                .builder(new HttpHost(Config.getStringProperty("ES_HOSTNAME", "127.0.0.1"),
                        Config.getIntProperty("ES_PORT", 9200), "https"))
                .setHttpClientConfigCallback((httpClientBuilder) -> {
                    if (sslContextFromPem != null) {
                        httpClientBuilder
                                .setSSLContext(sslContextFromPem);
                    }
                    httpClientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider);
                    return httpClientBuilder;
                })
                .setFailureListener(new RestClient.FailureListener() {
                    public void onFailure(Node node) {
                        Logger.error(this, node.toString());
                    }
                });

        //Loading token if JWT authentication is set
        if (esAuthType.equals(JWT_AUTH_TYPE)) {
            clientBuilder.setDefaultHeaders(new Header[]{new BasicHeader("Authorization",
                    "Bearer " + Config.getStringProperty("ES_AUTH_JWT_TOKEN", null))});
            Logger.info(DotRestHighLevelClientProvider.class,
                    "Initializing Elastic RestHighLevelClient using JWT authentication");
        }

        return clientBuilder;
    }

    private String getESAuthType() {
        return Config.getStringProperty("ES_AUTH_TYPE", BASIC_AUTH_TYPE);
    }

    private void loadTLSCertificates() throws IOException, GeneralSecurityException {
        String clientCertPath = getCertPath("ES_AUTH_TLS_CLIENT_CERT", "esnode.pem");
        String clientKeyPath = getCertPath("ES_AUTH_TLS_CLIENT_KEY", "esnode-key.pem");
        String serverCertPath = getCertPath("ES_AUTH_TLS_CA_CERT", "root-ca.pem");

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
        Logger.info(DotRestHighLevelClientProvider.class,
                "Initializing Elastic RestHighLevelClient using TLS certificates");
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

                Files.copy(Paths.get(getESPathHome() + File.separator + ES_CONFIG_DIR
                                + File.separator + fileName), Paths.get(clientCertPath),
                        StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
        return clientCertPath;
    }



    private String getESPathHome() {
        String esPathHome = Config
                .getStringProperty(ES_PATH_HOME, ES_PATH_HOME_DEFAULT_VALUE);

        esPathHome =
                !new File(esPathHome).isAbsolute() ? FileUtil.getRealPath(esPathHome) : esPathHome;

        return esPathHome;
    }

    public RestHighLevelClient getClient() {
        return client;
    }

    public void setClient(final RestHighLevelClient theClient) {
        if(theClient!=null) {
            client = theClient;
        }
    }

    public void close() throws IOException {
        if(client!=null) {
            client.close();
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

            if (!keyPassword.isPresent()) {
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