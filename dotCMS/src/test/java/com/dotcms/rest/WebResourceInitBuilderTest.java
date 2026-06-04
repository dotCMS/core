package com.dotcms.rest;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.Test;

/**
 * Unit tests for the runtime guard around
 * {@link WebResource.AuthCheckOptions#SERVLET_ONLY_FALLBACK_TO_ANONYMOUS_ON_AUTH_FAILURE}: the
 * option must not be settable through the generic {@link WebResource.InitBuilder#authCheckOptions}
 * API (so a REST resource cannot enable it by copy-paste) and is only reachable via the dedicated
 * {@link WebResource.InitBuilder#servletAnonymousFallbackOnAuthFailure()} method.
 */
public class WebResourceInitBuilderTest {

    private WebResource.InitBuilder builder() {
        // The guard lives entirely in the builder; a mocked WebResource avoids any API wiring.
        return new WebResource.InitBuilder(mock(WebResource.class));
    }

    /**
     * Method to test: {@link WebResource.InitBuilder#authCheckOptions(WebResource.AuthCheckOptions...)}
     * Given Scenario: the SERVLET_ONLY fallback option is passed through the generic options API.
     * Expected Result: an IllegalArgumentException is thrown (the option is reserved for servlets).
     */
    @Test(expected = IllegalArgumentException.class)
    public void servletOnlyOptionRejectedViaAuthCheckOptions() {
        builder().authCheckOptions(
                WebResource.AuthCheckOptions.SERVLET_ONLY_FALLBACK_TO_ANONYMOUS_ON_AUTH_FAILURE);
    }

    /**
     * Method to test: {@link WebResource.InitBuilder#authCheckOptions(WebResource.AuthCheckOptions...)}
     * Given Scenario: the SERVLET_ONLY option is mixed in with otherwise-valid options.
     * Expected Result: still rejected with IllegalArgumentException.
     */
    @Test(expected = IllegalArgumentException.class)
    public void servletOnlyOptionRejectedWhenMixedWithOtherOptions() {
        builder().authCheckOptions(
                WebResource.AuthCheckOptions.SKIP_CHECK_FORCE_SSL,
                WebResource.AuthCheckOptions.SERVLET_ONLY_FALLBACK_TO_ANONYMOUS_ON_AUTH_FAILURE);
    }

    /**
     * Method to test: {@link WebResource.InitBuilder#authCheckOptions(WebResource.AuthCheckOptions...)}
     * Given Scenario: only non-servlet options are passed.
     * Expected Result: accepted, builder returned for chaining.
     */
    @Test
    public void authCheckOptionsAcceptsNonServletOptions() {
        final WebResource.InitBuilder builder = builder();
        assertSame(builder, builder.authCheckOptions(
                WebResource.AuthCheckOptions.SKIP_CHECK_FORCE_SSL,
                WebResource.AuthCheckOptions.SKIP_CHECK_ANONYMOUS_PERMISSIONS));
    }

    /**
     * Method to test: {@link WebResource.InitBuilder#servletAnonymousFallbackOnAuthFailure()}
     * Given Scenario: the dedicated servlet-only opt-in method is called.
     * Expected Result: no exception, builder returned for chaining.
     */
    @Test
    public void dedicatedMethodEnablesServletFallback() {
        final WebResource.InitBuilder builder = builder();
        assertSame(builder, builder.servletAnonymousFallbackOnAuthFailure());
    }
}
