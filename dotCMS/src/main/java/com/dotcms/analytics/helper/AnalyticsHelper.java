package com.dotcms.analytics.helper;

import com.dotcms.analytics.AnalyticsAPI;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AccessTokenErrorType;
import com.dotcms.analytics.model.AccessTokenStatus;
import com.dotcms.analytics.model.AnalyticsKey;
import com.dotcms.analytics.model.TokenStatus;
import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.exception.AnalyticsException;
import com.dotcms.exception.UnrecoverableAnalyticsException;
import com.dotcms.http.CircuitBreakerUrl;
import com.dotcms.rest.WebResource;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;

/**
 * Helper class for analytics matters.
 *
 * @author vico
 */
public class AnalyticsHelper implements EventSubscriber<SystemTableUpdatedKeyEvent> {

    private static final Lazy<AnalyticsHelper> INSTANCE = Lazy.of(AnalyticsHelper::new);

    public static AnalyticsHelper get(){
        return INSTANCE.get();
    }

    private final AtomicLong accessTokenTtl;
    private final AtomicLong accessTokenTtlWindow;

    private AnalyticsHelper() {
        accessTokenTtl = new AtomicLong(resolveAccessTokenTtl());
        accessTokenTtlWindow = new AtomicLong(resolveAccessTokenTtlWindow());
        APILocator.getLocalSystemEventsAPI().subscribe(SystemTableUpdatedKeyEvent.class, this);
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
     * Given a {@link CircuitBreakerUrl.Response<AccessToken>} instance, extracts JSON representing the token
     * and deserializes to {@link AnalyticsKey}.
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
        final long tokenTtl = Optional.ofNullable(accessToken.expiresIn().longValue()).orElse(accessTokenTtl.get());
        return Optional.ofNullable(accessToken.issueDate())
            .map(issuedAt -> {
                final Instant now = Instant.now();
                final Instant expireDate = issuedAt.plusSeconds(tokenTtl);
                return now.isBefore(expireDate) &&
                        Optional.ofNullable(filter).map(f -> f.test(now, expireDate)).orElse (true);
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
            (now, expireDate) -> now.isAfter(expireDate.minusSeconds(accessTokenTtlWindow.get())));
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

    @Override
    public void notify(final SystemTableUpdatedKeyEvent event) {
        if (event.getKey().contains(AnalyticsAPI.ANALYTICS_ACCESS_TOKEN_TTL_KEY)) {
            accessTokenTtl.set(resolveAccessTokenTtl());
        } else if (event.getKey().contains(AnalyticsAPI.ANALYTICS_ACCESS_TOKEN_TTL_WINDOW_KEY)) {
            accessTokenTtlWindow.set(resolveAccessTokenTtlWindow());
        }
    }

    private long resolveAccessTokenTtl() {
        return Config.getLongProperty(AnalyticsAPI.ANALYTICS_ACCESS_TOKEN_TTL_KEY, TimeUnit.HOURS.toSeconds(1));
    }

    private long resolveAccessTokenTtlWindow() {
        return Config.getLongProperty(
                AnalyticsAPI.ANALYTICS_ACCESS_TOKEN_TTL_WINDOW_KEY,
                TimeUnit.MINUTES.toSeconds(1));
    }

    /**
     * Evaluates if provided {@link TokenStatus} is {@link TokenStatus#OK} or {@link TokenStatus#IN_WINDOW}
     *
     * @param tokenStatus token status
     * @return true if it has a {@link TokenStatus#OK} or {@link TokenStatus#IN_WINDOW}
     */
    private boolean canUseToken(final TokenStatus tokenStatus) {
        return tokenStatus.matchesAny(TokenStatus.OK, TokenStatus.IN_WINDOW);
    }

    /**
     * Extracts actual access token value from {@link AccessToken} and prepends the "Bearer " prefix to be used
     * when add in the corresponding header.
     *
     * @param accessToken provided access token
     * @param type token type (most of times it will be 'Bearer')
     * @return the actual string value of token for header usage
     * @throws AnalyticsException when validating token
     */
    public String formatToken(final AccessToken accessToken, final String type) throws AnalyticsException {
        checkAccessToken(accessToken);
        return StringUtils.defaultIfBlank(type, StringPool.BLANK) + accessToken.accessToken();
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
        return formatToken(accessToken, AnalyticsAPI.BEARER);
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
        if (CircuitBreakerUrl.isSuccessResponse(response)) {
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

    /**
     * Extracts the missing analytics properties from the provided {@link IllegalStateException} exception.
     *
     * @param exception provided exception
     * @return missing analytics properties
     */
    public String extractMissingAnalyticsProps(final IllegalStateException exception) {
        final int openBracket = exception.getMessage().indexOf("[");
        final int closeBracket = exception.getMessage().indexOf("]");

        if (openBracket == -1 || closeBracket == -1) {
            return StringPool.BLANK;
        }

        return exception.getMessage().substring(openBracket + 1, closeBracket);
    }

    /**
     * Resolves the {@link AnalyticsApp} instance associated with the current host.
     *
     * @param user current user
     * @return resolved analytics app
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public AnalyticsApp resolveAnalyticsApp(final User user) throws DotDataException, DotSecurityException {
        final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHost();
        return resolveAnalyticsApp(user, currentHost);
    }

    public AnalyticsApp resolveAnalyticsApp(final User user, final String siteId)
            throws DotDataException, DotSecurityException {
        final Host host = APILocator.getHostAPI().find(siteId, user, false);
        return resolveAnalyticsApp(user, host);
    }

    public AnalyticsApp resolveAnalyticsApp(final User user, final Host site)
            throws DotDataException, DotSecurityException {
         try {
            return appFromHost(site);
        } catch (final IllegalStateException e) {
            throw new DotDataException(
                    Try.of(() ->
                                    LanguageUtil.get(
                                            user,
                                            "analytics.app.not.configured",
                                            AnalyticsHelper.get().extractMissingAnalyticsProps(e)))
                            .getOrElse(String.format("Analytics App not found for host: %s", site.getHostname())));
        }
    }

    /**
     * Resolves a cache key from token client id and audience.
     *
     * @param clientId token client id
     * @param audience token audience
     * @return key to use as key to cache for a specific access token
     */
    public String resolveKey(final String clientId, final String audience) {
        final List<String> keyChunks = new ArrayList<>();
        keyChunks.add(AnalyticsAPI.ANALYTICS_ACCESS_TOKEN_KEY_PREFIX);

        if (StringUtils.isNotBlank(clientId)) {
            keyChunks.add(clientId);
        }

        if (StringUtils.isNotBlank(audience)) {
            keyChunks.add(audience);
        }

        return String.join(StringPool.UNDERLINE, keyChunks);
    }

    /**
     * Creates a cache key from given {@link AccessToken} evaluating several conditions.
     *
     * @param accessToken provided access token
     * @return key to use as key to cache for a specific access token
     */
    public String resolveKey(final AccessToken accessToken) {
        return resolveKey(accessToken.clientId(), accessToken.aud());
    }

}
