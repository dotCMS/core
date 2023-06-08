package com.dotcms.experiments.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;

/**
 * {@link RegexUrlPatterStrategy} implementation to Root Index Page,
 * it means that the page is the root index page
 */
public class RootIndexRegexUrlPatterStrategy extends RegexUrlPatterStrategy{

    public static final String INDEX = "/index";

    @Override
    public boolean isMatch(HTMLPageAsset htmlPageAsset) throws RegexUrlPatterStrategyException{
        try {
            return htmlPageAsset.getURI().equals(INDEX);
        } catch (DotDataException e) {
            throw new RegexUrlPatterStrategyException(e);
        }
    }

    @Override
    public String getRegexPattern(HTMLPageAsset htmlPageAsset) {
        return String.format(REDIRECT_REGEX_TEMPLATE, "(\\/index|\\/)?");
    }
}
