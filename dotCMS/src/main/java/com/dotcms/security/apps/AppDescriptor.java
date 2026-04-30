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
     * Material icon name used when no {@link #getIconUrl()} is set. May be {@code null}.
     */
    default String getIcon() {
        return null;
    }

    /**
     * Hex color (e.g. {@code #3b82f6}) or PrimeNG token (e.g. {@code blue}) used to tint the icon.
     * May be {@code null}.
     */
    default String getColor() {
        return null;
    }

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
