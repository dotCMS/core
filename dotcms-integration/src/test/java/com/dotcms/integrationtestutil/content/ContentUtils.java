package com.dotcms.integrationtestutil.content;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.liferay.portal.model.User;

import java.util.Map;

/**
 * This utility class provides common-use methods used during the execution of integration tests
 * related to Contentlets in dotCMS.
 *
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 29, 2017
 *
 */
public class ContentUtils {

    /**
     * Deletes the specified list of Contentlets following the expected workflow: Unpublish,
     * archive, and delete.
     * 
     * @param user - The user performing this action.
     * @param contentlets - The Contentlets that will be deleted.
     * @throws DotContentletStateException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static void deleteContentlets(final User user, final Contentlet... contentlets)
                    throws DotContentletStateException, DotDataException, DotSecurityException {
        for (Contentlet contentlet : contentlets) {
            if (null != contentlet) {
                try {
                    APILocator.getContentletAPI().destroy(contentlet, user, false );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Utility method used to create test Key/Value contents.
     *
     * @param key - The content key.
     * @param value - The content value.
     * @param languageId - The associated language ID.
     * @param keyValueContentType - The Key/Value Content Type.
     * @param user - The user performing this action.
     * @return The new {@link Contentlet} object.
     * @throws DotContentletValidationException
     * @throws DotContentletStateException
     * @throws IllegalArgumentException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static Contentlet createTestKeyValueContent(final String key, final String value, final long languageId,
            final ContentType keyValueContentType, final User user)
            throws DotContentletStateException, IllegalArgumentException, DotDataException, DotSecurityException {
        
        return createTestKeyValueContent(null, key, value, languageId, keyValueContentType, user);
    }

    public static Contentlet createTestKeyValueContent(final String identifier, final String key,
            final String value, final long languageId,
            final ContentType keyValueContentType, final User user)
            throws DotContentletStateException, IllegalArgumentException, DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        Contentlet contentlet = new Contentlet();
        if (null != identifier) {
            contentlet.setIdentifier(identifier);
        }
        contentlet.setContentTypeId(keyValueContentType.inode());
        contentlet.setLanguageId(languageId);
        final Map<String, Field> fields = keyValueContentType.fieldMap();
        contentletAPI.setContentletProperty(contentlet, asOldField(fields.get(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR)), key);
        contentletAPI.setContentletProperty(contentlet, asOldField(fields.get(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR)),
                value);
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        contentlet = contentletAPI.checkin(contentlet, user, Boolean.FALSE);
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        contentletAPI.publish(contentlet, user, Boolean.FALSE);

        return contentlet;
    }

    /**
     * Utility method used to update a specific test Key/Value content.
     *
     * @param contentlet - The Contentlet to update.
     * @param newKey - The new content key.
     * @param newValue - The new content value.
     * @param languageId - The new associated language ID.
     * @param keyValueContentType - The Key/Value Content Type.
     * @param user - The user performing this action.
     * @return The updated {@link Contentlet} object.
     * @throws DotContentletValidationException
     * @throws DotContentletStateException
     * @throws IllegalArgumentException
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public static Contentlet updateTestKeyValueContent(final Contentlet contentlet, final String newKey, final String newValue,
            final long languageId, final ContentType keyValueContentType, final User user)
            throws DotContentletValidationException, DotContentletStateException, IllegalArgumentException,
            DotDataException, DotSecurityException {

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        final Map<String, Field> fields = keyValueContentType.fieldMap();

        final Contentlet checkoutContentlet =
                contentletAPI.checkout(contentlet.getInode(), user, false);
        final String inode = checkoutContentlet.getInode();
        checkoutContentlet.setIndexPolicy(IndexPolicy.FORCE);

        contentletAPI.copyProperties(checkoutContentlet, contentlet.getMap());
        checkoutContentlet.setInode(inode);

        contentletAPI.setContentletProperty(checkoutContentlet, asOldField(fields.get(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR)),
                newKey);
        contentletAPI.setContentletProperty(checkoutContentlet, asOldField(fields.get(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR)),
                newValue);


        checkoutContentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        return contentletAPI.checkin(checkoutContentlet, user, Boolean.FALSE);
    }

    /**
     * Utility method used to transform a new {@link Field} object into a legacy
     * {@link com.dotmarketing.portlets.structure.model.Field}.
     *
     * @param newField - The new {@link Field} object that will be transformed.
     * @return The legacy {@link com.dotmarketing.portlets.structure.model.Field} representation.
     */
    public static com.dotmarketing.portlets.structure.model.Field asOldField(final Field newField) {
        return new LegacyFieldTransformer(newField).asOldField();
    }

}
