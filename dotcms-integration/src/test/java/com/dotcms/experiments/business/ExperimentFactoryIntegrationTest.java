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
     * When: Create 2 host and for each Host create the follow:
     * - Create a Page with Draft Experiment on it
     * - Create a Page with an Ended Experiment on it
     * - Create a Page with a Running Experiment on it
     * - Create a Page with an Archived Experiment on it
     * - Create a Page with a Scheduled Experiment on it
     *
     * and then call the method with one on this host
     *
     * should: return 3 Experiments, these are the DRAFT, RUNNING and Scheduled Experiment on the host
     * that was used as parameter
     *
     */
    @Test
    public void listByHost() throws DotDataException, DotSecurityException {
        final Host host_1 = new SiteDataGen().nextPersisted();
        final ExperimentsCreated experimentsCreated_1 = createExperiments(host_1);

        final Host host_2 = new SiteDataGen().nextPersisted();
        final ExperimentsCreated experimentsCreated_2 = createExperiments(host_2);

        try {
            final Collection<String> experiments = FactoryLocator.getExperimentsFactory().listActive(host_1.getIdentifier())
                    .stream().map(Experiment::getIdentifier).collect(Collectors.toList());

            assertEquals(3, experiments.size());
            assertTrue(experiments.contains(experimentsCreated_1.scheduledExperiment.getIdentifier()));
            assertTrue(experiments.contains(experimentsCreated_1.runningExperiment.getIdentifier()));
            assertTrue(experiments.contains(experimentsCreated_1.draftExperiment.getIdentifier()));
        } finally {
            APILocator.getExperimentsAPI().end(experimentsCreated_1.runningExperiment.getIdentifier(), APILocator.systemUser());
            APILocator.getExperimentsAPI().end(experimentsCreated_2.runningExperiment.getIdentifier(), APILocator.systemUser());
        }
    }

    /**
     * This method do:
     * - Create a Template and a Page in host
     * - Create a Page in the host and a Draft Experiment on it
<<<<<<< HEAD
     *  - Create a Page in the host and a Ended Experiment on it
=======
     *  - Create a Page in the host and an Ended Experiment on it
>>>>>>> origin/master
     * - Create a Page in the host and a Running Experiment on it
     * - Create a Page in the host and an Archived Experiment on it
     * - Create a Page in the host and a Scheduled Experiment on it
     *
     * @param host
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private static ExperimentsCreated createExperiments(final Host host) throws DotDataException, DotSecurityException {
        final Template template_1 = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset page_1 = new HTMLPageDataGen(host, template_1).nextPersisted();
        final HTMLPageAsset page_2 = new HTMLPageDataGen(host, template_1).nextPersisted();
        final HTMLPageAsset page_3 = new HTMLPageDataGen(host, template_1).nextPersisted();
        final HTMLPageAsset page_4 = new HTMLPageDataGen(host, template_1).nextPersisted();
        final HTMLPageAsset page_5 = new HTMLPageDataGen(host, template_1).nextPersisted();

        final User systemUser = APILocator.systemUser();
        final ExperimentsCreated experimentsCreated = new ExperimentsCreated();

        experimentsCreated.draftExperiment = new ExperimentDataGen().page(page_1).nextPersisted();
        experimentsCreated.endedExperiment = new ExperimentDataGen().page(page_2).nextPersistedAndStart();
        experimentsCreated.endedExperiment = APILocator.getExperimentsAPI().end(
                experimentsCreated.endedExperiment.getIdentifier(), systemUser);

        experimentsCreated.archivedExperiment = new ExperimentDataGen().page(page_3).nextPersistedAndStart();
        experimentsCreated.archivedExperiment = APILocator.getExperimentsAPI().end(
                experimentsCreated.archivedExperiment.getIdentifier(), systemUser);
        experimentsCreated.archivedExperiment = APILocator.getExperimentsAPI().archive(
                experimentsCreated.archivedExperiment.getIdentifier(), systemUser);

        experimentsCreated.runningExperiment = new ExperimentDataGen().page(page_4).nextPersistedAndStart();

        experimentsCreated.scheduledExperiment = new ExperimentDataGen()
                .page(page_5)
                .status(AbstractExperiment.Status.DRAFT)
                .scheduling(Scheduling.builder()
                        .startDate(Instant.now().plus(10, ChronoUnit.DAYS))
                        .endDate(Instant.now().plus(20, ChronoUnit.DAYS))
                        .build())
                .nextPersisted();

        experimentsCreated.scheduledExperiment = APILocator.getExperimentsAPI().start(
                experimentsCreated.scheduledExperiment.getIdentifier(), systemUser);

        return experimentsCreated;
    }

    private static class ExperimentsCreated {
        Experiment draftExperiment;
        Experiment endedExperiment;
        Experiment runningExperiment;
        Experiment scheduledExperiment;
        Experiment archivedExperiment;
        HTMLPageAsset page;
    }

}
