package com.dotcms.rest.api.v1.analytics.event;

import com.dotcms.rest.api.v1.analytics.content.util.ContentAnalyticsUtil;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotmarketing.util.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link UrlParamFilter}, which reduces the page URL fields of analytics events
 * to an allowlist of query parameters before they are forwarded.
 */
class UrlParamFilterTest {

    private static final Set<String> DEFAULTS = UrlParamFilter.DEFAULT_ALLOWED_PARAMS;

    @AfterEach
    void resetConfig() {
        Config.setProperty(UrlParamFilter.ALLOWED_PARAMS_PROPERTY, null);
    }

    // --- filterUrl ---------------------------------------------------------------------------

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: a {@code null} URL.
     * Expected result: {@code null}.
     */
    @Test
    void filterUrl_null_returnsNull() {
        assertNull(UrlParamFilter.filterUrl(null, DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: an empty URL.
     * Expected result: an empty string.
     */
    @Test
    void filterUrl_empty_returnsEmpty() {
        assertEquals("", UrlParamFilter.filterUrl("", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p}, no query and no fragment.
     * Expected result: the URL unchanged.
     */
    @Test
    void filterUrl_noQuery_returnsUnchanged() {
        assertEquals("https://h/p", UrlParamFilter.filterUrl("https://h/p", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p?utm_source=s&gclid=g}, every param allowed.
     * Expected result: the URL unchanged.
     */
    @Test
    void filterUrl_onlyAllowedParams_returnsUnchanged() {
        assertEquals("https://h/p?utm_source=s&gclid=g",
                UrlParamFilter.filterUrl("https://h/p?utm_source=s&gclid=g", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p?a=1&utm_source=s&b=2&utm_medium=m}.
     * Expected result: {@code https://h/p?utm_source=s&utm_medium=m}, original order kept.
     */
    @Test
    void filterUrl_mixedParams_keepsOnlyAllowedInOrder() {
        assertEquals("https://h/p?utm_source=s&utm_medium=m",
                UrlParamFilter.filterUrl("https://h/p?a=1&utm_source=s&b=2&utm_medium=m", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p?a=1&b=2}, no allowed params.
     * Expected result: {@code https://h/p}, with no trailing {@code ?}.
     */
    @Test
    void filterUrl_noAllowedParams_dropsQuery() {
        assertEquals("https://h/p", UrlParamFilter.filterUrl("https://h/p?a=1&b=2", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p?a=1#/r?b=2&utm_source=s} and {@code https://h/p#a=1}.
     * Expected result: {@code https://h/p#/r?utm_source=s} and {@code https://h/p}; the
     * fragment follows the same rule as {@code filterFragment}.
     */
    @Test
    void filterUrl_fragment_followsFragmentRule() {
        assertEquals("https://h/p#/r?utm_source=s",
                UrlParamFilter.filterUrl("https://h/p?a=1#/r?b=2&utm_source=s", DEFAULTS));
        assertEquals("https://h/p", UrlParamFilter.filterUrl("https://h/p#a=1", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p?UTM_Source=s}.
     * Expected result: unchanged; names match regardless of case.
     */
    @Test
    void filterUrl_allowedNameDifferentCase_isKept() {
        assertEquals("https://h/p?UTM_Source=s", UrlParamFilter.filterUrl("https://h/p?UTM_Source=s", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p?utm_source=a%20b%26c&a=1}.
     * Expected result: {@code https://h/p?utm_source=a%20b%26c}, value bytes unchanged.
     */
    @Test
    void filterUrl_encodedValue_keptAsIs() {
        assertEquals("https://h/p?utm_source=a%20b%26c",
                UrlParamFilter.filterUrl("https://h/p?utm_source=a%20b%26c&a=1", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p?utm_term=x&a=1&utm_term=y}.
     * Expected result: {@code https://h/p?utm_term=x&utm_term=y}.
     */
    @Test
    void filterUrl_repeatedAllowedParam_allKept() {
        assertEquals("https://h/p?utm_term=x&utm_term=y",
                UrlParamFilter.filterUrl("https://h/p?utm_term=x&a=1&utm_term=y", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p?utm_id&a&utm_term=}.
     * Expected result: {@code https://h/p?utm_id&utm_term=}.
     */
    @Test
    void filterUrl_paramWithoutValue_treatedAsParam() {
        assertEquals("https://h/p?utm_id&utm_term=",
                UrlParamFilter.filterUrl("https://h/p?utm_id&a&utm_term=", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: the relative URL {@code /p?a=1&utm_source=s}.
     * Expected result: {@code /p?utm_source=s}.
     */
    @Test
    void filterUrl_relativePath_isFiltered() {
        assertEquals("/p?utm_source=s", UrlParamFilter.filterUrl("/p?a=1&utm_source=s", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code http://[h/p?a=1&utm_source=s#k=v}, which contains characters that are
     * not valid in a URI.
     * Expected result: {@code http://[h/p?utm_source=s}, and no exception.
     */
    @Test
    void filterUrl_unparseableUrl_filteredWithoutError() {
        assertEquals("http://[h/p?utm_source=s",
                assertDoesNotThrow(() -> UrlParamFilter.filterUrl("http://[h/p?a=1&utm_source=s#k=v", DEFAULTS)));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p?a=1&&utm_source=s&}.
     * Expected result: {@code https://h/p?utm_source=s}.
     */
    @Test
    void filterUrl_emptyPairs_areIgnored() {
        assertEquals("https://h/p?utm_source=s", UrlParamFilter.filterUrl("https://h/p?a=1&&utm_source=s&", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p?utm%5Fsource=s&utm_source=t}.
     * Expected result: {@code https://h/p?utm_source=t}; names are compared as sent, not decoded.
     */
    @Test
    void filterUrl_encodedParamName_comparedRaw() {
        assertEquals("https://h/p?utm_source=t",
                UrlParamFilter.filterUrl("https://h/p?utm%5Fsource=s&utm_source=t", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: a second {@code ?} after an allowed param, as in
     * {@code https://h/p?utm_campaign=c?a=1}, {@code https://h/p?utm_campaign=c?utm_source=s} and
     * {@code https://h/p?utm_content=x?y}.
     * Expected result: {@code ?} separates params like {@code &}: {@code https://h/p?utm_campaign=c},
     * {@code https://h/p?utm_campaign=c&utm_source=s} and {@code https://h/p?utm_content=x}.
     */
    @Test
    void filterUrl_secondQuestionMark_separatesParams() {
        assertEquals("https://h/p?utm_campaign=c", UrlParamFilter.filterUrl("https://h/p?utm_campaign=c?a=1", DEFAULTS));
        assertEquals("https://h/p?utm_campaign=c&utm_source=s",
                UrlParamFilter.filterUrl("https://h/p?utm_campaign=c?utm_source=s", DEFAULTS));
        assertEquals("https://h/p?utm_content=x", UrlParamFilter.filterUrl("https://h/p?utm_content=x?y", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p?utm_source=s;a=1} and {@code https://h/p?a=1;utm_medium=m}.
     * Expected result: {@code ;} separates params like {@code &}: {@code https://h/p?utm_source=s}
     * and {@code https://h/p?utm_medium=m}.
     */
    @Test
    void filterUrl_semicolon_separatesParams() {
        assertEquals("https://h/p?utm_source=s", UrlParamFilter.filterUrl("https://h/p?utm_source=s;a=1", DEFAULTS));
        assertEquals("https://h/p?utm_medium=m", UrlParamFilter.filterUrl("https://h/p?a=1;utm_medium=m", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: a URL with 200,000 one-letter params before an allowed one, and a URL whose
     * first param name is 100,000 characters long.
     * Expected result: only the allowed param is kept in both cases.
     */
    @Test
    void filterUrl_veryLongParamLists_keepOnlyAllowed() {
        assertEquals("https://h/p?utm_source=s",
                UrlParamFilter.filterUrl("https://h/p?" + "a&".repeat(200_000) + "utm_source=s", DEFAULTS));
        assertEquals("https://h/p?utm_source=s",
                UrlParamFilter.filterUrl("https://h/p?" + "x".repeat(100_000) + "=1&utm_source=s", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: user info before the host: {@code https://u:p@h/p?a=1}, {@code https://u@h}
     * and the scheme-relative {@code //u:p@h/p}; plus {@code https://h/p@x?utm_source=s}, where
     * the {@code @} is in the path.
     * Expected result: {@code https://h/p}, {@code https://h} and {@code //h/p}; the last URL is
     * unchanged.
     */
    @Test
    void filterUrl_userInfo_isRemoved() {
        assertEquals("https://h/p", UrlParamFilter.filterUrl("https://u:p@h/p?a=1", DEFAULTS));
        assertEquals("https://h", UrlParamFilter.filterUrl("https://u@h", DEFAULTS));
        assertEquals("//h/p", UrlParamFilter.filterUrl("//u:p@h/p", DEFAULTS));
        assertEquals("https://h/p@x?utm_source=s", UrlParamFilter.filterUrl("https://h/p@x?utm_source=s", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p?utm_marketing_tactic=t&a=1}; {@code utm_marketing_tactic}
     * is the longest default name; and, with the allowlist {@code {"ab"}}, the URL
     * {@code https://h/p?ab=1&abc=2}.
     * Expected result: {@code https://h/p?utm_marketing_tactic=t} and {@code https://h/p?ab=1}; a
     * name exactly as long as the longest allowed name is still matched.
     */
    @Test
    void filterUrl_nameAsLongAsLongestAllowed_isKept() {
        assertEquals("https://h/p?utm_marketing_tactic=t",
                UrlParamFilter.filterUrl("https://h/p?utm_marketing_tactic=t&a=1", DEFAULTS));
        assertEquals("https://h/p?ab=1", UrlParamFilter.filterUrl("https://h/p?ab=1&abc=2", Set.of("ab")));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: the relative URL {@code /p//u@h}, where {@code //} is part of the path and
     * not preceded by a scheme.
     * Expected result: unchanged; nothing is treated as user info.
     */
    @Test
    void filterUrl_doubleSlashInsidePath_isNotUserInfo() {
        assertEquals("/p//u@h", UrlParamFilter.filterUrl("/p//u@h", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p;jsessionid=A1?utm_source=s},
     * {@code https://h/p;JSESSIONID=A1/q} and {@code https://h/p;v=1}.
     * Expected result: {@code https://h/p?utm_source=s}, {@code https://h/p/q}; the last URL is
     * unchanged.
     */
    @Test
    void filterUrl_pathSessionId_isRemoved() {
        assertEquals("https://h/p?utm_source=s",
                UrlParamFilter.filterUrl("https://h/p;jsessionid=A1?utm_source=s", DEFAULTS));
        assertEquals("https://h/p/q", UrlParamFilter.filterUrl("https://h/p;JSESSIONID=A1/q", DEFAULTS));
        assertEquals("https://h/p;v=1", UrlParamFilter.filterUrl("https://h/p;v=1", DEFAULTS));
    }

    // --- filterPath --------------------------------------------------------------------------

    /**
     * Method to test: {@link UrlParamFilter#filterPath}
     * Given scenario: {@code null}, {@code /p}, {@code /p;jsessionid=A1} and
     * {@code /a;jsessionid=A1/b}.
     * Expected result: {@code null}, {@code /p}, {@code /p} and {@code /a/b}.
     */
    @Test
    void filterPath_sessionId_isRemoved() {
        assertNull(UrlParamFilter.filterPath(null));
        assertEquals("/p", UrlParamFilter.filterPath("/p"));
        assertEquals("/p", UrlParamFilter.filterPath("/p;jsessionid=A1"));
        assertEquals("/a/b", UrlParamFilter.filterPath("/a;jsessionid=A1/b"));
    }

    // --- filterQuery -------------------------------------------------------------------------

    /**
     * Method to test: {@link UrlParamFilter#filterQuery}
     * Given scenario: {@code null}, {@code ""} and {@code "?"}.
     * Expected result: {@code null}, {@code ""} and {@code ""}.
     */
    @Test
    void filterQuery_nullEmptyOrBareQuestionMark() {
        assertNull(UrlParamFilter.filterQuery(null, DEFAULTS));
        assertEquals("", UrlParamFilter.filterQuery("", DEFAULTS));
        assertEquals("", UrlParamFilter.filterQuery("?", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterQuery}
     * Given scenario: {@code ?a=1&utm_source=s}.
     * Expected result: {@code ?utm_source=s}.
     */
    @Test
    void filterQuery_withLeadingQuestionMark_keepsPrefix() {
        assertEquals("?utm_source=s", UrlParamFilter.filterQuery("?a=1&utm_source=s", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterQuery}
     * Given scenario: {@code a=1&utm_source=s}.
     * Expected result: {@code utm_source=s}.
     */
    @Test
    void filterQuery_withoutPrefix_isFiltered() {
        assertEquals("utm_source=s", UrlParamFilter.filterQuery("a=1&utm_source=s", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterQuery}
     * Given scenario: {@code ?a=1&b=2}.
     * Expected result: {@code ""}.
     */
    @Test
    void filterQuery_noAllowedParams_returnsEmpty() {
        assertEquals("", UrlParamFilter.filterQuery("?a=1&b=2", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterQuery}
     * Given scenario: {@code ?utm_campaign=c?a=1} and {@code ?utm_source=s;a=1}.
     * Expected result: {@code ?utm_campaign=c} and {@code ?utm_source=s}.
     */
    @Test
    void filterQuery_secondQuestionMarkOrSemicolon_separatesParams() {
        assertEquals("?utm_campaign=c", UrlParamFilter.filterQuery("?utm_campaign=c?a=1", DEFAULTS));
        assertEquals("?utm_source=s", UrlParamFilter.filterQuery("?utm_source=s;a=1", DEFAULTS));
    }

    // --- filterFragment ----------------------------------------------------------------------

    /**
     * Method to test: {@link UrlParamFilter#filterFragment}
     * Given scenario: the hash route {@code #/products/42}.
     * Expected result: unchanged.
     */
    @Test
    void filterFragment_routeOnly_isKept() {
        assertEquals("#/products/42", UrlParamFilter.filterFragment("#/products/42", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterFragment}
     * Given scenario: {@code #/products/42?a=1&utm_source=s}.
     * Expected result: {@code #/products/42?utm_source=s}.
     */
    @Test
    void filterFragment_routeWithQuery_keepsRouteAndAllowedParams() {
        assertEquals("#/products/42?utm_source=s",
                UrlParamFilter.filterFragment("#/products/42?a=1&utm_source=s", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterFragment}
     * Given scenario: {@code #/products/42?a=1}.
     * Expected result: {@code #/products/42}, with no trailing {@code ?}.
     */
    @Test
    void filterFragment_routeWithNoAllowedParams_keepsRouteOnly() {
        assertEquals("#/products/42", UrlParamFilter.filterFragment("#/products/42?a=1", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterFragment}
     * Given scenario: {@code #a=1&b=2} and {@code #a=1}.
     * Expected result: {@code ""} for both; a fragment made of params is filtered like a query.
     */
    @Test
    void filterFragment_parameterStyle_noAllowedParams_returnsEmpty() {
        assertEquals("", UrlParamFilter.filterFragment("#a=1&b=2", DEFAULTS));
        assertEquals("", UrlParamFilter.filterFragment("#a=1", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterFragment}
     * Given scenario: {@code #utm_source=s&a=1}.
     * Expected result: {@code #utm_source=s}.
     */
    @Test
    void filterFragment_parameterStyle_keepsAllowedParams() {
        assertEquals("#utm_source=s", UrlParamFilter.filterFragment("#utm_source=s&a=1", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterFragment}
     * Given scenario: hash routes with parameter-like segments:
     * {@code #/catalog/color=red?utm_source=x}, {@code #/products;id=42}, {@code #!/p;id=1},
     * {@code #/r=1?a=1}, {@code #!/verify/k=abc} and {@code #/reset&m=x}.
     * Expected result: the route is kept up to the first {@code &} or {@code ;}, without the
     * first segment containing {@code =} and everything after it; its query is still filtered:
     * {@code #/catalog?utm_source=x}, {@code #/products}, {@code #!/p}, {@code #/},
     * {@code #!/verify} and {@code #/reset}.
     */
    @Test
    void filterFragment_routeWithParameterSegment_keepsRouteUpToIt() {
        assertEquals("#/catalog?utm_source=x",
                UrlParamFilter.filterFragment("#/catalog/color=red?utm_source=x", DEFAULTS));
        assertEquals("#/products", UrlParamFilter.filterFragment("#/products;id=42", DEFAULTS));
        assertEquals("#!/p", UrlParamFilter.filterFragment("#!/p;id=1", DEFAULTS));
        assertEquals("#/", UrlParamFilter.filterFragment("#/r=1?a=1", DEFAULTS));
        assertEquals("#!/verify", UrlParamFilter.filterFragment("#!/verify/k=abc", DEFAULTS));
        assertEquals("#/reset", UrlParamFilter.filterFragment("#/reset&m=x", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterFragment}
     * Given scenario: a slash route made only of pairs, {@code #/k=x&t=Bearer} and
     * {@code #!/k=x&t=Bearer}.
     * Expected result: {@code #/} and {@code #!/}; no pair is kept.
     */
    @Test
    void filterFragment_slashRouteOfPairs_keepsOnlyRoot() {
        assertEquals("#/", UrlParamFilter.filterFragment("#/k=x&t=Bearer", DEFAULTS));
        assertEquals("#!/", UrlParamFilter.filterFragment("#!/k=x&t=Bearer", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterUrl}
     * Given scenario: {@code https://h/p?a=1#/catalog/color=red} and
     * {@code https://h/p#/k=x&t=Bearer}.
     * Expected result: {@code https://h/p#/catalog} and {@code https://h/p#/}.
     */
    @Test
    void filterUrl_hashRouteWithParameterSegment_keepsRouteUpToIt() {
        assertEquals("https://h/p#/catalog",
                UrlParamFilter.filterUrl("https://h/p?a=1#/catalog/color=red", DEFAULTS));
        assertEquals("https://h/p#/", UrlParamFilter.filterUrl("https://h/p#/k=x&t=Bearer", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterFragment}
     * Given scenario: {@code #section-2}, {@code #!/page} and {@code section-2} (no prefix).
     * Expected result: each unchanged.
     */
    @Test
    void filterFragment_plainAnchor_isKept() {
        assertEquals("#section-2", UrlParamFilter.filterFragment("#section-2", DEFAULTS));
        assertEquals("#!/page", UrlParamFilter.filterFragment("#!/page", DEFAULTS));
        assertEquals("section-2", UrlParamFilter.filterFragment("section-2", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterFragment}
     * Given scenario: {@code null}, {@code ""} and {@code "#"}.
     * Expected result: {@code null}, {@code ""} and {@code ""}.
     */
    @Test
    void filterFragment_nullEmptyOrBareHash() {
        assertNull(UrlParamFilter.filterFragment(null, DEFAULTS));
        assertEquals("", UrlParamFilter.filterFragment("", DEFAULTS));
        assertEquals("", UrlParamFilter.filterFragment("#", DEFAULTS));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterFragment}
     * Given scenario: {@code #/r?utm_source=s?a=1} and {@code #utm_source=s;a=1}.
     * Expected result: {@code #/r?utm_source=s} and {@code #utm_source=s}.
     */
    @Test
    void filterFragment_secondQuestionMarkOrSemicolon_separatesParams() {
        assertEquals("#/r?utm_source=s", UrlParamFilter.filterFragment("#/r?utm_source=s?a=1", DEFAULTS));
        assertEquals("#utm_source=s", UrlParamFilter.filterFragment("#utm_source=s;a=1", DEFAULTS));
    }

    // --- resolveAllowedParams / isEnabled ----------------------------------------------------

    /**
     * Method to test: {@link UrlParamFilter#resolveAllowedParams}
     * Given scenario: no instance setting and no site setting.
     * Expected result: exactly utm_source, utm_medium, utm_campaign, utm_term, utm_content,
     * utm_id, utm_source_platform, utm_creative_format, utm_marketing_tactic, gclid, fbclid, dclid.
     */
    @Test
    void resolveAllowedParams_defaults_returnsBuiltInSet() {
        assertEquals(Set.of("utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
                        "utm_id", "utm_source_platform", "utm_creative_format", "utm_marketing_tactic",
                        "gclid", "fbclid", "dclid"),
                UrlParamFilter.resolveAllowedParams(Map.of()));
    }

    /**
     * Method to test: {@link UrlParamFilter#resolveAllowedParams}
     * Given scenario: {@code analytics.url.allowed.params=" Lang , page "}.
     * Expected result: built-in set plus {@code lang} and {@code page}.
     */
    @Test
    void resolveAllowedParams_instanceSetting_isAdded() {
        Config.setProperty(UrlParamFilter.ALLOWED_PARAMS_PROPERTY, " Lang , page ");

        assertEquals(withDefaults("lang", "page"), UrlParamFilter.resolveAllowedParams(Map.of()));
    }

    /**
     * Method to test: {@link UrlParamFilter#resolveAllowedParams}
     * Given scenario: the documented property name {@code analytics.url.allowed.params} is set to
     * {@code "lang"}.
     * Expected result: built-in set plus {@code lang}.
     */
    @Test
    void resolveAllowedParams_documentedPropertyName_isRead() {
        Config.setProperty("analytics.url.allowed.params", "lang");
        try {
            assertEquals(withDefaults("lang"), UrlParamFilter.resolveAllowedParams(Map.of()));
        } finally {
            Config.setProperty("analytics.url.allowed.params", null);
        }
    }

    /**
     * Method to test: {@link UrlParamFilter#resolveAllowedParams}
     * Given scenario: site secret {@code allowedUrlParams="ref"}.
     * Expected result: built-in set plus {@code ref}.
     */
    @Test
    void resolveAllowedParams_siteSetting_isAdded() {
        assertEquals(withDefaults("ref"), UrlParamFilter.resolveAllowedParams(siteSetting("ref")));
    }

    /**
     * Method to test: {@link UrlParamFilter#resolveAllowedParams}
     * Given scenario: site secret {@code allowedUrlParams=" a , , b "}, with an empty entry.
     * Expected result: built-in set plus {@code a} and {@code b}.
     */
    @Test
    void resolveAllowedParams_siteSettingWithEmptyEntries_ignoresThem() {
        assertEquals(withDefaults("a", "b"), UrlParamFilter.resolveAllowedParams(siteSetting(" a , , b ")));
    }

    /**
     * Method to test: {@link UrlParamFilter#resolveAllowedParams}
     * Given scenario: both the instance setting and the site setting are present.
     * Expected result: the union of built-in, instance and site params.
     */
    @Test
    void resolveAllowedParams_bothSettings_areMerged() {
        Config.setProperty(UrlParamFilter.ALLOWED_PARAMS_PROPERTY, "lang");

        assertEquals(withDefaults("lang", "ref"), UrlParamFilter.resolveAllowedParams(siteSetting("ref")));
    }

    /**
     * Method to test: {@link UrlParamFilter#resolveAllowedParams}
     * Given scenario: instance setting {@code " , ,"} and a {@code null} secrets map.
     * Expected result: the built-in set, no exception.
     */
    @Test
    void resolveAllowedParams_blankOrMissingSettings_returnsBuiltInSet() {
        Config.setProperty(UrlParamFilter.ALLOWED_PARAMS_PROPERTY, " , ,");

        assertEquals(DEFAULTS, assertDoesNotThrow(() -> UrlParamFilter.resolveAllowedParams(null)));
    }

    /**
     * Method to test: {@link UrlParamFilter#isEnabled}
     * Given scenario: the site has no {@code filterUrlParams} setting: a {@code null} secrets map,
     * an empty one, and one with only other fields.
     * Expected result: {@code true} in every case.
     */
    @Test
    void isEnabled_noSiteSetting_isTrue() {
        assertTrue(UrlParamFilter.isEnabled(null));
        assertTrue(UrlParamFilter.isEnabled(Map.of()));
        assertTrue(UrlParamFilter.isEnabled(siteSetting("ref")));
    }

    /**
     * Method to test: {@link UrlParamFilter#isEnabled}
     * Given scenario: the site's {@code filterUrlParams} is {@code "false"}, then {@code " FALSE "}.
     * Expected result: {@code false} in both cases.
     */
    @Test
    void isEnabled_siteSettingFalse_isFalse() {
        assertFalse(UrlParamFilter.isEnabled(filterSetting("false")));
        assertFalse(UrlParamFilter.isEnabled(filterSetting(" FALSE ")));
    }

    /**
     * Method to test: {@link UrlParamFilter#isEnabled}
     * Given scenario: the site's {@code filterUrlParams} is {@code "true"}, {@code ""} and
     * {@code "x"}.
     * Expected result: {@code true} in every case; only an explicit {@code false} turns it off.
     */
    @Test
    void isEnabled_siteSettingTrueBlankOrOther_isTrue() {
        assertTrue(UrlParamFilter.isEnabled(filterSetting("true")));
        assertTrue(UrlParamFilter.isEnabled(filterSetting("")));
        assertTrue(UrlParamFilter.isEnabled(filterSetting("x")));
    }

    // --- filterEventPages --------------------------------------------------------------------

    /**
     * Method to test: {@link UrlParamFilter#filterEventPages}
     * Given scenario: a pageview event with url {@code https://h/p?a=1&utm_source=s#/r?b=2},
     * doc_search {@code ?a=1&utm_source=s}, doc_hash {@code #/r?b=2}.
     * Expected result: url {@code https://h/p?utm_source=s#/r}, doc_search {@code ?utm_source=s},
     * doc_hash {@code #/r}.
     */
    @Test
    void filterEventPages_pageview_filtersUrlFields() {
        final Map<String, Object> page = page("https://h/p?a=1&utm_source=s#/r?b=2");
        page.put("doc_search", "?a=1&utm_source=s");
        page.put("doc_hash", "#/r?b=2");

        UrlParamFilter.filterEventPages(body(event("pageview", page)), DEFAULTS);

        assertEquals("https://h/p?utm_source=s#/r", page.get("url"));
        assertEquals("?utm_source=s", page.get("doc_search"));
        assertEquals("#/r", page.get("doc_hash"));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterEventPages}
     * Given scenario: a content_click event whose page.url is {@code https://h/p?a=1}.
     * Expected result: page.url {@code https://h/p}.
     */
    @Test
    void filterEventPages_contentEvent_filtersPageUrl() {
        final Map<String, Object> page = page("https://h/p?a=1");

        UrlParamFilter.filterEventPages(body(event("content_click", page)), DEFAULTS);

        assertEquals("https://h/p", page.get("url"));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterEventPages}
     * Given scenario: an event with doc_path, doc_host, title, utm, custom and a context block.
     * Expected result: all of those are unchanged.
     */
    @Test
    void filterEventPages_otherFields_unchanged() {
        final Map<String, Object> page = page("https://h/p?a=1");
        page.put("doc_path", "/p");
        page.put("doc_host", "h");
        page.put("title", "T?a=1");
        final Map<String, Object> event = event("pageview", page);
        @SuppressWarnings("unchecked")
        final Map<String, Object> data = (Map<String, Object>) event.get("data");
        data.put("utm", new LinkedHashMap<>(Map.of("source", "s")));
        data.put("custom", new LinkedHashMap<>(Map.of("k", "v?a=1")));
        final Map<String, Object> body = body(event);
        body.put("context", new LinkedHashMap<>(Map.of("site_auth", "x", "site_id", "y")));

        UrlParamFilter.filterEventPages(body, DEFAULTS);

        assertEquals("/p", page.get("doc_path"));
        assertEquals("h", page.get("doc_host"));
        assertEquals("T?a=1", page.get("title"));
        assertEquals(Map.of("source", "s"), data.get("utm"));
        assertEquals(Map.of("k", "v?a=1"), data.get("custom"));
        assertEquals(Map.of("site_auth", "x", "site_id", "y"), body.get("context"));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterEventPages}
     * Given scenario: two events, each with a non-allowed param in page.url.
     * Expected result: both are filtered.
     */
    @Test
    void filterEventPages_multipleEvents_allFiltered() {
        final Map<String, Object> first = page("https://h/p1?a=1");
        final Map<String, Object> second = page("https://h/p2?b=2");

        UrlParamFilter.filterEventPages(body(event("pageview", first), event("content_click", second)), DEFAULTS);

        assertEquals("https://h/p1", first.get("url"));
        assertEquals("https://h/p2", second.get("url"));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterEventPages}
     * Given scenario: no events key; events not a list; an event without data or page; a page
     * that is null; scalar (number, boolean, null) url fields; alongside one well-formed event.
     * Expected result: no exception; those entries are kept unchanged; the well-formed event is
     * filtered.
     */
    @Test
    void filterEventPages_unexpectedShapes_noError() {
        assertDoesNotThrow(() -> UrlParamFilter.filterEventPages(null, DEFAULTS));
        assertDoesNotThrow(() -> UrlParamFilter.filterEventPages(new LinkedHashMap<>(), DEFAULTS));
        final Map<String, Object> notAList = new LinkedHashMap<>(Map.of("events", "x"));
        assertDoesNotThrow(() -> UrlParamFilter.filterEventPages(notAList, DEFAULTS));

        final Map<String, Object> noData = new LinkedHashMap<>(Map.of("event_type", "pageview"));
        final Map<String, Object> noPage = new LinkedHashMap<>(Map.of("data", new LinkedHashMap<>()));
        final Map<String, Object> nullPage = event("pageview", null);
        final Map<String, Object> scalarFields = page(null);
        scalarFields.put("url", 42);
        scalarFields.put("doc_search", true);
        scalarFields.put("doc_hash", null);
        final Map<String, Object> wellFormed = page("https://h/p?a=1");
        final Map<String, Object> body = body("x", noData, noPage, nullPage,
                event("pageview", scalarFields), event("pageview", wellFormed));

        assertDoesNotThrow(() -> UrlParamFilter.filterEventPages(body, DEFAULTS));

        assertEquals(6, ((List<?>) body.get("events")).size());
        assertEquals(42, scalarFields.get("url"));
        assertEquals(true, scalarFields.get("doc_search"));
        assertTrue(scalarFields.containsKey("doc_hash"));
        assertNull(scalarFields.get("doc_hash"));
        assertEquals("https://h/p", wellFormed.get("url"));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterEventPages}
     * Given scenario: a page with {@code url} already allowed-only, plus {@code URL},
     * {@code Doc_Search} and {@code DOC_HASH} carrying non-allowed params.
     * Expected result: every variant is filtered: {@code https://h/p}, {@code ""} and {@code ""}.
     */
    @Test
    void filterEventPages_fieldNameDifferentCase_isFiltered() {
        final Map<String, Object> page = page("https://h/p");
        page.put("URL", "https://h/p?a=1");
        page.put("Doc_Search", "?a=1");
        page.put("DOC_HASH", "#a=1");

        UrlParamFilter.filterEventPages(body(event("pageview", page)), DEFAULTS);

        assertEquals("https://h/p", page.get("url"));
        assertEquals("https://h/p", page.get("URL"));
        assertEquals("", page.get("Doc_Search"));
        assertEquals("", page.get("DOC_HASH"));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterEventPages}
     * Given scenario: a page with {@code doc_path} {@code /p;jsessionid=A1} and another with
     * {@code Doc_Path} {@code /q;jsessionid=A1}.
     * Expected result: {@code /p} and {@code /q}.
     */
    @Test
    void filterEventPages_docPath_sessionIdRemoved() {
        final Map<String, Object> first = page(null);
        first.put("doc_path", "/p;jsessionid=A1");
        final Map<String, Object> second = page(null);
        second.put("Doc_Path", "/q;jsessionid=A1");

        UrlParamFilter.filterEventPages(body(event("pageview", first), event("pageview", second)), DEFAULTS);

        assertEquals("/p", first.get("doc_path"));
        assertEquals("/q", second.get("Doc_Path"));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterEventPages}
     * Given scenario: the container keys are spelled {@code Events}, {@code Data} and {@code Page}.
     * Expected result: the page url is filtered all the same.
     */
    @Test
    void filterEventPages_containerKeyDifferentCase_isFiltered() {
        final Map<String, Object> page = page("https://h/p?a=1");
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put("Page", page);
        final Map<String, Object> event = new LinkedHashMap<>();
        event.put("Data", data);
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("Events", new ArrayList<>(List.of(event)));

        UrlParamFilter.filterEventPages(body, DEFAULTS);

        assertEquals("https://h/p", page.get("url"));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterEventPages}
     * Given scenario: {@code url} is an array of strings, {@code doc_search} an object and
     * {@code doc_hash} an array.
     * Expected result: each is replaced with {@code ""}.
     */
    @Test
    void filterEventPages_arrayOrObjectValue_isBlanked() {
        final Map<String, Object> page = page(null);
        page.put("url", new ArrayList<>(List.of("https://h/p?a=1")));
        page.put("doc_search", new LinkedHashMap<>(Map.of("v", "?a=1")));
        page.put("doc_hash", new ArrayList<>(List.of("#a=1")));

        UrlParamFilter.filterEventPages(body(event("pageview", page)), DEFAULTS);

        assertEquals("", page.get("url"));
        assertEquals("", page.get("doc_search"));
        assertEquals("", page.get("doc_hash"));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterEventPages}
     * Given scenario: three events, one whose page is an array, one whose page is a string, and
     * one well-formed.
     * Expected result: the first two events are removed from the batch; the well-formed one
     * remains, filtered.
     */
    @Test
    @SuppressWarnings("unchecked")
    void filterEventPages_pageNotObject_dropsEvent() {
        final Map<String, Object> arrayPage = event("pageview", null);
        ((Map<String, Object>) arrayPage.get("data")).put("page",
                new ArrayList<>(List.of(page("https://h/p?a=1"))));
        final Map<String, Object> stringPage = event("pageview", null);
        ((Map<String, Object>) stringPage.get("data")).put("page", "https://h/p?a=1");
        final Map<String, Object> wellFormedPage = page("https://h/p?a=1");
        final Map<String, Object> wellFormed = event("pageview", wellFormedPage);
        final Map<String, Object> body = body(arrayPage, stringPage, wellFormed);

        UrlParamFilter.filterEventPages(body, DEFAULTS);

        assertEquals(List.of(wellFormed), body.get("events"));
        assertEquals("https://h/p", wellFormedPage.get("url"));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterEventPages}
     * Given scenario: four events, one whose data is an array holding a page, one whose data is
     * a string, one whose data is null, and one well-formed.
     * Expected result: the array-data and string-data events are removed from the batch; the
     * null-data event and the well-formed one remain, the latter filtered.
     */
    @Test
    void filterEventPages_dataNotObject_dropsEvent() {
        final Map<String, Object> arrayData = new LinkedHashMap<>();
        arrayData.put("event_type", "pageview");
        arrayData.put("data", new ArrayList<>(List.of(
                new LinkedHashMap<>(Map.of("page", page("https://h/p?a=1"))))));
        final Map<String, Object> stringData = new LinkedHashMap<>();
        stringData.put("event_type", "pageview");
        stringData.put("data", "https://h/p?a=1");
        final Map<String, Object> nullData = new LinkedHashMap<>();
        nullData.put("event_type", "pageview");
        nullData.put("data", null);
        final Map<String, Object> wellFormedPage = page("https://h/p?a=1");
        final Map<String, Object> wellFormed = event("pageview", wellFormedPage);
        final Map<String, Object> body = body(arrayData, stringData, nullData, wellFormed);

        UrlParamFilter.filterEventPages(body, DEFAULTS);

        assertEquals(List.of(nullData, wellFormed), body.get("events"));
        assertEquals("https://h/p", wellFormedPage.get("url"));
    }

    /**
     * Method to test: {@link UrlParamFilter#filterEventPages}
     * Given scenario: content_click events whose element has {@code href}
     * {@code /t?y=2&utm_source=s#k=v}, {@code HREF} {@code https://u@h/t;jsessionid=A1?y=2}, and
     * {@code href} as an array; plus element {@code text} {@code go?y=2}.
     * Expected result: {@code /t?utm_source=s}, {@code https://h/t}, {@code ""}; the text is
     * unchanged.
     */
    @Test
    void filterEventPages_elementHref_isFiltered() {
        final Map<String, Object> first = element("/t?y=2&utm_source=s#k=v");
        first.put("text", "go?y=2");
        final Map<String, Object> second = new LinkedHashMap<>();
        second.put("HREF", "https://u@h/t;jsessionid=A1?y=2");
        final Map<String, Object> third = new LinkedHashMap<>();
        third.put("href", new ArrayList<>(List.of("/t?y=2")));

        UrlParamFilter.filterEventPages(body(clickEvent(first), clickEvent(second), clickEvent(third)), DEFAULTS);

        assertEquals("/t?utm_source=s", first.get("href"));
        assertEquals("go?y=2", first.get("text"));
        assertEquals("https://h/t", second.get("HREF"));
        assertEquals("", third.get("href"));
    }

    /**
     * Method to test: {@link UrlParamFilter#removeUserInfoAndSessionIds}
     * Given scenario: a content_click event whose element href is
     * {@code https://u@h/t;jsessionid=A1?y=2#k=v}.
     * Expected result: {@code https://h/t?y=2#k=v}; query and fragment are kept.
     */
    @Test
    void removeUserInfoAndSessionIds_elementHref_userInfoAndSessionIdRemoved() {
        final Map<String, Object> element = element("https://u@h/t;jsessionid=A1?y=2#k=v");

        UrlParamFilter.removeUserInfoAndSessionIds(body(clickEvent(element)));

        assertEquals("https://h/t?y=2#k=v", element.get("href"));
    }

    // --- removeUserInfoAndSessionIds ----------------------------------------------------------------

    /**
     * Method to test: {@link UrlParamFilter#removeUserInfoAndSessionIds}
     * Given scenario: a page with url {@code https://u:p@h/p;jsessionid=A1?a=1#k=v}, doc_path
     * {@code /p;jsessionid=A1}, doc_search {@code ?a=1} and doc_hash {@code #k=v}; and a second
     * page whose keys are spelled {@code URL} and {@code Doc_Path}.
     * Expected result: user info and session id segments are removed (url
     * {@code https://h/p?a=1#k=v}, doc_path {@code /p}); query parameters, doc_search and doc_hash
     * are kept as they are; the second page is handled the same way.
     */
    @Test
    void removeUserInfoAndSessionIds_keepsParams_removesUserInfoAndSessionId() {
        final Map<String, Object> page = page("https://u:p@h/p;jsessionid=A1?a=1#k=v");
        page.put("doc_path", "/p;jsessionid=A1");
        page.put("doc_search", "?a=1");
        page.put("doc_hash", "#k=v");
        final Map<String, Object> upperCase = page(null);
        upperCase.put("URL", "https://u@h/q;jsessionid=A1?b=2");
        upperCase.put("Doc_Path", "/q;jsessionid=A1");

        UrlParamFilter.removeUserInfoAndSessionIds(body(event("pageview", page), event("pageview", upperCase)));

        assertEquals("https://h/p?a=1#k=v", page.get("url"));
        assertEquals("/p", page.get("doc_path"));
        assertEquals("?a=1", page.get("doc_search"));
        assertEquals("#k=v", page.get("doc_hash"));
        assertEquals("https://h/q?b=2", upperCase.get("URL"));
        assertEquals("/q", upperCase.get("Doc_Path"));
    }

    /**
     * Method to test: {@link UrlParamFilter#removeUserInfoAndSessionIds}
     * Given scenario: a {@code null} body, a body without events, and an event whose page is an
     * array.
     * Expected result: no exception; nothing is removed from the batch.
     */
    @Test
    void removeUserInfoAndSessionIds_unexpectedShapes_noErrorAndNoDrop() {
        assertDoesNotThrow(() -> UrlParamFilter.removeUserInfoAndSessionIds(null));
        assertDoesNotThrow(() -> UrlParamFilter.removeUserInfoAndSessionIds(new LinkedHashMap<>()));
        final Map<String, Object> arrayPage = event("pageview", null);
        @SuppressWarnings("unchecked")
        final Map<String, Object> data = (Map<String, Object>) arrayPage.get("data");
        data.put("page", new ArrayList<>(List.of(page("https://u@h/p"))));
        final Map<String, Object> body = body(arrayPage);

        assertDoesNotThrow(() -> UrlParamFilter.removeUserInfoAndSessionIds(body));

        assertEquals(1, ((List<?>) body.get("events")).size());
    }

    /**
     * Method to test: {@link UrlParamFilter#removeUserInfoAndSessionIds}
     * Given scenario: {@code events} that is not a list; an event that is not an object; an event
     * whose data is not an object; a page whose url is a number.
     * Expected result: no exception, and every value is left as it was.
     */
    @Test
    void removeUserInfoAndSessionIds_otherShapes_leftUnchanged() {
        final Map<String, Object> notAList = new LinkedHashMap<>(Map.of("events", "x"));
        assertDoesNotThrow(() -> UrlParamFilter.removeUserInfoAndSessionIds(notAList));
        assertEquals("x", notAList.get("events"));

        final Map<String, Object> stringData = new LinkedHashMap<>();
        stringData.put("data", "https://u@h/p");
        final Map<String, Object> numericUrl = page(null);
        numericUrl.put("url", 42);
        final Map<String, Object> body = body("x", stringData, event("pageview", numericUrl));

        assertDoesNotThrow(() -> UrlParamFilter.removeUserInfoAndSessionIds(body));

        assertEquals("https://u@h/p", stringData.get("data"));
        assertEquals(42, numericUrl.get("url"));
    }

    /**
     * Method to test: {@link UrlParamFilter#removeUserInfoAndSessionIds}
     * Given scenario: urls {@code https://u@h/p} (no query or fragment) and {@code https://u@h/p#k}
     * (fragment only).
     * Expected result: {@code https://h/p} and {@code https://h/p#k}.
     */
    @Test
    void removeUserInfoAndSessionIds_urlWithoutQuery_userInfoRemoved() {
        final Map<String, Object> plain = page("https://u@h/p");
        final Map<String, Object> withFragment = page("https://u@h/p#k");

        UrlParamFilter.removeUserInfoAndSessionIds(body(event("pageview", plain), event("pageview", withFragment)));

        assertEquals("https://h/p", plain.get("url"));
        assertEquals("https://h/p#k", withFragment.get("url"));
    }

    // --- helpers -----------------------------------------------------------------------------

    private static Set<String> withDefaults(final String... extra) {
        final Set<String> expected = new HashSet<>(DEFAULTS);
        expected.addAll(List.of(extra));
        return expected;
    }

    private static Map<String, Secret> siteSetting(final String names) {
        return Map.of(ContentAnalyticsUtil.ALLOWED_URL_PARAMS_KEY,
                Secret.builder().withType(Type.STRING).withValue(names).build());
    }

    private static Map<String, Secret> filterSetting(final String value) {
        return Map.of(ContentAnalyticsUtil.FILTER_URL_PARAMS_KEY,
                Secret.builder().withType(Type.BOOL).withValue(value).build());
    }

    private static Map<String, Object> page(final String url) {
        final Map<String, Object> page = new LinkedHashMap<>();
        if (url != null) {
            page.put("url", url);
        }
        return page;
    }

    private static Map<String, Object> event(final String type, final Map<String, Object> page) {
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put("page", page);
        final Map<String, Object> event = new LinkedHashMap<>();
        event.put("event_type", type);
        event.put("data", data);
        return event;
    }

    private static Map<String, Object> element(final String href) {
        final Map<String, Object> element = new LinkedHashMap<>();
        element.put("href", href);
        return element;
    }

    private static Map<String, Object> clickEvent(final Map<String, Object> element) {
        final Map<String, Object> event = event("content_click", page("https://h/p"));
        @SuppressWarnings("unchecked")
        final Map<String, Object> data = (Map<String, Object>) event.get("data");
        data.put("element", element);
        return event;
    }

    private static Map<String, Object> body(final Object... events) {
        final Map<String, Object> body = new LinkedHashMap<>();
        body.put("events", new ArrayList<>(List.of(events)));
        return body;
    }
}
