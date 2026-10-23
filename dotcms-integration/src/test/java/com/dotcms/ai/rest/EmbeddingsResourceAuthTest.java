package com.dotcms.ai.rest;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.ai.rest.forms.EmbeddingsForm;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.exception.SecurityException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Access tests for {@link EmbeddingsResource#embed}. The endpoint accepts a request-supplied
 * Velocity template and renders it server-side, so it requires the same CMS Administrator role as
 * the other administrative methods on the resource ({@code DELETE /db}, {@code GET /indexCount}).
 */
public class EmbeddingsResourceAuthTest {

    private static EmbeddingsResource resource;
    private static HttpServletResponse response;
    private static User adminUser;
    private static User nonAdminUser;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        resource = new EmbeddingsResource();
        response = new MockHttpResponse().response();
        adminUser = TestUserUtils.getAdminUser();
        nonAdminUser = new UserDataGen().nextPersisted();
        APILocator.getRoleAPI().addRoleToUser(
                APILocator.getRoleAPI().loadBackEndUserRole(), nonAdminUser);
    }

    private static EmbeddingsForm form() {
        return new EmbeddingsForm.Builder()
                .query("+contentType:doesNotExist711Auth +live:true")
                .indexName("sec711auth")
                .build();
    }

    private static HttpServletRequest requestForUser(final User user) {
        final HttpServletRequest request = new MockAttributeRequest(
                new MockHttpRequestIntegrationTest("localhost", "/").request()).request();
        request.setAttribute(WebKeys.USER, user);
        return request;
    }

    /**
     * Given scenario: a back-end user without the CMS Administrator role posts to the embeddings
     *                 endpoint.
     * Expected result: the request is rejected at initialization, before any template is rendered,
     *                  the same way the administrative sibling methods reject it.
     */
    @Test(expected = SecurityException.class)
    public void test_embed_asNonAdmin_throwsSecurity() {
        resource.embed(requestForUser(nonAdminUser), response, form());
    }

    /**
     * Given scenario: an administrator posts to the embeddings endpoint with a query that matches
     *                 no content.
     * Expected result: the call is not rejected by the access check (no 401/403); the
     *                  administrative path keeps working.
     */
    @Test
    public void test_embed_asAdmin_isNotRejected() {
        final Response result = resource.embed(requestForUser(adminUser), response, form());
        assertNotNull(result);
        assertNotEquals(401, result.getStatus());
        assertNotEquals(403, result.getStatus());
    }
}
