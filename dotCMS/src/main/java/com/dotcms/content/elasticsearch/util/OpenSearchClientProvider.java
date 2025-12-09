package com.dotcms.content.elasticsearch.util;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.opensearch.client.opensearch.OpenSearchClient;

/**
 * Provider used to load custom OpenSearch clients to handle API requests in OpenSearch
 * By default, {@link DotOpenSearchClientProvider} is used if `OPENSEARCH_CLIENT_PROVIDER_CLASS` is not set
 * @author fabrizzio
 */
public abstract class OpenSearchClientProvider {

    private static OpenSearchClientProvider INSTANCE;

    public static OpenSearchClientProvider getInstance() {
        if (INSTANCE == null) {
            synchronized (OpenSearchClientProvider.class) {
                if (INSTANCE == null) {

                    final String providerClassName = Config.getStringProperty("OPENSEARCH_CLIENT_PROVIDER_CLASS",
                            "com.dotcms.content.elasticsearch.util.DotOpenSearchClientProvider");

                    try {
                        INSTANCE = ((Class<OpenSearchClientProvider>) Class.forName(providerClassName)).newInstance();
                        Logger.info(OpenSearchClientProvider.class,
                                "OpenSearchClientProvider " + providerClassName + " loaded successfully");
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        INSTANCE = new DotOpenSearchClientProvider();
                        Logger.error(OpenSearchClientProvider.class,
                                "Unable to get the class reference for the OpenSearchClientProvider ["
                                        + providerClassName + "].",
                                e);
                    }
                }
            }
        }

        return INSTANCE;
    }

    public abstract OpenSearchClient getClient();

    public void rebuildClient() {
        Logger.info(this, "Not rebuilding OpenSearch Client. To rebuild, override this method");
    }

    public abstract void setClient(final OpenSearchClient client);
}