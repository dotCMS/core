package com.dotcms.rest.config;

/**
 * Utility class that allows registering and unregistering REST resources.
 * 
 * <p>When plugins are undeployed, the system will automatically clean up all 
 * resources using the DotBundleListener in conjunction with 
 * DirectJerseyInteraction to ensure proper cleanup of Jersey resources.</p>
 * 
 * <p>The automatic cleanup happens during OSGi bundle stopping and uninstalling events,
 * no manual cleanup is needed in most cases.</p>
 */
public class RestServiceUtil {

     /**
      * Registers a REST resource with Jersey.
      * 
      * @param clazz The REST resource class to register
      */
     public static void addResource(Class clazz) {
         DotRestApplication.addClass(clazz);
     }

     /**
      * Unregisters a REST resource from Jersey.
      * This method is called automatically when an OSGi bundle is undeployed.
      * 
      * @param clazz The REST resource class to unregister
      */
     public static void removeResource(Class clazz) {
         DotRestApplication.removeClass(clazz);
     }
}
