package com.dotcms.auth.providers.oauth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.oauth.OAuthHelper.BuildRolesStrategy;
import com.dotcms.auth.providers.oauth.provider.OAuthProvider;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DuplicateUserException;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.SecurityLogger;
import com.liferay.portal.model.User;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
     * from the ID token. With no user for the subject and {@code trustEmailWithoutVerifiedClaim}
     * on, the existing account must be linked by email, the way SAML does, instead of
     * attempting a duplicate create.
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
                    "sub", "okta-subject-1"), config(true), true);

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
                    "sub", "entra-subject-1"), config(true), true);

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

    // ---------- email linking by email_verified (#37702) ----------

    /** A verified email links with the default config: no opt-in is needed. */
    @Test
    void emailVerifiedTrue_linksWithDefaultConfig() throws Exception {
        final EmailLink link = tryLink(emailClaims(true), null, config(false));

        assertSame(link.existing, link.resolved, "A verified email must link without any setting");
        assertTrue(link.securityLog.stream().anyMatch(m -> m.contains("linked to existing user existing-user")));
    }

    /** The id_token is signed, so its claim counts even when userinfo carries none. */
    @Test
    void emailVerifiedTrueInIdToken_linksWithDefaultConfig() throws Exception {
        final EmailLink link = tryLink(emailClaims(null), Map.of("email_verified", true), config(false));

        assertSame(link.existing, link.resolved);
    }

    /**
     * No {@code email_verified} claim and the setting off (the default): the email does not
     * link. The lookup is a plain miss, so provisioning proceeds and hits the duplicate email.
     */
    @Test
    void emailVerifiedAbsent_doesNotLink_whenSettingIsOff() throws Exception {
        final EmailLink link = tryLink(emailClaims(null), null, config(false));

        assertNull(link.resolved);
        assertInstanceOf(DuplicateUserException.class, link.thrown,
                "A refused link falls through to the normal create path");
        assertRefusal(link, "no email_verified claim", OAuthAppConfig.KEY_TRUST_EMAIL_WITHOUT_VERIFIED_CLAIM);
    }

    /** The opt-in links an IdP that never sends the claim (Okta custom authorization server). */
    @Test
    void emailVerifiedAbsent_links_whenSettingIsOn() throws Exception {
        final EmailLink link = tryLink(emailClaims(null), null, config(true));

        assertSame(link.existing, link.resolved);
    }

    /** An explicit false is a statement that the address is unproven; the opt-in does not override it. */
    @Test
    void emailVerifiedFalse_neverLinks_evenWhenSettingIsOn() throws Exception {
        final EmailLink link = tryLink(emailClaims(false), null, config(true));

        assertNull(link.resolved);
        assertInstanceOf(DuplicateUserException.class, link.thrown);
        assertRefusal(link, "email_verified=false");
    }

    /** Some IdPs send the claim as the string "false"; it must not be mistaken for an absent claim. */
    @Test
    void emailVerifiedFalseAsString_neverLinks_evenWhenSettingIsOn() throws Exception {
        final EmailLink link = tryLink(emailClaims("false"), null, config(true));

        assertNull(link.resolved);
        assertRefusal(link, "email_verified=false");
    }

    /** A false in the signed id_token wins over a true in the unsigned userinfo response. */
    @Test
    void emailVerifiedFalseInIdToken_overridesTrueInUserInfo() throws Exception {
        final EmailLink link = tryLink(emailClaims(true), Map.of("email_verified", false), config(true));

        assertNull(link.resolved);
        assertRefusal(link, "email_verified=false");
    }

    /** A per-IdP override can turn the opt-in on over an off base config. */
    @Test
    void trustedIdpOverride_canEnableLinking_overOffBase() throws Exception {
        final OAuthAppConfig base = config(false);
        final OAuthAppConfig idp = base.withTrustedIdpOverrides(
                Map.of(OAuthAppConfig.KEY_TRUST_EMAIL_WITHOUT_VERIFIED_CLAIM, true));

        final EmailLink overridden = tryLink(emailClaims(null), null, idp);
        assertSame(overridden.existing, overridden.resolved);
        assertNull(tryLink(emailClaims(null), null, base).resolved, "The base config must stay off");
    }

    /** A per-IdP override can turn the opt-in off over an on base config. */
    @Test
    void trustedIdpOverride_canDisableLinking_overOnBase() throws Exception {
        final OAuthAppConfig idp = config(true).withTrustedIdpOverrides(
                Map.of(OAuthAppConfig.KEY_TRUST_EMAIL_WITHOUT_VERIFIED_CLAIM, false));

        assertNull(tryLink(emailClaims(null), null, idp).resolved);
    }

    /** A trusted IdP that does not mention the setting inherits the base value. */
    @Test
    void trustedIdpWithoutOverride_inheritsBase() throws Exception {
        assertTrue(config(true).withTrustedIdpOverrides(Map.of()).trustEmailWithoutVerifiedClaim);
        assertFalse(config(false).withTrustedIdpOverrides(Map.of()).trustEmailWithoutVerifiedClaim);
    }

    /** With nothing stored for the key the setting is off. */
    @Test
    void trustEmailWithoutVerifiedClaim_defaultsToOff() throws Exception {
        assertFalse(ssoConfig(Map.of()).trustEmailWithoutVerifiedClaim);
    }

    private static Map<String, Object> emailClaims(final Object emailVerified) {
        final Map<String, Object> claims = new HashMap<>();
        claims.put("iss", "https://tenant.okta.com/oauth2/default");
        claims.put("email", "alice@example.com");
        claims.put("sub", "idp-subject-1");
        if (emailVerified != null) {
            claims.put("email_verified", emailVerified);
        }
        return claims;
    }

    private static void assertRefusal(final EmailLink link, final String... expectedFragments) {
        assertTrue(link.securityLog.stream().noneMatch(m -> m.contains("linked to existing user")),
                "A refused link must not be logged as a link");
        final String warning = link.securityLog.stream().filter(m -> m.contains("NOT linked")).findFirst()
                .orElseThrow(() -> new AssertionError("The refusal must be recorded in the security log"));
        assertTrue(warning.contains("existing-user"), warning);
        for (final String fragment : expectedFragments) {
            assertTrue(warning.contains(fragment), warning);
        }
        assertFalse(warning.contains("alice@example.com"), "The email address must not be logged");
    }

    /** What happened when an IdP identity with an unknown subject arrived and an account has its email. */
    private static final class EmailLink {
        final User existing = mock(User.class);
        final List<String> securityLog = new ArrayList<>();
        User resolved;
        Exception thrown;
    }

    /**
     * Runs the real {@code resolveOrProvisionUser} for an IdP identity whose subject matches no
     * user while one existing account (id {@code existing-user}) has the identity's email. Only
     * the persistence layer is mocked, and {@code createUser} rejects the duplicate email the
     * way the real user API does, so a refused link surfaces as the exception.
     */
    private static EmailLink tryLink(final Map<String, Object> userInfo,
                                     final Map<String, Object> verifiedClaims,
                                     final OAuthAppConfig config) throws Exception {
        final EmailLink link = new EmailLink();
        final UserAPI userAPI = mock(UserAPI.class);
        final User systemUser = mock(User.class);

        when(link.existing.getUserId()).thenReturn("existing-user");
        when(link.existing.isActive()).thenReturn(true);
        when(link.existing.getFirstName()).thenReturn("Alice");
        when(link.existing.getLastName()).thenReturn("Example");
        when(userAPI.loadUserById(Mockito.anyString())).thenReturn(null);
        when(userAPI.loadByUserByEmail(eq("alice@example.com"), eq(systemUser), anyBoolean()))
                .thenReturn(link.existing);
        when(userAPI.loadUserById(eq("existing-user"), eq(systemUser), anyBoolean())).thenReturn(link.existing);
        when(userAPI.createUser(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new DuplicateUserException("User already exists with this email"));

        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class);
             MockedStatic<Config> cfg = Mockito.mockStatic(Config.class);
             MockedStatic<SecurityLogger> security = Mockito.mockStatic(SecurityLogger.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            api.when(APILocator::systemUser).thenReturn(systemUser);
            cfg.when(() -> Config.getIntProperty("dotcms.user.id.maxlength", 100)).thenReturn(100);
            cfg.when(() -> Config.getStringProperty(ROLE_STRATEGY_PROP, BuildRolesStrategy.ALL.name()))
                    .thenReturn(BuildRolesStrategy.NONE.name());
            security.when(() -> SecurityLogger.logInfo(eq(OAuthHelper.class), Mockito.anyString()))
                    .thenAnswer(inv -> link.securityLog.add(inv.getArgument(1)));
            security.when(() -> SecurityLogger.logWarn(eq(OAuthHelper.class), Mockito.anyString()))
                    .thenAnswer(inv -> link.securityLog.add(inv.getArgument(1)));

            try {
                link.resolved = new OAuthHelper().resolveOrProvisionUser(
                        provider(), null, userInfo, verifiedClaims, config, true);
            } catch (final DuplicateUserException e) {
                link.thrown = e;
            }
        }
        return link;
    }

    private static OAuthAppConfig config(final boolean trustEmailWithoutVerifiedClaim) throws Exception {
        return ssoConfig(Map.of(OAuthAppConfig.KEY_TRUST_EMAIL_WITHOUT_VERIFIED_CLAIM,
                String.valueOf(trustEmailWithoutVerifiedClaim)));
    }

    /** Build a real SSO {@link OAuthAppConfig} via its private secrets constructor. */
    private static OAuthAppConfig ssoConfig(final Map<String, String> values) throws Exception {
        final Map<String, Secret> secrets = new HashMap<>();
        values.forEach((k, v) -> secrets.put(k, Secret.builder().withValue(v).withType(Type.STRING).build()));
        final Constructor<OAuthAppConfig> ctor = OAuthAppConfig.class.getDeclaredConstructor(Map.class);
        ctor.setAccessible(true);
        return ctor.newInstance(secrets);
    }

    private static OAuthProvider provider() {
        final OAuthProvider provider = mock(OAuthProvider.class);
        when(provider.getProviderType()).thenReturn("OIDC");
        return provider;
    }
}
