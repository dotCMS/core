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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import javax.ws.rs.ApplicationPath;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

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
				@Tag(name = "Accessibility Checker", description = "Web accessibility checking and compliance"),
				@Tag(name = "Administration", description = "System administration and management tools"),
				@Tag(name = "AI", description = "AI-powered content generation and analysis endpoints"),
				@Tag(name = "Announcements", description = "System announcements and notifications"),
				@Tag(name = "API Token", description = "API token management and authentication"),
				@Tag(name = "Apps", description = "Third-party application integration and configuration"),
				@Tag(name = "Authentication", description = "User authentication and session management"),
				@Tag(name = "Browser Tree", description = "File and folder browser tree operations"),
				@Tag(name = "Bundle", description = "Content bundle management and deployment"),
				@Tag(name = "Cache Management", description = "Cache provider management and operations"),
				@Tag(name = "Categories", description = "Content categorization and taxonomy"),
				@Tag(name = "Cluster Management", description = "Cluster nodes and distributed system management"),
				@Tag(name = "Containers", description = "Container templates and management"),
				@Tag(name = "Content", description = "Endpoints for managing content and contentlets"),
				@Tag(name = "Content Analytics", description = "Content performance analytics and reporting"),
				@Tag(name = "Content Delivery", description = "Content delivery and rendering"),
				@Tag(name = "Content Report", description = "Content reporting and analytics"),
				@Tag(name = "Content Type", description = "Content type definitions and schema management"),
				@Tag(name = "Content Type Field", description = "Content type field definitions and configuration"),
				@Tag(name = "Data Integrity", description = "Data integrity checking and conflict resolution"),
				@Tag(name = "Environment", description = "Publishing environment management and configuration"),
				@Tag(name = "Experiment", description = "A/B testing and experimentation"),
				@Tag(name = "File Assets", description = "File asset management and download operations"),
				@Tag(name = "Folders", description = "Folder structure and organization"),
				@Tag(name = "Forms", description = "Form management and processing"),
				@Tag(name = "Health", description = "System health monitoring and diagnostics"),
				@Tag(name = "Internationalization", description = "Language management and localization"),
				@Tag(name = "JavaScript", description = "JavaScript execution and server-side scripting"),
				@Tag(name = "Job Queue", description = "Background job management and monitoring"),
				@Tag(name = "License", description = "License management and validation"),
				@Tag(name = "Maintenance", description = "System maintenance and administration operations"),
				@Tag(name = "Navigation", description = "Site navigation and menu management"),
				@Tag(name = "Notifications", description = "User notifications and alerts management"),
				@Tag(name = "OSGi Plugins", description = "OSGi plugin management and dynamic deployment"),
				@Tag(name = "Page", description = "Page management and rendering"),
				@Tag(name = "Permissions", description = "Permission management and access control"),
				@Tag(name = "Personalization", description = "Content personalization and persona management"),
				@Tag(name = "Portlets", description = "Portlet management and administration"),
				@Tag(name = "Publishing", description = "Content publishing and deployment operations"),
				@Tag(name = "Relationships", description = "Content relationship management"),
				@Tag(name = "Roles", description = "User role and permission management"),
				@Tag(name = "Rules Engine", description = "Business rules and conditional logic management"),
				@Tag(name = "SAML Authentication", description = "SAML SSO authentication and integration"),
				@Tag(name = "Search", description = "Content search and query operations"),
				@Tag(name = "Search Index", description = "Search index management and operations"),
				@Tag(name = "Sites", description = "Multi-site management and configuration"),
				@Tag(name = "System Configuration", description = "System configuration and company settings"),
				@Tag(name = "System Logging", description = "System logging configuration and management"),
				@Tag(name = "System Monitoring", description = "System monitoring and health checks"),
				@Tag(name = "System Storage", description = "Storage providers and data replication management"),
				@Tag(name = "Tags", description = "Content tagging and labeling"),
				@Tag(name = "Templates", description = "Template design and management"),
				@Tag(name = "Temporary Files", description = "Temporary file upload and management operations"),
				@Tag(name = "Users", description = "User account management and administration"),
				@Tag(name = "Variants", description = "Content variant management and personalization"),
				@Tag(name = "Widgets", description = "Widget development and rendering"),
				@Tag(name = "Workflow", description = "Content workflow and approval processes")
		}
)
public class DotRestApplication extends ResourceConfig {


	private static final Lazy<Boolean> ENABLE_TELEMETRY_FROM_CORE = Lazy.of(() ->
			Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_TELEMETRY_CORE_ENABLED, true));

	private static final WeakClassRegistry customClasses = new WeakClassRegistry();

	/**
	 * Default constructor that initializes the application with the current viable classes.
	 * This constructor is used when no specific dynamic classes are provided.
	 */
	public DotRestApplication() {
		this(customClasses.getViableClasses());
	}

	/**
	 * Constructor that initializes the application with a set of dynamic classes.
	 * @param dynamicClasses the set of classes to be included in the application configuration.
	 */
	public DotRestApplication(Set<Class<?>> dynamicClasses) {
		// Register providers before anything else
		registerEarlyProviders();

		// Then: Include the rest of the application configuration
		configureApplication(dynamicClasses);
	}

	/**
	 * Registers the {@link DotResourceMethodInvocationHandlerProvider} as the first
	 */
	private void registerEarlyProviders() {
		register(DotResourceMethodInvocationHandlerProvider.class);
		register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(DotResourceMethodInvocationHandlerProvider.class)
						.to(ResourceMethodInvocationHandlerProvider.class)
						.in(Singleton.class)
						.ranked(1); // Ensure this provider is used first
			}
		});
		Logger.debug(DotRestApplication.class, "MethodInvocationHandlerProvider provider registered");
	}

	private void configureApplication(Set<Class<?>> dynamicClasses) {
		final List<String> packages = new ArrayList<>(List.of(
				"com.dotcms.rest",
				"com.dotcms.contenttype.model.field",
				"com.dotcms.rendering.js",
				"com.dotcms.ai.rest",
				"com.dotcms.health",
				"io.swagger.v3.jaxrs2"));

		if (Boolean.TRUE.equals(ENABLE_TELEMETRY_FROM_CORE.get())) {
			packages.add(TelemetryResource.class.getPackageName());
		}

		register(MultiPartFeature.class)
				.register(JacksonJaxbJsonProvider.class)
				.registerClasses(dynamicClasses != null ? dynamicClasses : Collections.emptySet())
				.packages(packages.toArray(new String[0]));

		Logger.info(DotRestApplication.class,
				"DotRestApplication configured with " +
						(dynamicClasses != null ? dynamicClasses.size() : 0) + " dynamic classes");
	}

	/**
	 * Creates a fresh ResourceConfig instance with current viable classes
	 * This method ensures no pollution from previous configurations
	 */
	public static ResourceConfig createFreshConfig() {
		Set<Class<?>> currentClasses = customClasses.getViableClasses();
		Logger.info(DotRestApplication.class,
				"Creating fresh ResourceConfig with " + currentClasses.size() + " viable classes");

		return new DotRestApplication(currentClasses);
	}

	/**
	 * Creates a fresh ResourceConfig instance excluding specific classes
	 * Useful for removing problematic classes during reload
	 */
	public static ResourceConfig createFreshConfigExcluding(Set<Class<?>> classesToExclude) {
		Set<Class<?>> currentClasses = customClasses.getViableClasses();

		if (classesToExclude != null && !classesToExclude.isEmpty()) {
			currentClasses.removeAll(classesToExclude);
			Logger.info(DotRestApplication.class,
					"Creating fresh ResourceConfig excluding " + classesToExclude.size() +
							" classes, final count: " + currentClasses.size());
		}

		return new DotRestApplication(currentClasses);
	}

	/**
	 * Adds a class and reloads using a fresh configuration
	 */
	public static synchronized void addClass(final Class<?> clazz) {
		if (clazz == null) {
			return;
		}

		if (Boolean.TRUE.equals(ENABLE_TELEMETRY_FROM_CORE.get())
				&& clazz.getName().equalsIgnoreCase("com.dotcms.experience.TelemetryResource")) {
			Logger.warn(DotRestApplication.class, "Bypassing activation of Telemetry REST Endpoint from OSGi");
			return;
		}

		if (customClasses.addIfAbsent(clazz)) {
			Logger.info(DotRestApplication.class,
					" ::: Adding custom class: " + clazz.getName() + " and classloader: " + clazz.getClassLoader());

			// Use fresh config to avoid pollution
			reloadWithFreshConfig();
		}
	}

	/**
	 * Removes a class and reloads using a fresh configuration
	 * @param clazz
	 */
	public static synchronized void removeClass(Class<?> clazz) {
		if (clazz == null) {
			return;
		}

		CDI.current().getBeanManager().
				getBeans(clazz).forEach(bean -> {
					try {
						CDI.current().destroy(bean);
					}catch (Exception e) {
						Logger.error(DotRestApplication.class,
								"Error destroying bean of class: " + clazz.getName(), e);
					}
				});


		if (customClasses.remove(clazz)) {
			Logger.info(DotRestApplication.class,
					" ::: Removing custom class: " + clazz.getName());
			System.gc();
			// Use fresh config to avoid pollution
			reloadWithFreshConfig();
		}
	}

	/**
	 * Reloads the container with a completely fresh configuration
	 */
	private static void reloadWithFreshConfig() {
		final Optional<ContainerReloader> reloader = CDIUtils.getBean(ContainerReloader.class);
		if (reloader.isPresent()) {
			// Create completely fresh config instead of reusing potentially polluted one
			ResourceConfig freshConfig = createFreshConfig();
			reloader.get().reload(freshConfig);
		}
	}

	/**
	 * Emergency method to reload excluding problematic classes
	 */
	public static synchronized void reloadExcluding(Class<?>... problematicClasses) {
		Set<Class<?>> toExclude = Set.of(problematicClasses);
		final Optional<ContainerReloader> reloader = CDIUtils.getBean(ContainerReloader.class);
		if (reloader.isPresent()) {
			ResourceConfig cleanConfig = createFreshConfigExcluding(toExclude);
			reloader.get().reload(cleanConfig);
		}
	}
}