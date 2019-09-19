package com.dotcms.rest.api.v2.user;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.WebResource.InitBuilder;
import com.dotcms.util.PaginationUtil;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * test {@link UserResource}
 */
public class UserResourceTest extends UnitTestBase {

    @Test
    public void testLoginAsData(){
        final HttpServletRequest request  = mock(HttpServletRequest.class);
        final HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        final WebResource webResource = mock(WebResource.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        final User user = new User();
        when(initDataObject.getUser()).thenReturn(user);
        // final InitDataObject initData = webResource.init(null, httpServletRequest, httpServletResponse, true, null);
        when(webResource.init(Mockito.any(InitBuilder.class))).thenReturn(initDataObject);

        String filter = "filter";
        int page = 3;
        int perPage = 4;

        List<User> users = new ArrayList<>();
        Response responseExpected = Response.ok(new ResponseEntityView(users)).build();

        final PaginationUtil paginationUtil = mock(PaginationUtil.class);
        when(paginationUtil.getPage(request, user, filter, page, perPage )).thenReturn(responseExpected);


        final UserResource resource = new UserResource( webResource, paginationUtil );
        Response response = null;

        RestUtilTest.verifySuccessResponse(
                response = resource.loginAsData(request, httpServletResponse, filter, page, perPage)
        );

        Assert.assertEquals(responseExpected.getEntity(), response.getEntity());
    }
}
