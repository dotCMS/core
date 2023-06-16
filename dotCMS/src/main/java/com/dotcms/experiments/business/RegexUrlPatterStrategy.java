package com.dotcms.experiments.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;

/**
 * Inside a Experiment we need to compare if a User a visited a specific page inside a
 * {@link com.dotcms.experiments.business.result.BrowserSession}, we need do this for:
 *
 * - Know if we need to redirect a page to be render in a specific {@link com.dotcms.variant.model.Variant}
 * when the {@link com.dotcms.experiments.model.Experiment} is running.
 * - Know if a Experiment's page was visited during the {@link com.dotcms.experiments.business.result.BrowserSession}
 * - In some goal we need to know if a specific page was visited or when it was visited like REACH_PAGE or BOUNCE_RATE.
 *
 * This is a little more complicated that it look at first glance because we need to take account different thing:
 *
 * - If this is a Index page, for example if the URl is /blog/index then the User can visit
 * http://localhost/blog or http://localhost/blog/ or http://localhost/blog/index or http://localhost/blog/index?param1=value1&param2=value2
 * and all are the same page.
 * - If the page is the root index we need to take account that even http://localhost/ is the same page.
 * - If the page is a Detail Page for a Content Type then we need to take account that the URL can be different.
 */
abstract class RegexUrlPatterStrategy {
    public static String REDIRECT_REGEX_TEMPLATE = "(http|https):\\/\\/.*(:[0-9])?%s(\\?.*)?";
    public abstract boolean isMatch(final HTMLPageAsset htmlPageAsset) throws RegexUrlPatterStrategyException;

    public abstract String getRegexPattern(final HTMLPageAsset htmlPageAsset);

    protected String getUriForRegex(final String uri) throws RegexUrlPatterStrategyException {
        return uri.replaceAll("/", "\\\\/");
    }
}
