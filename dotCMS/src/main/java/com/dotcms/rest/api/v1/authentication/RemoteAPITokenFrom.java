package com.dotcms.rest.api.v1.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

@JsonDeserialize(builder = RemoteAPITokenFrom.Builder.class)
public class RemoteAPITokenFrom {
    private TokenInfo token;
    private RemoteHostInfo remoteHostInfo;
    private AuthInfo authInfo;

    private RemoteAPITokenFrom( Builder builder) {
        this.token = builder.getToken();
        this.remoteHostInfo = builder.getRemoteHostInfo();
        this.authInfo = builder.getAuthInfo();
    }

    public String host(){
        return remoteHostInfo.host;
    }

    public String protocol(){
        return remoteHostInfo.protocol;
    }

    public int port(){
        return remoteHostInfo.port;
    }

    public String login(){
        return authInfo.login;
    }

    public String password(){
        return authInfo.password;
    }

    public Map<String, Object> getTokenInfo(){
        return map(
                "network", token.network,
                "expirationSeconds", token.expirationSeconds,
                "userId", token.userId,
                "claims", token.claims
        );
    }

    public static final class Builder {
        @JsonProperty
        private TokenInfo token;

        @JsonProperty
        private RemoteHostInfo remote;

        @JsonProperty
        private AuthInfo auth;

        public RemoteAPITokenFrom build() {
            return new RemoteAPITokenFrom(this);
        }

        public TokenInfo getToken() {
            return token;
        }

        public RemoteAPITokenFrom.RemoteHostInfo getRemoteHostInfo() {
            return remote;
        }

        public RemoteAPITokenFrom.AuthInfo getAuthInfo() {
            return auth;
        }
    }

    private static class TokenInfo{
        public String userId;
        public int expirationSeconds;
        public String network;
        public Map<String,Object> claims;
    }

    private static class RemoteHostInfo{
        public String host;
        public String protocol;
        public int port;
    }

    private static class AuthInfo{
        public String login;
        public String password;
    }
}
