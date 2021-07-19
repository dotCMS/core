package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.db.DotDatabaseMetaData;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task210719CleanUpDotAssetTitleTest {

    private static ContentletAPI contentletAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        contentletAPI = APILocator.getContentletAPI();
        user = APILocator.getUserAPI().getSystemUser();
    }

    @Test
    public void testExecuteUpgrade() throws DotDataException, DotSecurityException {
        //Add columns before running task
        Contentlet contentlet = createDotAssets();

        assertEquals(contentlet.getIdentifier(), contentlet.getTitle());
        final Task210719CleanUpDotAssetTitle upgradeTask = new Task210719CleanUpDotAssetTitle();
        upgradeTask.executeUpgrade();

        contentlet = contentletAPI.find(contentlet.getInode(), user, false);
        assertNotEquals(contentlet.getIdentifier(), contentlet.getTitle());
    }

    private Contentlet createDotAssets() throws DotSecurityException, DotDataException {
        Contentlet contentlet = TestDataUtils.getDotAssetLikeContentlet();
        contentlet = contentletAPI.checkout(contentlet.getInode(), user, false);

        //set invalid title
        contentlet.setProperty("title", contentlet.getIdentifier());
        contentlet = contentletAPI.checkin(contentlet, user, false);

        return contentlet;
    }

}
