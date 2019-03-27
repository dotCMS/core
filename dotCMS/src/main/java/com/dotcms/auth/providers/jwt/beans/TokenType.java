package com.dotcms.auth.providers.jwt.beans;

public enum TokenType {

    USER_TOKEN("user"), 
    API_TOKEN("api");

    public final String prefix;


    TokenType(final String prefix) {
        this.prefix = prefix;
    }

    public static TokenType getTokenType(final String token) {
        if (token != null && token.startsWith(API_TOKEN.prefix)) {
            return API_TOKEN;
        }
        return USER_TOKEN;


    }


}
