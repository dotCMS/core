package com.dotcms.rest.api.v1.page;

import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRendered;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfig;
import com.dotcms.rest.*;
import com.dotcms.rest.api.v1.personalization.PersonalizationPersonaPageView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.*;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import java.util.*;
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
     * Should return at least one persona personalized
     *
     * @throws JSONException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testGetPersonalizedPersonasOnPage()
            throws Exception {


        final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
        final String  htmlPage            = UUIDGenerator.generateUuid();

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        final ContentType contentGenericType = contentTypeAPI.find("webPageContent");

        final PageRenderTestUtil.PageRenderTest pageRenderTest = PageRenderTestUtil.createPage(1, host);
        final HTMLPageAsset pagetest = pageRenderTest.getPage();
        final Container container = pageRenderTest.getFirstContainer();
        //Create Contentlet in English
        final Contentlet content = new ContentletDataGen(contentGenericType.id())
                .languageId(1)
                .folder(APILocator.getFolderAPI().findSystemFolder())
                .host(host)
                .setProperty("title", "content1")
                .setProperty("body", "content1")
                .nextPersisted();

        final Persona persona    = new PersonaDataGen().keyTag("persona"+System.currentTimeMillis()).hostFolder(host.getIdentifier()).nextPersisted();
        final String personaTag  = persona.getKeyTag();
        final String personalization = Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + personaTag;

        multiTreeAPI.saveMultiTree(new MultiTree(pagetest.getIdentifier(), container.getIdentifier(), content.getIdentifier(), UUIDGenerator.generateUuid(), 1)); // dot:default
        multiTreeAPI.saveMultiTree(new MultiTree(pagetest.getIdentifier(), container.getIdentifier(), content.getIdentifier(), UUIDGenerator.generateUuid(), 2, personalization)); // dot:somepersona


        
        
        
        persona.setIndexPolicy(IndexPolicy.WAIT_FOR);
        APILocator.getContentletAPI().publish(persona, user, false);

        when(request.getRequestURI()).thenReturn(pagetest.getURI());
        final Response response = pageResource.getPersonalizedPersonasOnPage(request, new EmptyHttpResponse(),
                null, 0, 10, "title", "ASC", host.getIdentifier(), pagetest.getIdentifier(), null);

        final ResponseEntityView entityView = (ResponseEntityView) response.getEntity();
        assertNotNull(entityView);
        System.out.println(entityView);
        final PaginatedArrayList<PersonalizationPersonaPageView> paginatedArrayList = (PaginatedArrayList) entityView.getEntity();
        assertNotNull(paginatedArrayList);

        Logger.info(this, "************ htmlPage: " + htmlPage + ", container: " + container + ", personaTag: " + personaTag);
        Logger.info(this, "************ PaginatedArrayList1: " + paginatedArrayList);
        Logger.info(this, "************ PaginatedArrayList2: " + paginatedArrayList.stream()
                .map(p -> Tuple.of(p.getPersona().get(PersonaAPI.KEY_TAG_FIELD), p.getPersona().get("personalized"))).collect(Collectors.toList()));

        paginatedArrayList.stream()
                .anyMatch(personalizationPersonaPageView ->
                        personalizationPersonaPageView.getPersona().get(PersonaAPI.KEY_TAG_FIELD).equals(personaTag) &&
                                Boolean.TRUE.equals(personalizationPersonaPageView.getPersona().get("personalized")));
    }

    /**
     * methodToTest {@link PageResource#getPersonalizedPersonasOnPage(HttpServletRequest, HttpServletResponse, String, int, int, String, String, String, String, Boolean)}
     * Given Scenario: Resuqest persona list with a limited user
     * ExpectedResult: Should respect front end roles when the parameter is set to true and not respect it when the
     * parameter is set to false
     */
    @Test
    public void getPersonasForLimitedUser() throws DotDataException, DotSecurityException, PortalException, SystemException {
        final Persona persona    = new PersonaDataGen()
                .keyTag("persona"+System.currentTimeMillis())
                .hostFolder(host.getIdentifier())
                .host(host)
                .nextPersisted();
        persona.setIndexPolicy(IndexPolicy.WAIT_FOR);

        APILocator.getContentletAPI().publish(persona, user, false);


        final User user = new UserDataGen().nextPersisted();
        when(initDataObject.getUser()).thenReturn(user);

        final PageRenderTestUtil.PageRenderTest pageRenderTest = PageRenderTestUtil.createPage(1, host);
        final HTMLPageAsset pagetest = pageRenderTest.getPage();

        when(request.getRequestURI()).thenReturn(pagetest.getURI());

        Response response = pageResource.getPersonalizedPersonasOnPage(request, new EmptyHttpResponse(),
                null, 0, 10, "title", "ASC", host.getIdentifier(),
                pagetest.getIdentifier(), true);

        List<PersonalizationPersonaPageView> personas = (List<PersonalizationPersonaPageView> ) ((ResponseEntityView) response.getEntity()).getEntity();
        assertTrue(personas.size() > 1);
        assertEquals("dot:persona", ((PersonalizationPersonaPageView) personas.get(0)).getPersona().get("keyTag"));
        personas.stream()
                .anyMatch(personalizationPersonaPageView ->
                        personalizationPersonaPageView.getPersona().get(PersonaAPI.KEY_TAG_FIELD).equals(persona.getKeyTag())
                );

        response = pageResource.getPersonalizedPersonasOnPage(request, new EmptyHttpResponse(),
                null, 0, 10, "title", "ASC", host.getIdentifier(),
                pagetest.getIdentifier(), false);

         personas = (List<PersonalizationPersonaPageView> ) ((ResponseEntityView) response.getEntity()).getEntity();
         assertEquals(1, personas.size());
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
        final String path = pagePath;

        final SearchResponse searchResponse = mock(SearchResponse.class);

        final Contentlet contentlet = pageAsset;

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
     * Should return about-us/index page
     *
     * @throws JSONException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testRender() throws DotDataException, DotSecurityException {

        final String pageUri =  pagePath;
        final long languageId = 1;

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final HTMLPageAsset pageByPath = (HTMLPageAsset) APILocator.getHTMLPageAssetAPI().getPageByPath(pageUri, host, languageId, false);

        final Structure structure1 = new StructureDataGen().nextPersisted();
        final Container localContainer1 = new ContainerDataGen().withStructure(structure1,"").friendlyName("container-1-friendly-name").title("container-1-title").nextPersisted();
        final Structure structure2 = new StructureDataGen().nextPersisted();
        final Container localContainer2 = new ContainerDataGen().withStructure(structure2,"").friendlyName("container-2-friendly-name").title("container-2-title").nextPersisted();
        final Template newTemplate = new TemplateDataGen().withContainer(localContainer1.getIdentifier()).withContainer(localContainer2.getIdentifier()).nextPersisted();
        APILocator.getVersionableAPI().setWorking(newTemplate);
        APILocator.getVersionableAPI().setLive(newTemplate);

        final Contentlet checkout = APILocator.getContentletAPIImpl().checkout(pageByPath.getInode(), systemUser, false);
        checkout.setStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, newTemplate.getIdentifier());

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

        assertEquals(pageView.getNumberContents(), 0);
        assertTrue(containerIds.contains(localContainer1.getIdentifier()));
        assertTrue(containerIds.contains(localContainer1.getIdentifier()));
        assertFalse(containerIds.contains(container1.getIdentifier()));
        assertFalse(containerIds.contains(container2.getIdentifier()));

        for (final ContainerRaw pageContainer : pageContainers) {
            final Map<String, List<Map<String, Object>>> contentlets = pageContainer.getContentlets();
            final Container container = pageContainer.getContainer();
            final List<Structure> structures = APILocator.getContainerAPI().getStructuresInContainer(container);
            if(container.getIdentifier().equals(localContainer1.getIdentifier())){
                assertEquals(localContainer1.getTitle(),container.getTitle());
                assertEquals(structures.size(),1);
            } else if(container.getIdentifier().equals(localContainer2.getIdentifier())){
                assertEquals(localContainer2.getTitle(),container.getTitle());
                assertEquals(structures.size(),1);
            } else {
                fail("Unknown container with id "+container.getIdentifier());
            }
            assertEquals(contentlets.size(), 1);
        }

    }

    /**
     * Should return haveContent equals to true for a page with MultiTree linked
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testRenderWithContent() throws DotDataException, DotSecurityException {

        final User systemUser = APILocator.getUserAPI().getSystemUser();

        final Structure structure = new StructureDataGen().nextPersisted();
        final Container localContainer = new ContainerDataGen().withStructure(structure,"").friendlyName("container-1-friendly-name").title("container-1-title").nextPersisted();

        final TemplateLayout templateLayout = TemplateLayoutDataGen.get()
                .withContainer(localContainer.getIdentifier())
                .next();

        final Template newTemplate = new TemplateDataGen()
                .drawedBody(templateLayout)
                .withContainer(localContainer.getIdentifier()
                ).nextPersisted();
        APILocator.getVersionableAPI().setWorking(newTemplate);
        APILocator.getVersionableAPI().setLive(newTemplate);

        final Contentlet checkout = APILocator.getContentletAPIImpl().checkout(pageAsset.getInode(), systemUser, false);
        checkout.setStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, newTemplate.getIdentifier());

        APILocator.getContentletAPIImpl().checkin(checkout, systemUser, false);

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        final ContentType contentGenericType = contentTypeAPI.find("webPageContent");

        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentGenericType.id());
        final Contentlet contentlet = contentletDataGen.setProperty("title", "title")
                .setProperty("body", "body").languageId(1).nextPersisted();


        final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
        final MultiTree multiTree = new MultiTree(pageAsset.getIdentifier(), localContainer.getIdentifier(), contentlet.getIdentifier(), "1", 1);
        multiTreeAPI.saveMultiTree(multiTree);

        final Response response = pageResource
                .loadJson(request, this.response, pagePath, "PREVIEW_MODE", null,
                        "1", null);

        RestUtilTest.verifySuccessResponse(response);

        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        assertEquals(pageView.getNumberContents(), 1);
    }

    @Test
    public void shouldReturnPageByURLPattern() throws DotDataException, DotSecurityException, InterruptedException {
        final String baseUrl = String.format("/test%s", System.currentTimeMillis());

        final User systemUser = APILocator.getUserAPI().getSystemUser();

        final ContentType contentType = new ContentTypeDataGen().user(systemUser)
                .host(host)
                .detailPage(pageAsset.getIdentifier())
                .urlMapPattern(String.format("%s/{text}", baseUrl))
                .nextPersisted();

        new FieldDataGen()
                .name("text")
                .velocityVarName("text")
                .type(TextField.class)
                .contentTypeId(contentType.id())
                .nextPersisted();


        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
        contentletDataGen
                .setProperty("text", "text")
                .languageId(1)
                .nextPersisted();

        Thread.sleep(500);

        final Response response = pageResource
                .render(request, this.response, String.format("%s/text", baseUrl), "PREVIEW_MODE", null,
                        "1", null);

        RestUtilTest.verifySuccessResponse(response);
    }


    /**
     * methodToTest {@link PageResource#render(HttpServletRequest, HttpServletResponse, String, String, String, String, String)}
     * Given Scenario: Create a page with URL Pattern, with a no publish content, and try to get it in ADMIN_MODE
     * ExpectedResult: Should return a 404 HTTP error
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test(expected = HTMLPageAssetNotFoundException.class)
    public void shouldReturn404ForPageWithURLPatternWithNotLIVEContentInAdminMode() throws DotDataException, DotSecurityException, InterruptedException {
        final String baseUrl = String.format("/test%s", System.currentTimeMillis());

        final User systemUser = APILocator.getUserAPI().getSystemUser();

        final ContentType contentType = new ContentTypeDataGen().user(systemUser)
                .host(host)
                .detailPage(pageAsset.getIdentifier())
                .urlMapPattern(String.format("%s/{text}", baseUrl))
                .nextPersisted();

        new FieldDataGen()
                .name("text")
                .velocityVarName("text")
                .type(TextField.class)
                .contentTypeId(contentType.id())
                .nextPersisted();


        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
        contentletDataGen
                .setProperty("text", "text")
                .languageId(1)
                .nextPersisted();

        Thread.sleep(500);

        pageResource
                .render(request, this.response, String.format("%s/text", baseUrl), PageMode.ADMIN_MODE.toString(), null,
                        "1", null);
    }

    /**
     * methodToTest {@link PageResource#render(HttpServletRequest, HttpServletResponse, String, String, String, String, String)}
     * Given Scenario: Create a page with URL Pattern, with a no publish content, and try to get it in ADMIN_MODE
     * ExpectedResult: Should return a 404 HTTP error
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test(expected = HTMLPageAssetNotFoundException.class)
    public void shouldReturn404ForPageWithURLPatternWithNotLIVEContentInLiveMode() throws DotDataException, DotSecurityException, InterruptedException {
        final String baseUrl = String.format("/test%s", System.currentTimeMillis());

        final User systemUser = APILocator.getUserAPI().getSystemUser();

        final ContentType contentType = new ContentTypeDataGen().user(systemUser)
                .host(host)
                .detailPage(pageAsset.getIdentifier())
                .urlMapPattern(String.format("%s/{text}", baseUrl))
                .nextPersisted();

        new FieldDataGen()
                .name("text")
                .velocityVarName("text")
                .type(TextField.class)
                .contentTypeId(contentType.id())
                .nextPersisted();


        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
        contentletDataGen
                .setProperty("text", "text")
                .languageId(1)
                .nextPersisted();

        Thread.sleep(500);

        pageResource
                .render(request, this.response, String.format("%s/text", baseUrl), PageMode.LIVE.toString(), null,
                        "1", null);
    }

    /**
     * Should return about-us/index page
     *
     * @throws JSONException
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void testNumberContentWithNotDrawTemplate() throws DotDataException, DotSecurityException {

        final String pageUri =  pagePath;
        final long languageId = 1;

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final HTMLPageAsset pageByPath = (HTMLPageAsset) APILocator.getHTMLPageAssetAPI().getPageByPath(pageUri, host, languageId, false);

        final Structure structure1 = new StructureDataGen().nextPersisted();
        final Container localContainer1 = new ContainerDataGen()
                .withStructure(structure1,"")
                .friendlyName("container-1-friendly-name")
                .title("container-1-title")
                .nextPersisted();

        final Structure structure2 = new StructureDataGen().nextPersisted();
        final Container localContainer2 = new ContainerDataGen()
                .withStructure(structure2,"")
                .friendlyName("container-2-friendly-name")
                .title("container-2-title")
                .nextPersisted();

        final Template newTemplate = new TemplateDataGen()
                .withContainer(localContainer1.getIdentifier())
                .withContainer(localContainer2.getIdentifier())
                .nextPersisted();

        APILocator.getVersionableAPI().setWorking(newTemplate);
        APILocator.getVersionableAPI().setLive(newTemplate);

        final Contentlet checkout = APILocator.getContentletAPIImpl().checkout(pageByPath.getInode(), systemUser, false);
        checkout.setStringProperty(HTMLPageAssetAPI.TEMPLATE_FIELD, newTemplate.getIdentifier());

        final Contentlet checkin = APILocator.getContentletAPIImpl().checkin(checkout, systemUser, false);

        final Contentlet contentlet =  TestDataUtils.getGenericContentContent(true, 1);

        final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
        final MultiTree multiTree = new MultiTree(pageAsset.getIdentifier(), localContainer1.getIdentifier(), contentlet.getIdentifier(), "1", 1);
        multiTreeAPI.saveMultiTree(multiTree);

        final Response response = pageResource
                .loadJson(request, this.response, pageUri, null, null,
                        String.valueOf(languageId), null);

        RestUtilTest.verifySuccessResponse(response);

        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        assertEquals(pageView.getNumberContents(), 1);

    }

    /***
     * Should return page for default persona
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testRenderNotPersonalizationVersion() throws DotDataException, DotSecurityException {
        final String modeParam = "PREVIEW_MODE";
        when(request.getAttribute(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.get(modeParam));
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(APILocator.systemUser());

        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTestUtil.PageRenderTest pageRenderTest = PageRenderTestUtil.createPage(2, host);
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
                .render(request, this.response, page.getURI(), modeParam, persona.getIdentifier(),
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

        final PageRenderTestUtil.PageRenderTest pageRenderTest = PageRenderTestUtil.createPage(2, host);
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

    /***
     * methodToTest {@link PageResource#render(HttpServletRequest, HttpServletResponse, String, String, String, String, String)}
     * Given Scenario: Create a page with two containers and a content in each of then
     * ExpectedResult: Should render the containers with the contents, the check it look into the render code the
     * content div <pre>assertTrue(code.indexOf("data-dot-object=\"contentlet\"") != -1)</pre>
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testShouldRenderContainers() throws DotDataException, DotSecurityException, InterruptedException {
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(APILocator.systemUser());

        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTestUtil.PageRenderTest pageRenderTest = PageRenderTestUtil.createPage(2, host);
        final HTMLPageAsset page = pageRenderTest.getPage();

        when(initDataObject.getUser()).thenReturn(APILocator.systemUser());

        final Collection<String> containersId = pageRenderTest.getContainersId();

        for (final String id : containersId) {
            final Container container = pageRenderTest.getContainer(id);
            pageRenderTest.createContent(container);
        }

        final Response response = pageResource
                .render(request, this.response, page.getURI(), "EDIT_MODE", null,
                        String.valueOf(languageId), null);

        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        PageRenderVerifier.verifyPageView(pageView, pageRenderTest, APILocator.systemUser());

        final Collection<? extends ContainerRendered> containers = (Collection<ContainerRendered>) pageView.getContainers();
        assertTrue(containers.size() > 0);

        for (ContainerRendered container : containers) {
            final Map<String, String> rendered = container.getRendered();
            final Collection<String> codes = rendered.values();
            assertTrue(codes.size() > 0);

            for (final String code : codes) {
                assertTrue(code.indexOf("data-dot-object=\"container\"") != -1);
                assertTrue(code.indexOf("data-dot-object=\"contentlet\"") != -1);
            }
        }
        assertNull(pageView.getViewAs().getPersona());
    }


    /**
     * methodToTest {@link PageResource#render(HttpServletRequest, HttpServletResponse, String, String, String, String, String)}
     * Given Scenario: Create a page with not LIVE version, then publish the page, and then update the page to crate a
     * new working version
     * ExpectedResult: Should return a LIVE attribute to true just in after the page is publish
     */

    @Test()
    public void shouldReturnLIVE() throws DotDataException, DotSecurityException {
        when(request.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.PREVIEW_MODE.toString());
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(APILocator.systemUser());

        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTestUtil.PageRenderTest pageRenderTest = PageRenderTestUtil.createPage(2, host, false);
        HTMLPageAsset page = pageRenderTest.getPage();

        when(initDataObject.getUser()).thenReturn(APILocator.systemUser());

        Response response = pageResource
                .render(request, this.response, page.getURI(), PageMode.PREVIEW_MODE.toString(), null,
                        String.valueOf(languageId), null);

        PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        assertFalse(pageView.isLive());


        //Publish the page
        APILocator.getContentletAPI().publish(page, user, false);

        response = pageResource
                .render(request, this.response, page.getURI(), PageMode.PREVIEW_MODE.toString(), null,
                        String.valueOf(languageId), null);

        pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        assertTrue(pageView.isLive());

        //Create a new working version
        final Contentlet checkout = APILocator.getContentletAPI().checkout(page.getInode(), user, false);
        APILocator.getContentletAPI().checkin(checkout, user, false);

        response = pageResource
                .render(request, this.response, page.getURI(), PageMode.PREVIEW_MODE.toString(), null,
                        String.valueOf(languageId), null);

        pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        assertTrue(pageView.isLive());
    }
}
