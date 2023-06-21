package com.dotcms.experiments.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.StructureUtil;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link RegexUrlPatterStrategy} implementation to UrlMap {@link HTMLPageAsset}
 */
public class UrlMapRegexUrlPatterStrategy extends RegexUrlPatterStrategy{
    private List<String>  urlMappedPattern;
    private String urlMappedPatternLoadedFOr;

    @Override
    public boolean isMatch(final HTMLPageAsset htmlPageAsset) {
        loadUrlMapPatterns(htmlPageAsset);
        return UtilMethods.isSet(urlMappedPattern);

    }

    private void loadUrlMapPatterns(final HTMLPageAsset htmlPageAsset) throws RegexUrlPatterStrategyException{
        try {
            this.urlMappedPattern = APILocator.getContentTypeAPI(
                            APILocator.systemUser())
                    .findUrlMappedPattern(htmlPageAsset.getIdentifier());

            this.urlMappedPatternLoadedFOr = htmlPageAsset.getIdentifier();

        } catch (DotDataException e) {
            throw new RegexUrlPatterStrategyException(e);
        }
    }

    @Override
    public String getRegexPattern(final HTMLPageAsset htmlPageAsset) {
        if (UtilMethods.isSet(urlMappedPattern) || !urlMappedPatternLoadedFOr.equals(htmlPageAsset.getIdentifier())) {
            loadUrlMapPatterns(htmlPageAsset);
        }

        final List<String> regexs = new ArrayList<>();

        for (String urlMap : urlMappedPattern) {
            String regExForURLMap = StructureUtil.generateRegExForURLMap(urlMap);
            regExForURLMap = regExForURLMap.substring(0, regExForURLMap.length() - 1);
            final String uriForRegex = getUriForRegex(regExForURLMap);

            regexs.add(String.format(DEFAULT_URL_REGEX_TEMPLATE, uriForRegex));
        }

        return String.join("|", regexs);
    }
}
