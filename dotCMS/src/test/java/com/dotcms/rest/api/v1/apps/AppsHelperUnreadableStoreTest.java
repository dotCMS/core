package com.dotcms.rest.api.v1.apps;

import com.dotcms.security.apps.AppDescriptor;
import com.dotcms.security.apps.AppDescriptorLoadError;
import com.dotcms.security.apps.AppsAPI;
import com.dotcms.security.apps.SecretsStoreUnreadableException;
import com.dotcms.util.SecurityLoggerServiceAPI;
import com.dotcms.rest.api.v1.apps.view.AppView;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the Apps portlet listing when this node cannot read the App secrets store.
 *
 * <p>Context: the listing calls {@code filterSitesForAppKey(appKey, appsAPI.appKeysByHost().keySet(), user)}.
 * {@code appKeysByHost()} is evaluated as the *argument*, so the cause-chain guard inside
 * {@code filterSitesForAppKey} never sees the failure -- it is raised before that method is entered.
 * Before this guard the listing returned a 500, which contradicted the documented behaviour that it
 * "reports 0 configurations".
 *
 * <p>The contract asserted here is that the listing degrades to zero counts <em>and says so</em>: a
 * silent zero on the secrets screen reads as "your secrets are gone", which is the misreading that
 * would prompt an administrator to re-enter them.
 *
 * <p>Note the guard lives at this call site rather than inside {@code appKeysByHost()} on purpose.
 * {@code removeApp()}, {@code removeSecretsForSite()} and {@code exportSecrets()} read it too, and an
 * empty map there would turn the removes into silent no-ops and make {@code exportSecrets()} write an
 * empty backup that reports success.
 */
public class AppsHelperUnreadableStoreTest {

    private AppsAPI appsAPI;
    private AppsHelper helper;
    private User user;

    /** The shape production actually produces: re-wrapped as a bare DotRuntimeException. */
    private static DotRuntimeException wrappedStoreFailure() {
        return new DotRuntimeException(new SecretsStoreUnreadableException(
                "cannot read dotSecretsStore.p12", new IOException("wrong password")));
    }

    private static AppDescriptor descriptor(final String key, final String name) {
        final AppDescriptor descriptor = mock(AppDescriptor.class);
        when(descriptor.getKey()).thenReturn(key);
        when(descriptor.getName()).thenReturn(name);
        return descriptor;
    }

    @Before
    public void setUp() throws Exception {
        appsAPI = mock(AppsAPI.class);
        user = mock(User.class);
        // collectLoadErrors() re-reads the app YAML directories, which pulls in ES infrastructure
        // that cannot initialize in a unit test (and fails with an Error, which its own
        // catch (Exception) does not stop). Stubbed out: YAML descriptor errors are a separate
        // concern from the store condition under test here.
        helper = new AppsHelper(appsAPI, mock(HostAPI.class), mock(PermissionAPI.class),
                mock(SecurityLoggerServiceAPI.class)) {
            @Override
            List<AppDescriptorLoadError> collectLoadErrors() {
                return List.of();
            }
        };
        // Built before the when(...) below: creating mocks inside thenReturn(...) interrupts
        // Mockito's ongoing stubbing and fails with UnfinishedStubbing.
        final List<AppDescriptor> descriptors = List.of(
                descriptor("dotAnalytics", "Analytics"), descriptor("dotEMA", "EMA"));
        when(appsAPI.getAppDescriptors(any(User.class))).thenReturn(descriptors);
    }

    /**
     * Given an unreadable store, when the apps are listed, then every app is still returned with a
     * count of zero and a dedicated error entry explains why.
     */
    @Test
    public void unreadableStore_listsAppsWithZeroCountsAndReportsTheCondition() throws Exception {
        when(appsAPI.appKeysByHost()).thenThrow(wrappedStoreFailure());

        final Tuple2<List<AppView>, List<AppDescriptorLoadError>> result =
                helper.getAvailableDescriptorViewsWithErrors(user, null);

        assertEquals("Every app should still be listed", 2, result._1.size());
        result._1.forEach(view -> assertEquals(
                "An unreadable store must not invent a configuration count",
                0, view.getConfigurationsCount()));

        assertEquals("The condition must be reported, not degraded silently", 1, result._2.size());
        final AppDescriptorLoadError error = result._2.get(0);
        assertEquals(AppDescriptorLoadError.SECRETS_STORE_UNREADABLE_ERROR_CODE,
                error.getErrorCode());
        assertEquals("dotSecretsStore.p12", error.getFileName());
        assertTrue("The message should reassure that nothing was lost",
                error.getMessage().contains("intact"));
    }

    /**
     * An unrelated runtime failure (a database blip surfaces as a plain DotRuntimeException too)
     * must still propagate -- degrading it would hide a real outage behind "0 configurations".
     */
    @Test
    public void unrelatedRuntimeFailure_isNotMasked() throws Exception {
        when(appsAPI.appKeysByHost()).thenThrow(new DotRuntimeException("the database went away"));

        final DotRuntimeException thrown = assertThrows(DotRuntimeException.class,
                () -> helper.getAvailableDescriptorViewsWithErrors(user, null));
        assertEquals("the database went away", thrown.getMessage());
    }

    /**
     * The happy path must keep its real counts, guarding against an over-broad catch: two sites
     * configured for one app, none for the other.
     */
    @Test
    public void readableStore_keepsRealCountsAndReportsNoError() throws Exception {
        when(appsAPI.appKeysByHost()).thenReturn(Map.of(
                "site-1", Set.of("dotAnalytics"), "site-2", Set.of("dotAnalytics")));
        when(appsAPI.filterSitesForAppKey(anyString(), any(), any(User.class)))
                .thenAnswer(inv -> "dotAnalytics".equals(inv.getArgument(0))
                        ? Set.of("site-1", "site-2") : Set.of());
        when(appsAPI.computeWarningsBySite(any(AppDescriptor.class), any(), any(User.class)))
                .thenReturn(Map.of());

        final Tuple2<List<AppView>, List<AppDescriptorLoadError>> result =
                helper.getAvailableDescriptorViewsWithErrors(user, null);

        assertTrue("A readable store must report no store error", result._2.isEmpty());
        // Sorted by count descending, so the configured app comes first.
        assertEquals(2, result._1.get(0).getConfigurationsCount());
        assertEquals(0, result._1.get(1).getConfigurationsCount());
    }
}
