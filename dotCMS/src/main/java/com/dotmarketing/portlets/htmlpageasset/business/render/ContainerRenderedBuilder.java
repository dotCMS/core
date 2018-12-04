package com.dotmarketing.portlets.htmlpageasset.business.render;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.velocity.context.Context;

import com.beust.jcommander.internal.Maps;
import com.dotcms.rendering.velocity.services.VelocityResourceKey;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.ContainerAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.VelocityUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

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

    public Collection<ContainerRendered> getContainers(final HTMLPageAsset page, final PageMode mode)
            throws DotSecurityException, DotDataException {
        return getContainersRendered(page, null, mode);
    }

    public Collection<ContainerRendered> getContainersRendered(HTMLPageAsset page, final Context velocityContext,
                                                         PageMode mode)

            throws DotSecurityException, DotDataException {

        
        Template template =(mode.showLive) 
                ?  templateAPI.findLiveTemplate(page.getTemplateId(), APILocator.systemUser(), false) 
                :  templateAPI.findWorkingTemplate(page.getTemplateId(), APILocator.systemUser(), false) ;


        final TemplateLayout layout =  DotTemplateTool.themeLayout(template.getInode());
        final List<ContainerUUID> containersUUID = this.templateAPI.getContainersUUID(layout);

        final Map<String, List<ContainerUUID>> groupByContainerID =
                containersUUID.stream().collect(Collectors.groupingBy(ContainerUUID::getIdentifier));

        Map<String,List<Contentlet>> conMap = Maps.newHashMap();
        if(velocityContext!=null) {
            for(String key : groupByContainerID.keySet()) {
                for(ContainerUUID uuid :groupByContainerID.get(key) ) {
                    final String[] consStr = (String[]) velocityContext.get("contentletList" + uuid.getIdentifier() + uuid.getUUID());
                    List<Contentlet> contentlets = Arrays.asList(consStr).stream().map(conId -> {
                        try {
                            return APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(conId);
                        } catch (Exception e) {
                            throw new DotStateException(e);
                        }
                    }).filter(Objects::nonNull).collect(Collectors.toList());
                    conMap.put( "uuid-" + uuid.getUUID(), contentlets);
                }
            }
        }


        
        

        return groupByContainerID.entrySet().stream().map(entry -> {
            final String containerId = entry.getKey();
            try {
                final Container container = getContainer(mode, containerId);

                final List<ContainerStructure> containerStructures = this.containerAPI.getContainerStructures(container);

                Map<String, String> containersRendered = velocityContext != null ?
                        render(velocityContext, mode, entry.getValue(), container): null;
                        
                        
                        
                        

                return new ContainerRendered(container, containerStructures, containersRendered,conMap);
            } catch (Exception e) {
                // if the container does not exists or is not valid for the mode, returns null to be filtrated
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
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
                        return VelocityUtil.getInstance().mergeTemplate(key.path, velocityContext);
                    } catch (Exception e) {
                        Logger.warn(this,String.format("Container '%s' could not be " +
                                "rendered via " + "Velocity.", container.getIdentifier()), e);
                        return StringPool.BLANK;
                    }
                }));
    }
}
