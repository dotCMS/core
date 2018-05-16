package com.dotcms.util.pagination;

import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpages.theme.business.ThemeAPIImpl;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Test;

import java.util.List;

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
        final PaginatedArrayList<Contentlet> themesExpected = new PaginatedArrayList<Contentlet>();
        themesExpected.setTotalResults(1);
        themesExpected.addAll(list(mock(Contentlet.class)));

        when(contentletAPI.search(query, 0, -1, null, user, false)).thenReturn(themesExpected);

        final ThemePaginator themePaginator = new ThemePaginator(contentletAPI);

        final PaginatedArrayList<Contentlet> themes = themePaginator.getItems(user, null, -1, 0, null,
                OrderDirection.ASC, map(ThemePaginator.HOST_ID_PARAMETER_NAME, "1"));

        assertEquals(themesExpected, themes);
    }

    /**
     * Test of {@link ThemeAPIImpl#findAll(User, String)}
     *
     * Given: a hostId null
     * Should: return all the themes
     */
    /*public void findThemesWithoutHostId() throws DotSecurityException, DotDataException {
        final String query = "+parentpath:/application/themes/* +title:template.vtl";
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final User user = mock(User.class);
        final List<Contentlet> themesExpected = list(mock(Contentlet.class));

        when(contentletAPI.search(query, 0, -1, null, user, false)).thenReturn(themesExpected);

        final ThemeAPIImpl themeAPI = new ThemeAPIImpl(contentletAPI);

        final List<Contentlet> themes = themeAPI.findAll(user, null);

        assertEquals(themesExpected, themes);
    }*/

    /**
     * Test of {@link ThemeAPIImpl#findAll(User, String)}
     *
     * Given: throw a DotSecurityException
     * Should: throw a DotSecurityException
     */
    /*public void findThemesThrowDotSecurityException() throws  DotDataException {
        final String query = "+parentpath:/application/themes/* +title:template.vtl";
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final User user = mock(User.class);

        final DotSecurityException exception = new DotSecurityException("");

        try {
            when(contentletAPI.search(query, 0, -1, null, user, false)).thenThrow(exception);

            final ThemeAPIImpl themeAPI = new ThemeAPIImpl(contentletAPI);

            themeAPI.findAll(user, null);
            assertTrue(false);
        } catch(DotSecurityException e){
            assertTrue(true);
        }
    }*/

    /**
     * Test of {@link ThemeAPIImpl#findAll(User, String)}
     *
     * Given: throw a DotDataException
     * Should: throw a DotDataException
     */
    /*public void findThemesThrowDotDataException() throws  DotSecurityException {
        final String query = "+parentpath:/application/themes/* +title:template.vtl";
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final User user = mock(User.class);

        final DotDataException exception = new DotDataException("");

        try {
            when(contentletAPI.search(query, 0, -1, null, user, false)).thenThrow(exception);

            final ThemeAPIImpl themeAPI = new ThemeAPIImpl(contentletAPI);

            themeAPI.findAll(user, null);
            assertTrue(false);
        } catch(DotDataException e){
            assertTrue(true);
        }
    }*/

    /**
     * Test of {@link ThemeAPIImpl#find(User, String, int, int, OrderDirection)}
     *
     * Given: OrderDirection.ASC
     * Should: parentPath asc sortby
     */
    /*public void findOrderAsc() throws DotDataException, DotSecurityException {
        final String hostId = "1";
        final int limit = 10;
        final int offset = 11;
        final String query = "+parentpath:/application/themes/* +title:template.vtl +conhost:1";
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final User user = mock(User.class);
        final List<ContentletSearch> contentletSearchList = list(createContentSerachMock("1"),
                createContentSerachMock("2"), createContentSerachMock("3"));
        final List<Contentlet> contentletListExpected = list(mock(Contentlet.class), mock(Contentlet.class), mock(Contentlet.class));

        when(contentletAPI.searchIndex(query, limit, offset, "parentPath asc", user, false))
                .thenReturn(contentletSearchList);
        when(contentletAPI.findContentlets(list("1", "2", "3"))).thenReturn(contentletListExpected);

        final ThemeAPIImpl themeAPI = new ThemeAPIImpl(contentletAPI);

        final List<Contentlet> themes = themeAPI.find(user, hostId, limit, offset, OrderDirection.ASC);
        assertEquals(contentletListExpected, themes);
    }*/

    /**
     * Test of {@link ThemeAPIImpl#find(User, String, int, int, OrderDirection)}
     *
     * Given: OrderDirection.DESC
     * Should: parentPath desc sortby
     */
    /*public void findOrderDesc() throws DotDataException, DotSecurityException {
        final String hostId = "1";
        final int limit = 10;
        final int offset = 11;
        final String query = "+parentpath:/application/themes/* +title:template.vtl +conhost:1";
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final User user = mock(User.class);
        final List<ContentletSearch> contentletSearchList = list(createContentSerachMock("1"),
                createContentSerachMock("2"), createContentSerachMock("3"));
        final List<Contentlet> contentletListExpected = list(mock(Contentlet.class), mock(Contentlet.class), mock(Contentlet.class));

        when(contentletAPI.searchIndex(query, limit, offset, "parentPath desc", user, false))
                .thenReturn(contentletSearchList);
        when(contentletAPI.findContentlets(list("1", "2", "3"))).thenReturn(contentletListExpected);

        final ThemeAPIImpl themeAPI = new ThemeAPIImpl(contentletAPI);

        final List<Contentlet> themes = themeAPI.find(user, hostId, limit, offset, OrderDirection.DESC);
        assertEquals(contentletListExpected, themes);
    }*/

    /**
     * Test of {@link ThemeAPIImpl#find(User, String, int, int, OrderDirection)}
     *
     * Given: hostId null and any OrderDirection
     * Should: return list of contents
     */
    /*public void findOrderAscWithHostIdNull() throws DotDataException, DotSecurityException {
        final String hostId = null;
        final int limit = 10;
        final int offset = 11;
        final String query = "+parentpath:/application/themes/* +title:template.vtl";
        final ContentletAPI contentletAPI = mock(ContentletAPI.class);
        final User user = mock(User.class);
        final List<ContentletSearch> contentletSearchList = list(createContentSerachMock("1"),
                createContentSerachMock("2"), createContentSerachMock("3"));
        final List<Contentlet> contentletListExpected = list(mock(Contentlet.class), mock(Contentlet.class), mock(Contentlet.class));

        when(contentletAPI.searchIndex(query, limit, offset, "parentPath asc", user, false))
                .thenReturn(contentletSearchList);
        when(contentletAPI.findContentlets(list("1", "2", "3"))).thenReturn(contentletListExpected);

        final ThemeAPIImpl themeAPI = new ThemeAPIImpl(contentletAPI);

        final List<Contentlet> themes = themeAPI.find(user, hostId, limit, offset, OrderDirection.ASC);
        assertEquals(contentletListExpected, themes);
    }*/

    private ContentletSearch createContentSerachMock(String contentInode) {
        ContentletSearch mockContentletSearch = mock(ContentletSearch.class);

        when(mockContentletSearch.getInode()).thenReturn(contentInode);

        return mockContentletSearch;
    }
}
