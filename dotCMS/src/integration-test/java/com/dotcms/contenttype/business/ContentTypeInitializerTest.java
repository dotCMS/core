package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for the {@link ContentTypeInitializer}
 * @author jsanca
 */
public class ContentTypeInitializerTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ContentTypeInitializer#init()}
     * Given Scenario: If the content type exists is being deleted, and try the initializer to see if works
     * ExpectedResult: The content type should be created
     *
     */
    @Test
    public void test_content_type_init() throws Exception {
        // make sure its cached. see https://github.com/dotCMS/dotCMS/issues/2465
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        ContentType contentType = Try.of(()-> APILocator.getContentTypeAPI(APILocator.systemUser())
                .find("dotFavoritePage")).getOrNull();

        if (null != contentType) {

            contentTypeAPI.delete(contentType);
        }

        new ContentTypeInitializer().init();

        contentType = Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser())
                .find("dotFavoritePage")).getOrNull();

        Assert.assertNotNull("The content type dotFavoritePage should be not null", contentType);

        //we make sure the url field is unique
        Assert.assertTrue(contentType.fieldMap().get("url").unique());

        //we make sure there is no content type using the legacy name
        Assert.assertNull(Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser())
                .find("favoritePage")).getOrNull());
    }
}
