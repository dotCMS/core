package com.dotcms.experiments.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;

/**
 * {@link RegexUrlPatterStrategy} implementation to any Index Page but no the Root index Page,
 * it means that the page's url ends with /index but not be equals to /index.
 */
public class IndexRegexUrlPatterStrategy implements RegexUrlPatterStrategy {

    public static final String INDEX = "/index";

    @Override
    public boolean isMatch(HTMLPageAsset htmlPageAsset) throws RegexUrlPatterStrategyException{
        try {
            return htmlPageAsset.getURI().endsWith(INDEX) && !htmlPageAsset.getURI().equals(INDEX);
        } catch (DotDataException e) {
            throw new RegexUrlPatterStrategyException(e);
        }
    }

    @Override
    public String getRegexPattern(HTMLPageAsset htmlPageAsset) throws RegexUrlPatterStrategyException{
       try {
            final String uriWithoutIndex = htmlPageAsset.getURI().substring(0,
                    htmlPageAsset.getURI().length() - INDEX.length());

            final String uriForRegex = getUriForRegex(uriWithoutIndex) + "(\\/index|\\/)?";
            return String.format(REDIRECT_REGEX_TEMPLATE, uriForRegex);
        } catch (DotDataException e) {
            throw new RegexUrlPatterStrategyException(e);
        }
    }
}
