package com.dotcms.rest.annotation;

import com.dotcms.repackage.javax.ws.rs.container.ContainerRequestContext;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * A {@link RequestFilterCommand} is registered (and associated to an annotation) on the {@link RequestFilter}
 * in order to do some command when a rest method resources is annotated with some desire functionality
 * @author jsanca
 */
public interface RequestFilterCommand extends Serializable {

    /**
     * Execute the Request Filter Command
     * @param annotation {@link Annotation}
     * @param containerRequestContext {@link ContainerRequestContext}
     * @param request {@link HttpServletRequest}
     */
    public void execute (final Annotation annotation, final ContainerRequestContext containerRequestContext,
                         final HttpServletRequest request);

} // E:O:F:RequestFilterCommand.
