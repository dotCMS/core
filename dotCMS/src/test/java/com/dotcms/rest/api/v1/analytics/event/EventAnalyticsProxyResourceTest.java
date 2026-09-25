package com.dotcms.rest.api.v1.analytics.event;

import com.dotcms.jitsu.validators.SiteAuthValidator;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Persistence Mode gate in
 * {@link EventAnalyticsProxyResource#proxyEventRequest}, per
 * {@code specs/37521-content-analytics-mode/contracts/persistence-mode-gate.md}.
 *
 * <p>{@link SiteAuthValidator} construction is mocked out via {@link org.mockito.Mockito#mockConstruction}
 * because it independently resolves the Site and reads App secrets through {@code APILocator} —
 * a real dotCMS server context this unit test does not have. That collaborator's own behavior is
 * covered elsewhere; here it is treated as already-validated so the gate under test can be
 * exercised in isolation.
 *
 * @author dotCMS
 * @since 2026
 */
public class EventAnalyticsProxyResourceTest {

    private static final String VALID_EVENT_BODY =
            "{\"context\":{\"site_auth\":\"any-token\"},\"events\":[]}";

    /**
     * Method to test: {@link EventAnalyticsProxyResource#proxyEventRequest(HttpServletRequest, HttpServletResponse, AsyncResponse, UriInfo, String)}
     *
     * Given Scenario: The resolved Site's Content Analytics app secrets have
     * {@code persistenceMode=readonly}.
     *
     * Expected Result: {@link EventAnalyticsProxyHelper#proxy} is never invoked, and the async
     * response is resumed directly with a {@code 200} success response carrying a
     * {@link ResponseEntityView} JSON envelope (not an empty body) — a caller that calls
     * {@code response.json()} on every 2xx, not just on error, must not fail to parse it.
     */
    @Test
    public void proxyEventRequest_readOnly_suppressesForwardAndResumesWith200() {
        final Host site = mock(Host.class);
        when(site.getIdentifier()).thenReturn("site-identifier");

        final Map<String, Secret> secrets = new HashMap<>();
        final Secret persistenceMode = mock(Secret.class);
        when(persistenceMode.getString()).thenReturn("readonly");
        secrets.put("persistenceMode", persistenceMode);

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final AsyncResponse asyncResponse = mock(AsyncResponse.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        try (MockedConstruction<SiteAuthValidator> ignoredSiteAuthValidator = mockConstruction(SiteAuthValidator.class);
             MockedStatic<ContentAnalyticsUtil> contentAnalyticsUtilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<EventAnalyticsProxyHelper> proxyHelperMock = mockStatic(EventAnalyticsProxyHelper.class);
             MockedStatic<ResponseUtil> responseUtilMock = mockStatic(ResponseUtil.class)) {

            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.getSiteFromRequest(request))
                    .thenReturn(site);
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.getAppSecrets(site))
                    .thenReturn(secrets);
            // Stubbed defensively so that, if the implementation under test does forward (i.e.
            // the gate isn't in place yet), it fails on the verifyNoInteractions() assertion
            // below rather than on an incidental NPE from an unrelated unstubbed mock deep in
            // the real forwarding path — and runs on this thread rather than the real async
            // thread pool, where Mockito's (thread-scoped) static mocks wouldn't apply anyway.
            proxyHelperMock.when(() -> EventAnalyticsProxyHelper.proxy(any(), any(), any(), any(), any()))
                    .thenReturn(Response.ok().build());
            responseUtilMock.when(() -> ResponseUtil.handleAsyncResponse(any(Supplier.class), eq(asyncResponse)))
                    .thenAnswer(invocation -> {
                        final Supplier<?> supplier = invocation.getArgument(0);
                        supplier.get();
                        return null;
                    });

            new EventAnalyticsProxyResource().proxyEventRequest(
                    request, response, asyncResponse, uriInfo, VALID_EVENT_BODY);

            proxyHelperMock.verifyNoInteractions();

            final ArgumentCaptor<Object> resumedResponse = ArgumentCaptor.forClass(Object.class);
            verify(asyncResponse).resume(resumedResponse.capture());
            assertInstanceOf(Response.class, resumedResponse.getValue());
            final Response response200 = (Response) resumedResponse.getValue();
            assertEquals(200, response200.getStatus());
            assertInstanceOf(ResponseEntityView.class, response200.getEntity(),
                    "Read Only short-circuit must carry a ResponseEntityView JSON envelope, not "
                            + "an empty body -- a caller parsing the response on every 2xx "
                            + "(not just on error) would otherwise fail to parse it");
        }
    }

    /**
     * Method to test: {@link EventAnalyticsProxyResource#proxyEventRequest(HttpServletRequest, HttpServletResponse, AsyncResponse, UriInfo, String)}
     *
     * Given Scenario: The resolved Site's Content Analytics app secrets have
     * {@code persistenceMode=readwrite} (regression coverage for FR-003).
     *
     * Expected Result: {@link EventAnalyticsProxyHelper#proxy} is invoked with the
     * {@code "event/ingest"} upstream path and the resolved Site, exactly as it forwards today.
     */
    @Test
    public void proxyEventRequest_readWrite_stillForwardsToUpstream() {
        final Map<String, Secret> secrets = new HashMap<>();
        final Secret persistenceMode = mock(Secret.class);
        when(persistenceMode.getString()).thenReturn("readwrite");
        secrets.put("persistenceMode", persistenceMode);

        assertForwardsToUpstream(secrets);
    }

    /**
     * Method to test: {@link EventAnalyticsProxyResource#proxyEventRequest(HttpServletRequest, HttpServletResponse, AsyncResponse, UriInfo, String)}
     *
     * Given Scenario: The resolved Site's Content Analytics app secrets do not have a
     * {@code persistenceMode} key at all — the state of an instance that had Content Analytics
     * configured before this field existed (FR-002 / FR-002a).
     *
     * Expected Result: A missing {@code persistenceMode} is treated exactly like
     * {@code readwrite} — {@link EventAnalyticsProxyHelper#proxy} is still invoked, per
     * {@code contracts/persistence-mode-gate.md}.
     */
    @Test
    public void proxyEventRequest_missingPersistenceMode_stillForwardsToUpstream() {
        assertForwardsToUpstream(new HashMap<>());
    }

    /**
     * Shared assertion for the two "should still forward" cases above: stubs
     * {@link ResponseUtil#handleAsyncResponse} to invoke its supplier synchronously — avoiding
     * the real thread pool it would otherwise dispatch to — then verifies
     * {@link EventAnalyticsProxyHelper#proxy} was actually invoked as a result.
     *
     * @param secrets the Content Analytics app secrets to stub {@code getAppSecrets} with.
     */
    private void assertForwardsToUpstream(final Map<String, Secret> secrets) {
        final Host site = mock(Host.class);
        when(site.getIdentifier()).thenReturn("site-identifier");

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final AsyncResponse asyncResponse = mock(AsyncResponse.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        try (MockedConstruction<SiteAuthValidator> ignoredSiteAuthValidator = mockConstruction(SiteAuthValidator.class);
             MockedStatic<ContentAnalyticsUtil> contentAnalyticsUtilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<EventAnalyticsProxyHelper> proxyHelperMock = mockStatic(EventAnalyticsProxyHelper.class);
             MockedStatic<ResponseUtil> responseUtilMock = mockStatic(ResponseUtil.class)) {

            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.getSiteFromRequest(request))
                    .thenReturn(site);
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.getAppSecrets(site))
                    .thenReturn(secrets);
            proxyHelperMock.when(() -> EventAnalyticsProxyHelper.proxy(any(), any(), any(), any(), any()))
                    .thenReturn(Response.ok().build());
            responseUtilMock.when(() -> ResponseUtil.handleAsyncResponse(any(Supplier.class), eq(asyncResponse)))
                    .thenAnswer(invocation -> {
                        final Supplier<?> supplier = invocation.getArgument(0);
                        supplier.get();
                        return null;
                    });

            new EventAnalyticsProxyResource().proxyEventRequest(
                    request, response, asyncResponse, uriInfo, VALID_EVENT_BODY);

            proxyHelperMock.verify(() -> EventAnalyticsProxyHelper.proxy(
                    eq("event/ingest"), eq(uriInfo), any(), any(), eq(site)));
        }
    }
}
