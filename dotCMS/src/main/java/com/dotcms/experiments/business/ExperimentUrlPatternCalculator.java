package com.dotcms.experiments.business;


import static com.dotcms.experiments.business.RegexUrlPatterStrategy.*;
import static com.dotcms.util.CollectionsUtils.list;


import com.dotcms.analytics.metrics.*;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.vanityurl.model.CachedVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.util.StringPool;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
     * Calculate the URL regex pattern for the given {@link Experiment},
     * This pattern is used to match the Event's url page with the {@link Experiment}'s {@link HTMLPageAsset} url.
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
     * @param experiment
     * @return
     */
    public String calculatePageUrlRegexPattern(final Experiment experiment) {
        final HTMLPageAsset htmlPageAsset = getHtmlPageAsset(experiment);

        try {
            final Host host = APILocator.getHostAPI().find(htmlPageAsset.getHost(), APILocator.systemUser(), false);
            final Language language = APILocator.getLanguageAPI().getLanguage(htmlPageAsset.getLanguageId());

            final String vanityUrlRegex = getVanityUrlsRegex(host, language, htmlPageAsset);

            final String experimentPageRegex = URL_REGEX_PATTERN_STRATEGIES.stream()
                    .filter(strategy -> strategy.isMatch(htmlPageAsset))
                    .findFirst()
                    .orElseGet(() -> DEFAULT_REGEX_URL_PATTERN_STRATEGY)
                    .getRegexPattern(htmlPageAsset).toLowerCase();

            return vanityUrlRegex.isEmpty() ?  experimentPageRegex :
            String.format("(%s|%s)", experimentPageRegex , vanityUrlRegex);
        } catch (final RegexUrlPatterStrategyException | DotDataException | DotSecurityException e) {
            throw new RuntimeException(String.format("It is not possible to get the URI for %s",
                    htmlPageAsset.getInode()), e);
        }
    }

    private static String getVanityUrlsRegex(final Host host, final Language language,
                                             final HTMLPageAsset htmlPageAsset) throws DotDataException {

        final String vanityUrlRegex = APILocator.getVanityUrlAPI()
                .findByForward(host, language, htmlPageAsset.getURI(), 200)
                .stream()
                .map(vanitysUrls -> String.format(DEFAULT_URL_REGEX_TEMPLATE, vanitysUrls.pattern))
                .collect(Collectors.joining(StringPool.PIPE));
        return vanityUrlRegex.isEmpty() ? StringPool.BLANK : String.format("^%s$", vanityUrlRegex);
    }

    private HTMLPageAsset getHtmlPageAsset(final Experiment experiment) {

        try {
            final Contentlet contentlet = APILocator.getContentletAPI()
                    .findContentletByIdentifierAnyLanguage(experiment.pageId(), false);
            final HTMLPageAsset htmlPageAsset = APILocator.getHTMLPageAssetAPI()
                    .fromContentlet(contentlet);
            return htmlPageAsset;
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
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
