package com.dotcms.content.elasticsearch.util;

import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.core.JsonFactory;
import java.lang.reflect.Field;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;

/**
 * Provider used to load custom High Level Rest clients to handle API requests in Elastic
 * By default, {@link DotRestHighLevelClientProvider} is used if `ES_REST_CLIENT_PROVIDER_CLASS` is not set
 * @author nollymar
 */
public abstract class RestHighLevelClientProvider {

    private static RestHighLevelClientProvider INSTANCE;

    public static RestHighLevelClientProvider getInstance() {
        if (INSTANCE == null) {
            synchronized (RestHighLevelClientProvider.class) {
                if (INSTANCE == null) {

                    final String providerClassName = Config.getStringProperty("ES_REST_CLIENT_PROVIDER_CLASS",
                                    "com.dotcms.content.elasticsearch.util.DotRestHighLevelClientProvider");

                    disableStringIntern();

                    try {
                        INSTANCE = ((Class<RestHighLevelClientProvider>) Class.forName(providerClassName)).newInstance();
                        Logger.info(DotRestHighLevelClientProvider.class,
                                        "RestHighLevelClientProvider " + providerClassName + " loaded successfully");
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        INSTANCE = new DotRestHighLevelClientProvider();
                        Logger.error(RestHighLevelClientProvider.class,
                                        "Unable to get the class reference for the RestHighLevelClientProvider  ["
                                                        + providerClassName + "].",
                                        e);
                    }
                }
            }
        }
        
        return INSTANCE;
    }

    /**
     * This method uses reflection to disable String.intern in elasticsearch's Jackson JsonFactory
     * instance. If this method fails for any reason, it is not tragic, just not optimal. Also, this is
     * hard to test for - the only indication that any change has happened to the JsonFactory is an
     * inner flag on the JsonFactory object itself
     * 
     * @return
     */
    private static boolean disableStringIntern() {
        try {
            // init the static field
            XContentBuilder x = JsonXContent.contentBuilder();

            // get a handle on the field
            Field jsonFactoryField = JsonXContent.class.getDeclaredField("jsonFactory");
            jsonFactoryField.setAccessible(true);
            JsonFactory jsonFactory = (JsonFactory) jsonFactoryField.get(null);

            // set the value
            Logger.info(RestHighLevelClientProvider.class, "disabling String.intern() in Jackson");
            jsonFactory.configure(JsonFactory.Feature.INTERN_FIELD_NAMES, false);
        } catch (Exception e) {
            Logger.warn(RestHighLevelClientProvider.class,
                            "Unable to disable String.intern(), elasticsearch performance might suffer");
            Logger.warnAndDebug(RestHighLevelClientProvider.class, e);
            return false;
        }

        return true;

    }
    
    public abstract RestHighLevelClient getClient();

    public abstract void setClient(final RestHighLevelClient client);
}
