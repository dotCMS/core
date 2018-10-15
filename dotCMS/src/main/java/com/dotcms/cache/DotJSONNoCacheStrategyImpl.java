package com.dotcms.cache;

import com.dotcms.api.vtl.model.DotJSON;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * No Cache implementation for HTTP methods that don't require cache
 */

public class DotJSONNoCacheStrategyImpl extends DotJSONCache {
    @Override
    public String getPrimaryGroup() {
        return null;
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public void clearCache() {

    }

    @Override
    public void add(HttpServletRequest request, User user, DotJSON dotJSON) {
        // no implementation
    }

    @Override
    public Optional<DotJSON> get(HttpServletRequest request, User user) {
        return Optional.empty();
    }

    @Override
    public void remove(DotJSONCacheKey dotJSONCacheKey) {
        // no implementation
    }
}
