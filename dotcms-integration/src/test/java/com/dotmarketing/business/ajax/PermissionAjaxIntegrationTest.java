package com.dotmarketing.business.ajax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the permission checks of {@link PermissionAjax}, against the stored
 * permission rows: a back-end user can only remove an asset's permissions with
 * {@link PermissionAPI#PERMISSION_EDIT_PERMISSIONS} on it, and only reads the permissions of an
 * asset it can read.
 */
public class PermissionAjaxIntegrationTest {

    private static final Long LANGUAGE_ID = 1L;

    private static PermissionAPI permissionAPI;
    private static User systemUser;
    private static Host site;
    private static Role ownerRole;
    private static User backendUser;
    private static Role backendUserRole;
    private static WebContextFactory.WebContextBuilder previousWebContextBuilder;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        permissionAPI = APILocator.getPermissionAPI();
        systemUser = APILocator.systemUser();
        ownerRole = new RoleDataGen().nextPersisted();

        site = new SiteDataGen().nextPersisted();
        backendUser = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole())
                .nextPersisted();
        backendUserRole = APILocator.getRoleAPI().getUserRole(backendUser);

        previousWebContextBuilder = currentWebContextBuilder();
        setUpDwrContext(backendUser);
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
     * Gives the asset its own permissions: everything for a separate role and, unless
     * {@code userPermission} is 0, {@code userPermission} for the back-end user's personal role.
     */
    private static void givenIndividualPermissions(final Permissionable asset, final int userPermission)
            throws DotDataException, DotSecurityException {
        permissionAPI.permissionIndividually(site, asset, systemUser);
        permissionAPI.removePermissions(asset);
        permissionAPI.save(new Permission(asset.getPermissionId(), ownerRole.getId(),
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_WRITE
                        | PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true), asset, systemUser, false);
        if (userPermission != 0) {
            permissionAPI.save(new Permission(asset.getPermissionId(), backendUserRole.getId(),
                    userPermission, true), asset, systemUser, false);
        }
    }

    private static List<Permission> individualPermissions(final Permissionable asset)
            throws DotDataException {
        return permissionAPI.getPermissions(asset, true, true);
    }

    /** A folder of the test site the back-end user can read but not edit permissions on. */
    private static Folder readOnlyFolder() throws DotDataException, DotSecurityException {
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        givenIndividualPermissions(folder, PermissionAPI.PERMISSION_READ);
        assertTrue(permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, backendUser, false));
        assertFalse(permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, backendUser, false));
        return folder;
    }

    /** A container of the test site with the given permission for the back-end user (0 for none). */
    private static Container container(final int userPermission) throws DotDataException, DotSecurityException {
        final Container container = new ContainerDataGen().site(site).nextPersisted();
        givenIndividualPermissions(container, userPermission);
        assertEquals(userPermission != 0,
                permissionAPI.doesUserHavePermission(container, PermissionAPI.PERMISSION_READ, backendUser, false));
        return container;
    }

    /**
     * Method to test: {@link PermissionAjax#resetAssetPermissions(String, Long)}
     * Given: a back-end user who can read a folder but can't edit its permissions
     * Expected: DotSecurityException, and the folder keeps its permission rows
     */
    @Test
    public void resetAssetPermissions_folderWithoutEditPermissions_keepsPermissions() throws Exception {
        final Folder folder = readOnlyFolder();
        final List<Permission> before = individualPermissions(folder);

        assertThrows(DotSecurityException.class,
                () -> new PermissionAjax().resetAssetPermissions(folder.getInode(), LANGUAGE_ID));

        assertEquals(before, individualPermissions(folder));
    }

    /**
     * Method to test: {@link PermissionAjax#saveAssetPermissions(String, Long, List, boolean)}
     * Given: a back-end user who can read a folder but can't edit its permissions, and an empty list
     * Expected: DotSecurityException, and the folder keeps its permission rows
     */
    @Test
    public void saveAssetPermissions_emptyListWithoutEditPermissions_keepsPermissions() throws Exception {
        final Folder folder = readOnlyFolder();
        final List<Permission> before = individualPermissions(folder);

        assertThrows(DotSecurityException.class, () -> new PermissionAjax()
                .saveAssetPermissions(folder.getInode(), LANGUAGE_ID, Collections.emptyList(), false));

        assertEquals(before, individualPermissions(folder));
    }

    /**
     * Method to test: {@link PermissionAjax#resetAssetPermissions(String, Long)}
     * Given: a back-end user with no permission at all on a container
     * Expected: DotSecurityException, and the container keeps its permission rows
     */
    @Test
    public void resetAssetPermissions_containerWithoutRead_keepsPermissions() throws Exception {
        final Container container = container(0);
        final List<Permission> before = individualPermissions(container);

        assertThrows(DotSecurityException.class,
                () -> new PermissionAjax().resetAssetPermissions(container.getIdentifier(), LANGUAGE_ID));

        assertEquals(before, individualPermissions(container));
    }

    /**
     * Method to test: {@link PermissionAjax#getAssetPermissions(String, Long)}
     * Given: a back-end user with no permission at all on a container
     * Expected: DotSecurityException
     */
    @Test
    public void getAssetPermissions_containerWithoutRead_isRejected() throws Exception {
        final Container container = container(0);

        assertThrows(DotSecurityException.class,
                () -> new PermissionAjax().getAssetPermissions(container.getIdentifier(), LANGUAGE_ID));
    }

    /**
     * Method to test: {@link PermissionAjax#getAssetPermissions(String, Long)}
     * Given: a back-end user who can read a container but can't edit its permissions
     * Expected: the container's permissions
     */
    @Test
    public void getAssetPermissions_containerWithRead_returnsPermissions() throws Exception {
        final Container container = container(PermissionAPI.PERMISSION_READ);

        final List<Map<String, Object>> permissions =
                new PermissionAjax().getAssetPermissions(container.getIdentifier(), LANGUAGE_ID);

        assertNotNull(permissions);
        assertTrue(permissions.stream().anyMatch(role -> backendUserRole.getId().equals(role.get("id"))));
        assertTrue(permissions.stream().anyMatch(role -> ownerRole.getId().equals(role.get("id"))));
    }

    /**
     * Method to test: {@link PermissionAjax#resetAssetPermissions(String, Long)}
     * Given: a back-end user who can edit a folder's permissions
     * Expected: the folder's own permission rows are removed, so it inherits again
     */
    @Test
    public void resetAssetPermissions_folderWithEditPermissions_removesPermissions() throws Exception {
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        givenIndividualPermissions(folder,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);

        new PermissionAjax().resetAssetPermissions(folder.getInode(), LANGUAGE_ID);

        assertTrue(individualPermissions(folder).isEmpty());
    }

    /**
     * Method to test: {@link PermissionAjax#getAssetPermissions(String, Long)}
     * Given: a back-end user with no permission at all on a folder
     * Expected: DotSecurityException
     */
    @Test
    public void getAssetPermissions_folderWithoutRead_isRejected() throws Exception {
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        givenIndividualPermissions(folder, 0);
        assertFalse(permissionAPI.doesUserHavePermission(folder, PermissionAPI.PERMISSION_READ, backendUser, false));

        assertThrows(DotSecurityException.class,
                () -> new PermissionAjax().getAssetPermissions(folder.getInode(), LANGUAGE_ID));
    }

    /**
     * Method to test: {@link PermissionAjax#saveAssetPermissions(String, Long, List, boolean)}
     * Given: a back-end user who can edit a folder's permissions, and an empty list
     * Expected: the folder's own permission rows are removed, so it inherits again
     */
    @Test
    public void saveAssetPermissions_emptyListWithEditPermissions_removesPermissions() throws Exception {
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        givenIndividualPermissions(folder,
                PermissionAPI.PERMISSION_READ | PermissionAPI.PERMISSION_EDIT_PERMISSIONS);

        new PermissionAjax().saveAssetPermissions(folder.getInode(), LANGUAGE_ID, Collections.emptyList(), false);

        assertTrue(individualPermissions(folder).isEmpty());
    }
}
