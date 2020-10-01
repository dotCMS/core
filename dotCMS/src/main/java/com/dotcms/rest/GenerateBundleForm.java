package com.dotcms.rest;

import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Stolen from @author jsanca
 */
@JsonDeserialize(builder = GenerateBundleForm.Builder.class)
public class GenerateBundleForm {

    public final String bundleId;
    public PushPublisherConfig.Operation operation;
    public final String filterKey;

    private GenerateBundleForm(final Builder builder) {

        this.bundleId = builder.bundleId;
        this.operation = builder.operation == PushPublisherConfig.Operation.PUBLISH.ordinal() ? PushPublisherConfig.Operation.PUBLISH : PushPublisherConfig.Operation.UNPUBLISH;
        this.filterKey = builder.filterKey;
    }


    @Override
    public String toString() {
        return "{bundleId=" + bundleId + ", operation=" + operation + ", filter=" + filterKey + "}";
    }


    public static final class Builder {

        private @JsonProperty String bundleId = null;
        private @JsonProperty String filterKey = null;
        private @JsonProperty int operation = 0;

        public Builder bundleId(final String bundleId) {
            this.bundleId = bundleId;
            return this;
        }

        public Builder filter(final String filter) {
            this.filterKey = filter;
            return this;
        }

        public Builder operation(final int operation) {
            this.operation = operation;
            return this;
        }

        public GenerateBundleForm build() {
            return new GenerateBundleForm(this);
        }

    }

}
