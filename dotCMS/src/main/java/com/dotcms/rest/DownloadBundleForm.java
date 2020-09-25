package com.dotcms.rest;

import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Stolen from @author jsanca
 */
@JsonDeserialize(builder = DownloadBundleForm.Builder.class)
public class DownloadBundleForm {

    public final String bundleId;
    public final String userId;
    public PushPublisherConfig.Operation operation;
    public final String filterKey;

    private DownloadBundleForm(final Builder builder) {

        this.bundleId = builder.bundleId;
        this.operation = builder.operation == PushPublisherConfig.Operation.PUBLISH.ordinal() ? PushPublisherConfig.Operation.PUBLISH : PushPublisherConfig.Operation.UNPUBLISH;
        this.filterKey = builder.filterKey;
        this.userId = builder.userId;
    }


    @Override
    public String toString() {
        return "{bundleId=" + bundleId + ", userId=" + userId + ", operation=" + operation + ", filter=" + filterKey + "}";
    }


    public static final class Builder {

        private @JsonProperty String bundleId = null;
        private @JsonProperty String filterKey = null;
        private @JsonProperty int operation = 0;
        private @JsonProperty String userId = null;

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

        public Builder userId(final String userId) {
            this.userId = userId;
            return this;
        }

        public DownloadBundleForm build() {
            return new DownloadBundleForm(this);
        }

    }

}
