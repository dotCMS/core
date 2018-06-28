package com.dotmarketing.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LayoutAPITest extends IntegrationTestBase {

    private static LayoutAPI layoutAPI;

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        layoutAPI = APILocator.getLayoutAPI();
    }


    @Test
    public void test_SaveLayout_WhenCreateNewLayout_Success() throws DotDataException {
        Layout layout = null;
        try {
            layout = createNewLayout("testNewLayout", "", 0);
            Assert.assertNotNull(layout);
        }finally {
            if(layout != null){
                layoutAPI.removeLayout(layout);
            }
        }
    }

    @Test
    public void test_SaveLayout_WhenCreateAndUpdateNewLayout_Success() throws DotDataException {
        Layout layout = null;
        try {
            layout = createNewLayout("testNewLayout", "", 0);
            Assert.assertNotNull(layout);

            layout.setName("testUpdateLayout");
            layout.setDescription("");
            layout.setTabOrder(1);

            layoutAPI.saveLayout(layout);
            final Layout updatedLayout = layoutAPI.loadLayout(layout.getId());
            Assert.assertNotNull(updatedLayout);
            Assert.assertEquals("testUpdateLayout",updatedLayout.getName());
            Assert.assertEquals(1,updatedLayout.getTabOrder());

        }finally {
            if(layout != null){
                layoutAPI.removeLayout(layout);
            }
        }
    }

    private Layout createNewLayout(final String layoutName, final String layoutDescription, final int order) throws DotDataException {
        Layout newLayout = new Layout();
        newLayout.setName(layoutName);
        newLayout.setDescription(layoutDescription);
        newLayout.setTabOrder(order);
        layoutAPI.saveLayout(newLayout);

        return layoutAPI.findLayoutByName(layoutName);
    }

}
