package com.dotcms.rest.api.v1.authentication;

/**
 * Thrown when a Token is invalid
 */
public class DotInvalidTokenException extends Exception{

    private String tokenInfo;
    private boolean expired;

    public DotInvalidTokenException(String tokenInfo, boolean expired) {
        this.tokenInfo = tokenInfo;
        this.expired = expired;
    }

    public DotInvalidTokenException(String tokenInfo) {
        this( tokenInfo, false );
    }

    public String getTokenInfo() {
        return tokenInfo;
    }

    public boolean isExpired() {
        return expired;
    }
}