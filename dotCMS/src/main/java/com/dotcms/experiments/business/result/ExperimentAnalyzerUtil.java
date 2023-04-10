package com.dotcms.experiments.business.result;

import static com.dotcms.util.CollectionsUtils.map;

import com.dotcms.analytics.metrics.Metric;
import com.dotcms.analytics.metrics.MetricType;

import com.dotcms.experiments.model.Experiment;
import com.dotcms.experiments.model.ExperimentVariant;
import com.dotcms.experiments.model.Goals;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.liferay.util.StringPool;
import io.vavr.Lazy;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;


/**
 * Analyze the {@link Event} into an {@link Experiment} to realize when the {@link com.dotcms.experiments.model.Goals}
 * was success into the {@link Experiment}, to put it in another way analyze a set of  {@link BrowserSession}
 * to return the {@link ExperimentResults}.
 *
 */
public enum ExperimentAnalyzerUtil {

    INSTANCE;

    final static Lazy<Map<MetricType, MetricExperimentAnalyzer>> experimentResultQueryHelpers =
            Lazy.of(() -> createHelpersMap());


    private static Map<MetricType, MetricExperimentAnalyzer> createHelpersMap() {
        return map(
                MetricType.REACH_PAGE, new ReachPageExperimentAnalyzer(),
                MetricType.BOUNCE_RATE, new BounceRateExperimentAnalyzer()
        );
    }

    /**
     * Return the {@link ExperimentResults} from a set of {@link BrowserSession} and an {@link Experiment}
     *
     * @param experiment
     * @param browserSessions
     * @return
     */
    public ExperimentResults getExperimentResult(final Experiment experiment,
                                                 final List<BrowserSession> browserSessions)  {
        final Goals goals = experiment.goals()
                .orElseThrow(() -> new IllegalArgumentException("The Experiment must have a Goal"));

        final MetricType goalMetricType = goals.primary().type();
        final MetricExperimentAnalyzer metricExperimentAnalyzer = experimentResultQueryHelpers.get()
                .get(goalMetricType);

        final SortedSet<ExperimentVariant> variants = experiment.trafficProportion().variants();
        final  ExperimentResults.Builder builder = new ExperimentResults.Builder(variants);

        final Metric goal = goals.primary();
        builder.addPrimaryGoal(goal);

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
