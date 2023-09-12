package com.dotcms.analytics.metrics;

import com.dotcms.experiments.model.Goals;
import com.dotmarketing.util.UtilMethods;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Util class for operations on Metrics. This will probably be moved to a MetricsAPI when created.
 */
public enum MetricsUtil {
    INSTANCE;

    public void validateGoals(Goals goals) {
        final Metric primaryGoal = goals.primary().getMetric();

        final Set<String> availableParams = primaryGoal.type()
                .availableParameters().stream().map(Parameter::name).collect(Collectors.toSet());

        final Set<String> providedParams = primaryGoal.conditions()
                .stream().map(Condition::parameter).collect(Collectors.toSet());


        if(UtilMethods.isSet(availableParams) && !availableParams.containsAll(providedParams)) {
            providedParams.removeAll(availableParams);
            throw new IllegalArgumentException("Invalid Parameters provided: " +
                    providedParams);
        }

        final Set<String> requiredParams = primaryGoal.type()
                .getAllRequiredParameters().stream().map(Parameter::name).collect(Collectors.toSet());

        if(UtilMethods.isSet(requiredParams) && !providedParams.containsAll(requiredParams)) {
            requiredParams.removeAll(providedParams);
            throw new IllegalArgumentException("Missing required Parameters: " +
                    requiredParams);
        }

        final Set<String> atLeastOneRequired = primaryGoal.type()
                .getAnyRequiredParameters().stream().map(Parameter::name).collect(Collectors.toSet());

        if(UtilMethods.isSet(atLeastOneRequired)
                && providedParams.stream().noneMatch(atLeastOneRequired::contains)) {
            throw new IllegalArgumentException("At least one of these are required Parameters: " +
                    atLeastOneRequired);
        }
    }
}
