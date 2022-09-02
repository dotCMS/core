package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
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
        ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find("favoritePage");
        if (null != contentType) {

            contentTypeAPI.delete(contentType);
        }

        new ContentTypeInitializer().init();

        contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find("favoritePage");

        Assert.assertNotNull("The content type favoritePage should be not null", contentType);

    }
}
