package org.glassfish.jersey.client;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.ServiceLocatorProvider;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import com.dotmarketing.util.Logger;
import java.lang.reflect.Field;

/**
 * Extension of ServiceLocatorProvider which contains helper static methods that extract
 * ServiceLocator from client specific JAX-RS components.
 * 
 * This is a compatibility class designed to provide the same functionality
 * as org.glassfish.jersey.client.ServiceLocatorClientProvider in Jersey 2.28.
 */
public class ServiceLocatorClientProvider extends ServiceLocatorProvider {
    
    private static final String PROPERTY_SERVICE_LOCATOR = "org.glassfish.jersey.serviceLocator";
    
    /**
     * Extract and return service locator from clientRequestContext.
     *
     * @param clientRequestContext Client request context.
     * @return Service locator.
     */
    public static ServiceLocator getServiceLocator(ClientRequestContext clientRequestContext) {
        try {
            // Get the ServiceLocator via reflection from properties
            if (clientRequestContext != null) {
                // Try to get it from the properties
                Object serviceLocator = clientRequestContext.getProperty(PROPERTY_SERVICE_LOCATOR);
                if (serviceLocator instanceof ServiceLocator) {
                    return (ServiceLocator) serviceLocator;
                }
                
                // Try using reflection
                Field locatorField = findField(clientRequestContext.getClass(), "serviceLocator");
                if (locatorField != null) {
                    locatorField.setAccessible(true);
                    Object locator = locatorField.get(clientRequestContext);
                    if (locator instanceof ServiceLocator) {
                        return (ServiceLocator) locator;
                    }
                }
                
                // Try to get the configuration
                Field configField = findField(clientRequestContext.getClass(), "configuration");
                if (configField != null) {
                    configField.setAccessible(true);
                    Object config = configField.get(clientRequestContext);
                    
                    if (config != null) {
                        Field serviceLocatorField = findField(config.getClass(), "serviceLocator");
                        if (serviceLocatorField != null) {
                            serviceLocatorField.setAccessible(true);
                            serviceLocator = serviceLocatorField.get(config);
                            if (serviceLocator instanceof ServiceLocator) {
                                return (ServiceLocator) serviceLocator;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.debug(ServiceLocatorClientProvider.class, 
                    "Error getting ServiceLocator from ClientRequestContext: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Extract and return service locator from clientResponseContext.
     *
     * @param clientResponseContext Client response context.
     * @return Service locator.
     */
    public static ServiceLocator getServiceLocator(ClientResponseContext clientResponseContext) {
        try {
            // This is harder to get because ClientResponseContext is usually an interface
            // We need to get the response context from the request context
            if (clientResponseContext != null) {
                // Try to get the request context first through reflection
                Field requestContextField = findField(clientResponseContext.getClass(), "requestContext");
                if (requestContextField != null) {
                    requestContextField.setAccessible(true);
                    Object requestContext = requestContextField.get(clientResponseContext);
                    if (requestContext instanceof ClientRequestContext) {
                        return getServiceLocator((ClientRequestContext) requestContext);
                    }
                }
                
                // Try to find serviceLocator directly
                Field locatorField = findField(clientResponseContext.getClass(), "serviceLocator");
                if (locatorField != null) {
                    locatorField.setAccessible(true);
                    Object locator = locatorField.get(clientResponseContext);
                    if (locator instanceof ServiceLocator) {
                        return (ServiceLocator) locator;
                    }
                }
            }
        } catch (Exception e) {
            Logger.debug(ServiceLocatorClientProvider.class, 
                    "Error getting ServiceLocator from ClientResponseContext: " + e.getMessage());
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