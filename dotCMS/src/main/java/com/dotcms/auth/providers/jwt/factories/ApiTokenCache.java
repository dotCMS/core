package com.dotcms.auth.providers.jwt.factories;

import java.util.Arrays;
import java.util.Optional;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheAdministrator;
import com.dotmarketing.business.DotCacheException;

public class ApiTokenCache implements Cachable {

    private final static String TOKEN_GROUP = ApiTokenCache.class.getSimpleName().toLowerCase();

    private final static String[] GROUPS = {TOKEN_GROUP};


    private DotCacheAdministrator cache = CacheLocator.getCacheAdministrator();

    @Override
    public String getPrimaryGroup() {
        return TOKEN_GROUP;
    }


    public Optional<ApiToken> getApiToken(final String tokenId) {

        try {
            return Optional.ofNullable((ApiToken) cache.get(tokenId, TOKEN_GROUP));
        } catch (DotCacheException e) {
            return Optional.empty();
        }
    }

    public void putApiToken(final ApiToken apiToken) {
        putApiToken(apiToken.id, apiToken);
    }
    public void putApiToken(final String tokenId, final ApiToken apiToken) {
        cache.put(tokenId, apiToken, TOKEN_GROUP);
    }

    public void removeApiToken(final String tokenId) {

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
