package com.dotcms.vanityurl.handler;

import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;

/**
 * This class get the handler to manage the Vanity URLs
 *
 * @version 4.2.0
 * @autor oswaldogallango
 * @since June 16, 2017
 */
public class VanityUrlHandlerResolver {

    public static final String DOTCMS_VANITY_URL_HANDLER_RESOLVER_CLASSNAME = "dotcms.vanityurl.url.handler.resolver.classname";
    private static final VanityUrlHandler defaultVanityUrlHandler = new DefaultVanityUrlHandler();

    private static VanityUrlHandlerResolver instance;

    protected VanityUrlHandlerResolver() {

    }

    /**
     * Get the instance of the VanityUrlHandlerResolver
     *
     * @return The VanityUrlHandlerResolver
     */
    public static synchronized VanityUrlHandlerResolver getInstance() {
        if (instance == null) {
            String classname = Config
                    .getStringProperty(DOTCMS_VANITY_URL_HANDLER_RESOLVER_CLASSNAME, null);
            if (UtilMethods.isSet(classname)) {
                instance = (VanityUrlHandlerResolver) ReflectionUtils.newInstance(classname);
            } else {
                instance = new VanityUrlHandlerResolver();
            }
        }
        return instance;
    }

    /**
     * Get the VanityUrlHandler implemented for this resolver
     *
     * @return The VanityUrlHandler
     */
    public VanityUrlHandler getVanityUrlHandler() {
        return defaultVanityUrlHandler;
    }
}
