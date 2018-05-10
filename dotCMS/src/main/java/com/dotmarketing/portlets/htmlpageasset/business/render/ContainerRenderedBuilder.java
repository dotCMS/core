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
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.VelocityUtil;

import com.google.common.collect.Table;
import com.liferay.portal.model.User;
import org.apache.velocity.context.Context;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public List<ContainerRendered> getContainers(final Template template)
            throws DotSecurityException, DotDataException {
        final User systemUser = this.userAPI.getSystemUser();
        final List<Container> templateContainers = this.templateAPI.getContainersInTemplate(template, systemUser,
                false);

        final List<ContainerRendered> containers = new ArrayList<>();
        for (final Container container : templateContainers) {
            final List<ContainerStructure> containerStructures = this.containerAPI.getContainerStructures(container);
            containers.add(new ContainerRendered(container, containerStructures));
        }

        return containers;
    }

    public List<ContainerRendered> getContainers(final HTMLPageAsset page,PageMode mode)
            throws DotSecurityException, DotDataException {
        final User systemUser = this.userAPI.getSystemUser();
        Table<String, String, Set<String>> pageContents = APILocator.getMultiTreeAPI().getPageMultiTrees(page, mode.showLive);
        final List<ContainerRendered> containers = new ArrayList<>();
        
        if (!pageContents.isEmpty()) {
            for (final String containerId : pageContents.rowKeySet()) {
                for (final String uniqueId : pageContents.row(containerId)
                    .keySet()) {

                    final Container container = (mode.showLive) ? (Container) APILocator.getVersionableAPI()
                            .findLiveVersion(containerId, systemUser, false)
                            : (Container) APILocator.getVersionableAPI()
                            .findWorkingVersion(containerId, systemUser, false);
                            
                            final List<ContainerStructure> containerStructures = this.containerAPI.getContainerStructures(container);
                            
                            containers.add(new ContainerRendered(container, containerStructures, uniqueId));
        
                }
            }
        }
                            
        return containers;
    }

    public List<ContainerRendered> getContainersRendered(final HTMLPageAsset page, final Context velocityContext,
                                                         PageMode mode )

            throws DotSecurityException, DotDataException {

        
        List<ContainerRendered> containers = this.getContainers(page, mode);
        return  renderContainer(containers, velocityContext, mode);

    }

    private List<ContainerRendered> renderContainer(final List<ContainerRendered> containers, final Context velocityContext, PageMode mode )
            throws DotSecurityException, DotDataException {

        
        final Map<String, ContainerRendered> uniqueContainers = 
                containers.stream().collect(Collectors.toMap(
                        containerRendered -> containerRendered.getContainer().getIdentifier(),
                        containerRendered -> containerRendered,
                        (c1, c2) -> c1
                ));

        for (final ContainerRendered containerView : containers) {
            final Container container = containerView.getContainer();
            VelocityResourceKey key = new VelocityResourceKey(container,containerView.getUuid(), mode);

            try {

                final String rendered = VelocityUtil.mergeTemplate(key.path, velocityContext);
                uniqueContainers.get(container.getIdentifier()).addRender(containerView.getUuid(), rendered);
            } catch (Exception e) {
                throw new DotDataException(String.format("Container '%s' could not be " +
                        "rendered via " + "Velocity.", container.getIdentifier()), e);
            }
        }
        return uniqueContainers.values().stream().collect(Collectors.toList());
    }


}
