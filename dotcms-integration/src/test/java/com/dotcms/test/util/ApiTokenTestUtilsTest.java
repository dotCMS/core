package com.dotcms.test.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.jwt.beans.ApiToken;
import com.dotcms.auth.providers.jwt.factories.ApiTokenAPI;
import com.dotmarketing.business.DotStateException;
import com.liferay.portal.model.User;
import java.util.Date;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit test for the per-fixture token cleanup in {@link ApiTokenTestUtils}. It uses Mockito, so it
 * proves the delete contract without a database.
 */
public class ApiTokenTestUtilsTest {

    private static final String FIXTURE_USER_ID = "fixture-user";

    /** Cleanup must delete exactly the tokens this instance minted, including multiple calls. */
    @Test
    public void cleanupDeletesOnlyTheTokensThisResourceMinted() {
        final ApiTokenAPI tokenAPI = mock(ApiTokenAPI.class);
        final User fixtureUser = fixtureUser();
        final User deletingUser = deletingUser();
        final ApiToken first = token("api-one");
        final ApiToken second = token("api-two");
        when(tokenAPI.persistApiToken(eq(FIXTURE_USER_ID), any(Date.class), any(String.class),
                any(String.class))).thenReturn(first, second);
        when(tokenAPI.getJWT(any(ApiToken.class), eq(fixtureUser))).thenReturn("jwt");

        final ApiTokenTestUtils utils = new ApiTokenTestUtils(tokenAPI, deletingUser);

        assertEquals("Bearer jwt", utils.bearerHeader(fixtureUser));
        assertEquals("Bearer jwt", utils.bearerHeader(fixtureUser));

        utils.deleteIssuedTokens();

        verify(tokenAPI).deleteToken(first, deletingUser);
        verify(tokenAPI).deleteToken(second, deletingUser);
        verify(tokenAPI, times(2)).deleteToken(any(ApiToken.class), eq(deletingUser));
    }

    /** A second cleanup must not delete the already-deleted tokens again. */
    @Test
    public void cleanupIsIdempotent() {
        final ApiTokenAPI tokenAPI = mock(ApiTokenAPI.class);
        final User fixtureUser = fixtureUser();
        final User deletingUser = deletingUser();
        when(tokenAPI.persistApiToken(eq(FIXTURE_USER_ID), any(Date.class), any(String.class),
                any(String.class))).thenReturn(token("api-one"));
        when(tokenAPI.getJWT(any(ApiToken.class), eq(fixtureUser))).thenReturn("jwt");

        final ApiTokenTestUtils utils = new ApiTokenTestUtils(tokenAPI, deletingUser);
        utils.bearerHeader(fixtureUser);

        utils.deleteIssuedTokens();
        utils.deleteIssuedTokens();

        verify(tokenAPI, times(1)).deleteToken(any(ApiToken.class), eq(deletingUser));
    }

    /** Two resources must never delete each other's tokens. */
    @Test
    public void oneResourceNeverDeletesAnotherResourcesTokens() {
        final ApiTokenAPI firstAPI = mock(ApiTokenAPI.class);
        final ApiTokenAPI secondAPI = mock(ApiTokenAPI.class);
        final User fixtureUser = fixtureUser();
        when(firstAPI.persistApiToken(any(String.class), any(Date.class), any(String.class),
                any(String.class))).thenReturn(token("api-first"));
        when(secondAPI.persistApiToken(any(String.class), any(Date.class), any(String.class),
                any(String.class))).thenReturn(token("api-second"));
        when(firstAPI.getJWT(any(ApiToken.class), eq(fixtureUser))).thenReturn("jwt");
        when(secondAPI.getJWT(any(ApiToken.class), eq(fixtureUser))).thenReturn("jwt");

        final ApiTokenTestUtils first = new ApiTokenTestUtils(firstAPI, deletingUser());
        final ApiTokenTestUtils second = new ApiTokenTestUtils(secondAPI, deletingUser());
        first.bearerHeader(fixtureUser);
        second.bearerHeader(fixtureUser);

        first.deleteIssuedTokens();

        verify(firstAPI, times(1)).deleteToken(any(ApiToken.class), any(User.class));
        verify(secondAPI, never()).deleteToken(any(ApiToken.class), any(User.class));
    }

    /** A token whose owner was removed must still be deleted, by re-homing the delete. */
    @Test
    public void cleanupDeletesTokenWhenOwnerIsGone() {
        final ApiTokenAPI tokenAPI = mock(ApiTokenAPI.class);
        final User fixtureUser = fixtureUser();
        final User deletingUser = deletingUser();
        final ApiToken token = token("api-orphan");
        when(tokenAPI.persistApiToken(any(String.class), any(Date.class), any(String.class),
                any(String.class))).thenReturn(token);
        when(tokenAPI.getJWT(any(ApiToken.class), eq(fixtureUser))).thenReturn("jwt");
        // The owner is gone, so the normal delete fails and the delete is re-homed to the system user.
        doThrow(new DotStateException("owner gone")).when(tokenAPI).deleteToken(eq(token), eq(deletingUser));
        doReturn(true).when(tokenAPI).deleteToken(argThat(candidate -> candidate != token), eq(deletingUser));

        final ApiTokenTestUtils utils = new ApiTokenTestUtils(tokenAPI, deletingUser);
        utils.bearerHeader(fixtureUser);
        utils.deleteIssuedTokens();

        final ArgumentCaptor<ApiToken> captor = ArgumentCaptor.forClass(ApiToken.class);
        verify(tokenAPI, times(2)).deleteToken(captor.capture(), eq(deletingUser));
        assertEquals("owner", captor.getAllValues().get(0).userId);
        final ApiToken rehomed = captor.getAllValues().get(1);
        assertEquals("api-orphan", rehomed.id);
        assertEquals("system-user", rehomed.userId);
    }

    private static User fixtureUser() {
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn(FIXTURE_USER_ID);
        return user;
    }

    private static User deletingUser() {
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("system-user");
        return user;
    }

    private static ApiToken token(final String id) {
        return ApiToken.builder()
                .withId(id)
                .withUserId("owner")
                .withExpires(new Date(System.currentTimeMillis() + 60_000))
                .build();
    }
}
