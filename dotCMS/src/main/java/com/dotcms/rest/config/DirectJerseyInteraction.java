package com.dotcms.rest.config;

import com.dotmarketing.util.Logger;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorState;
import org.glassfish.hk2.api.ServiceHandle;

/**
 * This class provides direct access to Jersey/HK2 internals via reflection.
 * It allows for direct manipulation of the service locator without
 * requiring direct dependencies on Jersey classes.
 * 
 * This is useful for diagnosing and fixing classloader issues
 * during plugin redeployment.
 */
public class DirectJerseyInteraction {
    
    /**
     * Unregisters a REST resource by class name
     * This is more reliable than unregistering by class instance
     * since we might not have access to the right classloader
     * 
     * @param className The fully qualified class name to unregister
     * @return true if successful
     */
    public static boolean unregisterRestResource(String className) {
        try {
            Logger.info(DirectJerseyInteraction.class, "Attempting to unregister REST resource: " + className);
            
            // First try the standard method
            try {
                // First remove from DotRestApplication customClasses
                removeFromCustomClasses(className);
            } catch (Exception e) {
                Logger.debug(DirectJerseyInteraction.class, "Error removing from customClasses: " + e.getMessage());
            }
            
            // Now try to directly access and clean HK2 service locator 
            cleanHK2ServiceLocator(className);
            
            // Force GC to help clean up lingering references
            System.gc();
            
            return true;
        } catch (Exception e) {
            Logger.error(DirectJerseyInteraction.class, "Error unregistering REST resource: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Removes a class from DotRestApplication.customClasses by name
     */
    private static void removeFromCustomClasses(String className) throws Exception {
        // Get the DotRestApplication class
        Class<?> appClass = Class.forName("com.dotcms.rest.config.DotRestApplication");
        
        // Get the customClasses field
        Field customClassesField = appClass.getDeclaredField("customClasses");
        customClassesField.setAccessible(true);
        
        // Get the Map object
        Object customClasses = customClassesField.get(null);
        
        // Find entries matching our class name
        Class<?> mapClass = Class.forName("java.util.Map");
        Method keySetMethod = mapClass.getMethod("keySet");
        Object keySet = keySetMethod.invoke(customClasses);
        
        // Convert to iterable
        Iterable<?> iterable = (Iterable<?>) keySet;
        List<Object> toRemove = new ArrayList<>();
        
        for (Object key : iterable) {
            if (key instanceof Class) {
                Class<?> cls = (Class<?>) key;
                if (cls.getName().equals(className)) {
                    toRemove.add(key);
                    Logger.info(DirectJerseyInteraction.class, "Found class to remove: " + cls.getName());
                }
            }
        }
        
        // Remove the entries
        Method removeMethod = mapClass.getMethod("remove", Object.class);
        for (Object key : toRemove) {
            removeMethod.invoke(customClasses, key);
            Logger.info(DirectJerseyInteraction.class, "Removed class from customClasses: " + key);
        }
        
        // Skip container reload as we're using direct HK2 cleanup instead which is more reliable
        if (!toRemove.isEmpty()) {
            Logger.info(DirectJerseyInteraction.class, "Skipping container reload; using direct service locator cleanup instead");
        }
    }
    
    /**
     * Attempts to clean up HK2 service locator references to the specified class
     */
    private static void cleanHK2ServiceLocator(String className) {
        try {
            // Get service locator using our helper method
            ServiceLocator serviceLocator = getServiceLocator();
            
            if (serviceLocator == null) {
                Logger.warn(DirectJerseyInteraction.class, "Unable to get service locator for cleanup");
                return;
            }
            
            Logger.debug(DirectJerseyInteraction.class, "Checking ServiceLocator: " + serviceLocator);
            
            try {
                // Get descriptors
                Iterable<?> descriptors = serviceLocator.getDescriptors(descriptor -> true);
                
                for (Object descriptor : descriptors) {
                    try {
                        // Get implementation class name using reflection (to avoid direct HK2 dependencies)
                        Method getImplementationMethod = descriptor.getClass().getMethod("getImplementation");
                        String impl = (String) getImplementationMethod.invoke(descriptor);
                        
                        if (impl != null && impl.equals(className)) {
                            Logger.debug(DirectJerseyInteraction.class, "Found descriptor for class: " + className);
                            
                            // Try to unget the service
                            try {
                                // First need to get the ActiveDescriptor interface
                                Class<?> activeDescriptorClass = Class.forName("org.glassfish.hk2.api.ActiveDescriptor");
                                Method ungetServiceMethod = serviceLocator.getClass().getMethod("ungetService", activeDescriptorClass);
                                
                                if (activeDescriptorClass.isInstance(descriptor)) {
                                    ungetServiceMethod.invoke(serviceLocator, descriptor);
                                    Logger.debug(DirectJerseyInteraction.class, "Successfully unget service for: " + className);
                                }
                            } catch (Exception e) {
                                Logger.debug(DirectJerseyInteraction.class, "Error ungetting service: " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        // Continue checking other descriptors
                        Logger.debug(DirectJerseyInteraction.class, "Error examining descriptor: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                Logger.debug(DirectJerseyInteraction.class, "Error examining service locator: " + e.getMessage());
            }
        } catch (Exception e) {
            Logger.error(DirectJerseyInteraction.class, "Error cleaning HK2 ServiceLocator: " + e.getMessage(), e);
        }
    }
    
    // Removed dotCDN specific cleanup method in favor of the more general cleanPluginResourcesByPackage
    /**
     * Cleans up all resources for a particular plugin by package name prefix
     * This method finds and unregisters all resources that match the given package prefix
     * 
     * @param packagePrefix The package prefix to match (e.g., "com.dotcms.myplugin")
     */
    public static void cleanPluginResourcesByPackage(String packagePrefix) {
        Logger.info(DirectJerseyInteraction.class, "Cleaning up all resources with package prefix: " + packagePrefix);
        
        try {
            // First, try to find all classes matching the package prefix in the service locator
            List<String> resourcesToClean = findResourcesByPackagePrefix(packagePrefix);
            
            // If we found some resources, unregister them
            if (resourcesToClean != null && !resourcesToClean.isEmpty()) {
                Logger.info(DirectJerseyInteraction.class, "Found " + resourcesToClean.size() + " resources to clean for package prefix: " + packagePrefix);
                
                for (String resource : resourcesToClean) {
                    try {
                        // Remove the resource from all places it might be registered
                        unregisterRestResource(resource);
                    } catch (Exception e) {
                        Logger.error(DirectJerseyInteraction.class, "Error cleaning resource " + resource + ": " + e.getMessage(), e);
                    }
                }
                
                // Force GC after unregistering all resources - just once at the end
                System.gc();
                Logger.info(DirectJerseyInteraction.class, "Completed cleanup for package: " + packagePrefix);
            } else {
                Logger.info(DirectJerseyInteraction.class, "No resources found for package prefix: " + packagePrefix);
            }
            
        } catch (IllegalStateException e) {
            // If the service locator is shut down, we can't do much else
            Logger.warn(DirectJerseyInteraction.class, "Service locator may be shut down: " + e.getMessage());
        } catch (Exception e) {
            Logger.error(DirectJerseyInteraction.class, "Error cleaning plugin resources for prefix " + packagePrefix + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Finds all REST resources in the service locator that match a package prefix
     * 
     * @param packagePrefix The package prefix to search for
     * @return A list of class names that match the prefix
     */
    private static List<String> findResourcesByPackagePrefix(String packagePrefix) {
        List<String> results = new ArrayList<>();
        
        try {
            // Get the HK2 service locator
            ServiceLocator serviceLocator = getServiceLocator();
            if (serviceLocator == null) {
                Logger.warn(DirectJerseyInteraction.class, "Unable to get service locator");
                return results;
            }
            
            // Make sure the service locator is active
            try {
                if (!serviceLocator.getState().equals(ServiceLocatorState.RUNNING)) {
                    Logger.warn(DirectJerseyInteraction.class, "Service locator is not in RUNNING state: " + serviceLocator.getState());
                    return results;
                }
            } catch (Exception e) {
                Logger.warn(DirectJerseyInteraction.class, "Error checking service locator state: " + e.getMessage());
                return results;
            }
            
            // Use HK2 service locator to find all services
            try {
                // Get all service handles
                List<ServiceHandle<?>> serviceHandles = serviceLocator.getAllServiceHandles(criteria -> true);
                
                // Iterate and find those that match our package prefix
                for (ServiceHandle<?> handle : serviceHandles) {
                    try {
                        Class<?> serviceClass = handle.getActiveDescriptor().getImplementationClass();
                        if (serviceClass != null) {
                            String className = serviceClass.getName();
                            
                            // Check if this class is from our package
                            if (className.startsWith(packagePrefix)) {
                                results.add(className);
                                Logger.debug(DirectJerseyInteraction.class, "Found resource to clean up: " + className);
                            }
                        }
                    } catch (Exception e) {
                        // Continue with the next handle, this might be a proxy or generated class
                        Logger.debug(DirectJerseyInteraction.class, "Error processing handle: " + e.getMessage());
                    }
                }
            } catch (IllegalStateException e) {
                // If the service locator is shut down during this operation
                Logger.warn(DirectJerseyInteraction.class, "Service locator state error: " + e.getMessage());
            } catch (Exception e) {
                Logger.error(DirectJerseyInteraction.class, "Error finding resources: " + e.getMessage(), e);
            }
            
            Logger.debug(DirectJerseyInteraction.class, "Found " + results.size() + " resources matching package prefix: " + packagePrefix);
            
        } catch (Exception e) {
            Logger.error(DirectJerseyInteraction.class, "Error searching for resources with prefix " + packagePrefix + ": " + e.getMessage(), e);
        }
        
        return results;
    }
    
    /**
     * Gets the HK2 ServiceLocator instance
     * 
     * @return The ServiceLocator instance, or null if not found
     */
    private static ServiceLocator getServiceLocator() {
        // Set of approaches to try in order of preference
        try {
            // 1. First try the standard factory approach - this is the preferred way but might be different in different Jersey versions
            try {
                Class<?> factoryClass = Class.forName("org.glassfish.hk2.api.ServiceLocatorFactory");
                Method getInstanceMethod = factoryClass.getMethod("getInstance");
                Object factory = getInstanceMethod.invoke(null);
                
                // Try different methods to find locators
                ServiceLocator locator = null;
                
                // Approach 1: Try getAllServiceLocators (older API)
                try {
                    Method getAllServiceLocatorsMethod = factoryClass.getMethod("getAllServiceLocators");
                    Object locators = getAllServiceLocatorsMethod.invoke(factory);
                    
                    if (locators instanceof Iterable) {
                        Iterable<?> iterable = (Iterable<?>) locators;
                        for (Object l : iterable) {
                            if (l instanceof ServiceLocator) {
                                locator = (ServiceLocator) l;
                                Logger.debug(DirectJerseyInteraction.class, "Found ServiceLocator using getAllServiceLocators");
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.debug(DirectJerseyInteraction.class, "getAllServiceLocators not available: " + e.getMessage());
                }
                
                // Approach 2: Try findAll (newer API)
                if (locator == null) {
                    try {
                        Method findAllMethod = factoryClass.getMethod("findAll");
                        Object locators = findAllMethod.invoke(factory);
                        
                        if (locators instanceof Iterable) {
                            Iterable<?> iterable = (Iterable<?>) locators;
                            for (Object l : iterable) {
                                if (l instanceof ServiceLocator) {
                                    locator = (ServiceLocator) l;
                                    Logger.debug(DirectJerseyInteraction.class, "Found ServiceLocator using findAll");
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Logger.debug(DirectJerseyInteraction.class, "findAll not available: " + e.getMessage());
                    }
                }
                
                // Approach 3: Try named lookups
                if (locator == null) {
                    try {
                        // Try common service locator names
                        String[] names = {"dotcms-service-locator", "default", "jersey-server-resource-locator", 
                                "system", "jersey-client-resource-locator"};
                        
                        Method getServiceLocatorMethod = factoryClass.getMethod("getServiceLocator", String.class);
                        
                        for (String name : names) {
                            try {
                                Object serviceLocator = getServiceLocatorMethod.invoke(factory, name);
                                if (serviceLocator instanceof ServiceLocator) {
                                    locator = (ServiceLocator) serviceLocator;
                                    Logger.debug(DirectJerseyInteraction.class, "Found ServiceLocator with name: " + name);
                                    break;
                                }
                            } catch (Exception e) {
                                // Continue with next name
                            }
                        }
                    } catch (Exception e) {
                        Logger.debug(DirectJerseyInteraction.class, "Named lookup not available: " + e.getMessage());
                    }
                }
                
                // If we found a locator, return it
                if (locator != null) {
                    return locator;
                }
            } catch (Exception e) {
                Logger.debug(DirectJerseyInteraction.class, "Standard factory approach failed: " + e.getMessage());
            }
            
            // 2. Try using reflection to get current thread locale
            try {
                // Try to get the thread-local service locator
                Class<?> serviceLocatorProviderClass = Class.forName("org.glassfish.jersey.ServiceLocatorProvider");
                
                // Check if our compatibility implementation
                if (serviceLocatorProviderClass.getClassLoader().equals(DirectJerseyInteraction.class.getClassLoader())) {
                    // It's our implementation, so try to use reflection to get the current locator
                    Class<?> applicationHandlerClass = Class.forName("org.glassfish.jersey.server.ApplicationHandler");
                    Field currentField = applicationHandlerClass.getDeclaredField("CURRENT_FACTORY");
                    currentField.setAccessible(true);
                    
                    // Get thread local
                    Object threadLocal = currentField.get(null);
                    if (threadLocal != null) {
                        Method getMethod = threadLocal.getClass().getMethod("get");
                        Object factory = getMethod.invoke(threadLocal);
                        
                        if (factory != null) {
                            Field locatorField = factory.getClass().getDeclaredField("serviceLocator");
                            locatorField.setAccessible(true);
                            Object locator = locatorField.get(factory);
                            
                            if (locator instanceof ServiceLocator) {
                                Logger.debug(DirectJerseyInteraction.class, "Found ServiceLocator using thread local");
                                return (ServiceLocator) locator;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Logger.debug(DirectJerseyInteraction.class, "Thread-local approach failed: " + e.getMessage());
            }
            
            // 3. Try DotServiceLocatorImpl
            try {
                Class<?> dotServiceLocatorImplClass = Class.forName("com.dotcms.rest.config.DotServiceLocatorImpl");
                
                // Try to find the dotCMS service locator
                Field instancesField = dotServiceLocatorImplClass.getDeclaredField("INSTANCES");
                instancesField.setAccessible(true);
                Object instances = instancesField.get(null);
                
                if (instances instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) instances;
                    
                    // Get the values
                    java.util.Collection<?> values = map.values();
                    if (values != null && !values.isEmpty()) {
                        // Just get the first one
                        for (Object value : values) {
                            if (value instanceof ServiceLocator) {
                                Logger.debug(DirectJerseyInteraction.class, "Found ServiceLocator from DotServiceLocatorImpl");
                                return (ServiceLocator) value;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Logger.debug(DirectJerseyInteraction.class, "DotServiceLocatorImpl approach failed: " + e.getMessage());
            }
            
            // 4. Final option - try to get the service locator from ContainerReloader
            try {
                Class<?> containerReloaderClass = Class.forName("com.dotcms.rest.config.ContainerReloader");
                Field containerRefField = containerReloaderClass.getDeclaredField("containerRef");
                containerRefField.setAccessible(true);
                
                // Get the atomic reference
                Object containerRef = containerRefField.get(null);
                if (containerRef != null) {
                    Method getMethod = containerRef.getClass().getMethod("get");
                    Object container = getMethod.invoke(containerRef);
                    
                    if (container != null) {
                        // The container should have a serviceLocator field
                        Field locatorField = findField(container.getClass(), "serviceLocator");
                        if (locatorField != null) {
                            locatorField.setAccessible(true);
                            Object locator = locatorField.get(container);
                            
                            if (locator instanceof ServiceLocator) {
                                Logger.debug(DirectJerseyInteraction.class, "Found ServiceLocator from ContainerReloader");
                                return (ServiceLocator) locator;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Logger.debug(DirectJerseyInteraction.class, "ContainerReloader approach failed: " + e.getMessage());
            }
            
            Logger.debug(DirectJerseyInteraction.class, "All ServiceLocator lookup approaches failed");
        } catch (Exception e) {
            Logger.error(DirectJerseyInteraction.class, "Error getting service locator: " + e.getMessage(), e);
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