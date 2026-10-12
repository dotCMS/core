package com.dotcms.experiments.business;

import com.dotcms.experiments.business.ExperimentsAPI;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static com.dotcms.experiments.model.AbstractExperiment.Status.ENDED;
import static com.dotcms.experiments.model.AbstractExperiment.Status.RUNNING;
import static com.dotcms.experiments.model.AbstractExperiment.Status.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExperimentsAPIImpl#isFreeSlotUsed()}.
 *
 * <p>The free-slot check counts all experiments in {@code {RUNNING, SCHEDULED, ENDED}} states
 * system-wide and returns {@code true} if the count is greater than zero. This is used to enforce
 * the one-experiment limit in limited mode (when {@code FEATURE_FLAG_EXPERIMENTS=false}).
 *
 * @author dotCMS
 * @since Oct 2026
 */
public class ExperimentsAPIImplTest {

    private MockedStatic<APILocator> apiLocatorMock;
    private ExperimentsFactory experimentsFactory;

    @BeforeEach
    void setUp() {
        experimentsFactory = mock(ExperimentsFactory.class);

        // Only getLocalSystemEventsAPI() must return a non-null mock — the constructor
        // calls subscribe() on it twice. All other field-init calls can safely return null
        // because isFreeSlotUsed() only calls factory.countByStatuses(), which is the mock we pass.
        apiLocatorMock = mockStatic(APILocator.class);
        apiLocatorMock.when(APILocator::getLocalSystemEventsAPI)
                .thenReturn(mock(LocalSystemEventsAPI.class));
    }

    @AfterEach
    void tearDown() {
        apiLocatorMock.close();
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#isFreeSlotUsed()}
     * Given scenario: No experiments exist in any of the {@code {RUNNING, SCHEDULED, ENDED}} states.
     * Expected result: {@code false} — the free slot is available.
     *
     * <p>The factory stub is keyed on the exact status set, so the test also fails if the
     * implementation queries with the wrong statuses (factory returns {@code 0} via default stub).
     */
    @Test
    void isFreeSlotUsed_noMatchingExperiments_returnsFalse() throws DotDataException {
        when(experimentsFactory.countByStatuses(argThat(isOccupyingStatusSet())))
                .thenReturn(0);

        assertFalse(new ExperimentsAPIImpl(experimentsFactory).isFreeSlotUsed(),
                "Count of zero for {RUNNING, SCHEDULED, ENDED} — slot must be free");
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#isFreeSlotUsed()}
     * Given scenario: At least one experiment in {@code {RUNNING, SCHEDULED, ENDED}} exists.
     * Expected result: {@code true} — the free slot is occupied.
     *
     * <p>The factory stub is keyed on the exact status set, so the test also fails if the
     * implementation queries with wrong statuses (factory returns {@code 0} via default stub → false).
     */
    @Test
    void isFreeSlotUsed_experimentsExist_returnsTrue() throws DotDataException {
        when(experimentsFactory.countByStatuses(argThat(isOccupyingStatusSet())))
                .thenReturn(1);

        assertTrue(new ExperimentsAPIImpl(experimentsFactory).isFreeSlotUsed(),
                "Count > 0 for {RUNNING, SCHEDULED, ENDED} must mark the slot as used");
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#validateScheduling(Scheduling)}
     * Given scenario: Feature flag disabled (limited mode); scheduling carries only a
     *                 {@code startDate} with no {@code endDate}.
     * Expected result: {@code endDate} is auto-set to exactly {@link ExperimentLimitedModeGate#MAX_DAYS}
     *                  days from {@code startDate}, enforcing the 10-day limited-mode cap.
     */
    @Test
    void validateScheduling_limitedMode_startDateOnly_capsEndDateAtMaxDays() {
        ConfigExperimentUtil.INSTANCE.setExperimentEnabled(false);
        try {
            final Instant startDate = Instant.now().plus(1, ChronoUnit.DAYS);
            final Scheduling input = Scheduling.builder().startDate(startDate).build();

            final Scheduling result = new ExperimentsAPIImpl(experimentsFactory).validateScheduling(input);

            assertTrue(result.endDate().isPresent(), "endDate must be auto-set in limited mode");
            assertEquals(ExperimentLimitedModeGate.MAX_DAYS,
                    ChronoUnit.DAYS.between(startDate, result.endDate().get()),
                    "endDate must be capped at MAX_DAYS (10) in limited mode");
        } finally {
            ConfigExperimentUtil.INSTANCE.setExperimentEnabled(true);
        }
    }

    /**
     * Method to test: {@link ExperimentsAPIImpl#validateScheduling(Scheduling)}
     * Given scenario: Feature flag enabled (normal mode); scheduling carries only a
     *                 {@code startDate} with no {@code endDate}.
     * Expected result: {@code endDate} is auto-set to the configured default duration from
     *                  {@code startDate}, not the limited-mode cap.
     */
    @Test
    void validateScheduling_normalMode_startDateOnly_setsDefaultEndDate() {
        ConfigExperimentUtil.INSTANCE.setExperimentEnabled(true);
        try {
            final Instant startDate = Instant.now().plus(1, ChronoUnit.DAYS);
            final Scheduling input = Scheduling.builder().startDate(startDate).build();

            final Scheduling result = new ExperimentsAPIImpl(experimentsFactory).validateScheduling(input);

            final long expectedDays = Config.getIntProperty(
                    ExperimentsAPI.EXPERIMENTS_DEFAULT_DURATION_KEY, 14);
            assertTrue(result.endDate().isPresent(), "endDate must be auto-set in normal mode");
            assertEquals(expectedDays,
                    ChronoUnit.DAYS.between(startDate, result.endDate().get()),
                    "endDate must be defaultDuration days from startDate in normal mode");
        } finally {
            ConfigExperimentUtil.INSTANCE.setExperimentEnabled(false);
        }
    }

    /**
     * Returns an {@code argThat} predicate that matches a {@link Set} containing exactly
     * {@code RUNNING}, {@code SCHEDULED}, and {@code ENDED}. Using this as the stub matcher
     * guarantees that any call with a different status set does not satisfy the stub.
     */
    private static org.mockito.ArgumentMatcher<Set<com.dotcms.experiments.model.AbstractExperiment.Status>>
            isOccupyingStatusSet() {
        final Set<com.dotcms.experiments.model.AbstractExperiment.Status> expected =
                Set.of(RUNNING, SCHEDULED, ENDED);
        return statuses -> statuses != null && statuses.equals(expected);
    }
}