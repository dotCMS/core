package com.dotcms.security.apps;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotcms.util.LicenseValiditySupplier;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.LayoutAPI;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import io.vavr.Tuple;
import java.util.Set;
import org.junit.After;
import org.junit.Test;

/**
 * Covers the rule that changing Apps configuration and reading its values requires an
 * administrator, and that {@link AppsAPIImpl#APPS_ALLOW_PORTLET_CONFIGURATION} lets users with the
 * Apps portlet do so as well.
 *
 * Deletes are used for the allowed cases because they reach the secrets store without the static
 * host lookup that {@code saveSecrets} performs; the integration test {@code AppsResourceTest}
 * covers an administrator save end to end.
 */
public class AppsAPIImplAdminSecretsTest {

    private static final String APP_KEY = "dotcms-app";
    private static final String SITE_ID = "48190c8c-42c4-46af-8d1a-0cd5db894797";

    private final SecretsStore secretsStore = mock(SecretsStore.class);
    private final Host site = mock(Host.class);

    @After
    public void resetSwitch() {
        Config.setProperty(AppsAPIImpl.APPS_ALLOW_PORTLET_CONFIGURATION, false);
    }

    private AppsAPIImpl newApi(final boolean userHasAppsPortlet) throws Exception {
        final LayoutAPI layoutAPI = mock(LayoutAPI.class);
        when(layoutAPI.doesUserHaveAccessToPortlet(anyString(), any(User.class)))
                .thenReturn(userHasAppsPortlet);
        when(site.getIdentifier()).thenReturn(SITE_ID);
        return new AppsAPIImpl(
                layoutAPI,
                mock(HostAPI.class),
                secretsStore,
                mock(AppsCache.class),
                mock(LocalSystemEventsAPI.class),
                mock(AppDescriptorHelper.class),
                new LicenseValiditySupplier() {
                    @Override
                    public boolean hasValidLicense() {
                        return true;
                    }
                });
    }

    private static User user(final boolean admin) {
        final User user = mock(User.class);
        when(user.isAdmin()).thenReturn(admin);
        when(user.getUserId()).thenReturn(admin ? "admin-user" : "apps-portlet-user");
        return user;
    }

    private static AppSecrets secrets() {
        return AppSecrets.builder().withKey(APP_KEY)
                .withHiddenSecret("apiKey", "value").build();
    }

    @Test
    public void nonAdministratorCannotSaveAppConfiguration() throws Exception {
        final AppsAPIImpl api = newApi(true);
        assertThrows(DotSecurityException.class,
                () -> api.saveSecrets(secrets(), site, user(false)));
        verifyNoInteractions(secretsStore);
    }

    @Test
    public void nonAdministratorCannotSaveOneAppConfigurationValue() throws Exception {
        final AppsAPIImpl api = newApi(true);
        assertThrows(DotSecurityException.class,
                () -> api.saveSecret(APP_KEY, Tuple.of("apiKey",
                        secrets().getSecrets().get("apiKey")), site, user(false)));
        verifyNoInteractions(secretsStore);
    }

    @Test
    public void nonAdministratorCannotDeleteAppConfiguration() throws Exception {
        final AppsAPIImpl api = newApi(true);
        assertThrows(DotSecurityException.class,
                () -> api.deleteSecrets(APP_KEY, site, user(false)));
        verify(secretsStore, never()).deleteValue(anyString());
    }

    @Test
    public void nonAdministratorCannotDeleteOneAppConfigurationValue() throws Exception {
        final AppsAPIImpl api = newApi(true);
        assertThrows(DotSecurityException.class,
                () -> api.deleteSecret(APP_KEY, Set.of("apiKey"), site, user(false)));
        verifyNoInteractions(secretsStore);
    }

    @Test
    public void nonAdministratorCannotReadAppConfigurationValues() throws Exception {
        final AppsAPIImpl api = newApi(true);
        assertThrows(DotSecurityException.class,
                () -> api.getSecrets(APP_KEY, site, user(false)));
        verifyNoInteractions(secretsStore);
    }

    @Test
    public void administratorCanDeleteAppConfiguration() throws Exception {
        newApi(false).deleteSecrets(APP_KEY, site, user(true));
        verify(secretsStore).deleteValue(AppsUtil.internalKey(APP_KEY, SITE_ID));
    }

    @Test
    public void appsPortletUserCanDeleteAppConfigurationWhenDelegationSwitchIsOn() throws Exception {
        Config.setProperty(AppsAPIImpl.APPS_ALLOW_PORTLET_CONFIGURATION, true);
        newApi(true).deleteSecrets(APP_KEY, site, user(false));
        verify(secretsStore).deleteValue(AppsUtil.internalKey(APP_KEY, SITE_ID));
    }

    @Test
    public void userWithoutAppsPortletCannotDeleteAppConfigurationWhenDelegationSwitchIsOn()
            throws Exception {
        Config.setProperty(AppsAPIImpl.APPS_ALLOW_PORTLET_CONFIGURATION, true);
        final AppsAPIImpl api = newApi(false);
        assertThrows(DotSecurityException.class,
                () -> api.deleteSecrets(APP_KEY, site, user(false)));
        verify(secretsStore, never()).deleteValue(anyString());
    }
}
