package com.dotcms.experiments.business;


import static com.dotcms.util.CollectionsUtils.list;


import com.dotcms.analytics.metrics.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;

import java.util.List;
import java.util.Optional;

/**
 * Util class to calculate the regex pattern for a given {@link HTMLPageAsset}
 */
public enum ExperimentUrlPatternCalculator {
    INSTANCE;

    private final RegexUrlPatterStrategy DEFAULT_REGEX_URL_PATTERN_STRATEGY = new DefaultRegexUrlPatterStrategy();
    private List<RegexUrlPatterStrategy> URL_REGEX_PATTERN_STRATEGIES = list(
            new UrlMapRegexUrlPatterStrategy(),
            new RootIndexRegexUrlPatterStrategy(),
            new IndexRegexUrlPatterStrategy()
    );

    /**
     * Calculate the URL regex pattern for the given {@link HTMLPageAsset},
     * This pattern is used to match the Event's url page with the {@link HTMLPageAsset} url.
     *
     * For example if you create a Experiment using the "/blog/index" page, any of
     * the following urls will be matched:
     *
     * - http://localhost:8080/blog/index
     * - http://localhost:8080/blog/index?param1=value1&param2=value2
     * - http://localhost:8080/blog/
     * - http://localhost:8080/blog
     * - http://localhost:8080/blog?param1=value1&param2=value2
     *
     * If the page use inside the Experiment isuse as Detail Page on any Content Type then is even
     * more complicated.
     *
     * @param htmlPageAsset
     * @return
     */
    public String calculatePageUrlRegexPattern(final HTMLPageAsset htmlPageAsset) {
        try {
            return URL_REGEX_PATTERN_STRATEGIES.stream()
                .filter(strategy -> strategy.isMatch(htmlPageAsset))
                .findFirst()
                .orElseGet(()-> DEFAULT_REGEX_URL_PATTERN_STRATEGY)
                    .getRegexPattern(htmlPageAsset).toLowerCase();

        } catch (final RegexUrlPatterStrategyException e) {
            throw new RuntimeException(String.format("It is not possible to get the URI for %s",
                    htmlPageAsset.getInode()), e);
        }
    }

    public Optional<String> calculateTargetPageUrlPattern(final HTMLPageAsset htmlPageAsset, final Metric metric) {
        final MetricType type = metric.type();
        Optional<String> regexParameterName = type.getRegexParameterName();

        if (!regexParameterName.isPresent()) {
            return Optional.empty();
        }

        final Condition regexCondition = metric.conditions().stream()
                .filter(condition -> condition.parameter().equals(regexParameterName.get()))
                .findFirst()
                .orElseThrow();

        final AbstractCondition.Operator operator = regexCondition.operator();
        final String regexOperator = operator.regex();

        final Parameter parameter = metric.type().getParameter(regexCondition.parameter()).orElseThrow();
        final String regex = parameter.type().regex(regexCondition.value(), regexOperator);
        return Optional.of(regex);
    }


    private static String getUriForRegex(final String uri) throws DotDataException {
        return uri.replaceAll("/", "\\\\/");
    }

}
