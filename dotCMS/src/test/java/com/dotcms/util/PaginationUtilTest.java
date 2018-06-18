package com.dotcms.util;

import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonNode;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.javax.ws.rs.core.MultivaluedMap;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.theme.ThemeResource;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.Paginator;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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

        when( req.getRequestURL() ).thenReturn( baseURL );

        final Map<String, Object> params = map(
                Paginator.DEFAULT_FILTER_PARAM_NAME, filter,
                Paginator.ORDER_BY_PARAM_NAME, orderBy,
                Paginator.ORDER_DIRECTION_PARAM_NAME, direction
        );

        when( paginator.getItems( user, perPage, offset, params ) ).thenReturn( items );

        final Response response = paginationUtil.getPage(req, user, filter, page, perPage, orderBy, direction, map());

        final ObjectMapper objectMapper = new ObjectMapper();
        final String responseString = response.getEntity().toString();
        final JsonNode jsonNode = objectMapper.readTree(responseString);

        assertEquals( "testing", jsonNode.get("entity").elements().next().get("testing").asText() );
        assertEquals( response.getHeaderString("X-Pagination-Per-Page"), String.valueOf( perPage ) );
        assertEquals( response.getHeaderString("X-Pagination-Current-Page"), String.valueOf( page ) );
        assertEquals( response.getHeaderString("X-Pagination-Link-Pages"), "5" );
        assertEquals( response.getHeaderString("X-Pagination-Total-Entries"), String.valueOf( totalRecords ) );
        assertEquals( response.getHeaderString("Link"), headerLink );
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

        when( req.getRequestURL() ).thenReturn( baseURL );

        final Map<String, Object> params = map(
                Paginator.DEFAULT_FILTER_PARAM_NAME, "",
                Paginator.ORDER_BY_PARAM_NAME, orderBy,
                Paginator.ORDER_DIRECTION_PARAM_NAME, direction
        );

        when( paginator.getItems( user, perPage, offset, params ) ).thenReturn( items );

        paginationUtil.getPage(req, user, filter, page, perPage, orderBy, direction, map());

        verify(paginator).getItems(user, perPage, offset, params);
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
