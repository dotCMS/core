package com.dotcms.auth.dotAuth.rest;

import static com.dotcms.rest.ResponseEntityView.OK;

import com.dotcms.auth.dotAuth.DotAuthConstants;
import com.dotcms.auth.providers.oauth.OAuthAppConfig;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.Secret;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST surface for the dotAuth portlet. Edits and reads the {@code dotAuth}
 * AppSecrets row on a per-site basis, with the SYSTEM_HOST row exposed as the
 * global default via the sentinel {@code "SYSTEM_HOST"} host id.
 * <p>
 * All endpoints delegate straight to {@link AppsAPI}; there is no separate
 * persistence layer.
 */
@Path("/v1/dotauth")
@Tag(name = "dotAuth", description = "OAuth / OIDC configuration per site, with SYSTEM_HOST as the global default")
public class DotAuthResource {

    /** Sentinel path value representing the SYSTEM_HOST row (the global default). */
    public static final String SYSTEM_HOST_SENTINEL = "SYSTEM_HOST";

    /** Value returned for hidden secrets; posting it back means "keep the stored value". */
    public static final String HIDDEN_SECRET_MASK = "****";

    /** Keys the portlet can set. Must stay in sync with {@link OAuthAppConfig}. */
    private static final List<String> SECRET_KEYS = List.of(
            OAuthAppConfig.KEY_ENABLED,
            OAuthAppConfig.KEY_ENABLE_BACKEND,
            OAuthAppConfig.KEY_ENABLE_FRONTEND,
            OAuthAppConfig.KEY_PROVIDER_TYPE,
            OAuthAppConfig.KEY_ISSUER_URL,
            OAuthAppConfig.KEY_CLIENT_ID,
            OAuthAppConfig.KEY_CLIENT_SECRET,
            OAuthAppConfig.KEY_SCOPES,
            OAuthAppConfig.KEY_AUTHORIZATION_URL,
            OAuthAppConfig.KEY_TOKEN_URL,
            OAuthAppConfig.KEY_USERINFO_URL,
            OAuthAppConfig.KEY_REVOCATION_URL,
            OAuthAppConfig.KEY_LOGOUT_URL,
            OAuthAppConfig.KEY_GROUPS_CLAIM,
            OAuthAppConfig.KEY_GROUPS_URL,
            OAuthAppConfig.KEY_EXTRA_ROLES,
            OAuthAppConfig.KEY_CALLBACK_URL);

    private static final Set<String> HIDDEN_KEYS = Set.of(OAuthAppConfig.KEY_CLIENT_SECRET);

    private static final Set<String> BOOLEAN_KEYS = Set.of(
            OAuthAppConfig.KEY_ENABLED,
            OAuthAppConfig.KEY_ENABLE_BACKEND,
            OAuthAppConfig.KEY_ENABLE_FRONTEND);

    private final WebResource webResource;
    private final AppsAPI appsAPI;

    public DotAuthResource() {
        this(new WebResource(), APILocator.getAppsAPI());
    }

    @VisibleForTesting
    public DotAuthResource(final WebResource webResource, final AppsAPI appsAPI) {
        this.webResource = webResource;
        this.appsAPI = appsAPI;
    }

    @GET
    @Path("/sites")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response listSites(@Context final HttpServletRequest request,
                                    @Context final HttpServletResponse response) {
        try {
            final User user = initUser(request, response);
            final Host systemHost = APILocator.systemHost();
            final Map<String, Set<String>> appsByHost = appsAPI.appKeysByHost();
            final boolean systemConfigured = appsByHost
                    .getOrDefault(systemHost.getIdentifier(), Set.of())
                    .contains(DotAuthConstants.APP_KEY);

            final List<Host> allHosts = APILocator.getHostAPI().findAll(user, false);
            final List<DotAuthSitesView.SiteRowView> rows = new ArrayList<>();
            for (final Host host : allHosts) {
                if (host == null || host.isSystemHost() || host.isArchived()) {
                    continue;
                }
                final boolean hasOwn = appsByHost
                        .getOrDefault(host.getIdentifier(), Set.of())
                        .contains(DotAuthConstants.APP_KEY);
                final DotAuthSiteStatus status;
                if (hasOwn) {
                    status = DotAuthSiteStatus.SITE_OVERRIDE;
                } else if (systemConfigured) {
                    status = DotAuthSiteStatus.INHERITED;
                } else {
                    status = DotAuthSiteStatus.NOT_CONFIGURED;
                }
                rows.add(new DotAuthSitesView.SiteRowView(host.getIdentifier(), host.getHostname(), status));
            }

            final DotAuthSitesView entity = new DotAuthSitesView(
                    new DotAuthSitesView.SystemView(systemConfigured), rows);
            return Response.ok(new ResponseEntityDotAuthSitesView(entity)).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error listing dotAuth sites", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @GET
    @Path("/sites/{hostId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response getConfig(@Context final HttpServletRequest request,
                                    @Context final HttpServletResponse response,
                                    @PathParam("hostId") final String hostId) {
        try {
            final User user = initUser(request, response);
            final Host host = resolveHost(hostId, user);

            final Optional<AppSecrets> hostOwn = appsAPI.getSecrets(
                    DotAuthConstants.APP_KEY, false, host, user);

            if (hostOwn.isPresent()) {
                return Response.ok(new ResponseEntityDotAuthConfigView(
                        new DotAuthConfigView(hostId, true, false, maskedValues(hostOwn.get())))).build();
            }

            if (host.isSystemHost()) {
                return Response.ok(new ResponseEntityDotAuthConfigView(
                        new DotAuthConfigView(hostId, false, false, Map.of()))).build();
            }

            final Optional<AppSecrets> systemSecrets = appsAPI.getSecrets(
                    DotAuthConstants.APP_KEY, false, APILocator.systemHost(), user);

            if (systemSecrets.isPresent()) {
                return Response.ok(new ResponseEntityDotAuthConfigView(
                        new DotAuthConfigView(hostId, false, true, maskedValues(systemSecrets.get())))).build();
            }

            return Response.ok(new ResponseEntityDotAuthConfigView(
                    new DotAuthConfigView(hostId, false, false, Map.of()))).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    String.format("Error loading dotAuth config for hostId `%s`", hostId), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @PUT
    @Path("/sites/{hostId}")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response saveConfig(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @PathParam("hostId") final String hostId,
                                     final DotAuthConfigForm form) {
        try {
            if (form == null) {
                throw new IllegalArgumentException("Body required");
            }
            form.checkValid();
            final User user = initUser(request, response);
            final Host host = resolveHost(hostId, user);

            final Optional<AppSecrets> existing = appsAPI.getSecrets(
                    DotAuthConstants.APP_KEY, false, host, user);

            final AppSecrets.Builder builder = AppSecrets.builder()
                    .withKey(DotAuthConstants.APP_KEY);
            final Map<String, Object> incoming = form.getValues() == null ? Map.of() : form.getValues();

            for (final String key : SECRET_KEYS) {
                final Object raw = incoming.get(key);
                if (HIDDEN_KEYS.contains(key)) {
                    final String str = raw == null ? null : String.valueOf(raw);
                    if (HIDDEN_SECRET_MASK.equals(str)) {
                        existing.map(AppSecrets::getSecrets)
                                .map(m -> m.get(key))
                                .ifPresent(secret -> builder.withSecret(key, secret));
                        continue;
                    }
                    if (str == null || str.isEmpty()) {
                        continue;
                    }
                    builder.withHiddenSecret(key, str);
                    continue;
                }
                if (raw == null) {
                    continue;
                }
                if (BOOLEAN_KEYS.contains(key)) {
                    builder.withSecret(key, Boolean.parseBoolean(String.valueOf(raw)));
                } else {
                    builder.withSecret(key, String.valueOf(raw));
                }
            }

            appsAPI.saveSecrets(builder.build(), host, user);
            return Response.ok(new ResponseEntityView<>(OK)).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    String.format("Error saving dotAuth config for hostId `%s`", hostId), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @DELETE
    @Path("/sites/{hostId}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response clearConfig(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @PathParam("hostId") final String hostId) {
        try {
            final User user = initUser(request, response);
            final Host host = resolveHost(hostId, user);
            appsAPI.deleteSecrets(DotAuthConstants.APP_KEY, host, user);
            return Response.noContent().build();
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    String.format("Error clearing dotAuth config for hostId `%s`", hostId), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private User initUser(final HttpServletRequest request, final HttpServletResponse response) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        return initData.getUser();
    }

    private Host resolveHost(final String hostId, final User user)
            throws DotDataException, DotSecurityException {
        if (SYSTEM_HOST_SENTINEL.equals(hostId)) {
            return APILocator.systemHost();
        }
        final Host host = APILocator.getHostAPI().find(hostId, user, false);
        if (host == null) {
            throw new DoesNotExistException(String.format("No site found for id `%s`", hostId));
        }
        return host;
    }

    private Map<String, Object> maskedValues(final AppSecrets secrets) {
        final Map<String, Secret> raw = secrets.getSecrets();
        final Map<String, Object> out = new HashMap<>();
        for (final String key : SECRET_KEYS) {
            final Secret secret = raw.get(key);
            if (secret == null) {
                continue;
            }
            if (HIDDEN_KEYS.contains(key)) {
                out.put(key, HIDDEN_SECRET_MASK);
                continue;
            }
            if (BOOLEAN_KEYS.contains(key)) {
                out.put(key, Try.of(secret::getBoolean).getOrElse(false));
                continue;
            }
            out.put(key, Try.of(secret::getString).getOrElse(""));
        }
        return out;
    }
}
