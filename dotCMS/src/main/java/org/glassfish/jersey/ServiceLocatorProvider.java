package org.glassfish.jersey;

import org.glassfish.hk2.api.ServiceLocator;
import javax.ws.rs.ext.WriterInterceptorContext;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.core.FeatureContext;
import com.dotmarketing.util.Logger;
import java.lang.reflect.Field;

/**
 * This is a compatibility class designed to provide the same functionality
 * as org.glassfish.jersey.ServiceLocatorProvider in Jersey 2.28.
 * 
 * This class helps to extract ServiceLocator from JAX-RS components.
 * 
 * It is used as a fallback method to obtain the service locator when 
 * standard methods fail.
 */
public class ServiceLocatorProvider {
    
    private static final String PROPERTY_SERVICE_LOCATOR = "org.glassfish.jersey.serviceLocator";
    
    protected ServiceLocatorProvider() {
        // Prevent instantiation
    }
    
    /**
     * Extract and return service locator from writerInterceptorContext.
     *
     * @param writerInterceptorContext Writer interceptor context.
     * @return Service locator.
     */
    public static ServiceLocator getServiceLocator(WriterInterceptorContext writerInterceptorContext) {
        try {
            // Get the ServiceLocator via reflection from the properties
            if (writerInterceptorContext != null) {
                Field propertyField = findField(writerInterceptorContext.getClass(), "properties");
                if (propertyField != null) {
                    propertyField.setAccessible(true);
                    Object properties = propertyField.get(writerInterceptorContext);
                    
                    if (properties instanceof java.util.Map) {
                        Object serviceLocator = ((java.util.Map<?, ?>) properties).get(PROPERTY_SERVICE_LOCATOR);
                        if (serviceLocator instanceof ServiceLocator) {
                            return (ServiceLocator) serviceLocator;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.debug(ServiceLocatorProvider.class, 
                    "Error getting ServiceLocator from WriterInterceptorContext: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Extract and return service locator from readerInterceptorContext.
     *
     * @param readerInterceptorContext Reader interceptor context.
     * @return Service locator.
     */
    public static ServiceLocator getServiceLocator(ReaderInterceptorContext readerInterceptorContext) {
        try {
            // Get the ServiceLocator via reflection from the properties
            if (readerInterceptorContext != null) {
                Field propertyField = findField(readerInterceptorContext.getClass(), "properties");
                if (propertyField != null) {
                    propertyField.setAccessible(true);
                    Object properties = propertyField.get(readerInterceptorContext);
                    
                    if (properties instanceof java.util.Map) {
                        Object serviceLocator = ((java.util.Map<?, ?>) properties).get(PROPERTY_SERVICE_LOCATOR);
                        if (serviceLocator instanceof ServiceLocator) {
                            return (ServiceLocator) serviceLocator;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.debug(ServiceLocatorProvider.class, 
                    "Error getting ServiceLocator from ReaderInterceptorContext: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Extract and return service locator from featureContext.
     *
     * @param featureContext Feature context.
     * @return Service locator.
     */
    public static ServiceLocator getServiceLocator(FeatureContext featureContext) {
        try {
            // Get the ServiceLocator via reflection from the properties
            if (featureContext != null) {
                Field serviceLocatorField = findField(featureContext.getClass(), "serviceLocator");
                if (serviceLocatorField != null) {
                    serviceLocatorField.setAccessible(true);
                    Object serviceLocator = serviceLocatorField.get(featureContext);
                    
                    if (serviceLocator instanceof ServiceLocator) {
                        return (ServiceLocator) serviceLocator;
                    }
                }
                
                // Try configuration property
                Field configField = findField(featureContext.getClass(), "configuration");
                if (configField != null) {
                    configField.setAccessible(true);
                    Object config = configField.get(featureContext);
                    
                    if (config != null) {
                        Field locatorField = findField(config.getClass(), "serviceLocator");
                        if (locatorField != null) {
                            locatorField.setAccessible(true);
                            Object locator = locatorField.get(config);
                            if (locator instanceof ServiceLocator) {
                                return (ServiceLocator) locator;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.debug(ServiceLocatorProvider.class, 
                    "Error getting ServiceLocator from FeatureContext: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Find a field in the class hierarchy
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        if (clazz == null) {
            return null;
        }
        
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // Try superclass
            return findField(clazz.getSuperclass(), fieldName);
        }
    }
} 