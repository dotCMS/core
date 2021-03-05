package com.dotmarketing.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import org.junit.Test;

import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * test for {@link ThemeAPIImpl}
 */
public class ThemeAPIImplTest {

    public static final String LUCENE_QUERY = "+conFolder:1 +title:theme.png +live:true +deleted:false";

    @Test
    public void when_FolderAndUserAreNotNull_And_ExceptionIsNotThrown() throws DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final FolderAPI folderAPI = mock(FolderAPI.class);
        final User user = mock(User.class);
        final Folder folder = mock(Folder.class);
        final String folderInode = "1";
        final String themeThumbnailExpected = "themeThumbnail";
        final Contentlet contentlet = mock(Contentlet.class);
        final List<Contentlet> contentlets = list(contentlet);

        when(contentlet.getIdentifier()).thenReturn(themeThumbnailExpected);
        when(folder.getInode()).thenReturn(folderInode);
        when(contentletAPI.search(LUCENE_QUERY, -1,
                0, null, user, false)).thenReturn(contentlets);

        final ThemeAPIImpl themeAPI = new ThemeAPIImpl(contentletAPI,folderAPI);
        final String themeThumbnail = themeAPI.getThemeThumbnail(folder, user);

        assertEquals(themeThumbnailExpected, themeThumbnail);
    }

    @Test(expected = DotDataException.class)
    public void when_FolderAndUserAreNotNull_And_DotDataExceptionThrown() throws DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final FolderAPI folderAPI = mock(FolderAPI.class);
        final User user = mock(User.class);
        final Folder folder = mock(Folder.class);
        final String folderInode = "1";
        final DotDataException dotDataException = mock(DotDataException.class);

        when(folder.getInode()).thenReturn(folderInode);
        when(contentletAPI.search(LUCENE_QUERY, -1,
                0, null, user, false)).thenThrow(dotDataException);


        final ThemeAPIImpl themeAPI = new ThemeAPIImpl(contentletAPI,folderAPI);
        themeAPI.getThemeThumbnail(folder, user);
    }

    @Test(expected = DotSecurityException.class)
    public void when_FolderAndUserAreNotNull_And_DotSecurityExceptionThrown() throws DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final FolderAPI folderAPI = mock(FolderAPI.class);
        final User user = mock(User.class);
        final Folder folder = mock(Folder.class);
        final String folderInode = "1";
        final DotSecurityException dotSecurityException = mock(DotSecurityException.class);

        when(folder.getInode()).thenReturn(folderInode);
        when(contentletAPI.search(LUCENE_QUERY, -1,
                0, null, user, false)).thenThrow(dotSecurityException);


        final ThemeAPIImpl themeAPI = new ThemeAPIImpl(contentletAPI,folderAPI);
        themeAPI.getThemeThumbnail(folder, user);
    }

    @Test
    public void when_FolderIsNull_And_DotDataExceptionThrown() throws DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final FolderAPI folderAPI = mock(FolderAPI.class);
        final User user = mock(User.class);
        final Folder folder = null;
        final DotDataException dotDataException = mock(DotDataException.class);

        when(contentletAPI.search(LUCENE_QUERY, -1,
                0, null, user, false)).thenThrow(dotDataException);


        final ThemeAPIImpl themeAPI = new ThemeAPIImpl(contentletAPI,folderAPI);
        final String themeThumbnail = themeAPI.getThemeThumbnail(folder, user);

        assertNull(themeThumbnail);
    }

    @Test
    public void when_UserIsNull_And_DotDataExceptionThrown() throws DotDataException, DotSecurityException {
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final FolderAPI folderAPI = mock(FolderAPI.class);
        final User user = null;
        final Folder folder = mock(Folder.class);
        final DotDataException dotDataException = mock(DotDataException.class);

        when(contentletAPI.search(LUCENE_QUERY, -1,
                0, null, user, false)).thenThrow(dotDataException);


        final ThemeAPIImpl themeAPI = new ThemeAPIImpl(contentletAPI,folderAPI);
        final String themeThumbnail = themeAPI.getThemeThumbnail(folder, user);

        assertNull(themeThumbnail);
    }
}
