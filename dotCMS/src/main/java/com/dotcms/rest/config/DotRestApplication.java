package com.dotcms.rest.config;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.telemetry.rest.TelemetryResource;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
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
				@Tag(name = "API Token", description = "API token management and authentication", 
					externalDocs = @ExternalDocumentation(description = "Additional API token information",
						url = "https://www.dotcms.com/docs/latest/rest-api-authentication#APIToken")),
				@Tag(name = "Apps", description = "Third-party application integration and configuration"),
				@Tag(name = "Authentication", description = "User authentication and session management",
					externalDocs = @ExternalDocumentation(description = "Additional Authentication API information",
						url = "https://www.dotcms.com/docs/latest/rest-api-authentication")),
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
				@Tag(name = "Content Type", description = "Content type definitions and schema management",
					externalDocs = @ExternalDocumentation(description = "Additional Content Type API information",
						url = "https://www.dotcms.com/docs/latest/content-type-api")),
				@Tag(name = "Content Type Field", description = "Content type field definitions and configuration"),
				@Tag(name = "Data Integrity", description = "Data integrity checking and conflict resolution"),
				@Tag(name = "Environment", description = "Publishing environment management and configuration"),
				@Tag(name = "Experiments", description = "A/B testing and experimentation management"),
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
				@Tag(name = "Page",
						description = "Endpoints that operate on pages",
						externalDocs = @ExternalDocumentation(description = "Additional Page API information",
								url = "https://www.dotcms.com/docs/latest/page-rest-api-layout-as-a-service-laas")
				),
				@Tag(name = "Permissions", description = "Permission management and access control"),
				@Tag(name = "Personas", description = "Content persona management and targeting"),
				@Tag(name = "Personalization", description = "Content personalization and persona management"),
				@Tag(name = "Portlets", description = "Portlet management and administration"),
				@Tag(name = "Publishing", description = "Content publishing and deployment operations"),
				@Tag(name = "Push Publishing", description = "Remote content publishing and synchronization"),
				@Tag(name = "Relationships", description = "Content relationship management"),
				@Tag(name = "Roles", description = "User role and permission management"),
				@Tag(name = "Rules Engine", description = "Business rules and conditional logic management"),
				@Tag(name = "SAML Authentication", description = "SAML SSO authentication and integration"),
				@Tag(name = "Search", description = "Content search and query operations"),
				@Tag(name = "Search Index", description = "Search index management and operations"),
				@Tag(name = "Sites", description = "Multi-site management and configuration"),
				@Tag(name = "System", description = "System-level operations and Redis management"),
				@Tag(name = "System Configuration", description = "System configuration and company settings"),
				@Tag(name = "System Logging", description = "System logging configuration and management"),
				@Tag(name = "System Monitoring", description = "System monitoring and health checks"),
				@Tag(name = "System Storage", description = "Storage providers and data replication management"),
				@Tag(name = "TailLog", description = "Server log file monitoring and real-time viewing"),
				@Tag(name = "Tags", description = "Content tagging and labeling"),
				@Tag(name = "Templates", description = "Template design and management"),
				@Tag(name = "Temporary Files", description = "Temporary file upload and management operations"),
				@Tag(name = "Testing", description = "Testing utilities and development endpoints"),
				@Tag(name = "Themes", description = "Theme design and management"),
				@Tag(name = "Tool Groups", description = "Administrative tool group management"),
				@Tag(name = "Users", description = "User account management and administration"),
				@Tag(name = "Variants", description = "Content variant management and personalization"),
				@Tag(name = "Versionables", description = "Version control and content archiving"),
				@Tag(name = "VTL", description = "Velocity Template Language execution and rendering"),
				@Tag(name = "Web Assets", description = "Web asset management and operations"),
				@Tag(name = "Widgets", description = "Widget development and rendering"),
				@Tag(name = "Workflow",
						description = "Endpoints that perform operations related to workflows.",
						externalDocs = @ExternalDocumentation(description = "Additional Workflow API information",
								url = "https://www.dotcms.com/docs/latest/workflow-rest-api")
				)
		}
)
public class DotRestApplication extends ResourceConfig {


	private static final Lazy<Boolean> ENABLE_TELEMETRY_FROM_CORE = Lazy.of(() ->
			Config.getBooleanProperty(FeatureFlagName.FEATURE_FLAG_TELEMETRY_CORE_ENABLED, true));

	public DotRestApplication() {

		// Include the rest of the application configuration
		configureApplication();
	}


	private void configureApplication() {
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
		.registerClasses(customClasses.keySet())
		.packages(packages.toArray(new String[0])
		);
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

		// Check if class with same name already exists (by name, not Class object identity)
		// This is necessary because OSGI can reload the same class with a different classloader,
		// resulting in a different Class<?> object with the same name
		final boolean alreadyRegistered = customClasses.keySet().stream()
				.anyMatch(registeredClass -> registeredClass.getName().equals(clazz.getName()));

		if (alreadyRegistered) {
			Logger.warn(DotRestApplication.class,
				"REST resource class already registered, skipping: " + clazz.getName());
			return;
		}

		// Add the new class and reload
		customClasses.put(clazz, true);
		Logger.info(DotRestApplication.class,
			"Registering new REST resource class: " + clazz.getName());
		final Optional<ContainerReloader> reloader = CDIUtils.getBean(ContainerReloader.class);
		reloader.ifPresent(ContainerReloader::reload);
	}

	/**
	 * removes a class and reloads
	 * @param clazz
	 */
	public static synchronized void removeClass(Class<?> clazz) {
		if(clazz==null){
			return;
		}
		if(customClasses.remove(clazz) != null){
			final Optional<ContainerReloader> reloader = CDIUtils.getBean(ContainerReloader.class);
			try {
				CDIUtils.cleanUpCache();
			}catch (Exception e){
				Logger.error(DotRestApplication.class, "Error cleaning up cache", e);
			}
			reloader.ifPresent(ContainerReloader::reload);
		}
	}

}
