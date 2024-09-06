package com.dotcms.util;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.theme.ThemeResource;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.Paginator;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PaginationUtilTest {

    private PaginationUtil paginationUtil;
    private Paginator paginator;

    @Before
    public void init(){
        paginator = mock( Paginator.class );
        paginationUtil = new PaginationUtil( paginator );
    }

    @Test
    public void testPage() throws IOException {
        final HttpServletRequest req = mock( HttpServletRequest.class );
        final User user = new User();
        final String filter = "filter";
        final int page = 2;
        final int perPage = 5;
        final String orderBy = "name";
        final OrderDirection direction = OrderDirection.ASC;
        final int offset = (page - 1) * perPage;
        final long totalRecords = 10;
        final StringBuffer baseURL = new StringBuffer("/baseURL");

        final String headerLink = "</baseURL?filter=filter&per_page=5&orderby=name&page=1&direction=ASC>;rel=\"first\",</baseURL?filter=filter&per_page=5&orderby=name&page=2&direction=ASC>;rel=\"last\",</baseURL?filter=filter&per_page=5&orderby=name&page=pageValue&direction=ASC>;rel=\"x-page\",</baseURL?filter=filter&per_page=5&orderby=name&page=1&direction=ASC>;rel=\"prev\"";

        final PaginatedArrayList items = new PaginatedArrayList<>();
        items.add(new PaginationUtilModeTest("testing"));
        items.setTotalResults(totalRecords);

        when( req.getRequestURI() ).thenReturn( baseURL.toString() );

        final Map<String, Object> params = Map.of(
                Paginator.DEFAULT_FILTER_PARAM_NAME, filter,
                Paginator.ORDER_BY_PARAM_NAME, orderBy,
                Paginator.ORDER_DIRECTION_PARAM_NAME, direction
        );

        when( paginator.getItems( user, perPage, offset, params ) ).thenReturn( items );

        final Response response = paginationUtil.getPage(req, user, filter, page, perPage, orderBy, direction, new HashMap<>());


        final Collection entity = (Collection) ((ResponseEntityView) response.getEntity()).getEntity();

        assertEquals( entity, items );
        assertEquals( response.getHeaderString("X-Pagination-Per-Page"), String.valueOf( perPage ) );
        assertEquals( response.getHeaderString("X-Pagination-Current-Page"), String.valueOf( page ) );
        assertEquals( response.getHeaderString("X-Pagination-Link-Pages"), "5" );
        assertEquals( response.getHeaderString("X-Pagination-Total-Entries"), String.valueOf( totalRecords ) );
        assertEquals( response.getHeaderString("Link"), headerLink );
    }

    @Test
    public void when_One_extraParamsIsACollection() throws IOException {
        final HttpServletRequest req = mock( HttpServletRequest.class );
        final User user = new User();
        final String filter = "filter";
        final int page = 2;
        final int perPage = 5;
        final String orderBy = "name";
        final OrderDirection direction = OrderDirection.ASC;
        final int offset = (page - 1) * perPage;
        final long totalRecords = 10;
        final StringBuffer baseURL = new StringBuffer("/baseURL");

        final String headerLink = "</baseURL?filter=filter&per_page=5&orderby=name&page=1&type=A%2CB&direction=ASC>;rel=\"first\",</baseURL?filter=filter&per_page=5&orderby=name&page=2&type=A%2CB&direction=ASC>;rel=\"last\",</baseURL?filter=filter&per_page=5&orderby=name&page=pageValue&type=A%2CB&direction=ASC>;rel=\"x-page\",</baseURL?filter=filter&per_page=5&orderby=name&page=1&type=A%2CB&direction=ASC>;rel=\"prev\"";

        final PaginatedArrayList items = new PaginatedArrayList<>();
        items.add(new PaginationUtilModeTest("testing"));
        items.setTotalResults(totalRecords);

        when( req.getRequestURI() ).thenReturn( baseURL.toString() );

        final Map<String, Object> params = Map.of(
                "type", list("A", "B"),
                Paginator.DEFAULT_FILTER_PARAM_NAME, filter,
                Paginator.ORDER_BY_PARAM_NAME, orderBy,
                Paginator.ORDER_DIRECTION_PARAM_NAME, direction
        );

        final Map<String, Object> extraParams = Map.of(
                "type", list("A", "B")
        );


        when( paginator.getItems( user, perPage, offset, params ) ).thenReturn( items );

        final Response response = paginationUtil.getPage(req, user, filter, page, perPage, orderBy, direction, extraParams);

        assertEquals(headerLink, response.getHeaderString("Link"));
    }

    /**
     * Test of {@link ThemeResource#findThemes(HttpServletRequest, String, int, int, String)}
     *
     * Given: A null filter parameter
     * Should: Should pass a empty String value to the paginator
     */
    @Test
    public void testPageWhenFilterIsNull() throws IOException {
        final HttpServletRequest req = mock( HttpServletRequest.class );
        final User user = new User();
        final String filter = null;
        final int page = 2;
        final int perPage = 5;
        final String orderBy = "name";
        final OrderDirection direction = OrderDirection.ASC;
        final int offset = (page - 1) * perPage;
        final long totalRecords = 10;
        final StringBuffer baseURL = new StringBuffer("/baseURL");

        final PaginatedArrayList items = new PaginatedArrayList<>();
        items.add(new PaginationUtilModeTest("testing"));
        items.setTotalResults(totalRecords);

        when( req.getRequestURI() ).thenReturn( baseURL.toString() );

        final Map<String, Object> params = Map.of(
                Paginator.DEFAULT_FILTER_PARAM_NAME, "",
                Paginator.ORDER_BY_PARAM_NAME, orderBy,
                Paginator.ORDER_DIRECTION_PARAM_NAME, direction
        );

        when( paginator.getItems( user, perPage, offset, params ) ).thenReturn( items );

        paginationUtil.getPage(req, user, filter, page, perPage, orderBy, direction, new HashMap<>());

        verify(paginator).getItems(user, perPage, offset, params);
    }

    /**
     * Test of {@link ThemeResource#findThemes(HttpServletRequest, String, int, int, String)}
     *
     * Given: getItems return null
     * Should: Should return a empty collection
     */
    @Test
    public void testPageWhenFilterIsEmpty() throws IOException {
        final HttpServletRequest req = mock( HttpServletRequest.class );
        final User user = new User();
        final String filter = null;
        final int page = 2;
        final int perPage = 5;
        final String orderBy = "name";
        final OrderDirection direction = OrderDirection.ASC;
        final int offset = (page - 1) * perPage;
        final StringBuffer baseURL = new StringBuffer("/baseURL");

        when( req.getRequestURI() ).thenReturn( baseURL.toString() );

        final Map<String, Object> params = Map.of(
                Paginator.DEFAULT_FILTER_PARAM_NAME, "",
                Paginator.ORDER_BY_PARAM_NAME, orderBy,
                Paginator.ORDER_DIRECTION_PARAM_NAME, direction
        );

        when( paginator.getItems( user, perPage, offset, params ) ).thenReturn( null );

        final Response response = paginationUtil.getPage(req, user, filter, page, perPage, orderBy, direction, new HashMap<>());

        verify(paginator).getItems(user, perPage, offset, params);

        final Collection entity = (Collection) ((ResponseEntityView) response.getEntity()).getEntity();
        assertTrue(entity.isEmpty());
    }
}

class PaginationUtilModeTest implements Serializable {
    private final String testing;

    PaginationUtilModeTest(final String testing) {
        this.testing = testing;
    }

    public String getTesting() {
        return testing;
    }
}
