package com.dotcms.analytics;


import com.dotcms.analytics.model.AccessToken;

/**
 * Analytics test utils.
 *
 * @author vico
 */
public class AnalyticsTestUtils {

    public static AccessToken createAccessToken(final String token,
                                                final String clientId,
                                                final String audience,
                                                final String scope,
                                                final String tokenType) {
        return AccessToken.builder()
            .accessToken(token)
            .clientId(clientId)
            .aud(audience)
            .scope(scope)
            .tokenType(tokenType)
            .expiresIn(3600)
            .build();
    }

}
