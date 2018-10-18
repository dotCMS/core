package com.dotcms.cache;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * This cache will keep a set of different {@link com.dotcms.api.vtl.model.DotJSON} objects and their
 * different versions, i.e., the specific user ID, language, etc., used when
 * they were requested.
 *
 *
 */
public abstract class DotJSONCache implements Cachable {

    /**
     * Utility class used to keep the parameters used to identify a DotJSON Object cache.
     *
     */
    public static class DotJSONCacheKey {
        private final User user;
        private final Language language;
        private final String queryString;
        private final IPersona persona;
        /**
         * Creates an object with a series of DotJSON-specific parameters to try
         * to uniquely identify a DotJSON request.
         *
         * @param user
         *            - Current user.
         * @param language
         *            - The language.
         * @param queryString
         *            - The current query String in the page URL.
         */
        public DotJSONCacheKey(final User user, final Language language, final String queryString, final IPersona persona) {
            this.user = user;
            this.language = language;
            this.queryString = queryString;
            this.persona = persona;
        }

        /**
         * Generates the DotJSON subkey that will be used in the DotJSON cache. This
         * key will represent a specific version of the DotJSON object.
         *
         * @return The subkey which is specific for a DotJSON object.
         */
        public String getKey() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.user.getUserId());
            sb.append(StringPool.UNDERLINE).append(this.language.getId());
            if (StringUtils.isNotBlank(this.queryString)) {
                sb.append(StringPool.UNDERLINE).append(this.queryString);
            }
            if (UtilMethods.isSet(persona) && StringUtils.isNotBlank(this.persona.getKeyTag())) {
                sb.append(StringPool.UNDERLINE).append(this.persona);
            }
            return sb.toString();
        }

    }

    @Override
    public abstract String getPrimaryGroup();

    @Override
    public abstract String[] getGroups();

    @Override
    public abstract void clearCache();

    /**
     * Adds a {@link DotJSON} to the cache using info obtained from the provided request and user.
     * A {@link DotJSONCacheKey} is built out of the provided objects and used as cache key.
     *
     * @param request - The {@link HttpServletRequest} to grab info from to build a {@link DotJSONCacheKey}.
     * @param user - The {@link User} to include as part of the {@link DotJSONCacheKey}.
     * @param dotJSON - The {@link DotJSON} object to add to cache.
     */
    abstract public void add(final HttpServletRequest request, final User user, final DotJSON dotJSON);

    /**
     * Returns an {@link Optional<DotJSON>} object, with the value present or not depending on
     * whether the object is found in cache or not, or if object is expired according to {@link DotJSON#getCacheTTL()}
     *
     * A {@link DotJSONCacheKey} is built out of the provided objects (request and user) and used as cache key to
     * retrieve the entry
     *
     * @param request - The {@link HttpServletRequest} to grab info from to build a {@link DotJSONCacheKey}.
     * @param user - The {@link User} to include as part of the {@link DotJSONCacheKey}.
     * @return
     */
    abstract public Optional<DotJSON> get(final HttpServletRequest request, final User user);

    /**
     * Removes a page from the cache, along with all of its versions.
     *
     * @param dotJSONCacheKey The {@link DotJSONCacheKey} key to remove.
     */
    abstract public void remove(DotJSONCacheKey dotJSONCacheKey);

    DotJSONCache.DotJSONCacheKey getDotJSONCacheKey(final HttpServletRequest request, final User user) {
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
