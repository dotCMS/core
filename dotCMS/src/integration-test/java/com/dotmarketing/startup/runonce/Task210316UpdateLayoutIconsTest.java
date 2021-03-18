package com.dotmarketing.startup.runonce;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotDataException;
import java.util.Collections;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Task210316UpdateLayoutIconsTest {

    private static LayoutAPI layoutAPI;

    @BeforeClass
    public static void prepare() throws Exception{
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        layoutAPI = APILocator.getLayoutAPI();
    }

    private void insertLayoutWithFaIcon(final String layoutName) throws DotDataException {
        final Layout layout = new Layout();
        layout.setName(layoutName);
        layout.setDescription("fa-icon-1");
        layout.setPortletIds(Collections.emptyList());
        layout.setTabOrder(-3200);
        layoutAPI.saveLayout(layout);
    }

    @Test
    public void test_upgradeTask() throws DotDataException {
        final String layoutName = "testLayoutUT" + System.currentTimeMillis();
        final Task210316UpdateLayoutIcons task = new Task210316UpdateLayoutIcons();
        //Create one layout with fa icon
        insertLayoutWithFaIcon(layoutName);
        //Check if forceRun is true means that at least 1 Layout has FA icons
        Assert.assertTrue(task.forceRun());
        //Run the UT
        task.executeUpgrade();
        //Check that forceRun is false means no layout has FA icons
        Assert.assertFalse(task.forceRun());
    }

}
