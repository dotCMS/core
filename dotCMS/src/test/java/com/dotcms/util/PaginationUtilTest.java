package com.dotcms.util;

import com.dotcms.repackage.javax.ws.rs.core.MultivaluedMap;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.Paginator;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
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
    public void testPage(){
        HttpServletRequest req = mock( HttpServletRequest.class );
        User user = new User();
        String filter = "filter";
        boolean showArchived = false;
        int page = 2;
        int perPage = 5;
        String orderBy = "name";
        OrderDirection direction = OrderDirection.ASC;
        int offset = page * perPage;
        long totalRecords = 10;
        StringBuffer baseURL = new StringBuffer("/baseURL");

        String headerLink = "</baseURL?filter=filter&per_page=5&orderby=name&page=1&direction=ASC>;rel=\"first\",</baseURL?filter=filter&per_page=5&orderby=name&page=2&direction=ASC>;rel=\"last\",</baseURL?filter=filter&per_page=5&orderby=name&page=pageValue&direction=ASC>;rel=\"x-page\",</baseURL?filter=filter&per_page=5&orderby=name&page=1&direction=ASC>;rel=\"prev\"";

        List items = new ArrayList<>();

        when( req.getRequestURL() ).thenReturn( baseURL );
        when( paginator.getItems( user, filter, perPage, offset, orderBy, direction ) ).thenReturn( items );
        when( paginator.getTotalRecords( filter )).thenReturn( totalRecords );

        Response response = paginationUtil.getPage(req, user, filter, page, perPage, orderBy, direction, null);

        Object entity = ((ResponseEntityView) response.getEntity()).getEntity();

        assertEquals( items, entity );
        assertEquals( response.getHeaderString("X-Pagination-Per-Page"), String.valueOf( perPage ) );
        assertEquals( response.getHeaderString("X-Pagination-Current-Page"), String.valueOf( page ) );
        assertEquals( response.getHeaderString("X-Pagination-Link-Pages"), "5" );
        assertEquals( response.getHeaderString("X-Pagination-Total-Entries"), String.valueOf( totalRecords ) );
        assertEquals( response.getHeaderString("Link"), headerLink );
    }
}
