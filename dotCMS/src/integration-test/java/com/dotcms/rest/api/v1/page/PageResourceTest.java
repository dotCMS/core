package com.dotcms.rest.api.v1.page;

import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfig;
import com.dotcms.rest.*;
import com.dotcms.rest.api.v1.personalization.PersonalizationPersonaPageView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.ViewAsPageStatus;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.*;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
    private Host host;
    private final String pageName = "index" + System.currentTimeMillis();
    private final String folderName = "about-us" + System.currentTimeMillis();
    private final String hostName = "my.host.com" + System.currentTimeMillis();
    private final String pagePath = String.format("/%s/%s",folderName,pageName);
    private HTMLPageAsset pageAsset;
    private Template template;
    private Container container1;
    private Container container2;

    private InitDataObject initDataObject;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void init() throws DotSecurityException, DotDataException {

        // Collection to store attributes keys/values
        final Map<String, Object> attributes = new ConcurrentHashMap<>();
        session = mock(HttpSession.class);
        request  = mock(HttpServletRequest.class);
        response  = mock(HttpServletResponse.class);


        final ModuleConfig moduleConfig     = mock(ModuleConfig.class);
        initDataObject = mock(InitDataObject.class);
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

        when(request.getRequestURI()).thenReturn("/test");
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        when(session.getAttribute("clickstream")).thenReturn(new Clickstream());

        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        host = new SiteDataGen().name(hostName).nextPersisted();
        APILocator.getVersionableAPI().setWorking(host);
        APILocator.getVersionableAPI().setLive(host);
        when(request.getParameter("host_id")).thenReturn(host.getIdentifier());
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);
        when(request.getAttribute("com.dotcms.repackage.org.apache.struts.action.MODULE")).thenReturn(moduleConfig);
        // Mock setAttribute
        Mockito.doAnswer((InvocationOnMock invocation)-> {

                String key = invocation.getArgumentAt(0, String.class);
                Object value = invocation.getArgumentAt(1, Object.class);
                if (null != key && null != value) {
                    attributes.put(key, value);
                }
                return null;
        }).when(request).setAttribute(Mockito.anyString(), Mockito.anyObject());


        Folder aboutUs = APILocator.getFolderAPI().findFolderByPath(String.format("/%s/",folderName), host, APILocator.systemUser(), false);
        if(null == aboutUs || !UtilMethods.isSet(aboutUs.getIdentifier())) {
            aboutUs = new FolderDataGen().site(host).name(folderName).nextPersisted();
        }
        container1 = new ContainerDataGen().nextPersisted();
        container2 = new ContainerDataGen().nextPersisted();
        template = new TemplateDataGen().withContainer(container1.getIdentifier()).withContainer(container2.getIdentifier()).nextPersisted();
        APILocator.getVersionableAPI().setWorking(template);
        APILocator.getVersionableAPI().setLive(template);
        pageAsset = new HTMLPageDataGen(aboutUs, template)
                .languageId(defaultLang.getId()).friendlyName(pageName).pageURL(pageName)
                .title(pageName).nextPersisted();
        APILocator.getVersionableAPI().setWorking(pageAsset);
        APILocator.getVersionableAPI().setLive(pageAsset);
        pageAsset = HTMLPageAsset.class.cast(APILocator.getHTMLPageAssetAPI().getPageByPath(pagePath, host, defaultLang.getId(), true));
        assertNotNull(pageAsset.getIdentifier());
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

        final SearchResponse searchResponse = mock(SearchResponse.class);
        final List contentlets = list(pageAsset);
        final ESSearchResults results = new ESSearchResults(searchResponse, contentlets);
        final String query = String.format("{"
                + "query: {"
                + "query_string: {"
                + "query: \"+basetype:5 +path:*%s*  languageid:1^10\""
                + "}"
                + "}"
                + "}", pagePath.replace("/", "\\\\/"));


        when(esapi.esSearch(query, false, user, false)).thenReturn(results);

        final Response response = pageResource.searchPage(request,  new EmptyHttpResponse(), pagePath, false, true);
        RestUtilTest.verifySuccessResponse(response);

        final Collection contentLetsResponse = (Collection) ((ResponseEntityView) response.getEntity()).getEntity();
        assertEquals(contentLetsResponse.size(), 1);

        final Map responseMap = (Map) contentLetsResponse.iterator().next();
        assertEquals(responseMap.get("identifier"), pageAsset.getIdentifier());
        assertEquals(responseMap.get("inode"), pageAsset.getInode());
    }

    /**
     * Should return at least one persona personalized
     *
     * @throws JSONException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testGetPersonalizedPersonasOnPage()
            throws Exception {

        final Host  host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
        final String  htmlPage            = UUIDGenerator.generateUuid();
        final String  container           = UUIDGenerator.generateUuid();
        final String  content             = UUIDGenerator.generateUuid();
        final Persona persona    = new PersonaDataGen().keyTag("persona"+System.currentTimeMillis()).hostFolder(host.getIdentifier()).nextPersisted();
        final String personaTag  = persona.getKeyTag();
        final String personalization = Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + personaTag;

        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content, UUIDGenerator.generateUuid(), 1)); // dot:default
        multiTreeAPI.saveMultiTree(new MultiTree(htmlPage, container, content, UUIDGenerator.generateUuid(), 2, personalization)); // dot:somepersona

        persona.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().publish(persona, user, false);

        when(request.getRequestURI()).thenReturn("/index");
        final Response response = pageResource.getPersonalizedPersonasOnPage(request, new EmptyHttpResponse(),
                null, 0, 10, "title", "ASC", host.getIdentifier(), htmlPage);

        final ResponseEntityView entityView = (ResponseEntityView) response.getEntity();
        assertNotNull(entityView);

        final PaginatedArrayList<PersonalizationPersonaPageView> paginatedArrayList = (PaginatedArrayList) entityView.getEntity();
        assertNotNull(paginatedArrayList);

        Logger.info(this, "************ htmlPage: " + htmlPage + ", container: " + container + ", personaTag: " + personaTag);
        Logger.info(this, "************ PaginatedArrayList1: " + paginatedArrayList);
        Logger.info(this, "************ PaginatedArrayList2: " + paginatedArrayList.stream()
                .map(p -> Tuple.of(p.getPersona().get(PersonaAPI.KEY_TAG_FIELD), p.getPersona().get("personalized"))).collect(Collectors.toList()));

        assertTrue(paginatedArrayList.stream().anyMatch(personalizationPersonaPageView ->
                personalizationPersonaPageView.getPersona().get(PersonaAPI.KEY_TAG_FIELD).equals(personaTag) &&
                        Boolean.TRUE.equals(personalizationPersonaPageView.getPersona().get("personalized"))));
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

        final String path = String.format("//%s/%s/%s", hostName, folderName, pageName);
        final SearchResponse searchResponse = mock(SearchResponse.class);

        final List contentlets = list(pageAsset);
        final ESSearchResults results = new ESSearchResults(searchResponse, contentlets);
        String preparedPagePath = String.format("%s/%s",folderName,pageName).replace("/", "\\\\/");
        final String query = String.format("{"
                + "query: {"
                + "query_string: {"
                + "query: \"+basetype:5 +path:*%s* +conhostName:%s languageid:1^10\""
                + "}"
                + "}"
                + "}", preparedPagePath, host.getHostname());

        when(esapi.esSearch(query, false, user, false)).thenReturn(results);

        final Response response = pageResource.searchPage(request,  new EmptyHttpResponse(), path, false, true);
        RestUtilTest.verifySuccessResponse(response);

        final Collection contentLetsResponse = (Collection) ((ResponseEntityView) response.getEntity()).getEntity();
        assertEquals(contentLetsResponse.size(), 1);

        final Map responseMap = (Map) contentLetsResponse.iterator().next();
        assertEquals(responseMap.get("identifier"), pageAsset.getIdentifier());
        assertEquals(responseMap.get("inode"), pageAsset.getInode());
    }

    /**
     * Should return the page without content
     *
     * @throws JSONException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testRender() throws DotDataException, DotSecurityException {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTest pageRenderTest = PageRenderUtilTest.createPage(2, host);
        final HTMLPageAsset page = pageRenderTest.getPage();

        final Response response = pageResource
                .render(request, this.response, page.getURI(), null, null,
                        String.valueOf(languageId), null);
        RestUtilTest.verifySuccessResponse(response);

        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        PageRenderVerifier.verifyPageView(pageView, pageRenderTest, user);
    }

    /**
     * Should remove visitors and persona from session
     *
     */
    @Test
    public void testRemoveVisitorAndPersona() throws DotDataException, DotSecurityException {
        final String personaId = "1";
        final String languageId = "1";
        final String deviceInode = "3";

        final String mode = "PREVIEW_MODE";
        pageResource.render(request, response, pagePath, mode, personaId, languageId, deviceInode);
        pageResource.render(request, response, pagePath, null, null, languageId, null);

        verify(session).removeAttribute(WebKeys.CURRENT_DEVICE);
        verify(session).removeAttribute(WebKeys.VISITOR);
    }

    /**
     * Should return haveContent equals to true for a page with MultiTree linked
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testRenderWithContent() throws DotDataException, DotSecurityException {

        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTest pageRenderTest = PageRenderUtilTest.createPage(1, host);
        final HTMLPageAsset page = pageRenderTest.getPage();

        final Container container = pageRenderTest.getFirstContainer();
        pageRenderTest.createContent(container);

        final Response response = pageResource
                .render(request, this.response, page.getURI(), null, null,
                        String.valueOf(languageId), null);
        RestUtilTest.verifySuccessResponse(response);

        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        PageRenderVerifier.verifyPageView(pageView, pageRenderTest, user);
    }

    /**
     * Should render page for limited user
     *
     * @throws JSONException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testRenderLimitedUser() throws DotDataException, DotSecurityException {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTest pageRenderTest = PageRenderUtilTest.createPage(2, host);
        final HTMLPageAsset page = pageRenderTest.getPage();

        final User limitedUser = createLimitedUser(page);

        final Response response = pageResource
                .render(request, this.response, page.getURI(), null, null,
                        String.valueOf(languageId), null);
        RestUtilTest.verifySuccessResponse(response);

        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        PageRenderVerifier.verifyPageView(pageView, pageRenderTest, limitedUser);
    }


    /**
     * Should return 403
     *
     * @throws JSONException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test(expected = DotSecurityException.class)
    public void testNotPermissionUser() throws DotDataException, DotSecurityException {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTest pageRenderTest = PageRenderUtilTest.createPage(2, host);
        final HTMLPageAsset page = pageRenderTest.getPage();

        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen()
                .roles(role)
                .nextPersisted();

        when(initDataObject.getUser()).thenReturn(user);

        APILocator.getPermissionAPI().save(
                new Permission(host.getPermissionId(),
                        role.getId(),
                        PermissionAPI.PERMISSION_READ),
                host, APILocator.systemUser(), false);

        pageResource.render(request, this.response, page.getURI(), null, null,
                        String.valueOf(languageId), null);
    }

    /**
     * Should return page with content in EDIT_MODE
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testRenderInEditMode() throws DotDataException, DotSecurityException {

        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTest pageRenderTest = PageRenderUtilTest.createPage(1, host);
        final HTMLPageAsset page = pageRenderTest.getPage();

        final Container container = pageRenderTest.getFirstContainer();
        pageRenderTest.createContent(container);

        final Response response = pageResource
                .render(request, this.response, page.getURI(), PageMode.EDIT_MODE.toString(), null,
                        String.valueOf(languageId), null);
        RestUtilTest.verifySuccessResponse(response);


        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        PageRenderVerifier.verifyPageView(pageView, pageRenderTest, user);

        assertEquals(PageMode.EDIT_MODE, pageView.getViewAs().getPageMode());
    }

    /**
     * Should thorow DotSecurityException  when request a page in EDIT_MODE with a limited user
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test(expected = DotSecurityException.class)
    public void testRenderInEditModeLimitedUser() throws DotDataException, DotSecurityException {

        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTest pageRenderTest = PageRenderUtilTest.createPage(1, host);
        final HTMLPageAsset page = pageRenderTest.getPage();

        final Container container = pageRenderTest.getFirstContainer();
        pageRenderTest.createContent(container);

        createLimitedUser(page);

        pageResource.render(request, this.response, page.getURI(), PageMode.EDIT_MODE.toString(),
                null, String.valueOf(languageId), null);
    }

    /**
     * Should return the page in another language
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testRenderInAnotherLanguage() throws DotDataException, DotSecurityException {

        final String languageId = "2";

        final PageRenderTest pageRenderTest = PageRenderUtilTest.createPage(1, host);
        final HTMLPageAsset page = pageRenderTest.getPage();

        final Container container = pageRenderTest.getFirstContainer();
        pageRenderTest.createContent(container);

        final Response response = pageResource
                .render(request, this.response, page.getURI(), null, null,
                        languageId, null);
        RestUtilTest.verifySuccessResponse(response);

        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        PageRenderVerifier.verifyPageView(pageView, pageRenderTest, user);
    }

    /***
     * Should return page for default persona
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testRenderNotPersonalizationVersion() throws DotDataException, DotSecurityException {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTest pageRenderTest = PageRenderUtilTest.createPage(2, host);
        final HTMLPageAsset page = pageRenderTest.getPage();

        final ContentType contentTypePersona = APILocator.getContentTypeAPI(APILocator.systemUser()).find("persona");
        final Contentlet persona = new ContentletDataGen(contentTypePersona.id())
                .setProperty("name", "name")
                .setProperty("keyTag", "keyTag")
                .host(host)
                .languageId(1)
                .nextPersisted();

        when(initDataObject.getUser()).thenReturn(APILocator.systemUser());

        final Response response = pageResource
                .render(request, this.response, page.getURI(), null, persona.getIdentifier(),
                        String.valueOf(languageId), null);

        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        PageRenderVerifier.verifyPageView(pageView, pageRenderTest, APILocator.systemUser());

        assertNull(pageView.getViewAs().getPersona());
    }

    /***
     * Should return page for a persona
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testRenderPersonalizationVersion() throws DotDataException, DotSecurityException {
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTest pageRenderTest = PageRenderUtilTest.createPage(2, host);
        final HTMLPageAsset page = pageRenderTest.getPage();

        final ContentType contentTypePersona = APILocator.getContentTypeAPI(APILocator.systemUser()).find("persona");
        final Contentlet persona = new ContentletDataGen(contentTypePersona.id())
                .setProperty("name", "name")
                .setProperty("keyTag", "keyTag")
                .host(host)
                .languageId(1)
                .nextPersisted();

        final Container container = pageRenderTest.getFirstContainer();
        pageRenderTest.createContent(container);

        APILocator.getMultiTreeAPI().copyPersonalizationForPage(
                page.getIdentifier(),
                Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + persona.getIdentifier()
            );

        final Response response = pageResource
                .render(request, this.response, page.getURI(), null, persona.getIdentifier(),
                        String.valueOf(languageId), null);

        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        PageRenderVerifier.verifyPageView(pageView, pageRenderTest, user);

        assertNull(pageView.getViewAs().getPersona());
    }

    private User createLimitedUser(final HTMLPageAsset page) throws DotDataException, DotSecurityException {
        final Role role = new RoleDataGen().nextPersisted();
        final User user = new UserDataGen()
                .roles(role)
                .nextPersisted();

        when(initDataObject.getUser()).thenReturn(user);

        APILocator.getPermissionAPI().save(
                new Permission(page.getPermissionId(),
                        role.getId(),
                        PermissionAPI.PERMISSION_READ),
                page, APILocator.systemUser(), false);

        APILocator.getPermissionAPI().save(
                new Permission(host.getPermissionId(),
                        role.getId(),
                        PermissionAPI.PERMISSION_READ),
                host, APILocator.systemUser(), false);

        return user;
    }
}
