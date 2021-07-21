package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task210719CleanUpTitleFieldTest {

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

        final Task210719CleanUpTitleField upgradeTask = new Task210719CleanUpTitleField();
        upgradeTask.executeUpgrade();

        final List results = new DotConnect()
                .setSQL("select inode from contentlet where title <> null").loadObjectResults();
        assertFalse(UtilMethods.isSet(results));
    }
}
