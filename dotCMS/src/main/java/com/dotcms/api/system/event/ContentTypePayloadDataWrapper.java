package com.dotcms.api.system.event;

import com.dotmarketing.portlets.structure.model.Structure;

/**
 * It is a wrapper to store the Content Type's action url when a event is push
 */
public class ContentTypePayloadDataWrapper implements DataWrapper<Structure> {
    private String actionUrl;
    private Structure structure;

    public ContentTypePayloadDataWrapper(String actionUrl, Structure structure) {
        this.actionUrl = actionUrl;
        this.structure = structure;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    @Override
    public Structure getData() {
        return structure;
    }
}
