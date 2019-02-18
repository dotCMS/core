package com.dotcms.auth.providers.jwt.factories;

import java.util.Arrays;
import java.util.Optional;

import com.dotcms.auth.providers.jwt.beans.JWTokenIssued;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;

public class JWTokenCache implements Cachable {

    private final static String TOKEN_GROUP = "jwttokenIssued";

    private final static String[] GROUPS = {TOKEN_GROUP};


    private DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

    @Override
    public String getPrimaryGroup() {
        return TOKEN_GROUP;
    }


    public Optional<JWTokenIssued> getToken(final String tokenId) {

        try {
            return Optional.ofNullable((JWTokenIssued) cache.get(tokenId, TOKEN_GROUP));
        } catch (DotCacheException e) {
            return Optional.empty();
        }
    }

    public void putJWTokenIssued(final JWTokenIssued tokenIssued) {
        putJWTokenIssued(tokenIssued.id, tokenIssued);
    }
    public void putJWTokenIssued(final String tokenId, final JWTokenIssued tokenIssued) {
        cache.put(tokenId, tokenIssued, TOKEN_GROUP);
    }

    public void removeToken(final String tokenId) {

        cache.remove(tokenId, TOKEN_GROUP);
    }


    @Override
    public String[] getGroups() {

        return GROUPS;
    }

    @Override
    public void clearCache() {
        Arrays.asList(getGroups()).forEach(group -> cache.flushGroup(group));
    }

}
