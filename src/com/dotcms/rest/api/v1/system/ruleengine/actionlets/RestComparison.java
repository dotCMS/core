package com.dotcms.rest.api.v1.system.ruleengine.actionlets;

import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = RestComparison.Builder.class)
public final class RestComparison {

    public final String id;
    public final String label;

    private RestComparison(Builder builder) {
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

        public Builder from(RestComparison copy) {
            id = copy.id;
            label = copy.label;
            return this;
        }

        public RestComparison build() {
            checkValid();
            return new RestComparison(this);
        }

        private void checkValid() {

        }
    }
}

