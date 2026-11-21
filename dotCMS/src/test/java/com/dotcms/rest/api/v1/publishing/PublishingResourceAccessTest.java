package com.dotcms.rest.api.v1.publishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.publisher.bundle.business.BundleAPI;
import com.dotcms.publisher.business.PublishAuditAPI;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.WebResource;
import io.vavr.Lazy;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Checks which portlets each {@link PublishingResource} operation requires when it initializes the
 * request. Each call is stopped right after init, so only the init request is asserted.
 */
class PublishingResourceAccessTest {

    private static final String BUNDLE_ID = "01ABCDEFGHJKMNPQRSTVWXYZ00";
    private static final Set<String> PUBLISHING_QUEUE_PORTLETS =
            Set.of("publishing-queue", "publishing-queue-beta");

    private WebResource webResource;
    private PublishingResource resource;

    @BeforeEach
    void setUp() {
        webResource = mock(WebResource.class);
        // Stop each call right after the request is initialized: only the init request matters here
        when(webResource.init(any(WebResource.InitBuilder.class))).thenThrow(new StopAfterInit());

        resource = new PublishingResource(webResource,
                Lazy.of(() -> mock(PublishAuditAPI.class)),
                Lazy.of(() -> mock(BundleAPI.class)),
                Lazy.of(() -> mock(com.dotcms.publishing.PublisherAPI.class)),
                Lazy.of(() -> mock(com.dotcms.publisher.business.PublisherAPI.class)),
                mock(PublishingJobsHelper.class),
                mock(PublishingRetryHelper.class));
    }

    @Test
    void listPublishingJobs_requiresPublishingQueuePortlet() throws Exception {
        callStoppedAfterInit(() -> resource.listPublishingJobs(mock(HttpServletRequest.class),
                new EmptyHttpResponse(), 1, 50, null, null));

        assertPublishingQueuePortletRequired();
    }

    @Test
    void getPublishingJobDetails_requiresPublishingQueuePortlet() throws Exception {
        callStoppedAfterInit(() -> resource.getPublishingJobDetails(mock(HttpServletRequest.class),
                new EmptyHttpResponse(), BUNDLE_ID));

        assertPublishingQueuePortletRequired();
    }

    @Test
    void deletePublishingJob_requiresPublishingQueuePortlet() throws Exception {
        callStoppedAfterInit(() -> resource.deletePublishingJob(mock(HttpServletRequest.class),
                new EmptyHttpResponse(), BUNDLE_ID));

        assertPublishingQueuePortletRequired();
    }

    @Test
    void retryBundles_requiresPublishingQueuePortlet() throws Exception {
        callStoppedAfterInit(() -> resource.retryBundles(mock(HttpServletRequest.class),
                new EmptyHttpResponse(), null));

        assertPublishingQueuePortletRequired();
    }

    @Test
    void pushBundle_requiresPublishingQueuePortlet() throws Exception {
        callStoppedAfterInit(() -> resource.pushBundle(mock(HttpServletRequest.class),
                new EmptyHttpResponse(), BUNDLE_ID, null));

        assertPublishingQueuePortletRequired();
    }

    @Test
    void purgePublishingJobs_requiresPublishingQueuePortlet() throws Exception {
        callStoppedAfterInit(() -> resource.purgePublishingJobs(mock(HttpServletRequest.class),
                new EmptyHttpResponse(), null));

        assertPublishingQueuePortletRequired();
    }

    /**
     * Runs a resource call that is expected to stop at init.
     *
     * @param call the resource call
     */
    private void callStoppedAfterInit(final ResourceCall call) throws Exception {
        try {
            call.run();
        } catch (final StopAfterInit expected) {
            // the call is stopped after init
        }
    }

    /**
     * Asserts that the init request accepts either the Publishing Queue portlet or its new UI.
     */
    private void assertPublishingQueuePortletRequired() {
        final ArgumentCaptor<WebResource.InitBuilder> captor =
                ArgumentCaptor.forClass(WebResource.InitBuilder.class);
        verify(webResource).init(captor.capture());
        assertEquals(PUBLISHING_QUEUE_PORTLETS, captor.getValue().getRequiredPortlets(),
                "the Publishing Queue portlets must be required");
    }

    @FunctionalInterface
    private interface ResourceCall {
        void run() throws Exception;
    }

    private static final class StopAfterInit extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
