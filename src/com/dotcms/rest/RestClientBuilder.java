package com.dotcms.rest;

import com.dotcms.publisher.util.TrustFactory;
import com.dotcms.repackage.javax.ws.rs.client.Client;
import com.dotcms.repackage.javax.ws.rs.client.ClientBuilder;
import com.dotmarketing.util.Config;

public class RestClientBuilder {

    public static Client newClient() {
        TrustFactory tFactory = new TrustFactory();

        Client client;

        if(Config.getStringProperty("TRUSTSTORE_PATH") != null && !Config.getStringProperty("TRUSTSTORE_PATH").trim().equals("")) {
            client = ClientBuilder.newBuilder().sslContext(tFactory.getSSLContext())
                    .hostnameVerifier(tFactory.getHostnameVerifier())
                    .build();
        } else {
            client = ClientBuilder.newClient();
        }

        return client;
    }
}
