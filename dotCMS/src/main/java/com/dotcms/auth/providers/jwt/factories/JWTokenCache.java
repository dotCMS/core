package com.dotcms.auth.providers.jwt.factories;

import java.util.Arrays;
import java.util.Optional;

import com.dotcms.auth.providers.jwt.beans.JWTokenIssue;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;

public class JWTokenCache implements Cachable {

    private final static String TOKEN_GROUP = "jwttokenissue";

    private final static String[] GROUPS = {TOKEN_GROUP};


    private DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

    @Override
    public String getPrimaryGroup() {
        return TOKEN_GROUP;
    }


    public Optional<JWTokenIssue> getToken(final String tokenId) {

        try {
            return Optional.ofNullable((JWTokenIssue) cache.get(tokenId, TOKEN_GROUP));
        } catch (DotCacheException e) {
            return Optional.empty();
        }
    }

    public void putJWTokenIssue(final JWTokenIssue tokenIssue) {
        putJWTokenIssue(tokenIssue.id, tokenIssue);
    }
    public void putJWTokenIssue(final String tokenId, final JWTokenIssue tokenIssue) {
        cache.put(tokenId, tokenIssue, TOKEN_GROUP);
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
