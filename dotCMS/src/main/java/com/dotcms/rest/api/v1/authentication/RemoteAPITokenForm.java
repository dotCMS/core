package com.dotcms.rest.api.v1.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;

@JsonDeserialize(builder = RemoteAPITokenForm.Builder.class)
public class RemoteAPITokenForm {
    private TokenInfo token;
    private RemoteHostInfo remoteHostInfo;
    private AuthInfo authInfo;

    private RemoteAPITokenForm(Builder builder) {
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
                "claims", token.claims,
                "shouldBeAdmin", true
        );
    }

    public static final class Builder {
        @JsonProperty
        private TokenInfo token;

        @JsonProperty
        private RemoteHostInfo remote;

        @JsonProperty
        private AuthInfo auth;

        public RemoteAPITokenForm build() {
            return new RemoteAPITokenForm(this);
        }

        public TokenInfo getToken() {
            return token;
        }

        public RemoteAPITokenForm.RemoteHostInfo getRemoteHostInfo() {
            return remote;
        }

        public RemoteAPITokenForm.AuthInfo getAuthInfo() {
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
