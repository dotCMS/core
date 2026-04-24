package com.dotcms.auth.dotAuth.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.auth.dotAuth.DotAuthConstants;
import com.dotcms.auth.dotAuth.rest.handler.OAuthProtocolHandler;
import com.dotcms.auth.dotAuth.rest.handler.ProtocolHandler;
import com.dotcms.auth.dotAuth.rest.handler.SamlProtocolHandler;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.saml.DotSamlProxyFactory;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Unit tests for {@link DotAuthResource} covering the protocol-dispatch surface
 * added in phase-3: SAML detection in {@code listSites}, inheritance from a
 * SAML-configured SYSTEM_HOST, per-protocol reads in {@code getConfig},
 * mutual-exclusion on {@code saveConfig}, and dual-key delete on
 * {@code clearConfig}.
 */
public class DotAuthResourceTest {

    private static final String OAUTH_KEY = DotAuthConstants.APP_KEY;
    private static final String SAML_KEY  = DotSamlProxyFactory.SAML_APP_CONFIG_KEY;

    private static final String SYSTEM_HOST_ID = "SYSTEM_HOST";
    private static final String SITE_ID        = "site-1";
    private static final String SITE_NAME      = "a.example";

    private WebResource webResource;
    private AppsAPI appsAPI;
    private HostAPI hostAPI;
    private Host systemHost;
    private Host site;
    private User user;
    private HttpServletRequest request;
    private HttpServletResponse response;

    private DotAuthResource resource;

    @BeforeEach
    public void setUp() throws Exception {
        webResource = mock(WebResource.class);
        appsAPI     = mock(AppsAPI.class);
        hostAPI     = mock(HostAPI.class);
        user        = new User();
        request     = mock(HttpServletRequest.class);
        response    = mock(HttpServletResponse.class);

        // InitBuilder#init() delegates to webResource.init(InitBuilder).
        final InitDataObject initDataObject = mock(InitDataObject.class);
        when(initDataObject.getUser()).thenReturn(user);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initDataObject);

        systemHost = mock(Host.class);
        when(systemHost.getIdentifier()).thenReturn(SYSTEM_HOST_ID);
        when(systemHost.isSystemHost()).thenReturn(true);
        when(systemHost.isArchived()).thenReturn(false);

        site = mock(Host.class);
        when(site.getIdentifier()).thenReturn(SITE_ID);
        when(site.getHostname()).thenReturn(SITE_NAME);
        when(site.isSystemHost()).thenReturn(false);
        when(site.isArchived()).thenReturn(false);

        final Map<DotAuthProtocol, ProtocolHandler> handlers = new EnumMap<>(DotAuthProtocol.class);
        handlers.put(DotAuthProtocol.OAUTH, new OAuthProtocolHandler());
        handlers.put(DotAuthProtocol.SAML, new SamlProtocolHandler());

        resource = new DotAuthResource(webResource, appsAPI, handlers);
    }

    // --- listSites ---------------------------------------------------------

    @Test
    public void listSites_marks_site_with_dotAuth_row_as_OAUTH() throws Exception {
        final Map<String, Set<String>> appsByHost = Map.of(
                SYSTEM_HOST_ID.toLowerCase(), Set.of(),
                SITE_ID.toLowerCase(), Set.of(OAUTH_KEY.toLowerCase()));
        when(appsAPI.appKeysByHost()).thenReturn(appsByHost);

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::systemHost).thenReturn(systemHost);
            apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
            when(hostAPI.findAll(user, false)).thenReturn(List.of(site));

            final Response rsp = resource.listSites(request, response);
            final DotAuthSitesView entity = (DotAuthSitesView) ((ResponseEntityView<?>) rsp.getEntity()).getEntity();

            assertFalse(entity.getSystem().isConfigured());
            assertNull(entity.getSystem().getProtocol());
            assertEquals(1, entity.getSites().size());
            final DotAuthSitesView.SiteRowView row = entity.getSites().get(0);
            assertEquals(DotAuthSiteStatus.SITE_OVERRIDE, row.getStatus());
            assertEquals(DotAuthProtocol.OAUTH, row.getProtocol());
        }
    }

    @Test
    public void listSites_marks_site_with_dotsaml_config_row_as_SAML() throws Exception {
        final Map<String, Set<String>> appsByHost = Map.of(
                SYSTEM_HOST_ID.toLowerCase(), Set.of(),
                SITE_ID.toLowerCase(), Set.of(SAML_KEY.toLowerCase()));
        when(appsAPI.appKeysByHost()).thenReturn(appsByHost);

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::systemHost).thenReturn(systemHost);
            apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
            when(hostAPI.findAll(user, false)).thenReturn(List.of(site));

            final Response rsp = resource.listSites(request, response);
            final DotAuthSitesView entity = (DotAuthSitesView) ((ResponseEntityView<?>) rsp.getEntity()).getEntity();

            final DotAuthSitesView.SiteRowView row = entity.getSites().get(0);
            assertEquals(DotAuthSiteStatus.SITE_OVERRIDE, row.getStatus());
            assertEquals(DotAuthProtocol.SAML, row.getProtocol());
        }
    }

    @Test
    public void listSites_marks_host_inherited_from_SAML_system_default() throws Exception {
        final Map<String, Set<String>> appsByHost = Map.of(
                SYSTEM_HOST_ID.toLowerCase(), Set.of(SAML_KEY.toLowerCase()),
                SITE_ID.toLowerCase(), Set.of());
        when(appsAPI.appKeysByHost()).thenReturn(appsByHost);

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::systemHost).thenReturn(systemHost);
            apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
            when(hostAPI.findAll(user, false)).thenReturn(List.of(site));

            final Response rsp = resource.listSites(request, response);
            final DotAuthSitesView entity = (DotAuthSitesView) ((ResponseEntityView<?>) rsp.getEntity()).getEntity();

            assertTrue(entity.getSystem().isConfigured());
            assertEquals(DotAuthProtocol.SAML, entity.getSystem().getProtocol());
            final DotAuthSitesView.SiteRowView row = entity.getSites().get(0);
            assertEquals(DotAuthSiteStatus.INHERITED, row.getStatus());
            assertEquals(DotAuthProtocol.SAML, row.getProtocol());
        }
    }

    @Test
    public void listSites_marks_host_not_configured_when_no_keys_anywhere() throws Exception {
        when(appsAPI.appKeysByHost()).thenReturn(Map.of());

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::systemHost).thenReturn(systemHost);
            apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
            when(hostAPI.findAll(user, false)).thenReturn(List.of(site));

            final Response rsp = resource.listSites(request, response);
            final DotAuthSitesView entity = (DotAuthSitesView) ((ResponseEntityView<?>) rsp.getEntity()).getEntity();

            final DotAuthSitesView.SiteRowView row = entity.getSites().get(0);
            assertEquals(DotAuthSiteStatus.NOT_CONFIGURED, row.getStatus());
            assertNull(row.getProtocol());
        }
    }

    // --- getConfig ---------------------------------------------------------

    @Test
    public void getConfig_returns_SAML_values_for_SAML_configured_host() throws Exception {
        final AppSecrets samlSecrets = AppSecrets.builder()
                .withKey(SAML_KEY)
                .withSecret("idpName", "Okta")
                .withHiddenSecret("privateKey", "stored-PEM")
                .build();

        when(appsAPI.getSecrets(eq(OAUTH_KEY), anyBoolean(), eq(site), eq(user)))
                .thenReturn(Optional.empty());
        when(appsAPI.getSecrets(eq(SAML_KEY), anyBoolean(), eq(site), eq(user)))
                .thenReturn(Optional.of(samlSecrets));

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::systemHost).thenReturn(systemHost);
            apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
            when(hostAPI.find(SITE_ID, user, false)).thenReturn(site);

            final Response rsp = resource.getConfig(request, response, SITE_ID);
            final DotAuthConfigView entity =
                    (DotAuthConfigView) ((ResponseEntityView<?>) rsp.getEntity()).getEntity();

            assertEquals(DotAuthProtocol.SAML, entity.getProtocol());
            assertTrue(entity.isConfigured());
            assertFalse(entity.isInherited());
            assertEquals("Okta", entity.getValues().get("idpName"));
            assertEquals(DotAuthConstants.HIDDEN_SECRET_MASK, entity.getValues().get("privateKey"));
        }
    }

    @Test
    public void getConfig_returns_OAUTH_inherited_when_only_system_has_dotAuth() throws Exception {
        final AppSecrets oauthSecrets = AppSecrets.builder()
                .withKey(OAUTH_KEY)
                .withSecret("clientId", "abc")
                .build();

        when(appsAPI.getSecrets(eq(OAUTH_KEY), eq(false), eq(site), eq(user)))
                .thenReturn(Optional.empty());
        when(appsAPI.getSecrets(eq(SAML_KEY), eq(false), eq(site), eq(user)))
                .thenReturn(Optional.empty());
        when(appsAPI.getSecrets(eq(OAUTH_KEY), eq(true), eq(site), eq(user)))
                .thenReturn(Optional.of(oauthSecrets));

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::systemHost).thenReturn(systemHost);
            apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
            when(hostAPI.find(SITE_ID, user, false)).thenReturn(site);

            final Response rsp = resource.getConfig(request, response, SITE_ID);
            final DotAuthConfigView entity =
                    (DotAuthConfigView) ((ResponseEntityView<?>) rsp.getEntity()).getEntity();

            assertEquals(DotAuthProtocol.OAUTH, entity.getProtocol());
            assertFalse(entity.isConfigured());
            assertTrue(entity.isInherited());
            assertEquals("abc", entity.getValues().get("clientId"));
        }
    }

    @Test
    public void getConfig_defaults_to_OAUTH_when_nothing_configured_anywhere() throws Exception {
        when(appsAPI.getSecrets(any(), anyBoolean(), any(Host.class), eq(user)))
                .thenReturn(Optional.empty());

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::systemHost).thenReturn(systemHost);
            apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
            when(hostAPI.find(SITE_ID, user, false)).thenReturn(site);

            final Response rsp = resource.getConfig(request, response, SITE_ID);
            final DotAuthConfigView entity =
                    (DotAuthConfigView) ((ResponseEntityView<?>) rsp.getEntity()).getEntity();

            assertEquals(DotAuthProtocol.OAUTH, entity.getProtocol());
            assertFalse(entity.isConfigured());
            assertFalse(entity.isInherited());
            assertNotNull(entity.getValues());
            assertTrue(entity.getValues().isEmpty());
        }
    }

    // --- saveConfig (mutual exclusion) -------------------------------------

    @Test
    public void saveConfig_with_OAUTH_deletes_existing_dotsaml_config_for_host() throws Exception {
        final DotAuthConfigForm form = new DotAuthConfigForm(
                DotAuthProtocol.OAUTH, Map.of("clientId", "abc"));

        when(appsAPI.getSecrets(eq(OAUTH_KEY), anyBoolean(), eq(site), eq(user)))
                .thenReturn(Optional.empty());

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::systemHost).thenReturn(systemHost);
            apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
            when(hostAPI.find(SITE_ID, user, false)).thenReturn(site);

            resource.saveConfig(request, response, SITE_ID, form);

            verify(appsAPI).deleteSecrets(SAML_KEY, site, user);
            verify(appsAPI, never()).deleteSecrets(eq(OAUTH_KEY), any(Host.class), any(User.class));
            verify(appsAPI).saveSecrets(any(AppSecrets.class), eq(site), eq(user));
        }
    }

    @Test
    public void saveConfig_with_SAML_deletes_existing_dotAuth_for_host() throws Exception {
        final DotAuthConfigForm form = new DotAuthConfigForm(
                DotAuthProtocol.SAML, Map.of("idpName", "Okta"));

        when(appsAPI.getSecrets(eq(SAML_KEY), anyBoolean(), eq(site), eq(user)))
                .thenReturn(Optional.empty());

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::systemHost).thenReturn(systemHost);
            apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
            when(hostAPI.find(SITE_ID, user, false)).thenReturn(site);

            resource.saveConfig(request, response, SITE_ID, form);

            verify(appsAPI).deleteSecrets(OAUTH_KEY, site, user);
            verify(appsAPI, never()).deleteSecrets(eq(SAML_KEY), any(Host.class), any(User.class));
            verify(appsAPI).saveSecrets(any(AppSecrets.class), eq(site), eq(user));
        }
    }

    @Test
    public void saveConfig_preserves_masked_secret_from_inherited_system_config() throws Exception {
        final DotAuthConfigForm form = new DotAuthConfigForm(
                DotAuthProtocol.OAUTH,
                Map.of("clientId", "site-client", "clientSecret", DotAuthConstants.HIDDEN_SECRET_MASK));
        final AppSecrets systemSecrets = AppSecrets.builder()
                .withKey(OAUTH_KEY)
                .withHiddenSecret("clientSecret", "system-secret")
                .build();

        when(appsAPI.getSecrets(eq(OAUTH_KEY), eq(true), eq(site), eq(user)))
                .thenReturn(Optional.of(systemSecrets));

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::systemHost).thenReturn(systemHost);
            apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
            when(hostAPI.find(SITE_ID, user, false)).thenReturn(site);

            resource.saveConfig(request, response, SITE_ID, form);

            final ArgumentCaptor<AppSecrets> saved = ArgumentCaptor.forClass(AppSecrets.class);
            verify(appsAPI).saveSecrets(saved.capture(), eq(site), eq(user));
            assertEquals("system-secret",
                    saved.getValue().getSecrets().get("clientSecret").getString());
        }
    }

    @Test
    public void saveConfig_with_missing_protocol_defaults_to_OAUTH() throws Exception {
        final DotAuthConfigForm form = new DotAuthConfigForm(null, Map.of("clientId", "abc"));

        when(appsAPI.getSecrets(eq(OAUTH_KEY), anyBoolean(), eq(site), eq(user)))
                .thenReturn(Optional.empty());

        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::systemHost).thenReturn(systemHost);
            apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
            when(hostAPI.find(SITE_ID, user, false)).thenReturn(site);

            resource.saveConfig(request, response, SITE_ID, form);

            // Missing protocol → OAUTH chosen → SAML row is the one deleted.
            verify(appsAPI).deleteSecrets(SAML_KEY, site, user);
            verify(appsAPI, never()).deleteSecrets(eq(OAUTH_KEY), any(Host.class), any(User.class));
        }
    }

    // --- clearConfig --------------------------------------------------------

    @Test
    public void clearConfig_deletes_both_dotAuth_and_dotsaml_config() throws Exception {
        try (MockedStatic<APILocator> apiLocator = Mockito.mockStatic(APILocator.class)) {
            apiLocator.when(APILocator::systemHost).thenReturn(systemHost);
            apiLocator.when(APILocator::getHostAPI).thenReturn(hostAPI);
            when(hostAPI.find(SITE_ID, user, false)).thenReturn(site);

            final Response rsp = resource.clearConfig(request, response, SITE_ID);

            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), rsp.getStatus());
            verify(appsAPI).deleteSecrets(OAUTH_KEY, site, user);
            verify(appsAPI).deleteSecrets(SAML_KEY, site, user);
        }
    }

    // --- auth gating -------------------------------------------------------

    @Test
    public void listSites_maps_security_failure_to_error_response_when_no_user() {
        // WebResource.InitBuilder#init() raises a NotAuthorizedException when
        // rejectWhenNoUser(true) is set and there's no authenticated user.
        // Using the JAX-RS type because DotSecurityException is checked and
        // cannot be passed to Mockito's thenThrow() on init()'s signature.
        when(webResource.init(any(WebResource.InitBuilder.class)))
                .thenThrow(new javax.ws.rs.NotAuthorizedException("No user in request"));

        final Response rsp = resource.listSites(request, response);

        // ResponseUtil.mapExceptionResponse turns the security exception into
        // a 401/403 response — either way, NOT a 200 OK.
        assertTrue(rsp.getStatus() >= 400,
                "Expected a non-2xx response when the user is rejected, got " + rsp.getStatus());
    }
}
