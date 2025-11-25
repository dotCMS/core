package com.dotcms.rest.api.v1.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

@JsonDeserialize(builder = RemoteAPITokenForm.Builder.class)
public class RemoteAPITokenForm {
    private TokenInfo token;
    private RemoteHostInfo remote;
    private AuthInfo auth;

    private RemoteAPITokenForm(Builder builder) {
        this.token = builder.getToken();
        this.remote = builder.getRemote();
        this.auth = builder.getAuth();
    }

    public String host(){
        return remote.host;
    }

    public String protocol(){
        return remote.protocol;
    }

    public int port(){
        return remote.port;
    }

    public String login(){
        return auth.login;
    }

    public String password(){
        return auth.password;
    }

    public Map<String, Object> getTokenInfo(){
        return Map.of(
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

        public RemoteHostInfo getRemote() {
            return remote;
        }

        public AuthInfo getAuth() {
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
