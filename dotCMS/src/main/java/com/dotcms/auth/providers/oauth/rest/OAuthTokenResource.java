package com.dotcms.auth.providers.oauth.rest;

import com.dotcms.auth.providers.jwt.JsonWebTokenUtils;
import com.dotcms.auth.providers.oauth.OAuthAppConfig;
import com.dotcms.auth.providers.oauth.OAuthHelper;
import com.dotcms.auth.providers.oauth.provider.GenericOAuth2Provider;
import com.dotcms.auth.providers.oauth.provider.OAuthProvider;
import com.dotcms.auth.providers.oauth.provider.OIDCProvider;
import com.dotcms.cms.login.LoginServiceAPI;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.SecurityLogger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST endpoint that exchanges a provider-issued OAuth access token for a dotCMS
 * Json Web Token. The caller must have already completed the OAuth flow with the
 * identity provider and obtained an access token.
 * <pre>
 * curl -XPOST http://localhost:8080/api/v1/oauth/token \
 *   -H "Content-Type:application/json" \
 *   -d '{"oauthToken":"eyJ...","expirationDays":10}'
 * </pre>
 */
@Path("/v1/oauth")
@Tag(name = "Authentication")
public class OAuthTokenResource implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int  MAX_ALLOWED_EXPIRATION_DAYS_DEFAULT = 30;

    private final OAuthHelper       oauthHelper = new OAuthHelper();
    private final JsonWebTokenUtils jwtUtils    = JsonWebTokenUtils.getInstance();

    @POST
    @Path("/token")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Exchange an OAuth access token for a dotCMS JWT",
            description = "Resolves the dotCMS user from the provider userinfo response and issues a JWT bound to that user.")
    public Response getToken(@Context final HttpServletRequest request,
                             @Context final HttpServletResponse response,
                             final TokenForm form) {
        try {
            if (form == null || !UtilMethods.isSet(form.getOauthToken())) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ResponseEntityView<>("oauthToken is required")).build();
            }

            final Optional<OAuthAppConfig> cfgOpt = OAuthAppConfig.config(request);
            if (cfgOpt.isEmpty()) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(new ResponseEntityView<>("OAuth is not configured for this site")).build();
            }
            final OAuthAppConfig config = cfgOpt.get();

            final OAuthProvider provider = config.isOidc()
                    ? new OIDCProvider(config.issuerUrl, config.clientId, config.clientSecret,
                            config.groupsClaim, config.groupsUrl)
                    : new GenericOAuth2Provider(config.clientId, config.clientSecret,
                            config.authorizationUrl, config.tokenUrl, config.userinfoUrl,
                            config.revocationUrl, config.logoutUrl,
                            config.groupsClaim, config.groupsUrl);

            final Map<String, Object> userInfo = provider.getUserInfo(form.getOauthToken());
            final User user = oauthHelper.authenticate(request, response, provider,
                    form.getOauthToken(), userInfo, config, false);

            final int expirationDays = clampExpirationDays(form.getExpirationDays());
            final String jwt = jwtUtils.createUserToken(user, expirationDays);

            SecurityLogger.logInfo(OAuthTokenResource.class,
                    "OAuth JWT issued for user " + user.getUserId() + " from " + request.getRemoteAddr());

            return Response.ok(new ResponseEntityView<>(Collections.singletonMap("token", jwt))).build();
        } catch (final Exception e) {
            Logger.warn(OAuthTokenResource.class, "OAuth token exchange failed: " + e.getMessage());
            return ExceptionMapperUtil.createResponse(e, Response.Status.UNAUTHORIZED);
        }
    }

    private int clampExpirationDays(final int requested) {
        final int defaultDays = Config.getIntProperty(
                LoginServiceAPI.JSON_WEB_TOKEN_DAYS_MAX_AGE,
                LoginServiceAPI.JSON_WEB_TOKEN_DAYS_MAX_AGE_DEFAULT);
        final int maxAllowed = Config.getIntProperty(
                LoginServiceAPI.JSON_WEB_TOKEN_MAX_ALLOWED_EXPIRATION_DAYS,
                MAX_ALLOWED_EXPIRATION_DAYS_DEFAULT);
        if (requested <= 0) {
            return defaultDays;
        }
        return maxAllowed > 0 && requested > maxAllowed ? maxAllowed : requested;
    }
}
