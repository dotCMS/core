package com.dotcms.experiments.business.result;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;
import com.dotcms.cube.CubeJSResultSet;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.Goals;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * Analyze the {@link Event} into an {@link Experiment} to realize when the {@link com.dotcms.experiments.model.Goals}
 * was success into the {@link Experiment}, to put it in another way analyze a set of  {@link BrowserSession}
 * to retur the {@link ExperimentResult}.
 *
 */
public enum ExperimentAnalyzerUtil {

    INSTANCE;

    final static Lazy<Map<MetricType, MetricExperimentAnalyzer>> experimentResultQueryHelpers =
            Lazy.of(() -> createHelpersMap());


    private static Map<MetricType, MetricExperimentAnalyzer> createHelpersMap() {
        return map(
                MetricType.REACH_PAGE, new ReachPageExperimentAnalyzer()
        );
    }

    /**
     * Return the {@link ExperimentResult} from a set of {@link BrowserSession} and an {@link Experiment}
     *
     * @param experiment
     * @param browserSessions
     * @return
     */
    public ExperimentResult getExperimentResult(final Experiment experiment,
            final List<BrowserSession> browserSessions)  {
        final Goals goals = experiment.goals()
                .orElseThrow(() -> new IllegalArgumentException("The Experiment must have a Goal"));

        final MetricType goalMetricType = goals.primary().type();
        final MetricExperimentAnalyzer metricExperimentAnalyzer = experimentResultQueryHelpers.get()
                .get(goalMetricType);

        final  ExperimentResult.Builder builder = new ExperimentResult.Builder();

        final SortedSet<ExperimentVariant> variants = experiment.trafficProportion().variants();
        builder.addVariants(variants);

        final Metric goal = goals.primary();
        builder.addGoal(goal);

        final String pageId = experiment.pageId();
        final HTMLPageAsset page = getPage(pageId);

        final List<BrowserSession> experimentSessions = new ArrayList<>();

        for (final BrowserSession browserSession : browserSessions) {

            final boolean isIntoExperiment = browserSession.getEvents().stream()
                    .map(event -> event.get("url").map(url -> url.toString()).orElse(StringPool.BLANK))
                    .anyMatch(url -> url.contains(page.getPageUrl()));

            if (isIntoExperiment) {
                experimentSessions.add(browserSession);
                metricExperimentAnalyzer.addResults(goal, browserSession, builder);
            }
        }

        builder.setSessionTotal(experimentSessions.size());
        builder.setTotalEvents(experimentSessions.stream()
                .map(browserSession -> browserSession.getEvents())
                .map(events -> events.size())
                .collect(Collectors.summingInt(Integer::intValue))
        );

        return builder.build();
    }

    private static HTMLPageAsset getPage(String pageId) {
        try {
            return APILocator.getHTMLPageAssetAPI().fromContentlet(
                    APILocator.getContentletAPI().findContentletByIdentifierAnyLanguage(pageId)
            );
        } catch (DotDataException e) {
            throw new RuntimeException(e);
        }
    }

}
