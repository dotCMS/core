package com.dotcms.analytics.helper;

import com.dotcms.analytics.AnalyticsAPI;
import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.model.AccessToken;
import com.dotcms.analytics.model.AnalyticsKey;
import com.dotmarketing.beans.Host;
import com.dotmarketing.util.Logger;

import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;


/**
 * Helper class for analytics matters.
 *
 * @author vico
 */
public class AnalyticsHelper {

    /**
     * Evaluates if a given {@link Response} instance has a http status within the SUCCESSFUL range.
     *
     * @param response http response representation
     * @return true if the response http status is considered tobe successful, otherwise false
     */
    public static boolean isSuccessResponse(final Response response) {
        return response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL;
    }

    /**
     * Given a {@link Response} instance, extracts JSON representing the token and deserializes to {@link AccessToken}.
     *
     * @param response http response representation
     * @return an {@link Optional<AccessToken>} instance holding the access token data
     */
    public static Optional<AccessToken> extractToken(final Response response) {
        return extractFromResponse(response, AccessToken.class, "ACCESS_TOKEN response is missing");
    }

    /**
     * Given a {@link Response} instance, extracts JSON representing the token and deserializes to {@link AnalyticsKey}.
     *
     * @param response http response representation
     * @return an {@link Optional<AnalyticsKey>} instance holding the analytics key data
     */
    public static Optional<AnalyticsKey> extractAnalyticsKey(final Response response) {
        return extractFromResponse(response, AnalyticsKey.class, "ANALYTICS_KEY response is missing");
    }

    /**
     * Given an {@link AccessToken} evaluates if it's expired by comparing issue date to current date time.
     * An offset can be provided to decrease the TTL window at the evaluation.
     *
     * @param accessToken provided access token
     * @param offset offset used to subtract to current date time
     * @return true if current time is in the TTL window
     */
    public static boolean isExpired(final AccessToken accessToken, final int offset) {
        if (Objects.isNull(accessToken.issueDate())) {
            Logger.warn(AnalyticsHelper.class, "ACCESS_TOKEN does not have a issued date, returning as expired");
            return true;
        }

        return Duration
            .between(accessToken.issueDate(), Instant.now())
            .toSeconds() >= (AnalyticsAPI.ANALYTICS_ACCESS_TOKEN_TTL + offset);
    }

    /**
     * Gets a an Base 64 encoded version of `clientId:clientSecret`.
     *
     * @param clientId client id
     * @param clientSecret client secret
     * @return String representation of base 64 bytes
     */
    public static String encodeClientIdAndSecret(final String clientId, final String clientSecret) {
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
     * Given an {@link AccessToken} evaluates if it's expired by comparing issue date to current date time.
     * An offset zero is used.
     *
     * @param accessToken provided access token
     * @return true if current time is in the TTL window
     */
    public static boolean isExpired(final AccessToken accessToken) {
        return isExpired(accessToken, 0);
    }

    /**
     * Creates a {@link AnalyticsApp} instance associated with provided host.
     *
     * @param host provided host
     * @return associated host app
     */
    public static AnalyticsApp getHostApp(final Host host) {
        return new AnalyticsApp(host);
    }

    /**
     * Extracts a deserialized JSON from a {@link Response} instance using a provided {@link Class<T>} in the
     * deserialization process.
     *
     * @param response http response
     * @param clazz class to use deserializing
     * @param message message to use when throwing NPE
     * @return {@link Optional<T>} instance wrapping the deserialized object
     * @param <T> type to use when deserializing
     */
    private static <T> Optional<T> extractFromResponse(final Response response,
                                                       final Class<T> clazz,
                                                       final String message) {
        Objects.requireNonNull(response, message);

        if (!isSuccessResponse(response)) {
            return Optional.empty();
        }

        return Optional.ofNullable(response.readEntity(clazz));
    }

}
