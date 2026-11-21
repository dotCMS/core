package com.dotcms.management.config;

/**
 * Shared routing and port-validation constants for management endpoints.
 * Services derive paths used by the management filter from these constants so routing and
 * connector checks cannot drift. Service implementations and responses remain separate.
 */
public final class InfrastructureConstants {
    
    private InfrastructureConstants() {
        throw new AssertionError("InfrastructureConstants should not be instantiated");
    }
    
    /**
     * Base path prefix for all management endpoints.
     */
    public static final String MANAGEMENT_PATH_PREFIX = "/dotmgt";

    /** Metrics path suffix, also exposed by the service's endpoint constants. */
    public static final String METRICS_SUFFIX = "/metrics";

    /** Shared metrics route used by both forwarding and connector validation. */
    public static final String METRICS_PATH = MANAGEMENT_PATH_PREFIX + METRICS_SUFFIX;
    
         /**
      * Port configuration for management access validation.
      * 
      * IMPORTANT: These values must match server.xml configuration:
      * <Connector port="${CMS_MANAGEMENT_PORT:-8090}" ... />
      */
     public static final class Ports {
         private Ports() {}
         
         /** Default management port - must match server.xml default */
         public static final int DEFAULT_MANAGEMENT_PORT = 8090;
         public static final int DEFAULT_APPLICATION_PORT = 8080;
         
         /** Environment variable name used by server.xml - do not change */
         public static final String MANAGEMENT_PORT_PROPERTY = "CMS_MANAGEMENT_PORT";
         
         /** Config property for disabling strict port checking (development/testing) */
         public static final String STRICT_CHECK_PROPERTY = "management.port.strict.check.enabled";
     }
    
    /**
     * HTTP headers used for proxy detection.
     */
    public static final class Headers {
        private Headers() {}
        
        public static final String X_FORWARDED_PORT = "X-Forwarded-Port";
        public static final String X_ORIGINAL_PORT = "X-Original-Port";
    }
    
} 