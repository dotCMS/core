package com.dotmarketing.portlets.templates.design.bean;

import com.dotmarketing.portlets.templates.design.util.PreviewTemplateUtil;

import java.util.List;

/**
 * Created by freddyrodriguez on 11/13/17.
 */
public class ContainerHolder {

    private boolean preview;
    private List<String> containers;

    public List<String> getContainers() {
        return containers;
    }

    public void setContainers(List<String> containers) {
        this.containers = containers;
    }

    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public String draw () throws Exception {

        StringBuffer sb = new StringBuffer();
        if ( this.containers != null ) {
            for ( String container: this.containers ) {

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
