package com.dotcms.analytics.helper;

import com.dotcms.analytics.AnalyticsAPI;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AccessTokenErrorType;
import com.dotcms.analytics.model.AccessTokenStatus;
import com.dotcms.analytics.model.AnalyticsKey;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.auth.providers.jwt.JsonWebTokenAuthCredentialProcessor;
import com.dotcms.exception.AnalyticsException;
import com.dotcms.exception.UnrecoverableAnalyticsException;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.WebResource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Logger;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Helper class for analytics matters.
 *
 * @author vico
 */
public class AnalyticsHelper {

    private static final Lazy<AnalyticsHelper> analyticsHelper = Lazy.of(AnalyticsHelper::new);

    public static AnalyticsHelper get(){
        return analyticsHelper.get();
    }

    private  AnalyticsHelper() {}

    /**
     * Evaluates if a given status code instance has a http status within the SUCCESSFUL range.
     *
     * @param statusCode http status code
     * @return true if the response http status is considered tobe successful, otherwise false
     */
    public boolean isSuccessResponse(final int statusCode) {
        return Response.Status.Family.familyOf(statusCode) == Response.Status.Family.SUCCESSFUL;
    }

    /**
     * Evaluates if a given status code instance has a http status within the SUCCESSFUL range.
     *
     * @param response http response representation
     * @return true if the response http status is considered tobe successful, otherwise false
     */
    public boolean isSuccessResponse(@NotNull final CircuitBreakerUrl.Response<?> response) {
        return isSuccessResponse(response.getStatusCode());
    }

    /**
     * Given a {@link CircuitBreakerUrl.Response<AccessToken>} instance, extracts JSON representing the token and
     * deserializes to {@link AccessToken}.
     *
     * @param response http response representation
     * @return an {@link Optional<AccessToken>} instance holding the access token data
     */
    public Optional<AccessToken> extractToken(final CircuitBreakerUrl.Response<AccessToken> response) {
        Objects.requireNonNull(response, "ACCESS_TOKEN response is missing");
        return Optional.ofNullable(response.getResponse());
    }

    /**
     * Given a {@link CircuitBreakerUrl.Response<AccessToken>} instance, extracts JSON representing the token and deserializes to {@link AnalyticsKey}.
     *
     * @param response http response representation
     * @return an {@link Optional<AnalyticsKey>} instance holding the analytics key data
     */
    public Optional<AnalyticsKey> extractAnalyticsKey(final CircuitBreakerUrl.Response<AnalyticsKey> response) {
        Objects.requireNonNull(response, "ANALYTICS_KEY response is missing");
        return Optional.ofNullable(response.getResponse());
    }

    /**
     * Given an {@link AccessToken} evaluates if it's expired by comparing issue date to current date time.
     *
     * @param accessToken provided access token
     * @return true if current time is in the TTL window
     */
    private boolean filterIssueDate(final AccessToken accessToken, final BiPredicate<Instant, Instant> filter) {
        return Optional.ofNullable(accessToken.issueDate())
            .map(issuedAt -> {
                final Instant now = Instant.now();
                final Instant expireDate = issuedAt.plusSeconds(AnalyticsAPI.ANALYTICS_ACCESS_TOKEN_TTL);
                return now.isBefore(expireDate) && (filter == null || filter.test(now, expireDate));
            })
            .orElseGet(() -> {
                Logger.warn(AnalyticsHelper.class, "ACCESS_TOKEN does not have a issued date, filtering token out");
                return false;
            });
    }

    /**
     * Given an {@link AccessToken} evaluates if it's expired by comparing issue date to current date time.
     *
     * @param accessToken provided access token
     * @return true if current time is in the TTL window
     */
    public boolean hasTokenExpired(final AccessToken accessToken) {
        return !filterIssueDate(accessToken, null);
    }

    /**
     * Evaluates if {@link AccessToken} instance is in the expiring window based on a config property defined offset.
     *
     * @param accessToken provided access token
     * @return true if access token is not expired and if it's after the initial mark of the expiring window
     */
    public boolean isTokenInWindow(final AccessToken accessToken) {
        return filterIssueDate(
            accessToken,
            (now, expireDate) -> now.isAfter(expireDate.minusSeconds(AnalyticsAPI.ANALYTICS_ACCESS_TOKEN_TTL_WINDOW)));
    }

    /**
     * Gets a an Base 64 encoded version of `clientId:clientSecret`.
     *
     * @param clientId client id
     * @param clientSecret client secret
     * @return String representation of base 64 bytes
     */
    public String encodeClientIdAndSecret(final String clientId, final String clientSecret) {
        return Base64.getEncoder()
            .encodeToString(
                String
                    .format(
                        "%s:%s",
                        clientId,
                        clientSecret)
                    .getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Given an {@link AccessToken} instance resolves its status by evaluating first if it's OK and check its issueDate
     * to determine if it's expired. If not just gets the actual {@link TokenStatus} and if null
     * return {@link TokenStatus#OK}.
     *
     * @param accessToken provided access token
     * @return resolved token status
     */
    public TokenStatus resolveTokenStatus(final AccessToken accessToken) {
        if (Objects.isNull(accessToken)) {
            return TokenStatus.NONE;
        }

        if (isTokenOk(accessToken)) {
            if (hasTokenExpired(accessToken)) {
                return TokenStatus.EXPIRED;
            }

            if (isTokenInWindow(accessToken)) {
                return TokenStatus.IN_WINDOW;
            }

            return TokenStatus.OK;
        }

        return Optional.ofNullable(accessToken.status()).map(AccessTokenStatus::tokenStatus).orElse(TokenStatus.NONE);
    }

    /**
     * Given an {@link AccessToken} instance based on some checks evaluates if access token can be used.
     *
     * @param accessToken provided access token
     */
    public void checkAccessToken(final AccessToken accessToken) throws AnalyticsException {
        final TokenStatus tokenStatus = resolveTokenStatus(accessToken);

        if (!canUseToken(tokenStatus)) {
            throw new AnalyticsException(
                String.format(
                    "ACCESS_TOKEN for clientId %s is %s",
                    accessToken.clientId(),
                    tokenStatus.name()));
        }
    }

    /**
     * Evaluates if provided {@link TokenStatus} is {@link TokenStatus#OK} or {@link TokenStatus#IN_WINDOW}
     *
     * @param tokenStatus token status
     * @return true if it has a {@link TokenStatus#OK} or {@link TokenStatus#IN_WINDOW}
     */
    private static boolean canUseToken(final TokenStatus tokenStatus) {
        return tokenStatus.matchesAny(TokenStatus.OK, TokenStatus.IN_WINDOW);
    }

    /**
     * Extracts actual access token value from {@link AccessToken} and prepends the "Bearer " prefix to be used
     * when add in the corresponding header.
     *
     * @param accessToken provided access token
     * @return the actual string value of token for header usage
     * @throws AnalyticsException when validating token
     */
    public String formatBearer(final AccessToken accessToken) throws AnalyticsException {
        checkAccessToken(accessToken);
        return JsonWebTokenAuthCredentialProcessor.BEARER + accessToken.accessToken();
    }

    /**
     * Extract actual analytics key from {@link AnalyticsKey} and prepends the "Basic " prefix to be used when sending
     * analytics data.
     *
     * @param analyticsKey the analytics key
     * @return the actual string value of key for header usage
     */
    public String formatBasic(final AnalyticsKey analyticsKey) {
        return WebResource.BASIC + analyticsKey.jsKey();
    }

    /**
     * Creates a {@link AnalyticsApp} instance associated with provided host.
     *
     * @param host provided host
     * @return associated host app
     */
    public AnalyticsApp appFromHost(final Host host) {
        return new AnalyticsApp(host);
    }

    /**
     * Throws an instance of {@link AnalyticsException} when the response are not considered successful and based on
     * https status code in response resolve to whether it is an {@link AnalyticsException} extending class.
     *
     * @param response {@link CircuitBreakerUrl.Response} instance to evaluate
     * @throws AnalyticsException when not a successful response is detected
     */
    public void throwFromResponse(final CircuitBreakerUrl.Response<AccessToken> response,
                                         final String message) throws AnalyticsException {
        if (isSuccessResponse(response)) {
            return;
        }

        final String reasonText = StringUtils.isNotBlank(message)
            ? String.format(" (due to: %s)", message)
            : StringPool.BLANK;
        final String finalMessage = String.format(
            "Response error detected%s, status code: %d",
            reasonText,
            response.getStatusCode());
        switch (response.getStatusCode()) {
            case HttpStatus.SC_BAD_REQUEST:
            case HttpStatus.SC_UNAUTHORIZED:
                throw new UnrecoverableAnalyticsException(finalMessage, response.getStatusCode());
            default:
                throw new AnalyticsException(finalMessage, response.getStatusCode());
        }
    }

    /**
     * Creates a BLOCKED {@link AccessToken} with a {@link TokenStatus#NOOP} status, provided app's clientId
     * and a provided error message.
     *
     * @param analyticsApp provided analytics app
     * @param reason error message
     * @return noop access token
     */
    public AccessToken createNoopToken(final AnalyticsApp analyticsApp, final String reason) {
        return AccessToken.builder()
            .accessToken(StringPool.BLANK)
            .tokenType(StringPool.BLANK)
            .expiresIn(0)
            .scope(StringPool.BLANK)
            .clientId(Optional.ofNullable(analyticsApp)
                .map(app -> app.getAnalyticsProperties().clientId())
                .orElse(null))
            .aud(AnalyticsHelper.get().resolveAudience(analyticsApp))
            .status(
                AccessTokenStatus.builder()
                    .tokenStatus(TokenStatus.NOOP)
                    .errorType(AccessTokenErrorType.PERMANENT_ERROR)
                    .reason(reason)
                    .build())
            .build();
    }

    /**
     * Creates a BLOCKED {@link AccessToken} with a {@link TokenStatus#BLOCKED} status, provided app's clientId
     * and a provided error message.
     *
     * @param analyticsApp provided analytics app
     * @param reason error message
     * @return blocked access token
     */
    public AccessToken createBlockedToken(final AnalyticsApp analyticsApp, final String reason) {
        return AccessToken.builder()
            .accessToken(StringPool.BLANK)
            .tokenType(StringPool.BLANK)
            .expiresIn(0)
            .scope(StringPool.BLANK)
            .clientId(Optional.ofNullable(analyticsApp)
                .map(app -> app.getAnalyticsProperties().clientId())
                .orElse(null))
            .aud(AnalyticsHelper.get().resolveAudience(analyticsApp))
            .status(
                AccessTokenStatus.builder()
                    .tokenStatus(TokenStatus.BLOCKED)
                    .errorType(AccessTokenErrorType.TEMPORARY_ERROR)
                    .reason(reason)
                    .build())
            .build();
    }

    /**
     * Evaluates if provided {@link AccessToken} has a {@link TokenStatus#OK} status.
     *
     * @param accessToken provided access token
     * @return true if access token status is NOOP, otherwise false
     */
    public boolean isTokenOk(final AccessToken accessToken) {
        return accessTokenHasStatus(accessToken, TokenStatus.OK);
    }

    /**
     * Evaluates if provided {@link AccessToken} has a {@link TokenStatus#NOOP} status.
     *
     * @param accessToken provided access token
     * @return true if access token status is NOOP, otherwise false
     */
    public boolean isTokenNoop(final AccessToken accessToken) {
        return accessTokenHasStatus(accessToken, TokenStatus.NOOP);
    }

    /**
     * Evaluates if provided {@link AccessToken} has a {@link TokenStatus#BLOCKED} status.
     *
     * @param accessToken provided access token
     * @return true if access token status is BLOCKED, otherwise false
     */
    public boolean isTokenBlocked(final AccessToken accessToken) {
        return accessTokenHasStatus(accessToken, TokenStatus.BLOCKED);
    }

    /**
     * Checks if a given {@link AccessToken} instance has the provided {@link TokenStatus}.
     *
     * @param accessToken provided access token
     * @param tokenStatus provided token status
     * @return true if the access token has the token status, otherwise the false
     */
    private boolean accessTokenHasStatus(final AccessToken accessToken, final TokenStatus tokenStatus) {
        return Optional
            .ofNullable(accessToken.status())
            .map(s -> s.tokenStatus() == tokenStatus)
            .orElse(false);
    }

    /**
     * Given an {@link AccessToken} instance extracts the status information and creates a
     * {@link String} representation.
     *
     * @param accessToken provided access token
     * @return status text
     */
    public String resolveStatusMessage(final AccessToken accessToken) {
        final StringBuilder sb = new StringBuilder("ACCESS_TOKEN for clientId ").append(accessToken.clientId());
        Optional
            .ofNullable(accessToken.status())
            .ifPresent(status -> {
                sb.append(" is currently ").append(status.tokenStatus());
                if (Objects.nonNull(status.errorType())) {
                    sb.append(" due to ").append(status.errorType());
                }
                if (StringUtils.isNotBlank(status.reason())) {
                    sb.append(" (").append(status.reason()).append(")");
                }
            });

        return sb.toString();
    }

    /**
     * Resolves aud value to be used when caching access token.
     * TODO: since it's not clear what value should be, this method needs to be updated to return some actual value
     *
     * @param analyticsApp analytics app to get the aud from
     * @return audience value
     */
    public String resolveAudience(final AnalyticsApp analyticsApp) {
        return null;
    }

}
