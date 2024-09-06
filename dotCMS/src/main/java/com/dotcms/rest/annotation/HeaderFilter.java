package com.dotcms.rest.annotation;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.ext.Provider;

/**
 * This decorator reads the annotations on the resources and includes header based on it based on them.
 * It is valid for response and request stuff.
 *
 * @author jsanca
 */
@Singleton
@Priority(Priorities.HEADER_DECORATOR)
@Provider
public class HeaderFilter implements ContainerResponseFilter {

	private final PermissionsUtil  permissionsUtil			= PermissionsUtil.getInstance();
	private final WebResource webResource					= new WebResource();
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN  = "Access-Control-Allow-Origin";
	public static final String CACHE_CONTROL    			= "Cache-Control";
	public static final String NO_CACHE_CONTROL 			= "no-cache, no-store, must-revalidate";
	public static final String PRAGMA 						= "Pragma";
	public static final String NO_CACHE 					= "no-cache";
	public static final String EXPIRES 						= "Expires";
	public static final String EXPIRES_DEFAULT_DATE 		= "Mon, 26 Jul 1997 05:00:00 GMT";

	private final Map<Class, HeaderDecorator> responseHeaderDecorators =
			Map.of(
					AccessControlAllowOrigin.class,
					(final Annotation annotation, final MultivaluedMap<String, Object> headers, final ContainerRequestContext requestContext,
					 final ContainerResponseContext responseContext) -> {

						headers.add(ACCESS_CONTROL_ALLOW_ORIGIN,
								AccessControlAllowOrigin.class.cast(annotation).value());
					},
					Cacheable.class,
					(final Annotation annotation, final MultivaluedMap<String, Object> headers, final ContainerRequestContext requestContext,
					 final ContainerResponseContext responseContext) -> {

						headers.add(CACHE_CONTROL,
								Cacheable.class.cast(annotation).cc());
					},
					IncludePermissions.class,
					this::applyPermissionable,

					NoCache.class,
					(final Annotation annotation, final MultivaluedMap<String, Object> headers, final ContainerRequestContext requestContext,
					 final ContainerResponseContext responseContext) -> {

						headers.add(CACHE_CONTROL, NO_CACHE_CONTROL); // HTTP 1.1.
						headers.add(PRAGMA, NO_CACHE); // HTTP 1.0.
						headers.add(EXPIRES, EXPIRES_DEFAULT_DATE); // Proxies.
					}
			);

    @Override
    public void filter(final ContainerRequestContext requestContext,
					   final ContainerResponseContext responseContext) throws IOException {

        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();

		for (final Annotation annotation : responseContext.getEntityAnnotations()) {

			HeaderDecorator headerDecorator = null;
			if (Proxy.isProxyClass(annotation.getClass())) {

				final Optional<Class> clazzFound = this.responseHeaderDecorators.keySet().stream()
						.filter(clazz -> clazz.isAssignableFrom(annotation.getClass())).findFirst();

				if (clazzFound.isPresent()) {

					headerDecorator = this.responseHeaderDecorators.get(clazzFound.get());
				}
			} else {
				if (this.responseHeaderDecorators.containsKey(annotation.getClass())) {

					headerDecorator = this.responseHeaderDecorators.get(annotation.getClass());
				}
			}

			if (null != headerDecorator) {

				headerDecorator.decorate(annotation, headers, requestContext, responseContext);
			}
		}
    } // filter.

	private void applyPermissionable(final Annotation annotation,
									final MultivaluedMap<String, Object> headers,
									final ContainerRequestContext requestContext,
									final ContainerResponseContext responseContext) {

    	final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();

    	if (null != request && "true".equalsIgnoreCase
				(request.getParameter(IncludePermissions.class.cast(annotation).queryParam())) && responseContext.hasEntity()) {

			final Object entity = responseContext.getEntity();

			if (entity instanceof ResponseEntityView
					&& ResponseEntityView.class.cast(entity).getEntity() instanceof Permissionable) {

				try {

					final User user = this.webResource.getCurrentUser(HttpServletRequestThreadLocal.INSTANCE.getRequest(),
							Collections.emptyMap(), false);

					if (null != user) {

						final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(entity);
						responseContext.setEntity(new ResponseEntityView
								(responseEntityView.getEntity(), responseEntityView.getErrors(),
										responseEntityView.getMessages(), responseEntityView.getI18nMessagesMap(),
										Arrays.asList(this.permissionsUtil.getPermissionsArray(Permissionable.class.cast(responseEntityView.getEntity()), user))));
					}
				} catch (Exception e) {
					Logger.debug(this, e.getMessage(), e);
				}
			}
		}
	} // applyPermissionable.
} // E:O:F:HeaderFilter.
 
