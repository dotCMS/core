package com.dotcms.keyvalue.business;

import java.util.List;

import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.keyvalue.model.DefaultKeyValue;
import com.dotcms.keyvalue.model.KeyValue;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Provides access to Key/Value contents in the system. This API provides easy access to any kind of
 * Content Type based on a key/value data structure, such as the Language Variables.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 19, 2017
 *
 */
public interface KeyValueAPI {

    /**
     * Transforms a given {@link Contentlet} object into a {@link KeyValue} object.
     * 
     * @param contentlet - The Contentlet containing the Key/Value data.
     * @return The transformed {@link KeyValue} object.
     */
    public default KeyValue fromContentlet(final Contentlet contentlet) {
        if (null == contentlet) {
            throw new DotStateException("The contentlet cannot be null.");
        }
        try {
            if (!contentlet.getContentType().baseType().equals(BaseContentType.KEY_VALUE)) {
                throw new DotStateException(String.format("The contentlet with ID '%s' is not a KeyValue content.",
                                contentlet.getIdentifier()));
            }
        } catch (DotSecurityException | DotDataException e) {
            throw new DotStateException(
                            String.format("The contentlet with ID '%s' could not be identified as a KeyValue content.",
                                            contentlet.getIdentifier()));
        }
        DefaultKeyValue keyValue;
        final String key = contentlet.getMap().get(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR).toString();
        final long langId = contentlet.getLanguageId();
        keyValue = DefaultKeyValue.class.cast(
                        CacheLocator.getKeyValueCache().getByLanguageAndContentType(key, langId, contentlet.getContentTypeId()));
        if (null != keyValue && UtilMethods.isSet(keyValue.getIdentifier())) {
            return keyValue;
        }
        keyValue = new DefaultKeyValue();
        keyValue.setContentTypeId(contentlet.getContentTypeId());
        try {
            APILocator.getContentletAPI().copyProperties(Contentlet.class.cast(keyValue), contentlet.getMap());
        } catch (DotRuntimeException | DotSecurityException e) {
            throw new DotStateException(
                            String.format("Properties of Contentlet with ID '%s' could not be copied to a KeyValue object.",
                                            contentlet.getIdentifier()),
                            e);
        }
        keyValue.setHost(contentlet.getHost());
        if (UtilMethods.isSet(contentlet.getFolder())) {
            try {
                final Folder folder = APILocator.getFolderAPI().find(contentlet.getFolder(),
                                APILocator.getUserAPI().getSystemUser(), Boolean.FALSE);
                keyValue.setFolder(folder.getInode());
            } catch (DotDataException | DotSecurityException e) {
                Logger.warn(this, String.format("Contentlet with ID '%s' could not be converted to a KeyValue object.",
                                contentlet.getIdentifier()), e);
                keyValue = new DefaultKeyValue();
            }
        }
        return keyValue;
    }

    /**
     * Returns a list of {@link KeyValue} objects that match the specified key.
     * 
     * @param key - The key.
     * @param user - The user performing this action.
     * @param respectFrontendRoles - Set to {@code true} if this method requires that front-end
     *        roles are take in count for the search (which means this is being called from the
     *        front-end). Otherwise, set to {@code false}.
     * @return The list of Key/Value objects.
     */
    public List<KeyValue> get(final String key, final User user, final boolean respectFrontendRoles);

    /**
     * Returns a list of {@link KeyValue} objects that match the specified key and language ID.
     * 
     * @param key - The key.
     * @param languageId - The ID of the language that the content was created for.
     * @param user - The user performing this action.
     * @param respectFrontendRoles - Set to {@code true} if this method requires that front-end
     *        roles are take in count for the search (which means this is being called from the
     *        front-end). Otherwise, set to {@code false}.
     * @return The list of Key/Value objects.
     */
    public List<KeyValue> get(final String key, final long languageId, final User user, final boolean respectFrontendRoles);

    /**
     * Returns a list of {@link KeyValue} objects that match the specified key and Content Type.
     * 
     * @param key - The key.
     * @param contentType - The {@link ContentType} used to create this content.
     * @param user - The user performing this action.
     * @param respectFrontendRoles - Set to {@code true} if this method requires that front-end
     *        roles are take in count for the search (which means this is being called from the
     *        front-end). Otherwise, set to {@code false}.
     * @return The list of Key/Value objects.
     */
    public List<KeyValue> get(final String key, final ContentType contentType, final User user,
                    final boolean respectFrontendRoles);

    /**
     * Returns the {@link KeyValue} object that matches the specified key, language ID, and Content
     * Type.
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
    public KeyValue get(final String key, final long languageId, final ContentType contentType, final User user,
                    final boolean respectFrontendRoles);

}
