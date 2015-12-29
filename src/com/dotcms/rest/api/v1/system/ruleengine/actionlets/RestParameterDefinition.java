package com.dotcms.rest.api.v1.system.ruleengine.actionlets;

import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestParameterDefinition.Builder.class)
public final class RestParameterDefinition {

    public final String id;
    public final String label;

    private RestParameterDefinition(Builder builder) {
        id = builder.id;
        label = builder.label;
    }

    public static final class Builder {
        private String id;
        private String label;

        public Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder from(RestParameterDefinition copy) {
            id = copy.id;
            label = copy.label;
            return this;
        }

        public RestParameterDefinition build() {
            checkValid();
            return new RestParameterDefinition(this);
        }

        private void checkValid() {

        }
    }
}

