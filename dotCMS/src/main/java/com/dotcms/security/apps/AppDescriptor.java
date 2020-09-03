package com.dotcms.security.apps;

import java.util.Map;

/**
 * This is a compilation of the app info that needs to be exposed and used by the endpoints.
 */
public interface AppDescriptor {

    /**
     * Service unique identifier
     * @return
     */
    String getKey();

    /**
     * Any name
     * @return
     */
    String getName();

    /**
     * Any meaningful read
     * @return
     */
    String getDescription();

    /**
     * an avatar URL
     * @return
     */
    String getIconUrl();

    /**
     * Tells the API if we allow any additional beside the ones already defined in the params map.
     * @return
     */
    boolean isAllowExtraParameters();

    /**
     * Tells the API if we allow any additional beside the ones already defined in the params map.
     * @return
     */
    Boolean getAllowExtraParameters();

    /**
     * Holds the definition of the params expected by the service.
     * This method returns a defensive copy.
     * @return
     */
    Map<String, ParamDescriptor> getParams();

}
