package com.dotmarketing.portlets.contentlet.model;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author nollymar
 */
public class ContentletTest {

    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static HostAPI hostAPI;
    private static Language defaultLanguage;
    private static LanguageAPI languageAPI;
    private static UserAPI userAPI;
    private static User user;
    private static Host defaultHost;


    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        fieldAPI    = APILocator.getContentTypeFieldAPI();
        hostAPI     = APILocator.getHostAPI();
        languageAPI = APILocator.getLanguageAPI();

        userAPI = APILocator.getUserAPI();
        user    = userAPI.getSystemUser();

        contentTypeAPI  = APILocator.getContentTypeAPI(user);
        defaultHost     = hostAPI.findDefaultHost(user, false);
        defaultLanguage = languageAPI.getDefaultLanguage();
    }

    @Test
    public void testGetContentTypeAlwaysReturnsTheLatestCachedVersion()
            throws DotSecurityException, DotDataException {

        Field field;
        final long time = System.currentTimeMillis();


        //Create Content Type.
        ContentType contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                .description("Test ContentType " + time)
                .host(defaultHost.getIdentifier())
                .name("Test ContentType "+ time)
                .owner("owner")
                .variable("testContentType" + time)
                .build();

        contentType = contentTypeAPI.save(contentType);

        try {
            //Creating new Text Field.
            field = ImmutableTextField.builder()
                    .name("Title")
                    .variable("title")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .build();

            fieldAPI.save(field, user);

            ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());

            final Contentlet contentlet = contentletDataGen.languageId(defaultLanguage.getId())
                    .nextPersisted();

            assertNotNull(contentlet.getContentType());

            //Adding a new field in the content type
            field = ImmutableTextField.builder()
                    .name("Description")
                    .variable("Description")
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.LONG_TEXT)
                    .build();

            fieldAPI.save(field, user);

            final ContentType cachedContentType = contentTypeAPI.find(contentType.inode());

            //Both content types (the one contained in the contentlet and the cached one) must be the same
            assertEquals(cachedContentType.fields().size(),
                    contentlet.getContentType().fields().size());

            assertEquals(cachedContentType.inode(), contentlet.getContentType().inode());

            assertEquals(cachedContentType.modDate(), contentlet.getContentType().modDate());

        }finally{
            contentTypeAPI.delete(contentType);
        }
    }

}
