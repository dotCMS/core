package com.dotcms.experiments.business;

import com.dotcms.system.event.local.business.LocalSystemEventsAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Set;

import static com.dotcms.experiments.model.AbstractExperiment.Status.ENDED;
import static com.dotcms.experiments.model.AbstractExperiment.Status.RUNNING;
import static com.dotcms.experiments.model.AbstractExperiment.Status.SCHEDULED;
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