package com.dotcms.experiments.business;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;

/**
 * Default Implementation for {@link RegexUrlPatterStrategy}
 */
 
public class DefaultRegexUrlPatterStrategy implements RegexUrlPatterStrategy {

    @Override
    public boolean isMatch(HTMLPageAsset htmlPageAsset) {
        return true;
    }

    @Override
    public String getRegexPattern(HTMLPageAsset htmlPageAsset) throws RegexUrlPatterStrategyException{

        try {
            final String  uriForRegex = getUriForRegex(htmlPageAsset.getURI());
            return String.format(REDIRECT_REGEX_TEMPLATE, uriForRegex);
        } catch (DotDataException e) {
            throw new RegexUrlPatterStrategyException(e);
        }

    }
}
