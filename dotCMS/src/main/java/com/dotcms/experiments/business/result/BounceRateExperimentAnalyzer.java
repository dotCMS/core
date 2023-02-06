package com.dotcms.experiments.business.result;

import com.dotcms.analytics.metrics.Condition;
import com.dotcms.analytics.metrics.EventType;
import com.dotcms.analytics.metrics.Metric;
import com.dotcms.experiments.business.result.ExperimentResult.Builder;
import com.google.common.collect.ImmutableList;
import com.liferay.util.StringPool;
import java.util.List;
import java.util.stream.Collectors;

public class BounceRateExperimentAnalyzer implements MetricExperimentAnalyzer  {

    @Override
    public void addResults(final Metric goal, final BrowserSession browserSession,
            final Builder experimentResultBuilder) {
        final List<Event> events = browserSession.getEvents().stream()
                .filter(event -> event.getType() == EventType.PAGE_VIEW)
                .collect(Collectors.toList());

        final Event lastEvent = events.get(events.size() - 1);

        if (goal.validateConditions(lastEvent)) {
            experimentResultBuilder.count(goal, browserSession.getLookBackWindow(), lastEvent);
        }
    }


}
