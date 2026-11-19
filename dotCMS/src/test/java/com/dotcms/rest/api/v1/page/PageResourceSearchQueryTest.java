package com.dotcms.rest.api.v1.page;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.content.index.domain.ContentSearchResponse;
import com.dotcms.content.index.domain.ContentSearchResults;
import com.dotcms.content.index.domain.SearchHits;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.exception.BadRequestException;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Verifies the search request {@link PageResource#searchPage} sends to the content index for a
 * given {@code path}, without starting the platform.
 */
class PageResourceSearchQueryTest {

    private static final Set<String> TOP_LEVEL_KEYS = Set.of("query", "size", "from");
    private static final int PAGE_SIZE = 10;

    private ContentletAPI contentletAPI;
    private User user;
    private PageResource resource;

    @BeforeEach
    void setUp() throws Exception {
        user = mock(User.class);
        final InitDataObject initData = mock(InitDataObject.class);
        when(initData.getUser()).thenReturn(user);

        final WebResource webResource = mock(WebResource.class);
        when(webResource.init(isNull(), any(HttpServletRequest.class), any(HttpServletResponse.class),
                anyBoolean(), isNull())).thenReturn(initData);

        contentletAPI = mock(ContentletAPI.class);
        when(contentletAPI.search(anyString(), anyBoolean(), any(User.class), anyBoolean()))
                .thenReturn(new ContentSearchResults<>(
                        ContentSearchResponse.builder().hits(SearchHits.empty()).tookMillis(0).build(),
                        List.<Contentlet>of()));

        resource = new PageResource(mock(PageResourceHelper.class), webResource,
                mock(HTMLPageAssetRenderedAPI.class), contentletAPI, mock(HostWebAPI.class));
    }

    /**
     * Given a plain folder and page path,
     * When the pages are searched,
     * Then the request holds only the query, a fixed size and a zero offset, and the path is
     * matched as a literal fragment.
     */
    @Test
    void searchPage_plainPath_sendsQueryWithFixedSizeAndOffset() throws Exception {
        final JSONObject body = search("about-us/index");

        assertEquals(TOP_LEVEL_KEYS, body.keySet());
        assertEquals(PAGE_SIZE, body.getInt("size"));
        assertEquals(0, body.getInt("from"));
        assertEquals("+basetype:5 +path:*about\\-us\\/index* languageid:1^10", queryText(body));
    }

    /**
     * Given a path that starts with {@code //site/},
     * When the pages are searched,
     * Then the site name becomes a host filter and the rest of the value the path fragment.
     */
    @Test
    void searchPage_pathWithSite_filtersByHostName() throws Exception {
        final JSONObject body = search("//demo.dotcms.com/about-us/index");

        assertEquals(TOP_LEVEL_KEYS, body.keySet());
        assertEquals("+basetype:5 +path:*about\\-us\\/index* +conhostName:demo.dotcms.com languageid:1^10",
                queryText(body));
    }

    /**
     * Given a path holding double quotes, braces and what looks like more request keys,
     * When the pages are searched,
     * Then the whole value stays inside the query text and the request keeps only its own keys,
     * size and offset.
     */
    @Test
    void searchPage_pathWithQuotesAndBraces_staysInsideQueryText() throws Exception {
        final String path = "zz\" }}, size: 500, from: 3, aggs: {a: {terms: {field: \"title\"}}}, x: {y: \"";

        final JSONObject body = search(path);

        assertEquals(TOP_LEVEL_KEYS, body.keySet());
        assertEquals(PAGE_SIZE, body.getInt("size"));
        assertEquals(0, body.getInt("from"));
        assertEquals(Set.of("query_string"), body.getJSONObject("query").keySet());
        assertEquals(Set.of("query"), body.getJSONObject("query").getJSONObject("query_string").keySet());
        assertEquals("+basetype:5 +path:*zz\\\"\\ \\}\\},\\ size\\:\\ 500,\\ from\\:\\ 3,\\ aggs\\:\\ "
                        + "\\{a\\:\\ \\{terms\\:\\ \\{field\\:\\ \\\"title\\\"\\}\\}\\},\\ x\\:\\ \\{y\\:\\ \\\"* "
                        + "languageid:1^10",
                queryText(body));
    }

    /**
     * Given a path holding query syntax (wildcards, operators, groups, field names),
     * When the pages are searched,
     * Then that syntax is matched as literal text in a single path term.
     */
    @Test
    void searchPage_pathWithQuerySyntax_isMatchedLiterally() throws Exception {
        final JSONObject body = search("x* OR (title:other)");

        assertEquals("+basetype:5 +path:*x\\*\\ OR\\ \\(title\\:other\\)* languageid:1^10",
                queryText(body));
    }

    /**
     * Given a {@code //site/} path whose site part holds spaces and query syntax,
     * When the pages are searched,
     * Then the site part is matched as one literal host name.
     */
    @Test
    void searchPage_siteWithQuerySyntax_isMatchedLiterally() throws Exception {
        final JSONObject body = search("//demo OR +live:false/about");

        assertEquals("+basetype:5 +path:*about* +conhostName:demo\\ OR\\ \\+live\\:false languageid:1^10",
                queryText(body));
    }

    /**
     * Given a {@code //site/} path whose site part and path part hold range signs,
     * When the pages are searched,
     * Then the signs are left out, so each part is still matched as one literal value
     * rather than as a range.
     */
    @Test
    void searchPage_rangeSignsInSiteAndPath_areLeftOut() throws Exception {
        final JSONObject body = search("//<zzz/a>=b");

        assertEquals("+basetype:5 +path:*a=b* +conhostName:zzz languageid:1^10", queryText(body));
    }

    /**
     * Given a request without a path, or with a blank one,
     * When the pages are searched,
     * Then the request is rejected as a bad request and nothing is sent to the index.
     */
    @Test
    void searchPage_missingOrBlankPath_isRejected() throws Exception {
        assertThrows(BadRequestException.class, () -> callSearch(null));
        assertThrows(BadRequestException.class, () -> callSearch("   "));

        verify(contentletAPI, never()).search(anyString(), anyBoolean(), any(User.class), anyBoolean());
    }

    /**
     * Given a path made only of the {@code //} site prefix, with no site name,
     * When the pages are searched,
     * Then the request is rejected as a bad request instead of failing on the server.
     */
    @Test
    void searchPage_sitePrefixWithoutSiteName_isRejected() throws Exception {
        assertThrows(BadRequestException.class, () -> callSearch("//"));

        verify(contentletAPI, never()).search(anyString(), anyBoolean(), any(User.class), anyBoolean());
    }

    private JSONObject search(final String path) throws Exception {
        callSearch(path);

        final ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        verify(contentletAPI).search(query.capture(), eq(false), eq(user), eq(false));
        return new JSONObject(query.getValue());
    }

    private void callSearch(final String path) throws Exception {
        resource.searchPage(mock(HttpServletRequest.class), new EmptyHttpResponse(), path, false, false);
    }

    private static String queryText(final JSONObject body) throws Exception {
        return body.getJSONObject("query").getJSONObject("query_string").getString("query");
    }
}
