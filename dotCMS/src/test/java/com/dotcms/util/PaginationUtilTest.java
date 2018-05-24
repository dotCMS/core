package com.dotcms.util;

import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonNode;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.PaginationException;
import com.dotcms.util.pagination.Paginator;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;

public class PaginationUtilTest {

    @Test
    public void testPage() throws IOException {

        final HttpServletRequest req = mock( HttpServletRequest.class );
        final User user = new User();
        final String filter = "filter";
        final int page = 2;
        final int perPage = 5;
        final String orderBy = "name";
        final OrderDirection direction = OrderDirection.ASC;
        final long totalRecords = 10;
        final StringBuffer baseURL = new StringBuffer("/baseURL");

        final String headerLink = "</baseURL?filter=filter&per_page=5&orderby=name&page=1&direction=ASC>;rel=\"first\",</baseURL?filter=filter&per_page=5&orderby=name&page=2&direction=ASC>;rel=\"last\",</baseURL?filter=filter&per_page=5&orderby=name&page=pageValue&direction=ASC>;rel=\"x-page\",</baseURL?filter=filter&per_page=5&orderby=name&page=1&direction=ASC>;rel=\"prev\"";

        final PaginatedArrayList items = new PaginatedArrayList<>();
        items.add(new PaginationUtilModeTest("testing"));
        items.setTotalResults(totalRecords);

        when( req.getRequestURL() ).thenReturn( baseURL );

        final Paginator paginator = new Paginator() {
            @Override
            public PaginatedArrayList getItems(User user, int limit, int offset, Map params)
                    throws PaginationException {
                return items;
            }
        };

        final PaginationUtil paginationUtil = new PaginationUtil( paginator );

        final Response response = paginationUtil.getPage(req, user, filter, page, perPage, orderBy, direction, map());

        final ObjectMapper objectMapper = new ObjectMapper();
        final String responseString = response.getEntity().toString();
        final JsonNode jsonNode = objectMapper.readTree(responseString);

        assertEquals( "testing", jsonNode.get("entity").elements().next().get("testing").asText() );
        final JsonNode pagination = jsonNode.get("pagination");
        assertEquals( pagination.get("perPage").asInt(), perPage );
        assertEquals( pagination.get("currentPage").asInt(), page );
        assertEquals( pagination.get("linkPages").asInt(), 5);
        assertEquals( pagination.get("totalRecords").asInt(), totalRecords );
        assertEquals( pagination.get("link").asText(), headerLink);
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
