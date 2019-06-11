package com.dotcms.rest.api.v1.page;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * {@link PageResource} test
 */
public class PageResourceTest {
    private ContentletAPI esapi;
    private PageResource pageResource;
    private User user;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void init() throws DotSecurityException, DotDataException {
        session = mock(HttpSession.class);
        request  = mock(HttpServletRequest.class);
        response  = mock(HttpServletResponse.class);

        final InitDataObject initDataObject = mock(InitDataObject.class);
        user = APILocator.getUserAPI().loadByUserByEmail("admin@dotcms.com", APILocator.getUserAPI().getSystemUser(), false);

        final PageResourceHelper pageResourceHelper = mock(PageResourceHelper.class);
        final WebResource webResource = mock(WebResource.class);
        final HTMLPageAssetRenderedAPI htmlPageAssetRenderedAPI = new HTMLPageAssetRenderedAPIImpl();
        esapi = mock(ContentletAPI.class);

        when(webResource.init(anyString(), any(HttpServletRequest.class), any(HttpServletResponse.class), anyBoolean(), anyString())).thenReturn(initDataObject);
        when(webResource.init(any(HttpServletRequest.class), any(HttpServletResponse.class), anyBoolean())).thenReturn(initDataObject);
        when(webResource.init(false, request, true)).thenReturn(initDataObject);
        when(initDataObject.getUser()).thenReturn(user);
        pageResource = new PageResource(pageResourceHelper, webResource, htmlPageAssetRenderedAPI, esapi);

        when(request.getSession()).thenReturn(session);
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
        final String path = "about-us/index";

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

        final Response response = pageResource.searchPage(request,  new EmptyHttpResponse(), path, false, true);
        RestUtilTest.verifySuccessResponse(response);

        final Collection contentLetsResponse = (Collection) ((ResponseEntityView) response.getEntity()).getEntity();
        assertEquals(contentLetsResponse.size(), 1);

        final Map responseMap = (Map) contentLetsResponse.iterator().next();
        assertEquals(responseMap.get("identifier"), contentlet.getIdentifier());
        assertEquals(responseMap.get("inode"), contentlet.getInode());
    }

    /**
     * Should return about-us/index page, when pass path with the host
     *
     * @throws JSONException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testPathParamWithHost()
            throws DotSecurityException, DotDataException {
        final String path = "//demo.dotcms.com/about-us/index";

        final SearchResponse searchResponse = mock(SearchResponse.class);

        final Contentlet contentlet = APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguage("a9f30020-54ef-494e-92ed-645e757171c2");

        final List contentlets = list(contentlet);
        final ESSearchResults results = new ESSearchResults(searchResponse, contentlets);
        final String query = String.format("{"
                + "query: {"
                + "query_string: {"
                + "query: \"+basetype:5 +path:*%s* +conhostName:demo.dotcms.com languageid:1^10\""
                + "}"
                + "}"
                + "}", "about-us/index".replace("/", "\\\\/"));


        when(esapi.esSearch(query, false, user, false)).thenReturn(results);

        final Response response = pageResource.searchPage(request,  new EmptyHttpResponse(), path, false, true);
        RestUtilTest.verifySuccessResponse(response);

        final Collection contentLetsResponse = (Collection) ((ResponseEntityView) response.getEntity()).getEntity();
        assertEquals(contentLetsResponse.size(), 1);

        final Map responseMap = (Map) contentLetsResponse.iterator().next();
        assertEquals(responseMap.get("identifier"), contentlet.getIdentifier());
        assertEquals(responseMap.get("inode"), contentlet.getInode());
    }

    /**
     * Should return about-us/index page
     *
     * @throws JSONException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testRender() throws DotDataException, DotSecurityException {

        final String pageUri = "/about-us/index";
        final long languageId = 1;

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final Host host = APILocator.getHostAPI().findByName("demo.dotcms.com", systemUser, false);
        final HTMLPageAsset pageByPath = (HTMLPageAsset) APILocator.getHTMLPageAssetAPI().getPageByPath(pageUri, host, languageId, false);

        final Template template_Quest_2_Column_Left_Bar =
                APILocator.getTemplateAPI().find("fe654925-f011-487c-b5db-d7cb4ed2553a", systemUser, false);

        final Contentlet checkout = APILocator.getContentletAPIImpl().checkout(pageByPath.getInode(), systemUser, false);
        checkout.setStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, template_Quest_2_Column_Left_Bar.getIdentifier());

        final Contentlet checkin = APILocator.getContentletAPIImpl().checkin(checkout, systemUser, false);
        final Response response = pageResource
                .loadJson(request, this.response, pageUri, null, null,
                        String.valueOf(languageId), null);

        RestUtilTest.verifySuccessResponse(response);

        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        final Collection<? extends ContainerRaw> pageContainers = pageView.getContainers();
        final List<String> containerIds = pageContainers.stream()
                .map((ContainerRaw containerRaw) -> containerRaw.getContainer().getIdentifier())
                .collect(Collectors.toList());

        assertTrue(containerIds.contains("fc193c82-8c32-4abe-ba8a-49522328c93e"));
        assertTrue(containerIds.contains("5363c6c6-5ba0-4946-b7af-cf875188ac2e"));
        assertTrue(containerIds.contains("a050073a-a31e-4aab-9307-86bfb248096a"));

        for (final ContainerRaw pageContainer : pageContainers) {
            final Map<String, List<Map<String, Object>>> contentlets = pageContainer.getContentlets();

            switch (pageContainer.getContainer().getIdentifier()) {
                case "fc193c82-8c32-4abe-ba8a-49522328c93e":
                    assertEquals(contentlets.size(), 1);
                    assertTrue(contentlets.containsKey("uuid-LEGACY_RELATION_TYPE"));
                    assertTrue(contentlets.get("uuid-LEGACY_RELATION_TYPE").isEmpty());
                    break;
                case "5363c6c6-5ba0-4946-b7af-cf875188ac2e":
                    assertEquals(contentlets.size(), 1);
                    assertTrue(contentlets.containsKey("uuid-1"));
                    assertEquals(contentlets.get("uuid-1").size(), 3);
                    assertEquals(contentlets.get("uuid-1").get(0).get("inode").toString(),
                            "55cda310-c503-44d9-b028-7bcaf7b66339");
                    assertEquals(contentlets.get("uuid-1").get(1).get("inode").toString(),
                            "25105f18-4d1a-4be6-a78a-c42ee4c00d61");
                    assertEquals(contentlets.get("uuid-1").get(2).get("inode").toString(),
                            "da45088c-0096-4300-8c25-1a2ff07c00ae");
                    break;
                case "a050073a-a31e-4aab-9307-86bfb248096a":
                    assertEquals(contentlets.size(), 1);
                    assertTrue(contentlets.containsKey("uuid-1"));
                    assertEquals(contentlets.get("uuid-1").size(), 1);
                    assertEquals(contentlets.get("uuid-1").get(0).get("inode").toString(),
                            "f6406747-0220-41fb-86e4-32bce21a8822");
                    break;
            }
        }
    }
}
