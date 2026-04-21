package com.dotcms.auth.dotAuth.rest;

import static com.dotcms.rest.ResponseEntityView.OK;

import com.dotcms.auth.dotAuth.rest.handler.OAuthProtocolHandler;
import com.dotcms.auth.dotAuth.rest.handler.ProtocolHandler;
import com.dotcms.auth.dotAuth.rest.handler.SamlProtocolHandler;
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
import java.util.ArrayList;
import java.util.EnumMap;
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
 * (OAuth) and {@code dotsaml-config} (SAML) AppSecrets rows on a per-site
 * basis, with the SYSTEM_HOST row exposed as the global default via the
 * sentinel {@code "SYSTEM_HOST"} host id.
 * <p>
 * Protocol dispatch is delegated to a {@link ProtocolHandler} per protocol.
 * Saves are mutually exclusive: writing one protocol's secrets deletes the
 * other protocol's row for the same host.
 * <p>
 * All endpoints delegate straight to {@link AppsAPI}; there is no separate
 * persistence layer.
 */
@Path("/v1/dotauth")
@Tag(name = "dotAuth", description = "OAuth/OIDC and SAML configuration per site, with SYSTEM_HOST as the global default")
public class DotAuthResource {

    /** Sentinel path value representing the SYSTEM_HOST row (the global default). */
    public static final String SYSTEM_HOST_SENTINEL = "SYSTEM_HOST";

    /** Value returned for hidden secrets; posting it back means "keep the stored value". */
    public static final String HIDDEN_SECRET_MASK = "****";

    private final WebResource webResource;
    private final AppsAPI appsAPI;
    private final Map<DotAuthProtocol, ProtocolHandler> handlers;

    public DotAuthResource() {
        this(new WebResource(), APILocator.getAppsAPI(), defaultHandlers());
    }

    @VisibleForTesting
    public DotAuthResource(final WebResource webResource,
                           final AppsAPI appsAPI,
                           final Map<DotAuthProtocol, ProtocolHandler> handlers) {
        this.webResource = webResource;
        this.appsAPI = appsAPI;
        this.handlers = handlers;
    }

    private static Map<DotAuthProtocol, ProtocolHandler> defaultHandlers() {
        final Map<DotAuthProtocol, ProtocolHandler> map = new EnumMap<>(DotAuthProtocol.class);
        map.put(DotAuthProtocol.OAUTH, new OAuthProtocolHandler());
        map.put(DotAuthProtocol.SAML, new SamlProtocolHandler());
        return map;
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
            final Set<String> systemKeys = appsByHost
                    .getOrDefault(systemHost.getIdentifier().toLowerCase(), Set.of());
            final DotAuthProtocol systemProtocol = detectProtocol(systemKeys);
            final boolean systemConfigured = systemProtocol != null;

            final List<Host> allHosts = APILocator.getHostAPI().findAll(user, false);
            final List<DotAuthSitesView.SiteRowView> rows = new ArrayList<>();
            for (final Host host : allHosts) {
                if (host == null || host.isSystemHost() || host.isArchived()) {
                    continue;
                }
                final Set<String> hostKeys = appsByHost
                        .getOrDefault(host.getIdentifier().toLowerCase(), Set.of());
                final DotAuthProtocol hostProtocol = detectProtocol(hostKeys);

                final DotAuthSiteStatus status;
                final DotAuthProtocol rowProtocol;
                if (hostProtocol != null) {
                    status = DotAuthSiteStatus.SITE_OVERRIDE;
                    rowProtocol = hostProtocol;
                } else if (systemConfigured) {
                    status = DotAuthSiteStatus.INHERITED;
                    rowProtocol = systemProtocol;
                } else {
                    status = DotAuthSiteStatus.NOT_CONFIGURED;
                    rowProtocol = null;
                }
                rows.add(new DotAuthSitesView.SiteRowView(
                        host.getIdentifier(), host.getHostname(), status, rowProtocol));
            }

            final DotAuthSitesView entity = new DotAuthSitesView(
                    new DotAuthSitesView.SystemView(systemConfigured, systemProtocol),
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

            for (final DotAuthProtocol protocol : DotAuthProtocol.values()) {
                final ProtocolHandler handler = handlers.get(protocol);
                final Optional<AppSecrets> own = appsAPI.getSecrets(
                        handler.appKey(), false, host, user);
                if (own.isPresent()) {
                    return Response.ok(new ResponseEntityDotAuthConfigView(
                            new DotAuthConfigView(hostId, protocol, true, false,
                                    handler.maskedValues(own.get())))).build();
                }
            }

            if (!host.isSystemHost()) {
                final Host systemHost = APILocator.systemHost();
                for (final DotAuthProtocol protocol : DotAuthProtocol.values()) {
                    final ProtocolHandler handler = handlers.get(protocol);
                    final Optional<AppSecrets> systemSecrets = appsAPI.getSecrets(
                            handler.appKey(), false, systemHost, user);
                    if (systemSecrets.isPresent()) {
                        return Response.ok(new ResponseEntityDotAuthConfigView(
                                new DotAuthConfigView(hostId, protocol, false, true,
                                        handler.maskedValues(systemSecrets.get())))).build();
                    }
                }
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

            final DotAuthProtocol chosen = form.getProtocol() == null
                    ? DotAuthProtocol.OAUTH
                    : form.getProtocol();
            final ProtocolHandler active = handlers.get(chosen);

            // Mutual exclusion: clear the other protocol's row before writing the chosen one.
            for (final ProtocolHandler handler : handlers.values()) {
                if (handler.protocol() != chosen) {
                    appsAPI.deleteSecrets(handler.appKey(), host, user);
                }
            }

            final Optional<AppSecrets> existing = appsAPI.getSecrets(
                    active.appKey(), false, host, user);

            appsAPI.saveSecrets(active.buildSecrets(
                    form.getValues() == null ? Map.of() : form.getValues(),
                    existing), host, user);
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
            for (final ProtocolHandler handler : handlers.values()) {
                appsAPI.deleteSecrets(handler.appKey(), host, user);
            }
            return Response.noContent().build();
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    String.format("Error clearing dotAuth config for hostId `%s`", hostId), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private DotAuthProtocol detectProtocol(final Set<String> hostKeys) {
        for (final ProtocolHandler handler : handlers.values()) {
            if (hostKeys.contains(handler.appKey().toLowerCase())) {
                return handler.protocol();
            }
        }
        return null;
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
