package com.dotmarketing.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
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
    layout2 = createNewLayout("testNewLayout2" + UUIDGenerator.generateUuid(), "", 1);

    String uri = "/c/portal/layout";
    String referer = "/c/portal/layout?p_l_id=" + layout2.getId()
        + "&p_p_id=content&p_p_action=0&&dm_rlout=1&r=1563999037622&in_frame=true&frame=detailFrame&container=true&angularCurrentPortlet=content";

    HttpServletRequest request = new MockHttpRequest("localhost", uri).request();
    HttpServletRequest headerRequest = new MockHeaderRequest(request, "referer", referer).request();
    HttpServletRequest paramRequest = new MockParameterRequest(request, ImmutableMap.of("p_l_id", layout1.getId())).request();

    // getting layout from url param
    Assert.assertEquals(layout1, layoutAPI.resolveLayout(paramRequest).get());
    
    // no url param, fall back to the layout specified on the referer
    Assert.assertEquals(layout2, layoutAPI.resolveLayout(headerRequest).get());
    
    // if neither of those, return the last layout visited (from session)
    Assert.assertEquals(layout2, layoutAPI.resolveLayout(request).get());
    
    
    // if there is nothing specified (no param, referer or session, you get nothing)
    Assert.assertFalse(layoutAPI.resolveLayout(new MockHttpRequest("localhost", uri).request()).isPresent());

  }
    
    
  /**
   * this test insures that a null user or portletId 
   * passed to the LayoutAPI.doesUserHaveAccessToPortlet
   * does not throw an error and instead returns false
   * @throws DotDataException
   */
  @Test
  public void test_doesUserHaveAccessToPortlet() throws DotDataException {
    Layout layout1, layout2 = null;
    User user = APILocator.systemUser();
    User anonUser = APILocator.getUserAPI().getAnonymousUserNoThrow();


    assertTrue("null user returns false" , !layoutAPI.doesUserHaveAccessToPortlet("content", null));
    
    assertTrue("null portlet returns false" , !layoutAPI.doesUserHaveAccessToPortlet(null, user));
    
    assertTrue("anonUser has no layouts returns false" , !layoutAPI.doesUserHaveAccessToPortlet(null, anonUser));
    
    assertTrue("systemUser has all layouts returns true" , !layoutAPI.doesUserHaveAccessToPortlet("content", anonUser));
    
  }

    /**
     * Method to test: {@link LayoutAPI#findGettingStartedLayout()}
     * Given Scenario: Try to get the Getting Started Layout, if exists remove it. And call the
     *                  findGettingStartedLayout method that will create the layout if not exists.
     * ExpectedResult: Getting Started Layout successfully created.
     *
     */
    @Test
    public void test_findGettingStartedLayout_Success() throws DotDataException {
      //Find the Getting Started Layout
      Layout gettingStartedLayout = layoutAPI.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
      //If it finds the layout remove it
      if(gettingStartedLayout != null && UtilMethods.isSet(gettingStartedLayout.getId())){
          layoutAPI.removeLayout(gettingStartedLayout);
      }
      //Create the Getting Started Layout
      gettingStartedLayout = layoutAPI.findGettingStartedLayout();
      //Find the Getting Started Layout
      gettingStartedLayout = layoutAPI.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
      assertNotNull(gettingStartedLayout);
      assertEquals("Getting Started",gettingStartedLayout.getName());
      assertEquals("whatshot",gettingStartedLayout.getDescription());
    }

    /**
     * Method to test: {@link LayoutAPI#addLayoutForUser(Layout, User)}
     * Given Scenario: Add the getting started layout successfully to an user.
     * ExpectedResult: Layout successfully added to the user.
     *
     */
    @Test
    public void test_addLayoutForUser_Success() throws DotDataException {
      //Create an user
      final User newUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
      //Check layouts for user, should be empty
      List<Layout> listOfLayouts = layoutAPI.loadLayoutsForUser(newUser);
      assertTrue(listOfLayouts.isEmpty());
      //Add the Getting Started Layout to User
      final Layout gettingStartedLayout = layoutAPI.findGettingStartedLayout();
      layoutAPI.addLayoutForUser(gettingStartedLayout,newUser);
      //Check that the layout was added to the user
      listOfLayouts = layoutAPI.loadLayoutsForUser(newUser);
      assertFalse(listOfLayouts.isEmpty());
      assertEquals("Getting Started",listOfLayouts.stream().findFirst().get().getName());
      assertEquals("whatshot",listOfLayouts.stream().findFirst().get().getDescription());
    }

    /**
     * Method to test: {@link LayoutAPI#addLayoutForUser(Layout, User)}
     * Given Scenario: Tries to add a NULL layout to an user.
     * ExpectedResult: DotDataException
     *
     */
    @Test(expected = DotDataException.class)
    public void test_addLayoutForUser_LayoutNull() throws DotDataException {
        //Create an user
        final User newUser = new UserDataGen().roles(TestUserUtils.getFrontendRole(), TestUserUtils.getBackendRole()).nextPersisted();
        //Check layouts for user, should be empty
        List<Layout> listOfLayouts = layoutAPI.loadLayoutsForUser(newUser);
        assertTrue(listOfLayouts.isEmpty());
        //Add the null Layout to User
        layoutAPI.addLayoutForUser(null,newUser);
        //Check that the layout was added to the user
        listOfLayouts = layoutAPI.loadLayoutsForUser(newUser);
        assertTrue(listOfLayouts.isEmpty());
    }

    /**
     * Method to test: {@link LayoutAPI#addLayoutForUser(Layout, User)}
     * Given Scenario: Tries to add a layout to a null user.
     * ExpectedResult: DotDataException
     *
     */
    @Test(expected = DotDataException.class)
    public void test_addLayoutForUser_UserNull() throws DotDataException {
        //Add the Layout to User
        final Layout gettingStartedLayout = layoutAPI.findGettingStartedLayout();
        layoutAPI.addLayoutForUser(gettingStartedLayout,null);
    }
    

}
