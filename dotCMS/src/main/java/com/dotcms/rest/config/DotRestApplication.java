package com.dotcms.rest.config;

import com.dotcms.cdi.CDIUtils;
import com.dotcms.telemetry.rest.TelemetryResource;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.Lazy;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.ext.cdi1x.internal.CdiComponentProvider;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.internal.ResourceMethodInvocationHandlerProvider;

import javax.inject.Singleton;
import javax.ws.rs.ApplicationPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
			Config.getBooleanProperty("FEATURE_FLAG_TELEMETRY_CORE_ENABLED", false));

	public DotRestApplication() {

		//Include the rest of the application configuration
		configureApplication();
	}


	private void configureApplication() {
		final List<String> packages = new ArrayList<>(List.of(
				"com.dotcms.rest",
				"com.dotcms.contenttype.model.field",
				"com.dotcms.rendering.js",
				"com.dotcms.ai.rest",
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
		if (Boolean.TRUE.equals(customClasses.computeIfAbsent(clazz,c -> true))) {
			final Optional<ContainerReloader> reloader = CDIUtils.getBean(ContainerReloader.class);
            reloader.ifPresent(ContainerReloader::reload);
		}
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
