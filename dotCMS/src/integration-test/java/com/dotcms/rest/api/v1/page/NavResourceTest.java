package com.dotcms.rest.api.v1.page;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonNode;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.javax.ws.rs.core.Response.Status;
import com.dotcms.repackage.org.glassfish.jersey.internal.util.Base64;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class NavResourceTest{

        @BeforeClass
        public static void prepare() throws Exception {
            //Setting web app environment
            IntegrationTestInitService.getInstance().init();
        }

    /**
     * BasicAuth
     */
    private HttpServletRequest getHttpRequest() {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                                .request())
                        .request());

        request.setHeader("Authorization",
                "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

        return request;
    }

    @Test
    public void getAboutUsNav_WhenDepthIsNotAValidNumber_BadRequest() throws IOException {
        final NavResource resource = new NavResource();
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final Response response = resource.loadJson(getHttpRequest(),mockResponse,"/about-us","asdad","1");
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),response.getStatus());
    }

    @Test
    public void getAboutUsNav_WhenLanguageIsNotAValidNumber_BadRequest() throws IOException {
        final NavResource resource = new NavResource();
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final Response response = resource.loadJson(getHttpRequest(),mockResponse,"/about-us","2","asdad");
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),response.getStatus());
    }

    @Test
    public void getAboutUsNav_WhenLanguageDoesNotExists_NotFound() throws IOException {
        final NavResource resource = new NavResource();
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final Response response = resource.loadJson(getHttpRequest(),mockResponse,"/about-us","2","99");
        Assert.assertEquals(Status.BAD_REQUEST.getStatusCode(),response.getStatus());
    }

    @Test
    public void getAboutUsNav_WhenDepthAndLanguageAreValid_Success() throws IOException {
        final NavResource resource = new NavResource();
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final Response response = resource.loadJson(getHttpRequest(),mockResponse,"/about-us","2","1");
        Assert.assertEquals(Status.OK.getStatusCode(),response.getStatus());

        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(response.getEntity());
        Assert.assertTrue(responseEntityView.toString().contains("About Us"));

    }

}
