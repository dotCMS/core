package com.dotcms.api.content;

import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.IntegrationTestBase;
import com.dotcms.content.model.KeyValue;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.KeyValueContentType;
import com.dotcms.contenttype.transform.field.LegacyFieldTransformer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 21, 2017
 *
 */
public class KeyValueAPITest extends IntegrationTestBase {

    private static ContentType keyValueContentType;
    private static User systemUser;

    private static long englishLanguageId;
    private static long spanishLanguageId;

    private static final String KEY_1 = "com.dotcms.test.key1";
    private static final String VALUE_1 = "Test Key #1";

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        // Test Key/Value Content Type
        final long time = System.currentTimeMillis();
        final String contentTypeName = "Key/Value Test " + time;
        final String contentTypeVelocityVarName = "Keyvaluetest" + time;
        systemUser = APILocator.systemUser();
        final Host site = APILocator.getHostAPI().findDefaultHost(systemUser, Boolean.FALSE);
        ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(systemUser);
        keyValueContentType = ContentTypeBuilder.builder(KeyValueContentType.class).host(site.getIdentifier())
                        .description("Testing the Key/Value API.").name(contentTypeName).variable(contentTypeVelocityVarName)
                        .fixed(Boolean.FALSE).owner(systemUser.getUserId()).build();
        keyValueContentType = contentTypeApi.save(keyValueContentType);
        englishLanguageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        spanishLanguageId = APILocator.getLanguageAPI().getLanguage("es", "ES").getId();
    }

    @Test
    public void saveKeyValueContent() throws DotContentletValidationException, DotContentletStateException,
                    IllegalArgumentException, DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(keyValueContentType.inode());
        contentlet.setLanguageId(englishLanguageId);
        final Map<String, Field> fields = keyValueContentType.fieldMap();
        contentletAPI.setContentletProperty(contentlet, asOldField(fields.get(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR)),
                        KEY_1);
        contentletAPI.setContentletProperty(contentlet, asOldField(fields.get(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR)),
                        VALUE_1);
        contentlet = contentletAPI.checkin(contentlet, systemUser, Boolean.TRUE);

        Assert.assertTrue("Failed creating a new Contentlet using the Key/Value Content Type.",
                        UtilMethods.isSet(contentlet.getIdentifier()));
        contentletAPI.delete(contentlet, systemUser, Boolean.FALSE);
    }

    @Test
    public void getKeyValueList() throws DotContentletValidationException, DotContentletStateException, IllegalArgumentException,
                    DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        Contentlet contentlet = new Contentlet();
        Contentlet contentlet2 = new Contentlet();

        // Content 1
        contentlet.setContentTypeId(keyValueContentType.inode());
        contentlet.setLanguageId(englishLanguageId);
        final Map<String, Field> fields = keyValueContentType.fieldMap();
        contentletAPI.setContentletProperty(contentlet, asOldField(fields.get(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR)),
                        KEY_1);
        contentletAPI.setContentletProperty(contentlet, asOldField(fields.get(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR)),
                        VALUE_1);
        contentlet = contentletAPI.checkin(contentlet, systemUser, Boolean.FALSE);
        contentletAPI.publish(contentlet, systemUser, Boolean.FALSE);
        contentletAPI.isInodeIndexed(contentlet.getInode());
        
        contentlet2.setContentTypeId(keyValueContentType.inode());
        contentlet2.setLanguageId(spanishLanguageId);
        contentletAPI.setContentletProperty(contentlet2, asOldField(fields.get(KeyValueContentType.KEY_VALUE_KEY_FIELD_VAR)),
                        KEY_1);
        contentletAPI.setContentletProperty(contentlet2, asOldField(fields.get(KeyValueContentType.KEY_VALUE_VALUE_FIELD_VAR)),
                        VALUE_1 + "spanish");
        contentlet2 = contentletAPI.checkin(contentlet2, systemUser, Boolean.FALSE);
        contentletAPI.publish(contentlet2, systemUser, Boolean.FALSE);
        contentletAPI.isInodeIndexed(contentlet2.getInode());
        
        Assert.assertTrue("Failed creating a new Contentlet using the Key/Value Content Type.",
                        UtilMethods.isSet(contentlet2.getIdentifier()));

        final KeyValueAPI keyValueAPI = APILocator.getKeyValueAPI();
        List<KeyValue> keyValueList = keyValueAPI.get(KEY_1, systemUser, Boolean.FALSE);

        System.out.println("Size = " + keyValueList.size());
        contentletAPI.unpublish(contentlet, systemUser, Boolean.FALSE);
        contentletAPI.archive(contentlet, systemUser, Boolean.FALSE);
        contentletAPI.delete(contentlet, systemUser, Boolean.FALSE);
        contentletAPI.unpublish(contentlet2, systemUser, Boolean.FALSE);
        contentletAPI.archive(contentlet2, systemUser, Boolean.FALSE);
        contentletAPI.delete(contentlet2, systemUser, Boolean.FALSE);
        Assert.assertTrue("Key/Value list cannot be empty.", !keyValueList.isEmpty());
    }

    @AfterClass
    public static void cleanup() throws DotDataException, DotSecurityException {
        if (null != keyValueContentType && UtilMethods.isSet(keyValueContentType.id())) {
            ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(systemUser);
            contentTypeApi.delete(keyValueContentType);
        }
    }

    /**
     * Utility method used to transform a new {@link Field} object into a legacy
     * {@link com.dotmarketing.portlets.structure.model.Field}.
     * 
     * @param newField - The new {@link Field} object that will be transformed.
     * @return The legacy {@link com.dotmarketing.portlets.structure.model.Field} representation.
     */
    private com.dotmarketing.portlets.structure.model.Field asOldField(Field newField) {
        return new LegacyFieldTransformer(newField).asOldField();
    }

}
