package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ErrorEntity;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentletStylingErrorEntity extends ErrorEntity {

    private final String contentletId;
    private final String uuid;
    private final String containerId;

    public ContentletStylingErrorEntity(String code, String message, String contentletId,
            String uuid, String containerId) {
        super(code, message);
        this.contentletId = contentletId;
        this.containerId = containerId;
        this.uuid = uuid;
    }

    public String getContainerId() {
        return containerId;
    }

    public String getUuid() {
        return uuid;
    }

    public String getContentletId() {
        return contentletId;
    }
}
