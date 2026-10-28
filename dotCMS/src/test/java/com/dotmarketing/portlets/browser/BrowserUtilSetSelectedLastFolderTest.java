package com.dotmarketing.portlets.browser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Guards the file-browser "last selected folder" preference endpoint against session-less
 * callers. The endpoint only records a per-session UI preference, so it must create the HTTP
 * session when the caller did not bring one: API-token callers no longer arrive with the login
 * session that the former Basic-authentication used to create, and a {@code getSession(false)}
 * dereference turned their request into a 500.
 */
public class BrowserUtilSetSelectedLastFolderTest {

    /** Request attribute the preference is recorded under; keep in step with BrowserUtil. */
    private static final String LAST_SELECTED_FOLDER_ID = "LAST_SELECTED_FOLDER_ID";

    /**
     * Recording the preference must create the session instead of dereferencing the absent one.
     */
    @Test
    public void recordsPreferenceOnASessionlessRequest() throws Exception {

        final HttpSession session = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        // The session-less caller under test.
        when(request.getSession(false)).thenReturn(null);
        when(request.getSession()).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);

        final Folder folder = mock(Folder.class);
        final User systemUser = mock(User.class);
        final FolderAPI folderAPI = mock(FolderAPI.class);
        when(folderAPI.find(anyString(), any(User.class), anyBoolean())).thenReturn(folder);

        try (MockedStatic<APILocator> api = mockStatic(APILocator.class)) {
            api.when(APILocator::getFolderAPI).thenReturn(folderAPI);
            api.when(APILocator::systemUser).thenReturn(systemUser);

            BrowserUtil.setSelectedLastFolder(request, "folder-id");
        }

        verify(session).setAttribute(eq(LAST_SELECTED_FOLDER_ID), eq("folder-id"));
    }
}