package com.dotcms.rest.config;

import com.dotcms.cdi.CDIUtils;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import io.swagger.v3.jaxrs2.integration.resources.AcceptHeaderOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
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
				@Tag(name = "Workflow"),
				@Tag(name = "Page"),
				@Tag(name = "Content Type"),
				@Tag(name = "Content Delivery"),
				@Tag(name = "Bundle"),
				@Tag(name = "Navigation"),
				@Tag(name = "Experiment"),
				@Tag(name = "Content Report")
		}
)
public class DotRestApplication extends ResourceConfig {

	public DotRestApplication() {

		register(MultiPartFeature.class).
		register(JacksonJaxbJsonProvider.class).
		registerClasses(customClasses.keySet()).
		packages(
		  "com.dotcms.rest",
		  "com.dotcms.contenttype.model.field",
		  "com.dotcms.rendering.js",
		  "com.dotcms.ai.rest",
		  "io.swagger.v3.jaxrs2"
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
	public static synchronized void addClass(Class<?> clazz) {
		if(clazz==null){
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
			reloader.ifPresent(ContainerReloader::reload);
		}
	}

}
