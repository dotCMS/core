package com.dotcms.rest.api.v1.authentication;

/**
 * Created by freddyrodriguez on 7/29/16.
 */
public class TokenUnvalidException extends Exception{

    private String tokenInfo;
    private boolean expired;

    public TokenUnvalidException(String tokenInfo, boolean expired) {
        this.tokenInfo = tokenInfo;
        this.expired = expired;
    }

    public TokenUnvalidException(String tokenInfo) {
        this( tokenInfo, false );
    }

    public String getTokenInfo() {
        return tokenInfo;
    }

    public boolean isExpired() {
        return expired;
    }
}
