package com.dotcms.rest.api.v1.authentication;

import com.dotcms.repackage.com.fasterxml.jackson.annotation.JsonProperty;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = ApiTokenForm.Builder.class)
public class ApiTokenForm  {


    public final String userId;

    public final String tokenId;
    public final boolean showRevoked;
    public final int expirationSeconds;
    public final String network;


    private ApiTokenForm(Builder builder) {
        userId = builder.userId;
        tokenId = builder.tokenId;
        showRevoked = builder.showRevoked;
        network = builder.network;
        expirationSeconds = builder.expirationSeconds;

    }

    public static final class Builder {
        @JsonProperty
        private String userId; // not present on create
        @JsonProperty
        private String tokenId;
        @JsonProperty
        private boolean showRevoked = false;
        @JsonProperty
        private String network;
        @JsonProperty
        private int expirationSeconds=-1;

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder tokenId(String tokenId) {
            this.tokenId = tokenId;
            return this;
        }

        public Builder showRevoked(boolean showRevoked) {
            this.showRevoked = showRevoked;
            return this;
        }

        public Builder netmask(String network) {
            this.network = network;
            return this;
        }

        public Builder expirationSeconds(int expirationSeconds) {
            this.expirationSeconds = expirationSeconds;
            return this;
        }

        public ApiTokenForm build() {
            return new ApiTokenForm(this);
        }
    }
}

