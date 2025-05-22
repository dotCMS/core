package com.dotcms.rest.config;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.telemetry.rest.TelemetryResource;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Lazy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import java.lang.reflect.Method;

/**
 * This class provides the list of all the REST end-points in dotCMS. Every new
 * service needs to be added to this list in order to be available for use.
 *
 * @author Will Ezell
 * @version 2.5.3
 * @since Dec 5, 2013
 *
 */

@ApplicationPath("/api")
@OpenAPIDefinition(
		info = @Info(
				title = "dotCMS REST API",
				version = "3"),
		servers = @Server(
						description = "dotCMS Server",
						url = "/"),
		tags = {
				@Tag(name = "Workflow"),
				@Tag(name = "Page"),
				@Tag(name = "Content Type"),
				@Tag(name = "Content Type Field"),
				@Tag(name = "Content Delivery"),
				@Tag(name = "Bundle"),
				@Tag(name = "Navigation"),
				@Tag(name = "Experiment"),
				@Tag(name = "Content Report")
		}
)
public class DotRestApplication extends ResourceConfig {

	private static final Lazy<Boolean> ENABLE_TELEMETRY_FROM_CORE = Lazy.of(() ->
			Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_TELEMETRY_CORE_ENABLED, true));

	public DotRestApplication() {
		final List<String> packages = new ArrayList<>(List.of(
				"com.dotcms.rest",
				"com.dotcms.contenttype.model.field",
				"com.dotcms.rendering.js",
				"com.dotcms.ai.rest",
				"io.swagger.v3.jaxrs2"));
		if (Boolean.TRUE.equals(ENABLE_TELEMETRY_FROM_CORE.get())) {
			packages.add(TelemetryResource.class.getPackageName());
		}
		register(MultiPartFeature.class).
		register(JacksonJaxbJsonProvider.class).
		registerClasses(customClasses.keySet()).
		packages(packages.toArray(new String[0]));
	}

	/**
	 * This is the cheap way to create a concurrent set of user provided classes
	 */
	private static final Map<Class<?>, Boolean> customClasses = new ConcurrentHashMap<>();

	/**
	 * adds a class and reloads
	 * @param clazz the class ot add
	 */
	public static synchronized void addClass(final Class<?> clazz) {
		if(clazz==null){
			return;
		}
		if (Boolean.TRUE.equals(ENABLE_TELEMETRY_FROM_CORE.get())
				&& clazz.getName().equalsIgnoreCase("com.dotcms.experience.TelemetryResource")) {
			Logger.warn(DotRestApplication.class, "Bypassing activation of Telemetry REST Endpoint from OSGi");
			return;
		}
		// Just add the class to customClasses without triggering a container reload
		customClasses.computeIfAbsent(clazz, c -> true);
		Logger.info(DotRestApplication.class, "Added REST resource: " + clazz.getName() + " (container reload skipped)");
	}

	/**
	 * removes a class and reloads
	 * @param clazz
	 */
	public static synchronized void removeClass(Class<?> clazz) {
		if(clazz==null){
			return;
		}
		
		boolean removed = customClasses.remove(clazz) != null;
		
		if(removed) {
            Logger.info(DotRestApplication.class, "Removing REST resource: " + clazz.getName());
            
            // Get plugin package prefix from class name for cleanup
            String className = clazz.getName();
            String packagePrefix = getPackagePrefix(className);
            
            // Clean up all resources related to this plugin package
            try {
                // Use reflection to avoid direct dependency
                Class<?> cleanupClass = Class.forName("com.dotcms.rest.config.DirectJerseyInteraction");
                Method cleanupMethod = cleanupClass.getMethod("cleanPluginResourcesByPackage", String.class);
                cleanupMethod.invoke(null, packagePrefix);
                Logger.info(DotRestApplication.class, "Cleaned up resources for package: " + packagePrefix);
            } catch (Exception e) {
                Logger.error(DotRestApplication.class, "Error cleaning plugin resources: " + e.getMessage(), e);
            }
            
            // Also directly unregister this specific resource class
            try {
                // Use reflection to avoid direct dependency
                Class<?> cleanupClass = Class.forName("com.dotcms.rest.config.DirectJerseyInteraction");
                Method cleanupMethod = cleanupClass.getMethod("unregisterRestResource", String.class);
                cleanupMethod.invoke(null, className);
                Logger.info(DotRestApplication.class, "Successfully unregistered REST resource: " + className);
            } catch (Exception e) {
                Logger.error(DotRestApplication.class, "Error unregistering REST resource: " + e.getMessage(), e);
            }
		}
	}
    
    /**
     * Helper method to extract the package prefix from a class name
     * This attempts to extract a meaningful plugin package name (up to 3 segments)
     * 
     * @param className The fully qualified class name
     * @return A package prefix suitable for plugin resource cleanup
     */
    private static String getPackagePrefix(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }
        
        // Check for common plugin package patterns
        if (className.contains(".dotcdn")) {
            return "com.dotcms.dotcdn";
        }
        
        // For other plugins, try to extract a reasonable package prefix
        String[] parts = className.split("\\.");
        if (parts.length <= 1) {
            return className; // No package structure
        }
        
        // Build a package prefix with up to 3 segments
        int segments = Math.min(3, parts.length - 1); // Exclude the class name
        StringBuilder packagePrefix = new StringBuilder();
        
        for (int i = 0; i < segments; i++) {
            if (i > 0) {
                packagePrefix.append(".");
            }
            packagePrefix.append(parts[i]);
        }
        
        return packagePrefix.toString();
    }
}
