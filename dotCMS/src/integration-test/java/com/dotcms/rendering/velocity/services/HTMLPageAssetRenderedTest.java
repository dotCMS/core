package com.dotcms.rendering.velocity.services;

import static org.mockito.Mockito.mock;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.dotcms.datagen.*;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPI;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContext;
import com.dotmarketing.portlets.htmlpageasset.business.render.PageContextBuilder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import com.dotcms.visitor.domain.Visitor;

@RunWith(DataProviderRunner.class)
public class HTMLPageAssetRenderedTest {

    private static ContentType contentGenericType;
    private static User systemUser;
    private static final String contentFallbackProperty = "DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE";
    private static final String pageFallbackProperty = "DEFAULT_PAGE_TO_DEFAULT_LANGUAGE";
    private static final boolean contentFallbackDefaultValue = Config.getBooleanProperty(contentFallbackProperty,false);
    private static final boolean pageFallbackDefaultValue =Config.getBooleanProperty(pageFallbackProperty,true);
    private static Folder folder;
    private static final List<String> contentletsIds = new ArrayList<>();
    private static ContentletAPI contentletAPI;
    private static final String UUID = UUIDGenerator.generateUuid();

    private static Language spanishLanguage;
    private static Host site;
    private static Persona persona;
    private static Visitor visitor;

    @DataProvider
    public static Object[][] cases() throws Exception {
        if (systemUser == null) {
            prepareGlobalData();
        }

        final Container container = createContainer();
        final Template templateContainer = createTemplate(container);

        final Container fileContainer = createFileContainer();
        final Template templateFileContainer = createTemplate(fileContainer);

        return new Object[][] {
                { container, templateContainer },
                { fileContainer, templateFileContainer}
        };
    }

    private static Template createTemplate(final Container container) throws DotSecurityException, WebAssetException, DotDataException {
        final Template template = new TemplateDataGen().title("PageContextBuilderTemplate"+System.currentTimeMillis())
                .host(site)
                .withContainer(container, UUID).nextPersisted();
        PublishFactory.publishAsset(template, systemUser, false, false);
        return template;
    }

    private static Container createContainer() throws DotSecurityException, DotDataException, WebAssetException {
        Container container = new Container();
        final String containerName = "containerHTMLPageRenderedTest" + System.currentTimeMillis();

        container.setFriendlyName(containerName);
        container.setTitle(containerName);
        container.setOwner(systemUser.getUserId());
        container.setMaxContentlets(5);

        final List<ContainerStructure> csList = new ArrayList<ContainerStructure>();
        final ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(contentGenericType.id());
        cs.setCode("$!{body}");
        csList.add(cs);

        container = APILocator.getContainerAPI().save(container, csList, site, systemUser, false);
        PublishFactory.publishAsset(container, systemUser, false, false);

        return container;
    }

    private static FileAssetContainer createFileContainer() throws DotSecurityException, DotDataException, WebAssetException {
        return createFileContainer(site);
    }

    private static FileAssetContainer createFileContainer(final Host host)
            throws DotSecurityException, DotDataException, WebAssetException {

        final String containerName = "containerHTMLPageRenderedTest" + System.currentTimeMillis();
        FileAssetContainer container = new ContainerAsFileDataGen()
                .host(host)
                .folderName(containerName)
                .contentType(contentGenericType, "$!{body}")
                .nextPersisted();

        container = (FileAssetContainer) APILocator.getContainerAPI().find(container.getInode(), systemUser, true);

        final Folder folder = APILocator.getFolderAPI().findFolderByPath(container.getPath(), host, systemUser, true);
        final List<FileAsset> containerFiles =
                APILocator.getFileAssetAPI().findFileAssetsByFolder(folder, systemUser, true);

        for (final FileAsset containerFile : containerFiles) {
            ContentletDataGen.publish(containerFile);
        }

        return container;
    }

    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();
    }

    public static void prepareGlobalData() throws Exception {

        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        contentletAPI = APILocator.getContentletAPI();
        createTestPage();

        IntegrationTestInitService.getInstance().mockStrutsActionModule();

        final Host  host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);

        persona = new PersonaDataGen().keyTag("persona"+System.currentTimeMillis()).hostFolder(host.getIdentifier()).nextPersisted();

        visitor = mock(Visitor.class);
        Mockito.when(visitor.getPersona()).thenReturn(persona);
    }

    private static void createTestPage() throws Exception{

        site = new SiteDataGen().nextPersisted();
        //Create test folder
        folder = new FolderDataGen().site(site).nextPersisted();

        //Spanish language
        spanishLanguage = TestDataUtils.getSpanishLanguage();

        //Get ContentGeneric Content-Type
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        contentGenericType = contentTypeAPI.find("webPageContent");

        //Create Contentlet in English
        final Contentlet contentlet1 = new ContentletDataGen(contentGenericType.id())
                .languageId(1)
                .folder(folder)
                .host(site)
                .setProperty("title", "content1")
                .setProperty("body", "content1")
                .nextPersisted();

        contentlet1.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet1.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet1.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet1, systemUser, false);
        //Assign permissions
        addAnonymousPermissions(contentlet1);
        contentletsIds.add(contentlet1.getIdentifier());

        //Create Contentlet with English and Spanish Versions
        final Contentlet contentlet2English = new ContentletDataGen(contentGenericType.id())
                .languageId(1)
                .folder(folder)
                .host(site)
                .setProperty("title", "content2")
                .setProperty("body", "content2")
                .nextPersisted();

        contentlet2English.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet2English.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet2English.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet2English, systemUser, false);

        Contentlet contentlet2Spanish = contentletAPI.find(contentlet2English.getInode(), systemUser, false);
        contentlet2Spanish.setProperty("title","content2Spa");
        contentlet2Spanish.setProperty("body","content2Spa");
        contentlet2Spanish.setInode("");
        contentlet2Spanish.setLanguageId(spanishLanguage.getId());
        contentlet2Spanish.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet2Spanish.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet2Spanish.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentlet2Spanish = contentletAPI.checkin(contentlet2Spanish, systemUser, false);

        contentlet2Spanish.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet2Spanish.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet2Spanish.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet2Spanish, systemUser, false);
        //Assign permissions
        addAnonymousPermissions(contentlet2Spanish);
        contentletsIds.add(contentlet2English.getIdentifier());

        //Create Contentlet in Spanish
        final Contentlet contentlet3 = new ContentletDataGen(contentGenericType.id())
                .languageId(spanishLanguage.getId())
                .setProperty("title", "content3Spa")
                .setProperty("body", "content3Spa")
                .nextPersisted();

        contentlet3.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet3.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet3.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet3, systemUser, false);
        //Assign permissions
        addAnonymousPermissions(contentlet3);
        contentletsIds.add(contentlet3.getIdentifier());

        //Create Contentlet to not default persona
        final Contentlet contentlet4 = new ContentletDataGen(contentGenericType.id())
                .languageId(1)
                .setProperty("title", "content4")
                .setProperty("body", "content4")
                .nextPersisted();

        contentlet4.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentlet4.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        contentlet4.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet4, systemUser, false);
        //Assign permissions
        addAnonymousPermissions(contentlet4);
        contentletsIds.add(contentlet4.getIdentifier());
    }


    private void  createMultiTree(final String pageId, final String containerId) throws DotDataException {
        createMultiTree(pageId, containerId, UUID);
    }

    private void  createMultiTree(final String pageId, final String containerId, final String UUID)
            throws DotDataException {

        MultiTree multiTree = new MultiTree(pageId, containerId, contentletsIds.get(0),UUID,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        multiTree = new MultiTree(pageId, containerId, contentletsIds.get(1),UUID,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        multiTree = new MultiTree(pageId, containerId, contentletsIds.get(2),UUID,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        final String personaTag  = persona.getKeyTag();
        final String personalization = Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + personaTag;

        multiTree = new MultiTree(pageId, containerId, contentletsIds.get(3),UUID,0, personalization);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);
    }

    @AfterClass
    public static void restore() throws Exception{

        Config.setProperty(contentFallbackProperty, contentFallbackDefaultValue);
        Config.setProperty(pageFallbackProperty, pageFallbackDefaultValue);

        //Deleting the folder will delete all the pages inside it
        if(folder != null){
            APILocator.getFolderAPI().delete(folder,systemUser,false);
        }

        for(final String contentletId : contentletsIds){
            final Contentlet contentlet = contentletAPI.findContentletByIdentifierAnyLanguage(contentletId);
            if(null == contentlet){
               continue;
            }

            contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
            contentlet.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
            contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            contentletAPI.destroy(contentlet, systemUser, false );
        }

    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to false and DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to true
     *       And the page have version just in ENG
     *       And the page have tree content, where: content1 is just in ENG version, content2 is in ENG and ESP version, content 3 is just in ESP version
     * Should: If the page is requests in ENG version it should be render with content1 and content2
     *         If the page is requests in ESP version it should be render with content3 and content2 (both in ESP version)
     */
    @Test
    @UseDataProvider("cases")
    public void ContentFallbackFalse_PageFallbackTrue_PageEnglish_ViewEnglishContent1And2_ViewSpanishContent2And3(
            final Container container, final Template template) throws Exception{

        Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", true);
        Config.setProperty(contentFallbackProperty,false);
        Config.setProperty(pageFallbackProperty,true);

        final String pageName = "test1Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        //request page ENG version
        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                    .setUser(systemUser)
                    .setPageUri(pageEnglishVersion.getURI())
                    .setPageMode(PageMode.LIVE)
                    .build(),
                mockRequest, mockResponse);
        Assert.assertTrue("ENG = "+html , html.contains("content2content1"));

        //request page ESP version
        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest
                .setAttribute(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(spanishLanguage.getId()));
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertTrue("ESP = "+html , html.contains("content3Spacontent2Spa"));
    }

    @NotNull
    private HTMLPageAsset createHtmlPageAsset(
            final Template template,
            final String pageName,
            final long languageId)

            throws DotSecurityException, DotDataException {
        final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder,template).languageId(languageId).pageURL(pageName).title(pageName).nextPersisted();
        pageEnglishVersion.setIndexPolicy(IndexPolicy.WAIT_FOR);
        pageEnglishVersion.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageEnglishVersion, systemUser, false);
        addAnonymousPermissions(pageEnglishVersion);
        return pageEnglishVersion;
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to false and DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to true
     *       And the page have version in ENG and ESP
     *       And the page have tree content, where: content1 is just in ENG version, content2 is in ENG and ESP version, content 3 is just in ESP version
     * Should: If the page is requests in ENG version it should be render with content1 and content2
     *         If the page is requests in ESP version it should be render with content3 and content2 (both in ESP version)
     */
    @Test
    @UseDataProvider("cases")
    public void ContentFallbackFalse_PageFallbackTrue_PageEnglishAndSpanish_ViewEnglishContent1And2_ViewSpanishContent2And3(final Container container, final Template template) throws Exception{

        Config.setProperty(contentFallbackProperty,false);
        Config.setProperty(pageFallbackProperty,true);

        final String pageName = "test2Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        Contentlet pageSpanishVersion = contentletAPI.find(pageEnglishVersion.getInode(),systemUser,false);
        pageSpanishVersion.setInode("");
        pageSpanishVersion.setLanguageId(spanishLanguage.getId());
        pageSpanishVersion.setIndexPolicy(IndexPolicy.WAIT_FOR);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        pageSpanishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        pageSpanishVersion = contentletAPI.checkin(pageSpanishVersion,systemUser,false);

        pageSpanishVersion.setIndexPolicy(IndexPolicy.WAIT_FOR);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        pageSpanishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageSpanishVersion,systemUser,false);
        addAnonymousPermissions(pageSpanishVersion);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertTrue("ENG = "+html , html.contains("content2content1"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest
                .setAttribute(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(spanishLanguage.getId()));
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertTrue("ESP = "+html , html.contains("content3Spacontent2Spa"));

    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to false and DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to true
     *       And the page have version in ESP
     *       And the page have tree content, where: content1 is just in ENG version, content2 is in ENG and ESP version, content 3 is just in ESP version
     * Should: If the page is requests in ENG version it should be thrown a {@link HTMLPageAssetNotFoundException}
     *         If the page is requests in ESP version it should be render with content3 and content2 (both in ESP version)
     */
    @UseDataProvider("cases")
    public void ContentFallbackFalse_PageFallbackTrue_PageSpanish_ViewEnglish404_ViewSpanishContent2And3(final Container container, final Template template) throws Exception{

        Config.setProperty(contentFallbackProperty,false);
        Config.setProperty(pageFallbackProperty,true);

        final String pageName = "test3Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageSpanishVersion = createHtmlPageAsset(template, pageName, spanishLanguage.getId());

        createMultiTree(pageSpanishVersion.getIdentifier(), container.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest
                .setAttribute(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(spanishLanguage.getId()));
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageSpanishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertTrue("ESP = "+html , html.contains("content3Spacontent2Spa"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);

        try {
            APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                    PageContextBuilder.builder()
                            .setUser(systemUser)
                            .setPageUri(pageSpanishVersion.getURI())
                            .setPageMode(PageMode.LIVE)
                            .build(),
                    mockRequest, mockResponse);

            throw new AssertionError("HTMLPageAssetNotFoundException expected");
        }catch(HTMLPageAssetNotFoundException e) {
            //expected
        }
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to false and DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to false
     *       And the page have version in ENG
     *       And the page have tree content, where: content1 is just in ENG version, content2 is in ENG and ESP version, content 3 is just in ESP version
     * Should: If the page is requests in ENG version it should be render with content1 and content2
     *         If the page is requests in ESP version it should be thrown a {@link HTMLPageAssetNotFoundException}
     */
    @UseDataProvider("cases")
    public void ContentFallbackFalse_PageFallbackFalse_PageEnglish_ViewEnglishContent1And2_ViewSpanish404(final Container container, final Template template) throws Exception{

        Config.setProperty(contentFallbackProperty,false);
        Config.setProperty(pageFallbackProperty,false);

        final String pageName = "test4Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertTrue("ENG = "+html , html.contains("content2content1"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest
                .setAttribute(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(spanishLanguage.getId()));
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);

        try {
            APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
            throw new AssertionError("HTMLPageAssetNotFoundException expected");
        }catch(HTMLPageAssetNotFoundException e) {
            //expected
        }
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to false and DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to false
     *       And the page have version in ENG and ESP
     *       And the page have tree content, where: content1 is just in ENG version, content2 is in ENG and ESP version, content 3 is just in ESP version
     * Should: If the page is requests in ENG version it should be render with content1 and content2
     *         If the page is requests in ESP version it should be render with content3 and content2 (both in ESP version)
     */
    @Test
    @UseDataProvider("cases")
    public void ContentFallbackFalse_PageFallbackFalse_PageEnglishAndSpanish_ViewEnglishContent1And2_ViewSpanishContent2And3(final Container container, final Template template) throws Exception{

        Config.setProperty(contentFallbackProperty,false);
        Config.setProperty(pageFallbackProperty,false);

        final String pageName = "test5Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        Contentlet pageSpanishVersion = contentletAPI.find(pageEnglishVersion.getInode(),systemUser,false);
        pageSpanishVersion.setInode("");
        pageSpanishVersion.setLanguageId(spanishLanguage.getId());
        pageSpanishVersion.setIndexPolicy(IndexPolicy.WAIT_FOR);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        pageSpanishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        pageSpanishVersion = contentletAPI.checkin(pageSpanishVersion,systemUser,false);
        pageSpanishVersion.setIndexPolicy(IndexPolicy.WAIT_FOR);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        pageSpanishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageSpanishVersion,systemUser,false);
        addAnonymousPermissions(pageSpanishVersion);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertTrue("ENG = "+html , html.contains("content2content1"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest
                .setAttribute(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(spanishLanguage.getId()));
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertTrue("ESP = "+html , html.contains("content3Spacontent2Spa"));
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to true and DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to true
     *       And the page have version in ENG and ESP
     *       And the page have tree content, where: content1 is just in ENG version, content2 is in ENG and ESP version, content 3 is just in ESP version
     * Should: If the page is requests in ENG version it should be render with content1 and content2
     *         If the page is requests in ESP version it should be render with content1 (ENG version), content3 and content2 (both in ESP version)
     */
    @Test
    @UseDataProvider("cases")
    public void ContentFallbackTrue_PageFallbackTrue_PageEnglishAndSpanish_ViewEnglishContent1And2_ViewSpanishContent1And2And3(final Container container, final Template template) throws Exception{

        Config.setProperty(contentFallbackProperty,true);
        Config.setProperty(pageFallbackProperty,true);

        final String pageName = "test6Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);
        Contentlet pageSpanishVersion = contentletAPI.find(pageEnglishVersion.getInode(),systemUser,false);

        pageSpanishVersion.setInode("");
        pageSpanishVersion.setLanguageId(spanishLanguage.getId());
        pageSpanishVersion.setIndexPolicy(IndexPolicy.WAIT_FOR);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        pageSpanishVersion = contentletAPI.checkin(pageSpanishVersion,systemUser,false);

        pageSpanishVersion.setIndexPolicy(IndexPolicy.WAIT_FOR);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageSpanishVersion,systemUser,false);
        addAnonymousPermissions(pageSpanishVersion);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertTrue("ENG = "+html , html.contains("content2content1"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest
                .setAttribute(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(spanishLanguage.getId()));
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertTrue("ESP = "+html , html.contains("content3Spacontent2Spacontent1"));
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to true and DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to false
     *       And the page have version just in ENG
     *       And the page have tree content, where: content1 is just in ENG version, content2 is in ENG and ESP version, content 3 is just in ESP version
     * Should: If the page is requests in ENG version it should be render with content1 and content2
     *         If the page is requests in ESP version it should be thrown a {@link HTMLPageAssetNotFoundException}
     */
    @UseDataProvider("cases")
    public void ContentFallbackTrue_PageFallbackFalse_PageEnglish_ViewEnglishContent1And2_ViewSpanish404(final Container container, final Template template) throws Exception{

        Config.setProperty(contentFallbackProperty,true);
        Config.setProperty(pageFallbackProperty,false);

        final String pageName = "test7Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertTrue("ENG = "+html , html.contains("content2content1"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest
                .setAttribute(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(spanishLanguage.getId()));
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);

        try{
            APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                    PageContextBuilder.builder()
                            .setUser(systemUser)
                            .setPageUri(pageEnglishVersion.getURI())
                            .setPageMode(PageMode.LIVE)
                            .build(),
                    mockRequest, mockResponse);

            throw new AssertionError("HTMLPageAssetNotFoundException expected");
        }catch(HTMLPageAssetNotFoundException e) {
            //expected
        }
    }

    /**
     * This test creates a widget content type, sets a value for the widget code,
     * creates a contentlet of this new widget and add it to a page.
     * If you update the value widget code, and hit again the page the new value should show up.
     *
     * @throws Exception
     */
    @Test
    @UseDataProvider("cases")
    public void constantField_notUpdatedCache_whenChanged(final Container container, final Template template) throws Exception{

        ContentType contentType = ContentTypeBuilder
                .builder(BaseContentType.WIDGET.immutableClass())
                .folder(Folder.SYSTEM_FOLDER)
                .host(Host.SYSTEM_HOST).name("WidgetContentType " + System.currentTimeMillis())
                .owner(APILocator.systemUser().toString())
                .variable("WCTVariable" + System.currentTimeMillis()).build();
        contentType = APILocator.getContentTypeAPI(systemUser).save(contentType);

        try {
            List<Field> fields = contentType.fields();
            ImmutableConstantField codeField = (ImmutableConstantField) fields.stream()
                    .filter(field -> field.name().equalsIgnoreCase("widget code")).findFirst()
                    .get();
            codeField = codeField.withValues("original code");
            APILocator.getContentTypeFieldAPI().save(codeField, systemUser);
            contentType = APILocator.getContentTypeAPI(systemUser).save(contentType);

            final Contentlet contentlet = new ContentletDataGen(contentType.id()).languageId(1)
                    .setProperty("widgetTitle", "testing").nextPersisted();

            contentlet.setIndexPolicy(IndexPolicy.WAIT_FOR);
            contentlet.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
            contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            contentletAPI.publish(contentlet, systemUser, false);
            addAnonymousPermissions(contentlet);

            final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder, template)
                    .languageId(1).pageURL("testPageWidget"+ System.currentTimeMillis()).title("testPageWidget")
                    .nextPersisted();
            pageEnglishVersion.setIndexPolicy(IndexPolicy.WAIT_FOR);
            pageEnglishVersion.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
            contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            contentletAPI.publish(pageEnglishVersion, systemUser, false);
            addAnonymousPermissions(pageEnglishVersion);

            final MultiTree multiTree = new MultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier(),
                    contentlet.getIdentifier(), UUID, 0);
            APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

            HttpServletRequest mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                            .request())
                    .request();
            Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
            mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
            final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
            String html = APILocator.getHTMLPageAssetRenderedAPI()
                    .getPageHtml(
                            PageContextBuilder.builder()
                                    .setUser(systemUser)
                                    .setPageUri(pageEnglishVersion.getURI())
                                    .setPageMode(PageMode.LIVE)
                                    .build(),
                            mockRequest, mockResponse);
            Assert.assertTrue(html, html.contains("original code"));

            fields = contentType.fields();
            codeField = (ImmutableConstantField) fields.stream()
                    .filter(field -> field.name().equalsIgnoreCase("widget code")).findFirst()
                    .get();
            codeField = codeField.withValues("this has been changed");
            APILocator.getContentTypeFieldAPI().save(codeField, systemUser);
            contentType = APILocator.getContentTypeAPI(systemUser).save(contentType);

            mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                            .request())
                    .request();
            Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
            mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
            html = APILocator.getHTMLPageAssetRenderedAPI()
                    .getPageHtml(
                            PageContextBuilder.builder()
                                    .setUser(systemUser)
                                    .setPageUri(pageEnglishVersion.getURI())
                                    .setPageMode(PageMode.LIVE)
                                    .build(),
                            mockRequest, mockResponse);
            Assert.assertTrue(html, html.contains("this has been changed"));
        }finally {
            APILocator.getContentTypeAPI(systemUser).delete(contentType);
        }
    }

    /**
     * This test is for when you archived a container that is being used on page,
     * the page needs to be resolved without issues. And when you unarchived and
     * publish the container, the page needs to show the content related to the container.
     *
     */
    @Test
    public void containerArchived_PageShouldResolve() throws Exception {

        final Container container = createContainer();
        final Template template = createTemplate(container);

        try {
            final String pageName = "testPageContainer-" + System.currentTimeMillis();
            final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

            pageEnglishVersion.setIndexPolicy(IndexPolicy.WAIT_FOR);
            pageEnglishVersion.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
            pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            contentletAPI.publish(pageEnglishVersion, systemUser, false);
            addAnonymousPermissions(pageEnglishVersion);

            createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

            HttpServletRequest mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                            .request())
                    .request();
            Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
            mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
            final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
            String html = APILocator.getHTMLPageAssetRenderedAPI()
                    .getPageHtml(
                            PageContextBuilder.builder()
                                    .setUser(systemUser)
                                    .setPageUri(pageEnglishVersion.getURI())
                                    .setPageMode(PageMode.LIVE)
                                    .build(),
                            mockRequest, mockResponse);
            Assert.assertTrue(html, html.contains("content2content1"));

            WebAssetFactory.unLockAsset(container);
            WebAssetFactory.archiveAsset(container, systemUser);
            CacheLocator.getVeloctyResourceCache().clearCache();

            mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                            .request())
                    .request();
            Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
            mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
            html = APILocator.getHTMLPageAssetRenderedAPI()
                    .getPageHtml(
                            PageContextBuilder.builder()
                                    .setUser(systemUser)
                                    .setPageUri(pageEnglishVersion.getURI())
                                    .setPageMode(PageMode.LIVE)
                                    .build(),
                            mockRequest, mockResponse);
            Assert.assertTrue(html, html.isEmpty());

            WebAssetFactory.unArchiveAsset(container);
            WebAssetFactory.publishAsset(container, systemUser);
            CacheLocator.getVeloctyResourceCache().clearCache();

            mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                            .request())
                    .request();
            Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
            mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
            html = APILocator.getHTMLPageAssetRenderedAPI()
                    .getPageHtml(
                            PageContextBuilder.builder()
                                    .setUser(systemUser)
                                    .setPageUri(pageEnglishVersion.getURI())
                                    .setPageMode(PageMode.LIVE)
                                    .build(),
                            mockRequest, mockResponse);
            Assert.assertTrue(html, html.contains("content2content1"));
        }finally {
            if (!(container instanceof FileAssetContainer)) {
                WebAssetFactory.unArchiveAsset(container);
                WebAssetFactory.publishAsset(container, systemUser);
            }
        }
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Create a page with three contents for default persona, and 1 content to another persona
     * ExpectedResult: The page should return the content according to the persona set into the request
     *
     * @throws Exception
     */
    @Test
    @UseDataProvider("cases")
    public void shouldReturnPageHTMLForPersona(final Container container, final Template template) throws Exception{


        final String pageName = "test5Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        Mockito.when(mockRequest.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(site);
        Mockito.when(mockRequest.getRequestURI()).thenReturn(pageEnglishVersion.getURI());

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        final HttpSession session = createHttpSession(mockRequest);
        Mockito.when(session.getAttribute(WebKeys.VISITOR)).thenReturn(visitor);

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertTrue(html , html.contains("content4"));

        Mockito.when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals(html , "content2content1");
    }

    private HttpSession createHttpSession(final HttpServletRequest mockRequest) {
        final HttpSession session = mock(HttpSession.class);
        Mockito.when(mockRequest.getSession()).thenReturn(session);
        Mockito.when(mockRequest.getSession(false)).thenReturn(session);
        Mockito.when(mockRequest.getSession(true)).thenReturn(session);
        return session;
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Create a page with legacy UUID
     * ExpectedResult: The page should return the right HTML
     *
     * @throws Exception
     */
    @Test
    @UseDataProvider("cases")
    public void shouldReturnPageHTMLForLegacyUUID(final Container container, final Template templateTestCase) throws Exception {

        final String containerId = container.getIdentifier();

        //Create a Template
        final Template template = new TemplateDataGen().title("PageContextBuilderTemplate"+System.currentTimeMillis())
                .withContainer(containerId, ContainerUUID.UUID_LEGACY_VALUE).nextPersisted();
        PublishFactory.publishAsset(template, systemUser, false, false);

        final String pageName = "testPage-"+System.currentTimeMillis();
        final HTMLPageAsset page = new HTMLPageDataGen(folder,template).languageId(1).pageURL(pageName).title(pageName).nextPersisted();
        page.setIndexPolicy(IndexPolicy.WAIT_FOR);
        page.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        page.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(page, systemUser, false);

        final String pageId = page.getIdentifier();
        MultiTree multiTree = new MultiTree(pageId, containerId, contentletsIds.get(0), ContainerUUID.UUID_START_VALUE,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        multiTree = new MultiTree(pageId, containerId, contentletsIds.get(1), ContainerUUID.UUID_START_VALUE,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        Mockito.when(mockRequest.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(site);
        Mockito.when(mockRequest.getRequestURI()).thenReturn(page.getURI());

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        final HttpSession session = createHttpSession(mockRequest);
        Mockito.when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals("content2content1", html);
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Create a page with legacy UUID and MultiTree
     * ExpectedResult: The page should return the right HTML
     *
     * @throws Exception
     */
    @Test
    @UseDataProvider("cases")
    public void shouldReturnPageHTMLForLegacyUUIDAndMultiTree(final Container container, final Template templateTestCase) throws Exception {
        final String containerId = container.getIdentifier();

        //Create a Template
        final Template template = new TemplateDataGen().title("PageContextBuilderTemplate"+System.currentTimeMillis())
                .withContainer(containerId, ContainerUUID.UUID_LEGACY_VALUE).nextPersisted();
        PublishFactory.publishAsset(template, systemUser, false, false);

        final String pageName = "testPage-"+System.currentTimeMillis();
        final HTMLPageAsset page = new HTMLPageDataGen(folder,template).languageId(1).pageURL(pageName).title(pageName).nextPersisted();
        page.setIndexPolicy(IndexPolicy.WAIT_FOR);
        page.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        page.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(page, systemUser, false);

        final String pageId = page.getIdentifier();
        MultiTree multiTree = new MultiTree(pageId, containerId, contentletsIds.get(0), ContainerUUID.UUID_LEGACY_VALUE,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        multiTree = new MultiTree(pageId, containerId, contentletsIds.get(1), ContainerUUID.UUID_LEGACY_VALUE,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        Mockito.when(mockRequest.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(site);
        Mockito.when(mockRequest.getRequestURI()).thenReturn(page.getURI());

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        final HttpSession session = createHttpSession(mockRequest);
        Mockito.when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        Assert.assertEquals("content2content1", html);
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Create a page with parseContainer directive into its template, and request the HTML in EDIT_MODE
     * ExpectedResult: should return a UUID with the 'dotParser_' prefix
     *
     * @throws Exception
     */
    @Test
    @UseDataProvider("cases")
    public void shouldReturnParserContainerUUID(final Container container, final Template template) throws Exception {

        Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", true);
        final String pageName = "test5Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        final HttpServletRequest mockRequest = createHttpServletRequest(pageEnglishVersion);

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        final HttpSession session = createHttpSession(mockRequest);

        Mockito.when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        systemUser.isBackendUser();

        final String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.EDIT_MODE)
                        .build(),
                mockRequest, mockResponse);

        final String regexExpected =
                "<div data-dot-object=\"container\" .* data-dot-uuid=\"dotParser_.*\" .*>" +
                    "<div data-dot-object=\"contentlet\" .*>.*</div>" +
                    "<div data-dot-object=\"contentlet\" .*>.*</div>" +
                    "<div data-dot-object=\"contentlet\" .*>.*</div>" +
                "</div>";

        Assert.assertTrue(html.matches(regexExpected));
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Create a page with File Container linked with relative path in the template
     * ExpectedResult: should work
     *
     * @throws Exception
     */
    @Test
    public void shouldRenderRelativeContainerPath() throws Exception {

        final FileAssetContainer container = createFileContainer();
        final Template template = new TemplateDataGen().title("PageContextBuilderTemplate"+System.currentTimeMillis())
                .host(site)
                .withContainer(container.getPath(), UUID)
                .nextPersisted();
        PublishFactory.publishAsset(template, systemUser, false, false);

        final String pageName = "testPage-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        final HttpServletRequest mockRequest = createHttpServletRequest(pageEnglishVersion);
        Mockito.when(mockRequest.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.LIVE.toString());

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final HttpSession session = createHttpSession(mockRequest);
        Mockito.when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);
        Mockito.when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(systemUser);

        final String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);

        Assert.assertEquals("content2content1", html);
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Create a page and a advance template using a File Container in another site
     * ExpectedResult: should work
     *
     * @throws Exception
     */
    @Test
    public void shouldRenderUsingOtherSiteContainer() throws Exception {
        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), true);
        final FileAssetContainer container = createFileContainer(defaultHost);
        final Template template = new TemplateDataGen().title("PageContextBuilderTemplate"+System.currentTimeMillis())
                .host(site)
                .withContainer("//" + defaultHost.getName()  + container.getPath(), UUID)
                .nextPersisted();
        PublishFactory.publishAsset(template, systemUser, false, false);

        final String pageName = "testPage-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        final HttpServletRequest mockRequest = createHttpServletRequest(pageEnglishVersion);
        Mockito.when(mockRequest.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.LIVE.toString());

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final HttpSession session = createHttpSession(mockRequest);
        Mockito.when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);
        Mockito.when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(systemUser);

        final String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);

        Assert.assertEquals("content2content1", html);
    }

    /**
     * Method to test: {@link com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * Given Scenario: Create a page and a not advance template using a File Container in another site
     * ExpectedResult: should work
     *
     * @throws Exception
     */
    @Test
    public void shouldRenderUsingOtherSiteContainerAndNotAdvanceTemplate() throws Exception {
        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), true);
        final FileAssetContainer container = createFileContainer(defaultHost);

        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer("//" + defaultHost.getName()  + container.getPath())
                .next();

        final Contentlet contentlet = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .title("PageContextBuilderTemplate"+System.currentTimeMillis())
                .host(site)
                .drawedBody(templateLayout)
                .theme(contentlet)
                .nextPersisted();

        PublishFactory.publishAsset(template, systemUser, false, false);

        final String pageName = "testPage-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier(), "1");

        final HttpServletRequest mockRequest = createHttpServletRequest(pageEnglishVersion);
        Mockito.when(mockRequest.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.LIVE.toString());

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final HttpSession session = createHttpSession(mockRequest);
        Mockito.when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);
        Mockito.when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(systemUser);

        final String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);

        Assert.assertTrue(html.contains("content2content1"));
    }

    //Data Provider for the Widget Pre-execute code test
    @DataProvider
    public static Object[] dataProviderWidgetPreExecuteCode() {
        return new Object[] {
                new WidgetPreExecuteCodeTestCase(PageMode.EDIT_MODE),
                new WidgetPreExecuteCodeTestCase(PageMode.PREVIEW_MODE),
                new WidgetPreExecuteCodeTestCase(PageMode.LIVE),
        };
    }

    private static class WidgetPreExecuteCodeTestCase {
        final PageMode pageMode;

        public WidgetPreExecuteCodeTestCase(final PageMode pageMode) {
            this.pageMode = pageMode;
        }
    }

    @Test
    @UseDataProvider("dataProviderWidgetPreExecuteCode")
    public void test_WidgetPreExecuteCodeShowRegardlessPageMode(final WidgetPreExecuteCodeTestCase testCase) throws Exception {

        Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE", true);
        //Update the Preexecute Field to have some code in it
        final String preExcuteCode = "PreExecute Code Displayed";
        ContentType contentType = TestDataUtils.getWidgetLikeContentType();
        final Field preExecuteField = APILocator.getContentTypeFieldAPI().byContentTypeIdAndVar(contentType.id(),WidgetContentType.WIDGET_PRE_EXECUTE_FIELD_VAR);
        APILocator.getContentTypeFieldAPI()
                .save(FieldBuilder.builder(preExecuteField).values(preExcuteCode).build(), systemUser);
        // Assert that the widget has set the pre-execute field
        Assert.assertTrue(APILocator.getContentTypeFieldAPI()
                .byContentTypeIdAndVar(contentType.id(),WidgetContentType.WIDGET_PRE_EXECUTE_FIELD_VAR)
                .values().equals(preExcuteCode));

        //Create Contentlet
        final Contentlet widgetContentlet = TestDataUtils.getWidgetContent(true,1,contentType.id());
        addAnonymousPermissions(widgetContentlet);

        //Create Container, Template and Page
        final Container container = createContainer();
        final Template template = createTemplate(container);

        final String pageName = "testPage-"+System.currentTimeMillis();
        final HTMLPageAsset page = createHtmlPageAsset(template, pageName, 1);

        //Add contentlet to the page
        MultiTree multiTree = new MultiTree(page.getIdentifier(), container.getIdentifier(),widgetContentlet.getIdentifier() ,UUID,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        //Request page
        final HttpServletRequest mockRequest = createHttpServletRequest(page);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final HttpSession session = createHttpSession(mockRequest);
        Mockito.when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);
        Mockito.when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(systemUser);
        final String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(page.getURI())
                        .setPageMode(testCase.pageMode)
                        .build(),
                mockRequest, mockResponse);

        //Page html must contains the pre-execute code
        Assert.assertTrue("Page Mode: " + testCase.pageMode + " html: " + html,html.contains(preExcuteCode));
    }

    @NotNull
    private HttpServletRequest createHttpServletRequest(HTMLPageAsset pageEnglishVersion) throws DotDataException {
        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        Mockito.when(mockRequest.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(site);
        Mockito.when(mockRequest.getRequestURI()).thenReturn(pageEnglishVersion.getURI());
        Mockito.when(mockRequest.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(PageMode.EDIT_MODE.toString());
        Mockito.when(mockRequest.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(systemUser);
        return mockRequest;
    }

    private static void addAnonymousPermissions(final Contentlet contentlet)
            throws DotDataException, DotSecurityException {

        //Assign permissions
        APILocator.getPermissionAPI().save(
                new Permission(contentlet.getPermissionId(),
                        APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                        PermissionAPI.PERMISSION_READ),
                contentlet, APILocator.systemUser(), false);
    }

}
