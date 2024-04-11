package com.dotcms.rest.annotation;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.org.apache.struts.Globals;
import com.dotmarketing.cms.factories.PublicCompanyFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.struts.MultiMessageResources;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.WebAppPool;
import com.liferay.portal.util.WebKeys;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;

import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * This filter decorates the headers in addition to apply Request Commander associated to the annotations.
 * The annotations for the resource method involved it collected by {@link RequestFilterAnnotationFeature} and stored on the thread pool: {@link HttpServletRequestThreadLocal}
 * @author jsanca
 */
@Singleton
@Provider
public class RequestFilter implements ContainerRequestFilter {

    private final HttpServletRequestThreadLocal requestAnnotationThreadLocal;

    private final Map<Class, RequestFilterCommand> requestCommands =
            Map.of(
                    InitRequestRequired.class,
                    (final Annotation annotation,
                     final ContainerRequestContext requestContext,
                     final HttpServletRequest request) -> {

                        final HttpSession session = request.getSession();

                        // CTX
                        final ServletContext ctx =
                                Config.CONTEXT;
                        ServletContext portalCtx =
                                ctx.getContext(PropsUtil.get(PropsUtil.PORTAL_CTX));

                        if (portalCtx == null) {
                            portalCtx = ctx;
                        }

                        request.setAttribute(WebKeys.CTX, portalCtx);

                        // CTX_PATH variable
                        final String ctxPath =
                                (String)ctx.getAttribute(WebKeys.CTX_PATH);

                        if (null == portalCtx.getAttribute(WebKeys.CTX_PATH)) {
                            portalCtx.setAttribute(WebKeys.CTX_PATH, ctxPath);
                        }

                        if (null == session.getAttribute(WebKeys.CTX_PATH)) {
                            session.setAttribute(WebKeys.CTX_PATH, ctxPath);
                        }

                        request.setAttribute(WebKeys.CTX_PATH, ctxPath);

                        // company
                        final String companyId = (UtilMethods.isSet(ctx.getInitParameter("company_id")))?
                                ctx.getInitParameter("company_id"):PublicCompanyFactory.getDefaultCompanyId();

                        ctx.setAttribute(WebKeys.COMPANY_ID, companyId);

                        // messages
                        final MultiMessageResources messageResources =
                                (MultiMessageResources) ctx.getAttribute(Globals.MESSAGES_KEY);

                        messageResources.setServletContext(ctx);
                        WebAppPool.put(companyId, Globals.MESSAGES_KEY, messageResources);
                    }
            );


    public RequestFilter() {
        this(HttpServletRequestThreadLocal.INSTANCE);
    }

    @VisibleForTesting
    public RequestFilter(final HttpServletRequestThreadLocal requestAnnotationThreadLocal) {
        this.requestAnnotationThreadLocal = requestAnnotationThreadLocal;
    }

    @Override
    public void filter(final ContainerRequestContext containerRequestContext) throws IOException {

        final HttpServletRequest request =
                this.requestAnnotationThreadLocal.getRequest();
        Annotation []        annotations = null;

        if (null != request) {

            annotations =
                    this.getMethodResourceAnnotations(containerRequestContext);

            if (null != annotations) {

                for (Annotation a : annotations) {

                    this.processAnnotation(containerRequestContext, request, a);
                }
            }
        }
    } // filter.

    private void processAnnotation(final ContainerRequestContext containerRequestContext,
                                   final HttpServletRequest request,
                                   final Annotation annotation) {

        final Class [] classes =
            (annotation instanceof Proxy)? // if it is a proxy should use the interfaces instead since the class is a proxy.
                    annotation.getClass().getInterfaces():
                    new Class[] { annotation.getClass() };

        for (Class clazz : classes) {

            if (this.requestCommands.containsKey(clazz)) {

                this.requestCommands.get(clazz).execute
                        (annotation, containerRequestContext, request);
            }
        }
    }

    private Annotation [] getMethodResourceAnnotations (final ContainerRequestContext containerRequestContext) {

        ContainerRequest  containerRequest  = null;
        UriRoutingContext uriRoutingContext = null;
        ResourceInfo      resourceInfo      = null;
        Annotation []     annotations       = null;

        if (containerRequestContext instanceof ContainerRequest) {

            containerRequest = (ContainerRequest)containerRequestContext;

            if (null != containerRequest.getUriInfo()
                    && containerRequest.getUriInfo() instanceof UriRoutingContext) {

                uriRoutingContext = (UriRoutingContext)containerRequest.getUriInfo();

                if (null != uriRoutingContext.getEndpoint()
                        && uriRoutingContext.getEndpoint() instanceof ResourceInfo) {

                    resourceInfo = (ResourceInfo)uriRoutingContext.getEndpoint();
                    annotations  = resourceInfo.getResourceMethod().getDeclaredAnnotations();

                }
            }
        }

        return annotations;
    } // getMethodResourceAnnotations.
} // E:O:F:RequestFilter.
