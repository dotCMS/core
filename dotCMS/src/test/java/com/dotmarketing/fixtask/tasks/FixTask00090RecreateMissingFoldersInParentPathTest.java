package com.dotmarketing.fixtask.tasks;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.fixtask.tasks.FixTask00090RecreateMissingFoldersInParentPath.LiteFolder;

import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class FixTask00090RecreateMissingFoldersInParentPathTest {

    private FixTask00090RecreateMissingFoldersInParentPath fixTask;
    private static final String aHostId = "host-id";

    @Before
    public void setUp() {
        fixTask = new FixTask00090RecreateMissingFoldersInParentPath();
    }

    @Test(expected = NullPointerException.class)
    public void getFoldersFromParentPath_ShouldThrowNPE_WhenNullParentPath() {
        fixTask.getFoldersFromParentPath(null, aHostId);
    }

    @Test
    public void getFoldersFromParentPath_ShouldReturnEmptyList_WhenBlankParentPath() {
        List result = fixTask.getFoldersFromParentPath("", aHostId);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getFoldersFromParentPath_ShouldReturnOneFolderList_WhenOneLevelParentPath() {
        List<LiteFolder> result = fixTask.getFoldersFromParentPath("/level1/", aHostId);
        assertEquals(result.size(), 1);

        LiteFolder folder = result.get(0);
        assertEquals(folder.parentPath, "/");
        assertEquals(folder.name, "level1");
    }

    @Test
    public void getFoldersFromParentPath_ShouldReturnOneFolderList_WhenTwoLevelParentPath() {
        List<LiteFolder> result = fixTask.getFoldersFromParentPath("/level1/level2/", aHostId);
        assertEquals(result.size(), 2);

        LiteFolder folder1 = result.get(0);
        assertEquals(folder1.parentPath, "/");
        assertEquals(folder1.name, "level1");

        LiteFolder folder2 = result.get(1);
        assertEquals(folder2.parentPath, "/level1/");
        assertEquals(folder2.name, "level2");
    }

    @Test(expected = NullPointerException.class)
    public void recreateMissingFoldersInParentPath_ShouldThrowNPE_WhenNullParentPath() {
        fixTask.getFoldersFromParentPath(null, aHostId);
    }

    @Test
    public void recreateMissingFoldersInParentPath_ShouldDoNothing_WhenSystemHost() throws DotSecurityException, SQLException, DotDataException {
        FixTask00090RecreateMissingFoldersInParentPath fixTaskSpy = spy(FixTask00090RecreateMissingFoldersInParentPath.class);
        doReturn(new ArrayList<>()).when(fixTaskSpy).getFoldersFromParentPath("/", "host-id");
        fixTaskSpy.recreateMissingFoldersInParentPath("/", "host-id");
        verify(fixTaskSpy, never()).recreateMissingFolders(anyList());
    }

    @Test
    public void recreateMissingFoldersInParentPath_ShouldDoNothing_WhenSystemFolder() throws DotSecurityException, SQLException, DotDataException {
        FixTask00090RecreateMissingFoldersInParentPath fixTaskSpy = spy(FixTask00090RecreateMissingFoldersInParentPath.class);
        doReturn(new ArrayList<>()).when(fixTaskSpy).getFoldersFromParentPath("/System folder", aHostId);
        fixTaskSpy.recreateMissingFoldersInParentPath("/", aHostId);
        verify(fixTaskSpy, never()).recreateMissingFolders(anyList());
    }

    @Test
    public void recreateMissingFoldersInParentPath_ShouldRecreate_WhenDifferentThanSystemHostOrFolder() throws DotSecurityException, SQLException, DotDataException {
        final String parentPath = "/level1/";
        FixTask00090RecreateMissingFoldersInParentPath fixTaskSpy = spy(FixTask00090RecreateMissingFoldersInParentPath.class);
        doReturn(new ArrayList<>()).when(fixTaskSpy).getFoldersFromParentPath(parentPath, aHostId);
        doNothing().when(fixTaskSpy).recreateMissingFolders(anyList());
        fixTaskSpy.recreateMissingFoldersInParentPath(parentPath, aHostId);
        verify(fixTaskSpy).recreateMissingFolders(anyList());
    }

    @Test
    public void recreateMissingFolders_ShouldCreateFolder_WhenFolderIsMissing() throws DotSecurityException, SQLException, DotDataException {
        FixTask00090RecreateMissingFoldersInParentPath fixTaskSpy = spy(FixTask00090RecreateMissingFoldersInParentPath.class);
        doReturn(true).when(fixTaskSpy).isFolderIdentifierMissing(any());
        doNothing().when(fixTaskSpy).createFolder(any());
        List<LiteFolder> liteFolders = new ArrayList<>();
        liteFolders.add(new LiteFolder());
        fixTaskSpy.recreateMissingFolders(liteFolders);
        // let's verify the create folder method was called for the missing folder
        verify(fixTaskSpy).createFolder(any());
    }

}
