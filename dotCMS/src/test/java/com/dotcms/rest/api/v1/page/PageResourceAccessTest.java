package com.dotcms.rest.api.v1.page;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.WebResource;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Verifies the access requirements {@link PageResource} declares for its page sub-resource
 * endpoints, without starting the platform.
 */
class PageResourceAccessTest {

    private static final String PAGE_ID = "page-id";

    private WebResource webResource;
    private PageResource resource;

    @BeforeEach
    void setUp() {
        webResource = mock(WebResource.class);
        // Stop each call right after the request is initialized: only the init request matters here
        when(webResource.init(any(WebResource.InitBuilder.class))).thenThrow(new StopAfterInit());
        when(webResource.init(any(HttpServletRequest.class), any(HttpServletResponse.class), anyBoolean()))
                .thenThrow(new StopAfterInit());

        resource = new PageResource(mock(PageResourceHelper.class), webResource,
                mock(HTMLPageAssetRenderedAPI.class), mock(ContentletAPI.class), mock(HostWebAPI.class));
    }

    /**
     * Given a request to the page content tree endpoint,
     * When the resource initializes the request,
     * Then it requires the caller to be a back-end user.
     */
    @Test
    void contentTree_requiresBackendUser() {
        try {
            resource.getContentTree(mock(HttpServletRequest.class), new EmptyHttpResponse(), PAGE_ID);
        } catch (final Exception expected) {
            // the call is stopped after init
        }

        assertBackendUserRequired();
    }

    /**
     * Given a request to the page personas endpoint,
     * When the resource initializes the request,
     * Then it requires the caller to be a back-end user.
     */
    @Test
    void personasOnPage_requiresBackendUser() {
        try {
            resource.getPersonalizedPersonasOnPage(mock(HttpServletRequest.class), new EmptyHttpResponse(),
                    null, 0, 10, "title", "ASC", null, PAGE_ID, null);
        } catch (final Exception expected) {
            // the call is stopped after init
        }

        assertBackendUserRequired();
    }

    private void assertBackendUserRequired() {
        final ArgumentCaptor<WebResource.InitBuilder> captor =
                ArgumentCaptor.forClass(WebResource.InitBuilder.class);
        verify(webResource).init(captor.capture());
        assertTrue(captor.getValue().getRequiredRoles().contains(Role.DOTCMS_BACK_END_USER),
                "the back-end user role must be required");
    }

    private static final class StopAfterInit extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
