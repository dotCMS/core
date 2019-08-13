package com.dotcms.content.elasticsearch.util;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * Provider used to load custom High Level Rest clients to handle API requests in Elastic
 * By default, {@link DotRestHighLevelClientProvider} is used if `ES_REST_CLIENT_PROVIDER_CLASS` is not set
 * @author nollymar
 */
public abstract class RestHighLevelClientProvider {

    private static RestHighLevelClientProvider INSTANCE;

    public static RestHighLevelClientProvider getInstance() {
        if (INSTANCE == null) {
            final String providerClassName = Config
                    .getStringProperty("ES_REST_CLIENT_PROVIDER_CLASS",
                            "com.dotcms.content.elasticsearch.util.DotRestHighLevelClientProvider");
            try {
                INSTANCE = ((Class<RestHighLevelClientProvider>) Class.forName(providerClassName))
                        .newInstance();
                Logger.info(DotRestHighLevelClientProvider.class,
                        "RestHighLevelClientProvider " + providerClassName + " loaded successfully");
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                INSTANCE = new DotRestHighLevelClientProvider();
                Logger.error(RestHighLevelClientProvider.class,
                        "Unable to get the class reference for the RestHighLevelClientProvider  ["
                                + providerClassName + "].", e);
            }
        }

        return INSTANCE;
    }

    public abstract RestHighLevelClient getClient();

    public abstract void setClient(final RestHighLevelClient client);
}
