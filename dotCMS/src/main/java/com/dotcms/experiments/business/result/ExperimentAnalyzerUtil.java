package com.dotcms.experiments.business.result;


import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.analytics.app.AnalyticsApp;
import com.dotcms.analytics.helper.AnalyticsHelper;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;

import com.dotcms.cube.CubeJSClient;
import com.dotcms.cube.CubeJSQuery;
import com.dotcms.cube.CubeJSQuery.Builder;
import com.dotcms.cube.CubeJSResultSet;
import com.dotcms.cube.filters.SimpleFilter.Operator;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
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

        final MetricType goalMetricType = goals.primary().type();
        final MetricExperimentAnalyzer metricExperimentAnalyzer = experimentResultQueryHelpers.get()
                .get(goalMetricType);

        final SortedSet<ExperimentVariant> variants = experiment.trafficProportion().variants();

        final CubeJSResultSet pageViewsByVariants = getPageViewsByVariants(experiment, variants);

        final  ExperimentResults.Builder builder = new ExperimentResults.Builder(variants);

        final Metric goal = goals.primary();
        builder.addPrimaryGoal(goal);

        pageViewsByVariants.forEach(row -> {
            final String variantId = row.get("Events.variant").map(variant -> variant.toString())
                    .orElse(StringPool.BLANK);
            final long pageViews = row.get("Events.count").map(object -> Long.parseLong(object.toString()))
                    .orElse(0L);

            builder.goal(goal).variant(variantId).pageView(pageViews);
        });

        builder.trafficProportion(experiment.trafficProportion());

        final String pageId = experiment.pageId();
        final HTMLPageAsset page = getPage(pageId);

        for (final BrowserSession browserSession : browserSessions) {

            final boolean isIntoExperiment = browserSession.getEvents().stream()
                    .map(event -> event.get("url").map(Object::toString).orElse(StringPool.BLANK))
                    .anyMatch(url -> {
                        try {
                            final String uri = page.getURI();
                            final String alternativeURI = uri.endsWith("index")
                                ? uri.substring(0, uri.indexOf("index"))
                                : uri;
                            return url.contains(uri) || url.contains(alternativeURI);
                        } catch (DotDataException e) {
                            throw new RuntimeException(e);
                        }
                    });

            if (isIntoExperiment) {
                builder.addSession(browserSession);
                metricExperimentAnalyzer.addResults(goal, browserSession, builder);
            }
        }

        return builder.build();
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
