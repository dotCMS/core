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
import com.dotmarketing.util.SecurityLogger;
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

    /**
     * When the issuer-namespaced subject already maps to a user, that user wins and the
     * email is never consulted, as in {@code SAMLHelper.resolveUser}.
     */
    @Test
    void subjectMatch_takesPrecedenceOverEmail() throws Exception {
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

            final User resolved = helper.resolveOrProvisionUser(provider, null, Map.of(
                    "email", "victim@example.com",
                    "sub", "attacker-subject"), null, true);

            assertSame(subjectUser, resolved, "A subject match must win over an email match");
        }

        verify(userAPI, never()).loadByUserByEmail(Mockito.anyString(), Mockito.any(), anyBoolean());
    }

    /**
     * Regression for #37691: Okta custom authorization servers omit {@code email_verified}
     * from the ID token. With no user for the subject, the existing account must be linked
     * by email, the way SAML does, instead of attempting a duplicate create.
     */
    @Test
    void missingEmailVerified_linksExistingAccountByEmail_whenSubjectIsUnknown() throws Exception {
        final OAuthHelper helper = new OAuthHelper();
        final OAuthProvider provider = provider();
        final UserAPI userAPI = mock(UserAPI.class);
        final User systemUser = mock(User.class);
        final User existing = mock(User.class);

        when(existing.getUserId()).thenReturn("existing-user");
        when(existing.isActive()).thenReturn(true);
        when(existing.getFirstName()).thenReturn("Alice");
        when(existing.getLastName()).thenReturn("Example");
        // No user carries the namespaced subject yet.
        when(userAPI.loadUserById(Mockito.anyString())).thenReturn(null);
        when(userAPI.loadByUserByEmail(eq("alice@example.com"), eq(systemUser), anyBoolean()))
                .thenReturn(existing);
        when(userAPI.loadUserById(eq("existing-user"), eq(systemUser), anyBoolean())).thenReturn(existing);

        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class);
             MockedStatic<Config> cfg = Mockito.mockStatic(Config.class);
             MockedStatic<SecurityLogger> security = Mockito.mockStatic(SecurityLogger.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            api.when(APILocator::systemUser).thenReturn(systemUser);
            cfg.when(() -> Config.getIntProperty("dotcms.user.id.maxlength", 100)).thenReturn(100);
            cfg.when(() -> Config.getStringProperty(ROLE_STRATEGY_PROP, BuildRolesStrategy.ALL.name()))
                    .thenReturn(BuildRolesStrategy.NONE.name());

            final User resolved = helper.resolveOrProvisionUser(provider, null, Map.of(
                    "iss", "https://tenant.okta.com/oauth2/default",
                    "email", "alice@example.com",
                    "sub", "okta-subject-1"), null, true);

            assertSame(existing, resolved, "The existing account must be linked by email");
            // Linking by email is a trust decision, so it is recorded in the security log.
            security.verify(() -> SecurityLogger.logInfo(eq(OAuthHelper.class),
                    Mockito.argThat((String msg) -> msg.contains("existing-user")
                            && msg.contains("https://tenant.okta.com/oauth2/default"))));
        }

        verify(userAPI, never()).createUser(Mockito.anyString(), Mockito.anyString());
    }

    /**
     * dotCMS stores emails lowercased (Liferay trims and lowercases on create and update)
     * and its duplicate check compares lowercased addresses, but the email lookup is an
     * exact match. A mixed-case email from the IdP must still link the existing account
     * rather than fall through to a duplicate create (#37691).
     */
    @Test
    void mixedCaseEmail_linksExistingAccount() throws Exception {
        final OAuthHelper helper = new OAuthHelper();
        final OAuthProvider provider = provider();
        final UserAPI userAPI = mock(UserAPI.class);
        final User systemUser = mock(User.class);
        final User existing = mock(User.class);

        when(existing.getUserId()).thenReturn("existing-user");
        when(existing.isActive()).thenReturn(true);
        when(existing.getFirstName()).thenReturn("Alice");
        when(existing.getLastName()).thenReturn("Example");
        when(userAPI.loadUserById(Mockito.anyString())).thenReturn(null);
        // Only the stored, lowercased form finds the account.
        when(userAPI.loadByUserByEmail(eq("alice@example.com"), eq(systemUser), anyBoolean()))
                .thenReturn(existing);
        when(userAPI.loadUserById(eq("existing-user"), eq(systemUser), anyBoolean())).thenReturn(existing);

        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class);
             MockedStatic<Config> cfg = Mockito.mockStatic(Config.class);
             MockedStatic<SecurityLogger> security = Mockito.mockStatic(SecurityLogger.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            api.when(APILocator::systemUser).thenReturn(systemUser);
            cfg.when(() -> Config.getIntProperty("dotcms.user.id.maxlength", 100)).thenReturn(100);
            cfg.when(() -> Config.getStringProperty(ROLE_STRATEGY_PROP, BuildRolesStrategy.ALL.name()))
                    .thenReturn(BuildRolesStrategy.NONE.name());

            final User resolved = helper.resolveOrProvisionUser(provider, null, Map.of(
                    "email", " Alice@Example.COM ",
                    "sub", "entra-subject-1"), null, true);

            assertSame(existing, resolved, "A mixed-case email must link the lowercased stored account");
        }

        verify(userAPI, never()).createUser(Mockito.anyString(), Mockito.anyString());
    }

    /**
     * The subject must win even when the IdP also asserts a verified email that belongs to a
     * different account. Before #37691 a verified email was checked first and won.
     */
    @Test
    void subjectMatch_winsEvenWhenEmailIsVerified() throws Exception {
        final OAuthHelper helper = new OAuthHelper();
        final OAuthProvider provider = provider();
        final UserAPI userAPI = mock(UserAPI.class);
        final User systemUser = mock(User.class);
        final User emailUser = mock(User.class);
        final User subjectUser = mock(User.class);

        when(subjectUser.getUserId()).thenReturn("subject-user");
        when(subjectUser.isActive()).thenReturn(true);
        when(subjectUser.getFirstName()).thenReturn("First");
        when(subjectUser.getLastName()).thenReturn("Last");
        when(userAPI.loadByUserByEmail(eq("someone@example.com"), eq(systemUser), anyBoolean()))
                .thenReturn(emailUser);
        when(userAPI.loadUserById(Mockito.anyString())).thenReturn(subjectUser);
        when(userAPI.loadUserById(eq("subject-user"), eq(systemUser), anyBoolean())).thenReturn(subjectUser);

        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class);
             MockedStatic<Config> cfg = Mockito.mockStatic(Config.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            api.when(APILocator::systemUser).thenReturn(systemUser);
            cfg.when(() -> Config.getIntProperty("dotcms.user.id.maxlength", 100)).thenReturn(100);
            cfg.when(() -> Config.getStringProperty(ROLE_STRATEGY_PROP, BuildRolesStrategy.ALL.name()))
                    .thenReturn(BuildRolesStrategy.NONE.name());

            final User resolved = helper.resolveOrProvisionUser(provider, null, Map.of(
                    "email", "someone@example.com",
                    "email_verified", true,
                    "sub", "subject-1"), null, true);

            assertSame(subjectUser, resolved, "A subject match must win over a verified email match");
        }
    }

    private static OAuthProvider provider() {
        final OAuthProvider provider = mock(OAuthProvider.class);
        when(provider.getProviderType()).thenReturn("OIDC");
        return provider;
    }
}
