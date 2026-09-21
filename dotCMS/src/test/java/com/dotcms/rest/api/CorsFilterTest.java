package com.dotcms.rest.api;

import com.dotcms.inference.rest.ChatCompletionsResource;
import com.dotcms.inference.rest.EmbeddingsResource;
import com.dotcms.inference.rest.ImagesResource;
import com.dotcms.inference.rest.ModelsResource;
import com.dotcms.rest.annotation.NoCors;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers the {@link NoCors} opt-out, which is the only thing standing between the
 * {@code /api/inference/v1} family and the cross-origin headers dotCMS emits by default.
 *
 * <p>Worth its own test because the gap it closes is invisible: {@code api.cors.default.*} ships
 * enabled, so a resource that says nothing is served {@code Access-Control-Allow-Origin: *} and
 * nothing in the resource's own code hints at it. A regression here would not fail any of this
 * family's other tests — they call the resource methods directly and never reach a response
 * filter — and would quietly advertise to browsers that a long-lived bearer token is welcome in
 * page JavaScript.</p>
 */
public class CorsFilterTest {

    /** A resource that has said nothing about CORS, as most of dotCMS has. */
    private static class OrdinaryResource {
    }

    @NoCors
    private static class OptedOutResource {
    }

    /**
     * Given each resource of the inference family
     * When the CORS filter decides whether to add its headers
     * Then every one of them is exempt
     *
     * <p>Asserted per class rather than on one of them: the annotation is applied by hand
     * to each resource, so a fifth operation added later without it is exactly the mistake this
     * catches, and testing a single representative would not.</p>
     */
    @Test
    public void test_everyInferenceResource_optsOutOfCors() {
        final CorsFilter filter = new CorsFilter();

        for (final Object resource : new Object[]{
                new ChatCompletionsResource(),
                new EmbeddingsResource(),
                new ImagesResource(),
                new ModelsResource()}) {
            assertTrue(resource.getClass().getSimpleName()
                            + " must not receive cross-origin headers: its credential is a "
                            + "long-lived bearer token, and advertising CORS invites putting it "
                            + "in browser JavaScript",
                    filter.optsOutOfCors(resource));
        }
    }

    /**
     * Given a resource that has not opted out, and the unmatched case
     * When the CORS filter decides
     * Then the opt-out does not apply, so dotCMS's default behaviour is untouched
     *
     * <p>The other half of that guarantee, and the one that keeps this change additive: the
     * exemption has to be something a resource asks for, not something the filter now does to
     * everyone. A null stands in for a request that matched no resource, which the filter already
     * tolerates.</p>
     */
    @Test
    public void test_resourcesThatDidNotOptOut_areUnaffected() {
        final CorsFilter filter = new CorsFilter();

        assertFalse("A resource that never asked to be exempt must keep dotCMS's default "
                + "cross-origin behaviour", filter.optsOutOfCors(new OrdinaryResource()));
        assertFalse("An unmatched request must not blow up the filter",
                filter.optsOutOfCors(null));
        assertTrue(filter.optsOutOfCors(new OptedOutResource()));
    }
}
