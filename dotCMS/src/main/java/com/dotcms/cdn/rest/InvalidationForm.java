package com.dotcms.cdn.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(builder = InvalidationForm.Builder.class)
public class InvalidationForm {

    private final List<String> urls;
    private final boolean invalidateAll;
    private final String hostId;

    private InvalidationForm(Builder builder) {
        urls = builder.urls;
        invalidateAll = builder.invalidateAll;
        hostId = builder.hostId;
    }

    public boolean isInvalidateAll() {
        return invalidateAll;
    }

    public String getHostId() {
        return hostId;
    }

    public List<String> getUrls() {
        return urls;
    }

    public static final class Builder {

        @JsonProperty
        private List<String> urls = new ArrayList<>();

        @JsonProperty
        private String hostId;

        @JsonProperty
        private boolean invalidateAll = false;

        public Builder urls(final List<String> urls) {
            this.urls = urls;
            return this;
        }

        public Builder invalidateAll(final boolean invalidateAll) {
            this.invalidateAll = invalidateAll;
            return this;
        }

        public Builder hostId(final String hostId) {
            this.hostId = hostId;
            return this;
        }

        public InvalidationForm build() {
            return new InvalidationForm(this);
        }
    }
}
