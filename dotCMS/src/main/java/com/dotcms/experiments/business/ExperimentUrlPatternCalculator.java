package com.dotcms.experiments.business;


import static com.dotcms.experiments.business.RegexUrlPatterStrategy.*;
import static com.dotcms.util.CollectionsUtils.list;


import com.dotcms.analytics.metrics.*;
import com.dotcms.experiments.model.Experiment;
import com.dotcms.vanityurl.business.VanityUrlAPI;
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
import java.util.stream.Stream;

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
     * <p><b>Security note:</b> The returned pattern is serialized to the
     * Experiments Analytics SDK and evaluated client-side via
     * {@code new RegExp(...).test(...)}. Because Vanity URL URIs can contain
     * admin-authored regex, the assembled pattern is NOT protected by
     * {@link com.dotcms.regex.MatcherTimeoutFactory} (which only guards the
     * server-side Vanity URL resolver). Client-side ReDoS protection is tracked
     * as a follow-up in <a href="https://github.com/dotCMS/core/issues/35379">#35379</a>.
     *
     * <p><b>Case folding:</b> The returned pattern is emitted entirely in
     * lowercase — both the experiment-page alternative and every Vanity URL
     * alternative — to match the SDK tracker, which lowercases the incoming
     * URL path before calling {@code test}. As a side effect, any admin-authored
     * vanity URI that relies on uppercase characters or uppercase-only
     * character classes (e.g. {@code [A-Z]+}) is folded to lowercase in this
     * path; such patterns are unsupported here. This is consistent with the
     * server-side resolver, which already compiles vanity patterns with
     * {@link java.util.regex.Pattern#CASE_INSENSITIVE} so case-sensitive regex
     * constructs do not influence vanity matching in any consumer.
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

        // includeSystemHost=true: a /cmsHomePage vanity forwarding to the
        // experiment page may be published on SYSTEM_HOST (site-wide), so we
        // need those matches as well. Mirrors resolveVanityUrl's host fallback.
        final List<CachedVanityUrl> vanityUrls = APILocator.getVanityUrlAPI()
                .findByForward(host, language, htmlPageAsset.getURI(), 200, true);

        // Exact match is intentional — regex-based cmsHomePage URIs (e.g. "/cmsHome.*")
        // are unsupported here. VanityUrlAPIImpl.resolveVanityUrl's legacy fallback
        // looks up the literal LEGACY_CMS_HOME_PAGE string, so only vanities whose
        // URI equals it (case-insensitive) actually participate in the "/" fallback.
        final boolean hasCmsHomePageVanity = vanityUrls.stream()
                .anyMatch(vanity -> VanityUrlAPI.LEGACY_CMS_HOME_PAGE.equalsIgnoreCase(vanity.url));

        // When a /cmsHomePage vanity forwards to the experiment page, visitors
        // reach it at "/" (see VanityUrlAPIImpl.resolveVanityUrl legacy fallback)
        // — add "/" as an extra alternative so the regex still matches.
        final String vanityUrlRegex = Stream.concat(
                vanityUrls.stream()
                        // Skip vanities whose URI failed CachedVanityUrl.normalize
                        // (VanityUrlUtil.isValidRegex returned false) — their
                        // compiled Pattern's source is "", which would otherwise
                        // expand the URL template into a catch-all.
                        .filter(vanity -> !vanity.pattern.pattern().isEmpty())
                        .map(vanity -> String.format(DEFAULT_URL_REGEX_TEMPLATE, vanity.pattern.pattern())),
                hasCmsHomePageVanity
                        ? Stream.of(String.format(DEFAULT_URL_REGEX_TEMPLATE, "\\/?"))
                        : Stream.empty()
        ).collect(Collectors.joining(StringPool.PIPE));

        // Lowercase the ENTIRE assembled vanity regex — this affects every
        // vanity pattern joined above, not just the /cmsHomePage fallback. The
        // SDK (parser.ts#verifyRegex) lowercases the incoming URL path before
        // calling RegExp.test, so a mixed-case vanity URI stored by the admin
        // would otherwise never match. Consequence: any admin-authored regex
        // construct that depends on uppercase characters (e.g. "[A-Z]+") is
        // folded to lowercase here and is unsupported in this path. This is
        // consistent with CachedVanityUrl, which compiles each vanity's URI
        // pattern with Pattern.CASE_INSENSITIVE — server-side vanity matching
        // is already case-insensitive, so no consumer loses functionality.
        return vanityUrlRegex.isEmpty() ? StringPool.BLANK : String.format("^%s$", vanityUrlRegex).toLowerCase();
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
