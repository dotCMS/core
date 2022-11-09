package com.dotcms.experiments.business;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExperimentAPIImpIT {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ExperimentsAPI#getExperimentRunning()}
     * When: You have tree Experiment:
     * - First one in DRAFT state, it has never been running.
     * - Second one in RUNNING state, it was already started and it was not stopped yet
     * - Finally one in STOP State, it was started, but it was stopped already
     */
    @Test
    public void getRunningExperiment() throws DotDataException, DotSecurityException {
        final Experiment draftExperiment = new ExperimentDataGen().nextPersisted();

        final Experiment runningExperiment = new ExperimentDataGen().nextPersisted();
        ExperimentDataGen.start(runningExperiment);

        final Experiment stoppedExperiment = new ExperimentDataGen().nextPersisted();
        ExperimentDataGen.start(stoppedExperiment);

        try {
            List<Experiment> experimentRunning = APILocator.getExperimentsAPI()
                    .getExperimentRunning();

            List<String> experiemtnsId = experimentRunning.stream()
                    .map(experiment -> experiment.getIdentifier()).collect(Collectors.toList());

            assertTrue(experiemtnsId.contains(runningExperiment.getIdentifier()));
            assertFalse(experiemtnsId.contains(draftExperiment.getIdentifier()));
            assertTrue(experiemtnsId.contains(stoppedExperiment.getIdentifier()));

            ExperimentDataGen.end(stoppedExperiment);

            experimentRunning = APILocator.getExperimentsAPI()
                    .getExperimentRunning();
            experiemtnsId = experimentRunning.stream()
                    .map(experiment -> experiment.getIdentifier()).collect(Collectors.toList());

            assertTrue(experiemtnsId.contains(runningExperiment.getIdentifier()));
            assertFalse(experiemtnsId.contains(draftExperiment.getIdentifier()));
            assertFalse(experiemtnsId.contains(stoppedExperiment.getIdentifier()));
        } finally {
            ExperimentDataGen.end(runningExperiment);
            ExperimentDataGen.end(stoppedExperiment);
        }
    }
}
