package com.dotcms.experiments.business;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.StructureUtil;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.List;

/**
 * Util class to calculate the regex pattern for a given {@link HTMLPageAsset}
 */
public enum ExperimentUrlPatternCalculator {
    INSTANCE;

    public static String REDIRECT_REGEX_TEMPLATE = "(http|https):\\/\\/.*:.*%s(\\?.*)?";
    public static final String INDEX = "/index";

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
    public String calculateUrlRegexPattern(final HTMLPageAsset htmlPageAsset) {

        try {
            String resulRegex = null;

            final List<String> urlMappedPattern = APILocator.getContentTypeAPI(
                            APILocator.systemUser())
                    .findUrlMappedPattern(htmlPageAsset.getIdentifier());

            if (UtilMethods.isSet(urlMappedPattern)) {
                final List<String> regexs = new ArrayList<>();

                for (String urlMap : urlMappedPattern) {
                    String regExForURLMap = StructureUtil.generateRegExForURLMap(urlMap);
                    regExForURLMap = regExForURLMap.substring(0, regExForURLMap.length() - 1);
                    final String uriForRegex = getUriForRegex(regExForURLMap);

                    regexs.add(String.format(REDIRECT_REGEX_TEMPLATE, uriForRegex));
                }

                resulRegex = String.join("|", regexs);

            } else if (htmlPageAsset.getURI().equals(INDEX)) {
                resulRegex =  String.format(REDIRECT_REGEX_TEMPLATE, "(\\/index|\\/)?");
            } else if (htmlPageAsset.getURI().endsWith(INDEX)) {
                final String uriWithoutIndex = htmlPageAsset.getURI().substring(0,
                        htmlPageAsset.getURI().length() - INDEX.length());
                final String uriForRegex = getUriForRegex(uriWithoutIndex) + "(\\/index|\\/)?";
                resulRegex = String.format(REDIRECT_REGEX_TEMPLATE, uriForRegex);
            } else {

                final String uriForRegex = getUriForRegex(htmlPageAsset.getURI());
                resulRegex = String.format(REDIRECT_REGEX_TEMPLATE, uriForRegex);
            }

            return String.format("^%s$",  resulRegex);

        } catch (DotDataException e) {
            throw new RuntimeException(String.format("It is not possible to get the URI for %s",
                    htmlPageAsset.getInode()), e);
        }
    }

    private static String getUriForRegex(final String uri) throws DotDataException {
        return uri.replaceAll("/", "\\\\/");
    }

}
