package com.dotmarketing.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.ajax.RoleAjax;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Class LayoutAPITest is responsible for testing the functionality of the LayoutAPI. This class
 * extends IntegrationTestBase to use initialization of integration test services. The tested
 * functionalities include creation, update, retrieval, and removal of layouts, resolving layouts
 * based on various request parameters, and verifying user access permissions to portlets based on
 * user roles and permissions.
 * <p>
 * The class uses JUnit framework for defining test cases and assertions.
 *
 * @author Erick Gonzalez
 * @since Jun 29th, 2018
 */
public class LayoutAPITest extends IntegrationTestBase {

    private static LayoutAPI layoutAPI;

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        layoutAPI = APILocator.getLayoutAPI();

        deleteGettingStartedLayout();
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

    HttpServletRequest request = new MockHttpRequestIntegrationTest("localhost", uri).request();
    HttpServletRequest headerRequest = new MockHeaderRequest(request, "referer", referer).request();
    HttpServletRequest paramRequest = new MockParameterRequest(request, ImmutableMap.of("p_l_id", layout1.getId())).request();

    // getting layout from url param
    Assert.assertEquals(layout1, layoutAPI.resolveLayout(paramRequest).get());
    
    // no url param, fall back to the layout specified on the referer
    Assert.assertEquals(layout2, layoutAPI.resolveLayout(headerRequest).get());
    
    // if neither of those, return the last layout visited (from session)
    Assert.assertEquals(layout2, layoutAPI.resolveLayout(request).get());
    
    
    // if there is nothing specified (no param, referer or session, you get nothing)
    Assert.assertFalse(layoutAPI.resolveLayout(new MockHttpRequestIntegrationTest("localhost", uri).request()).isPresent());

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
     * <ul>
     *     <li><b>Method to test:</b> {@link LayoutAPI#doesUserHaveAccessToPortlet(String, User)}
     *     </li>
     *     <li><b>Given Scenario:</b> You should be able to edit content within the Edit Page,
     *     regardless of the portlets you have assigned.</li>
     *     <li><b>Expected Result:</b> If the user has edit permissions, they should be given
     *     access to the portlet.</li>
     * </ul>
     */
    @Test
    public void test_doesUserHaveAccessToPortlet_editPagePortletShouldBeAccessedIfValidPermission() throws DotDataException, DotSecurityException, SystemException, PortalException {
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        //limited user
        final User newUser = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        final User systemUser = APILocator.systemUser();

        // Create a site
        Host site = new Host();
        site.setHostname("testHost"+System.currentTimeMillis());
        site.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        site = APILocator.getHostAPI().save(site, systemUser, false);

        //create a role
        final String roleName = "testRole"+System.currentTimeMillis();
        Role nrole = new Role();
        nrole.setName(roleName);
        nrole.setRoleKey(roleName);
        nrole.setEditUsers(true);
        nrole.setEditPermissions(true);
        nrole.setEditLayouts(true);
        nrole.setDescription(roleName);
        nrole = APILocator.getRoleAPI().save(nrole);

        //validate that user does not have access to the portlet until the permissions are assigned
        assertFalse("The user should not have access to the portlet" , layoutAPI.doesUserHaveAccessToPortlet("edit-page", newUser));

        //assign the role to the user
        roleAPI.addRoleToUser(nrole, newUser);

        //assign the permissions to the role
        Map<String,String> permList=new HashMap<>();
        permList.put("pages", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT));
        permList.put("content", Integer.toString(PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT));
        RoleAjax roleAjax = new RoleAjax();
        roleAjax.saveRolePermission(nrole.getId(), site.getIdentifier(), permList, false);

        //validate that the user does have access to the portlet
        assertTrue("The user should have access to the portlet", layoutAPI.doesUserHaveAccessToPortlet("edit-page", newUser));
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
        //Create the Getting Started Layout
      layoutAPI.findGettingStartedLayout();
      //Find the Getting Started Layout
        Layout gettingStartedLayout = layoutAPI.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
      assertNotNull(gettingStartedLayout);
      assertEquals("Getting Started",gettingStartedLayout.getName());
      assertEquals("whatshot",gettingStartedLayout.getDescription());
    }

    static private void deleteGettingStartedLayout() throws DotDataException {
        Layout gettingStartedLayout = layoutAPI.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
        //If it finds the layout remove it
        if(gettingStartedLayout != null && UtilMethods.isSet(gettingStartedLayout.getId())){
            layoutAPI.removeLayout(gettingStartedLayout);
        } else {
            // try by name, which is unique as well
            gettingStartedLayout = layoutAPI.findLayoutByName(LayoutAPI.GETTING_STARTED_LAYOUT_NAME);
            if(gettingStartedLayout != null && UtilMethods.isSet(gettingStartedLayout.getId())){
                layoutAPI.removeLayout(gettingStartedLayout);
            }
        }
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
     * Given Scenario: Add the getting started layout successfully to an user. Then try to add it again,
     *                  no error should be thrown but the layout should not be re-added.
     * ExpectedResult: Layout successfully added to the user.
     *
     */
    @Test
    public void test_addLayoutForUser_reAddSameLayout_Success() throws DotDataException {
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
        assertEquals(1,listOfLayouts.size());
        assertEquals("Getting Started",listOfLayouts.stream().findFirst().get().getName());
        assertEquals("whatshot",listOfLayouts.stream().findFirst().get().getDescription());

        //Re-Add the layout to User
        layoutAPI.addLayoutForUser(gettingStartedLayout,newUser);
        //Check that the layouts size is still the same
        listOfLayouts = layoutAPI.loadLayoutsForUser(newUser);
        assertFalse(listOfLayouts.isEmpty());
        assertEquals(1,listOfLayouts.size());
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
