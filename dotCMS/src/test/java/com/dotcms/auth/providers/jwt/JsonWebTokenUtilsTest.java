package com.dotcms.auth.providers.jwt;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotmarketing.business.APILocator;
import com.liferay.portal.model.User;
import java.util.Optional;
import org.junit.Test;
import org.mockito.MockedStatic;

/**
 * Loading the user a login token belongs to with {@link JsonWebTokenUtils#getUser}.
 */
public class JsonWebTokenUtilsTest {

    private static final String JWT = "a.b.c";
    private static final String IP = "127.0.0.1";

    /**
     * A validated token whose user still holds its id returns that user.
     */
    @Test
    public void getUser_returnsUserHoldingTheTokenId() {
        final User user = user("token-id");

        assertSame(user, getUser(token("token-id", user)));
    }

    /**
     * A token validated just before a logout or password change replaced its id does not return
     * the reloaded user, so no new token is issued from it.
     */
    @Test
    public void getUser_returnsNull_whenTokenIdWasReplacedAfterValidation() {
        assertNull(getUser(token("token-id", user("replaced-id"))));
    }

    /**
     * A token that does not validate, or whose user is inactive, returns null.
     */
    @Test
    public void getUser_returnsNull_forInvalidTokenOrInactiveUser() {
        assertNull(getUser(Optional.empty()));
        final JWToken inactive = mock(JWToken.class);
        when(inactive.getId()).thenReturn("token-id");
        when(inactive.getActiveUser()).thenReturn(Optional.empty());
        assertNull(getUser(Optional.of(inactive)));
    }

    private static User getUser(final Optional<JWToken> validated) {
        final ApiTokenAPI apiTokenAPI = mock(ApiTokenAPI.class);
        when(apiTokenAPI.fromJwt(JWT, IP)).thenReturn(validated);
        try (MockedStatic<APILocator> apis = mockStatic(APILocator.class)) {
            apis.when(APILocator::getApiTokenAPI).thenReturn(apiTokenAPI);
            return new JsonWebTokenUtils(mock(JsonWebTokenService.class)).getUser(JWT, IP);
        }
    }

    private static Optional<JWToken> token(final String id, final User activeUser) {
        final JWToken token = mock(JWToken.class);
        when(token.getId()).thenReturn(id);
        when(token.getActiveUser()).thenReturn(Optional.of(activeUser));
        return Optional.of(token);
    }

    private static User user(final String rememberMeToken) {
        final User user = new User();
        user.setUserId("user-id");
        user.setRememberMeToken(rememberMeToken);
        return user;
    }
}
