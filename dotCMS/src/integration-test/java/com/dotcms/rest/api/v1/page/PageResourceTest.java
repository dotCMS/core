package com.dotcms.rest.api.v1.page;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.util.ContentletUtil;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.model.User;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.Map;

/**
 * {@link PageResource} test
 */
public class PageResourceTest {
    private ContentletAPI esapi;
    private PageResource pageResource;
    private User user;
    private HttpServletRequest request;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void init(){
        request  = mock(HttpServletRequest.class);
        final InitDataObject initDataObject = mock(InitDataObject.class);
        user = new User();

        final PageResourceHelper pageResourceHelper = mock(PageResourceHelper.class);
        final WebResource webResource = mock(WebResource.class);
        final HTMLPageAssetRenderedAPI htmlPageAssetRenderedAPI = mock(HTMLPageAssetRenderedAPI.class);
        esapi = mock(ContentletAPI.class);

        when(webResource.init(null, true, request, true, null)).thenReturn(initDataObject);
        when(initDataObject.getUser()).thenReturn(user);

        pageResource = new PageResource(pageResourceHelper, webResource, htmlPageAssetRenderedAPI, esapi);
    }

    /**
     * Should return about-us/index page
     *
     * @throws JSONException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testPathParam()
            throws DotSecurityException, DotDataException {
        String path = "about-us/index";

        final SearchResponse searchResponse = mock(SearchResponse.class);

        final Contentlet contentlet = APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguage("a9f30020-54ef-494e-92ed-645e757171c2");

        final List contentlets = list(contentlet);
        final ESSearchResults results = new ESSearchResults(searchResponse, contentlets);
        final String query = String.format("{"
                + "query: {"
                + "query_string: {"
                    + "query: \"+basetype:5 +path:*%s*  languageid:1^10\""
                    + "}"
                    + "}"
                + "}", path.replace("/", "\\\\/"));


        when(esapi.esSearch(query, false, user, false)).thenReturn(results);

        final Response response = pageResource.searchPage(request, path, false, true);
        RestUtilTest.verifySuccessResponse(response);

        final Collection contentLetsResponse = (Collection) ((ResponseEntityView) response.getEntity()).getEntity();
        assertEquals(contentLetsResponse.size(), 1);

        final Map responseMap = (Map) contentLetsResponse.iterator().next();
        assertEquals(responseMap.get("identifier"), contentlet.getIdentifier());
        assertEquals(responseMap.get("inode"), contentlet.getInode());
    }
}
