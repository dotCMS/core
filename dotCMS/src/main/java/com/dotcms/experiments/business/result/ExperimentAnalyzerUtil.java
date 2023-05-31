package com.dotcms.experiments.business.result;


import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.metrics.MetricType;

import com.dotcms.cube.CubeJSClient;
import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.CubeJSQuery.Builder;
import com.dotcms.cube.CubeJSResultSet;
import com.dotcms.cube.filters.SimpleFilter.Operator;

import com.dotcms.experiments.business.ExperimentUrlPatternCalculator;
import com.dotcms.experiments.model.Goal;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.Goal.GoalType;
import com.dotcms.experiments.model.Goals;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.liferay.util.StringPool;
import graphql.VisibleForTesting;
import io.vavr.Lazy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static com.dotcms.util.CollectionsUtils.map;


/**
 * Analyze the {@link Event} into an {@link Experiment} to realize when the {@link com.dotcms.experiments.model.Goals}
 * was success into the {@link Experiment}, to put it in another way analyze a set of  {@link BrowserSession}
 * to return the {@link ExperimentResults}.
 *
 */
public enum ExperimentAnalyzerUtil {

    INSTANCE;

    private static AnalyticsHelper analyticsHelper = AnalyticsHelper.get();

    final static Lazy<Map<MetricType, MetricExperimentAnalyzer>> experimentResultQueryHelpers =
            Lazy.of(ExperimentAnalyzerUtil::createHelpersMap);

    private static Map<MetricType, MetricExperimentAnalyzer> createHelpersMap() {
        return map(
                MetricType.REACH_PAGE, new ReachPageExperimentAnalyzer(),
                MetricType.BOUNCE_RATE, new BounceRateExperimentAnalyzer()
        );
    }

    @VisibleForTesting
    public static void setAnalyticsHelper(final AnalyticsHelper analyticsHelper) {
        ExperimentAnalyzerUtil.analyticsHelper = analyticsHelper;
    }

    /**
     * Return the {@link ExperimentResults} from a set of {@link BrowserSession} and an {@link Experiment}
     *
     * @param experiment
     * @param browserSessions
     * @return
     */
    public ExperimentResults getExperimentResult(final Experiment experiment,
                                                 final List<BrowserSession> browserSessions)
            throws DotDataException, DotSecurityException {

        final Goals goals = experiment.goals()
                .orElseThrow(() -> new IllegalArgumentException("The Experiment must have a Goal"));

        final Goal primaryGoal = goals.primary();
        final SortedSet<ExperimentVariant> variants = experiment.trafficProportion().variants();

        final  ExperimentResults.Builder builder = new ExperimentResults.Builder(variants);
        builder.addPrimaryGoal(primaryGoal);
        builder.trafficProportion(experiment.trafficProportion());

        if (!browserSessions.isEmpty()) {
            final CubeJSResultSet pageViewsByVariants = getPageViewsByVariants(experiment,
                    variants);
            pageViewsByVariants.forEach(row -> {
                final String variantId = row.get("Events.variant")
                        .map(variant -> variant.toString())
                        .orElse(StringPool.BLANK);
                final long pageViews = row.get("Events.count")
                        .map(object -> Long.parseLong(object.toString()))
                        .orElse(0L);

                builder.goal(primaryGoal).variant(variantId).pageView(pageViews);
            });

            analyzeBrowserSessions(browserSessions, primaryGoal, builder, experiment);
        }

        return builder.build();
    }

    private void analyzeBrowserSessions(final List<BrowserSession> browserSessions,
            final Goal goal,
            final ExperimentResults.Builder builder,
            final Experiment experiment) {

        final String pageId = experiment.pageId();
        final HTMLPageAsset page = getPage(pageId);

        final MetricType goalMetricType = goal.getMetric().type();

        final MetricExperimentAnalyzer metricExperimentAnalyzer = experimentResultQueryHelpers.get()
                .get(goalMetricType);

        final String urlRegexPattern = ExperimentUrlPatternCalculator.INSTANCE
                .calculateUrlRegexPattern(page);

        for (final BrowserSession browserSession : browserSessions) {

            final boolean isIntoExperiment = browserSession.getEvents().stream()
                    .map(event -> event.get("url").map(Object::toString).orElse(StringPool.BLANK))
                    .anyMatch(url -> url.matches(urlRegexPattern));

            if (isIntoExperiment) {
                builder.addSession(browserSession);
                final Collection<Event> occurrences = metricExperimentAnalyzer.getOccurrences(
                        goal.getMetric(), browserSession);

                final String lookBackWindow = browserSession.getLookBackWindow();

                if (goal.type() == GoalType.MINIMIZE && occurrences.isEmpty()) {
                    final Event event = browserSession.getEvents().get(0);
                    builder.goal(goal).success(lookBackWindow, event);
                } if (goal.type() == GoalType.MAXIMIZE && !occurrences.isEmpty()) {
                    occurrences.stream().forEach(event -> builder.goal(goal)
                            .success(lookBackWindow, event));
                }

            }
        }
    }

    private static CubeJSResultSet getPageViewsByVariants(
            final Experiment experiment, final SortedSet<ExperimentVariant> variants)
            throws DotDataException, DotSecurityException {

        final CubeJSQuery cubeJSQuery = new Builder()
                .dimensions("Events.variant")
                .measures("Events.count")
                .filter("Events.eventType", Operator.EQUALS, EventType.PAGE_VIEW.getName())
                .filter("Events.variant", Operator.EQUALS, variants.stream().map(ExperimentVariant::id).toArray())
                .filter("Events.experiment", Operator.EQUALS, experiment.getIdentifier())
                .build();

        final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHost();
        final AnalyticsApp analyticsApp = analyticsHelper.appFromHost(currentHost);

        final CubeJSClient cubeClient = new CubeJSClient(
                analyticsApp.getAnalyticsProperties().analyticsReadUrl());

        return cubeClient.send(cubeJSQuery);
    }

    private HTMLPageAsset getPage(final String pageId) {

        try {
            return APILocator.getHTMLPageAssetAPI().fromContentlet(
                    APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(pageId)
            );
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

}
