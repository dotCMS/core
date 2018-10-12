package com.dotcms.cache;

import com.dotcms.api.vtl.model.DotJSON;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class DotJSONNoCacheStrategyImpl implements DotJSONCacheStrategy {
    @Override
    public void addIfNeeded(HttpServletRequest request, User user, DotJSON dotJSON) {
        // no implementation
    }

    @Override
    public Optional<DotJSON> get(HttpServletRequest request, User user) {
        return Optional.empty();
    }
}
