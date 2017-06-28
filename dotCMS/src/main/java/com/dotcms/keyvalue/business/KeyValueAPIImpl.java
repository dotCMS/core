package com.dotcms.keyvalue.business;

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
    public List<KeyValue> get(final String key, final User user, final boolean respectFrontEnd) {
        List<KeyValue> results = this.cache.get(key);
        if (null != results) {
            return results;
        }
        results = queryKeyValues(key, -1, null, user, respectFrontEnd);
        if (null != results && !results.isEmpty()) {
            this.cache.add(key, results);
        }
        return results;
    }

    @Override
    public List<KeyValue> get(final String key, final long languageId, final User user, final boolean respectFrontEnd) {
        List<KeyValue> results = this.cache.getByLanguage(key, languageId);
        if (null != results) {
            return results;
        }
        results = queryKeyValues(key, languageId, null, user, respectFrontEnd);
        if (null != results && !results.isEmpty()) {
            this.cache.addByLanguage(key, languageId, results);
        }
        return results;
    }

    @Override
    public List<KeyValue> get(final String key, final ContentType contentType, final User user, final boolean respectFrontEnd) {
        List<KeyValue> results = this.cache.getByContentType(key, contentType.id());
        if (null != results) {
            return results;
        }
        results = queryKeyValues(key, -1, contentType, user, respectFrontEnd);
        if (null != results && !results.isEmpty()) {
            this.cache.addByContentType(key, contentType.id(), results);
        }
        return queryKeyValues(key, -1, contentType, user, respectFrontEnd);
    }

    @Override
    public KeyValue get(final String key, final long languageId, final ContentType contentType, final User user,
                    final boolean respectFrontEnd) {
        if (languageId > -1 && null != contentType && UtilMethods.isSet(contentType.id())) {
            final KeyValue keyValue = this.cache.getByLanguageAndContentType(key, languageId, contentType.id());
            if (null != keyValue) {
                return keyValue;
            }
        }
        final List<KeyValue> result = queryKeyValues(key, languageId, contentType, user, respectFrontEnd);
        result.stream().forEach((KeyValue keyValue) -> {
            this.cache.addByLanguageAndContentType(languageId, contentType.id(), keyValue);
        });
        return (result.size() > 0) ? result.get(0) : null;
    }

    /**
     * 
     * @param key
     * @param languageId
     * @param contentType
     * @param user
     * @param respectFrontEnd
     * @return
     */
    private List<KeyValue> queryKeyValues(final String key, final long languageId, final ContentType contentType, final User user,
                    final boolean respectFrontEnd) {
        final ImmutableList.Builder<KeyValue> results = new Builder<KeyValue>();
        // No limit and no pagination required
        final int limit = 0;
        final int offset = -1;
        final StringBuilder query = new StringBuilder();
        try {
            query.append((UtilMethods.isSet(contentType) && UtilMethods.isSet(contentType.variable()))
                            ? "+contentType:" + contentType.variable() : "+baseType:" + BaseContentType.KEY_VALUE.getType());
            query.append(" +key:").append(key);
            query.append((languageId >= 0) ? " +languageId:" + languageId : StringPool.BLANK);
            query.append(" +live:true +deleted:false");
            List<Contentlet> contentResults =
                            contentletAPI.search(query.toString(), limit, offset, StringPool.BLANK, user, Boolean.FALSE);
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

}
