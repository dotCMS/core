package com.dotcms.auth.providers.oauth;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.auth.providers.oauth.provider.OAuthProvider;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * Pins the default-closed group→role security model in
 * {@code OAuthHelper.applyProviderGroups}: only IdP groups with an explicit
 * groupMappings entry resolve to dotCMS roles by default. Passthrough of unmapped
 * group names (which would let an IdP group literally named "CMS Administrator"
 * grant the admin role) requires the explicit {@code allowUnmappedGroups=true}
 * opt-in, and the optional {@code groupFilterPattern} regex allow-list constrains
 * the final role key — failing closed on an unparseable pattern.
 */
class OAuthHelperGroupSecurityTest {

    private static final String ROLE_STRATEGY_PROP = "OAUTH_BUILD_ROLES_STRATEGY";

    /** Role key of the dotCMS administrator — a group named this must not auto-grant. */
    private static final String ADMIN_ROLE_KEY = "CMS Administrator";

    // ---------- tests ----------

    @Test
    void unmappedGroupNamedLikeAdminRole_skippedByDefault() throws Exception {
        final Role adminRole = role(ADMIN_ROLE_KEY);
        final Role mappedRole = role("CMS-Mapped-Admin");
        final RoleApiMocks roles = roleApiMocks(adminRole, mappedRole);
        // "CMS Administrator" has NO mapping entry; "mapped-admins" does.
        final OAuthAppConfig config = ssoConfig(Map.of(
                "buildRolesStrategy", "ALL",
                "groupMappings", "[{\"idpGroup\":\"mapped-admins\",\"dotcmsRole\":\"CMS-Mapped-Admin\"}]"));

        runExchange(roles, config,
                Set.of(ADMIN_ROLE_KEY, "mapped-admins", "unmapped-editors"));

        // The mapped group resolves to its role...
        verify(roles.api).addRoleToUser(eq(mappedRole), any(User.class));
        // ...while the unmapped groups — including one named after the admin role key —
        // never even reach role lookup.
        verify(roles.api, never()).loadRoleByKey(ADMIN_ROLE_KEY);
        verify(roles.api, never()).loadRoleByKey("unmapped-editors");
        verify(roles.api, never()).addRoleToUser(eq(adminRole), any(User.class));
    }

    @Test
    void allowUnmappedGroupsOptIn_passesUnmappedGroupNamesThrough() throws Exception {
        final Role adminRole = role(ADMIN_ROLE_KEY);
        final RoleApiMocks roles = roleApiMocks(adminRole, role("CMS-Mapped-Admin"));
        final OAuthAppConfig config = ssoConfig(Map.of(
                "buildRolesStrategy", "ALL",
                "allowUnmappedGroups", "true",
                "groupMappings", "[{\"idpGroup\":\"mapped-admins\",\"dotcmsRole\":\"CMS-Mapped-Admin\"}]"));

        runExchange(roles, config, Set.of(ADMIN_ROLE_KEY, "mapped-admins"));

        // Explicit opt-in: the unmapped group name IS tried as a role key and grants the
        // role it happens to match. This is the documented trust trade-off of the flag.
        verify(roles.api).loadRoleByKey(ADMIN_ROLE_KEY);
        verify(roles.api).addRoleToUser(eq(adminRole), any(User.class));
    }

    @Test
    void groupFilterPattern_appliedToFinalRoleKey_afterMapping() throws Exception {
        final Role editorRole = role("dotcms-editor");
        final Role contribRole = role("dotcms-contrib");
        final RoleApiMocks roles = roleApiMocks(editorRole, contribRole);
        final OAuthAppConfig config = ssoConfig(Map.of(
                "buildRolesStrategy", "ALL",
                "allowUnmappedGroups", "true",
                "groupFilterPattern", "^dotcms-",
                "groupMappings", "[{\"idpGroup\":\"any-idp-group\",\"dotcmsRole\":\"dotcms-editor\"}]"));

        runExchange(roles, config, Set.of("any-idp-group", "dotcms-contrib", ADMIN_ROLE_KEY));

        // Mapped role key matches the pattern; unmapped "dotcms-contrib" matches too...
        verify(roles.api).addRoleToUser(eq(editorRole), any(User.class));
        verify(roles.api).addRoleToUser(eq(contribRole), any(User.class));
        // ...but "CMS Administrator" fails the allow-list even with passthrough on.
        verify(roles.api, never()).loadRoleByKey(ADMIN_ROLE_KEY);
    }

    @Test
    void invalidGroupFilterPattern_failsClosed() throws Exception {
        final Role editorRole = role("dotcms-editor");
        final RoleApiMocks roles = roleApiMocks(editorRole);
        final OAuthAppConfig config = ssoConfig(Map.of(
                "buildRolesStrategy", "ALL",
                // Disable the backend baseline role so the blanket never()-assertions below
                // observe ONLY group-derived role assignments.
                "enableBackend", "false",
                // Unparseable regex — must not degrade into "allow everything".
                "groupFilterPattern", "[",
                "groupMappings", "[{\"idpGroup\":\"any-idp-group\",\"dotcmsRole\":\"dotcms-editor\"}]"));

        runExchange(roles, config, Set.of("any-idp-group"));

        // Even the MAPPED group is skipped: an invalid allow-list blocks all
        // group-derived roles until the pattern is fixed.
        verify(roles.api, never()).loadRoleByKey(anyString());
        verify(roles.api, never()).addRoleToUser(any(Role.class), any(User.class));
    }

    // ---------- harness ----------

    private static final class RoleApiMocks {
        final RoleAPI api;
        RoleApiMocks(final RoleAPI api) { this.api = api; }
    }

    private static Role role(final String key) {
        final Role role = mock(Role.class);
        when(role.getName()).thenReturn(key);
        return role;
    }

    private static RoleApiMocks roleApiMocks(final Role... knownRoles) throws Exception {
        final RoleAPI roleAPI = mock(RoleAPI.class);
        for (final Role role : knownRoles) {
            when(roleAPI.loadRoleByKey(role.getName())).thenReturn(role);
        }
        // applySystemRoles baseline (enableBackend defaults true on the SSO config)
        when(roleAPI.loadBackEndUserRole()).thenReturn(mock(Role.class));
        return new RoleApiMocks(roleAPI);
    }

    /**
     * Drive {@code resolveOrProvisionUser} through the full ALL-strategy path with the
     * given provider groups, against the given config.
     */
    private static void runExchange(final RoleApiMocks roles,
                                    final OAuthAppConfig config,
                                    final Collection<String> providerGroups) throws Exception {
        final OAuthHelper helper = new OAuthHelper();
        final OAuthProvider provider = mock(OAuthProvider.class);
        when(provider.getProviderType()).thenReturn("OIDC");
        when(provider.getGroups(any(), any())).thenReturn(new LinkedHashSet<>(providerGroups));

        final UserAPI userAPI = mock(UserAPI.class);
        final User systemUser = mock(User.class);
        final User user = mock(User.class);
        when(user.getUserId()).thenReturn("subject-user");
        when(user.isActive()).thenReturn(true);
        when(userAPI.loadByUserByEmail(anyString(), any(User.class), anyBoolean())).thenReturn(null);
        when(userAPI.loadUserById(anyString())).thenReturn(user);
        when(userAPI.loadUserById(anyString(), any(User.class), anyBoolean())).thenReturn(user);

        final Map<String, Object> userInfo = Map.of(
                "sub", "subject-1",
                "email", "user@example.com",
                "email_verified", true);

        try (MockedStatic<APILocator> api = Mockito.mockStatic(APILocator.class);
             MockedStatic<Config> cfg = Mockito.mockStatic(Config.class)) {
            api.when(APILocator::getUserAPI).thenReturn(userAPI);
            api.when(APILocator::systemUser).thenReturn(systemUser);
            api.when(APILocator::getRoleAPI).thenReturn(roles.api);
            cfg.when(() -> Config.getIntProperty("dotcms.user.id.maxlength", 100)).thenReturn(100);
            cfg.when(() -> Config.getStringProperty(ROLE_STRATEGY_PROP, "ALL")).thenReturn("ALL");

            final User resolved = helper.resolveOrProvisionUser(
                    provider, "access-token", userInfo, config, true);
            assertSame(user, resolved);
        }
    }

    /** Build a real SSO {@link OAuthAppConfig} via its private secrets constructor. */
    private static OAuthAppConfig ssoConfig(final Map<String, String> values) throws Exception {
        final Map<String, Secret> secrets = new HashMap<>();
        values.forEach((k, v) -> secrets.put(k,
                Secret.builder().withValue(v).withType(Type.STRING).build()));
        final Constructor<OAuthAppConfig> ctor =
                OAuthAppConfig.class.getDeclaredConstructor(Map.class);
        ctor.setAccessible(true);
        return ctor.newInstance(secrets);
    }
}
