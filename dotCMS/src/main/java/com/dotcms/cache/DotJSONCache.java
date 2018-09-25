package com.dotcms.cache;

import com.dotcms.api.vtl.model.DotJSON;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.IPersona;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import org.apache.commons.lang.StringUtils;

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
        private String queryString;
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
     * Adds a new entry to the cache.
     *
     * @param dotJSONCacheKey - The {@link DotJSONCacheKey} key.
     * @param dotJSON - The {@link DotJSON} object.
     */
    abstract public void add(DotJSONCacheKey dotJSONCacheKey, DotJSON dotJSON);

    /**
     * Returns an {@link Optional<DotJSON>} object, with the value present or not depending on
     * whether found in cache or not, or if object is expired according to {@link DotJSON#getCacheTTL()}
     *
     * @param dotJSONCacheKey key used to retrieve a specific DotJSON from the cache.
     * @return
     */
    abstract public Optional<DotJSON> get(DotJSONCacheKey dotJSONCacheKey);

    /**
     * Removes a page from the cache, along with all of its versions.
     *
     * @param dotJSONCacheKey The {@link DotJSONCacheKey} key to remove.
     */
    abstract public void remove(DotJSONCacheKey dotJSONCacheKey);

}
