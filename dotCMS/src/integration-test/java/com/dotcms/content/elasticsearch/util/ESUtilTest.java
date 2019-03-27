package com.dotcms.content.elasticsearch.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.google.common.hash.Hashing;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class ESUtilTest extends IntegrationTestBase {

    private final User systemUser = APILocator.systemUser();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] dataProviderSpecialChars() {
        return new String[]{
                "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "?", ":", "\\"
        };
    }

    @Test
    @UseDataProvider("dataProviderSpecialChars")
    public void testEscape_textWithSpecialCharAndNoSpaces_ESQueryShouldMatch(String testValue)
        throws DotSecurityException, DotDataException {

        long time = System.currentTimeMillis();
        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name("type"+time).variable("type"+time)
            .build();
        type = APILocator.getContentTypeAPI(systemUser).save(type);

        Field field = FieldBuilder.builder(TextField.class).name("text"+time).contentTypeId(type.id()).build();
        field = APILocator.getContentTypeFieldAPI().save(field, systemUser);

        try {
            Contentlet contentlet = new Contentlet();
            contentlet.setContentTypeId(type.id());
            contentlet.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
            contentlet.setStringProperty(field.variable(),
                    ESUtils.escape("this" + testValue + "has" + testValue + "specialchars"));
            contentlet.setIndexPolicy(IndexPolicy.FORCE);

            contentlet = contentletAPI.checkin(contentlet, systemUser, false);

            final String fieldValue = contentlet.get(field.variable()).toString();
            final String escapedValue = ESUtils.escape(fieldValue);
            System.out.println("fieldValue = " + fieldValue);
            System.out.println("escapedValue = " + escapedValue);

            final StringBuilder luceneQuery = new StringBuilder().append("+structureInode:")
                    .append(type.id())
                    .append(' ').append(type.variable()).append('.').append(field.variable())
                    .append(':').append(escapedValue);

            final List<ContentletSearch> contentlets = contentletAPI
                    .searchIndex(luceneQuery.toString(), 0, -1,
                            null, systemUser, false);

            assertEquals(contentlets.get(0).getInode(), contentlet.getInode());
        }finally{
            APILocator.getContentTypeAPI(systemUser).delete(type);
        }

    }

    @Test
    public void testEscape_textWithSpecialCharAndSpaces_ESQueryShouldMatch()
        throws DotSecurityException, DotDataException {

        long time = System.currentTimeMillis();
        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class).name("type"+time).variable("type"+time)
            .build();
        type = APILocator.getContentTypeAPI(systemUser).save(type);

        Field field = FieldBuilder.builder(TextField.class).name("text"+time).contentTypeId(type.id()).build();
        field = APILocator.getContentTypeFieldAPI().save(field, systemUser);

        try {
            Contentlet contentlet = new Contentlet();
            contentlet.setContentTypeId(type.id());
            contentlet.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
            contentlet.setStringProperty(field.variable(), "this has spaces");
            contentlet.setIndexPolicy(IndexPolicy.FORCE);

            contentlet = contentletAPI.checkin(contentlet, systemUser, false);

            final String fieldValue = contentlet.get(field.variable()).toString();
            final String escapedValue = ESUtils.escape(fieldValue);

            final StringBuilder luceneQuery = new StringBuilder().append("+structureInode:")
                    .append(type.id())
                    .append(' ').append(type.variable()).append('.').append(field.variable())
                    .append(':').append(escapedValue);

            final List<ContentletSearch> contentlets = contentletAPI
                    .searchIndex(luceneQuery.toString(), 0, -1,
                            null, systemUser, false);

            assertFalse(contentlets.isEmpty());
            assertEquals(contentlets.get(0).getInode(), contentlet.getInode());
        }finally {
            APILocator.getContentTypeAPI(systemUser).delete(type);
        }

    }

    @Test
    public void testSha256() {

        final String fieldName = "testContentType.testTitle";
        final String fieldValue = "Test Title 1";

        final String expectedHash = Hashing.sha256().hashString(fieldName + "_"
                + (fieldValue == null ? "" : fieldValue) + "_"
                + 1, Charset.forName("UTF-8")).toString();

        final String resultHash = ESUtils.sha256(fieldName, fieldValue, 1);

        assertEquals(expectedHash, resultHash);

    }
}
