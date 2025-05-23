package com.dotcms.rest.config;

import com.dotmarketing.util.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * This class is responsible for cleaning up REST resources when
 * bundles are uninstalled from the OSGi framework.
 * 
 * This helps solve the "object is not an instance of declaring class" errors
 * that happen when plugins with REST endpoints are redeployed.
 */
public class DotBundleListener implements BundleListener {
    
    private static DotBundleListener instance;
    private BundleContext context;
    private boolean isInitialized = false;
    
    /**
     * Get the singleton instance
     * @return The DotBundleListener instance
     */
    public static synchronized DotBundleListener getInstance() {
        if (instance == null) {
            instance = new DotBundleListener();
        }
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private DotBundleListener() {
        // Private constructor
        Logger.info(this, "DotBundleListener created");
    }
    
    /**
     * Check if the listener is initialized
     * @return true if initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Initialize the listener with a bundle context
     * @param context The OSGi bundle context
     */
    public void init(BundleContext context) {
        if (this.context == null) {
            this.context = context;
            try {
                context.addBundleListener(this);
                isInitialized = true;
                Logger.info(this, "DotBundleListener initialized and registered with OSGi framework");
                
                // Log all current bundles for diagnostic purposes
                Bundle[] bundles = context.getBundles();
                Logger.info(this, "Currently installed bundles: " + bundles.length);
                for (Bundle bundle : bundles) {
                    Logger.info(this, String.format("Bundle: %s [ID: %d, State: %d]", 
                            bundle.getSymbolicName(), 
                            bundle.getBundleId(),
                            bundle.getState()));
                }
            } catch (Exception e) {
                Logger.error(this, "Error initializing DotBundleListener", e);
            }
        } else {
            Logger.info(this, "DotBundleListener already initialized");
        }
    }
    
    /**
     * Clean up the listener
     */
    public void destroy() {
        if (context != null) {
            try {
                context.removeBundleListener(this);
                Logger.info(this, "DotBundleListener unregistered from OSGi framework");
            } catch (Exception e) {
                Logger.error(this, "Error destroying DotBundleListener", e);
            } finally {
                context = null;
                isInitialized = false;
            }
        }
    }
    
    /**
     * The BundleListener implementation
     * This method is called when a bundle's state changes in the OSGi framework
     * @param event The bundle event
     */
    @Override
    public void bundleChanged(BundleEvent event) {
        try {
            Bundle bundle = event.getBundle();
            String bundleName = bundle.getSymbolicName();
            long bundleId = bundle.getBundleId();
            
            Logger.info(this, String.format("Bundle event received: %s [ID: %d, Event Type: %d]", 
                    bundleName, bundleId, event.getType()));
            
            switch (event.getType()) {
                case BundleEvent.STARTING:
                    // When a bundle is starting, log the event
                    Logger.info(this, "Bundle starting: " + bundleName);
                    break;
                    
                case BundleEvent.STOPPING:
                    // When a bundle is stopping, clean up its REST resources
                    Logger.info(this, "Bundle stopping: " + bundleName + ", cleaning up REST resources");
                    cleanupBundleResources(bundle);
                    break;
                
                case BundleEvent.UNINSTALLED:
                    // When a bundle is uninstalled, clean up its REST resources again (for safety)
                    Logger.info(this, "Bundle uninstalled: " + bundleName + ", cleaning up REST resources");
                    cleanupBundleResources(bundle);
                    break;
                    
                case BundleEvent.INSTALLED:
                    // When a bundle is installed, log the event
                    Logger.info(this, "Bundle installed: " + bundleName);
                    break;
                    
                default:
                    // Ignore other events
                    break;
            }
        } catch (Exception e) {
            Logger.error(this, "Error in bundleChanged event handler", e);
        }
    }
    
    /**
     * Clean up all REST resources for a bundle that is being uninstalled
     * @param bundle The bundle being uninstalled
     */
    private void cleanupBundleResources(Bundle bundle) {
        try {
            String bundleName = bundle.getSymbolicName();
            
            // Convert OSGi bundle name to package format (e.g., my-plugin to my.plugin)
            String packageBase = bundleName.replace('-', '.');
            
            // Clean up using the DirectJerseyInteraction utility
            Logger.info(this, "Cleaning up resources for bundle: " + bundleName + 
                    " with base package: " + packageBase);
            
            // Try to find the actual package by checking the bundle headers
            String basePackage = findBasePackage(bundle, packageBase);
            Logger.info(this, "Using base package: " + basePackage + " for cleanup");
            
            // First clean up using the DotRestApplication mechanism
            // This ensures the REST resources are removed from the Jersey registry
            cleanupDotRestApplicationResources(bundle, basePackage);
            
            // Clean up all resources with this package prefix using the direct HK2 method
            DirectJerseyInteraction.cleanPluginResourcesByPackage(basePackage);
            
            // Force garbage collection to help clean up lingering references
            System.gc();
            
            Logger.info(this, "Completed cleanup for bundle: " + bundleName);
            
        } catch (Exception e) {
            Logger.error(this, "Error cleaning up REST resources for bundle: " + 
                    bundle.getSymbolicName(), e);
        }
    }
    
    /**
     * Clean up resources registered through DotRestApplication
     * @param bundle The bundle being uninstalled
     * @param basePackage The base package of the bundle
     */
    private void cleanupDotRestApplicationResources(Bundle bundle, String basePackage) {
        try {
            // Get all installed bundles
            Bundle[] bundles = context.getBundles();
            ClassLoader bundleClassLoader = bundle.adapt(ClassLoader.class);
            
            // Try to find classes from the plugin that might have been registered as REST resources
            for (String exportedPackage : findExportedPackages(bundle)) {
                Logger.info(this, "Checking exported package: " + exportedPackage);
                
                if (exportedPackage.startsWith(basePackage) || exportedPackage.contains("rest")) {
                    try {
                        // Look for Resource classes in this package
                        String className = exportedPackage + ".Resource";
                        Logger.debug(this, "Looking for class: " + className);
                        
                        try {
                            Class<?> clazz = bundleClassLoader.loadClass(className);
                            Logger.info(this, "Found REST resource class: " + clazz.getName());
                            DotRestApplication.removeClass(clazz);
                        } catch (ClassNotFoundException e) {
                            // This is expected, as we're guessing class names
                            Logger.debug(this, "Class not found: " + className);
                        }
                        
                        // Try more specific resource name patterns
                        String[] suffixes = new String[] {
                            "Resource", "RestResource", "RESTResource", "API", "APIImpl", "Endpoint"
                        };
                        
                        for (String suffix : suffixes) {
                            className = exportedPackage + "." + exportedPackage.substring(exportedPackage.lastIndexOf('.') + 1) + suffix;
                            Logger.debug(this, "Looking for class: " + className);
                            
                            try {
                                Class<?> clazz = bundleClassLoader.loadClass(className);
                                Logger.info(this, "Found REST resource class: " + clazz.getName());
                                DotRestApplication.removeClass(clazz);
                            } catch (ClassNotFoundException e) {
                                // This is expected, as we're guessing class names
                                Logger.debug(this, "Class not found: " + className);
                            }
                        }
                    } catch (Exception e) {
                        Logger.debug(this, "Error looking for REST resources in package: " + exportedPackage + " - " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Logger.error(this, "Error cleaning up DotRestApplication resources", e);
        }
    }
    
    /**
     * Find all exported packages for a bundle
     * @param bundle The bundle
     * @return Array of exported package names
     */
    private String[] findExportedPackages(Bundle bundle) {
        String exportPackage = bundle.getHeaders().get("Export-Package");
        if (exportPackage == null || exportPackage.isEmpty()) {
            return new String[0];
        }
        
        // Parse the Export-Package header which could have multiple packages
        String[] packages = exportPackage.split(",");
        for (int i = 0; i < packages.length; i++) {
            // Remove attributes and parameters
            String pkg = packages[i].trim();
            int semicolonPos = pkg.indexOf(';');
            if (semicolonPos > 0) {
                packages[i] = pkg.substring(0, semicolonPos).trim();
            } else {
                packages[i] = pkg;
            }
        }
        
        return packages;
    }
    
    /**
     * Try to find the base package for a plugin by looking at its headers
     * @param bundle The bundle
     * @param defaultPackage Default package to use if we can't find it
     * @return The base package name
     */
    private String findBasePackage(Bundle bundle, String defaultPackage) {
        // Try to get the Export-Package header which might contain the main package
        String exportPackage = bundle.getHeaders().get("Export-Package");
        Logger.debug(this, "Bundle Export-Package header: " + exportPackage);
        
        if (exportPackage != null && !exportPackage.isEmpty()) {
            // Parse the Export-Package header which could have multiple packages
            String[] packages = exportPackage.split(",");
            for (String pkg : packages) {
                // Remove attributes and parameters
                pkg = pkg.trim();
                int semicolonPos = pkg.indexOf(';');
                if (semicolonPos > 0) {
                    pkg = pkg.substring(0, semicolonPos).trim();
                }
                
                // Return the first package that's not an OSGi system package
                if (!pkg.startsWith("org.osgi") && 
                    !pkg.startsWith("javax.") && 
                    !pkg.startsWith("java.")) {
                    Logger.debug(this, "Found exported package: " + pkg);
                    return pkg;
                }
            }
        }
        
        // Also try Bundle-SymbolicName with "com.dotcms." prefix
        String result = "com.dotcms." + defaultPackage.toLowerCase();
        Logger.debug(this, "Using default package name: " + result);
        return result;
    }
} 