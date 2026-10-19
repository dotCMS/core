package com.dotmarketing.business.ajax;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.repackage.org.directwebremoting.WebContext;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Unit tests for the permission checks of {@link PermissionAjax}: removing an asset's permissions
 * requires {@link PermissionAPI#PERMISSION_EDIT_PERMISSIONS} on it, and an asset is only resolved
 * for a user who can read it. Every collaborator is mocked; the integration counterpart checks the
 * stored permission rows.
 */
class PermissionAjaxTest {

    private static final String FOLDER_ID = "folder-inode";
    private static final String CONTAINER_ID = "container-identifier";
    private static final String CONTAINER_INODE = "container-inode";
    private static final String INODE_ID = "plain-inode";
    private static final Long LANGUAGE_ID = 1L;

    private final User user = mock(User.class);
    private final PermissionAPI permissionAPI = mock(PermissionAPI.class);
    private final HostAPI hostAPI = mock(HostAPI.class);
    private final ContentletAPI contentletAPI = mock(ContentletAPI.class);
    private final FolderAPI folderAPI = mock(FolderAPI.class);
    private final ContentTypeAPI contentTypeAPI = mock(ContentTypeAPI.class);
    private final CategoryAPI categoryAPI = mock(CategoryAPI.class);
    private final UserWebAPI userWebAPI = mock(UserWebAPI.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<WebAPILocator> webAPILocator;
    private MockedStatic<WebContextFactory> webContextFactory;
    private MockedStatic<HibernateUtil> hibernateUtil;
    private MockedStatic<InodeFactory> inodeFactory;
    private MockedStatic<SecurityLogger> securityLogger;
    private MockedConstruction<DotConnect> dotConnect;

    /** Rows the lookup's {@code DotConnect} returns, one list per {@code loadResults()} call. */
    private final List<ArrayList<Map<String, Object>>> dbResults = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        when(user.getUserId()).thenReturn("limited-user");

        apiLocator = mockStatic(APILocator.class);
        apiLocator.when(APILocator::getPermissionAPI).thenReturn(permissionAPI);
        apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        apiLocator.when(APILocator::getContentletAPI).thenReturn(contentletAPI);
        apiLocator.when(APILocator::getFolderAPI).thenReturn(folderAPI);
        apiLocator.when(() -> APILocator.getContentTypeAPI(any(User.class))).thenReturn(contentTypeAPI);
        apiLocator.when(APILocator::getCategoryAPI).thenReturn(categoryAPI);

        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(request);
        webContextFactory = mockStatic(WebContextFactory.class);
        webContextFactory.when(WebContextFactory::get).thenReturn(webContext);

        when(userWebAPI.getLoggedInUser(request)).thenReturn(user);
        when(userWebAPI.isLoggedToBackend(request)).thenReturn(true);
        webAPILocator = mockStatic(WebAPILocator.class);
        webAPILocator.when(WebAPILocator::getUserWebAPI).thenReturn(userWebAPI);

        hibernateUtil = mockStatic(HibernateUtil.class);
        securityLogger = mockStatic(SecurityLogger.class);
        inodeFactory = mockStatic(InodeFactory.class);
        // Like the real lookup: an id that is not an inode resolves to an empty Inode
        inodeFactory.when(() -> InodeFactory.getInode(anyString(), eq(Inode.class))).thenReturn(new Inode());
        dotConnect = mockConstruction(DotConnect.class, (mock, context) ->
                when(mock.loadResults()).thenAnswer(invocation ->
                        dbResults.isEmpty() ? new ArrayList<>() : dbResults.remove(0)));
    }

    @AfterEach
    void tearDown() {
        dotConnect.close();
        inodeFactory.close();
        securityLogger.close();
        hibernateUtil.close();
        webAPILocator.close();
        webContextFactory.close();
        apiLocator.close();
    }

    /** The folder finder resolves {@link #FOLDER_ID} to a folder. */
    private Folder givenFolder() throws Exception {
        final Folder folder = mock(Folder.class);
        when(folder.getPermissionId()).thenReturn(FOLDER_ID);
        when(folderAPI.find(eq(FOLDER_ID), any(User.class), anyBoolean())).thenReturn(folder);
        return folder;
    }

    /** {@link #CONTAINER_ID} is only found through the identifier table and the inode lookup. */
    private Container givenContainer() {
        final Container container = mock(Container.class);
        when(container.getPermissionId()).thenReturn(CONTAINER_ID);
        dbResults.add(new ArrayList<>(List.of(Map.of("asset_type", "containers"))));
        dbResults.add(new ArrayList<>(List.of(Map.of("type", "containers", "inode", CONTAINER_INODE))));
        inodeFactory.when(() -> InodeFactory.getInode(eq(CONTAINER_INODE), any())).thenReturn(container);
        return container;
    }

    /** {@link #INODE_ID} is only found through the final inode lookup, which loads it as a link. */
    private Link givenLinkFromInodeLookup() {
        final Link link = mock(Link.class);
        when(link.getPermissionId()).thenReturn(INODE_ID);
        inodeFactory.when(() -> InodeFactory.getInode(INODE_ID, Inode.class)).thenReturn(link);
        return link;
    }

    /**
     * {@link #INODE_ID} is only found through the final inode lookup, which loads it as a plain
     * {@link Inode}, the way a contentlet's inode resolves there.
     */
    private Inode givenPlainInode() {
        final Inode inode = new Inode();
        inode.setInode(INODE_ID);
        inodeFactory.when(() -> InodeFactory.getInode(INODE_ID, Inode.class)).thenReturn(inode);
        return inode;
    }

    private void givenPermission(final Permissionable asset, final int permission,
            final boolean granted) throws Exception {
        when(permissionAPI.doesUserHavePermission(eq(asset), eq(permission), eq(user)))
                .thenReturn(granted);
        when(permissionAPI.doesUserHavePermission(eq(asset), eq(permission), eq(user), anyBoolean()))
                .thenReturn(granted);
    }

    /**
     * Method to test: {@link PermissionAjax#resetAssetPermissions(String, Long)}
     * Given: a user who can read a folder but lacks EDIT_PERMISSIONS on it
     * Expected: DotSecurityException naming only the user and the folder, written to the security
     * log; the folder's permissions are not removed, and the transaction is rolled back, not committed
     */
    @Test
    void resetAssetPermissions_withoutEditPermissions_isRejected() throws Exception {
        final Folder folder = givenFolder();
        givenPermission(folder, PermissionAPI.PERMISSION_READ, true);
        givenPermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, false);

        final DotSecurityException exception = assertThrows(DotSecurityException.class,
                () -> new PermissionAjax().resetAssetPermissions(FOLDER_ID, LANGUAGE_ID));
        assertTrue(exception.getMessage().contains("limited-user"));
        assertTrue(exception.getMessage().contains(FOLDER_ID));
        securityLogger.verify(() -> SecurityLogger.logInfo(PermissionAjax.class, exception.getMessage()));
        verify(permissionAPI, never()).removePermissions(any(Permissionable.class));
        hibernateUtil.verify(HibernateUtil::rollbackTransaction);
        hibernateUtil.verify(HibernateUtil::closeAndCommitTransaction, never());
    }

    /**
     * Method to test: {@link PermissionAjax#resetAssetPermissions(String, Long)}
     * Given: a user logged in on the front end, whose EDIT_PERMISSIONS on a folder comes only
     * from front-end roles
     * Expected: DotSecurityException, and the folder's permissions are not removed
     */
    @Test
    void resetAssetPermissions_editPermissionsOnlyFromFrontendRoles_isRejected() throws Exception {
        when(userWebAPI.isLoggedToBackend(request)).thenReturn(false);
        final Folder folder = givenFolder();
        givenPermission(folder, PermissionAPI.PERMISSION_READ, true);
        when(permissionAPI.doesUserHavePermission(eq((Permissionable) folder),
                eq(PermissionAPI.PERMISSION_EDIT_PERMISSIONS), eq(user))).thenReturn(true);
        when(permissionAPI.doesUserHavePermission(eq((Permissionable) folder),
                eq(PermissionAPI.PERMISSION_EDIT_PERMISSIONS), eq(user), eq(true))).thenReturn(true);
        when(permissionAPI.doesUserHavePermission(eq((Permissionable) folder),
                eq(PermissionAPI.PERMISSION_EDIT_PERMISSIONS), eq(user), eq(false))).thenReturn(false);

        assertThrows(DotSecurityException.class,
                () -> new PermissionAjax().resetAssetPermissions(FOLDER_ID, LANGUAGE_ID));
        verify(permissionAPI, never()).removePermissions(any(Permissionable.class));
    }

    /**
     * Method to test: {@link PermissionAjax#resetAssetPermissions(String, Long)}
     * Given: a user with EDIT_PERMISSIONS on a folder
     * Expected: the folder's permissions are removed, so it inherits again, and the transaction is committed
     */
    @Test
    void resetAssetPermissions_withEditPermissions_removesPermissions() throws Exception {
        final Folder folder = givenFolder();
        givenPermission(folder, PermissionAPI.PERMISSION_READ, true);
        givenPermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);

        new PermissionAjax().resetAssetPermissions(FOLDER_ID, LANGUAGE_ID);

        verify(permissionAPI).removePermissions(folder);
        hibernateUtil.verify(HibernateUtil::closeAndCommitTransaction);
        hibernateUtil.verify(HibernateUtil::rollbackTransaction, never());
    }

    /**
     * Method to test: {@link PermissionAjax#saveAssetPermissions(String, Long, List, boolean)}
     * Given: a user who can read a folder but lacks EDIT_PERMISSIONS on it, and an empty list
     * Expected: DotSecurityException, and the folder's permissions are not removed
     */
    @Test
    void saveAssetPermissions_emptyListWithoutEditPermissions_isRejected() throws Exception {
        final Folder folder = givenFolder();
        givenPermission(folder, PermissionAPI.PERMISSION_READ, true);
        givenPermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, false);

        assertThrows(DotSecurityException.class, () -> new PermissionAjax()
                .saveAssetPermissions(FOLDER_ID, LANGUAGE_ID, Collections.emptyList(), false));
        verify(permissionAPI, never()).removePermissions(any(Permissionable.class));
    }

    /**
     * Method to test: {@link PermissionAjax#saveAssetPermissions(String, Long, List, boolean)}
     * Given: a user with EDIT_PERMISSIONS on a folder, and an empty list
     * Expected: the folder's permissions are removed
     */
    @Test
    void saveAssetPermissions_emptyListWithEditPermissions_removesPermissions() throws Exception {
        final Folder folder = givenFolder();
        givenPermission(folder, PermissionAPI.PERMISSION_READ, true);
        givenPermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);

        new PermissionAjax().saveAssetPermissions(FOLDER_ID, LANGUAGE_ID, Collections.emptyList(), false);

        verify(permissionAPI).removePermissions(folder);
    }

    /**
     * Method to test: {@link PermissionAjax#saveAssetPermissions(String, Long, List, boolean)}
     * Given: a non-empty list of permissions for a folder
     * Expected: the list goes through {@link PermissionAPI#assignPermissions}, which checks
     * EDIT_PERMISSIONS, and nothing is removed directly
     */
    @Test
    void saveAssetPermissions_nonEmptyList_assignsThroughPermissionAPI() throws Exception {
        final Folder folder = givenFolder();
        givenPermission(folder, PermissionAPI.PERMISSION_READ, true);
        givenPermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, true);

        new PermissionAjax().saveAssetPermissions(FOLDER_ID, LANGUAGE_ID,
                List.of(Map.of("roleId", "some-role", "individualPermission", "1")), false);

        verify(permissionAPI).assignPermissions(anyList(), eq(folder), eq(user), anyBoolean());
        verify(permissionAPI, never()).removePermissions(any(Permissionable.class));
    }

    /**
     * Method to test: {@link PermissionAjax#saveAssetPermissions(String, Long, List, boolean)}
     * Given: a user who can read a folder but lacks EDIT_PERMISSIONS on it, and a non-empty list
     * Expected: DotSecurityException before any permission is assigned
     */
    @Test
    void saveAssetPermissions_nonEmptyListWithoutEditPermissions_isRejected() throws Exception {
        final Folder folder = givenFolder();
        givenPermission(folder, PermissionAPI.PERMISSION_READ, true);
        givenPermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, false);

        assertThrows(DotSecurityException.class, () -> new PermissionAjax().saveAssetPermissions(
                FOLDER_ID, LANGUAGE_ID,
                List.of(Map.of("roleId", "some-role", "individualPermission", "1")), false));
        verify(permissionAPI, never()).assignPermissions(anyList(), any(Permissionable.class),
                any(User.class), anyBoolean());
    }

    /**
     * Method to test: {@link PermissionAjax#getAssetPermissions(String, Long)}
     * Given: a user without READ on a container, which is resolved through the inode lookup
     * Expected: DotSecurityException, and the container's permissions are not read
     */
    @Test
    void getAssetPermissions_containerWithoutRead_isRejected() throws Exception {
        final Container container = givenContainer();
        givenPermission(container, PermissionAPI.PERMISSION_READ, false);

        assertThrows(DotSecurityException.class,
                () -> new PermissionAjax().getAssetPermissions(CONTAINER_ID, LANGUAGE_ID));
        verify(permissionAPI, never()).getPermissions(any(Permissionable.class), anyBoolean());
    }

    /**
     * Method to test: {@link PermissionAjax#getAssetPermissions(String, Long)}
     * Given: a user with READ but without EDIT_PERMISSIONS on a container
     * Expected: the container's permissions are returned
     */
    @Test
    void getAssetPermissions_containerWithRead_returnsPermissions() throws Exception {
        final Container container = givenContainer();
        givenPermission(container, PermissionAPI.PERMISSION_READ, true);
        givenPermission(container, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, false);

        new PermissionAjax().getAssetPermissions(CONTAINER_ID, LANGUAGE_ID);

        verify(permissionAPI).getPermissions(container, true);
    }

    /**
     * Method to test: {@link PermissionAjax#getAssetPermissions(String, Long)}
     * Given: a user without READ on a link only found through the final inode lookup
     * Expected: DotSecurityException, and its permissions are not read
     */
    @Test
    void getAssetPermissions_linkFromInodeLookupWithoutRead_isRejected() throws Exception {
        final Link link = givenLinkFromInodeLookup();
        givenPermission(link, PermissionAPI.PERMISSION_READ, false);

        assertThrows(DotSecurityException.class,
                () -> new PermissionAjax().getAssetPermissions(INODE_ID, LANGUAGE_ID));
        verify(permissionAPI, never()).getPermissions(any(Permissionable.class), anyBoolean());
    }

    /**
     * Method to test: {@link PermissionAjax#resetAssetPermissions(String, Long)}
     * Given: an id that resolves to no asset
     * Expected: the call is rejected and no permissions are removed
     */
    @Test
    void resetAssetPermissions_unknownId_isRejected() throws Exception {
        inodeFactory.when(() -> InodeFactory.getInode(anyString(), eq(Inode.class)))
                .thenReturn(new Inode());
        when(permissionAPI.doesUserHavePermission(any(Permissionable.class), anyInt(), any(User.class)))
                .thenReturn(true);
        when(permissionAPI.doesUserHavePermission(any(Permissionable.class), anyInt(), any(User.class), anyBoolean()))
                .thenReturn(true);

        assertThrows(DotDataException.class,
                () -> new PermissionAjax().resetAssetPermissions("unknown-id", LANGUAGE_ID));
        verify(permissionAPI, never()).removePermissions(any(Permissionable.class));
    }

    /**
     * Method to test: {@link PermissionAjax#getAssetPermissions(String, Long)}
     * Given: an id that resolves to no asset, e.g. a contentlet in a language it has no version in
     * Expected: an empty list, so the permissions tab still renders
     */
    @Test
    void getAssetPermissions_unknownId_returnsNoPermissions() throws Exception {
        inodeFactory.when(() -> InodeFactory.getInode(anyString(), eq(Inode.class)))
                .thenReturn(new Inode());

        final List<Map<String, Object>> permissions =
                new PermissionAjax().getAssetPermissions("unknown-id", LANGUAGE_ID);

        assertTrue(permissions.isEmpty());
    }

    /**
     * Method to test: {@link PermissionAjax#saveAssetPermissions(String, Long, List, boolean)}
     * Given: a user without EDIT_PERMISSIONS on a folder, and a list whose entries carry no
     * permission keys, so no permission would be built from it
     * Expected: DotSecurityException, and the folder's permissions are not removed
     */
    @Test
    void saveAssetPermissions_listWithoutPermissionKeysWithoutEditPermissions_isRejected() throws Exception {
        final Folder folder = givenFolder();
        givenPermission(folder, PermissionAPI.PERMISSION_READ, true);
        givenPermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, false);

        assertThrows(DotSecurityException.class, () -> new PermissionAjax().saveAssetPermissions(
                FOLDER_ID, LANGUAGE_ID, List.of(Map.of("roleId", "some-role")), false));
        verify(permissionAPI, never()).removePermissions(any(Permissionable.class));
    }

    /**
     * Method to test: {@link PermissionAjax#getAssetPermissions(String, Long)}
     * Given: an id of a site the user can't read
     * Expected: the site lookup's DotSecurityException reaches the caller, and nothing is read
     */
    @Test
    void getAssetPermissions_siteWithoutRead_isRejected() throws Exception {
        when(hostAPI.find(eq("site-id"), any(User.class), anyBoolean()))
                .thenThrow(new DotSecurityException("no read"));

        assertThrows(DotSecurityException.class,
                () -> new PermissionAjax().getAssetPermissions("site-id", LANGUAGE_ID));
        verify(permissionAPI, never()).getPermissions(any(Permissionable.class), anyBoolean());
    }

    /**
     * Method to test: {@link PermissionAjax#getAssetPermissions(String, Long)}
     * Given: an id of a contentlet the user can't read
     * Expected: the contentlet lookup's DotSecurityException reaches the caller, and nothing is read
     */
    @Test
    void getAssetPermissions_contentletWithoutRead_isRejected() throws Exception {
        when(contentletAPI.findContentletByIdentifier(eq("content-id"), anyBoolean(), anyLong(),
                any(User.class), anyBoolean())).thenThrow(new DotSecurityException("no read"));

        assertThrows(DotSecurityException.class,
                () -> new PermissionAjax().getAssetPermissions("content-id", LANGUAGE_ID));
        verify(permissionAPI, never()).getPermissions(any(Permissionable.class), anyBoolean());
    }

    /**
     * Method to test: {@link PermissionAjax#getAssetPermissions(String, Long)}
     * Given: an id of a folder the user can't read
     * Expected: the folder lookup's DotSecurityException reaches the caller, and nothing is read
     */
    @Test
    void getAssetPermissions_folderWithoutRead_isRejected() throws Exception {
        when(folderAPI.find(eq(FOLDER_ID), any(User.class), anyBoolean()))
                .thenThrow(new DotSecurityException("no read"));

        assertThrows(DotSecurityException.class,
                () -> new PermissionAjax().getAssetPermissions(FOLDER_ID, LANGUAGE_ID));
        verify(permissionAPI, never()).getPermissions(any(Permissionable.class), anyBoolean());
    }

    /**
     * Method to test: {@link PermissionAjax#permissionIndividually(String, Long)}
     * Given: a user who can read a folder but lacks EDIT_PERMISSIONS on it
     * Expected: DotSecurityException before the folder's permissions are copied, the transaction
     * is rolled back, not committed, and the session is closed
     */
    @Test
    void permissionIndividually_withoutEditPermissions_isRejected() throws Exception {
        final Folder folder = givenFolder();
        givenPermission(folder, PermissionAPI.PERMISSION_READ, true);
        givenPermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, false);
        when(permissionAPI.findParentPermissionable(folder)).thenReturn(mock(Folder.class));

        assertThrows(DotSecurityException.class,
                () -> new PermissionAjax().permissionIndividually(FOLDER_ID, LANGUAGE_ID));
        verify(permissionAPI, never()).permissionIndividually(any(Permissionable.class),
                any(Permissionable.class), any(User.class));
        hibernateUtil.verify(HibernateUtil::rollbackTransaction);
        hibernateUtil.verify(HibernateUtil::closeAndCommitTransaction, never());
        hibernateUtil.verify(HibernateUtil::closeSession);
    }

    /**
     * Method to test: {@link PermissionAjax#getAssetPermissions(String, Long)}
     * Given: an id only found as a plain Inode (no permissions of its own), and a user without READ
     * on it
     * Expected: no exception; its (empty) permissions are read as before
     */
    @Test
    void getAssetPermissions_plainInodeWithoutRead_returnsItsPermissions() throws Exception {
        final Inode inode = givenPlainInode();
        givenPermission(inode, PermissionAPI.PERMISSION_READ, false);

        assertDoesNotThrow(() -> new PermissionAjax().getAssetPermissions(INODE_ID, LANGUAGE_ID));

        verify(permissionAPI).getPermissions(inode, true);
    }

    /**
     * Method to test: {@link PermissionAjax#resetAssetPermissions(String, Long)}
     * Given: an id only found as a plain Inode, and a user without EDIT_PERMISSIONS on it
     * Expected: DotSecurityException, and nothing is removed
     */
    @Test
    void resetAssetPermissions_plainInodeWithoutEditPermissions_isRejected() throws Exception {
        final Inode inode = givenPlainInode();
        givenPermission(inode, PermissionAPI.PERMISSION_READ, false);
        givenPermission(inode, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, false);

        assertThrows(DotSecurityException.class,
                () -> new PermissionAjax().resetAssetPermissions(INODE_ID, LANGUAGE_ID));
        verify(permissionAPI, never()).removePermissions(any(Permissionable.class));
    }

    /**
     * Method to test: {@link PermissionAjax#saveAssetPermissions(String, Long, List, boolean)}
     * Given: a user who can read a folder but lacks EDIT_PERMISSIONS on it
     * Expected: the refusal is not also written as a WARN to the main log (the security log has it)
     */
    @Test
    void saveAssetPermissions_refused_doesNotWarn() throws Exception {
        final Folder folder = givenFolder();
        givenPermission(folder, PermissionAPI.PERMISSION_READ, true);
        givenPermission(folder, PermissionAPI.PERMISSION_EDIT_PERMISSIONS, false);

        try (MockedStatic<Logger> logger = mockStatic(Logger.class)) {
            assertThrows(DotSecurityException.class, () -> new PermissionAjax()
                    .saveAssetPermissions(FOLDER_ID, LANGUAGE_ID, Collections.emptyList(), false));
            logger.verify(() -> Logger.warn(any(Object.class), anyString(), any(Throwable.class)), never());
        }
        hibernateUtil.verify(HibernateUtil::rollbackTransaction);
    }
}
