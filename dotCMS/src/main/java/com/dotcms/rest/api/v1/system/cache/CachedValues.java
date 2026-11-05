package com.dotcms.rest.api.v1.system.cache;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.business.CacheLocator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import io.vavr.Lazy;

/**
 * Builds what the cache and Redis maintenance routes send back for a stored value. The value is written with a copy
 * of the REST object mapper that leaves out, wherever they appear (at the top or nested in maps, lists or other
 * objects), a {@link User}'s stored password, password-reset token ({@code icqId}) and remember-me token
 * ({@code skinId}), and a {@link Company}'s key. Every other property comes out as the REST mapper writes it, and the
 * stored object is not modified.
 */
public final class CachedValues {

    /**
     * A copy of the REST object mapper with the mix-ins. Building a JSON tree would otherwise strip the trailing zeros
     * of decimal numbers ({@code 100} would come out as {@code 1E+2}), which writing the value directly does not do.
     */
    private static final Lazy<ObjectMapper> MAPPER = Lazy.of(() ->
            DotObjectMapperProvider.getInstance().getDefaultObjectMapper().copy()
                    .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false)
                    .addMixIn(User.class, UserWithoutCredentials.class)
                    .addMixIn(Company.class, CompanyWithoutKey.class));

    private CachedValues() {
    }

    /**
     * Returns the value to send back for a stored value. Text, numbers and booleans are returned as they are; any
     * other value is returned as the JSON tree the REST mapper would write, without the properties listed in the
     * class description.
     *
     * @param value the stored value, or {@code null}
     * @return {@code null}, the scalar as it is, or the JSON tree of the value
     */
    public static Object forResponse(final Object value) {
        if (value == null || value instanceof CharSequence || value instanceof Number || value instanceof Boolean
                || value instanceof Character) {
            return value;
        }
        return MAPPER.get().valueToTree(value);
    }

    /**
     * Tells whether a cache group is the group that holds the Apps secret values.
     *
     * @param group the cache group, in any case
     * @return {@code true} for the Apps secrets group
     */
    public static boolean isAppsSecretsGroup(final String group) {
        return CacheLocator.getAppsCache().getPrimaryGroup().equalsIgnoreCase(group);
    }

    /**
     * Tells whether a Redis key is one the Redis cache wrote for the Apps secrets group. The Redis cache builds its
     * keys as {@code <prefix>.<group>.<key>.} ({@code RedisCache#cacheKey}).
     *
     * @param redisKey the Redis key, in any case
     * @return {@code true} when the key holds the Apps secrets group as one of its dot-separated parts
     */
    public static boolean isAppsSecretsKey(final String redisKey) {
        if (redisKey == null) {
            return false;
        }
        final String group = CacheLocator.getAppsCache().getPrimaryGroup();
        return redisKey.toLowerCase().contains("." + group.toLowerCase() + ".");
    }

    /**
     * Leaves a user's stored password, password-reset token and remember-me token out of its JSON.
     */
    @JsonIgnoreProperties(value = {"password", "icqId", "skinId"}, ignoreUnknown = true)
    private abstract static class UserWithoutCredentials {
    }

    /**
     * Leaves a company's key out of its JSON.
     */
    @JsonIgnoreProperties(value = {"key"}, ignoreUnknown = true)
    private abstract static class CompanyWithoutKey {
    }
}
