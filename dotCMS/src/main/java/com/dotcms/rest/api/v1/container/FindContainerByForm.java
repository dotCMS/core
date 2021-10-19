package com.dotcms.rest.api.v1.container;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.rest.api.Validated;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.util.HttpRequestDataUtil;
import com.dotmarketing.util.SecurityLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = FindContainerByForm.Builder.class)
public class FindContainerByForm extends Validated {

    private final String containerId;

    private FindContainerByForm(FindContainerByForm.Builder builder) {
        containerId = builder.containerId;
        checkValid();
    }

    public String getContainerId() {
        return containerId;
    }

    public static final class Builder {
        @JsonProperty(required = true)
        private String containerId;

        public FindContainerByForm.Builder containerId(String containerId) {
            this.containerId = containerId;
            return this;
        }

        public FindContainerByForm build() {
            return new FindContainerByForm(this);
        }
    }
}
