package com.dotcms.auth.dotAuth.rest;

import com.dotcms.auth.dotAuth.session.DotAuthSessionCache;
import com.dotmarketing.business.CacheLocator;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.util.SecurityLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.ContainerRequest;

/**
 * Logout companion to {@link DotAuthOAuthExchangeResource}. Invalidates the
 * dotAuth session-ref carried in the {@code Authorization: Bearer} header by
 * removing its entry from {@link DotAuthSessionCache}. Safe to call
 * unconditionally — unknown or malformed refs are silently ignored so a
 * "sign out" button does not need to care whether the session had already
 * expired server-side.
 *
 * <h3>Authentication trade-off</h3>
 * This endpoint trusts the bearer header without any additional authentication
 * check. An attacker in possession of a stolen session-ref can use it here to
 * invalidate the legitimate user's session (forced logout / DoS with no recovery
 * other than re-authenticating against the IdP). The impact is strictly weaker
 * than the impersonation that same bearer already affords the attacker until it
 * expires, which is why we accept it — but the property is worth stating plainly
 * so a future hardening pass knows this was an explicit choice rather than an
 * oversight.
 */
@Path("/v1/dotauth/oauth")
@Tag(name = "dotAuth", description = "OAuth/OIDC token exchange for headless SPA consumers")
public class DotAuthSessionLogoutResource implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String BEARER = "Bearer ";

    private final DotAuthSessionCache sessionCache = CacheLocator.getDotAuthSessionCache();

    @DELETE
    @Path("/session")
    @NoCache
    @Operation(
            summary = "Invalidate the caller's dotAuth session-ref",
            description = "Removes the session referenced by the Authorization: Bearer header "
                    + "from the in-memory session cache. Always returns 204 — unknown or "
                    + "missing refs are silently ignored so this is safe to call "
                    + "unconditionally on sign-out.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Session invalidated (or was unknown)")
    })
    public Response logout(@Context final HttpServletRequest request) {
        final String header = request.getHeader(ContainerRequest.AUTHORIZATION);
        if (StringUtils.isNotEmpty(header) && header.startsWith(BEARER)) {
            sessionCache.invalidate(header.substring(BEARER.length()).trim());
            SecurityLogger.logInfo(DotAuthSessionLogoutResource.class,
                    "dotAuth session-ref logout requested from " + request.getRemoteAddr());
        }
        return Response.noContent().build();
    }
}
