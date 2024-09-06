package com.dotcms.experiments.business;

import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.experiments.model.AbstractExperiment;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.Scheduling;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExperimentFactoryIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ExperimentsFactory#listActive(String)}
     * When: Create two pages
     * - First one wit a RUNNING, DRAFT and ENDED and Archived Experiment
     * - Second one wit a SCHEDULUED, DRAFT and ENDED and Archived Experiment
     *
     * should: return 3 Experiments, for each Page.
     *
     */
    @Test
    public void listByPage() throws DotDataException, DotSecurityException {
        final Host host_1 = new SiteDataGen().nextPersisted();
        final Template template_1 = new TemplateDataGen().host(host_1).nextPersisted();
        final HTMLPageAsset page_1 = new HTMLPageDataGen(host_1, template_1).nextPersisted();
        final HTMLPageAsset page_2 = new HTMLPageDataGen(host_1, template_1).nextPersisted();

        final ExperimentsCreated experimentsCreated_1 = createExperiments(page_1);
        final Experiment runningExperiment = new ExperimentDataGen().page(page_1).nextPersistedAndStart();

        final Host host_2 = new SiteDataGen().nextPersisted();
        final ExperimentsCreated experimentsCreated_2 = createExperiments(page_2);
        final Experiment scheduledExperiment = createScheduledExperiment(page_2);

        try {
            final Collection<String> experiments_1 = FactoryLocator.getExperimentsFactory().listActive(page_1.getIdentifier())
                    .stream().map(Experiment::getIdentifier).collect(Collectors.toList());

            assertEquals(2, experiments_1.size());
            assertTrue(experiments_1.contains(runningExperiment.getIdentifier()));
            assertTrue(experiments_1.contains(experimentsCreated_1.draftExperiment.getIdentifier()));

            final Collection<String> experiments_2 = FactoryLocator.getExperimentsFactory().listActive(page_2.getIdentifier())
                    .stream().map(Experiment::getIdentifier).collect(Collectors.toList());

            assertEquals(2, experiments_2.size());
            assertTrue(experiments_2.contains(scheduledExperiment.getIdentifier()));
            assertTrue(experiments_2.contains(experimentsCreated_2.draftExperiment.getIdentifier()));
        } finally {
            APILocator.getExperimentsAPI().end(runningExperiment.getIdentifier(), APILocator.systemUser());
        }
    }

    /**
     * This method do:
     * - Create a Template and a Page in host
     * - Create a Page in the host and a Draft Experiment on it
     *  - Create a Page in the host and an Ended Experiment on it
     * - Create a Page in the host and a Running Experiment on it
     * - Create a Page in the host and an Archived Experiment on it
     * - Create a Page in the host and a Scheduled Experiment on it
     *
     * @param page
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private static ExperimentsCreated createExperiments(final HTMLPageAsset page) throws DotDataException,
            DotSecurityException {

        final User systemUser = APILocator.systemUser();
        final ExperimentsCreated experimentsCreated = new ExperimentsCreated();

        experimentsCreated.draftExperiment = new ExperimentDataGen().page(page).nextPersisted();
        experimentsCreated.endedExperiment = new ExperimentDataGen().page(page).nextPersistedAndStart();
        experimentsCreated.endedExperiment = APILocator.getExperimentsAPI().end(
                experimentsCreated.endedExperiment.getIdentifier(), systemUser);

        experimentsCreated.archivedExperiment = new ExperimentDataGen().page(page).nextPersistedAndStart();
        experimentsCreated.archivedExperiment = APILocator.getExperimentsAPI().end(
                experimentsCreated.archivedExperiment.getIdentifier(), systemUser);
        experimentsCreated.archivedExperiment = APILocator.getExperimentsAPI().archive(
                experimentsCreated.archivedExperiment.getIdentifier(), systemUser);

        return experimentsCreated;
    }

    private static Experiment createScheduledExperiment(final HTMLPageAsset page) throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.systemUser();

        Experiment experiment = new ExperimentDataGen()
                .page(page)
                .status(AbstractExperiment.Status.DRAFT)
                .scheduling(Scheduling.builder()
                        .startDate(Instant.now().plus(10, ChronoUnit.DAYS))
                        .endDate(Instant.now().plus(20, ChronoUnit.DAYS))
                        .build())
                .nextPersisted();

        return APILocator.getExperimentsAPI().start(experiment.getIdentifier(), systemUser);
    }

    private static class ExperimentsCreated {
        Experiment draftExperiment;
        Experiment endedExperiment;
        Experiment archivedExperiment;
        HTMLPageAsset page;
    }

}
