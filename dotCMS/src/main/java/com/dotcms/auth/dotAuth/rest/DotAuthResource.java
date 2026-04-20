package com.dotcms.auth.dotAuth.rest;

import static com.dotcms.rest.ResponseEntityView.OK;

import com.dotcms.auth.dotAuth.DotAuthConstants;
import com.dotcms.auth.dotAuth.rest.handler.OAuthProtocolHandler;
import com.dotcms.auth.providers.oauth.OAuthAppConfig;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
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

    private final WebResource webResource;
    private final AppsAPI appsAPI;
    private final OAuthProtocolHandler oauthHandler = new OAuthProtocolHandler();

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
            // AppsUtil.internalKey lowercases both the host id AND the app key when storing,
            // so everything in `appsByHost` is lowercase. Compare against lowercased values.
            final String appKeyLower = DotAuthConstants.APP_KEY.toLowerCase();
            final boolean systemConfigured = appsByHost
                    .getOrDefault(systemHost.getIdentifier().toLowerCase(), Set.of())
                    .contains(appKeyLower);

            final List<Host> allHosts = APILocator.getHostAPI().findAll(user, false);
            final List<DotAuthSitesView.SiteRowView> rows = new ArrayList<>();
            for (final Host host : allHosts) {
                if (host == null || host.isSystemHost() || host.isArchived()) {
                    continue;
                }
                final boolean hasOwn = appsByHost
                        .getOrDefault(host.getIdentifier().toLowerCase(), Set.of())
                        .contains(appKeyLower);
                final DotAuthSiteStatus status;
                if (hasOwn) {
                    status = DotAuthSiteStatus.SITE_OVERRIDE;
                } else if (systemConfigured) {
                    status = DotAuthSiteStatus.INHERITED;
                } else {
                    status = DotAuthSiteStatus.NOT_CONFIGURED;
                }
                final DotAuthProtocol rowProtocol = status == DotAuthSiteStatus.NOT_CONFIGURED
                        ? null
                        : DotAuthProtocol.OAUTH;
                rows.add(new DotAuthSitesView.SiteRowView(
                        host.getIdentifier(), host.getHostname(), status, rowProtocol));
            }

            final DotAuthSitesView entity = new DotAuthSitesView(
                    new DotAuthSitesView.SystemView(systemConfigured,
                            systemConfigured ? DotAuthProtocol.OAUTH : null),
                    rows);
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
                        new DotAuthConfigView(hostId, DotAuthProtocol.OAUTH, true, false,
                                oauthHandler.maskedValues(hostOwn.get())))).build();
            }

            if (host.isSystemHost()) {
                return Response.ok(new ResponseEntityDotAuthConfigView(
                        new DotAuthConfigView(hostId, DotAuthProtocol.OAUTH, false, false, Map.of()))).build();
            }

            final Optional<AppSecrets> systemSecrets = appsAPI.getSecrets(
                    DotAuthConstants.APP_KEY, false, APILocator.systemHost(), user);

            if (systemSecrets.isPresent()) {
                return Response.ok(new ResponseEntityDotAuthConfigView(
                        new DotAuthConfigView(hostId, DotAuthProtocol.OAUTH, false, true,
                                oauthHandler.maskedValues(systemSecrets.get())))).build();
            }

            return Response.ok(new ResponseEntityDotAuthConfigView(
                    new DotAuthConfigView(hostId, DotAuthProtocol.OAUTH, false, false, Map.of()))).build();
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

            appsAPI.saveSecrets(oauthHandler.buildSecrets(
                    form.getValues() == null ? Map.of() : form.getValues(),
                    existing), host, user);
            return Response.ok(new ResponseEntityView<>(OK)).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    String.format("Error saving dotAuth config for hostId `%s`", hostId), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    /**
     * TEMP DIAGNOSTIC — reports what {@link OAuthAppConfig#config(HttpServletRequest)}
     * sees, so we can debug why {@code OAuthWebInterceptor} isn't redirecting
     * authenticated requests to the OAuth provider.
     */
    @GET
    @Path("/debug")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response debug(@Context final HttpServletRequest request,
                                @Context final HttpServletResponse response) {
        try {
            initUser(request, response);
            final Host host = com.dotmarketing.business.web.WebAPILocator.getHostWebAPI()
                    .getCurrentHostNoThrow(request);
            final Map<String, Object> result = new HashMap<>();
            result.put("currentHostId", host == null ? null : host.getIdentifier());
            result.put("currentHostName", host == null ? null : host.getHostname());
            result.put("systemHostId", APILocator.systemHost().getIdentifier());

            // Raw getSecrets as systemUser (mirrors OAuthAppConfig.loadSecrets)
            final Optional<AppSecrets> secretsAsSystemUser = Try.of(() ->
                    appsAPI.getSecrets(DotAuthConstants.APP_KEY, true, host,
                            APILocator.systemUser())).getOrElse(Optional.empty());
            result.put("secretsPresent_systemUser", secretsAsSystemUser.isPresent());
            secretsAsSystemUser.ifPresent(s -> {
                final Map<String, String> keys = new HashMap<>();
                s.getSecrets().forEach((k, v) -> keys.put(k,
                        oauthHandler.hiddenKeys().contains(k) ? HIDDEN_SECRET_MASK
                                : Try.of(v::getString).getOrElse("")));
                result.put("secretValues", keys);
            });

            // What OAuthAppConfig.config returns (includes the enabled filter)
            final Optional<OAuthAppConfig> cfg = OAuthAppConfig.config(request);
            result.put("configPresent", cfg.isPresent());
            cfg.ifPresent(c -> {
                result.put("configEnabled", c.enabled);
                result.put("configEnableBackend", c.enableBackend);
                result.put("configEnableFrontend", c.enableFrontend);
                result.put("configProviderType", c.providerType);
            });

            return Response.ok(new ResponseEntityView<>(result)).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error in dotAuth debug", e);
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
}
