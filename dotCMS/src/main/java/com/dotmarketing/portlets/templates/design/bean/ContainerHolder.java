package com.dotmarketing.portlets.templates.design.bean;

import com.dotmarketing.portlets.templates.design.util.PreviewTemplateUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * it is block that can contain {@link com.dotmarketing.portlets.containers.model.Container}
 */
public class ContainerHolder {

    private boolean preview;
    private List<String> containers;

    @JsonCreator
    public ContainerHolder(@JsonProperty("containers")  final List<String> containers) {
        this.containers = containers;
    }

    public List<String> getContainers() {
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
            for ( final String container: this.containers ) {

                if ( this.preview ) {
                    sb.append( PreviewTemplateUtil.getMockBodyContent() );
                } else {
                    sb.append( "#parseContainer('" ).append( container ).append( "')" );
                }
            }
        }

        return sb.toString();
    }
}
