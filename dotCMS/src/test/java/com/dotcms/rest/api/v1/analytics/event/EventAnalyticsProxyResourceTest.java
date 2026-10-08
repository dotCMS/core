package com.dotcms.rest.api.v1.analytics.event;

import com.dotcms.jitsu.validators.SiteAuthValidator;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
            // App is configured — the gate passes through to the persistence-mode check.
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(site))
                    .thenReturn(true);
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
            // App is configured — gate passes through.
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(site))
                    .thenReturn(true);
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

    // --- US1 regression guards: App configured → no 503 before proxy logic ---

    /**
     * Method to test: {@link EventAnalyticsProxyResource#proxyGetRequest}
     * Given scenario: The Analytics App is configured for the resolved site.
     * Expected result: The request reaches {@link EventAnalyticsProxyHelper#proxy} — no {@code 503}
     *   is returned before the proxy call. This is the pre-gate regression baseline.
     */
    @Test
    public void proxyGetRequest_appConfigured_doesNotReturn503BeforeProxy() throws Exception {
        final Host site = mock(Host.class);
        when(site.getIdentifier()).thenReturn("site-id");

        final User user = mock(User.class);
        final InitDataObject initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(user);

        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());

        try (MockedConstruction<WebResource.InitBuilder> ignored = mockConstruction(
                WebResource.InitBuilder.class,
                (mock, ctx) -> {
                    when(mock.requestAndResponse(any(), any())).thenReturn(mock);
                    when(mock.requiredBackendUser(anyBoolean())).thenReturn(mock);
                    when(mock.rejectWhenNoUser(anyBoolean())).thenReturn(mock);
                    when(mock.init()).thenReturn(initData);
                });
             MockedStatic<ContentAnalyticsUtil> contentAnalyticsUtilMock =
                     mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<WebAPILocator> webApiLocatorMock = mockStatic(WebAPILocator.class);
             MockedStatic<EventAnalyticsProxyHelper> proxyHelperMock =
                     mockStatic(EventAnalyticsProxyHelper.class)) {

            // App is configured for this site
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(any(Host.class)))
                    .thenReturn(true);

            final com.dotmarketing.business.web.HostWebAPI hostWebAPI =
                    mock(com.dotmarketing.business.web.HostWebAPI.class);
            webApiLocatorMock.when(WebAPILocator::getHostWebAPI).thenReturn(hostWebAPI);
            // getCurrentHost throws checked exceptions — use thenAnswer to avoid compiler error
            org.mockito.Mockito.doAnswer(inv -> site)
                    .when(hostWebAPI).getCurrentHost(any(), any(User.class));

            proxyHelperMock.when(() -> EventAnalyticsProxyHelper.proxy(
                    anyString(), any(), any(), any(), any()))
                    .thenReturn(Response.ok().build());

            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);

            final Response result = new EventAnalyticsProxyResource(mock(WebResource.class))
                    .proxyGetRequest(request, response, uriInfo, "events/total");

            assertNotEquals(503, result.getStatus(),
                    "App configured — proxyGetRequest must not return 503 before reaching proxy");
        }
    }

    /**
     * Method to test: {@link EventAnalyticsProxyResource#proxyEventRequest}
     * Given scenario: The Analytics App is configured for the resolved site.
     * Expected result: {@link EventAnalyticsProxyHelper#proxy} is invoked — proving the flow was
     *   not short-circuited with a {@code 503} before reaching the forwarding step. This is the
     *   pre-gate regression baseline.
     */
    @Test
    public void proxyEventRequest_appConfigured_reachesProxy() {
        final Host site = mock(Host.class);
        when(site.getIdentifier()).thenReturn("site-id");

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final AsyncResponse asyncResponse = mock(AsyncResponse.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        try (MockedConstruction<SiteAuthValidator> ignoredValidator =
                     mockConstruction(SiteAuthValidator.class);
             MockedStatic<ContentAnalyticsUtil> contentAnalyticsUtilMock =
                     mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<EventAnalyticsProxyHelper> proxyHelperMock =
                     mockStatic(EventAnalyticsProxyHelper.class);
             MockedStatic<ResponseUtil> responseUtilMock = mockStatic(ResponseUtil.class)) {

            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.getSiteFromRequest(request))
                    .thenReturn(site);
            // App is configured — no persistence-mode key → flow proceeds to forward
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.getAppSecrets(site))
                    .thenReturn(new HashMap<>());
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(any(Host.class)))
                    .thenReturn(true);

            proxyHelperMock.when(() -> EventAnalyticsProxyHelper.proxy(any(), any(), any(), any(), any()))
                    .thenReturn(Response.ok().build());
            responseUtilMock.when(() -> ResponseUtil.handleAsyncResponse(any(Supplier.class), eq(asyncResponse)))
                    .thenAnswer(inv -> {
                        ((Supplier<?>) inv.getArgument(0)).get();
                        return null;
                    });

            new EventAnalyticsProxyResource()
                    .proxyEventRequest(request, response, asyncResponse, uriInfo, VALID_EVENT_BODY);

            // Verify proxy was actually invoked — no App-config gate short-circuited the flow
            proxyHelperMock.verify(() -> EventAnalyticsProxyHelper.proxy(
                    eq("event/ingest"), eq(uriInfo), any(), any(), eq(site)));
        }
    }

    // --- US3: App not configured gate (T037 / T038) ---

    /**
     * Method to test: {@link EventAnalyticsProxyResource#proxyGetRequest}
     * Given scenario: The Analytics App is NOT configured for the resolved site.
     * Expected result: {@code 503 Service Unavailable} — the gate fires before reaching the proxy.
     *   This test is RED until the App-config gate is added to {@code proxyGetRequest}.
     */
    @Test
    public void proxyGetRequest_appNotConfigured_returns503() throws Exception {
        final Host site = mock(Host.class);
        when(site.getIdentifier()).thenReturn("site-id");

        final User user = mock(User.class);
        final InitDataObject initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(user);

        final UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());

        try (MockedConstruction<WebResource.InitBuilder> ignored = mockConstruction(
                WebResource.InitBuilder.class,
                (mock, ctx) -> {
                    when(mock.requestAndResponse(any(), any())).thenReturn(mock);
                    when(mock.requiredBackendUser(anyBoolean())).thenReturn(mock);
                    when(mock.rejectWhenNoUser(anyBoolean())).thenReturn(mock);
                    when(mock.init()).thenReturn(initData);
                });
             MockedStatic<ContentAnalyticsUtil> contentAnalyticsUtilMock =
                     mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<WebAPILocator> webApiLocatorMock = mockStatic(WebAPILocator.class);
             MockedStatic<EventAnalyticsProxyHelper> proxyHelperMock =
                     mockStatic(EventAnalyticsProxyHelper.class)) {

            // App is NOT configured
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(any(Host.class)))
                    .thenReturn(false);

            final com.dotmarketing.business.web.HostWebAPI hostWebAPI =
                    mock(com.dotmarketing.business.web.HostWebAPI.class);
            webApiLocatorMock.when(WebAPILocator::getHostWebAPI).thenReturn(hostWebAPI);
            doAnswer(inv -> site).when(hostWebAPI)
                    .getCurrentHost(any(HttpServletRequest.class), any(User.class));

            proxyHelperMock.when(() -> EventAnalyticsProxyHelper.proxy(
                    anyString(), any(), any(), any(), any()))
                    .thenReturn(Response.ok().build());

            final HttpServletRequest request = mock(HttpServletRequest.class);
            final HttpServletResponse response = mock(HttpServletResponse.class);

            final Response result = new EventAnalyticsProxyResource(mock(WebResource.class))
                    .proxyGetRequest(request, response, uriInfo, "events/total");

            assertEquals(503, result.getStatus(),
                    "App not configured — proxyGetRequest must return 503");
        }
    }

    /**
     * Method to test: {@link EventAnalyticsProxyResource#proxyGetRequest}
     * Given scenario: The Analytics App is configured for the resolved site.
     * Expected result: No {@code 503} — the request reaches the proxy.
     * (Regression: already tested in US1; repeated here alongside the 503 case for readability.)
     */
    @Test
    public void proxyGetRequest_appConfigured_noGate() throws Exception {
        // This is the same as the US1 test — verifying no regression alongside the 503 case
        proxyGetRequest_appConfigured_doesNotReturn503BeforeProxy();
    }

    /**
     * Method to test: {@link EventAnalyticsProxyResource#proxyEventRequest}
     * Given scenario: The Analytics App is NOT configured. The request body is missing
     *   {@code site_auth}. The App check fires BEFORE {@code site_auth} parsing.
     * Expected result: {@code 503 Service Unavailable} — not {@code 400 Bad Request}.
     *   This test is RED until the App-config check is moved before {@code site_auth} validation.
     */
    @Test
    public void proxyEventRequest_appNotConfigured_siteAuthMissing_returns503NotBadRequest() {
        final Host site = mock(Host.class);
        when(site.getIdentifier()).thenReturn("site-id");

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final AsyncResponse asyncResponse = mock(AsyncResponse.class);
        final UriInfo uriInfo = mock(UriInfo.class);
        // Body with no site_auth — currently causes 400; after gate added, causes 503
        final String bodyNoSiteAuth = "{\"context\":{},\"events\":[]}";

        try (MockedStatic<ContentAnalyticsUtil> contentAnalyticsUtilMock =
                     mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<EventAnalyticsProxyHelper> proxyHelperMock =
                     mockStatic(EventAnalyticsProxyHelper.class)) {

            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.getSiteFromRequest(request))
                    .thenReturn(site);
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(site))
                    .thenReturn(false);
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.getAppSecrets(site))
                    .thenReturn(new HashMap<>());

            new EventAnalyticsProxyResource()
                    .proxyEventRequest(request, response, asyncResponse, uriInfo, bodyNoSiteAuth);

            final ArgumentCaptor<Object> resumed = ArgumentCaptor.forClass(Object.class);
            verify(asyncResponse).resume(resumed.capture());
            assertInstanceOf(Response.class, resumed.getValue());
            assertEquals(503, ((Response) resumed.getValue()).getStatus(),
                    "App not configured must return 503 regardless of site_auth presence");
        }
    }

    /**
     * Method to test: {@link EventAnalyticsProxyResource#proxyEventRequest}
     * Given scenario: App is NOT configured. Body has a valid {@code site_auth} field.
     * Expected result: {@code 503} — App check fires before site_auth validation.
     */
    @Test
    public void proxyEventRequest_appNotConfigured_validSiteAuth_returns503() {
        final Host site = mock(Host.class);
        when(site.getIdentifier()).thenReturn("site-id");

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final AsyncResponse asyncResponse = mock(AsyncResponse.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        try (MockedConstruction<SiteAuthValidator> ignoredValidator =
                     mockConstruction(SiteAuthValidator.class);
             MockedStatic<ContentAnalyticsUtil> contentAnalyticsUtilMock =
                     mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<EventAnalyticsProxyHelper> proxyHelperMock =
                     mockStatic(EventAnalyticsProxyHelper.class)) {

            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.getSiteFromRequest(request))
                    .thenReturn(site);
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(site))
                    .thenReturn(false);
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.getAppSecrets(site))
                    .thenReturn(new HashMap<>());

            new EventAnalyticsProxyResource()
                    .proxyEventRequest(request, response, asyncResponse, uriInfo, VALID_EVENT_BODY);

            final ArgumentCaptor<Object> resumed = ArgumentCaptor.forClass(Object.class);
            verify(asyncResponse).resume(resumed.capture());
            assertInstanceOf(Response.class, resumed.getValue());
            assertEquals(503, ((Response) resumed.getValue()).getStatus(),
                    "App not configured must return 503 even when site_auth is valid");
        }
    }

    /**
     * Method to test: {@link EventAnalyticsProxyResource#proxyEventRequest}
     * Given scenario: App IS configured but site_auth is missing/invalid.
     * Expected result: {@code 400 Bad Request} — App check passes, then site_auth gate fires.
     */
    @Test
    public void proxyEventRequest_appConfigured_siteAuthMissing_returns400() {
        final Host site = mock(Host.class);
        when(site.getIdentifier()).thenReturn("site-id");

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final AsyncResponse asyncResponse = mock(AsyncResponse.class);
        final UriInfo uriInfo = mock(UriInfo.class);
        final String bodyNoSiteAuth = "{\"context\":{},\"events\":[]}";

        try (MockedStatic<ContentAnalyticsUtil> contentAnalyticsUtilMock =
                     mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<EventAnalyticsProxyHelper> proxyHelperMock =
                     mockStatic(EventAnalyticsProxyHelper.class)) {

            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.getSiteFromRequest(request))
                    .thenReturn(site);
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(site))
                    .thenReturn(true);
            contentAnalyticsUtilMock.when(() -> ContentAnalyticsUtil.getAppSecrets(site))
                    .thenReturn(new HashMap<>());

            new EventAnalyticsProxyResource()
                    .proxyEventRequest(request, response, asyncResponse, uriInfo, bodyNoSiteAuth);

            final ArgumentCaptor<Object> resumed = ArgumentCaptor.forClass(Object.class);
            verify(asyncResponse).resume(resumed.capture());
            assertInstanceOf(Response.class, resumed.getValue());
            assertEquals(400, ((Response) resumed.getValue()).getStatus(),
                    "App configured + missing site_auth must return 400");
        }
    }
}
