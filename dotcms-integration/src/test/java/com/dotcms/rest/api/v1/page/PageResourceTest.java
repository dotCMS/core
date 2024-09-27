package com.dotcms.rest.api.v1.page;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContainerAsFileDataGen;
import com.dotcms.datagen.ContainerDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.PersonaDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.StructureDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TemplateLayoutDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.rendering.velocity.viewtools.content.util.ContentUtils;
import com.dotcms.repackage.org.apache.struts.config.ModuleConfig;
import com.dotcms.rest.EmptyHttpResponse;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.RestUtilTest;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.api.v1.personalization.PersonalizationPersonaPageView;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.Clickstream;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.VersionableAPI;
import com.dotmarketing.business.web.HostWebAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.HTMLPageAssetRendered;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.business.PersonaAPI;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.PaginatedContentList;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.util.json.JSONException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import org.elasticsearch.action.search.SearchResponse;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

/**
 * {@link PageResource} test
 */
public class PageResourceTest {
    private ContentletAPI esapi;
    private PageResource pageResource;
    private PageResource pageResourceWithHelper;
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
    private ContentType contentGenericType;
    private HostWebAPI hostWebAPI;

    private final Map<String, Object> sessionAttributes = new ConcurrentHashMap<>(
            Map.of("clickstream",new Clickstream())
    );

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Before
    public void init()
            throws DotSecurityException, DotDataException, SystemException, PortalException {

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
        when(pageResourceHelper.decorateRequest(request)).thenReturn(request);

        when(webResource.init(nullable(String.class), any(HttpServletRequest.class), any(HttpServletResponse.class), any(Boolean.class), nullable(String.class))).thenReturn(initDataObject);
        when(webResource.init(any(HttpServletRequest.class), any(HttpServletResponse.class), any(Boolean.class))).thenReturn(initDataObject);
        when(webResource.init(false, request, true)).thenReturn(initDataObject);
        when(webResource.init(any(WebResource.InitBuilder.class))).thenReturn(initDataObject);
        when(initDataObject.getUser()).thenReturn(user);
        host = new SiteDataGen().name(hostName).nextPersisted();
        hostWebAPI = mock(HostWebAPI.class);
        when(hostWebAPI.getCurrentHost(request, user)).thenReturn(host);
        when(hostWebAPI.getCurrentHost(request)).thenReturn(host);
        when(hostWebAPI.getCurrentHost()).thenReturn(host);
        when(hostWebAPI.getCurrentHostNoThrow(any(HttpServletRequest.class))).thenReturn(host);
        when(hostWebAPI.findDefaultHost(any(User.class),anyBoolean())).thenReturn(host);

        pageResource = new PageResource(pageResourceHelper, webResource, htmlPageAssetRenderedAPI, esapi, hostWebAPI);
        this.pageResourceWithHelper = new PageResource(PageResourceHelper.getInstance(), webResource, htmlPageAssetRenderedAPI, this.esapi, hostWebAPI);

        when(request.getRequestURI()).thenReturn("/test");
        when(request.getSession()).thenReturn(session);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);

        //Set up the behavior of setAttribute to store values in the map
        doAnswer(invocation -> {
            final String key = invocation.getArgument(0);
            final Object value = invocation.getArgument(1);
            sessionAttributes.put(key, value);
            return null;
        }).when(session).setAttribute(anyString(), any());

        // Set up the behavior of getAttribute to retrieve values from the map
        when(session.getAttribute(anyString())).thenAnswer(invocation -> sessionAttributes.get(invocation.getArgument(0)));

        //Set up the behavior of removeAttribute to remove values from the map
        doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            sessionAttributes.remove(key);
            return null;
        }).when(session).removeAttribute(anyString());

        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();

        APILocator.getVersionableAPI().setWorking(host);
        APILocator.getVersionableAPI().setLive(host);
        when(request.getParameter("host_id")).thenReturn(host.getIdentifier());
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(host);
        when(request.getAttribute("com.dotcms.repackage.org.apache.struts.action.MODULE")).thenReturn(moduleConfig);
        // Mock setAttribute
        doAnswer((InvocationOnMock invocation)-> {

                String key = invocation.getArgument(0, String.class);
                Object value = invocation.getArgument(1, Object.class);
                if (null != key && null != value) {
                    attributes.put(key, value);
                }
                return null;
        }).when(request).setAttribute(anyString(), Mockito.any());


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

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        contentGenericType = contentTypeAPI.find("webPageContent");

        final ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(contentGenericType.id());
        cs.setCode("$!{body}");
        container1 = APILocator.getContainerAPI().save(container1, list(cs), host, APILocator.systemUser(), false);
    }

    /**
     * Method to test: PageResource#addContent
     * Given Scenario: Add invalid contentlet (by invalid content type)
     * ExpectedResult: Since the contentlet has a content type that is not part of the types allowed on the container, expected the Bad request
     *
     */
    @Test (expected = com.dotcms.rest.exception.BadRequestException.class)
    public void test_addContent_invalid_content_type() throws Exception {

        final PageRenderTestUtil.PageRenderTest pageRenderTest = PageRenderTestUtil.createPage(1, host);
        final HTMLPageAsset pagetest = pageRenderTest.getPage();
        final Container container = pageRenderTest.getFirstContainer();

        final ContentType bannerLikeContentType = TestDataUtils.getBannerLikeContentType();
        final Contentlet contentlet = TestDataUtils.getBannerLikeContent(true, 1, bannerLikeContentType.id(),
                host);
        final List<PageContainerForm.ContainerEntry> entries = new ArrayList<>();
        final String requestJson = null;
        final PageContainerForm.ContainerEntry containerEntry =
            new PageContainerForm.ContainerEntry(null, container.getIdentifier(), "1");
        containerEntry.addContentId(contentlet.getIdentifier());
        entries.add(containerEntry);
        final PageContainerForm pageContainerForm = new PageContainerForm(entries, requestJson);
        this.pageResource.addContent(request, response, pagetest.getIdentifier(), VariantAPI.DEFAULT_VARIANT.name(), pageContainerForm);
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
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
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
                        String.valueOf(languageId), null, null);

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
            final Map<String, List<Contentlet>> contentlets = pageContainer.getContentlets();
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
        final long languageId = 1l;

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
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT).languageId(languageId).nextPersisted();


        final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
        final MultiTree multiTree = new MultiTree(pageAsset.getIdentifier(), localContainer.getIdentifier(), contentlet.getIdentifier(), "1", 1);
        multiTreeAPI.saveMultiTree(multiTree);

        when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn(String.valueOf(languageId));

        final Response response = pageResource
                .loadJson(request, this.response, pagePath, "PREVIEW_MODE", null,
                        "1", null, null);

        RestUtilTest.verifySuccessResponse(response);

        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        assertEquals(pageView.getNumberContents(), 1);
    }

    @Test
    public void shouldReturnPageByURLPattern()
            throws DotDataException, DotSecurityException, InterruptedException, SystemException, PortalException {

        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

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
                .host(host)
                .nextPersisted();

        Thread.sleep(500);

        final Response response = pageResource
                .render(request, this.response, String.format("%s/text", baseUrl), "PREVIEW_MODE", null,
                        "1", null, null);

        RestUtilTest.verifySuccessResponse(response);
    }


    /**
     * methodToTest {@link PageResource#render(HttpServletRequest, HttpServletResponse, String, String, String, String, String, String)}
     * Given Scenario: Create a page with URL Pattern, with a no publish content, and try to get it in ADMIN_MODE
     * ExpectedResult: Should return a 404 HTTP error
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test(expected = HTMLPageAssetNotFoundException.class)
    public void shouldReturn404ForPageWithURLPatternWithNotLIVEContentInAdminMode()
            throws DotDataException, DotSecurityException, InterruptedException, SystemException, PortalException {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

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
                        "1", null, null);
    }

    /**
     * methodToTest {@link PageResource#render(HttpServletRequest, HttpServletResponse, String, String, String, String, String, String)}
     * Given Scenario: Create a page with URL Pattern, with a no publish content, and try to get it in ADMIN_MODE
     * ExpectedResult: Should return a 404 HTTP error
     *
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws InterruptedException
     */
    @Test(expected = HTMLPageAssetNotFoundException.class)
    public void shouldReturn404ForPageWithURLPatternWithNotLIVEContentInLiveMode()
            throws DotDataException, DotSecurityException, InterruptedException, SystemException, PortalException {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

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
                        "1", null, null);
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

        when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn(String.valueOf(languageId));

        final Response response = pageResource
                .loadJson(request, this.response, pageUri, null, null,
                        String.valueOf(languageId), null, null);

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
    public void testRenderNotPersonalizationVersion()
            throws DotDataException, DotSecurityException, SystemException, PortalException {
        final String modeParam = "PREVIEW_MODE";
        when(request.getAttribute(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.get(modeParam));
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);

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
                        String.valueOf(languageId), null, null);

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
    public void testRenderPersonalizationVersion()
            throws DotDataException, DotSecurityException, SystemException, PortalException {

        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);

        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTestUtil.PageRenderTest pageRenderTest = PageRenderTestUtil.createPage(2, host);
        final HTMLPageAsset page = pageRenderTest.getPage();

        final ContentType contentTypePersona = APILocator.getContentTypeAPI(APILocator.systemUser()).find("persona");
        final Contentlet persona = new ContentletDataGen(contentTypePersona.id())
                .setProperty("name", "name")
                .setProperty("keyTag", "keyTag")
                .host(host)
                .languageId(languageId)
                .nextPersisted();

        final Container container = pageRenderTest.getFirstContainer();
        pageRenderTest.addContent(container);

        APILocator.getMultiTreeAPI().copyPersonalizationForPage(
                page.getIdentifier(),
                Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + persona.getIdentifier(),
                VariantAPI.DEFAULT_VARIANT.name()
        );

        when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn(String.valueOf(languageId));

        final Response response = pageResource
                .render(request, this.response, page.getURI(), null, persona.getIdentifier(),
                        String.valueOf(languageId), null, null);

        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        PageRenderVerifier.verifyPageView(pageView, pageRenderTest, user);

        assertNull(pageView.getViewAs().getPersona());
    }

    /***
     * methodToTest {@link PageResource#render(HttpServletRequest, HttpServletResponse, String, String, String, String, String, String)}
     * Given Scenario: Create a page with two containers and a content in each of then
     * ExpectedResult: Should render the containers with the contents, the check it look into the render code the
     * content div <pre>assertTrue(code.indexOf("data-dot-object=\"contentlet\"") != -1)</pre>
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testShouldRenderContainers()
            throws DotDataException, DotSecurityException, InterruptedException, SystemException, PortalException {
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(APILocator.systemUser());

        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTestUtil.PageRenderTest pageRenderTest = PageRenderTestUtil.createPage(2, host);
        final HTMLPageAsset page = pageRenderTest.getPage();

        when(initDataObject.getUser()).thenReturn(APILocator.systemUser());

        final Collection<String> containersId = pageRenderTest.getContainersId();

        for (final String id : containersId) {
            final Container container = pageRenderTest.getContainer(id);
            pageRenderTest.addContent(container);
        }

        when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn(String.valueOf(languageId));

        final Response response = pageResource
                .render(request, this.response, page.getURI(), "EDIT_MODE", null,
                        String.valueOf(languageId), null, null);

        final PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        PageRenderVerifier.verifyPageView(pageView, pageRenderTest, APILocator.systemUser());

        final Collection<? extends ContainerRendered> containers = (Collection<ContainerRendered>) pageView.getContainers();
        assertFalse(containers.isEmpty());

        for (ContainerRendered container : containers) {
            final Map<String, Object> rendered = container.getRendered();
            final Collection<Object> codes = rendered.values();
            assertFalse(codes.isEmpty());

            for (final Object code : codes) {
                assertTrue(code.toString().contains("data-dot-object=\"container\""));
                assertTrue(code.toString().contains("data-dot-object=\"contentlet\""));
            }
        }
        assertNull(pageView.getViewAs().getPersona());
    }


    /**
     * methodToTest {@link PageResource#render(HttpServletRequest, HttpServletResponse, String, String, String, String, String, String)}
     * Given Scenario: Create a page with not LIVE version, then publish the page, and then update the page to crate a
     * new working version
     * ExpectedResult: Should return a LIVE attribute to true just in after the page is publish
     */

    @Test()
    public void shouldReturnLIVE()
            throws DotDataException, DotSecurityException, SystemException, PortalException {
        when(request.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.PREVIEW_MODE.toString());
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(APILocator.systemUser());

        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        final PageRenderTestUtil.PageRenderTest pageRenderTest = PageRenderTestUtil.createPage(2, host, false);
        HTMLPageAsset page = pageRenderTest.getPage();

        when(initDataObject.getUser()).thenReturn(APILocator.systemUser());

        Response response = pageResource
                .render(request, this.response, page.getURI(), PageMode.PREVIEW_MODE.toString(), null,
                        String.valueOf(languageId), null, null);

        PageView pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        assertFalse(pageView.isLive());


        //Publish the page
        APILocator.getContentletAPI().publish(page, user, false);

        response = pageResource
                .render(request, this.response, page.getURI(), PageMode.PREVIEW_MODE.toString(), null,
                        String.valueOf(languageId), null, null);

        pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        assertTrue(pageView.isLive());

        //Create a new working version
        final Contentlet checkout = APILocator.getContentletAPI().checkout(page.getInode(), user, false);
        APILocator.getContentletAPI().checkin(checkout, user, false);

        response = pageResource
                .render(request, this.response, page.getURI(), PageMode.PREVIEW_MODE.toString(), null,
                        String.valueOf(languageId), null, null);

        pageView = (PageView) ((ResponseEntityView) response.getEntity()).getEntity();
        assertTrue(pageView.isLive());
    }


    /**
     * Method to test: {@link PageResource#saveLayout(HttpServletRequest, HttpServletResponse, PageForm)}
     * Given Scenario: Create a page with a content into a container add using #parseContainer, and save a new layout to this page
     * ExpectedResult: The content should not blows away (the UUID should kepp with the same value)
     *
     * @throws Exception
     */
    @Test
    public void shouldKeepTheAParserContainerContentAfterLayoutSaved()
            throws DotDataException, DotSecurityException, IOException, SystemException, PortalException {
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final User systemUser = APILocator.systemUser();
        final String modeParam = "PREVIEW_MODE";
        when(request.getAttribute(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.get(modeParam));
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(systemUser);

        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();
        when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn(String.valueOf(languageId));

        final String pageName = "testPage-"+System.currentTimeMillis();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(languageId)
                .pageURL(pageName)
                .title(pageName)
                .nextPersisted();

        page.setIndexPolicy(IndexPolicy.WAIT_FOR);
        page.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        page.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().publish(page, systemUser, false);

        when(initDataObject.getUser()).thenReturn(systemUser);

        final Contentlet contentlet1 = new ContentletDataGen(contentGenericType.id())
                .languageId(languageId)
                .folder(folder)
                .host(host)
                .setProperty("title", "content1")
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .nextPersisted();

        contentlet1.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet1.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet1.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().publish(contentlet1, systemUser, false);

        final MultiTree multiTree = new MultiTree(page.getIdentifier(), container1.getIdentifier(), contentlet1.getIdentifier(),"1",0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        final Response response = pageResource
                .render(request, this.response, page.getURI(), modeParam, null,
                        String.valueOf(languageId), null, null);

        final HTMLPageAssetRendered htmlPageAssetRendered = (HTMLPageAssetRendered) ((ResponseEntityView) response.getEntity()).getEntity();

        assertEquals("Rendered HTML Page is NOT the same as the expected one", "<div>" + TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT + "</div><div></div>", htmlPageAssetRendered.getHtml());

        final ObjectMapper MAPPER = new ObjectMapper();
        final String layoutString =
                "{" +
                    "\"layout\": {" +
                        "\"body\": {" +
                            "\"rows\": [" +
                                "{" +
                                    "\"columns\": [" +
                                        "{" +
                                            "\"leftOffset\": 1," +
                                            "\"width\": 12," +
                                            "\"containers\": [" +
                                                "{" +
                                                    "\"identifier\":\"" + container2.getIdentifier() + "\"," +
                                                    "\"uuid\": \"1\"" +
                                                "}" +
                                            "]" +
                                        "}" +
                                    "]" +
                                "}" +
                             "]" +
                          "}" +
                    "}" +
                "}";

        final PageForm.Builder builder = MAPPER.readValue(layoutString, PageForm.Builder.class);
        pageResource.saveLayout(request, this.response, builder.build());

        final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI().getMultiTrees(page.getIdentifier());

        assertEquals(1, multiTrees.size());
        assertEquals("1", multiTrees.get(0).getRelationType());
    }

    /**
     * Method to test: {@link PageResource#saveLayout(HttpServletRequest, HttpServletResponse, PageForm)}
     * Given Scenario: When a Template have a {@link FileAssetContainer}
     * ExpectedResult: Should response with the absolute path
     *
     * @throws Exception
     */
    @Test
    public void shouldResponseWith()
            throws DotDataException, DotSecurityException, IOException, SystemException, PortalException {
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final User systemUser = APILocator.systemUser();
        final String modeParam = "PREVIEW_MODE";
        when(request.getAttribute(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.get(modeParam));
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(systemUser);

        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();
        when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn(String.valueOf(languageId));
        when(request.getParameter("host_id")).thenReturn(host.getIdentifier());

        final String pageName = "testPage-"+System.currentTimeMillis();

        final String testContainer = "/test-get-container" + System.currentTimeMillis();
        Container container  = new ContainerAsFileDataGen().host(host).folderName(testContainer).nextPersisted();

        container = APILocator.getContainerAPI().find(container.getInode(), systemUser, true);

        final Template template = new TemplateDataGen()
                .withContainer(container.getIdentifier())
                .nextPersisted();

        final HTMLPageAsset page = new HTMLPageDataGen(folder, template)
                .languageId(languageId)
                .pageURL(pageName)
                .title(pageName)
                .nextPersisted();

        page.setIndexPolicy(IndexPolicy.WAIT_FOR);
        page.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        page.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().publish(page, systemUser, false);

        when(initDataObject.getUser()).thenReturn(systemUser);

        final Contentlet contentlet1 = new ContentletDataGen(contentGenericType.id())
                .languageId(languageId)
                .folder(folder)
                .host(host)
                .setProperty("title", "content1")
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .nextPersisted();

        contentlet1.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet1.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet1.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        APILocator.getContentletAPI().publish(contentlet1, systemUser, false);

        final MultiTree multiTree = new MultiTree(page.getIdentifier(), ((FileAssetContainer) container).getPath(), contentlet1.getIdentifier(),"1",0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        final Response response = pageResource
                .render(request, this.response, page.getURI(), modeParam, null,
                        String.valueOf(languageId), null, null);

        final HTMLPageAssetRendered htmlPageAssetRendered = (HTMLPageAssetRendered) ((ResponseEntityView) response.getEntity()).getEntity();

        assertEquals(1, htmlPageAssetRendered.getContainers().size());
        final ContainerRaw containerRaw = htmlPageAssetRendered.getContainers().iterator().next();

        assertEquals(
                FileAssetContainerUtil.getInstance().getFullPath((FileAssetContainer) container),
                containerRaw.getContainerView().getPath()
        );
    }

    /**
     * Utility method used to create an instance of the {@link PageContainerForm} object with the provided data.
     *
     * @param containerId   The Container ID.
     * @param contentletIds The Contentlets being added to the Container.
     * @param containerUUID The unique Container UUID.
     *
     * @return The {@link PageContainerForm} object.
     */
    private PageContainerForm createPageContainerForm(final String containerId, final List<String> contentletIds,
                                                      final String containerUUID) {
        final List<PageContainerForm.ContainerEntry> entries = new ArrayList<>();
        final PageContainerForm.ContainerEntry containerEntry = new PageContainerForm.ContainerEntry(null,
                containerId, containerUUID);
        contentletIds.forEach(containerEntry::addContentId);
        entries.add(containerEntry);
        return new PageContainerForm(entries, null);
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link PageResource#render(HttpServletRequest, HttpServletResponse, String, String, String, String, String, String)}</li>
     *     <li><b>Given Scenario:</b> In Edit Mode, test the rest API</li>
     *     <li><b>Expected Result:</b> Receive the on-number-of-pages data attribute for the contentlet object inside rendered element.</li>
     * </ul>
     */
    @Test
    public void testOnNumberOfPagesDataAttribute_render() throws DotDataException, SystemException, DotSecurityException,
                                                                PortalException {

        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);

        // Initialization
        final String modeParam = "EDIT_MODE";
        final Language defaultLang = APILocator.getLanguageAPI().getDefaultLanguage();
        final long languageId = defaultLang.getId();

        // Test data generation
        final PageRenderTestUtil.PageRenderTest pageRenderTestOne = PageRenderTestUtil.createPage(1, this.host);
        final HTMLPageAsset pageOne = pageRenderTestOne.getPage();
        final Container container = pageRenderTestOne.getFirstContainer();
        final Contentlet testContent = pageRenderTestOne.addContent(container);
        Response pageResponse = this.pageResource.render(this.request, this.response, pageOne.getURI(), modeParam, null,
                String.valueOf(languageId), null, null);

        final HTMLPageAssetRendered htmlPageAssetRendered =
                (HTMLPageAssetRendered) ((ResponseEntityView<?>) pageResponse.getEntity()).getEntity();

        final ContainerRaw containerRaw = htmlPageAssetRendered.getContainers().stream().findFirst().orElse(null);
        // Assertions
        assertTrue(containerRaw.toString().contains("data-dot-on-number-of-pages="));
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link PageResource#render(HttpServletRequest, HttpServletResponse, String, String, String, String, String, String)}</li>
     *     <li><b>Given Scenario:</b> The deviceInode is not set as part of the request</li>
     *     <li><b>Expected Result:</b> The {@link WebKeys#CURRENT_DEVICE} is removed from session</li>
     * </ul>
     */
    @Test
    public void testCleanUpSessionWhenDeviceInodeIsNull() throws Exception {
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);

        pageResource.render(request, response, pagePath, null, null, APILocator.getLanguageAPI().getDefaultLanguage().getLanguage(), null, null);

        verify(session).removeAttribute(WebKeys.CURRENT_DEVICE);
    }

    /**
     * <ul>
     *     <li><b>Method to Test:</b> {@link PageResource#render(HttpServletRequest, HttpServletResponse, String, String, String, String, String, String)}</li>
     *     <li><b>Given Scenario:</b> The deviceInode in the request is blank</li>
     *     <li><b>Expected Result:</b> The {@link WebKeys#CURRENT_DEVICE} is removed from session</li>
     * </ul>
     */
    @Test
    public void testCleanUpSessionWhenDeviceInodeIsBlank() throws Exception {
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(user);

        pageResource.render(request, response, pagePath, null, null, APILocator.getLanguageAPI().getDefaultLanguage().getLanguage(), "", null);

        verify(session).removeAttribute(WebKeys.CURRENT_DEVICE);
    }

    /**
     * This is probably the most intricate test in this class.
     * It tests the behavior of the page rendering when a contentlet is published in the future.
     * This explains the complexity of the test:
     * 1. We need to create a page with a container and a contentlet.
     * 2. The container has to hold a widget that will render the contentlet in the future using the dotcontent velocity tool.
     * 3. The container and the widget must be published.
     * 4. Then we need to create a contentlet that will be published in the future. in this case we're going to use a blog
     * 5. We need to publish the page and everything else. But the blog. The blog is set to be published in the future using the publishDate property.
     * 6. The blog content-type must be prepared indicating what property will be used as the publish date.
     * Given scenario: A page with a container and a contentlet is created. The contentlet is published in a future date.
     * Expected result: The contentlet should be rendered in the page once we pass the future date as a parameter.
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testRenderWithTimeMachine()
            throws DotDataException, DotSecurityException, WebAssetException, JsonProcessingException {

        final TimeZone defaultZone = TimeZone.getDefault();
        try {
            final TimeZone utc = TimeZone.getTimeZone("UTC");
            TimeZone.setDefault(utc);

            // Calculate the date relative to today
            // Publish Date is 10 days from now
            final LocalDateTime relativeDate1 = LocalDateTime.now().plusDays(10);
            final Instant relativeInstant1 = relativeDate1.atZone(utc.toZoneId()).toInstant();
            final Date publishDate = Date.from(relativeInstant1);

            // Time Machine Date is 11 days from now
            final LocalDateTime relativeDate2 = LocalDateTime.now().plusDays(10).plusHours(1);
            final Instant relativeInstant2 = relativeDate2.atZone(utc.toZoneId()).toInstant();
            final Date timeMachineDate = Date.from(relativeInstant2);

            final LocalDateTime relativeDate3 = LocalDateTime.now().plusDays(4);
            final Instant relativeInstant3 = relativeDate3.atZone(utc.toZoneId()).toInstant();
            final Date timeMachineDateBefore = Date.from(relativeInstant3);

            //Then we should get the content after publish date
            validatePageRendering(PageMode.LIVE, false, null, null);
            validatePageRendering(PageMode.PREVIEW_MODE, false, null, null);
            validatePageRendering(PageMode.EDIT_MODE, false, null, null);
            validatePageRendering(PageMode.ADMIN_MODE, false, null, null);

            validatePageRendering(PageMode.LIVE, true, publishDate, timeMachineDate);
            validatePageRendering(PageMode.PREVIEW_MODE, true, publishDate, timeMachineDate);
            validatePageRendering(PageMode.EDIT_MODE, true, publishDate, timeMachineDate);
            validatePageRendering(PageMode.ADMIN_MODE, true, publishDate, timeMachineDate);

            validatePageRendering(PageMode.LIVE, false, publishDate, timeMachineDateBefore);
            validatePageRendering(PageMode.PREVIEW_MODE, false, publishDate, timeMachineDateBefore);
            validatePageRendering(PageMode.EDIT_MODE, false, publishDate, timeMachineDateBefore);
            validatePageRendering(PageMode.ADMIN_MODE, false, publishDate, timeMachineDateBefore);
        } finally {
            TimeZone.setDefault(defaultZone);
        }
    }

    /**
     * Widget code that will render the contentlet in the future
     * This mirrors the code we ship with the dotCMS this is how we actually render the contentlets in the future
     * @param host the host our content is in
     * @param contentType the content type of the content we want to render
     * @return the widget code
     */
    String widgetCode(final Host host, final ContentType contentType) {
        return String.format(
                "#set($blogs = $dotcontent.pullPerPage(\"+contentType:%s +(conhost:%s conhost:SYSTEM_HOST) +variant:default\", 0, 100, null))\n" +
                "#set($resultList = []) \n" +
                "#foreach($con in $blogs)\n" +
                   "    #set($resultdoc = {})\n" +
                   "    #set($resultdoc.identifier = ${con.identifier})\n" +
                   "    #set($resultdoc.inode = ${con.inode})\n" +
                   "    #set($resultdoc.title = $!{con.title})\n" +
                   "    #set($notUsedValue = $resultList.add($resultdoc))\n" +
                 "#end\n" +
                 "!$dotJSON.put(\"posts\", $resultList)",
                contentType.variable(), host.getIdentifier()
        );
    }

    /**
     *
     * @param mode PageMode
     * @param expectContentlet true if we expect a contentlet to be rendered, false otherwise
     * @param publishDate the publish-date of the contentlet null if the contentlet is needed to be published right away
     * @param timeMachineDate the date to be used as time machine
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws WebAssetException
     */
    private void validatePageRendering(final PageMode mode, final boolean expectContentlet, final Date publishDate, final Date timeMachineDate)
            throws DotDataException, DotSecurityException, WebAssetException, JsonProcessingException {

        final User systemUser = APILocator.getUserAPI().getSystemUser();
        final long languageId = 1L;
        final ContentType blogLikeContentType = TestDataUtils.getBlogLikeContentType();

        final Structure structure = new StructureDataGen().nextPersisted();
        final Container myContainer = new ContainerDataGen()
                .withStructure(structure, "")
                .friendlyName("container-friendly-name" + System.currentTimeMillis())
                .title("container-title")
                .site(host)
                .nextPersisted();

        ContainerDataGen.publish(myContainer);

        final TemplateLayout templateLayout = TemplateLayoutDataGen.get()
                .withContainer(myContainer.getIdentifier())
                .next();

        final Template newTemplate = new TemplateDataGen()
                .drawedBody(templateLayout)
                .withContainer(myContainer.getIdentifier())
                .nextPersisted();

        final VersionableAPI versionableAPI = APILocator.getVersionableAPI();
        versionableAPI.setWorking(newTemplate);
        versionableAPI.setLive(newTemplate);

        final String myFolderName = "folder-" + System.currentTimeMillis();
        final Folder myFolder = new FolderDataGen().name(myFolderName).site(host).nextPersisted();
        final String myPageName = "my-testPage-" + System.currentTimeMillis();
        final HTMLPageAsset myPage = new HTMLPageDataGen(myFolder, newTemplate)
                .languageId(languageId)
                .pageURL(myPageName)
                .title(myPageName)
                .nextPersisted();

        final ContentletAPI contentletAPI = APILocator.getContentletAPI();
        contentletAPI.publish(myPage, systemUser, false);
        //  These are the blogs that will be shown in the widget
        // if it's published then it'll show immediately otherwise it'll show in the future
        // if it's set to show then we need to pass the time machine date to show it
        //Our blog content type has to have a publishDate field set otherwise it will never make it properly into the index
        assertNotNull(blogLikeContentType.publishDateVar());

        final ContentletDataGen blogsDataGen = new ContentletDataGen(blogLikeContentType.id())
                .languageId(languageId)
                .host(host)
                .setProperty("title", "myBlogTest")
                .setProperty("body", TestDataUtils.BLOCK_EDITOR_DUMMY_CONTENT)
                .setPolicy(IndexPolicy.WAIT_FOR)
                .languageId(languageId)
                .setProperty(Contentlet.IS_TEST_MODE, true);

        if (null != publishDate) {
            blogsDataGen.setProperty("publishDate", publishDate);  // Set the publish-date in the future
        }
        final Contentlet blog = blogsDataGen.nextPersisted();
        assertNotNull(blog.getIdentifier());
        if (null != publishDate) {
            assertFalse(blog.isLive());
        }

        //Here we're testing the time machine  query will return contentlets that are published in the future
        if(null != timeMachineDate ) {
            final String query = String.format(
                    "+contentType:%s +(conhost:%s conhost:SYSTEM_HOST) +variant:default +live:true",
                    blogLikeContentType.variable(), host.getIdentifier());
            final PaginatedContentList<Contentlet> pull = ContentUtils.pullPerPage(query, 0, 10,
                    null, APILocator.systemUser(), String.valueOf(timeMachineDate.getTime()));
            if(expectContentlet) {
                assertFalse("We should get items from using the time-machine date", pull.isEmpty());
            } else {
                assertTrue("We should not get items from using the time-machine date", pull.isEmpty());
            }
        }

        // Create a widget to show the blog
        //The widget hold the code that calls the $dotcontent.pullPerPage view tool which takes into consideration the tm date
        //in a nutshell, the widget will show the blog if the tm date is greater than the publish date
        //This is how we accomplish the time machine feature
        final ContentType widgetLikeContentType = TestDataUtils.getWidgetLikeContentType(()-> widgetCode(host, blogLikeContentType));
        final Contentlet myWidget = new ContentletDataGen(widgetLikeContentType)
                .languageId(languageId)
                .host(host)
                .setProperty("widgetTitle", "myWidgetThatCallsDotContent#pullPerPageSoThatTimeMachineWorks")
                .setProperty("code","meh.")
                .nextPersisted();
        ContentletDataGen.publish(myWidget);

        final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
        final MultiTree multiTree = new MultiTree(myPage.getIdentifier(),
                myContainer.getIdentifier(), myWidget.getIdentifier(), "1", 1);
        multiTreeAPI.saveMultiTree(multiTree);

        when(request.getAttribute(WebKeys.HTMLPAGE_LANGUAGE)).thenReturn(
                String.valueOf(languageId));

        String futureIso8601 = null;
        if (null != timeMachineDate) {
            // Convert the date to ISO 8601 format if necessary
            futureIso8601 = timeMachineDate.toInstant().toString();
        }

        HttpServletResponseThreadLocal.INSTANCE.setResponse(this.response);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(this.request);

        //This param is required to be live to behave correctly when building the query
        when(request.getAttribute(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.LIVE);
        when(request.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(systemUser);

        final String myPagePath = String.format("/%s/%s", myFolderName, myPageName);
        final Response myResponse = pageResource
                .loadJson(this.request, this.response, myPagePath, mode.name(), null,
                        String.valueOf(languageId), null, futureIso8601);

        RestUtilTest.verifySuccessResponse(myResponse);

        final PageView pageView = (PageView) ((ResponseEntityView<?>) myResponse.getEntity()).getEntity();
        final ObjectMapper objectMapper = new ObjectMapper();
        final String json = objectMapper.writeValueAsString(pageView);
        final JsonNode node = objectMapper.readTree(json);
        final Optional<JsonNode> widgetCodeJSON = findNode(node, "widgetCodeJSON");

        if(widgetCodeJSON.isPresent()){
            final JsonNode posts = widgetCodeJSON.get().get("posts");
            if(expectContentlet){
                assertFalse(posts.isEmpty());
            } else {
                assertTrue(posts.isEmpty());
            }
        } else {
            assertFalse(expectContentlet);
        }
    }

    /**
     * Utility method to find a node in a JSON tree
     * @param currentNode
     * @param nodeName
     * @return
     */
    public static Optional<JsonNode> findNode(final JsonNode currentNode, final String nodeName) {
        if (currentNode.has(nodeName)) {
            return Optional.of(currentNode.get(nodeName));  // Node found in the current level
        }

        // if the current node is an object or array, iterate over its children
        Iterator<JsonNode> elements = currentNode.elements();
        while (elements.hasNext()) {
            JsonNode child = elements.next();
            Optional<JsonNode> result = findNode(child, nodeName);  // recursive call
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();  // Node not found
    }

}
