package com.dotcms.dotpubsub;

import com.dotmarketing.util.Config;
import io.vavr.Lazy;
import io.vavr.control.Try;

public class DotPubSubProviderLocator {

    
    
    
    
    
    
    /**
     * Default provider is postgres, can be overriden by setting:
     * DEFAULT_DOT_PUBSUB_PROVIDER
     */
    public static Lazy<DotPubSubProvider> provider = Lazy.of(()->Try.of(()->(DotPubSubProvider) Class.forName(Config.getStringProperty("DEFAULT_DOT_PUBSUB_PROVIDER", null)).newInstance()).getOrElse(new PostgresPubSubImpl()));
    

    
}
