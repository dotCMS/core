package com.dotcms.experiments.business.web;


import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.datagen.ExperimentDataGen;
import com.dotcms.experiments.model.AbstractExperiment.Status;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.TargetingCondition;
import com.dotcms.experiments.model.TrafficProportion;
import com.dotcms.mock.response.DotCMSMockResponse;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.rules.model.LogicalOperator;

import com.dotmarketing.util.Calendar;

import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.BeforeClass;
import org.junit.Test;

public class ExperimentWebAPIImplIT {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link ExperimentWebAPIImpl#isUserIncluded(HttpServletRequest, HttpServletResponse, List)}
     * When: You have one experiment running with 100% of traffic allocation and without targeting
     * Should: Return this experiment all the time
     *
     * @throws DotDataException
     */
    @Test
    public void isUserIncluded() throws DotDataException, DotSecurityException {
        final Experiment experiment = new ExperimentDataGen().trafficAllocation(100).nextPersisted();

        try {
            final Experiment experimentStarted = ExperimentDataGen.start(experiment);

            final HTMLPageAsset htmlPageAsset = getExperimentPage(experiment);

            final HttpServletRequest request = mock(HttpServletRequest.class);

            for (int i = 0; i < 10; i++) {
                final DotCMSMockResponse response = new DotCMSMockResponse();

                final SelectedExperiments selectedExperiments = WebAPILocator.getExperimentWebAPI()
                        .isUserIncluded(request, response, null);

                assertEquals(1, selectedExperiments.getExperiments().size());
                assertEquals(experiment.id().get(), selectedExperiments.getExperiments().get(0).id());
                assertEquals(htmlPageAsset.getURI(), selectedExperiments.getExperiments().get(0).pageUrl());

                assertEquals(1, selectedExperiments.getIncludedExperimentIds().size());
                assertEquals(experiment.id().get(), selectedExperiments.getIncludedExperimentIds().get(0));

                assertTrue(selectedExperiments.getExcludedExperimentIds().isEmpty());

                checkCookie(response, selectedExperiments.getExperiments().get(0),
                        experimentStarted.scheduling().get().endDate().get());

            }
        } finally {
            ExperimentDataGen.end(experiment);
        }
    }

    /**
     * Method to test: {@link ExperimentWebAPIImpl#isUserIncluded(HttpServletRequest, HttpServletResponse, List)}
     * When: You have 2 experiments running with 100% of traffic allocation and without targeting
     * Should: Return the two experiments all the time
     *
     * @throws DotDataException
     */
    @Test
    public void isUserIncludedInMoreThanOneExperiment() throws DotDataException, DotSecurityException {
        final Experiment experiment_1 = new ExperimentDataGen().trafficAllocation(100).nextPersisted();
        final Experiment experiment_2 = new ExperimentDataGen().trafficAllocation(100).nextPersisted();

        try {
            final Experiment experimentStarted_1 = ExperimentDataGen.start(experiment_1);
            final Experiment experimentStarted_2 = ExperimentDataGen.start(experiment_2);

            final HTMLPageAsset htmlPageAsset_1 = getExperimentPage(experimentStarted_1);
            final HTMLPageAsset htmlPageAsset_2 = getExperimentPage(experimentStarted_2);

            final HttpServletRequest request = mock(HttpServletRequest.class);

            for (int i = 0; i < 10; i++) {
                final DotCMSMockResponse response = new DotCMSMockResponse();


                final SelectedExperiments selectedExperiments = WebAPILocator.getExperimentWebAPI()
                        .isUserIncluded(request, response, null);

                assertEquals(2, selectedExperiments.getExperiments().size());

                for (SelectedExperiment selectedExperiment : selectedExperiments.getExperiments()) {
                    if (selectedExperiment.id().equals(experiment_1.id())) {
                        assertEquals(htmlPageAsset_1.getPageUrl(), selectedExperiment.pageUrl());
                        checkCookie(response, selectedExperiment, experiment_1.scheduling().get().endDate().get());
                    } else if (selectedExperiment.id().equals(experiment_2.id())) {
                        assertEquals(htmlPageAsset_2.getPageUrl(), selectedExperiment.pageUrl());
                        checkCookie(response, selectedExperiment, experiment_2.scheduling().get().endDate().get());
                    }
                }

                assertEquals(2, selectedExperiments.getIncludedExperimentIds().size());
                assertTrue(selectedExperiments.getIncludedExperimentIds().contains(experiment_1.id().get()));
                assertTrue(selectedExperiments.getIncludedExperimentIds().contains(experiment_2.id().get()));

                assertTrue(selectedExperiments.getExcludedExperimentIds().isEmpty());
            }
        } finally {
            ExperimentDataGen.end(experiment_1);
            ExperimentDataGen.end(experiment_2);
        }
    }

    private void checkCookie(final DotCMSMockResponse response,
            final SelectedExperiment experimentSelected)  {
        checkCookie(response, experimentSelected, null);
    }

    private void checkCookie(final DotCMSMockResponse response,
            final SelectedExperiment experimentSelected, final Instant expire)  {

        final Cookie runningExperimentCookie = response.getCookie("runningExperiment_" + experimentSelected.id())
                .orElseThrow(() -> new AssertionError(
                        "The cookie " + "runningExperiment_" + experimentSelected.id() +" should exists"));

        final String[] cookieValues = runningExperimentCookie.getValue().split(StringPool.AMPERSAND);
        final Map<String, String> cookiesValueMap = new HashMap<>();

        for (String cookieValue : cookieValues) {
            final String[] split = cookieValue.split(StringPool.COLON);
            cookiesValueMap.put(split[0], split[1]);
        }

        assertEquals(experimentSelected.id(), cookiesValueMap.get("experiment"));
        assertEquals(experimentSelected.variant().name(), cookiesValueMap.get("variant"));
        assertNotNull(cookiesValueMap.get("lookBackWindow"));

        if (UtilMethods.isSet(expire)) {
            Duration res = Duration.between(Instant.now(), expire);
            assertEquals(res.getSeconds(), runningExperimentCookie.getMaxAge());
        }
    }

    /**
     * Method to test: {@link ExperimentWebAPIImpl#isUserIncluded(HttpServletRequest, HttpServletResponse, List)}
     * When: You have none experiment
     * Should: Should return {@link ExperimentWebAPIImpl#NONE_EXPERIMENT}
     *
     * @throws DotDataException
     */
    @Test
    public void noExperimentRunning() throws DotDataException, DotSecurityException {
        final HttpServletRequest request = mock(HttpServletRequest.class);

        final Instant expireDate = Instant.now().plus(30, ChronoUnit.DAYS);

        for (int i = 0; i < 10; i++) {

            final DotCMSMockResponse response = new DotCMSMockResponse();
            final SelectedExperiments selectedExperiments = WebAPILocator.getExperimentWebAPI()
                    .isUserIncluded(request, response,  null);

            assertEquals(1, selectedExperiments.getExperiments().size());
            assertEquals(ExperimentWebAPI.NONE_EXPERIMENT.id(), selectedExperiments.getExperiments()
                    .get(0).id());
            assertEquals(ExperimentWebAPI.NONE_EXPERIMENT.pageUrl(),
                    selectedExperiments.getExperiments()
                            .get(0).pageUrl());

            checkCookie(response, selectedExperiments.getExperiments().get(0), expireDate);

            assertTrue(selectedExperiments.getIncludedExperimentIds().isEmpty());
            assertTrue(selectedExperiments.getExcludedExperimentIds().isEmpty());
        }
    }

    /**
     * Method to test: {@link ExperimentWebAPIImpl#isUserIncluded(HttpServletRequest, HttpServletResponse, List)}
     * When: You have one experiment running with 50% of traffic allocation and without targeting
     * Should: Return this experiment sometimes and sometimes return {@link ExperimentWebAPIImpl#NONE_EXPERIMENT}
     *
     * @throws DotDataException
     */
    @Test
    public void trafficLocation50() throws DotDataException, DotSecurityException {
        final Experiment experiment = new ExperimentDataGen().trafficAllocation(50).nextPersisted();

        try {
            ExperimentDataGen.start(experiment);

            final HttpServletRequest request = mock(HttpServletRequest.class);
            final List<SelectedExperiment> experimentsSelected = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                final DotCMSMockResponse response = new DotCMSMockResponse();
                final SelectedExperiments selectedExperiments = WebAPILocator.getExperimentWebAPI()
                        .isUserIncluded(request, response, null);

                assertEquals(1, selectedExperiments.getExperiments().size());
                experimentsSelected.add(selectedExperiments.getExperiments().get(0));

                checkCookie(response, selectedExperiments.getExperiments().get(0));


                assertEquals(1, selectedExperiments.getIncludedExperimentIds().size());
                assertEquals(experiment.id().get(), selectedExperiments.getIncludedExperimentIds().get(0));
                assertTrue(selectedExperiments.getExcludedExperimentIds().isEmpty());
            }

            final boolean anyNoneExperiment = experimentsSelected.stream()
                    .anyMatch(experimentSelected -> ExperimentWebAPI.NONE_EXPERIMENT.id().equals(
                            experimentSelected.id()));
            assertTrue("Expected some NONE response", anyNoneExperiment);

            final boolean anySelectedExperiment = experimentsSelected.stream()
                    .anyMatch(experimentSelected -> experiment.id().get().equals(
                            experimentSelected.id()));
            assertTrue("Expected some SELECTED response", anySelectedExperiment);
        } finally {
            ExperimentDataGen.end(experiment);
        }
    }

    /**
     * Method to test: {@link ExperimentWebAPIImpl#isUserIncluded(HttpServletRequest, HttpServletResponse, List)}
     * When: You have two experiment running with 50% of traffic allocation each one and without targeting
     * Should: Return both experiment or {@link ExperimentWebAPIImpl#NONE_EXPERIMENT} sometimes
     *
     * @throws DotDataException
     */
    @Test
    public void severalExperiment() throws DotDataException, DotSecurityException {
        final Experiment experiment_1 = new ExperimentDataGen().trafficAllocation(50).nextPersisted();
        final Experiment experiment_2 = new ExperimentDataGen().trafficAllocation(50).nextPersisted();

        try {
            ExperimentDataGen.start(experiment_1);
            ExperimentDataGen.start(experiment_2);

            final HttpServletRequest request = mock(HttpServletRequest.class);
            final List<SelectedExperiment> experimentsSelected = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                final DotCMSMockResponse response = new DotCMSMockResponse();
                final SelectedExperiments selectedExperiments = WebAPILocator.getExperimentWebAPI()
                        .isUserIncluded(request, response, null);

                assertTrue(selectedExperiments.getExperiments().size() == 2 ||
                        selectedExperiments.getExperiments().size() == 1);

                for (SelectedExperiment selectedExperiment : selectedExperiments.getExperiments()) {
                    experimentsSelected.add(selectedExperiment);
                    checkCookie(response, selectedExperiment);
                }

                assertEquals(2, selectedExperiments.getIncludedExperimentIds().size());
                assertTrue(selectedExperiments.getIncludedExperimentIds().contains(experiment_1.id().get()));
                assertTrue(selectedExperiments.getIncludedExperimentIds().contains(experiment_2.id().get()));
                assertTrue(selectedExperiments.getExcludedExperimentIds().isEmpty());
            }

            final boolean anyNoneExperiment = experimentsSelected.stream()
                    .anyMatch(experimentSelected -> ExperimentWebAPI.NONE_EXPERIMENT.name().equals(
                            experimentSelected.name()));
            assertTrue("Expected some NONE response", anyNoneExperiment);

            final boolean anySelectedExperiment_1 = experimentsSelected.stream()
                    .anyMatch(experimentSelected -> experiment_1.id().get().equals(
                            experimentSelected.id()));
            assertTrue("Expected some SELECTED response", anySelectedExperiment_1);

            final boolean anySelectedExperiment_2 = experimentsSelected.stream()
                    .anyMatch(experimentSelected -> experiment_2.id().get().equals(
                            experimentSelected.id()));
            assertTrue("Expected some SELECTED response", anySelectedExperiment_2);
        } finally {
            ExperimentDataGen.end(experiment_1);
            ExperimentDataGen.end(experiment_2);
        }
    }

    private HTMLPageAsset getExperimentPage(Experiment experiment_1) throws DotDataException {
        final Contentlet contentlet = APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguage(experiment_1.pageId(), false);
        return APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
    }

    /**
     * Method to test: {@link ExperimentWebAPIImpl#isUserIncluded(HttpServletRequest, HttpServletResponse, List)}
     * When: You have one experiment with a rules but the rules conditions is not TRUE
     * Should: Return {@link ExperimentWebAPIImpl#NONE_EXPERIMENT} all the times
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void experimentWithFalseRules() throws DotDataException, DotSecurityException {
        final TargetingCondition targetingCondition = TargetingCondition.builder()
                .conditionKey("RequestAttributeConditionlet")
                .values((Map<String, String>) map("comparison", "is", "request-attribute",
                        "testing-attribute", "request-attribute-value", "testing"))
                .operator(LogicalOperator.AND)
                .build();

        final Experiment experiment = new ExperimentDataGen()
                .trafficAllocation(100)
                .addTargetingConditions(targetingCondition)
                .nextPersisted();

        try{
            ExperimentDataGen.start(experiment);

            final HttpServletRequest request = mock(HttpServletRequest.class);

            for (int i = 0; i < 10; i++) {
                final DotCMSMockResponse response = new DotCMSMockResponse();
                final SelectedExperiments selectedExperiments = WebAPILocator.getExperimentWebAPI()
                        .isUserIncluded(request, response, null);

                assertEquals(1, selectedExperiments.getExperiments().size());
                assertEquals(ExperimentWebAPI.NONE_EXPERIMENT.id(), selectedExperiments.getExperiments().get(0).id());
                assertEquals(ExperimentWebAPI.NONE_EXPERIMENT.pageUrl(), selectedExperiments.getExperiments().get(0).pageUrl());

                checkCookie(response, selectedExperiments.getExperiments().get(0));

                assertEquals(1, selectedExperiments.getIncludedExperimentIds().size());
                assertTrue(selectedExperiments.getIncludedExperimentIds().contains(experiment.id().get()));
                assertTrue(selectedExperiments.getExcludedExperimentIds().isEmpty());
            }
        } finally {
            ExperimentDataGen.end(experiment);
        }
    }

    /**
     * Method to test: {@link ExperimentWebAPIImpl#isUserIncluded(HttpServletRequest, HttpServletResponse, List)}
     * When: You have one experiment with a rules but the rules conditions is TRUE
     * Should: Return Experiment as selected all the time
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void experimentWithTrueRules() throws DotDataException, DotSecurityException {
        final TargetingCondition targetingCondition = TargetingCondition.builder()
                .conditionKey("RequestAttributeConditionlet")
                .values((Map<String, String>) map("comparison", "is", "request-attribute",
                        "testing-attribute", "request-attribute-value", "testing"))
                .operator(LogicalOperator.AND)
                .build();

        final Experiment experiment = new ExperimentDataGen()
                .trafficAllocation(100)
                .addTargetingConditions(targetingCondition)
                .nextPersisted();

        final HTMLPageAsset htmlPageAsset = getExperimentPage(experiment);

        try{
            ExperimentDataGen.start(experiment);

            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute("testing-attribute")).thenReturn("testing");

            for (int i = 0; i < 10; i++) {
                final DotCMSMockResponse response = new DotCMSMockResponse();
                final SelectedExperiments selectedExperiments = WebAPILocator.getExperimentWebAPI()
                        .isUserIncluded(request, response, null);

                assertEquals(1, selectedExperiments.getExperiments().size());
                assertEquals(experiment.id().get(), selectedExperiments.getExperiments().get(0).id());
                assertEquals(htmlPageAsset.getURI(), selectedExperiments.getExperiments().get(0).pageUrl());

                checkCookie(response, selectedExperiments.getExperiments().get(0));

                assertEquals(1, selectedExperiments.getIncludedExperimentIds().size());
                assertTrue(selectedExperiments.getIncludedExperimentIds().contains(experiment.id().get()));
                assertTrue(selectedExperiments.getExcludedExperimentIds().isEmpty());
            }
        } finally {
            ExperimentDataGen.end(experiment);
        }
    }

    /**
     * Method to test: {@link ExperimentWebAPIImpl#isUserIncluded(HttpServletRequest, HttpServletResponse, List)}
     * When: You have 2 experiments with a rules and the rules conditions is TRUE for both of them
     * Should: Return {@link ExperimentWebAPIImpl#NONE_EXPERIMENT} all the times
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void severalExperimenstWithTrueRules() throws DotDataException, DotSecurityException {
        final TargetingCondition targetingCondition = TargetingCondition.builder()
                .conditionKey("RequestAttributeConditionlet")
                .values((Map<String, String>) map("comparison", "is", "request-attribute",
                        "testing-attribute", "request-attribute-value", "testing"))
                .operator(LogicalOperator.AND)
                .build();

        final Experiment experiment_1 = new ExperimentDataGen()
                .trafficAllocation(100)
                .addTargetingConditions(targetingCondition)
                .nextPersisted();


        final Experiment experiment_2 = new ExperimentDataGen()
                .trafficAllocation(100)
                .addTargetingConditions(targetingCondition)
                .nextPersisted();

        final HTMLPageAsset htmlPageAsset_1 = getExperimentPage(experiment_1);
        final HTMLPageAsset htmlPageAsset_2 = getExperimentPage(experiment_2);

        try{
            ExperimentDataGen.start(experiment_1);
            ExperimentDataGen.start(experiment_2);

            final HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getAttribute("testing-attribute")).thenReturn("testing");

            for (int i = 0; i < 10; i++) {
                final DotCMSMockResponse response = new DotCMSMockResponse();
                final SelectedExperiments selectedExperiments = WebAPILocator.getExperimentWebAPI()
                        .isUserIncluded(request, response, null);

                assertEquals(selectedExperiments.getExperiments().size(), 2);

                for (final SelectedExperiment selectedExperiment : selectedExperiments.getExperiments()) {

                    if (selectedExperiment.id().equals(experiment_1.id())) {
                        assertEquals(htmlPageAsset_1.getPageUrl(), selectedExperiment.pageUrl());
                        checkCookie(response, selectedExperiment, experiment_1.scheduling().get().endDate().get());
                    } else if (selectedExperiment.id().equals(experiment_2.id())) {
                        assertEquals(htmlPageAsset_2.getPageUrl(), selectedExperiment.pageUrl());
                        checkCookie(response, selectedExperiment, experiment_2.scheduling().get().endDate().get());
                    }
                }

                assertEquals(2, selectedExperiments.getIncludedExperimentIds().size());
                assertTrue(selectedExperiments.getIncludedExperimentIds().contains(experiment_1.id().get()));
                assertTrue(selectedExperiments.getIncludedExperimentIds().contains(experiment_2.id().get()));
                assertTrue(selectedExperiments.getExcludedExperimentIds().isEmpty());
            }
        } finally {
            ExperimentDataGen.end(experiment_1);
            ExperimentDataGen.end(experiment_2);
        }
    }

    /**
     * Method to test: {@link ExperimentWebAPIImpl#isUserIncluded(HttpServletRequest, HttpServletResponse, List)}
     * When: You have one experiment running with 100% of traffic allocation and without targeting
     * and two variants each one with 50% of traffic location
     * Should: Return this experiment all the time and some time the first variant and sometimes the second one
     *
     * @throws DotDataException
     */
    @Test
    public void isUserIncludedWithVariants() throws DotDataException, DotSecurityException {

        final Experiment experiment = new ExperimentDataGen()
                .trafficAllocation(100)
                .addVariant("variant_1")
                .addVariant("variant_2")
                .nextPersisted();

        try {
            ExperimentDataGen.start(experiment);

            final HTMLPageAsset htmlPageAsset = getExperimentPage(experiment);

            final HttpServletRequest request = mock(HttpServletRequest.class);

            final List<SelectedExperiment> experiments = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                final DotCMSMockResponse response = new DotCMSMockResponse();
                final SelectedExperiments selectedExperiments = WebAPILocator.getExperimentWebAPI()
                        .isUserIncluded(request, response, null);

                assertEquals(1, selectedExperiments.getExperiments().size());
                assertEquals(experiment.id().get(), selectedExperiments.getExperiments().get(0).id());
                assertEquals(htmlPageAsset.getURI(), selectedExperiments.getExperiments().get(0).pageUrl());

                experiments.add(selectedExperiments.getExperiments().get(0));

                checkCookie(response, selectedExperiments.getExperiments().get(0));

                assertEquals(1, selectedExperiments.getIncludedExperimentIds().size());
                assertTrue(selectedExperiments.getIncludedExperimentIds().contains(experiment.id().get()));
                assertTrue(selectedExperiments.getExcludedExperimentIds().isEmpty());
            }

            final TrafficProportion trafficProportion = experiment.trafficProportion();

            for (ExperimentVariant variant : trafficProportion.variants()) {
                final boolean defaultIsThre = experiments.stream().anyMatch(
                        experimentSelected -> variant.id().equals(experimentSelected.variant().name()));

                assertTrue("Should the variant " + variant.id() + " been returned sometimes", defaultIsThre);
            }
        } finally {
            ExperimentDataGen.end(experiment);
        }
    }

    /**
     * Method to test: {@link ExperimentWebAPIImpl#isUserIncluded(HttpServletRequest, HttpServletResponse, List)}
     * When:
     * - You have one experiment running with 100% of traffic allocation and without targeting and called the method
     * it should return this experiment, and also include it into the included list, the excluded list should be empty.
     * - Start a new experiment with 100% of traffic allocation and without targeting and called the method
     * with the first experiment in the excluded list.
     * it should return this new experiment, and also include it into the included list, the excluded list should contain the first experiment.
     *
     * @throws DotDataException
     */
    @Test
    public void excludeExperiments() throws DotDataException, DotSecurityException {
        final Experiment experiment_1 = new ExperimentDataGen().trafficAllocation(100).nextPersisted();
        final Experiment experiment_2 = new ExperimentDataGen().trafficAllocation(100).nextPersisted();

        try {
            ExperimentDataGen.start(experiment_1);

            final HttpServletRequest request = mock(HttpServletRequest.class);

            final DotCMSMockResponse response = new DotCMSMockResponse();

            final SelectedExperiments selectedExperiments_1 = WebAPILocator.getExperimentWebAPI()
                    .isUserIncluded(request, response, null);

            assertEquals(1, selectedExperiments_1.getIncludedExperimentIds().size());
            assertEquals(experiment_1.id().get(), selectedExperiments_1.getIncludedExperimentIds().get(0));

            assertTrue(selectedExperiments_1.getExcludedExperimentIds().isEmpty());

            ExperimentDataGen.start(experiment_2);

            final SelectedExperiments selectedExperiments_2 = WebAPILocator.getExperimentWebAPI()
                    .isUserIncluded(request, response, list(experiment_1.id().get()));

            assertEquals(1, selectedExperiments_2.getIncludedExperimentIds().size());
            assertEquals(experiment_2.id().get(), selectedExperiments_2.getIncludedExperimentIds().get(0));

            assertEquals(1, selectedExperiments_2.getExcludedExperimentIds().size());
            assertEquals(experiment_1.id().get(), selectedExperiments_2.getExcludedExperimentIds().get(0));
        } finally {
            ExperimentDataGen.end(experiment_1);

            final Optional<Experiment> experiment = APILocator.getExperimentsAPI()
                    .find(experiment_2.id().get(), APILocator.systemUser());

            if (experiment.isPresent() && experiment.get().status() == Status.RUNNING) {
                ExperimentDataGen.end(experiment_2);
            }
        }
    }
}
