package com.dotcms.rest.api.v1.authentication;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Hidden;

@JsonDeserialize(builder = ApiTokenForm.Builder.class)
public class ApiTokenForm  {


    public final String userId;
    @Hidden
    public final String tokenId;
    @Hidden
    public final boolean showRevoked;
    public final int expirationSeconds;
    public final String network;
    public final Map<String,Object> claims;
    public boolean shouldBeAdmin;

    private ApiTokenForm(Builder builder) {
        userId = builder.userId;
        tokenId = builder.tokenId;
        showRevoked = builder.showRevoked;
        network = builder.network;
        expirationSeconds = builder.expirationSeconds;
        claims = builder.claims;
        shouldBeAdmin = builder.shouldBeAdmin;
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
        public  Map<String,Object> claims = ImmutableMap.of();
        @JsonProperty
        private int expirationSeconds=-1;
        @JsonProperty
        private boolean shouldBeAdmin = false;

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
        public Builder claims(Map<String,Object> claims) {
            this.claims = claims;
            return this;
        }

        public Builder shouldBeAdmin(boolean shouldBeAdmin) {
            this.shouldBeAdmin = shouldBeAdmin;
            return this;
        }

        public ApiTokenForm build() {
            return new ApiTokenForm(this);
        }
    }
}

