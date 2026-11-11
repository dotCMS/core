package com.dotcms.rendering.velocity.viewtools;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.business.web.UserWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.calendar.business.EventAPI;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for the user that {@link DotCMSMacroWebAPI#getMediaGalleryFolderFiles(String, String)} and
 * {@link DotCMSMacroWebAPI#getEvents(String, int, int, int)} read folders, files, categories and events as.
 */
class DotCMSMacroWebAPIViewerTest {

    private static final String SITE_ID = "site-id";
    private static final String FOLDER_PATH = "/gallery";
    private static final String NORMALIZED_PATH = "/gallery/";

    private MockedStatic<APILocator> apiLocator;
    private MockedStatic<WebAPILocator> webApiLocator;

    private HostAPI hostAPI;
    private FolderAPI folderAPI;
    private FileAssetAPI fileAssetAPI;
    private PermissionAPI permissionAPI;
    private CategoryAPI categoryAPI;
    private EventAPI eventAPI;
    private UserWebAPI userWebAPI;

    private User anonymous;
    private User system;
    private List<FileAsset> folderFiles;
    private Host site;
    private Folder folder;
    private HttpServletRequest previousThreadRequest;

    @BeforeEach
    void setUp() throws Exception {
        previousThreadRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        HttpServletRequestThreadLocal.INSTANCE.setRequest(null);

        apiLocator = mockStatic(APILocator.class);
        webApiLocator = mockStatic(WebAPILocator.class);

        hostAPI = mock(HostAPI.class);
        folderAPI = mock(FolderAPI.class);
        fileAssetAPI = mock(FileAssetAPI.class);
        permissionAPI = mock(PermissionAPI.class);
        categoryAPI = mock(CategoryAPI.class);
        eventAPI = mock(EventAPI.class);
        userWebAPI = mock(UserWebAPI.class);
        final UserAPI userAPI = mock(UserAPI.class);

        anonymous = user("anonymous");
        system = user("system");
        when(userAPI.getAnonymousUserNoThrow()).thenReturn(anonymous);
        when(userAPI.getAnonymousUser()).thenReturn(anonymous);
        when(userAPI.getSystemUser()).thenReturn(system);

        apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
        apiLocator.when(APILocator::getFolderAPI).thenReturn(folderAPI);
        apiLocator.when(APILocator::getFileAssetAPI).thenReturn(fileAssetAPI);
        apiLocator.when(APILocator::getPermissionAPI).thenReturn(permissionAPI);
        apiLocator.when(APILocator::getCategoryAPI).thenReturn(categoryAPI);
        apiLocator.when(APILocator::getEventAPI).thenReturn(eventAPI);
        apiLocator.when(APILocator::getUserAPI).thenReturn(userAPI);
        webApiLocator.when(WebAPILocator::getUserWebAPI).thenReturn(userWebAPI);

        site = mock(Host.class);
        folder = mock(Folder.class);
        when(folder.getInode()).thenReturn("folder-inode");
        when(hostAPI.find(eq(SITE_ID), any(User.class), anyBoolean())).thenReturn(site);
        when(folderAPI.findFolderByPath(eq(NORMALIZED_PATH), any(Host.class), any(User.class), anyBoolean()))
                .thenReturn(folder);
        when(folderAPI.findFolderByPath(eq(NORMALIZED_PATH), anyString(), any(User.class), anyBoolean()))
                .thenReturn(folder);
        folderFiles = new ArrayList<>(List.of(mock(FileAsset.class)));
        when(fileAssetAPI.findFileAssetsByFolder(any(Folder.class), anyString(), anyBoolean(), any(User.class),
                anyBoolean())).thenReturn(folderFiles);
        when(permissionAPI.filterCollection(anyList(), anyInt(), anyBoolean(), any(User.class)))
                .thenReturn(new ArrayList<>());
    }

    @AfterEach
    void tearDown() {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(previousThreadRequest);
        webApiLocator.close();
        apiLocator.close();
    }

    @Test
    void galleryFiles_withoutLoggedInUser_readAsAnonymousRespectingFrontendRoles() throws Exception {
        // Given a request with nobody logged in
        final DotCMSMacroWebAPI macro = toolFor(requestOf(null));

        // When the gallery lists a folder
        assertDoesNotThrow(() -> macro.getMediaGalleryFolderFiles(FOLDER_PATH, SITE_ID));

        // Then the site, the folder and the files are read as the anonymous user, honoring front-end roles
        verify(hostAPI).find(SITE_ID, anonymous, true);
        verify(folderAPI).findFolderByPath(NORMALIZED_PATH, site, anonymous, true);
        verifyFilesListedAs(anonymous);
    }

    @Test
    void galleryFiles_withLoggedInUser_readAsThatUser() throws Exception {
        // Given a request with a logged-in user
        final User viewer = user("viewer");
        final DotCMSMacroWebAPI macro = toolFor(requestOf(viewer));

        // When the gallery lists a folder
        assertDoesNotThrow(() -> macro.getMediaGalleryFolderFiles(FOLDER_PATH, SITE_ID));

        // Then every read uses that user
        verify(hostAPI).find(SITE_ID, viewer, true);
        verify(folderAPI).findFolderByPath(NORMALIZED_PATH, site, viewer, true);
        verifyFilesListedAs(viewer);
    }

    @Test
    void galleryFiles_whenFolderIsNotReadable_returnsEmptyList() throws Exception {
        // Given a folder the current user cannot read
        when(folderAPI.findFolderByPath(eq(NORMALIZED_PATH), any(Host.class), any(User.class), anyBoolean()))
                .thenThrow(new DotSecurityException("no read"));
        when(folderAPI.findFolderByPath(eq(NORMALIZED_PATH), anyString(), any(User.class), anyBoolean()))
                .thenThrow(new DotSecurityException("no read"));
        final DotCMSMacroWebAPI macro = toolFor(requestOf(null));

        // When the gallery lists it
        final List<HashMap<String, IFileAsset>> files =
                assertDoesNotThrow(() -> macro.getMediaGalleryFolderFiles(FOLDER_PATH, SITE_ID));

        // Then it answers an empty list and never lists the folder's files
        assertTrue(files.isEmpty());
        verify(permissionAPI, never()).filterCollection(anyList(), anyInt(), anyBoolean(), any(User.class));
    }

    @Test
    void galleryFiles_whenSiteIsNotReadable_returnsEmptyList() throws Exception {
        // Given a site the current user cannot read
        when(hostAPI.find(eq(SITE_ID), any(User.class), anyBoolean())).thenThrow(new DotSecurityException("no read"));
        final DotCMSMacroWebAPI macro = toolFor(requestOf(null));

        // When the gallery lists a folder of that site
        final List<HashMap<String, IFileAsset>> files =
                assertDoesNotThrow(() -> macro.getMediaGalleryFolderFiles(FOLDER_PATH, SITE_ID));

        // Then it answers an empty list and never lists files
        assertTrue(files.isEmpty());
        verify(permissionAPI, never()).filterCollection(anyList(), anyInt(), anyBoolean(), any(User.class));
    }

    @Test
    void galleryFiles_threadRequestWinsOverInitRequest() throws Exception {
        // Given a tool created for one request while another request is bound to the current thread
        final User initUser = user("init-user");
        final User threadUser = user("thread-user");
        final DotCMSMacroWebAPI macro = toolFor(requestOf(initUser));
        HttpServletRequestThreadLocal.INSTANCE.setRequest(requestOf(threadUser));

        // When the gallery lists a folder
        assertDoesNotThrow(() -> macro.getMediaGalleryFolderFiles(FOLDER_PATH, SITE_ID));

        // Then the user of the current thread's request is used
        verify(folderAPI).findFolderByPath(NORMALIZED_PATH, site, threadUser, true);
        verifyFilesListedAs(threadUser);
    }

    @Test
    void galleryFiles_withoutThreadRequest_usesInitRequestUser() throws Exception {
        // Given no request bound to the thread and a tool created for a request with a logged-in user
        final User initUser = user("init-user");
        final DotCMSMacroWebAPI macro = toolFor(requestOf(initUser));

        // When the gallery lists a folder
        assertDoesNotThrow(() -> macro.getMediaGalleryFolderFiles(FOLDER_PATH, SITE_ID));

        // Then the user of the request the tool was created for is used
        verify(folderAPI).findFolderByPath(NORMALIZED_PATH, site, initUser, true);
        verifyFilesListedAs(initUser);
    }

    @Test
    void galleryFiles_withoutAnyRequest_readAsAnonymous() throws Exception {
        // Given a tool created outside of a request and no request bound to the thread
        final DotCMSMacroWebAPI macro = toolFor(null);

        // When the gallery lists a folder
        assertDoesNotThrow(() -> macro.getMediaGalleryFolderFiles(FOLDER_PATH, SITE_ID));

        // Then it reads as the anonymous user
        verify(folderAPI).findFolderByPath(NORMALIZED_PATH, site, anonymous, true);
        verifyFilesListedAs(anonymous);
    }

    @Test
    void events_readCategoriesAndEventsAsRequestUser() throws Exception {
        // Given a request with a logged-in user
        final User viewer = user("viewer");
        when(categoryAPI.find(anyString(), any(User.class), anyBoolean())).thenReturn(mock(Category.class));
        final DotCMSMacroWebAPI macro = toolFor(requestOf(viewer));

        // When the events of a category are listed
        assertDoesNotThrow(() -> macro.getEvents("category-inode", 5, 0, 0));

        // Then the category and the events are read as that user, honoring front-end roles
        verify(categoryAPI).find("category-inode", viewer, true);
        verify(eventAPI).find(any(), any(), isNull(), isNull(), anyList(), eq(true), eq(false), eq(0), eq(5),
                eq(viewer), eq(true));
    }

    @Test
    void events_whenACategoryIsNotReadable_returnsEmptyList() throws Exception {
        // Given two categories, the second one not readable by the current user
        when(categoryAPI.find(eq("readable-category"), any(User.class), anyBoolean()))
                .thenReturn(mock(Category.class));
        when(categoryAPI.find(eq("closed-category"), any(User.class), anyBoolean()))
                .thenThrow(new DotSecurityException("no read"));
        final DotCMSMacroWebAPI macro = toolFor(requestOf(null));

        // When the events of both categories are listed
        final List<?> events = assertDoesNotThrow(
                () -> macro.getEvents("readable-category,closed-category", 5, 0, 0));

        // Then it answers an empty list and never searches events with a partial category filter
        assertTrue(events.isEmpty());
        verify(eventAPI, never()).find(any(), any(), any(), any(), anyList(), anyBoolean(), anyBoolean(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), any(),
                anyBoolean());
    }

    @Test
    void events_whenACategoryDoesNotExist_returnsEmptyList() throws Exception {
        // Given two categories, the second one unknown
        when(categoryAPI.find(eq("known-category"), any(User.class), anyBoolean()))
                .thenReturn(mock(Category.class));
        when(categoryAPI.find(eq("unknown-category"), any(User.class), anyBoolean())).thenReturn(null);
        final DotCMSMacroWebAPI macro = toolFor(requestOf(null));

        // When the events of both categories are listed
        final List<?> events = assertDoesNotThrow(
                () -> macro.getEvents("known-category,unknown-category", 5, 0, 0));

        // Then it answers an empty list and never searches events with a partial category filter
        assertTrue(events.isEmpty());
        verify(eventAPI, never()).find(any(), any(), any(), any(), anyList(), anyBoolean(), anyBoolean(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), any(),
                anyBoolean());
    }

    /**
     * Checks that the folder's live files were filtered with the given user's READ permission, honoring front-end
     * roles.
     */
    private void verifyFilesListedAs(final User user) throws Exception {
        verify(permissionAPI).filterCollection(folderFiles, PermissionAPI.PERMISSION_READ, true, user);
    }

    private static User user(final String id) {
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(id);
        return user;
    }

    private HttpServletRequest requestOf(final User user) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(userWebAPI.getUser(request)).thenReturn(user);
        return request;
    }

    private static DotCMSMacroWebAPI toolFor(final HttpServletRequest request) {
        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getVelocityContext()).thenReturn(mock(Context.class));
        when(viewContext.getRequest()).thenReturn(request);
        final DotCMSMacroWebAPI macro = new DotCMSMacroWebAPI();
        macro.init(viewContext);
        return macro;
    }
}
