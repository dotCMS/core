package com.dotcms.auth.providers.jwt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.jwt.beans.JWToken;
import com.dotcms.auth.providers.jwt.beans.UserToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotcms.auth.providers.jwt.services.JsonWebTokenService;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cms.factories.PublicEncryptionFactory;
import com.liferay.portal.model.User;
import java.time.Duration;
import java.util.Optional;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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

    /**
     * The owner of a validated token comes back with the token's own id.
     */
    @Test
    public void getLoginTokenOwner_returnsUserWithValidatedTokenId() {
        final User user = user("token-id");
        final Optional<JWToken> validated = token("token-id", user);
        final ApiTokenAPI apiTokenAPI = mock(ApiTokenAPI.class);
        when(apiTokenAPI.fromJwt(JWT, IP)).thenReturn(validated);
        try (MockedStatic<APILocator> apis = mockStatic(APILocator.class)) {
            apis.when(APILocator::getApiTokenAPI).thenReturn(apiTokenAPI);

            final JsonWebTokenUtils.LoginTokenOwner owner =
                    new JsonWebTokenUtils(mock(JsonWebTokenService.class)).getLoginTokenOwner(JWT, IP).orElseThrow();

            assertSame(user, owner.user());
            assertEquals("token-id", owner.tokenId());
        }
    }

    /**
     * A session login token created with an explicit id carries that id, whatever the user holds.
     */
    @Test
    public void createSessionLoginToken_carriesGivenTokenId() {
        final JsonWebTokenService service = mock(JsonWebTokenService.class);
        final User user = user("id-set-by-password-change");

        try (MockedStatic<PublicEncryptionFactory> encryption = mockStatic(PublicEncryptionFactory.class);
                MockedStatic<ClusterFactory> cluster = mockStatic(ClusterFactory.class)) {
            encryption.when(() -> PublicEncryptionFactory.encryptString(any())).thenReturn("encrypted-user-id");
            cluster.when(ClusterFactory::getClusterId).thenReturn("cluster-id");
            new JsonWebTokenUtils(service).createSessionLoginToken(user, "validated-token-id", Duration.ofMinutes(5));
        }

        final ArgumentCaptor<UserToken> issued = ArgumentCaptor.forClass(UserToken.class);
        verify(service).generateUserToken(issued.capture());
        assertEquals("validated-token-id", issued.getValue().getId());
        assertEquals(Boolean.TRUE, issued.getValue().getClaims().get(JsonWebTokenUtils.CLAIM_SESSION_LOGIN));
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
