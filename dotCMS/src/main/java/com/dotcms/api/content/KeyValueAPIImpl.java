package com.dotcms.api.content;

import java.util.List;

import com.dotcms.cache.KeyValueCache;
import com.dotcms.content.model.DefaultKeyValue;
import com.dotcms.content.model.KeyValue;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
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
    public KeyValue fromContentlet(final Contentlet contentlet) {
        if (null == contentlet) {
            throw new DotStateException("The contentlet cannot be null.");
        }
        try {
            if (!contentlet.getContentType().baseType().equals(BaseContentType.KEY_VALUE)) {
                throw new DotStateException(String.format("The contentlet with ID %s is not a KeyValue content.",
                                contentlet.getIdentifier()));
            }
        } catch (DotSecurityException | DotDataException e) {
            throw new DotStateException(String.format("The contentlet with ID %s could not be identified as a KeyValue content.",
                            contentlet.getIdentifier()));
        }
        DefaultKeyValue keyValue;
        final String key = contentlet.getMap().get(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR).toString();
        final long langId = contentlet.getLanguageId();
        keyValue = DefaultKeyValue.class.cast(this.cache.get(this.cache.generateCacheKey(key, langId)));
        if (null != keyValue) {
            return keyValue;
        }
        keyValue = new DefaultKeyValue();
        keyValue.setContentTypeId(contentlet.getContentTypeId());
        try {
            this.contentletAPI.copyProperties(Contentlet.class.cast(keyValue), contentlet.getMap());
        } catch (DotRuntimeException | DotSecurityException e) {
            throw new DotStateException(String.format("Properties of Contentlet %s could not be copied to a KeyValue object.",
                            contentlet.getIdentifier()), e);
        }
        keyValue.setHost(contentlet.getHost());
        if (UtilMethods.isSet(contentlet.getFolder())) {
            try {
                final Folder folder = this.folderAPI.find(contentlet.getFolder(), this.userAPI.getSystemUser(), Boolean.FALSE);
                keyValue.setFolder(folder.getInode());
            } catch (DotDataException | DotSecurityException e) {
                Logger.warn(this, String.format("Contentlet with ID %s could not be converted to a KeyValue object.",
                                contentlet.getIdentifier()), e);
                keyValue = new DefaultKeyValue();
            }
        }
        return keyValue;
    }

    @Override
    public List<KeyValue> get(final String key, final User user, final boolean respectFrontEnd) {
        return get(key, null, user, respectFrontEnd);
    }

    /*
        grupo 1 = key-lang
        grupo 2 = key-lang-conttype
        grupo 3 = key-contetype
    */
    @Override
    public List<KeyValue> get(final String key, final long languageId, final User user, final boolean respectFrontEnd) {
        return null;
    }
    
    @Override
    public List<KeyValue> get(final String key, final ContentType contentType, final User user, final boolean respectFrontEnd) {
        return queryKeyValues(key, -1, contentType, user, respectFrontEnd);
    }

    @Override
    public KeyValue get(final String key, final long languageId, final ContentType contentType, final User user,
                    final boolean respectFrontEnd) {
        if (languageId > -1 && null != contentType && UtilMethods.isSet(contentType.id())) {
            KeyValue keyValue = this.cache.get(this.cache.generateCacheKey(key, languageId));
            if (null != keyValue) {
                return keyValue;
            }
        }
        List<KeyValue> result = queryKeyValues(key, languageId, contentType, user, respectFrontEnd);
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
        final int limit = 0;
        final int offset = -1;
        StringBuilder query = new StringBuilder();
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
                this.cache.add(this.cache.generateCacheKey(keyValue), keyValue);
                results.add(keyValue);
            });
        } catch (DotDataException | DotSecurityException e) {
            Logger.error(this, String.format("An error occurred when retrieving a KeyValue object with key '%s': %s", key,
                            e.getMessage()), e);
        }
        return results.build();
    }

}
