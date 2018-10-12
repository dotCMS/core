package com.dotcms.cache;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class DotJSONCacheStrategyImpl implements DotJSONCacheStrategy {

    private final DotJSONCache cache = CacheLocator.getDotJSONCache();

    @Override
    public void addIfNeeded(final HttpServletRequest request, final User user, final DotJSON dotJSON) {
        DotJSONCache.DotJSONCacheKey cacheKey = getDotJSONCacheKey(request, user);
        cache.add(cacheKey, dotJSON);
    }

    @Override
    public Optional<DotJSON> get(HttpServletRequest request, User user) {
        final DotJSONCache.DotJSONCacheKey dotJSONCacheKey = getDotJSONCacheKey(request, user);
        return cache.get(dotJSONCacheKey);
    }

    private DotJSONCache.DotJSONCacheKey getDotJSONCacheKey(final HttpServletRequest request, final User user) {
        final Language language = WebAPILocator.getLanguageWebAPI().getLanguage(request);
        IPersona persona = null;
        final Optional<Visitor> visitor = APILocator.getVisitorAPI().getVisitor(request, false);

        if (visitor.isPresent() && visitor.get().getPersona() != null) {
            persona = visitor.get().getPersona();
        }

        final String requestURI = request.getRequestURI() + StringPool.QUESTION + request.getQueryString();

        return new DotJSONCache.DotJSONCacheKey(user, language, requestURI, persona);
    }
}
