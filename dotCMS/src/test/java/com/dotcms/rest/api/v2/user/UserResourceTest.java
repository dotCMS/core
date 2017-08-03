package com.dotcms.rest.api.v2.user;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.contenttype.ContentTypeHelper;
import com.dotcms.rest.api.v1.contenttype.ContentTypeResource;
import com.dotcms.util.PaginationUtil;
import com.dotcms.util.pagination.OrderDirection;
import com.dotcms.util.pagination.UserPaginator;
import com.liferay.portal.model.User;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * test {@link UserResource}
 */
public class UserResourceTest {

    @Test
    public void testLoginAsData(){
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);

        String filter = "filter";
        int page = 3;
        int perPage = 4;

        List<User> users = new ArrayList<>();
        Response responseExpected = Response.ok(new ResponseEntityView(users)).build();

        final PaginationUtil paginationUtil = mock(PaginationUtil.class);
        when(paginationUtil.getPage(request, user, filter, false, page, perPage )).thenReturn(responseExpected);


        final UserResource resource = new UserResource( webResource, paginationUtil );
        Response response = null;

        RestUtilTest.verifySuccessResponse(
                response = resource.loginAsData(request, filter, page, perPage)
        );

        assertEquals(responseExpected.getEntity(), response.getEntity());
    }
}
