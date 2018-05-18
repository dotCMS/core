package com.dotcms.util.pagination;

import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.dotcms.util.CollectionsUtils.map;

/**
 * Test of {@linkn ThemeAPIImpl}
 */
public class ThemePaginatorTest {

    /**
     * Test of {@link ThemeAPIImpl#findAll(User, String)}
     *
     * Given: a hostId
     * Should: return all the themes
     */
    @Test
    public void findThemesWithHostId() throws DotSecurityException, DotDataException {
        final String query = "+parentpath:/application/themes/* +title:template.vtl +conhost:1";
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final User user = mock(User.class);
        final PaginatedArrayList<ContentletSearch> contentletSearchList = new PaginatedArrayList<>();
        contentletSearchList.addAll(list(createContentSerachMock("1"), createContentSerachMock("2"),
                createContentSerachMock("3")));
        final List<Contentlet> contentlets = list(mock(Contentlet.class), mock(Contentlet.class),
                mock(Contentlet.class));

        when(contentletAPI.searchIndex(query, 0, -1, "parentPath asc", user, false))
                .thenReturn(contentletSearchList);

        when(contentletAPI.findContentlets(list("1", "2", "3"))).thenReturn(contentlets);

        final ThemePaginator themePaginator = new ThemePaginator(contentletAPI);
        Map<String, Object> params = map(ThemePaginator.HOST_ID_PARAMETER_NAME, "1");
        final PaginatedArrayList<Contentlet> themes = themePaginator.getItems(user, 0, -1, params);

        assertEquals(contentlets.size(), themes.size());
        assertEquals(contentlets.get(0), themes.get(0));
        assertEquals(contentlets.get(1), themes.get(1));
        assertEquals(contentlets.get(2), themes.get(2));
    }

    /**
     * Test of {@link ThemeAPIImpl#findAll(User, String)}
     *
     * Given: a hostId null
     * Should: return all the themes
     */
    @Test
    public void findThemesWithoutHostId() throws DotSecurityException, DotDataException {
        final String query = "+parentpath:/application/themes/* +title:template.vtl";
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final User user = mock(User.class);
        final PaginatedArrayList<ContentletSearch> contentletSearchList = new PaginatedArrayList<>();
        contentletSearchList.addAll(list(createContentSerachMock("1"), createContentSerachMock("2"),
                createContentSerachMock("3")));
        final List<Contentlet> contentlets = list(mock(Contentlet.class), mock(Contentlet.class),
                mock(Contentlet.class));

        when(contentletAPI.searchIndex(query, 0, -1, "parentPath asc", user, false))
                .thenReturn(contentletSearchList);

        when(contentletAPI.findContentlets(list("1", "2", "3"))).thenReturn(contentlets);

        final ThemePaginator themePaginator = new ThemePaginator(contentletAPI);
        final PaginatedArrayList<Contentlet> themes = themePaginator.getItems(user, 0, -1, null);

        assertEquals(contentlets.size(), themes.size());
        assertEquals(contentlets.get(0), themes.get(0));
        assertEquals(contentlets.get(1), themes.get(1));
        assertEquals(contentlets.get(2), themes.get(2));
    }

    /**
     * Test of {@link ThemeAPIImpl#findAll(User, String)}
     *
     * Given: throw a DotSecurityException
     * Should: throw a DotSecurityException
     */
    @Test
    public void findThemesThrowDotSecurityException() throws DotDataException, DotSecurityException {
        final String query = "+parentpath:/application/themes/* +title:template.vtl";
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final User user = mock(User.class);

        final DotSecurityException exception = new DotSecurityException("");

        try {
            when(contentletAPI.searchIndex(query, 0, -1, "parentPath asc", user, false))
                    .thenThrow(exception);

            final ThemePaginator themePaginator = new ThemePaginator(contentletAPI);

            themePaginator.getItems(user, 0, -1, null);
            assertTrue(false);
        } catch(PaginationException e){
            assertEquals(exception, e.getCause());
        }
    }

    /**
     * Test of {@link ThemeAPIImpl#findAll(User, String)}
     *
     * Given: throw a DotDataException
     * Should: throw a DotDataException
     */
    @Test
    public void findThemesThrowDotDataException() throws DotSecurityException, DotDataException {
        final String query = "+parentpath:/application/themes/* +title:template.vtl";
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final User user = mock(User.class);

        final DotDataException exception = new DotDataException("");

        try {
            when(contentletAPI.searchIndex(query, 0, -1, "parentPath asc", user, false))
                    .thenThrow(exception);

            final ThemePaginator themePaginator = new ThemePaginator(contentletAPI);

            themePaginator.getItems(user, 0, -1, null);
            assertTrue(false);
        } catch(PaginationException e){
            assertEquals(exception, e.getCause());
        }
    }

    /**
     * Test of {@link ThemeAPIImpl#find(User, String, int, int, OrderDirection)}
     *
     * Given: OrderDirection.DESC
     * Should: parentPath desc sortby
     */
    @Test
    public void findOrderDesc() throws DotDataException, DotSecurityException {
        final String query = "+parentpath:/application/themes/* +title:template.vtl +conhost:1";
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final User user = mock(User.class);
        final PaginatedArrayList<ContentletSearch> contentletSearchList = new PaginatedArrayList<>();
        contentletSearchList.addAll(list(createContentSerachMock("1"), createContentSerachMock("2"),
                createContentSerachMock("3")));
        final List<Contentlet> contentlets = list(mock(Contentlet.class), mock(Contentlet.class),
                mock(Contentlet.class));

        when(contentletAPI.searchIndex(query, 0, -1, "parentPath desc", user, false))
                .thenReturn(contentletSearchList);

        when(contentletAPI.findContentlets(list("1", "2", "3"))).thenReturn(contentlets);

        final ThemePaginator themePaginator = new ThemePaginator(contentletAPI);
        Map<String, Object> params = map(
                ThemePaginator.HOST_ID_PARAMETER_NAME, "1",
                Paginator.ORDER_DIRECTION_PARAM_NAME, OrderDirection.DESC
        );
        final PaginatedArrayList<Contentlet> themes = themePaginator.getItems(user, 0, -1, params);

        assertEquals(contentlets.size(), themes.size());
        assertEquals(contentlets.get(0), themes.get(0));
        assertEquals(contentlets.get(1), themes.get(1));
        assertEquals(contentlets.get(2), themes.get(2));
    }

    /**
     * Test of {@link ThemeAPIImpl#find(User, String, int, int, OrderDirection)}
     *
     * Given: hostId null and OrderDirection DESC
     * Should: return list of contents
     */
    @Test
    public void findOrderAscWithHostIdNull() throws DotDataException, DotSecurityException {
        final String query = "+parentpath:/application/themes/* +title:template.vtl";
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final User user = mock(User.class);
        final PaginatedArrayList<ContentletSearch> contentletSearchList = new PaginatedArrayList<>();
        contentletSearchList.addAll(list(createContentSerachMock("1"), createContentSerachMock("2"),
                createContentSerachMock("3")));
        final List<Contentlet> contentlets = list(mock(Contentlet.class), mock(Contentlet.class),
                mock(Contentlet.class));

        when(contentletAPI.searchIndex(query, 0, -1, "parentPath desc", user, false))
                .thenReturn(contentletSearchList);

        when(contentletAPI.findContentlets(list("1", "2", "3"))).thenReturn(contentlets);

        final ThemePaginator themePaginator = new ThemePaginator(contentletAPI);

        Map<String, Object> params = map(
                Paginator.ORDER_DIRECTION_PARAM_NAME, OrderDirection.DESC
        );

        final PaginatedArrayList<Contentlet> themes = themePaginator.getItems(user, 0, -1, params);

        assertEquals(contentlets.size(), themes.size());
        assertEquals(contentlets.get(0), themes.get(0));
        assertEquals(contentlets.get(1), themes.get(1));
        assertEquals(contentlets.get(2), themes.get(2));
    }

    private ContentletSearch createContentSerachMock(String contentInode) {
        ContentletSearch mockContentletSearch = mock(ContentletSearch.class);

        when(mockContentletSearch.getInode()).thenReturn(contentInode);

        return mockContentletSearch;
    }
}
