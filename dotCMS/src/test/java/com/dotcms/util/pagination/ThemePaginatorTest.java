package com.dotcms.util.pagination;

import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.pagination.ThemePaginator.BASE_LUCENE_QUERY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.dotcms.util.CollectionsUtils.map;

/**
 * Test of {@link ThemePaginator}
 */
public class ThemePaginatorTest {

    private final ContentletAPI contentletAPI = mock(ContentletAPI.class);
    private final FolderAPI folderAPI         = mock(FolderAPI.class);

    private static final String QUERY_WITH_HOST = "+parentpath:/application/themes/* +title:template.vtl +conhost:1";

    private final User user = mock(User.class);

    /**
     * Test of {@link ThemePaginator#getItems(User, int, int, Map)}
     *
     * Given: a hostId
     * Should: return all the themes
     */
    @Test
    public void findThemesWithHostId() throws DotSecurityException, DotDataException {

        final PaginatedArrayList<ContentletSearch> contentletSearchList = new PaginatedArrayList<>();
        contentletSearchList.addAll(list(createContentSearchMock("1"), createContentSearchMock("2"),
                createContentSearchMock("3")));
        final List<Contentlet> contentlets = list(createContentletMock("1"), createContentletMock("2"),
                createContentletMock("3"));

        final List<String> elements = list("1", "2", "3");

        final ThemePaginator themePaginator = new ThemePaginator(contentletAPI, folderAPI);

        when(contentletAPI.searchIndex(QUERY_WITH_HOST, 0, -1, "parentPath asc", user, false))
                .thenReturn(contentletSearchList);

        when(contentletAPI.findContentlets(elements)).thenReturn(contentlets);

        //mocking folders
        elements.forEach(elem -> {
            try {
                final Folder folder = createFolderMock(elem);
                when(folderAPI.find(elem, user, false)).thenReturn(folder);
                when(contentletAPI.search(getFolderQuery(elem),-1, 0, null, user, false)).thenReturn(null);
            } catch (DotSecurityException | DotDataException e) {
                Logger.error(this, e.getMessage());
            }
        });

        final Map<String, Object> params = map(ThemePaginator.HOST_ID_PARAMETER_NAME, "1");
        final PaginatedArrayList<Folder> themes = themePaginator.getItems(user, 0, -1, params);

        assertEquals(contentlets.size(), themes.size());
        assertEquals(contentlets.get(0).getFolder(), themes.get(0).getInode());
        assertEquals(contentlets.get(1).getFolder(), themes.get(1).getInode());
        assertEquals(contentlets.get(2).getFolder(), themes.get(2).getInode());
    }

    /**
     * Test of {@link ThemePaginator#getItems(User, int, int, Map)}
     *
     * Given: a hostId null
     * Should: return all the themes
     */
    @Test
    public void findThemesWithoutHostId() throws DotSecurityException, DotDataException {

        final PaginatedArrayList<ContentletSearch> contentletSearchList = new PaginatedArrayList<>();
        contentletSearchList.addAll(list(createContentSearchMock("1"), createContentSearchMock("2"),
                createContentSearchMock("3")));
        final List<Contentlet> contentlets = list(createContentletMock("1"), createContentletMock("2"),
                createContentletMock("3"));

        final List<String> elements = list("1", "2", "3");

        final ThemePaginator themePaginator = new ThemePaginator(contentletAPI, folderAPI);

        when(contentletAPI.searchIndex(BASE_LUCENE_QUERY, 0, -1, "parentPath asc", user, false))
                .thenReturn(contentletSearchList);

        when(contentletAPI.findContentlets(elements)).thenReturn(contentlets);

        //mocking folders
        elements.forEach(elem -> {
            try {
                final Folder folder = createFolderMock(elem);
                when(folderAPI.find(elem, user, false)).thenReturn(folder);
                when(contentletAPI.search(getFolderQuery(elem),-1, 0, null, user, false)).thenReturn(null);
            } catch (DotSecurityException | DotDataException e) {
                Logger.error(this, e.getMessage());
            }
        });

        final PaginatedArrayList<Folder> themes = themePaginator.getItems(user, 0, -1, null);

        assertEquals(contentlets.size(), themes.size());
        assertEquals(contentlets.get(0).getFolder(), themes.get(0).getInode());
        assertEquals(contentlets.get(1).getFolder(), themes.get(1).getInode());
        assertEquals(contentlets.get(2).getFolder(), themes.get(2).getInode());
    }

    /**
     * Test of {@link ThemePaginator#getItems(User, int, int, Map)}
     *
     * Given: throw a DotSecurityException
     * Should: throw a DotSecurityException
     */
    @Test
    public void findThemesThrowDotSecurityException() throws DotDataException, DotSecurityException {

        final DotSecurityException exception = new DotSecurityException("");

        try {
            when(contentletAPI.searchIndex(BASE_LUCENE_QUERY, 0, -1, "parentPath asc", user, false))
                    .thenThrow(exception);

            final ThemePaginator themePaginator = new ThemePaginator(contentletAPI, folderAPI);

            themePaginator.getItems(user, 0, -1, null);
            assertTrue(false);
        } catch(PaginationException e){
            assertEquals(exception, e.getCause());
        }
    }

    /**
     * Test of {@link ThemePaginator#getItems(User, int, int, Map)}
     *
     * Given: throw a DotDataException
     * Should: throw a DotDataException
     */
    @Test
    public void findThemesThrowDotDataException() throws DotSecurityException, DotDataException {

        final DotDataException exception = new DotDataException("");

        try {
            when(contentletAPI.searchIndex(BASE_LUCENE_QUERY, 0, -1, "parentPath asc", user, false))
                    .thenThrow(exception);

            final ThemePaginator themePaginator = new ThemePaginator(contentletAPI, folderAPI);

            themePaginator.getItems(user, 0, -1, null);
            assertTrue(false);
        } catch(PaginationException e){
            assertEquals(exception, e.getCause());
        }
    }

    /**
     * Test of {@link ThemePaginator#getItems(User, int, int, Map)}
     *
     * Given: OrderDirection.DESC
     * Should: parentPath desc sortby
     */
    @Test
    public void findOrderDesc() throws DotDataException, DotSecurityException {

        final PaginatedArrayList<ContentletSearch> contentletSearchList = new PaginatedArrayList<>();
        contentletSearchList.addAll(list(createContentSearchMock("1"), createContentSearchMock("2"),
                createContentSearchMock("3")));
        final List<Contentlet> contentlets = list(createContentletMock("1"), createContentletMock("2"),
                createContentletMock("3"));

        final List<String> elements = list("1", "2", "3");

        final ThemePaginator themePaginator = new ThemePaginator(contentletAPI, folderAPI);

        when(contentletAPI.searchIndex(QUERY_WITH_HOST, 0, -1, "parentPath desc", user, false))
                .thenReturn(contentletSearchList);

        when(contentletAPI.findContentlets(elements)).thenReturn(contentlets);

        //mocking folders
        elements.forEach(elem -> {
            try {
                final Folder folder = createFolderMock(elem);
                when(folderAPI.find(elem, user, false)).thenReturn(folder);
                when(contentletAPI.search(getFolderQuery(elem),-1, 0, null, user, false)).thenReturn(null);
            } catch (DotSecurityException | DotDataException e) {
                Logger.error(this, e.getMessage());
            }
        });

        final Map<String, Object> params = map(
                ThemePaginator.HOST_ID_PARAMETER_NAME, "1",
                Paginator.ORDER_DIRECTION_PARAM_NAME, OrderDirection.DESC
        );
        final PaginatedArrayList<Folder> themes = themePaginator.getItems(user, 0, -1, params);

        assertEquals(contentlets.size(), themes.size());
        assertEquals(contentlets.get(0).getFolder(), themes.get(0).getInode());
        assertEquals(contentlets.get(1).getFolder(), themes.get(1).getInode());
        assertEquals(contentlets.get(2).getFolder(), themes.get(2).getInode());
    }

    /**
     * Test of {@link ThemePaginator#getItems(User, int, int, Map)}
     *
     * Given: hostId null and OrderDirection DESC
     * Should: return list of contents
     */
    @Test
    public void findOrderAscWithHostIdNull() throws DotDataException, DotSecurityException {

        final PaginatedArrayList<ContentletSearch> contentletSearchList = new PaginatedArrayList<>();
        contentletSearchList.addAll(list(createContentSearchMock("1"), createContentSearchMock("2"),
                createContentSearchMock("3")));
        final List<Contentlet> contentlets = list(createContentletMock("1"), createContentletMock("2"),
                createContentletMock("3"));

        final List<String> elements = list("1", "2", "3");

        final ThemePaginator themePaginator = new ThemePaginator(contentletAPI, folderAPI);

        when(contentletAPI.searchIndex(BASE_LUCENE_QUERY, -1, 0, "parentPath desc", user, false))
                .thenReturn(contentletSearchList);

        when(contentletAPI.findContentlets(elements)).thenReturn(contentlets);

        //mocking folders
        elements.forEach(elem -> {
            try {
                final Folder folder = createFolderMock(elem);
                when(folderAPI.find(elem, user, false)).thenReturn(folder);
                when(contentletAPI.search(getFolderQuery(elem),-1, 0, null, user, false)).thenReturn(null);
            } catch (DotSecurityException | DotDataException e) {
                Logger.error(this, e.getMessage());
            }
        });

        Map<String, Object> params = map(
                Paginator.ORDER_DIRECTION_PARAM_NAME, OrderDirection.DESC
        );

        final PaginatedArrayList<Folder> themes = themePaginator.getItems(user, -1, 0, params);

        assertEquals(contentlets.size(), themes.size());
        assertEquals(contentlets.get(0).getFolder(), themes.get(0).getInode());
        assertEquals(contentlets.get(1).getFolder(), themes.get(1).getInode());
        assertEquals(contentlets.get(2).getFolder(), themes.get(2).getInode());
    }

    private ContentletSearch createContentSearchMock(final String contentInode) {
        final ContentletSearch mockContentletSearch = mock(ContentletSearch.class);

        when(mockContentletSearch.getInode()).thenReturn(contentInode);

        return mockContentletSearch;
    }

    private Contentlet createContentletMock(final String contentInode){
        final Contentlet contentlet = mock(Contentlet.class);
        when(contentlet.getFolder()).thenReturn(contentInode);
        return contentlet;
    }

    private Folder createFolderMock(final String id){
        final Folder folder = mock(Folder.class);
        when(folder.getInode()).thenReturn(id);
        return folder;
    }

    private String getFolderQuery(final String folderInode){
        final StringBuilder query = new StringBuilder();
        query.append("+conFolder:").append(folderInode).append(" +title:").append(ThemePaginator.THEME_PNG);

        return query.toString();
    }
}
