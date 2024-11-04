package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.JUnit4WeldRunner;
import com.dotcms.content.elasticsearch.constants.ESMappingConstants;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.org.directwebremoting.guice.ApplicationScoped;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.ApiProvider;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import org.junit.runner.RunWith;

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
     * Method to test: {@link com.dotmarketing.portlets.contentlet.business.ContentletAPI#search(String, int, int, String, User, boolean)}
     * Given Scenario: Creates a few favorite pages
     * ExpectedResult: On search, since favorite pages are system content, they should not be returned
     *
     */
    @Test
    public void test_system_field() throws Exception {

        final ContentType contentType = Try.of(()-> APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME)).getOrNull();

        new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "test").setProperty("url", "test" + System.currentTimeMillis()).nextPersisted();
        new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "test").setProperty("url", "test" + System.currentTimeMillis()).nextPersisted();
        new ContentletDataGen(contentType).setPolicy(IndexPolicy.WAIT_FOR)
                .setProperty("title", "test").setProperty("url", "test" + System.currentTimeMillis()).nextPersisted();

        final StringBuffer luceneQuery = new StringBuffer();
        luceneQuery.append("+contentType:"+contentType.variable() + " ");
        luceneQuery.append("+" + ESMappingConstants.SYSTEM_TYPE + ":false ");
        final List<Contentlet> contentlets = APILocator.getContentletAPI().search(luceneQuery.toString(),
                0, 10, null, APILocator.systemUser(), false);

        Assert.assertNotNull("The contentlets shouldn't be null", contentlets);
        Assert.assertTrue("Should not return any contentlet since they are system contentlets",contentlets.isEmpty());
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
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME)).getOrNull();

        if (null != contentType) {
            contentTypeAPI.delete(contentType);
        }

        new ContentTypeInitializer().init();

        contentType = Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.FAVORITE_PAGE_VAR_NAME)).getOrNull();

        Assert.assertNotNull("The content type dotFavoritePage shouldn't be null", contentType);

        Assert.assertTrue("CT : "+contentType, contentType.fieldMap().get("order").indexed());
        //we make sure the url field isn't unique
        Assert.assertFalse("CT : "+contentType,contentType.fieldMap().get("url").unique());

        //we make sure there is no content type using the legacy name
        Assert.assertNull(Try.of(()->APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(ContentTypeInitializer.LEGACY_FAVORITE_PAGE_VAR_NAME)).getOrNull());

        checkPermissionsOnContentlet(contentType);
    }

    private static void checkPermissionsOnContentlet(final ContentType contentType) throws DotDataException {
        final Role backendRole = APILocator.getRoleAPI().loadCMSOwnerRole();
        final List<Permission> permissions = APILocator.getPermissionAPI().getPermissions(contentType);
        Assert.assertTrue(permissions.stream().anyMatch(p->p.getPermission()==PermissionAPI.PERMISSION_USE && p.getRoleId().equals(backendRole.getId())));
        Assert.assertTrue(permissions.stream().anyMatch(p->p.getPermission()==PermissionAPI.PERMISSION_EDIT  && p.getRoleId().equals(backendRole.getId())));
        Assert.assertTrue(permissions.stream().anyMatch(p->p.getPermission()==PermissionAPI.PERMISSION_PUBLISH  && p.getRoleId().equals(backendRole.getId())));
        Assert.assertTrue(permissions.stream().anyMatch(p->p.getPermission()==PermissionAPI.PERMISSION_EDIT_PERMISSIONS && p.getRoleId().equals(backendRole.getId())));
    }
}
