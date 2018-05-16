package com.dotmarketing.portlets.htmlpageasset.business.render;

import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRenderedBuilder;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Builder of {@link ContainerRendered}
 */
public class ContainerRenderedBuilder {

    private final UserAPI userAPI;
    private final TemplateAPI templateAPI;
    private final ContainerAPI containerAPI;

    public ContainerRenderedBuilder() {
        userAPI = APILocator.getUserAPI();
        templateAPI = APILocator.getTemplateAPI();
        containerAPI = APILocator.getContainerAPI();
    }

    public Collection<ContainerRendered> getContainers(final Template template, final PageMode mode)
            throws DotSecurityException, DotDataException {
        return getContainersRendered(template, null, mode);
    }

    public Collection<ContainerRendered> getContainersRendered(final Template template, final Context velocityContext,
                                                         PageMode mode)

            throws DotSecurityException, DotDataException {

        if (!template.isDrawed()) {
            return Collections.EMPTY_LIST;
        }

        final TemplateLayout layout =  DotTemplateTool.themeLayout(template.getInode());
        final List<ContainerUUID> containersUUID = this.templateAPI.getContainersUUID(layout);

        final Map<String, List<ContainerUUID>> groupByContainerID =
                containersUUID.stream().collect(Collectors.groupingBy(ContainerUUID::getIdentifier));

        return groupByContainerID.entrySet().stream().map(entry -> {
            final String containerId = entry.getKey();
            try {
                final Container container = getContainer(mode, containerId);

                final List<ContainerStructure> containerStructures = this.containerAPI.getContainerStructures(container);

                Map<String, String> containersRendered = velocityContext != null ?
                        render(velocityContext, mode, entry.getValue(), container): null;

                return new ContainerRendered(container, containerStructures, containersRendered);
            } catch (DotDataException | DotSecurityException e) {
                Logger.error(ContainerRenderedBuilder.class, e.getMessage());
                throw new DotRuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    private Container getContainer(PageMode mode, String containerId) throws DotDataException, DotSecurityException {
        final User systemUser = this.userAPI.getSystemUser();

        return (mode.showLive) ?
                    (Container) APILocator.getVersionableAPI().findLiveVersion(containerId, systemUser, false) :
                    (Container) APILocator.getVersionableAPI().findWorkingVersion(containerId, systemUser, false);
    }

    private Map<String, String> render(Context velocityContext, PageMode mode, Collection <ContainerUUID> uuids,
                                         Container container) {
        return uuids.stream()
                .map(containerUUID -> containerUUID.getUUID())
                .collect(Collectors.toMap(uuid -> "uuid-" + uuid, uuid -> {
                    final VelocityResourceKey key = new VelocityResourceKey(container, uuid, mode);

                    try {
                        return VelocityUtil.mergeTemplate(key.path, velocityContext);
                    } catch (Exception e) {
                        throw new DotRuntimeException(String.format("Container '%s' could not be " +
                                "rendered via " + "Velocity.", container.getIdentifier()), e);
                    }
                }));
    }
}
