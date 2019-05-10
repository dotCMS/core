package com.dotcms.rest;

import com.dotcms.publisher.util.TrustFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

/**
 * This class provides an instance of a Jersey REST Client. This client allows
 * the developer to access the different RESTful services provided by dotCMS
 * regarding interaction with contents, workflows, Rule Engine, Push Publishing,
 * Integrity Checker, among many other features.
 * 
 * @author Daniel Silva
 * @version 1.0
 * @since Jun 4, 2015
 *
 */
public class RestClientBuilder {

    /**
     * Creates a new instance of the REST client used to access the RESTful
     * services available in the dotCMS back-end.
     *
     * @return The REST {@link Client} object.
     */
    public static Client newClient() {
        TrustFactory tFactory = new TrustFactory();

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        //  clientConfig = clientConfig.property(ClientProperties.CONNECT_TIMEOUT, 10000);
        //  clientConfig = clientConfig.property(ClientProperties.READ_TIMEOUT, 1000);

        final HostnameVerifier verifier = new HostnameVerifier(){
           @Override
           public boolean verify(String paramString, SSLSession paramSSLSession) {
             System.out.println(" verify: " + paramString  +  "  "  + paramSSLSession);
             return true;
           }
         };

        Client client;
        String truststorePath = Config.getStringProperty("TRUSTSTORE_PATH", "");
        if (UtilMethods.isSet(truststorePath)) {
           /*
            client = ClientBuilder.newBuilder().withConfig(clientConfig).sslContext(tFactory.getSSLContext())
                    .hostnameVerifier(tFactory.getHostnameVerifier())
                    .build();
            */
            client  = ClientBuilder.newBuilder().sslContext(tFactory.getSSLContext()).hostnameVerifier(verifier).build();
        } else {
            client = ClientBuilder.newBuilder().withConfig(clientConfig).hostnameVerifier(verifier).build();
        }
        client.register(MultiPartFeature.class);
        return client;
    }

}
