package com.dotmarketing.portlets.templates.design.bean;

import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.templates.design.util.PreviewTemplateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.dotmarketing.portlets.templates.design.util.DesignTemplateUtil.getContainerIdentifierOrPath;
/**
 * it is block that can contain {@link com.dotmarketing.portlets.containers.model.Container}
 */
public class ContainerHolder implements Serializable{

    private static final String PARSE_CONTAINER_STATEMENT =  "#parseContainer('%s', '%s', true)";
    private boolean preview;
    private final List<ContainerUUID> containers;

    @JsonCreator
    public ContainerHolder(@JsonProperty("containers")  final List<ContainerUUID> containers) {
        this.containers = this.checkFileAssetContainers(containers);
    }

    private List<ContainerUUID> checkFileAssetContainers(final List<ContainerUUID> containers) {

        final List<ContainerUUID> containerUUIDS = new ArrayList<>();

        for (final ContainerUUID containerUUID : containers) {

            String containerIdOrPath = containerUUID.getIdentifier();
            try {

                containerIdOrPath = getContainerIdentifierOrPath(containerUUID.getIdentifier());
            } catch (DotDataException e) {
                Logger.error(this, e.getMessage(), e);
            }

            containerUUIDS.add(new ContainerUUID(containerIdOrPath, containerUUID.getUUID(), containerUUID.getHistoryUUIDs()));
        }

        return containerUUIDS;
    }

    public List<ContainerUUID> getContainers() {
        return containers;
    }

    public boolean isPreview() {
        return preview;
    }

    public void setPreview(final boolean preview) {
        this.preview = preview;
    }

    public String draw () throws Exception {

        final StringBuilder sb = new StringBuilder();

        if ( this.containers != null ) {
            for ( final ContainerUUID container: this.containers ) {

                if ( this.preview ) {
                    sb.append( PreviewTemplateUtil.getMockBodyContent() );
                } else {
                    final String uuid = StringUtils.getOrDefault(container.getUUID(), () -> ParseContainer.DEFAULT_UUID_VALUE);
                    sb.append( String.format(PARSE_CONTAINER_STATEMENT, container.getIdentifier(), uuid) );
                }
            }
        }

        return sb.toString();
    }
    @Override
    public String toString() {
       try {
           return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }
}


