package com.dotcms.util.pagination;

import com.dotcms.rest.api.v1.folder.FolderSearchResultView;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.business.FolderSearchParams;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FolderSearchPaginator}.
 */
public class FolderSearchPaginatorTest {

    private FolderAPI folderAPI;
    private FolderSearchPaginator paginator;
    private PaginatedArrayList<FolderSearchResultView> results;

    @Before
    public void setUp() {
        folderAPI = mock(FolderAPI.class);
        paginator = new FolderSearchPaginator(folderAPI);

        results = new PaginatedArrayList<>();
        results.setTotalResults(3);
        results.add(mock(FolderSearchResultView.class));
        results.add(mock(FolderSearchResultView.class));
        results.add(mock(FolderSearchResultView.class));
    }

    /**
     * Method to test: {@link FolderSearchPaginator#getItems} <br>
     * Given Scenario: Only siteId provided, no name, no path, no recursive <br>
     * Expected Result: FolderAPI called with null name, default path "/" and recursive=false
     */
    @Test
    public void test_getItems_noName_defaultPath_returnsAllSiteFolders() throws Exception {
        final String siteId = "site-1";
        final User user = new User();
        final FolderSearchParams expected = FolderSearchParams.builder()
                .siteId(siteId)
                .user(user)
                .limit(40)
                .offset(0)
                .build(); // path defaults to "/", recursive defaults to false

        when(folderAPI.searchFolders(expected)).thenReturn(results);

        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("siteId", siteId);

        final PaginatedArrayList<FolderSearchResultView> items =
                paginator.getItems(user, null, 40, 0, null, null, extraParams);

        assertEquals(3, items.getTotalResults());
        assertSame(results, items);
        verify(folderAPI).searchFolders(expected);
    }

    /**
     * Method to test: {@link FolderSearchPaginator#getItems} <br>
     * Given Scenario: name filter provided, no path or recursive in extra params <br>
     * Expected Result: FolderAPI called with name, default path "/" and recursive=false
     */
    @Test
    public void test_getItems_nameOnly_searchesEntireSite() throws Exception {
        final String siteId = "site-1";
        final String name = "images";
        final User user = new User();
        final FolderSearchParams expected = FolderSearchParams.builder()
                .name(name)
                .siteId(siteId)
                .user(user)
                .limit(40)
                .offset(0)
                .build(); // path defaults to "/", recursive defaults to false

        when(folderAPI.searchFolders(expected)).thenReturn(results);

        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("siteId", siteId);

        final PaginatedArrayList<FolderSearchResultView> items =
                paginator.getItems(user, name, 40, 0, null, null, extraParams);

        assertSame(results, items);
        verify(folderAPI).searchFolders(expected);
    }

    /**
     * Method to test: {@link FolderSearchPaginator#getItems} <br>
     * Given Scenario: path provided with recursive=true, no name filter <br>
     * Expected Result: FolderAPI called with null name, the given path, true
     */
    @Test
    public void test_getItems_pathOnly_recursive_returnsDescendants() throws Exception {
        final String siteId = "site-1";
        final String path = "/assets/";
        final User user = new User();
        final FolderSearchParams expected = FolderSearchParams.builder()
                .siteId(siteId)
                .path(path)
                .recursive(true)
                .user(user)
                .limit(10)
                .offset(0)
                .build();

        when(folderAPI.searchFolders(expected)).thenReturn(results);

        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("siteId", siteId);
        extraParams.put("path", path);
        extraParams.put("recursive", true);

        final PaginatedArrayList<FolderSearchResultView> items =
                paginator.getItems(user, null, 10, 0, null, null, extraParams);

        assertSame(results, items);
        verify(folderAPI).searchFolders(expected);
    }

    /**
     * Method to test: {@link FolderSearchPaginator#getItems} <br>
     * Given Scenario: path + recursive=false <br>
     * Expected Result: FolderAPI called with recursive=false (direct children only)
     */
    @Test
    public void test_getItems_pathOnly_notRecursive_returnsDirectChildren() throws Exception {
        final String siteId = "site-1";
        final String path = "/assets/";
        final User user = new User();
        final FolderSearchParams expected = FolderSearchParams.builder()
                .siteId(siteId)
                .path(path)
                .recursive(false)
                .user(user)
                .limit(10)
                .offset(0)
                .build();

        when(folderAPI.searchFolders(expected)).thenReturn(results);

        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("siteId", siteId);
        extraParams.put("path", path);
        extraParams.put("recursive", false);

        final PaginatedArrayList<FolderSearchResultView> items =
                paginator.getItems(user, null, 10, 0, null, null, extraParams);

        assertSame(results, items);
        verify(folderAPI).searchFolders(expected);
    }

    /**
     * Method to test: {@link FolderSearchPaginator#getItems} <br>
     * Given Scenario: name + path combined <br>
     * Expected Result: FolderAPI called with both name and path (combined mode)
     */
    @Test
    public void test_getItems_nameAndPath_combined() throws Exception {
        final String siteId = "site-1";
        final String name = "img";
        final String path = "/assets/";
        final User user = new User();
        final FolderSearchParams expected = FolderSearchParams.builder()
                .name(name)
                .siteId(siteId)
                .path(path)
                .recursive(true)
                .user(user)
                .limit(20)
                .offset(0)
                .orderBy("folder.mod_date")
                .orderDirection("DESC")
                .build();

        when(folderAPI.searchFolders(expected)).thenReturn(results);

        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("siteId", siteId);
        extraParams.put("path", path);
        extraParams.put("recursive", true);

        final PaginatedArrayList<FolderSearchResultView> items =
                paginator.getItems(user, name, 20, 0, "mod_date", OrderDirection.DESC, extraParams);

        assertSame(results, items);
        verify(folderAPI).searchFolders(expected);
    }

    /**
     * Method to test: {@link FolderSearchPaginator#getItems} <br>
     * Given Scenario: FolderAPI throws DotDataException <br>
     * Expected Result: PaginationException wraps the original exception
     */
    @Test(expected = PaginationException.class)
    public void test_getItems_whenAPIThrows_throwsPaginationException() throws Exception {
        final User user = new User();
        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("siteId", "site-1");
        extraParams.put("path", "/");
        extraParams.put("recursive", true);

        when(folderAPI.searchFolders(any(FolderSearchParams.class)))
                .thenThrow(new DotDataException("DB error"));

        paginator.getItems(user, null, 10, 0, null, null, extraParams);
    }

    /**
     * Method to test: {@link FolderSearchPaginator#getItems} <br>
     * Given Scenario: FolderAPI throws DotSecurityException <br>
     * Expected Result: PaginationException wraps the security exception
     */
    @Test(expected = PaginationException.class)
    public void test_getItems_whenSecurityExceptionThrown_throwsPaginationException() throws Exception {
        final User user = new User();
        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("siteId", "site-1");
        extraParams.put("path", "/");
        extraParams.put("recursive", true);

        when(folderAPI.searchFolders(any(FolderSearchParams.class)))
                .thenThrow(new DotSecurityException("Access denied"));

        paginator.getItems(user, null, 10, 0, null, null, extraParams);
    }
}