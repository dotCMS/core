package com.dotcms.rest.api.v1.experiments;

import com.dotcms.experiments.business.ConfigExperimentUtil;
import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.business.ExperimentsAPI.Health;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.analytics.event.EventAnalyticsProxyHelper;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.liferay.portal.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import com.dotcms.security.apps.Secret;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExperimentsResource} gate behavior.
 *
 * <p>Covers two categories:
 * <ul>
 *   <li>Happy-path regression guards: when {@code FEATURE_FLAG_EXPERIMENTS=true} and the
 *       Analytics App is configured, all lifecycle endpoints — start, end, cancel, archive —
 *       must proceed normally without a {@code 403} or {@code 503} gate response.
 *   <li>Limited-mode gates: when {@code FEATURE_FLAG_EXPERIMENTS=false}, start is restricted
 *       to a single immediate experiment within the 10-day cap, and end/cancel/archive return
 *       {@code 403 FEATURE_DISABLED}.
 * </ul>
 *
 * @author dotCMS
 * @since Oct 2026
 */
public class ExperimentsResourceTest {

    private ExperimentsAPI experimentsAPI;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private User user;
    private Experiment experiment;
    private MockedStatic<com.dotmarketing.business.APILocator> apiLocatorMock;

    @BeforeEach
    void setUp() {
        experimentsAPI = mock(ExperimentsAPI.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        user = mock(User.class);
        experiment = mock(Experiment.class);
        ConfigExperimentUtil.INSTANCE.setExperimentEnabled(false);
        // The health tests route through caemHealthCheck (not legacyHealthCheck) to avoid
        // AnalyticsHelper.get() which requires a live CDI container. Reset to false after each.
        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(true);
        // ExperimentLimitedModeGate.evaluate() calls APILocator.getExperimentsAPI() internally
        apiLocatorMock = mockStatic(com.dotmarketing.business.APILocator.class);
        apiLocatorMock.when(com.dotmarketing.business.APILocator::getExperimentsAPI)
                .thenReturn(experimentsAPI);
    }

    @AfterEach
    void tearDown() {
        apiLocatorMock.close();
        ConfigExperimentUtil.INSTANCE.setExperimentEnabled(false);
        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(false);
    }

    /**
     * Method to test: {@link ExperimentsResource#start(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: {@code FEATURE_FLAG_EXPERIMENTS=true} and the Analytics App is configured.
     * Expected result: The call reaches {@link ExperimentsAPI#start} — no gate response (403/503)
     *   short-circuits the flow.
     */
    @Test
    void start_flagEnabledAndAppConfigured_proceedsToApi() throws Exception {
        ConfigExperimentUtil.INSTANCE.setExperimentEnabled(true);
        when(experimentsAPI.start("exp-id-1", user)).thenReturn(experiment);

        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<WebAPILocator> webApiMock = mockStatic(WebAPILocator.class)) {

            final Host host = mock(Host.class);
            stubSiteResolution(webApiMock, host);
            utilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(host)).thenReturn(true);

            final Response result = withAuthenticatedUserRaw(
                    resource -> resource.start(request, response, "exp-id-1"));

            assertEquals(200, result.getStatus(), "flag=true + App configured must return 200, not a gate response");
            verify(experimentsAPI).start("exp-id-1", user);
        }
    }

    /**
     * Method to test: {@link ExperimentsResource#end(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: {@code FEATURE_FLAG_EXPERIMENTS=true}.
     * Expected result: The call reaches {@link ExperimentsAPI#end} — no gate response short-circuits.
     */
    @Test
    void end_flagEnabled_proceedsToApi() throws Exception {
        ConfigExperimentUtil.INSTANCE.setExperimentEnabled(true);
        when(experimentsAPI.end("exp-id-1", user)).thenReturn(experiment);

        final Response result = withAuthenticatedUserRaw(resource -> resource.end(request, response, "exp-id-1"));

        assertEquals(200, result.getStatus(), "flag=true must return 200, not a gate response");
        verify(experimentsAPI).end("exp-id-1", user);
    }

    /**
     * Method to test: {@link ExperimentsResource#cancel(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: {@code FEATURE_FLAG_EXPERIMENTS=true}.
     * Expected result: The call reaches {@link ExperimentsAPI#cancel} — no gate response short-circuits.
     */
    @Test
    void cancel_flagEnabled_proceedsToApi() throws Exception {
        ConfigExperimentUtil.INSTANCE.setExperimentEnabled(true);
        when(experimentsAPI.cancel("exp-id-1", user)).thenReturn(experiment);

        final Response result = withAuthenticatedUserRaw(resource -> resource.cancel(request, response, "exp-id-1"));

        assertEquals(200, result.getStatus(), "flag=true must return 200, not a gate response");
        verify(experimentsAPI).cancel("exp-id-1", user);
    }

    /**
     * Method to test: {@link ExperimentsResource#archive(HttpServletRequest, HttpServletResponse, String)}
     * Given scenario: {@code FEATURE_FLAG_EXPERIMENTS=true}.
     * Expected result: The call reaches {@link ExperimentsAPI#archive} — no gate response short-circuits.
     */
    @Test
    void archive_flagEnabled_proceedsToApi() throws Exception {
        ConfigExperimentUtil.INSTANCE.setExperimentEnabled(true);
        when(experimentsAPI.archive("exp-id-1", user)).thenReturn(experiment);

        final Response result = withAuthenticatedUserRaw(resource -> resource.archive(request, response, "exp-id-1"));

        assertEquals(200, result.getStatus(), "flag=true must return 200, not a gate response");
        verify(experimentsAPI).archive("exp-id-1", user);
    }

    /**
     * Method to test: {@link ExperimentsResource#start}
     * Given scenario: flag=false, App configured, free slot available, immediate start (no
     *   future startDate), duration within 10-day cap.
     * Expected result: The call reaches {@link ExperimentsAPI#start} — limited-mode slot is free
     *   and duration constraint satisfied, so no gate fires.
     */
    @Test
    void start_flagFalse_slotFree_immediateStart_within10days_proceedsToApi() throws Exception {
        // flag=false (set in setUp); slot free; experiment has no startDate (immediate)
        final Scheduling scheduling = mock(Scheduling.class);
        when(scheduling.startDate()).thenReturn(Optional.empty());
        when(scheduling.endDate()).thenReturn(Optional.of(Instant.now().plus(9, ChronoUnit.DAYS)));
        when(experiment.scheduling()).thenReturn(Optional.of(scheduling));
        when(experimentsAPI.find("exp-id", user)).thenReturn(Optional.of(experiment));
        when(experimentsAPI.isFreeSlotUsed()).thenReturn(false);
        when(experimentsAPI.start("exp-id", user)).thenReturn(experiment);

        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<WebAPILocator> webApiMock = mockStatic(WebAPILocator.class)) {

            final Host host = mock(Host.class);
            stubSiteResolution(webApiMock, host);
            utilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(host)).thenReturn(true);

            final Response result = withAuthenticatedUserRaw(resource -> resource.start(request, response, "exp-id"));
            assertEquals(200, result.getStatus(), "Slot free + immediate + within cap must return 200 — no gate");
            verify(experimentsAPI).start("exp-id", user);
        }
    }

    /**
     * Method to test: {@link ExperimentsResource#start}
     * Given scenario: flag=false, App configured, free slot available, immediate start, but
     *   duration exceeds 10 days.
     * Expected result: {@code 400 Bad Request} — the 10-day cap is exceeded.
     */
    @Test
    void start_flagFalse_slotFree_immediateStart_over10days_returns400() throws Exception {
        final Scheduling scheduling = mock(Scheduling.class);
        when(scheduling.startDate()).thenReturn(Optional.empty());
        when(scheduling.endDate()).thenReturn(Optional.of(Instant.now().plus(15, ChronoUnit.DAYS)));
        when(experiment.scheduling()).thenReturn(Optional.of(scheduling));
        when(experimentsAPI.find("exp-id", user)).thenReturn(Optional.of(experiment));
        when(experimentsAPI.isFreeSlotUsed()).thenReturn(false);

        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<WebAPILocator> webApiMock = mockStatic(WebAPILocator.class)) {

            final Host host = mock(Host.class);
            stubSiteResolution(webApiMock, host);
            utilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(host)).thenReturn(true);

            final Response result = withAuthenticatedUserRaw(
                    resource -> resource.start(request, response, "exp-id"));
            assertEquals(400, result.getStatus(),
                    "Duration > 10 days in limited mode must return 400");
            verify(experimentsAPI, never()).start(any(), any());
        }
    }

    /**
     * Method to test: {@link ExperimentsResource#start}
     * Given scenario: flag=false, App configured, free slot already occupied.
     * Expected result: {@code 403} with {@code errorCode="FEATURE_DISABLED"}.
     */
    @Test
    void start_flagFalse_slotUsed_returns403() throws Exception {
        final Scheduling scheduling = mock(Scheduling.class);
        when(scheduling.startDate()).thenReturn(Optional.empty());
        when(scheduling.endDate()).thenReturn(Optional.of(Instant.now().plus(9, ChronoUnit.DAYS)));
        when(experiment.scheduling()).thenReturn(Optional.of(scheduling));
        when(experimentsAPI.find("exp-id", user)).thenReturn(Optional.of(experiment));
        when(experimentsAPI.isFreeSlotUsed()).thenReturn(true);

        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<WebAPILocator> webApiMock = mockStatic(WebAPILocator.class)) {

            final Host host = mock(Host.class);
            stubSiteResolution(webApiMock, host);
            utilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(host)).thenReturn(true);

            final Response result = withAuthenticatedUserRaw(
                    resource -> resource.start(request, response, "exp-id"));
            assertEquals(403, result.getStatus(), "Slot occupied in limited mode must return 403");
            assertFeatureDisabledErrorCode(result);
        }
    }

    /**
     * Method to test: {@link ExperimentsResource#start}
     * Given scenario: flag=false, App configured, start has a future startDate (scheduled start).
     * Expected result: {@code 403} with {@code errorCode="FEATURE_DISABLED"} — scheduling is not
     *   allowed in limited mode.
     */
    @Test
    void start_flagFalse_futureStartDate_returns403() throws Exception {
        final Scheduling scheduling = mock(Scheduling.class);
        when(scheduling.startDate()).thenReturn(Optional.of(Instant.now().plusSeconds(3600)));
        when(experiment.scheduling()).thenReturn(Optional.of(scheduling));
        when(experimentsAPI.find("exp-id", user)).thenReturn(Optional.of(experiment));

        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<WebAPILocator> webApiMock = mockStatic(WebAPILocator.class)) {

            final Host host = mock(Host.class);
            stubSiteResolution(webApiMock, host);
            utilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(host)).thenReturn(true);

            final Response result = withAuthenticatedUserRaw(
                    resource -> resource.start(request, response, "exp-id"));
            assertEquals(403, result.getStatus(),
                    "Future startDate (scheduled start) in limited mode must return 403");
            assertFeatureDisabledErrorCode(result);
        }
    }

    /**
     * Method to test: {@link ExperimentsResource#end}
     * Given scenario: flag=false (App configuration irrelevant for end).
     * Expected result: {@code 403} with {@code errorCode="FEATURE_DISABLED"}.
     */
    @Test
    void end_flagFalse_returns403() throws Exception {
        final Response result = withAuthenticatedUserRaw(
                resource -> resource.end(request, response, "exp-id"));
        assertEquals(403, result.getStatus(), "flag=false must gate end() with 403");
        assertFeatureDisabledErrorCode(result);
    }

    /**
     * Method to test: {@link ExperimentsResource#cancel}
     * Given scenario: flag=false.
     * Expected result: {@code 403} with {@code errorCode="FEATURE_DISABLED"}.
     */
    @Test
    void cancel_flagFalse_returns403() throws Exception {
        final Response result = withAuthenticatedUserRaw(
                resource -> resource.cancel(request, response, "exp-id"));
        assertEquals(403, result.getStatus(), "flag=false must gate cancel() with 403");
        assertFeatureDisabledErrorCode(result);
    }

    /**
     * Method to test: {@link ExperimentsResource#archive}
     * Given scenario: flag=false.
     * Expected result: {@code 403} with {@code errorCode="FEATURE_DISABLED"}.
     */
    @Test
    void archive_flagFalse_returns403() throws Exception {
        final Response result = withAuthenticatedUserRaw(
                resource -> resource.archive(request, response, "exp-id"));
        assertEquals(403, result.getStatus(), "flag=false must gate archive() with 403");
        assertFeatureDisabledErrorCode(result);
    }

    /**
     * Method to test: {@link ExperimentsResource#healthcheck}
     * Given scenario: flag=true, App configured → backend reachable.
     * Expected result: {@code { health: OK, tier: "full" }} — no freeExperimentUsed, no warning.
     */
    @Test
    void healthcheck_flagTrue_appConfigured_returnsTierFull() throws Exception {
        ConfigExperimentUtil.INSTANCE.setExperimentEnabled(true);
        // Pre-create mocks before entering mockStatic context to avoid Mockito state conflicts
        final Map<String, Secret> configuredSecrets = mockAppConfiguredSecrets();

        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<EventAnalyticsProxyHelper> proxyMock = mockStatic(EventAnalyticsProxyHelper.class);
             MockedStatic<WebAPILocator> webApiMock = mockStatic(WebAPILocator.class)) {

            final Host host = mock(Host.class);
            stubSiteResolution(webApiMock, host);

            utilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(host)).thenReturn(true);
            utilMock.when(() -> ContentAnalyticsUtil.resolveAnalyticsHealth(host)).thenReturn(Health.OK);
            utilMock.when(() -> ContentAnalyticsUtil.getAppSecrets(host)).thenReturn(configuredSecrets);
            proxyMock.when(EventAnalyticsProxyHelper::healthCheck).thenReturn(true);

            final ResponseEntityView<ExperimentsHealthView> result =
                            withAuthenticatedUser(resource -> resource.healthcheck(request, response));

            final ExperimentsHealthView view = result.getEntity();
            assertEquals(Health.OK, view.health());
            assertEquals(ExperimentsHealthView.Tier.FULL, view.tier());
            assertNull(view.freeExperimentUsed(), "freeExperimentUsed must be null when tier=full");
            assertNull(view.warning(), "warning must be absent when App is configured");
        }
    }

    /**
     * Method to test: {@link ExperimentsResource#healthcheck}
     * Given scenario: flag=false, App configured, free slot available.
     * Expected result: {@code { health: OK, tier: "limited", freeExperimentUsed: false }}.
     */
    @Test
    void healthcheck_flagFalse_appConfigured_slotFree_returnsTierLimited() throws Exception {
        when(experimentsAPI.isFreeSlotUsed()).thenReturn(false);
        final Map<String, Secret> configuredSecrets = mockAppConfiguredSecrets();

        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<EventAnalyticsProxyHelper> proxyMock = mockStatic(EventAnalyticsProxyHelper.class);
             MockedStatic<WebAPILocator> webApiMock = mockStatic(WebAPILocator.class)) {

            final Host host = mock(Host.class);
            stubSiteResolution(webApiMock, host);

            utilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(host)).thenReturn(true);
            utilMock.when(() -> ContentAnalyticsUtil.resolveAnalyticsHealth(host)).thenReturn(Health.OK);
            utilMock.when(() -> ContentAnalyticsUtil.getAppSecrets(host)).thenReturn(configuredSecrets);
            proxyMock.when(EventAnalyticsProxyHelper::healthCheck).thenReturn(true);

            final ResponseEntityView<ExperimentsHealthView> result =
                            withAuthenticatedUser(resource -> resource.healthcheck(request, response));

            final ExperimentsHealthView view = result.getEntity();
            assertEquals(Health.OK, view.health());
            assertEquals(ExperimentsHealthView.Tier.LIMITED, view.tier());
            assertEquals(Boolean.FALSE, view.freeExperimentUsed());
            assertNull(view.warning());
        }
    }

    /**
     * Method to test: {@link ExperimentsResource#healthcheck}
     * Given scenario: flag=false, App configured, free slot occupied.
     * Expected result: {@code { health: OK, tier: "limited", freeExperimentUsed: true }}.
     */
    @Test
    void healthcheck_flagFalse_appConfigured_slotUsed_returnsSlotUsedTrue() throws Exception {
        when(experimentsAPI.isFreeSlotUsed()).thenReturn(true);
        final Map<String, Secret> configuredSecrets = mockAppConfiguredSecrets();

        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<EventAnalyticsProxyHelper> proxyMock = mockStatic(EventAnalyticsProxyHelper.class);
             MockedStatic<WebAPILocator> webApiMock = mockStatic(WebAPILocator.class)) {

            final Host host = mock(Host.class);
            stubSiteResolution(webApiMock, host);

            utilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(host)).thenReturn(true);
            utilMock.when(() -> ContentAnalyticsUtil.resolveAnalyticsHealth(host)).thenReturn(Health.OK);
            utilMock.when(() -> ContentAnalyticsUtil.getAppSecrets(host)).thenReturn(configuredSecrets);
            proxyMock.when(EventAnalyticsProxyHelper::healthCheck).thenReturn(true);

            @SuppressWarnings("unchecked")
            final ResponseEntityView<ExperimentsHealthView> result =
                            withAuthenticatedUser(resource -> resource.healthcheck(request, response));

            final ExperimentsHealthView view = result.getEntity();
            assertEquals(ExperimentsHealthView.Tier.LIMITED, view.tier());
            assertEquals(Boolean.TRUE, view.freeExperimentUsed());
        }
    }

    /**
     * Method to test: {@link ExperimentsResource#healthcheck}
     * Given scenario: flag=true, App NOT configured.
     * Expected result: {@code { health: NOT_CONFIGURED, tier: "full", warning: "ANALYTICS_DISABLED" }}
     *   — no freeExperimentUsed (tier=full).
     */
    @Test
    void healthcheck_flagTrue_appNotConfigured_returnsWarning() throws Exception {
        ConfigExperimentUtil.INSTANCE.setExperimentEnabled(true);

        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<WebAPILocator> webApiMock = mockStatic(WebAPILocator.class)) {

            final Host host = mock(Host.class);
            stubSiteResolution(webApiMock, host);

            utilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(host)).thenReturn(false);
            utilMock.when(() -> ContentAnalyticsUtil.resolveAnalyticsHealth(host)).thenReturn(Health.NOT_CONFIGURED);
            // App NOT configured — caemHealthCheck returns NOT_CONFIGURED after checking empty secrets
            utilMock.when(() -> ContentAnalyticsUtil.getAppSecrets(host)).thenReturn(emptyMap());

            @SuppressWarnings("unchecked")
            final ResponseEntityView<ExperimentsHealthView> result =
                            withAuthenticatedUser(resource -> resource.healthcheck(request, response));

            final ExperimentsHealthView view = result.getEntity();
            assertEquals(Health.NOT_CONFIGURED, view.health());
            assertEquals(ExperimentsHealthView.Tier.FULL, view.tier());
            assertNull(view.freeExperimentUsed());
            assertEquals(ExperimentsHealthView.Warning.ANALYTICS_DISABLED, view.warning());
        }
    }

    /**
     * Method to test: {@link ExperimentsResource#healthcheck}
     * Given scenario: flag=false, App NOT configured.
     * Expected result: {@code { health: NOT_CONFIGURED, tier: "limited", freeExperimentUsed: ...,
     *   warning: "ANALYTICS_DISABLED" }}.
     */
    @Test
    void healthcheck_flagFalse_appNotConfigured_returnsWarningAndTierLimited() throws Exception {
        when(experimentsAPI.isFreeSlotUsed()).thenReturn(false);

        try (MockedStatic<ContentAnalyticsUtil> utilMock = mockStatic(ContentAnalyticsUtil.class);
             MockedStatic<WebAPILocator> webApiMock = mockStatic(WebAPILocator.class)) {

            final Host host = mock(Host.class);
            stubSiteResolution(webApiMock, host);

            utilMock.when(() -> ContentAnalyticsUtil.isAppConfigured(host)).thenReturn(false);
            utilMock.when(() -> ContentAnalyticsUtil.resolveAnalyticsHealth(host)).thenReturn(Health.NOT_CONFIGURED);
            utilMock.when(() -> ContentAnalyticsUtil.getAppSecrets(host)).thenReturn(emptyMap());

            @SuppressWarnings("unchecked")
            final ResponseEntityView<ExperimentsHealthView> result =
                            withAuthenticatedUser(resource -> resource.healthcheck(request, response));

            final ExperimentsHealthView view = result.getEntity();
            assertEquals(ExperimentsHealthView.Tier.LIMITED, view.tier());
            assertEquals(ExperimentsHealthView.Warning.ANALYTICS_DISABLED, view.warning());
            assertNotNull(view.freeExperimentUsed(),
                    "freeExperimentUsed must be present when tier=limited");
        }
    }

    @FunctionalInterface
    private interface ResourceAction<T> {
        T invoke(ExperimentsResource resource) throws Exception;
    }

    /**
     * Wraps a resource action with mocked auth and returns the raw result from the resource method.
     * Cast to the expected type at the call site with {@code @SuppressWarnings("unchecked")}.
     * For gate-rejection assertions (status code checks) use {@link #withAuthenticatedUserRaw}.
     */
    private <T> T withAuthenticatedUser(final ResourceAction<T> action) throws Exception {
        final InitDataObject initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(user);

        try (MockedConstruction<WebResource.InitBuilder> ignored = mockConstruction(
                WebResource.InitBuilder.class,
                (mock, ctx) -> {
                    when(mock.requestAndResponse(any(), any())).thenReturn(mock);
                    when(mock.requiredBackendUser(anyBoolean())).thenReturn(mock);
                    when(mock.rejectWhenNoUser(anyBoolean())).thenReturn(mock);
                    when(mock.init()).thenReturn(initData);
                })) {
            return action.invoke(new ExperimentsResource(mock(WebResource.class), experimentsAPI));
        }
    }

    /**
     * Wraps a resource action with mocked auth. For methods returning {@link Response} directly
     * (start/end/cancel/archive after gating), the Response passes through. For other return types,
     * the result is wrapped in a 200 Response so callers can always assert status codes.
     */
    private Response withAuthenticatedUserRaw(final ResourceAction<Response> action) throws Exception {
        final InitDataObject initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(user);

        try (MockedConstruction<WebResource.InitBuilder> ignored = mockConstruction(
                WebResource.InitBuilder.class,
                (mock, ctx) -> {
                    when(mock.requestAndResponse(any(), any())).thenReturn(mock);
                    when(mock.requiredBackendUser(anyBoolean())).thenReturn(mock);
                    when(mock.rejectWhenNoUser(anyBoolean())).thenReturn(mock);
                    when(mock.init()).thenReturn(initData);
                })) {
            final ExperimentsResource resource =
                    new ExperimentsResource(mock(WebResource.class), experimentsAPI);
            final Object result = action.invoke(resource);
            return (result instanceof Response r) ? r : Response.ok(result).build();
        }
    }

    /**
     * Stubs site resolution for resource methods that call
     * {@code WebAPILocator.getHostWebAPI().getCurrentHost(request)} (single-arg overload).
     */
    private void stubSiteResolution(final MockedStatic<WebAPILocator> webApiMock,
            final Host host) throws Exception {
        final HostWebAPI hostWebAPI = mock(HostWebAPI.class);
        webApiMock.when(WebAPILocator::getHostWebAPI).thenReturn(hostWebAPI);
        // Stub the single-arg getCurrentHost(HttpServletRequest) overload used by healthcheck/start
        doAnswer(inv -> host).when(hostWebAPI)
                .getCurrentHost(any(HttpServletRequest.class));
    }

    /**
     * Returns a mock secrets map with non-blank {@code siteAuth} and {@code bearerToken},
     * representing a site where the Analytics App is fully configured.
     * Must be created before entering a {@code mockStatic} block to avoid Mockito state conflicts.
     */
    private static Map<String, Secret> mockAppConfiguredSecrets() {
        return Map.of(
                ContentAnalyticsUtil.SITE_AUTH_KEY, siteAuthSecret("site-auth"),
                ContentAnalyticsUtil.BEARER_TOKEN_KEY, siteAuthSecret("bearer-token"));
    }

    /** Creates a mock {@link Secret} returning the given value. */
    private static Secret siteAuthSecret(final String value) {
        final Secret s = mock(Secret.class);
        when(s.getString()).thenReturn(value);
        return s;
    }

    /** Asserts the response entity carries {@code errorCode="FEATURE_DISABLED"}. */
    @SuppressWarnings("unchecked")
    private static void assertFeatureDisabledErrorCode(final Response response) {
        final Object entity = response.getEntity();
        if (entity instanceof ResponseEntityView<?> rev && rev.getErrors() instanceof List<?> errors
                && !errors.isEmpty() && errors.get(0) instanceof ErrorEntity err) {
            assertEquals("FEATURE_DISABLED", err.getErrorCode(),
                    "Gate response must carry errorCode=FEATURE_DISABLED");
        }
    }
}