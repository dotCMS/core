package com.dotcms.auth.dotAuth.rest;

import static com.dotcms.rest.ResponseEntityView.OK;

import com.dotcms.auth.dotAuth.session.DotAuthSessionCache;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotmarketing.business.CacheLocator;
import com.google.common.collect.ImmutableMap;
import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotcms.auth.dotAuth.DotAuthConstants;
import com.dotcms.auth.dotAuth.rest.handler.HeadlessConfigHelper;
import com.dotcms.auth.dotAuth.rest.handler.OAuthProtocolHandler;
import com.dotcms.auth.dotAuth.rest.handler.ProtocolHandler;
import com.dotcms.auth.dotAuth.rest.handler.SamlProtocolHandler;
import com.dotcms.auth.providers.oauth.OAuthSsrfGuard;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityStringView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.api.MultiPartUtils;
import com.dotcms.rest.api.v1.authentication.ResponseUtil;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.AppsUtil;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PortletID;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.Key;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

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
@Tag(name = "dotAuth", description = "OAuth/OIDC and SAML authentication: per-site configuration (SYSTEM_HOST is the global default) and headless OIDC token exchange")
public class DotAuthResource {

    /** Sentinel path value representing the SYSTEM_HOST row (the global default). */
    public static final String SYSTEM_HOST_SENTINEL = "SYSTEM_HOST";
    private static final int OIDC_DISCOVERY_MAX_RESPONSE_BYTES = 1024 * 1024;
    private static final int SAML_METADATA_MAX_RESPONSE_BYTES = 5 * 1024 * 1024;

    private final WebResource webResource;
    private final AppsAPI appsAPI;
    private final Map<DotAuthProtocol, ProtocolHandler> handlers;
    private final HeadlessConfigHelper headlessHelper;
    private final DotAuthSessionCache sessionCache = CacheLocator.getDotAuthSessionCache();
    private static final ObjectMapper MAPPER =
            DotObjectMapperProvider.getInstance().getDefaultObjectMapper();
    private static final int EXPORT_PASSWORD_MIN_LENGTH = 14;
    private static final int EXPORT_PASSWORD_MAX_LENGTH = 32;
    private static final Set<String> DOTAUTH_APP_KEYS = Set.of(
            DotAuthConstants.APP_KEY,
            com.dotcms.saml.DotSamlProxyFactory.SAML_APP_CONFIG_KEY,
            DotAuthConstants.HEADLESS_APP_KEY);

    public DotAuthResource() {
        this(new WebResource(), APILocator.getAppsAPI(), defaultHandlers(),
                new HeadlessConfigHelper());
    }

    @VisibleForTesting
    public DotAuthResource(final WebResource webResource,
                           final AppsAPI appsAPI,
                           final Map<DotAuthProtocol, ProtocolHandler> handlers,
                           final HeadlessConfigHelper headlessHelper) {
        this.webResource = webResource;
        this.appsAPI = appsAPI;
        this.handlers = handlers;
        this.headlessHelper = headlessHelper;
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
    @Operation(operationId = "listDotAuthSites",
            summary = "List all sites with their dotAuth status",
            description = "Returns the SYSTEM_HOST (global default) status plus a per-site list " +
                    "indicating whether each site has its own OAuth/SAML configuration, inherits " +
                    "from the system default, or is unconfigured.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sites retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityDotAuthSitesView.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to access dotAuth"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
            final Optional<DotAuthProtocol> systemProtocol = detectProtocol(systemKeys);
            final boolean systemHeadless = systemKeys.contains(
                    DotAuthConstants.HEADLESS_APP_KEY.toLowerCase());

            // Use the paginated search API instead of the deprecated findAll.
            // An empty filter with limit <= 0 returns all live, non-system hosts.
            final List<Host> allHosts = APILocator.getHostAPI()
                    .search("", false, false, 0, 0, user, false);
            final List<DotAuthSitesView.SiteRowView> rows = new ArrayList<>();
            for (final Host host : allHosts) {
                final Set<String> hostKeys = appsByHost
                        .getOrDefault(host.getIdentifier().toLowerCase(), Set.of());
                final Optional<DotAuthProtocol> hostProtocol = detectProtocol(hostKeys);

                final DotAuthSiteStatus status;
                final DotAuthProtocol rowProtocol;
                if (hostProtocol.isPresent()) {
                    status = DotAuthSiteStatus.SITE_OVERRIDE;
                    rowProtocol = hostProtocol.get();
                } else if (systemProtocol.isPresent()) {
                    status = DotAuthSiteStatus.INHERITED;
                    rowProtocol = systemProtocol.get();
                } else {
                    status = DotAuthSiteStatus.NOT_CONFIGURED;
                    rowProtocol = null;
                }
                rows.add(new DotAuthSitesView.SiteRowView(
                        host.getIdentifier(), host.getHostname(), status, rowProtocol));
            }

            final DotAuthSitesView entity = new DotAuthSitesView(
                    new DotAuthSitesView.SystemView(systemProtocol.isPresent(),
                            systemProtocol.orElse(null), systemHeadless),
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
    @Operation(operationId = "getDotAuthConfig",
            summary = "Get the dotAuth configuration for a site",
            description = "Returns the protocol-specific configuration stored for the given hostId. " +
                    "Use the sentinel \"SYSTEM_HOST\" for the global default. When the host has no row " +
                    "of its own and the system default is configured, the response carries " +
                    "inherited=true and values holds the system defaults. Hidden secrets (e.g. " +
                    "clientSecret, privateKey) are masked as \"****\".")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityDotAuthConfigView.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to read this site"),
            @ApiResponse(responseCode = "404", description = "Site not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public final Response getConfig(@Context final HttpServletRequest request,
                                    @Context final HttpServletResponse response,
                                    @PathParam("hostId") final String hostId) {
        try {
            final User user = initUser(request, response);
            final Host host = resolveHost(hostId, user);

            // --- SSO config ---
            DotAuthProtocol ssoProtocol = DotAuthProtocol.OAUTH;
            boolean ssoConfigured = false;
            boolean ssoInherited = false;
            Map<String, Object> ssoValues = Map.of();

            final Optional<ProtocolConfig> ownConfig = findConfiguredProtocol(host, user, false);
            if (ownConfig.isPresent()) {
                final ProtocolConfig config = ownConfig.get();
                ssoProtocol = config.protocol;
                ssoConfigured = true;
                ssoValues = config.values;
            }

            if (!ssoConfigured && !host.isSystemHost()) {
                final Optional<ProtocolConfig> inheritedConfig =
                        findConfiguredProtocol(host, user, true);
                if (inheritedConfig.isPresent()) {
                    final ProtocolConfig config = inheritedConfig.get();
                    ssoProtocol = config.protocol;
                    ssoInherited = true;
                    ssoValues = config.values;
                }
            }

            // --- Headless config (system-only, no per-site) ---
            final Host systemHost = APILocator.systemHost();
            final Map<String, Object> headlessValues = appsAPI.getSecrets(
                    DotAuthConstants.HEADLESS_APP_KEY, false, systemHost, user)
                    .map(headlessHelper::values).orElse(Map.of());

            return Response.ok(new ResponseEntityDotAuthConfigView(
                    new DotAuthConfigView(hostId, ssoProtocol, ssoConfigured, ssoInherited,
                            ssoValues, headlessValues))).build();
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
    @Operation(operationId = "saveDotAuthConfig",
            summary = "Save (upsert) the dotAuth configuration for a site",
            description = "Writes the chosen protocol's secrets for the given hostId and deletes " +
                    "the other protocol's row for that host (mutual exclusion). Posting \"****\" on " +
                    "a hidden field preserves the stored value.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration saved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityStringView.class))),
            @ApiResponse(responseCode = "400", description = "Invalid form payload"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to edit this site"),
            @ApiResponse(responseCode = "404", description = "Site not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public final Response saveConfig(@Context final HttpServletRequest request,
                                     @Context final HttpServletResponse response,
                                     @PathParam("hostId") final String hostId,
                                     final DotAuthConfigForm form) {
        try {
            if (form == null) {
                throw new IllegalArgumentException("Body required");
            }
            final User user = initUser(request, response);
            form.checkValid();
            final Host host = resolveHost(hostId, user);

            // DotAuthConfigForm#constructor already defaults protocol to OAUTH when null,
            // so no extra fallback is needed here.
            final DotAuthProtocol chosen = form.getProtocol();
            final ProtocolHandler active = handlers.get(chosen);
            if (active == null) {
                // Jackson should have caught an invalid enum value before we get here, but guard
                // anyway: surface as 400 rather than NPE-ing inside buildSecrets below.
                throw new BadRequestException("Unknown dotAuth protocol: " + chosen);
            }

            // Mutual exclusion between protocols on the same host. AppsAPI is not
            // transactional, so order matters on failure:
            //   1. Save the chosen protocol FIRST. If validation/IO blows up here, the
            //      caller still has the previous config intact — no data loss.
            //   2. Only once the save succeeds, delete the other protocol's row. A
            //      failure in step 2 leaves both rows briefly; getConfig prefers the
            //      row with the newest dotAuth save marker, so the user still sees the
            //      freshly saved config and can retry the clean-up by re-saving.
            final Optional<AppSecrets> existing = secretsToPreserve(active, host, user);

            appsAPI.saveSecrets(withLastSavedMarker(
                    active.buildSecrets(form.getValues(), existing)), host, user);

            for (final ProtocolHandler handler : handlers.values()) {
                if (handler.protocol() != chosen) {
                    try {
                        appsAPI.deleteSecrets(handler.appKey(), host, user);
                    } catch (final Exception cleanupError) {
                        Logger.warn(this.getClass(), String.format(
                                "dotAuth save succeeded but clean-up of stale %s row for host %s failed: %s",
                                handler.protocol(), hostId, cleanupError.getMessage()));
                    }
                }
            }
            SecurityLogger.logInfo(DotAuthResource.class,
                    String.format("User %s saved dotAuth %s config for host %s",
                            user.getUserId(), chosen, hostId));
            return Response.ok(new ResponseEntityStringView(OK)).build();
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
    @Operation(operationId = "clearDotAuthConfig",
            summary = "Clear the dotAuth configuration for a site",
            description = "Deletes both OAuth and SAML secret rows for the given hostId. On " +
                    "SYSTEM_HOST this removes the global default; non-system hosts fall back to " +
                    "inheriting from SYSTEM_HOST afterward.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Configuration cleared successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to edit this site"),
            @ApiResponse(responseCode = "404", description = "Site not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public final Response clearConfig(@Context final HttpServletRequest request,
                                      @Context final HttpServletResponse response,
                                      @PathParam("hostId") final String hostId) {
        try {
            final User user = initUser(request, response);
            final Host host = resolveHost(hostId, user);
            for (final ProtocolHandler handler : handlers.values()) {
                appsAPI.deleteSecrets(handler.appKey(), host, user);
            }
            SecurityLogger.logInfo(DotAuthResource.class,
                    String.format("User %s cleared dotAuth config for host %s",
                            user.getUserId(), hostId));
            return Response.noContent().build();
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    String.format("Error clearing dotAuth config for hostId `%s`", hostId), e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @PUT
    @Path("/headless")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "saveDotAuthHeadlessConfig",
            summary = "Save the system-level headless token-exchange configuration",
            description = "Writes headless config to SYSTEM_HOST. Headless config is system-wide " +
                    "(not per-site). SSO saves/deletes never affect it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Headless config saved"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public final Response saveHeadlessConfig(@Context final HttpServletRequest request,
                                             @Context final HttpServletResponse response,
                                             final Map<String, Object> form) {
        try {
            if (form == null || form.isEmpty()) {
                throw new IllegalArgumentException("Body required");
            }
            final User user = initUser(request, response);
            requireAdmin(user);
            final Host systemHost = APILocator.systemHost();
            appsAPI.saveSecrets(headlessHelper.buildSecrets(form), systemHost, user);
            SecurityLogger.logInfo(DotAuthResource.class,
                    String.format("User %s saved headless config", user.getUserId()));
            return Response.ok(new ResponseEntityStringView(OK)).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error saving headless config", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @DELETE
    @Path("/headless")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "clearDotAuthHeadlessConfig",
            summary = "Clear the system-level headless token-exchange configuration",
            description = "Deletes the headless config. SSO config is not affected.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Headless config cleared"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public final Response clearHeadlessConfig(@Context final HttpServletRequest request,
                                              @Context final HttpServletResponse response) {
        try {
            final User user = initUser(request, response);
            requireAdmin(user);
            final Host systemHost = APILocator.systemHost();
            appsAPI.deleteSecrets(DotAuthConstants.HEADLESS_APP_KEY, systemHost, user);
            SecurityLogger.logInfo(DotAuthResource.class,
                    String.format("User %s cleared headless config", user.getUserId()));
            return Response.noContent().build();
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error clearing headless config", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @POST
    @Path("/export")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON})
    @Operation(operationId = "exportDotAuthAppSecrets",
            summary = "Export all dotAuth AppSecrets",
            description = "Exports OAuth/OIDC, SAML, and headless dotAuth AppSecrets into one encrypted Apps export file.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Encrypted export file",
                    content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "400", description = "Invalid password or no secrets configured"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public final Response exportDotAuthSecrets(@Context final HttpServletRequest request,
                                               @Context final HttpServletResponse response,
                                               final Map<String, Object> form) {
        try {
            final User user = initUser(request, response);
            requireAdmin(user);
            final String password = exportPassword(form);
            final Map<String, Set<String>> appKeysBySite = dotAuthAppKeysBySite(user);
            final Key key = AppsUtil.generateKey(AppsUtil.loadPass(() -> password));
            final java.nio.file.Path exportFile = appsAPI.exportSecrets(key, false, appKeysBySite, user);
            SecurityLogger.logInfo(DotAuthResource.class,
                    String.format("User %s exported dotAuth AppSecrets", user.getUserId()));
            final StreamingOutput stream = (OutputStream out) -> {
                try (InputStream in = Files.newInputStream(exportFile)) {
                    in.transferTo(out);
                } finally {
                    Files.deleteIfExists(exportFile);
                }
            };
            return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=dotauth-appSecrets.export")
                    .build();
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error exporting dotAuth AppSecrets", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @POST
    @Path("/import")
    @JSONP
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "importDotAuthAppSecrets",
            summary = "Import dotAuth AppSecrets",
            description = "Imports an encrypted Apps export file containing only OAuth/OIDC, SAML, and headless dotAuth AppSecrets.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Import succeeded",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "object",
                                    description = "Import result with count of imported secrets"))),
            @ApiResponse(responseCode = "400", description = "Invalid password, empty file, or non-dotAuth secrets in file"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public final Response importDotAuthSecrets(@Context final HttpServletRequest request,
                                               @Context final HttpServletResponse response,
                                               final FormDataMultiPart form) {
        try {
            final User user = initUser(request, response);
            requireAdmin(user);
            final MultiPartUtils multiPartUtils = new MultiPartUtils();
            final Map<String, Object> body = multiPartUtils.getBodyMapFromMultipart(form);
            final String password = exportPassword(body);
            final Key key = AppsUtil.generateKey(AppsUtil.loadPass(() -> password));
            final List<File> files = multiPartUtils.getBinariesFromMultipart(form);
            if (files == null || files.isEmpty()) {
                throw new BadRequestException("Import file is required");
            }

            int imported = 0;
            try {
                for (final File file : files) {
                    if (file.length() == 0) {
                        throw new BadRequestException("Import file is empty");
                    }
                    imported += importDotAuthFile(file.toPath(), key, user);
                }
            } finally {
                cleanupUploadedFiles(files);
            }
            SecurityLogger.logInfo(DotAuthResource.class,
                    String.format("User %s imported %s dotAuth AppSecrets", user.getUserId(), imported));
            return Response.ok(new ResponseEntityView<>(Map.of("imported", imported))).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error importing dotAuth AppSecrets", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @POST
    @Path("/discover/oidc")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "discoverDotAuthOidc",
            summary = "Fetch and parse an OIDC discovery document",
            description = "Thin authenticated proxy used by the dotAuth portlet to populate " +
                    "issuer, endpoint, JWKS, and supported algorithm fields from a " +
                    ".well-known/openid-configuration URL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Discovery document parsed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "object",
                                    description = "Parsed OIDC discovery fields: issuer, endpoints, JWKS URI, and signing algorithms"))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid discovery URL"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public final Response discoverOidc(@Context final HttpServletRequest request,
                                       @Context final HttpServletResponse response,
                                       final Map<String, Object> form) {
        try {
            final User user = initUser(request, response);
            final String url = form == null ? null : String.valueOf(form.get("url"));
            if (!UtilMethods.isSet(url)) {
                throw new BadRequestException("Discovery URL is required");
            }
            validateProxyUrl(url);
            final String body = CircuitBreakerUrl.builder()
                    .setUrl(url)
                    .setTimeout(8_000)
                    .setHeaders(ImmutableMap.of("Accept", MediaType.APPLICATION_JSON))
                    .setMaxResponseBytes(OIDC_DISCOVERY_MAX_RESPONSE_BYTES)
                    .build()
                    .doString();
            final JsonNode doc = MAPPER.readTree(body);
            final Map<String, Object> entity = Map.of(
                    "issuer", text(doc, "issuer"),
                    "authorizationEndpoint", text(doc, "authorization_endpoint"),
                    "tokenEndpoint", text(doc, "token_endpoint"),
                    "jwksUri", text(doc, "jwks_uri"),
                    "userinfoEndpoint", text(doc, "userinfo_endpoint"),
                    "endSessionEndpoint", text(doc, "end_session_endpoint"),
                    "signingAlgs", doc.has("id_token_signing_alg_values_supported")
                            ? MAPPER.convertValue(
                                    doc.path("id_token_signing_alg_values_supported"), List.class)
                            : List.of());
            SecurityLogger.logInfo(DotAuthResource.class,
                    String.format("User %s ran dotAuth OIDC discovery", user.getUserId()));
            return Response.ok(new ResponseEntityView<>(entity)).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error discovering dotAuth OIDC metadata", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @POST
    @Path("/fetch/saml-metadata")
    @JSONP
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "fetchSamlMetadata",
            summary = "Fetch SAML IdP metadata XML from a URL",
            description = "Authenticated proxy that fetches SAML metadata XML from an IdP's "
                    + "metadata endpoint URL. Returns the raw XML as a string so the admin "
                    + "can review it before saving.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metadata fetched successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "object",
                                    description = "Object with a single 'xml' field containing the raw metadata XML"))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid metadata URL"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public final Response fetchSamlMetadata(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            final Map<String, Object> form) {
        try {
            final User user = initUser(request, response);
            final String url = form == null ? null : String.valueOf(form.get("url"));
            if (!UtilMethods.isSet(url)) {
                throw new BadRequestException("Metadata URL is required");
            }
            validateProxyUrl(url);
            final String xml = CircuitBreakerUrl.builder()
                    .setUrl(url)
                    .setTimeout(8_000)
                    .setHeaders(ImmutableMap.of("Accept",
                            "application/samlmetadata+xml, application/xml, text/xml"))
                    .setMaxResponseBytes(SAML_METADATA_MAX_RESPONSE_BYTES)
                    .build()
                    .doString();
            SecurityLogger.logInfo(DotAuthResource.class,
                    String.format("User %s fetched SAML metadata from %s", user.getUserId(), url));
            return Response.ok(new ResponseEntityView<>(Map.of("xml", xml))).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error fetching SAML metadata", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    @GET
    @Path("/saml/metadata/{hostId}")
    @NoCache
    @Produces({MediaType.APPLICATION_XML, "application/xml"})
    @Operation(operationId = "getSamlSpMetadata",
            summary = "Download SAML SP metadata XML for a site",
            description = "Generates SAML Service Provider metadata XML for the given site. "
                    + "For inherited configs the certificate comes from the SYSTEM_HOST row, but "
                    + "the entity ID and ACS URL use the target site's hostname so the IdP can "
                    + "distinguish per-site requests.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "SP metadata XML",
                    content = @Content(mediaType = "application/xml")),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "No SAML configuration found for this site"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public final Response getSamlSpMetadata(@Context final HttpServletRequest request,
                                            @Context final HttpServletResponse response,
                                            @PathParam("hostId") final String hostId) {
        try {
            final User user = initUser(request, response);
            final Host host = resolveHost(hostId, user);

            final ProtocolHandler samlHandler = handlers.get(DotAuthProtocol.SAML);
            final Optional<AppSecrets> secrets = appsAPI.getSecrets(
                    samlHandler.appKey(), true, host, user);
            if (secrets.isEmpty()) {
                throw new DoesNotExistException(
                        "No SAML configuration found for site " + hostId);
            }

            final Map<String, Object> vals = samlHandler.maskedValues(secrets.get());
            // The ACS must mirror what the SAML runtime actually serves:
            // https://<sPEndpointHostname>/dotsaml/login/<hostId> — the configured SP
            // endpoint hostname (which may carry a port), NOT the site's bare hostname,
            // and always with the site-id path segment the interceptor routes on.
            final String spEndpointHostname = String.valueOf(vals.getOrDefault("sPEndpointHostname", ""))
                    .replaceFirst("(?i)^https?://", "")
                    .replaceAll("/+$", "");
            final String hostname = UtilMethods.isSet(spEndpointHostname)
                    ? spEndpointHostname
                    : host.getHostname();

            final String storedEntityId = String.valueOf(vals.getOrDefault("sPIssuerURL", ""));
            final String entityId = UtilMethods.isSet(storedEntityId)
                    ? storedEntityId : "https://" + hostname;
            final String acsUrl = "https://" + hostname + "/dotsaml/login/" + host.getIdentifier();

            final String certPem = String.valueOf(vals.getOrDefault("publicCert", ""));
            // Strip PEM armor/whitespace, then constrain to the base64 alphabet so a
            // stored value containing XML metacharacters cannot break (or inject markup
            // into) the generated SP-metadata document. A real certificate is unaffected.
            final String certBase64 = certPem
                    .replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s+", "")
                    .replaceAll("[^A-Za-z0-9+/=]", "");

            final String sigType = String.valueOf(vals.getOrDefault("signatureValidationType", ""));
            final boolean wantAssertionsSigned =
                    "assertion".equalsIgnoreCase(sigType)
                            || "responseandassertion".equalsIgnoreCase(sigType);
            final boolean wantResponseSigned =
                    "response".equalsIgnoreCase(sigType)
                            || "responseandassertion".equalsIgnoreCase(sigType);

            final String xml = buildSpMetadataXml(
                    entityId, acsUrl, certBase64, wantAssertionsSigned, wantResponseSigned);

            return Response.ok(xml, MediaType.APPLICATION_XML_TYPE)
                    .header("Content-Disposition",
                            "attachment; filename=\"saml-sp-metadata-" + hostname + ".xml\"")
                    .build();
        } catch (final Exception e) {
            Logger.error(this.getClass(),
                    "Error generating SAML SP metadata for " + hostId, e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private static String buildSpMetadataXml(final String entityId,
                                              final String acsUrl,
                                              final String certBase64,
                                              final boolean wantAssertionsSigned,
                                              final boolean wantResponseSigned) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\"\n");
        sb.append("                     entityID=\"").append(xmlEscape(entityId)).append("\">\n");
        sb.append("  <md:SPSSODescriptor");
        sb.append(" AuthnRequestsSigned=\"").append(wantResponseSigned || wantAssertionsSigned).append("\"");
        sb.append(" WantAssertionsSigned=\"").append(wantAssertionsSigned).append("\"");
        sb.append(" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n");
        if (UtilMethods.isSet(certBase64)) {
            sb.append("    <md:KeyDescriptor use=\"signing\">\n");
            sb.append("      <ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n");
            sb.append("        <ds:X509Data>\n");
            sb.append("          <ds:X509Certificate>").append(xmlEscape(certBase64)).append("</ds:X509Certificate>\n");
            sb.append("        </ds:X509Data>\n");
            sb.append("      </ds:KeyInfo>\n");
            sb.append("    </md:KeyDescriptor>\n");
        }
        sb.append("    <md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</md:NameIDFormat>\n");
        sb.append("    <md:AssertionConsumerService\n");
        sb.append("        Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n");
        sb.append("        Location=\"").append(xmlEscape(acsUrl)).append("\"\n");
        sb.append("        index=\"0\" isDefault=\"true\" />\n");
        sb.append("  </md:SPSSODescriptor>\n");
        sb.append("</md:EntityDescriptor>\n");
        return sb.toString();
    }

    private static String xmlEscape(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    @POST
    @Path("/sessionrefs/revoke")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Operation(operationId = "revokeDotAuthSessionRefs",
            summary = "Revoke all dotAuth sessionRefs",
            description = "Flushes the dotAuth sessionRef cache. Existing browser sessions are not affected.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All session-refs revoked",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResponseEntityStringView.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Admin access required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public final Response revokeAllSessionRefs(@Context final HttpServletRequest request,
                                               @Context final HttpServletResponse response) {
        try {
            final User user = initUser(request, response);
            requireAdmin(user);
            sessionCache.invalidateAll();
            SecurityLogger.logInfo(DotAuthResource.class,
                    String.format("User %s revoked all dotAuth sessionRefs", user.getUserId()));
            return Response.ok(new ResponseEntityStringView(OK)).build();
        } catch (final Exception e) {
            Logger.error(this.getClass(), "Error revoking dotAuth sessionRefs", e);
            return ResponseUtil.mapExceptionResponse(e);
        }
    }

    private Optional<DotAuthProtocol> detectProtocol(final Set<String> hostKeys) {
        for (final ProtocolHandler handler : handlers.values()) {
            if (hostKeys.contains(handler.appKey().toLowerCase())) {
                return Optional.of(handler.protocol());
            }
        }
        return Optional.empty();
    }

    private Map<String, Set<String>> dotAuthAppKeysBySite(final User user)
            throws DotDataException, DotSecurityException {
        final Map<String, Set<String>> selected = new LinkedHashMap<>();
        final Map<String, Set<String>> appKeysByHost = appsAPI.appKeysByHost();
        final String systemHostIdentifier = APILocator.systemHost().getIdentifier();

        for (final Map.Entry<String, Set<String>> entry : appKeysByHost.entrySet()) {
            final Set<String> dotAuthKeys = new LinkedHashSet<>();
            for (final String key : entry.getValue()) {
                canonicalDotAuthAppKey(key).ifPresent(dotAuthKeys::add);
            }
            if (!dotAuthKeys.isEmpty()) {
                final String siteId = entry.getKey().equalsIgnoreCase(systemHostIdentifier)
                        ? Host.SYSTEM_HOST
                        : entry.getKey();
                selected.put(siteId, dotAuthKeys);
            }
        }

        if (selected.isEmpty()) {
            throw new BadRequestException("No dotAuth AppSecrets are configured to export");
        }
        return selected;
    }

    // Package-private for unit testing (see DotAuthResourceTest).
    int importDotAuthFile(final java.nio.file.Path importFile, final Key key, final User user)
            throws Exception {
        int count = 0;
        final Map<String, List<AppSecrets>> importedSecretsBySiteId =
                AppsUtil.importSecrets(importFile, key);
        for (final Map.Entry<String, List<AppSecrets>> entry : importedSecretsBySiteId.entrySet()) {
            final Host host = Host.SYSTEM_HOST.equalsIgnoreCase(entry.getKey())
                    ? APILocator.systemHost()
                    : APILocator.getHostAPI().find(entry.getKey(), user, false);
            if (host == null) {
                throw new BadRequestException("No site found for imported id `" + entry.getKey() + "`");
            }
            for (final AppSecrets appSecrets : entry.getValue()) {
                if (appSecrets == null || appSecrets.getSecrets().isEmpty()) {
                    throw new BadRequestException("Incoming dotAuth AppSecrets entry is empty");
                }
                final String canonicalKey = canonicalDotAuthAppKey(appSecrets.getKey())
                        .orElseThrow(() -> new BadRequestException(
                                "Import file contains non-dotAuth AppSecrets key `"
                                        + appSecrets.getKey() + "`"));
                if (DotAuthConstants.HEADLESS_APP_KEY.equals(canonicalKey) && !host.isSystemHost()) {
                    throw new BadRequestException("Headless dotAuth config may only be imported to SYSTEM_HOST");
                }
                // No App Descriptor check here: dotAuth OAuth/headless configs store secrets
                // directly (no descriptor YAML exists, only SAML has one), mirroring the save
                // path which never requires a descriptor. canonicalDotAuthAppKey already
                // restricts canonicalKey to the known dotAuth keys.
                appsAPI.saveSecrets(AppSecrets.builder()
                        .withKey(canonicalKey)
                        .withSecrets(appSecrets.getSecrets())
                        .build(), host, user);
                count++;
            }
        }
        return count;
    }

    private static Optional<String> canonicalDotAuthAppKey(final String appKey) {
        if (appKey == null) {
            return Optional.empty();
        }
        return DOTAUTH_APP_KEYS.stream()
                .filter(key -> key.equalsIgnoreCase(appKey))
                .findFirst();
    }

    private static String exportPassword(final Map<String, Object> form) {
        final Object raw = form == null ? null : form.get("password");
        final String password = raw == null ? null : String.valueOf(raw);
        if (password == null
                || password.length() < EXPORT_PASSWORD_MIN_LENGTH
                || password.length() > EXPORT_PASSWORD_MAX_LENGTH) {
            throw new BadRequestException(String.format(
                    "Password must be between %s and %s characters",
                    EXPORT_PASSWORD_MIN_LENGTH, EXPORT_PASSWORD_MAX_LENGTH));
        }
        return password;
    }

    private static void requireAdmin(final User user) throws DotSecurityException {
        if (user == null || !user.isAdmin()) {
            throw new DotSecurityException("Only Admins are allowed to import dotAuth AppSecrets");
        }
    }

    private static void cleanupUploadedFiles(final List<File> files) {
        for (final File file : files) {
            if (file == null) {
                continue;
            }
            final File parent = file.getParentFile();
            if (!file.delete()) {
                Logger.debug(DotAuthResource.class,
                        "Unable to remove uploaded dotAuth import file `" + file.getAbsolutePath() + "`");
            }
            if (parent != null && parent.isDirectory() && parent.getName().startsWith("tmp_upload")) {
                parent.delete();
            }
        }
    }

    private static String text(final JsonNode doc, final String field) {
        final JsonNode value = doc.path(field);
        return value.isTextual() ? value.asText() : "";
    }

    private Optional<AppSecrets> secretsToPreserve(final ProtocolHandler active,
                                                   final Host host,
                                                   final User user)
            throws DotDataException, DotSecurityException {
        return appsAPI.getSecrets(active.appKey(), true, host, user);
    }

    private Optional<ProtocolConfig> findConfiguredProtocol(final Host host,
                                                            final User user,
                                                            final boolean fallbackOnSystemHost)
            throws DotDataException, DotSecurityException {
        ProtocolConfig selected = null;
        long selectedSavedAt = Long.MIN_VALUE;
        for (final ProtocolHandler handler : handlers.values()) {
            final Optional<AppSecrets> secrets = appsAPI.getSecrets(
                    handler.appKey(), fallbackOnSystemHost, host, user);
            if (secrets.isEmpty()) {
                continue;
            }
            final long savedAt = lastSavedAt(secrets.get());
            if (selected == null || savedAt > selectedSavedAt) {
                selected = new ProtocolConfig(
                        handler.protocol(), handler.maskedValues(secrets.get()));
                selectedSavedAt = savedAt;
            }
        }
        return Optional.ofNullable(selected);
    }

    private static AppSecrets withLastSavedMarker(final AppSecrets appSecrets) {
        return AppSecrets.builder()
                .withKey(appSecrets.getKey())
                .withSecrets(appSecrets.getSecrets())
                .withSecret(DotAuthConstants.LAST_SAVED_PROTOCOL_AT_KEY,
                        Secret.builder()
                                .withValue(String.valueOf(System.currentTimeMillis()))
                                .withHidden(false)
                                .withType(Type.STRING)
                                .build())
                .build();
    }

    private static long lastSavedAt(final AppSecrets appSecrets) {
        return Optional.ofNullable(appSecrets.getSecrets()
                        .get(DotAuthConstants.LAST_SAVED_PROTOCOL_AT_KEY))
                .map(secret -> {
                    try {
                        return Long.parseLong(secret.getString());
                    } catch (final NumberFormatException e) {
                        return Long.MIN_VALUE;
                    }
                })
                .orElse(Long.MIN_VALUE);
    }

    private static final class ProtocolConfig {
        private final DotAuthProtocol protocol;
        private final Map<String, Object> values;

        private ProtocolConfig(final DotAuthProtocol protocol, final Map<String, Object> values) {
            this.protocol = protocol;
            this.values = values;
        }
    }

    private User initUser(final HttpServletRequest request, final HttpServletResponse response) {
        final InitDataObject initData = new WebResource.InitBuilder(webResource)
                .requiredBackendUser(true)
                .requiredFrontendUser(false)
                .requiredPortlet(PortletID.DOT_AUTH.toString())
                .requestAndResponse(request, response)
                .rejectWhenNoUser(true)
                .init();
        return initData.getUser();
    }

    private static void validateProxyUrl(final String url) {
        final String rejection = OAuthSsrfGuard.validateUrl(url);
        if (rejection != null) {
            throw new BadRequestException(rejection);
        }
    }

    private Host resolveHost(final String hostId, final User user)
            throws DotDataException, DotSecurityException {
        if (SYSTEM_HOST_SENTINEL.equalsIgnoreCase(hostId)) {
            return APILocator.systemHost();
        }
        final Host host = APILocator.getHostAPI().find(hostId, user, false);
        if (host == null) {
            throw new DoesNotExistException(String.format("No site found for id `%s`", hostId));
        }
        return host;
    }
}
