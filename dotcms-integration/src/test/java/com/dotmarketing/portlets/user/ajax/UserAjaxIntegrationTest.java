package com.dotmarketing.portlets.user.ajax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.LayoutDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Layout;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for {@link UserAjax#updateUserRoles(String, List)}: only a CMS Administrator
 * can add or remove roles through the Users screen, a save that does not change the roles keeps
 * working for any caller with access to the Users tool, and role ids that match no role are
 * skipped.
 */
public class UserAjaxIntegrationTest {

    private static RoleAPI roleAPI;
    private static Role usersToolRole;
    private static WebContextFactory.WebContextBuilder previousWebContextBuilder;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        roleAPI = APILocator.getRoleAPI();

        final Layout usersLayout = new LayoutDataGen()
                .name("Users tool " + UUIDGenerator.shorty()).portletIds("users").nextPersisted();
        usersToolRole = new RoleDataGen().layout(usersLayout).nextPersisted();

        previousWebContextBuilder = currentWebContextBuilder();
    }

    /**
     * Puts back the DWR context that was attached before this class ran, so later tests in the same
     * JVM don't get this class's mocked request.
     */
    @AfterClass
    public static void restoreDwrContext() {
        attachWebContextBuilder(previousWebContextBuilder);
    }

    /**
     * Reads the builder {@code WebContextFactory} currently uses; the field is private and has no
     * getter.
     */
    private static WebContextFactory.WebContextBuilder currentWebContextBuilder() throws Exception {
        final Field builder = WebContextFactory.class.getDeclaredField("builder");
        builder.setAccessible(true);
        return (WebContextFactory.WebContextBuilder) builder.get(null);
    }

    /**
     * Makes {@code WebContextFactory} use the given builder ({@code null} detaches it).
     */
    private static void attachWebContextBuilder(final WebContextFactory.WebContextBuilder webContextBuilder) {
        final com.dotcms.repackage.org.directwebremoting.Container dwrContainer =
                mock(com.dotcms.repackage.org.directwebremoting.Container.class);
        when(dwrContainer.getBean(WebContextFactory.WebContextBuilder.class)).thenReturn(webContextBuilder);
        WebContextFactory.attach(dwrContainer);
    }

    /**
     * Makes {@code WebContextFactory.get()} return a request whose logged-in user is the given one.
     */
    private static void setUpDwrContext(final User user) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession()).thenReturn(mock(HttpSession.class));
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);

        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(request);

        final WebContextFactory.WebContextBuilder webContextBuilder =
                mock(WebContextFactory.WebContextBuilder.class);
        when(webContextBuilder.get()).thenReturn(webContext);

        attachWebContextBuilder(webContextBuilder);
    }

    /**
     * Creates a back-end user, not a CMS Administrator, whose only tool is Users, holding the given
     * extra roles, and makes them the logged-in user.
     */
    private static User newUserManagerLoggedIn(final Role... extraRoles) throws DotDataException {
        final List<Role> roles = new ArrayList<>(List.of(TestUserUtils.getBackendRole(), usersToolRole));
        roles.addAll(List.of(extraRoles));
        final User manager = new UserDataGen().roles(roles.toArray(new Role[0])).nextPersisted();
        assertFalse("precondition: the caller must not be a CMS Administrator", manager.isAdmin());
        assertTrue("precondition: the caller must have the Users tool",
                APILocator.getLayoutAPI().doesUserHaveAccessToPortlet("users", manager));
        setUpDwrContext(manager);
        return manager;
    }

    /** The ids of the roles the user holds directly, as a new mutable list. */
    private static List<String> directRoleIds(final User user) throws DotDataException {
        return roleAPI.loadRolesForUser(user.getUserId(), false).stream()
                .map(Role::getId).collect(Collectors.toCollection(ArrayList::new));
    }

    private static Set<String> directRoleIdSet(final User user) throws DotDataException {
        return Set.copyOf(directRoleIds(user));
    }

    /**
     * Method to test: {@link UserAjax#updateUserRoles(String, List)}
     * Given Scenario: A user with the Users tool who is not a CMS Administrator saves their own roles
     * with the CMS Administrator role added.
     * Expected Result: {@link DotSecurityException}; the user is still not a CMS Administrator and
     * their roles are unchanged.
     */
    @Test
    public void updateUserRoles_nonAdminAddingAdministratorRole_refused() throws Exception {
        final User manager = newUserManagerLoggedIn();
        final Set<String> before = directRoleIdSet(manager);
        final List<String> requested = directRoleIds(manager);
        requested.add(roleAPI.loadCMSAdminRole().getId());

        assertThrows(DotSecurityException.class,
                () -> new UserAjax().updateUserRoles(manager.getUserId(), requested));

        assertFalse(manager.isAdmin());
        assertEquals(before, directRoleIdSet(manager));
    }

    /**
     * Method to test: {@link UserAjax#updateUserRoles(String, List)}
     * Given Scenario: A user with the Users tool who is not a CMS Administrator saves their own roles
     * with an ordinary assignable role added.
     * Expected Result: {@link DotSecurityException}; the role is not granted.
     */
    @Test
    public void updateUserRoles_nonAdminAddingOrdinaryRole_refused() throws Exception {
        final User manager = newUserManagerLoggedIn();
        final Role ordinary = new RoleDataGen().nextPersisted();
        final List<String> requested = directRoleIds(manager);
        requested.add(ordinary.getId());

        assertThrows(DotSecurityException.class,
                () -> new UserAjax().updateUserRoles(manager.getUserId(), requested));

        assertFalse(roleAPI.doesUserHaveRole(manager, ordinary));
    }

    /**
     * Method to test: {@link UserAjax#updateUserRoles(String, List)}
     * Given Scenario: A user with the Users tool who is not a CMS Administrator holds an extra
     * assignable role and saves their own roles without it.
     * Expected Result: {@link DotSecurityException}; the role is still held.
     */
    @Test
    public void updateUserRoles_nonAdminRemovingOwnRole_refused() throws Exception {
        final Role extra = new RoleDataGen().nextPersisted();
        final User manager = newUserManagerLoggedIn(extra);
        final List<String> requested = directRoleIds(manager);
        requested.remove(extra.getId());

        assertThrows(DotSecurityException.class,
                () -> new UserAjax().updateUserRoles(manager.getUserId(), requested));

        assertTrue(roleAPI.doesUserHaveRole(manager, extra));
    }

    /**
     * Method to test: {@link UserAjax#updateUserRoles(String, List)}
     * Given Scenario: A user with the Users tool who is not a CMS Administrator saves their own roles
     * without changing them.
     * Expected Result: No error; the roles are identical before and after.
     */
    @Test
    public void updateUserRoles_nonAdminUnchangedRoles_succeeds() throws Exception {
        final User manager = newUserManagerLoggedIn();
        final Set<String> before = directRoleIdSet(manager);

        new UserAjax().updateUserRoles(manager.getUserId(), directRoleIds(manager));

        assertEquals(before, directRoleIdSet(manager));
    }

    /**
     * Method to test: {@link UserAjax#updateUserRoles(String, List)}
     * Given Scenario: A user with the Users tool who is not a CMS Administrator holds an extra
     * assignable role and saves their own roles without it and with an ordinary new role added.
     * Expected Result: {@link DotSecurityException}; nothing changes: the extra role is still held and
     * the new role is not granted.
     */
    @Test
    public void updateUserRoles_nonAdminAddingAndRemovingRoles_refused() throws Exception {
        final Role extra = new RoleDataGen().nextPersisted();
        final Role ordinary = new RoleDataGen().nextPersisted();
        final User manager = newUserManagerLoggedIn(extra);
        final List<String> requested = directRoleIds(manager);
        requested.remove(extra.getId());
        requested.add(ordinary.getId());

        assertThrows(DotSecurityException.class,
                () -> new UserAjax().updateUserRoles(manager.getUserId(), requested));

        assertTrue(roleAPI.doesUserHaveRole(manager, extra));
        assertFalse(roleAPI.doesUserHaveRole(manager, ordinary));
    }

    /**
     * Method to test: {@link UserAjax#updateUserRoles(String, List)}
     * Given Scenario: A user with the Users tool who is not a CMS Administrator saves their own roles
     * exactly as the Users screen lists them ({@link UserAjax#getUserRoles(String)}: direct roles
     * without the user's personal role).
     * Expected Result: No error; the roles are identical before and after.
     */
    @Test
    public void updateUserRoles_nonAdminSavingRolesAsListedByScreen_succeeds() throws Exception {
        final User manager = newUserManagerLoggedIn(new RoleDataGen().nextPersisted());
        final Set<String> before = directRoleIdSet(manager);

        final List<String> listedByScreen = new UserAjax().getUserRoles(manager.getUserId()).stream()
                .map(roleMap -> roleMap.get("id").toString())
                .collect(Collectors.toCollection(ArrayList::new));
        assertFalse("precondition: the screen does not list the personal role",
                listedByScreen.contains(roleAPI.getUserRole(manager).getId()));

        new UserAjax().updateUserRoles(manager.getUserId(), listedByScreen);

        assertEquals(before, directRoleIdSet(manager));
    }

    /**
     * Method to test: {@link UserAjax#updateUserRoles(String, List)}
     * Given Scenario: A user holds parent role P directly and its child role C through it. A CMS
     * Administrator saves the user's roles with C instead of P.
     * Expected Result: P is removed and C is now held directly, so the user keeps C.
     */
    @Test
    public void updateUserRoles_adminReplacingParentRoleWithChild_childGrantedDirectly() throws Exception {
        final Role parent = new RoleDataGen().nextPersisted();
        final Role child = new RoleDataGen().parent(parent.getId()).nextPersisted();
        final User target = new UserDataGen().roles(TestUserUtils.getBackendRole(), parent).nextPersisted();
        assertTrue("precondition: the child role must be inherited", roleAPI.doesUserHaveRole(target, child));
        setUpDwrContext(TestUserUtils.getAdminUser());

        final List<String> requested = directRoleIds(target);
        requested.remove(parent.getId());
        requested.add(child.getId());
        new UserAjax().updateUserRoles(target.getUserId(), requested);

        final Set<String> direct = directRoleIdSet(target);
        assertFalse("the parent role must be removed", direct.contains(parent.getId()));
        assertTrue("the child role must be held directly", direct.contains(child.getId()));
        assertTrue(roleAPI.doesUserHaveRole(target, child));
    }

    /**
     * Method to test: {@link UserAjax#addUser(String, String, String, String, String)}
     * Given Scenario: A user with the Users tool who is not a CMS Administrator creates a user from the
     * Users screen.
     * Expected Result: {@link DotSecurityException}; no user is created. Creating users requires a CMS
     * Administrator on this path as on {@code POST /api/v1/users}.
     */
    @Test
    public void addUser_nonAdmin_refusedAndNoUserCreated() throws Exception {
        newUserManagerLoggedIn();
        final String newUserId = "addedbymanager-" + UUIDGenerator.shorty();

        assertThrows(DotSecurityException.class, () -> new UserAjax().addUser(newUserId, "Added",
                "ByManager", newUserId + "@dotcms.com", "Passw0rd!" + UUIDGenerator.shorty()));

        assertThrows(com.dotmarketing.business.NoSuchUserException.class,
                () -> APILocator.getUserAPI().loadUserById(newUserId));
    }

    /**
     * Method to test: {@link UserAjax#updateUserRoles(String, List)}
     * Given Scenario: A CMS Administrator saves another user's roles, dropping role A and adding role B.
     * Expected Result: Role A is removed and role B is added, as before.
     */
    @Test
    public void updateUserRoles_adminChangesAnotherUsersRoles_succeeds() throws Exception {
        final Role roleA = new RoleDataGen().nextPersisted();
        final Role roleB = new RoleDataGen().nextPersisted();
        final User target = new UserDataGen().roles(TestUserUtils.getBackendRole(), roleA).nextPersisted();
        setUpDwrContext(TestUserUtils.getAdminUser());

        final List<String> requested = directRoleIds(target);
        requested.remove(roleA.getId());
        requested.add(roleB.getId());
        new UserAjax().updateUserRoles(target.getUserId(), requested);

        assertFalse(roleAPI.doesUserHaveRole(target, roleA));
        assertTrue(roleAPI.doesUserHaveRole(target, roleB));
        assertTrue(roleAPI.doesUserHaveRole(target, TestUserUtils.getBackendRole()));
    }

    /**
     * Method to test: {@link UserAjax#updateUserRoles(String, List)}
     * Given Scenario: A CMS Administrator saves a user's current roles plus an id that matches no role.
     * Expected Result: No error; the unknown id is skipped and the roles are unchanged.
     */
    @Test
    public void updateUserRoles_unknownRoleIdIgnored() throws Exception {
        final User target = new UserDataGen().roles(TestUserUtils.getBackendRole()).nextPersisted();
        setUpDwrContext(TestUserUtils.getAdminUser());
        final Set<String> before = directRoleIdSet(target);

        final List<String> requested = directRoleIds(target);
        requested.add("no-such-role-" + UUIDGenerator.shorty());
        new UserAjax().updateUserRoles(target.getUserId(), requested);

        assertEquals(before, directRoleIdSet(target));
    }

}