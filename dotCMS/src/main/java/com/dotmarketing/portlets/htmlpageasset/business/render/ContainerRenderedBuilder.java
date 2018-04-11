package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRenderedBuilder;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Builder of {@link ContainerRendered}
 */
public class ContainerRenderedBuilder {

    private final UserAPI userAPI;
    private final TemplateAPI templateAPI;
    private final ContainerAPI containerAPI;

    private ContainerRenderedBuilder() {
        userAPI = APILocator.getUserAPI();
        templateAPI = APILocator.getTemplateAPI();
        containerAPI = APILocator.getContainerAPI();
    }

    public static ContainerRenderedBuilder get() {
        return new ContainerRenderedBuilder();
    }

    public List<ContainerRendered> getContainers(final Template template)
            throws DotSecurityException, DotDataException {
        final User systemUser = this.userAPI.getSystemUser();
        final List<Container> templateContainers = this.templateAPI.getContainersInTemplate(template, systemUser,
                false);

        final List<ContainerRendered> containers = new LinkedList<>();
        for (final Container container : templateContainers) {
            final List<ContainerStructure> containerStructures = this.containerAPI.getContainerStructures(container);
            containers.add(new ContainerRendered(container, containerStructures));
        }

        return containers;
    }

    public List<ContainerRendered> getContainersRendered(final Template template, final Context velocityContext,
                                                         PageMode mode )

            throws DotSecurityException, DotDataException {

        List<ContainerRendered> containers = this.getContainers(template);
        renderContainer(containers, velocityContext, mode);
        return containers;
    }

    private void renderContainer(final List<ContainerRendered> containers, final Context velocityContext, PageMode mode )
            throws DotSecurityException, DotDataException {


        for (final ContainerRendered containerView : containers) {
            final Container container = containerView.getContainer();
            VelocityResourceKey key = new VelocityResourceKey(container, mode);

            try {

                final String rendered = VelocityUtil.mergeTemplate(key.path, velocityContext);
                containerView.setRendered(rendered);
            } catch (Exception e) {
                throw new DotDataException(String.format("Container '%s' could not be " +
                        "rendered via " + "Velocity.", container.getIdentifier()), e);
            }
        }
    }
}
