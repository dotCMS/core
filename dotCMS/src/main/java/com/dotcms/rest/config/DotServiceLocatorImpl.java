package com.dotcms.rest.config;

import com.dotmarketing.util.Logger;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceHandle;
import org.jvnet.hk2.internal.ServiceLocatorImpl;
import org.jvnet.hk2.internal.Utilities;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides a workaround for a known issue in Jersey where a service locator,
 * once marked as inactive, throws an exception that renders Jersey unusable.
 *
 * <p>When this exception is thrown directly from the service locator during dependency
 * injection, Jersey enters a non-functional state. The solution implemented here involves
 * overriding the service locator using JavaServiceProviderLocator. By leveraging the
 * class loader, we replace the problematic service instances with safe-to-dispose null
 * classes.</p>
 *
 * <p>The replacement is configured in the <code>META-INF/services</code> folder via Service Provider Interface (SPI),
 * where the necessary class overrides are specified. This ensures that when an exception
 * is encountered during disposal, it is intercepted, and a safe null instance is returned,
 * preventing additional failures.</p>
 *
 * <p>It is important to note that the root cause of this issue is a known Github issue,
 * and this workaround should be removed once the migration to Tomcat 10 is complete.</p>
 * See <a href="https://github.com/dotCMS/core/issues/31185">31185</a> for more information.
 */
public class DotServiceLocatorImpl extends ServiceLocatorImpl {

    private static final String PATTERN = "DotServiceLocatorImpl\\(__HK2_Generated_\\d+,\\d+,\\d+\\) has been shut down";
    private static final Pattern REGEX = Pattern.compile(PATTERN);
    private static final Pattern PLUGIN_RELOAD_PATTERN = Pattern.compile("object is not an instance of declaring class");
    
    // Track plugin services and their dependencies
    private static final Map<String, Set<String>> pluginServiceDependencies = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> activePluginServices = new ConcurrentHashMap<>();

    // Track problematic resources during reload to prevent repeated errors
    private static final Set<String> problematicResources = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, ClassLoader> resourceClassLoaders = new ConcurrentHashMap<>();

    /**
     * To Narrow down the exception to the one we are looking for
     * @param e The exception to check
     * @return True if the exception is the one we are looking for
     */
    public static boolean isServiceShutDownException(IllegalStateException e) {
        String exceptionMessage = e.getMessage();
        Matcher matcher = REGEX.matcher(exceptionMessage);
        return matcher.matches();
    }

    /**
     * Checks if the exception is related to plugin reinstallation
     * @param e The exception to check
     * @return True if the exception is related to plugin reinstallation
     */
    public static boolean isPluginReloadException(IllegalArgumentException e) {
        String exceptionMessage = e.getMessage();
        return PLUGIN_RELOAD_PATTERN.matcher(exceptionMessage).matches();
    }

    /**
     * Tracks a plugin service and its dependencies
     * @param serviceClass The plugin service class
     * @param dependencies Set of dependent service class names
     */
    private void trackPluginService(Class<?> serviceClass, Set<String> dependencies) {
        if (serviceClass != null) {
            String serviceName = serviceClass.getName();
            pluginServiceDependencies.put(serviceName, dependencies);
            activePluginServices.put(serviceName, serviceClass);
            Logger.debug(this, String.format("Tracked plugin service: %s with dependencies: %s", 
                serviceName, dependencies));
        }
    }

    /**
     * Clears only the necessary services for a plugin reload
     * @param serviceClass The service class that triggered the reload
     */
    private void clearTargetedPluginServices(Class<?> serviceClass) {
        if (serviceClass != null) {
            String serviceName = serviceClass.getName();
            Set<String> dependencies = pluginServiceDependencies.get(serviceName);
            
            if (dependencies != null) {
                // Clear only the specific service and its direct dependencies
                activePluginServices.remove(serviceName);
                for (String dependency : dependencies) {
                    activePluginServices.remove(dependency);
                }
                
                Logger.info(this, String.format("Cleared targeted services for plugin reload: %s and dependencies: %s",
                    serviceName, dependencies));
            } else {
                // If no dependencies are tracked, just clear the specific service
                activePluginServices.remove(serviceName);
                Logger.info(this, String.format("Cleared single service for plugin reload: %s", serviceName));
            }
        }
    }

    /**
     * Logs information about a classloader mismatch
     * This is useful for debugging plugin reload issues
     * 
     * @param resourceClass The resource class
     * @param methodName The method name being invoked
     * @param classLoader The classloader that loaded the resource
     * @param url The URL being accessed
     */
    public static void logClassLoaderMismatch(Class<?> resourceClass, String methodName, ClassLoader classLoader, String url) {
        if (resourceClass != null) {
            String className = resourceClass.getName();
            problematicResources.add(className);
            
            Logger.debug(DotServiceLocatorImpl.class, 
                String.format("Classloader mismatch detected - Class: %s, Method: %s", 
                    className, methodName));
        }
    }

    /**
     * Clears the record of problematic resources
     * Useful after a complete restart or when reload is complete
     */
    public static void clearProblematicResources() {
        int size = problematicResources.size();
        problematicResources.clear();
        resourceClassLoaders.clear();
        Logger.info(DotServiceLocatorImpl.class, 
            String.format("Cleared %d problematic resources", size));
    }

    private final String name;
    /**
     * Called by the Generator, and hence must be a public method
     *
     * @param name   The name of this locator
     * @param parent The parent of this locator (maybe null)
     */
    public DotServiceLocatorImpl(String name, ServiceLocatorImpl parent) {
        super(name, parent);
        this.name = name;
    }

    /**
     * This Method is overridden to ignore IllegalStateException during reload
     * Injects the given object using the given strategy
     * @param injectMe The object to be analyzed and injected into
     * @param strategy The name of the {@link ClassAnalyzer} that should be used. If
     */
    @Override
    public void inject(Object injectMe, String strategy) {
        try {
            //there's a bug in jersey that causes a IllegalStateException to be thrown when the container is reloading
            //This Bug Kills the container leaving it useless
            //And the reason if the checkState Method in the super Class (which is private)
            Utilities.justInject(injectMe, this, strategy);
        } catch (IllegalStateException e) {
            if(isServiceShutDownException(e)) {
                Logger.debug(this,
                String.format("Service locator shutdown detected during inject - handled gracefully. Object: %s",
                injectMe.getClass().getName())
                );
                // Do nothing, allowing Jersey to continue
            } else {
                throw e;
            }
        } catch (IllegalArgumentException e) {
            if(isPluginReloadException(e)) {
                String className = injectMe != null ? injectMe.getClass().getName() : "unknown";
                Logger.debug(this,
                String.format("Plugin reload detected during inject - handled gracefully. Object: %s",
                className)
                );
                
                // Do nothing, allowing Jersey to continue
            } else {
                throw e;
            }
        }
    }

    /**
     * This Method is overridden to ignore IllegalStateException during reload
     * @param activeDescriptor The descriptor for which to create a {@link ServiceHandle}.
     * May not be null
     * @return
     * @param <T>
     * @throws MultiException
     */
    @Override
    public <T> ServiceHandle<T> getServiceHandle(ActiveDescriptor<T> activeDescriptor) throws MultiException {
        try {
            return super.getServiceHandle(activeDescriptor);
        } catch (IllegalStateException e) {
            if(isServiceShutDownException(e)) {
                Logger.debug(this,
                String.format("Service locator shutdown detected during reload - handled gracefully. Service: %s",
                activeDescriptor.getImplementation())
                );
                return null;
            }
            throw e;
        } catch (IllegalArgumentException e) {
            if(isPluginReloadException(e)) {
                String implementation = activeDescriptor.getImplementation();
                Logger.debug(this,
                String.format("Plugin reload detected - handled gracefully. Service: %s",
                implementation)
                );
                return null;
            }
            throw e;
        }
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(ActiveDescriptor<T> activeDescriptor, Injectee injectee) throws MultiException {
        try {
            return super.getServiceHandle(activeDescriptor, injectee);
        } catch (IllegalStateException e) {
            if(isServiceShutDownException(e)) {
                Logger.debug(this,
                String.format("Service locator shutdown detected during reload - handled gracefully. Service: %s",
                activeDescriptor.getImplementation())
                );
                return null;
            }
            throw e;
        } catch (IllegalArgumentException e) {
            if(isPluginReloadException(e)) {
                String implementation = activeDescriptor.getImplementation();
                
                // Also log injectee info which might help identify the source
                if (injectee != null && injectee.getRequiredType() != null) {
                    Logger.debug(this, 
                        String.format("Injectee details - Type: %s, Position: %s, Parent: %s",
                            injectee.getRequiredType(),
                            injectee.getPosition(),
                            injectee.getParent()
                        )
                    );
                }
                
                Logger.debug(this,
                String.format("Plugin reload detected - handled gracefully. Service: %s",
                implementation)
                );
                return null;
            }
            throw e;
        }
    }

    /**
     * The getInjecteeDescriptor method is called during undeployment and can throw the shutdown exception.
     * This override handles the exception more gracefully.
     */
    @Override
    public ActiveDescriptor<?> getInjecteeDescriptor(Injectee injectee) throws MultiException {
        try {
            return super.getInjecteeDescriptor(injectee);
        } catch (IllegalStateException e) {
            if(isServiceShutDownException(e)) {
                Logger.debug(this,
                String.format("Service locator shutdown detected during undeploy - handled gracefully. Injectee: %s",
                injectee.getRequiredType())
                );
                return null;
            }
            throw e;
        } catch (IllegalArgumentException e) {
            if(isPluginReloadException(e)) {
                Logger.debug(this,
                String.format("Plugin reload detected during getInjecteeDescriptor - handled gracefully. Injectee: %s",
                injectee.getRequiredType())
                );
                
                // Log injectee details
                if (injectee != null && injectee.getRequiredType() != null) {
                    Logger.debug(this, 
                        String.format("Injectee details - Type: %s, Position: %s, Parent: %s",
                            injectee.getRequiredType(),
                            injectee.getPosition(),
                            injectee.getParent()
                        )
                    );
                }
                
                return null;
            }
            throw e;
        }
    }

    /**
     * Super useful for debugging
     * @return A string representation of this object
     */
    @Override
    public String toString() {
        return "DotServiceLocatorImpl(" + name + "," + super.getLocatorId() + "," + System.identityHashCode(this) + ")";
    }
    
    /**
     * Makes Jersey errors more visible by changing the logging level
     * Call this method to improve the visibility of classloader issues
     */
    public static void enableDetailedClassLoaderLogging() {
        // Setup Jersey to log at DEBUG level
        java.util.logging.Logger jerseyLogger = java.util.logging.Logger.getLogger("org.glassfish.jersey");
        if (jerseyLogger != null) {
            jerseyLogger.setLevel(java.util.logging.Level.FINE);
        }
        
        // Also set relevant HK2 loggers
        java.util.logging.Logger hk2Logger = java.util.logging.Logger.getLogger("org.jvnet.hk2");
        if (hk2Logger != null) {
            hk2Logger.setLevel(java.util.logging.Level.FINE);
        }
        
        Logger.info(DotServiceLocatorImpl.class, 
            "Enhanced logging enabled for Jersey and HK2 to diagnose classloader issues");
    }
    
    /**
     * Disables detailed classloader logging by resetting Jersey and HK2 loggers to INFO level
     */
    public static void disableDetailedClassLoaderLogging() {
        // Reset Jersey loggers to INFO level
        java.util.logging.Logger jerseyLogger = java.util.logging.Logger.getLogger("org.glassfish.jersey");
        if (jerseyLogger != null) {
            jerseyLogger.setLevel(java.util.logging.Level.INFO);
        }
        
        // Reset HK2 loggers to INFO level
        java.util.logging.Logger hk2Logger = java.util.logging.Logger.getLogger("org.jvnet.hk2");
        if (hk2Logger != null) {
            hk2Logger.setLevel(java.util.logging.Level.INFO);
        }
        
        Logger.info(DotServiceLocatorImpl.class, 
            "Enhanced logging disabled for Jersey and HK2 - reset to INFO level");
    }

    /**
     * This method is called by Jersey's ResourceMethodInvocationHandlerFactory to handle 
     * method invocation on REST resources. We need to capture and handle the error here
     * before it's propagated to the client.
     * 
     * @param object The target object
     * @param method The method to invoke
     * @param args The arguments to pass
     * @return The result of method invocation
     * @throws Exception If an error occurred
     */
    public static Object safeInvokeResourceMethod(Object object, Method method, Object[] args) throws Exception {
        try {
            // Try normal invocation first
            return method.invoke(object, args);
        } catch (IllegalArgumentException e) {
            // Check if this is the classloader mismatch exception
            if (e.getMessage() != null && e.getMessage().contains("object is not an instance of declaring class")) {
                // Log this at info level as it's expected during plugin reload
                Logger.info(DotServiceLocatorImpl.class, 
                    "Detected classloader mismatch during resource method invocation: " + 
                    (object != null ? object.getClass().getName() : "null") + "." + 
                    (method != null ? method.getName() : "null"));
                
                // Return a graceful response instead of throwing
                return null;
            }
            throw e;
        }
    }
}
