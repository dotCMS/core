package com.dotcms.rendering.velocity.viewtools.content;

import static org.mockito.Mockito.mock;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.PermissionType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import io.vavr.API;
import java.util.Date;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentMapTest extends IntegrationTestBase {

    private static ContentletAPI contentletAPI;
    private static Host defaultHost;
    private static HostAPI hostAPI;
    private static LanguageAPI languageAPI;
    private static UserAPI userAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        hostAPI = APILocator.getHostAPI();
        userAPI = APILocator.getUserAPI();
        user    = userAPI.getSystemUser();

        contentletAPI  = APILocator.getContentletAPI();
        languageAPI    = APILocator.getLanguageAPI();
        defaultHost     = hostAPI.findDefaultHost(user, false);
    }

    /**
     * This test is for issue https://github.com/dotCMS/core/issues/16409
     * Categories should be pulled in Front End
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testGet_showCategories_AsAnonUser() throws DotDataException, DotSecurityException {

        // save proper permissions to SYSTEM_HOST

        final Permission catsPermsSystemHost = new Permission();
        final Role anonUserRole = APILocator.getRoleAPI().loadRoleByKey("CMS Anonymous");
        catsPermsSystemHost.setRoleId(anonUserRole.getId());
        catsPermsSystemHost.setInode(Host.SYSTEM_HOST);
        catsPermsSystemHost.setBitPermission(true);
        catsPermsSystemHost.setType(PermissionType.CATEGORY.getKey());
        catsPermsSystemHost.setPermission(PermissionAPI.PERMISSION_READ);

        APILocator.getPermissionAPI().save(catsPermsSystemHost, APILocator.systemHost(),
                APILocator.systemUser(), false);

        //Create Categories
        final Category categoryChild1 = new CategoryDataGen().setCategoryName("RoadBike-"+System.currentTimeMillis()).setKey("RoadBike").setKeywords("RoadBike").setCategoryVelocityVarName("roadBike").next();
        final Category categoryChild2 = new CategoryDataGen().setCategoryName("MTB-"+System.currentTimeMillis()).setKey("MTB").setKeywords("MTB").setCategoryVelocityVarName("mtb").next();
        final Category rootCategory = new CategoryDataGen().setCategoryName("Bikes-"+System.currentTimeMillis())
                .setKey("Bikes").setKeywords("Bikes").setCategoryVelocityVarName("bikes").children(categoryChild1,categoryChild2).nextPersisted();

        // Get "News" content-type
        final ContentType contentType = TestDataUtils.getNewsLikeContentType("newsCategoriesTest"+System.currentTimeMillis(),rootCategory.getInode());

        // Create dummy "News" content
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.inode())
                .host(defaultHost);

        contentletDataGen.setProperty("title", "Bicycle");
        contentletDataGen.setProperty("byline", "Bicycle");
        contentletDataGen.setProperty("story", "BicycleBicycleBicycle");
        contentletDataGen.setProperty("sysPublishDate", new Date());
        contentletDataGen.setProperty("urlTitle", "/news/bicycle");
        contentletDataGen.addCategory(categoryChild1);
        contentletDataGen.addCategory(categoryChild2);

        // Persist dummy "News" contents to ensure at least one result will be returned
        final Contentlet contentlet = contentletDataGen.nextPersisted();
        ContentletDataGen.publish(contentlet);

        final Context velocityContext = mock(Context.class);

        final ContentMap contentMap = new ContentMap(contentlet, userAPI.getAnonymousUser(),
                PageMode.LIVE,defaultHost,velocityContext);

        //If is null no categories were pulled
        Assert.assertNotNull(contentMap.get("categories"));

        APILocator.getContentTypeAPI(user).delete(contentType);
        APILocator.getCategoryAPI().delete(categoryChild2,user,false);
        APILocator.getCategoryAPI().delete(categoryChild1,user,false);
        APILocator.getCategoryAPI().delete(rootCategory,user,false);


    }

}
