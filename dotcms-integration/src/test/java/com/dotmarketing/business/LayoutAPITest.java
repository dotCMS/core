package com.dotmarketing.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.LayoutDataGen;
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

    // ==================== #37353: Getting Started lookup resolves by fixed id ====================

    /** Puts the product's Getting Started section back to its defaults. */
    private static void resetGettingStarted() throws DotDataException {
        deleteGettingStartedLayout();
        layoutAPI.findGettingStartedLayout();
    }

    /**
     * Method to test: {@link LayoutAPI#findGettingStartedLayout()}
     * Given: the Getting Started section was renamed, re-iconed and moved
     * Expected: the lookup returns the section as edited and does not rewrite it
     */
    @Test
    public void findGettingStartedLayout_keepsRenamedIconedMovedSection() throws DotDataException {
        resetGettingStarted();
        try {
            final Layout edited = layoutAPI.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
            edited.setName("Onboarding " + UUIDGenerator.shorty());
            edited.setDescription("rocket_launch");
            edited.setTabOrder(777777);
            layoutAPI.saveLayout(edited);

            final Layout resolved = layoutAPI.findGettingStartedLayout();

            assertEquals(LayoutAPI.GETTING_STARTED_LAYOUT_ID, resolved.getId());
            assertEquals(edited.getName(), resolved.getName());
            assertEquals("rocket_launch", resolved.getDescription());
            assertEquals(777777, resolved.getTabOrder());
            assertEquals(List.of("starter"), resolved.getPortletIds());
            final Layout stored = layoutAPI.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
            assertEquals(edited.getName(), stored.getName());
            assertEquals(777777, stored.getTabOrder());
        } finally {
            resetGettingStarted();
        }
    }

    /**
     * Method to test: {@link LayoutAPI#findGettingStartedLayout()}
     * Given: no section has the fixed id, but a section named "Getting Started" exists (an
     *        install that predates the fixed id)
     * Expected: that section is adopted as it is and no second section is created
     */
    @Test
    public void findGettingStartedLayout_adoptsSectionByName_whenFixedIdMissing() throws DotDataException {
        deleteGettingStartedLayout();
        final Layout legacy = new LayoutDataGen().name(LayoutAPI.GETTING_STARTED_LAYOUT_NAME)
                .description("legacy-icon").portletIds("starter", "templates").tabOrder(-5).nextPersisted();
        try {
            final Layout resolved = layoutAPI.findGettingStartedLayout();

            assertEquals(legacy.getId(), resolved.getId());
            assertEquals("legacy-icon", resolved.getDescription());
            assertEquals(List.of("starter", "templates"), resolved.getPortletIds());
            final Layout fixed = layoutAPI.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
            assertTrue("a second Getting Started was created", fixed == null || !UtilMethods.isSet(fixed.getId()));
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(legacy.getId()));
            resetGettingStarted();
        }
    }

    /**
     * Method to test: {@link LayoutAPI#findGettingStartedLayout()}
     * Given: neither the fixed id nor the name exists
     * Expected: the section is created with its defaults
     */
    @Test
    public void findGettingStartedLayout_createsDefaults_whenNeitherExists() throws DotDataException {
        deleteGettingStartedLayout();
        try {
            final Layout resolved = layoutAPI.findGettingStartedLayout();

            assertEquals(LayoutAPI.GETTING_STARTED_LAYOUT_ID, resolved.getId());
            assertEquals(LayoutAPI.GETTING_STARTED_LAYOUT_NAME, resolved.getName());
            assertEquals("whatshot", resolved.getDescription());
            assertEquals(-320000, resolved.getTabOrder());
            assertEquals(List.of("starter"), resolved.getPortletIds());
        } finally {
            resetGettingStarted();
        }
    }

    /**
     * Method to test: {@link LayoutAPI#findGettingStartedLayout()}
     * Given: the Getting Started section exists, renamed, but holds no tools
     * Expected: the welcome tool is restored and nothing else changes
     */
    @Test
    public void findGettingStartedLayout_restoresStarterTool_whenEmpty_andTouchesNothingElse() throws DotDataException {
        resetGettingStarted();
        try {
            final Layout edited = layoutAPI.findLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID);
            edited.setName("Empty GS " + UUIDGenerator.shorty());
            layoutAPI.saveLayout(edited);
            layoutAPI.setPortletIdsToLayout(edited, List.of());
            assertTrue(layoutAPI.loadLayout(edited.getId()).getPortletIds().isEmpty());

            final Layout resolved = layoutAPI.findGettingStartedLayout();

            assertEquals(List.of("starter"), resolved.getPortletIds());
            assertEquals(edited.getName(), resolved.getName());
            assertEquals(List.of("starter"), layoutAPI.loadLayout(LayoutAPI.GETTING_STARTED_LAYOUT_ID).getPortletIds());
        } finally {
            resetGettingStarted();
        }
    }

    // ==================== #37353: setTabOrders writes every position in one transaction ====================

    /**
     * Method to test: {@link LayoutAPI#setTabOrders(Map)}
     * Given: three sections and a new position for each
     * Expected: every position is written and the navigation order follows
     */
    @Test
    public void setTabOrders_writesEveryPosition() throws DotDataException {
        final Layout a = new LayoutDataGen().name("Pos A " + UUIDGenerator.shorty()).tabOrder(910001).nextPersisted();
        final Layout b = new LayoutDataGen().name("Pos B " + UUIDGenerator.shorty()).tabOrder(910002).nextPersisted();
        final Layout c = new LayoutDataGen().name("Pos C " + UUIDGenerator.shorty()).tabOrder(910003).nextPersisted();
        try {
            layoutAPI.setTabOrders(Map.of(a.getId(), 910003, b.getId(), 910001, c.getId(), 910002));

            assertEquals(910003, layoutAPI.loadLayout(a.getId()).getTabOrder());
            assertEquals(910001, layoutAPI.loadLayout(b.getId()).getTabOrder());
            assertEquals(910002, layoutAPI.loadLayout(c.getId()).getTabOrder());
            final List<String> tail = layoutAPI.findAllLayouts().stream().map(Layout::getId)
                    .filter(id -> List.of(a.getId(), b.getId(), c.getId()).contains(id)).collect(java.util.stream.Collectors.toList());
            assertEquals(List.of(b.getId(), c.getId(), a.getId()), tail);
        } finally {
            for (final Layout l : List.of(a, b, c)) {
                layoutAPI.removeLayout(layoutAPI.findLayout(l.getId()));
            }
        }
    }

    /**
     * Method to test: {@link LayoutAPI#setTabOrders(Map)}
     * Given: a map holding an id no section has
     * Expected: the call fails and no position changes
     */
    @Test
    public void setTabOrders_unknownId_writesNothing() throws DotDataException {
        final Layout a = new LayoutDataGen().name("Pos X " + UUIDGenerator.shorty()).tabOrder(910011).nextPersisted();
        try {
            try {
                layoutAPI.setTabOrders(Map.of(a.getId(), 910099, "no-such-" + UUIDGenerator.shorty(), 1));
                Assert.fail("unknown id accepted");
            } catch (final DotDataException expected) {
                // ok
            }
            assertEquals(910011, layoutAPI.loadLayout(a.getId()).getTabOrder());
        } finally {
            layoutAPI.removeLayout(layoutAPI.findLayout(a.getId()));
        }
    }
}
