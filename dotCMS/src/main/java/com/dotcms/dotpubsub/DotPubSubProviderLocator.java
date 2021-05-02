package com.dotcms.dotpubsub;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import io.vavr.Lazy;
import io.vavr.control.Try;

public class DotPubSubProviderLocator {


    public final static String DEFAULT_DOT_PUBSUB_PROVIDER="DEFAULT_DOT_PUBSUB_PROVIDER";
    /**
     * Default provider is postgres, can be overriden by setting config: DEFAULT_DOT_PUBSUB_PROVIDER
     */
    public static Lazy<DotPubSubProvider> provider = Lazy.of(() -> {
        
        
        final String clazz = System.getProperty(DEFAULT_DOT_PUBSUB_PROVIDER)!=null
                    ? System.getProperty(DEFAULT_DOT_PUBSUB_PROVIDER)
                    : Config.getStringProperty("DEFAULT_DOT_PUBSUB_PROVIDER", PostgresPubSubImpl.class.getCanonicalName());
        
        

        DotPubSubProvider provider= (DotPubSubProvider) Try.of(()->Class.forName(clazz).newInstance()).getOrElseThrow(e->new DotRuntimeException(e));

        
        return new QueuingPubSubWrapper(provider);
        
        
    });
   


}
