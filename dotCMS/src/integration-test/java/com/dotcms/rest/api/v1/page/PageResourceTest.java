package com.dotcms.rest.api.v1.page;

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
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
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
    public void shouldReturnPageByURLPattern() throws DotDataException, DotSecurityException {
        final User systemUser = APILocator.getUserAPI().getSystemUser();

        final ContentType contentType = new ContentTypeDataGen().user(systemUser)
                .host(host)
                .detailPage(pageAsset.getIdentifier())
                .urlMapPattern("/{text}")
                .nextPersisted();

        new FieldDataGen()
                .name("text")
                .type(TextField.class)
                .contentTypeId(contentType.id())
                .nextPersisted();


        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.id());
        contentletDataGen
                .setProperty("text", "test_text")
                .languageId(1)
                .nextPersisted();

        final Response response = pageResource
                .render(request, this.response, "/text_test", "PREVIEW_MODE", null,
                        "1", null);

        RestUtilTest.verifySuccessResponse(response);
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

}
