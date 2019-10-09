package com.dotcms.keyvalue.business;

import com.dotcms.util.CollectionsUtils;
import java.util.Collections;
import java.util.List;

import com.dotcms.cache.KeyValueCache;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

/**
 * Implementation class for the {@link KeyValueAPI}.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 19, 2017
 *
 */
public class KeyValueAPIImpl implements KeyValueAPI {

    protected final ContentletAPI contentletAPI;
    protected final FolderAPI folderAPI;
    protected final UserAPI userAPI;
    protected final KeyValueCache cache;
    final static KeyValue KEY_VALUE_404=new KeyValue404();
    /**
     * Creates a new instance of the {@link KeyValueAPI}.
     */
    public KeyValueAPIImpl() {
        this.contentletAPI = APILocator.getContentletAPI();
        this.folderAPI = APILocator.getFolderAPI();
        this.userAPI = APILocator.getUserAPI();
        this.cache = CacheLocator.getKeyValueCache();
    }

    @Override
    public List<KeyValue> get(final String key, final User user,
                              final ContentType contentType, final boolean respectFrontendRoles) {
        List<KeyValue> results = this.cache.get(key);
        if (null != results) {
            return results;
        }
        results = queryKeyValues(key, -1, contentType, user, respectFrontendRoles);
        if (null != results) {
            this.cache.add(key, results);
        }
        return results;
    }
    @Override
    public List<KeyValue> get(final String key, final User user, final boolean respectFrontendRoles) {
        List<KeyValue> results = this.cache.get(key);
        if (null != results) {
            return results;
        }
        results = queryKeyValues(key, -1, null, user, respectFrontendRoles);
        if (null != results) {
            this.cache.add(key, results);
        }
        return results;
    }


    @Override
    public List<KeyValue> get(final String key, final ContentType contentType,
                              final long languageId, final User user, final boolean respectFrontendRoles) {
        List<KeyValue> results = this.cache.getByLanguage(key, languageId);
        if (null != results) {
            return results;
        }
        results = queryKeyValues(key, languageId, contentType, user, respectFrontendRoles);
        if (null != results) {
            this.cache.addByLanguage(key, languageId, results);
        }
        return results;
    }

    @Override
    public List<KeyValue> get(final String key, final long languageId, final User user, final boolean respectFrontendRoles) {
        List<KeyValue> results = this.cache.getByLanguage(key, languageId);
        if (null != results) {
            return results;
        }
        results = queryKeyValues(key, languageId, null, user, respectFrontendRoles);
        if (null != results) {
            this.cache.addByLanguage(key, languageId, results);
        }
        return results;
    }

    @Override
    public List<KeyValue> get(final String key, final ContentType contentType, final User user,
                    final boolean respectFrontendRoles) {
        List<KeyValue> results = this.cache.getByContentType(key, contentType.id());
        if (null != results) {
            return results;
        }
        results = queryKeyValues(key, -1, contentType, user, respectFrontendRoles);
        if (null != results) {
            this.cache.addByContentType(key, contentType.id(), results);
        }
        return results;
    }

    @Override
    public KeyValue get(final String key, final long languageId, final ContentType contentType, final User user,
                    final boolean respectFrontendRoles) {

        return this.get(key, languageId, contentType, user, true, respectFrontendRoles);
    }

    @Override
    public KeyValue get(final String key, final long languageId, final ContentType contentType, final User user, final boolean live,
                        final boolean respectFrontendRoles) {

        if (languageId > -1 && null != contentType && UtilMethods.isSet(contentType.id())) {
            final KeyValue keyValue = this.cache.get(key, languageId, contentType.id(), live);
            if (null != keyValue ) {
                return (KEY_VALUE_404.equals(keyValue)) ? null : keyValue;
            }
        }

        final List<KeyValue> result = queryKeyValues(key, languageId, contentType, user, live, respectFrontendRoles);
        result.stream().forEach((KeyValue keyValue) -> this.cache.add(languageId, contentType.id(), live, keyValue));
        return (!result.isEmpty()) ? result.get(0) : null;
    }


    /**
     * Performs a Lucene query to returns a list of {@link KeyValue} objects that match the
     * specified key, language ID, and Content Type. This method don't use pagination or limit in
     * its results.
     *
     * @param key - The key.
     * @param languageId - The ID of the language that the content was created for.
     * @param contentType - The {@link ContentType} used to create this content.
     * @param user - The user performing this action.
     * @param respectFrontendRoles - Set to {@code true} if this method requires that front-end
     *        roles are take in count for the search (which means this is being called from the
     *        front-end). Otherwise, set to {@code false}.
     * @return The Key/Value object.
     */
    private List<KeyValue> queryKeyValues(final String key, final long languageId, final ContentType contentType, final User user,
                                          final boolean live, final boolean respectFrontendRoles) {
        final ImmutableList.Builder<KeyValue> results = new Builder<>();
        // No limit and no pagination required
        final int limit = 0;
        final int offset = -1;
        final StringBuilder query = new StringBuilder();
        try {

            query.append((UtilMethods.isSet(contentType) && UtilMethods.isSet(contentType.variable()))
                            ? "+contentType:" + contentType.variable() : "+baseType:" + BaseContentType.KEY_VALUE.getType());

            if (UtilMethods.isSet(contentType) && UtilMethods.isSet(contentType.variable())) {

                query.append(" +").append(contentType.variable()).append(".key_dotraw:").append(key);
            } else {
                query.append(" +key_dotraw:").append(key);
            }

            query.append((languageId >= 0) ? " +languageId:" + languageId : StringPool.BLANK);
            query.append(live ? " +live:true" : " +working:true");
            query.append(" +deleted:false");
            List<Contentlet> contentResults =
                            contentletAPI.search(query.toString(), limit, offset, StringPool.BLANK, user, respectFrontendRoles);

            contentResults.stream().forEach((Contentlet contentlet) -> {
                KeyValue keyValue = fromContentlet(contentlet);
                results.add(keyValue);
            });
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, String.format("An error occurred when retrieving a KeyValue object with key '%s': %s", key,
                            e.getMessage()), e);
        }
        return results.build();
    }

    private List<KeyValue> queryKeyValues(final String key, final long languageId, final ContentType contentType, final User user,
                                          final boolean respectFrontendRoles) {

        return this.queryKeyValues(key, languageId, contentType, user, true, respectFrontendRoles);
    }

    public List<KeyValue> getKeyValuesByKeyStartingWith(final String key, final long languageId, final ContentType contentType, final User user,
            final boolean respectFrontendRoles, final int limit) {
        final StringBuilder query = new StringBuilder();
        try {

            if (UtilMethods.isSet(contentType) && UtilMethods.isSet(contentType.variable())) {
                query.append("+contentType:").append(contentType.variable());
                query.append(" +").append(contentType.variable()).append(".key:").append(key).append("*");
            } else {
                query.append("+baseType:").append(BaseContentType.KEY_VALUE.getType());
                query.append(" +key:").append(key).append("*");
            }

            query.append((languageId >= 0) ? " +languageId:" + languageId : StringPool.BLANK);
            query.append(" +live:true +deleted:false");
            List<Contentlet> contentResults =
                    contentletAPI.search(query.toString(), limit, -1, StringPool.BLANK, user, respectFrontendRoles);
            return contentResults.stream().map(this::fromContentlet).collect(CollectionsUtils.toImmutableList());
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, String.format("An error occurred when retrieving a KeyValue object with key '%s': %s", key,
                    e.getMessage()), e);
        }
        return Collections.EMPTY_LIST;
    }

}
