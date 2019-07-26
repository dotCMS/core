package com.dotmarketing.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDGenerator;
import com.google.common.collect.ImmutableMap;

import javax.servlet.http.HttpServletRequest;

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
            layout = createNewLayout("testNewLayout"+ UUIDGenerator.generateUuid(), "", 0);
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
            layout = createNewLayout("testNewLayout"+ UUIDGenerator.generateUuid(), "", 0);
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
        final Layout newLayout = new Layout();
        newLayout.setName(layoutName);
        newLayout.setDescription(layoutDescription);
        newLayout.setTabOrder(order);
        layoutAPI.saveLayout(newLayout);

        return layoutAPI.findLayoutByName(layoutName);
    }
    
    
    @Test
    public void test_resolveLayout() throws DotDataException {
        Layout layout1, layout2 = null;
 
            layout1 = createNewLayout("testNewLayout" + UUIDGenerator.generateUuid(), "", 0);
            layout2 = createNewLayout("testNewLayout2"+ UUIDGenerator.generateUuid(), "", 1);
      

            String uri ="/c/portal/layout";
            String referer ="/c/portal/layout?p_l_id=" + layout2.getId() + "&p_p_id=content&p_p_action=0&&dm_rlout=1&r=1563999037622&in_frame=true&frame=detailFrame&container=true&angularCurrentPortlet=content";
            
            
            HttpServletRequest request = new MockHttpRequest("localhost",uri).request();
            HttpServletRequest headerRequest = new MockHeaderRequest(request, "referer", referer).request();
            HttpServletRequest paramRequest = new MockParameterRequest(request,ImmutableMap.of("p_l_id", layout1.getId())).request();
            
            Assert.assertEquals(layout1, layoutAPI.resolveLayout(paramRequest).get());
            Assert.assertEquals(layout2, layoutAPI.resolveLayout(headerRequest).get());
            Assert.assertFalse(layoutAPI.resolveLayout(request).isPresent());
    
    }
    
    
    
    
    
    
    

}
