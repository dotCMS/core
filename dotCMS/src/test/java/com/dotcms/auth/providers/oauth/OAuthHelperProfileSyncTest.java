package com.dotcms.auth.providers.oauth;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.oauth.OAuthHelper.BuildRolesStrategy;
import com.dotcms.auth.providers.oauth.provider.OAuthProvider;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class OAuthHelperProfileSyncTest {

    private static final String ROLE_STRATEGY_PROP = "OAUTH_BUILD_ROLES_STRATEGY";

    @Test
    void existingUser_updatesNameFieldsFromProviderClaims() throws Exception {
        final OAuthHelper helper = new OAuthHelper();
        final OAuthProvider provider = provider();
        final UserAPI userAPI = mock(UserAPI.class);
        final User systemUser = mock(User.class);
        final User user = mock(User.class);

        when(user.getUserId()).thenReturn("user-1");
        when(user.getEmailAddress()).thenReturn("new.name@example.com");
        when(user.getFirstName()).thenReturn("OldFirst");
        when(user.getLastName()).thenReturn("OldLast");
        when(user.isActive()).thenReturn(true);
        when(userAPI.loadByUserByEmail(eq("new.name@example.com"), eq(systemUser), anyBoolean()))
                .thenReturn(user);

        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class);
             MockedStatic<Config> cfg = Mockito.mockStatic(Config.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            api.when(APILocator::systemUser).thenReturn(systemUser);
            cfg.when(() -> Config.getIntProperty("dotcms.user.id.maxlength", 100)).thenReturn(100);
            cfg.when(() -> Config.getStringProperty(ROLE_STRATEGY_PROP, BuildRolesStrategy.ALL.name()))
                    .thenReturn(BuildRolesStrategy.NONE.name());

            helper.resolveOrProvisionUser(provider, null, Map.of(
                    "email", "new.name@example.com",
                    "email_verified", true,
                    "sub", "subject-1",
                    "given_name", "NewFirst",
                    "family_name", "NewLast"), null, true);
        }

        verify(user).setFirstName("NewFirst");
        verify(user).setNickName("NewFirst");
        verify(user).setLastName("NewLast");
        verify(userAPI).save(user, systemUser, false);
    }

    @Test
    void existingUser_doesNotOverwriteNamesWhenClaimsAreAbsent() throws Exception {
        final OAuthHelper helper = new OAuthHelper();
        final OAuthProvider provider = provider();
        final UserAPI userAPI = mock(UserAPI.class);
        final User systemUser = mock(User.class);
        final User user = mock(User.class);

        when(user.getUserId()).thenReturn("user-1");
        when(user.getEmailAddress()).thenReturn("unchanged@example.com");
        when(user.getFirstName()).thenReturn("ExistingFirst");
        when(user.getLastName()).thenReturn("ExistingLast");
        when(user.isActive()).thenReturn(true);
        when(userAPI.loadByUserByEmail(eq("unchanged@example.com"), eq(systemUser), anyBoolean()))
                .thenReturn(user);

        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class);
             MockedStatic<Config> cfg = Mockito.mockStatic(Config.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            api.when(APILocator::systemUser).thenReturn(systemUser);
            cfg.when(() -> Config.getIntProperty("dotcms.user.id.maxlength", 100)).thenReturn(100);
            cfg.when(() -> Config.getStringProperty(ROLE_STRATEGY_PROP, BuildRolesStrategy.ALL.name()))
                    .thenReturn(BuildRolesStrategy.NONE.name());

            helper.resolveOrProvisionUser(provider, null, Map.of(
                    "email", "unchanged@example.com",
                    "email_verified", true,
                    "sub", "subject-1"), null, true);
        }

        verify(user, never()).setFirstName(Mockito.anyString());
        verify(user, never()).setNickName(Mockito.anyString());
        verify(user, never()).setLastName(Mockito.anyString());
        verify(userAPI, never()).save(user, systemUser, false);
    }

    @Test
    void unverifiedEmail_isNotUsedToMatchExistingAccount() throws Exception {
        final OAuthHelper helper = new OAuthHelper();
        final OAuthProvider provider = provider();
        final UserAPI userAPI = mock(UserAPI.class);
        final User systemUser = mock(User.class);
        final User emailUser = mock(User.class);   // the account that must NOT be matched
        final User subjectUser = mock(User.class); // resolved via the namespaced subject instead

        when(subjectUser.getUserId()).thenReturn("subject-user");
        when(subjectUser.isActive()).thenReturn(true);
        when(subjectUser.getFirstName()).thenReturn("First");
        when(subjectUser.getLastName()).thenReturn("Last");
        // If the gate were broken, this stub would hand back the wrong (email-matched) account.
        when(userAPI.loadByUserByEmail(eq("victim@example.com"), eq(systemUser), anyBoolean()))
                .thenReturn(emailUser);
        // The issuer-namespaced subject is the trustworthy identity key.
        when(userAPI.loadUserById(Mockito.anyString())).thenReturn(subjectUser);
        when(userAPI.loadUserById(eq("subject-user"), eq(systemUser), anyBoolean())).thenReturn(subjectUser);

        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class);
             MockedStatic<Config> cfg = Mockito.mockStatic(Config.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            api.when(APILocator::systemUser).thenReturn(systemUser);
            cfg.when(() -> Config.getIntProperty("dotcms.user.id.maxlength", 100)).thenReturn(100);
            cfg.when(() -> Config.getStringProperty(ROLE_STRATEGY_PROP, BuildRolesStrategy.ALL.name()))
                    .thenReturn(BuildRolesStrategy.NONE.name());

            // No email_verified claim → the address must be ignored for existing-account matching.
            final User resolved = helper.resolveOrProvisionUser(provider, null, Map.of(
                    "email", "victim@example.com",
                    "sub", "attacker-subject"), null, true);

            assertSame(subjectUser, resolved, "Unverified email must not match the email-keyed account");
        }

        verify(userAPI, never()).loadByUserByEmail(Mockito.anyString(), Mockito.any(), anyBoolean());
    }

    private static OAuthProvider provider() {
        final OAuthProvider provider = mock(OAuthProvider.class);
        when(provider.getProviderType()).thenReturn("OIDC");
        return provider;
    }
}
