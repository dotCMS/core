package com.dotcms.experiments.business;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.dotcms.business.SystemTableUpdatedKeyEvent;
import com.dotcms.featureflag.FeatureFlagName;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link ConfigExperimentUtil}, pinning its <b>independence</b> from the UVE
 * Experiments entry-point switch {@code FEATURE_FLAG_EXPERIMENTS_PORTLET} introduced by #37005.
 *
 * <p>Why this class exists: {@code FEATURE_FLAG_EXPERIMENTS} is the kill-switch for the entire
 * Experiments feature. {@link ConfigExperimentUtil#isExperimentEnabled()} gates experiment
 * JavaScript injection into rendered pages and experiment resolution during page render, so its
 * value decides whether running experiments reach site visitors at all. #37005 requires that its
 * new entry-point switch never move that value (FR-014, FR-015a) and that turning the entry point
 * off leave live experiments serving unchanged (SC-003). Those are negatives, and a negative is
 * only a promise until something asserts it.
 *
 * <p><b>The specific trap.</b> {@link ConfigExperimentUtil#notify} matches its key with
 * {@code event.getKey().contains(FEATURE_FLAG_EXPERIMENTS_KEY)} — a substring test. The new
 * switch's name, {@code FEATURE_FLAG_EXPERIMENTS_PORTLET}, <i>contains</i>
 * {@code FEATURE_FLAG_EXPERIMENTS}, so a system-table write to the entry-point switch also
 * satisfies that branch and re-resolves the kill-switch. The re-resolution reads the correct
 * property and so lands on the correct value, which is why this is a latent coupling rather than a
 * live defect — but it is one line away from becoming one, and nothing else in the codebase would
 * notice. {@link #notify_experimentsPortletFlagEvent_leavesKillSwitchEnabled()} is the guard.
 */
public class ConfigExperimentUtilTest {

    private MockedStatic<Config> mockedConfig;
    private MockedStatic<APILocator> mockedApiLocator;

    @BeforeEach
    void setUp() {
        // Both statics must be open before ConfigExperimentUtil is first touched: it is an enum
        // singleton, so its constructor runs at class-initialization and calls both
        // Config.getBooleanProperty and APILocator.getLocalSystemEventsAPI().subscribe(...).
        mockedConfig = mockStatic(Config.class);
        mockedConfig.when(() -> Config.getBooleanProperty(anyString(), anyBoolean()))
                .thenAnswer(inv -> inv.getArgument(1));

        mockedApiLocator = mockStatic(APILocator.class);
        mockedApiLocator.when(APILocator::getLocalSystemEventsAPI)
                .thenReturn(mock(LocalSystemEventsAPI.class));
    }

    @AfterEach
    void tearDown() {
        mockedApiLocator.close();
        mockedConfig.close();
    }

    /**
     * Method to test: {@link ConfigExperimentUtil#isExperimentEnabled()}
     * Given scenario: The entry-point switch {@code FEATURE_FLAG_EXPERIMENTS_PORTLET} is off — its
     *   shipped default — while the kill-switch {@code FEATURE_FLAG_EXPERIMENTS} is explicitly on.
     * Expected result: {@code isExperimentEnabled()} is {@code true}. Turning the portlet entry
     *   point off must not take running experiments off the air.
     */
    @Test
    void isExperimentEnabled_entryPointSwitchOff_experimentsStillEnabled() {
        ConfigExperimentUtil.INSTANCE.setExperimentEnabled(true);  // explicit: default is now false
        mockedConfig.when(() -> Config.getBooleanProperty(
                FeatureFlagName.FEATURE_FLAG_EXPERIMENTS_PORTLET, false)).thenReturn(false);

        assertTrue(ConfigExperimentUtil.INSTANCE.isExperimentEnabled(),
                "The entry-point switch being off must not take experiments off the air");
    }

    /**
     * Method to test: {@link ConfigExperimentUtil#notify(SystemTableUpdatedKeyEvent)}
     * Given scenario: An operator writes {@code FEATURE_FLAG_EXPERIMENTS_PORTLET} to the system
     *   table. The kill-switch is explicitly set to {@code true} beforehand.
     * Expected result: {@code isExperimentEnabled()} is still {@code true} — a write to the
     *   portlet entry-point switch must not affect the kill-switch.
     *
     * <p>With the live-toggle removed, {@code notify()} no longer has a branch for
     *   {@code FEATURE_FLAG_EXPERIMENTS}, so the flag value is unchanged regardless of what Config
     *   returns. The test therefore no longer relies on the substring-match coupling that existed
     *   before the live-toggle was removed.
     */
    @Test
    void notify_experimentsPortletFlagEvent_leavesKillSwitchEnabled() {
        ConfigExperimentUtil.INSTANCE.setExperimentEnabled(true);  // explicit: default is now false

        final SystemTableUpdatedKeyEvent event = mock(SystemTableUpdatedKeyEvent.class);
        when(event.getKey()).thenReturn(FeatureFlagName.FEATURE_FLAG_EXPERIMENTS_PORTLET);
        ConfigExperimentUtil.INSTANCE.notify(event);

        assertTrue(ConfigExperimentUtil.INSTANCE.isExperimentEnabled(),
                "A system-table write to FEATURE_FLAG_EXPERIMENTS_PORTLET must not disable "
                        + "experiments");
    }

    /**
     * Method to test: {@link FeatureFlagName}
     * Given scenario: The two switch names are compared.
     * Expected result: They are distinct properties. The entry-point switch's name contains the
     *   kill-switch's as a substring — a historical coupling that is now harmless because the
     *   {@code FEATURE_FLAG_EXPERIMENTS} branch was removed from {@code notify()}, making
     *   live-toggle impossible regardless of substring matching.
     */
    @Test
    void featureFlagNames_entryPointSwitchIsDistinctButNameContainsKillSwitch() {
        assertTrue(!FeatureFlagName.FEATURE_FLAG_EXPERIMENTS_PORTLET.equals(
                        FeatureFlagName.FEATURE_FLAG_EXPERIMENTS),
                "The entry-point switch must be a separate property from the kill-switch");
        assertTrue(FeatureFlagName.FEATURE_FLAG_EXPERIMENTS_PORTLET.contains(
                        FeatureFlagName.FEATURE_FLAG_EXPERIMENTS),
                "Documented: the portlet switch name contains the kill-switch name as a substring");
    }

    /**
     * Method to test: {@link ConfigExperimentUtil#isExperimentEnabled()} default resolution.
     * Given scenario: {@code FEATURE_FLAG_EXPERIMENTS} is absent from the system config — the
     *   mock returns the default argument passed to {@link com.dotmarketing.util.Config#getBooleanProperty}.
     * Expected result: The resolved value is {@code false}. This pin-points the default changing
     *   from {@code true} to {@code false}: before the change the invocation passed {@code true}
     *   as the default and the mock returned {@code true}; after the change it passes {@code false}
     *   and the mock returns {@code false}.
     */
    @Test
    void resolveFeatureFlag_keyAbsentFromConfig_defaultIsFalse() throws Exception {
        // The setUp mock returns inv.getArgument(1) — the default — for any getBooleanProperty.
        // Use reflection to call the private resolveFeatureFlag() so we can assert its return
        // value directly without relying on the singleton constructor's initialization order.
        final java.lang.reflect.Method method =
                ConfigExperimentUtil.class.getDeclaredMethod("resolveFeatureFlag");
        method.setAccessible(true);
        final boolean resolved = (boolean) method.invoke(ConfigExperimentUtil.INSTANCE);

        assertFalse(resolved,
                "FEATURE_FLAG_EXPERIMENTS default must be false when the key is absent from config");
    }

    /**
     * Method to test: {@link ConfigExperimentUtil#notify(SystemTableUpdatedKeyEvent)}
     * Given scenario: A system-table write arrives for the {@code FEATURE_FLAG_EXPERIMENTS} key.
     *   Config is stubbed so that if re-resolution were to occur it would return {@code true}.
     *   The current value of the flag is set to {@code false} before the event fires.
     * Expected result: {@code isExperimentEnabled()} still returns {@code false} — the live-toggle
     *   for {@code FEATURE_FLAG_EXPERIMENTS} has been removed, so {@code notify()} must not
     *   re-resolve this flag regardless of what Config says.
     */
    @Test
    void notify_featureFlagExperimentsKey_doesNotResolveFlag() {
        ConfigExperimentUtil.INSTANCE.setExperimentEnabled(false);

        // If the FEATURE_FLAG_EXPERIMENTS branch were still in notify(), it would call
        // resolveFeatureFlag() which reads Config and would land on true — changing the flag.
        mockedConfig.when(() -> Config.getBooleanProperty(
                eq(FeatureFlagName.FEATURE_FLAG_EXPERIMENTS), anyBoolean())).thenReturn(true);

        final SystemTableUpdatedKeyEvent event = mock(SystemTableUpdatedKeyEvent.class);
        when(event.getKey()).thenReturn(FeatureFlagName.FEATURE_FLAG_EXPERIMENTS);
        ConfigExperimentUtil.INSTANCE.notify(event);

        assertFalse(ConfigExperimentUtil.INSTANCE.isExperimentEnabled(),
                "notify() with FEATURE_FLAG_EXPERIMENTS must not re-resolve the flag — live-toggle removed");
    }

    /**
     * Method to test: {@link ConfigExperimentUtil#notify(SystemTableUpdatedKeyEvent)}
     * Given scenario: A system-table write arrives for the {@code FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS} key.
     *   Config is stubbed to return {@code true} for that key.
     *   The current value is set to {@code false} before the event fires.
     * Expected result: {@code isCaemExperimentResultsEnabled()} returns {@code true} — the live-toggle
     *   for this flag must remain intact.
     */
    @Test
    void notify_caemExperimentResultsKey_doesResolveFlag() {
        ConfigExperimentUtil.INSTANCE.setCaemExperimentResultsEnabled(false);

        mockedConfig.when(() -> Config.getBooleanProperty(
                eq(FeatureFlagName.FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS), anyBoolean())).thenReturn(true);

        final SystemTableUpdatedKeyEvent event = mock(SystemTableUpdatedKeyEvent.class);
        when(event.getKey()).thenReturn(FeatureFlagName.FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS);
        ConfigExperimentUtil.INSTANCE.notify(event);

        assertTrue(ConfigExperimentUtil.INSTANCE.isCaemExperimentResultsEnabled(),
                "notify() with FEATURE_FLAG_CAEM_EXPERIMENT_RESULTS must still re-resolve the flag");
    }
}
