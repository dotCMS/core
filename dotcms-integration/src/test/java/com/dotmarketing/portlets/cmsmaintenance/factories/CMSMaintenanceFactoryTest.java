package com.dotmarketing.portlets.cmsmaintenance.factories;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CMSMaintenanceFactoryTest {

    private static User adminUser;
    private static ContentletAPI contentletAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        adminUser = TestUserUtils.getAdminUser();
        contentletAPI = APILocator.getContentletAPI();
    }

    /**
     * Method to test: {@link CMSMaintenanceFactory#deleteOldAssetVersions(Date)}
     * Given Scenario: Create a contentlet that will have a few of old versions, that are gonna be deleted.
     * ExpectedResult: Old versions of the contentlet (that not are live or working) are deleted.
     */
    @Test
    public void Test_deleteOldAssetVersions_success()
            throws DotSecurityException, DotDataException {
        //Create a site
        final Host site = new SiteDataGen().nextPersisted();
        //Create a contentlet, this version will have today's Date
        Contentlet contentlet = TestDataUtils.getGenericContentContent(true,APILocator.getLanguageAPI().getDefaultLanguage().getId(),site);
        //Create a couple of new versions, with an old Date so these will be deleted
        //Using the "_use_mod_date" so when checkin the contentlet use the Date that is set
        for(int i=0;i<100;i++) {
            contentlet.setInode(UUIDGenerator.generateUuid());
            contentlet.getMap()
                    .put("_use_mod_date", DateUtil.addDate(new Date(), Calendar.MONTH, -2));
            contentlet = contentletAPI.checkin(contentlet, adminUser, false);
        }
        //Create a new version it is the working version of the contentlet, so the above can be deleted
        contentlet.setInode(UUIDGenerator.generateUuid());
        contentlet = contentletAPI.checkin(contentlet,adminUser,false);

        //Check that the contentlet has diff versions
        final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet.getIdentifier());
        final List<Contentlet> contentletVersionListBeforeDelete = contentletAPI.findAllVersions(identifier,adminUser,false);
        Assert.assertFalse(contentletVersionListBeforeDelete.isEmpty());

        //DeleteOldAssetVersions, using current Date
        final int amountOfVersionsDeleted = CMSMaintenanceFactory.deleteOldAssetVersions(new Date());

        //Assert that some contentlets were deleted
        Assert.assertNotEquals(0,amountOfVersionsDeleted);
        final List<Contentlet> contentletVersionListAfterDelete = contentletAPI.findAllVersions(identifier,adminUser,false);
        Assert.assertFalse(contentletVersionListAfterDelete.isEmpty());
        Assert.assertNotEquals(contentletVersionListBeforeDelete.size(),contentletVersionListAfterDelete.size());


    }
}
