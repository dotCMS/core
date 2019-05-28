package com.dotcms.rest.annotation;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedMap;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * Encapsulates a decorator for a header annotation
 * @author jsanca
 */
public interface HeaderDecorator extends Serializable {

    /**
     * Based on the annotation meta info, decorates the headers
     * @param annotation {@link Annotation}
     * @param headers {@link MultivaluedMap}
     * @param requestContext {@link ContainerRequestContext}
     * @param responseContext {@link ContainerResponseContext}
     */
    void decorate (final Annotation annotation, final MultivaluedMap<String, Object> headers, final ContainerRequestContext requestContext,
                   final ContainerResponseContext responseContext);

} // E:O:F:HeaderDecorator.
