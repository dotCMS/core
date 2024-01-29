package com.dotmarketing.factories;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebAssetFactoryTest extends IntegrationTestBase {

    private static User systemUser;
    private static UserAPI userAPI;
    private static User shlub;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        Host site = new SiteDataGen().nextPersisted();
        userAPI = APILocator.getUserAPI();
        systemUser = userAPI.getSystemUser();

        shlub = new UserDataGen().nextPersisted();



        Template t = new TemplateDataGen().site(site).nextPersisted();
        Assert.assertNotNull(t);
        Assert.assertNotNull(t.getInode());
    }

    @Test
    public void testGetAssetsWorkingBytheSystemUser() {
        final List<WebAsset> webAssetList =
                        WebAssetFactory.getAssetsWorkingWithPermission(Template.class, 10, 0, "title", null, systemUser);

        Assert.assertNotNull(webAssetList);
        Assert.assertTrue(!webAssetList.isEmpty());
        Assert.assertTrue(webAssetList.get(0).getInode() != null && !webAssetList.get(0).getInode().isEmpty());
        Assert.assertTrue(webAssetList.get(0).getIdentifier() != null && !webAssetList.get(0).getIdentifier().isEmpty());



    }

    /**
     * Makes sure that a user without permissions cannot get access to working templates
     */
    @Test
    public void testGetAssetsWorkingBy_A_Shlub() {
        final List<WebAsset> webAssetList =
                        WebAssetFactory.getAssetsWorkingWithPermission(Template.class, 10, 0, "title", null, shlub);

        Assert.assertNotNull(webAssetList);
        Assert.assertTrue(webAssetList.isEmpty());




    }
    /**
     * Makes sure that cms ANON cannot get access to working templates
     */
    @Test
    public void testGetAssetsWorkingBy_CMS_ANON() {
        final List<WebAsset> webAssetList =
                        WebAssetFactory.getAssetsWorkingWithPermission(Template.class, 10, 0, "title", null, null);

        Assert.assertNotNull(webAssetList);
        Assert.assertTrue(webAssetList.isEmpty());




    }


}
