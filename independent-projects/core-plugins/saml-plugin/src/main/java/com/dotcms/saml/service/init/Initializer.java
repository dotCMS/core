package com.dotcms.saml.service.init;

import java.io.Serializable;
import java.util.Map;

/**
 * Defines a contract to initialize the plugin
 * @author jsanca
 */
public interface Initializer extends Serializable {

	
    /**
     * Init the app, the context will be any info it needs to start up
     * 
     * @param context {@link Map}
     */
    void init(Map<String, Object> context);

    
    /**
     * Determines if the initialization is done, this must be thread-safe.
     * 
     * @return boolean
     */
    boolean isInitializationDone();

} 
