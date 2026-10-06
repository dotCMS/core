package com.dotcms.experiments.business;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
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
     *   shipped default — while the kill-switch {@code FEATURE_FLAG_EXPERIMENTS} is at its own
     *   default of {@code true}.
     * Expected result: {@code isExperimentEnabled()} is {@code true}. The state #37005 ships by
     *   default must leave experiments serving to visitors (FR-014, SC-003).
     */
    @Test
    void isExperimentEnabled_entryPointSwitchOff_experimentsStillEnabled() {
        mockedConfig.when(() -> Config.getBooleanProperty(
                FeatureFlagName.FEATURE_FLAG_EXPERIMENTS_PORTLET, false)).thenReturn(false);

        assertTrue(ConfigExperimentUtil.INSTANCE.isExperimentEnabled(),
                "The entry-point switch being off must not take experiments off the air");
    }

    /**
     * Method to test: {@link ConfigExperimentUtil#notify(SystemTableUpdatedKeyEvent)}
     * Given scenario: An operator writes {@code FEATURE_FLAG_EXPERIMENTS_PORTLET} to the system
     *   table. Because {@code notify} matches with {@code contains}, and the entry-point switch's
     *   name is a superstring of the kill-switch's, that event reaches the kill-switch branch and
     *   re-resolves it.
     * Expected result: {@code isExperimentEnabled()} is still {@code true} — the re-resolution
     *   reads {@code FEATURE_FLAG_EXPERIMENTS}, not the key that arrived in the event
     *   (FR-014, FR-015a).
     *
     * <p>This is the assertion that makes the substring coupling safe to leave in place. If
     * {@code resolveFeatureFlag()} is ever changed to read the event's key, or the branch to
     * assign from it, this fails.
     */
    @Test
    void notify_experimentsPortletFlagEvent_leavesKillSwitchEnabled() {
        final SystemTableUpdatedKeyEvent event = mock(SystemTableUpdatedKeyEvent.class);
        when(event.getKey()).thenReturn(FeatureFlagName.FEATURE_FLAG_EXPERIMENTS_PORTLET);
        mockedConfig.when(() -> Config.getBooleanProperty(
                FeatureFlagName.FEATURE_FLAG_EXPERIMENTS, true)).thenReturn(true);

        ConfigExperimentUtil.INSTANCE.notify(event);

        assertTrue(ConfigExperimentUtil.INSTANCE.isExperimentEnabled(),
                "A system-table write to FEATURE_FLAG_EXPERIMENTS_PORTLET must not disable "
                        + "experiments, even though notify() matches keys by substring");
    }

    /**
     * Method to test: {@link FeatureFlagName}
     * Given scenario: The two switch names are compared.
     * Expected result: They are distinct properties, and the entry-point switch's name contains the
     *   kill-switch's — the fact that makes {@code notify}'s substring match couple them.
     *
     * <p>Documents the coupling rather than asserting it away. If a future rename breaks the
     * containment the coupling disappears, which is fine; this test then fails and points a reader
     * at {@link #notify_experimentsPortletFlagEvent_leavesKillSwitchEnabled()}, which can be
     * retired with it.
     */
    @Test
    void featureFlagNames_entryPointSwitchIsDistinctButNameContainsKillSwitch() {
        assertTrue(!FeatureFlagName.FEATURE_FLAG_EXPERIMENTS_PORTLET.equals(
                        FeatureFlagName.FEATURE_FLAG_EXPERIMENTS),
                "The entry-point switch must be a separate property from the kill-switch (FR-011a)");
        assertTrue(FeatureFlagName.FEATURE_FLAG_EXPERIMENTS_PORTLET.contains(
                        FeatureFlagName.FEATURE_FLAG_EXPERIMENTS),
                "If this no longer holds, notify()'s substring match no longer couples the two and "
                        + "notify_experimentsPortletFlagEvent_leavesKillSwitchEnabled can be retired");
    }
}
