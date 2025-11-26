package com.dotcms.graphql.datafetcher.page;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.graphql.DotGraphQLContext;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import graphql.schema.DataFetchingEnvironment;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class RunningExperimentFetcherTest {

    private static UserAPI userAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        userAPI = APILocator.getUserAPI();
        user = userAPI.getSystemUser();
    }

    /**
     * MethodToTest: {@link RunningExperimentFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: Using the {@link RunningExperimentFetcher} on a content that does not
     * have a running experiment.
     * Expected Result: Should return null since no experiment is running on the page
     */
    @Test
    public void testGet_WithNullExperiment() throws Exception {

        final var fetcher = new RunningExperimentFetcher();
        final var environment = Mockito.mock(DataFetchingEnvironment.class);

        // Create a mock page contentlet
        final Contentlet mockPage = Mockito.mock(Contentlet.class);
        Mockito.when(mockPage.getIdentifier()).thenReturn("id-does-not-exist");

        Mockito.when(environment.getContext()).thenReturn(
                DotGraphQLContext.createServletContext().with(user).build()
        );
        Mockito.when(environment.getSource()).thenReturn(mockPage);

        final var result = fetcher.get(environment);
        assertNull(result);
    }

    /**
     * MethodToTest: {@link RunningExperimentFetcher#get(DataFetchingEnvironment)}
     * Given Scenario: Using the {@link RunningExperimentFetcher} on a page that has a running
     * experiment on it.
     * Expected Result: Should return the running experiment id.
     */
    @Test
    public void testGet_WithRunningExperiment() throws Exception {

        final var fetcher = new RunningExperimentFetcher();

        final var environment = Mockito.mock(DataFetchingEnvironment.class);

        // Creating a dummy experiment
        final Experiment experiment = new ExperimentDataGen().nextPersisted();

        // Validate the just created experiment
        final Experiment experimentFromDataBase = APILocator.getExperimentsAPI()
                .find(experiment.id().get(), APILocator.systemUser())
                .orElseThrow(() -> new AssertionError("Experiment not found"));
        assertNotNull(experimentFromDataBase.runningIds());
        assertFalse(experimentFromDataBase.runningIds().iterator().hasNext());

        // Starting the experiment
        final Experiment experimentStarted = APILocator.getExperimentsAPI()
                .start(experiment.id().get(), APILocator.systemUser());
        assertNotNull(experimentStarted);

        final Contentlet mockPage = Mockito.mock(Contentlet.class);
        Mockito.when(mockPage.getIdentifier()).thenReturn(experiment.pageId());

        Mockito.when(environment.getContext()).thenReturn(
                DotGraphQLContext.createServletContext().with(
                        APILocator.systemUser()
                ).build()
        );
        Mockito.when(environment.getSource()).thenReturn(mockPage);

        final var result = fetcher.get(environment);
        assertNotNull(result);
        assertEquals(experiment.getIdentifier(), result);
    }

}