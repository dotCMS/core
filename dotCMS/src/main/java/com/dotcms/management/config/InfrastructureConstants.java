package com.dotcms.management.config;

/**
 * Core infrastructure constants for the management filter and port validation.
 * 
 * This class contains ONLY the minimal information needed by the generic 
 * InfrastructureManagementFilter. It should NOT contain specific service 
 * endpoint details to maintain proper decoupling.
 * 
 * Specific services (like HealthProbeServlet) should define their own 
 * endpoint constants while sharing the common MANAGEMENT_PATH_PREFIX.
 */
public final class InfrastructureConstants {
    
    private InfrastructureConstants() {
        throw new AssertionError("InfrastructureConstants should not be instantiated");
    }
    
    /**
     * Base path prefix for ALL management endpoints.
     * 
     * This is the ONLY path knowledge the management filter should have.
     * Specific services define their own endpoints under this prefix.
     */
    public static final String MANAGEMENT_PATH_PREFIX = "/dotmgt";
    
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