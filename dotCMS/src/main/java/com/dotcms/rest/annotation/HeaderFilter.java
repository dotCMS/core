package com.dotcms.rest.annotation;

import com.dotcms.repackage.javax.annotation.Priority;
import com.dotcms.repackage.javax.inject.Singleton;
import com.dotcms.repackage.javax.ws.rs.Priorities;
import com.dotcms.repackage.javax.ws.rs.container.ContainerRequestContext;
import com.dotcms.repackage.javax.ws.rs.container.ContainerResponseContext;
import com.dotcms.repackage.javax.ws.rs.container.ContainerResponseFilter;
import com.dotcms.repackage.javax.ws.rs.core.MultivaluedMap;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * This decorator reads the annotations on the resources and includes header based on it based on them.
 * It is valid for response and request stuff.
 *
 * @author jsanca
 */
@Singleton
@Priority(Priorities.HEADER_DECORATOR)
public class HeaderFilter implements ContainerResponseFilter {

	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

	public static final String CACHE_CONTROL = "Cache-Control";
	public static final String NO_CACHE_CONTROL = "no-cache, no-store, must-revalidate";
	public static final String PRAGMA = "Pragma";
	public static final String NO_CACHE = "no-cache";
	public static final String EXPIRES = "Expires";
	public static final String EXPIRES_DEFAULT_DATE = "Mon, 26 Jul 1997 05:00:00 GMT";

	private final Map<Class, HeaderDecorator> responseHeaderDecorators =
			map(
					AccessControlAllowOrigin.class,
					(Annotation annotation, MultivaluedMap<String, Object> headers) -> {

						headers.add(ACCESS_CONTROL_ALLOW_ORIGIN,
								AccessControlAllowOrigin.class.cast(annotation).value());
					},

					Cacheable.class,
					(Annotation annotation, MultivaluedMap<String, Object> headers) -> {

						headers.add(CACHE_CONTROL,
								Cacheable.class.cast(annotation).cc());
					},

					NoCache.class,
					(Annotation annotation, MultivaluedMap<String, Object> headers) -> {

						headers.add(CACHE_CONTROL, NO_CACHE_CONTROL); // HTTP 1.1.
						headers.add(PRAGMA, NO_CACHE); // HTTP 1.0.
						headers.add(EXPIRES, EXPIRES_DEFAULT_DATE); // Proxies.
					},
					Deprecated.class,
					(Annotation annotation, MultivaluedMap<String, Object> headers) -> {
						// "This API is deprecated. Please refer to the following end-point: ..."
						headers.add("Deprecated API ", "This is an old API, will be not available in the future");
					}

			);

    @Override
    public void filter(final ContainerRequestContext requestContext,
					   final ContainerResponseContext responseContext) throws IOException {

        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
		for (Annotation a : responseContext.getEntityAnnotations()) {

			if (this.responseHeaderDecorators.containsKey(a.getClass())) {

				this.responseHeaderDecorators.get(a.getClass()).decorate
						(a, headers);
			}
		}
    } // filter.

} // E:O:F:HeaderFilter.
 
