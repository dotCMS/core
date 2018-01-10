package com.dotmarketing.portlets.templates.design.bean;

import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotmarketing.portlets.templates.design.util.PreviewTemplateUtil;
import com.dotmarketing.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * it is block that can contain {@link com.dotmarketing.portlets.containers.model.Container}
 */
public class ContainerHolder {

    private static final String PARSE_CONTAINER_STATEMENT =  "#parseContainer('%s', '%s')";
    private boolean preview;
    private final List<ContainerUUID> containers;

    @JsonCreator
    public ContainerHolder(@JsonProperty("containers")  final List<ContainerUUID> containers) {
        this.containers = containers;
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
                    String uuid = StringUtils.getOrDefault(container.getUUID(), () -> ParseContainer.DEFAULT_UUID_VALUE);
                    sb.append( String.format(PARSE_CONTAINER_STATEMENT, container.getIdentifier(), uuid) );
                }
            }
        }

        return sb.toString();
    }
}


