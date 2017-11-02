package com.dotmarketing.factories;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
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

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        userAPI = APILocator.getUserAPI();
        systemUser = userAPI.getSystemUser();
    }

    @Test
    public void testGetAssetsWorkingWithPermission() {
        List<WebAsset> webAssetList = WebAssetFactory
                .getAssetsWorkingWithPermission(Template.class, 10, 0, "title", null, systemUser);

        Assert.assertNotNull(webAssetList);
        Assert.assertTrue(!webAssetList.isEmpty());
        Assert.assertTrue(webAssetList.get(0).getInode()!=null && !webAssetList.get(0).getInode().isEmpty());
        Assert.assertTrue(webAssetList.get(0).getIdentifier()!=null && !webAssetList.get(0).getIdentifier().isEmpty());
    }

}
