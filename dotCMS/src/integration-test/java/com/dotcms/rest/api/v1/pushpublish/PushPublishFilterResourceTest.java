package com.dotcms.rest.api.v1.pushpublish;

import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherAPIImpl;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.internal.util.Base64;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.rest.exception.SecurityException;

public class PushPublishFilterResourceTest {

    static HttpServletResponse response;
    static PushPublishFilterResource resource;
    static String filterKey;

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();
        resource = new PushPublishFilterResource();
        response = new MockHttpResponse();

        PublisherAPIImpl.class.cast(APILocator.getPublisherAPI()).getFilterDescriptorMap().clear();
        filterKey = "filterTestAPI.yml";

        createFilter();
    }

    private static void createFilter(){
        final Map<String,Object> filtersMap =
                ImmutableMap.of("dependencies",true,"relationships",true,"excludeClasses","Host,Workflow");
        final FilterDescriptor filterDescriptor =
                new FilterDescriptor(filterKey,"Filter Test Title",filtersMap,true,"Reviewer,dotcms.org.2789");

        APILocator.getPublisherAPI().addFilterDescriptor(filterDescriptor);
    }

    private HttpServletRequest getHttpRequest(final boolean authorization) {
        final MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(
                        new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                                .request())
                        .request());

        if(authorization) {
            request.setHeader("Authorization",
                    "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));

        }

        return request;
    }

    /**
     * Method to test: {@link PushPublishFilterResource#getFilters(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Get the filters that the user has access to, passing a user
     * ExpectedResult: filters that the user has access to, 200 Code
     *
     */
    @Test
    public void test_getFilter_withUser_success_returnFilter() throws DotDataException {
        final Response responseResource = resource.getFilters(getHttpRequest(true),response);
        Assert.assertEquals(Status.OK.getStatusCode(),responseResource.getStatus());
        final ResponseEntityView responseEntityView = ResponseEntityView.class.cast(responseResource.getEntity());
        Assert.assertTrue(responseEntityView.getEntity().toString().contains(filterKey));
    }

    /**
     * Method to test: {@link PushPublishFilterResource#getFilters(HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Get the filters that the user has access to, but no user is set
     * ExpectedResult: 401 Code, Invalid User, SecurityException
     *
     */
    @Test(expected = SecurityException.class)
    public void test_getFilter_noUser_return401() throws DotDataException {
        resource.getFilters(getHttpRequest(false),response);
    }
}
