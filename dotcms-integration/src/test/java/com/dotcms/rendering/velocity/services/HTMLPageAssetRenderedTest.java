package com.dotcms.rendering.velocity.services;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.DotAssetContentType;
import com.dotcms.contenttype.model.type.WidgetContentType;
import com.dotcms.datagen.*;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.rendering.velocity.directive.ParseContainer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.visitor.domain.Visitor;
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
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.business.FolderAPI;
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
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Function2;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("JUnitMalformedDeclaration")
@RunWith(DataProviderRunner.class)
public class HTMLPageAssetRenderedTest {

    private static ContentType contentGenericType;
    private static User systemUser;
    private static final String DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE = "DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE";
    private static final String DEFAULT_PAGE_TO_DEFAULT_LANGUAGE = "DEFAULT_PAGE_TO_DEFAULT_LANGUAGE";
    private static boolean contentFallbackDefaultValue;
    private static boolean pageFallbackDefaultValue;
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

        return new Object[][]{
                {TestContainerType.DEFAULT},
                {TestContainerType.FILE}
        };
    }

    private static Template createTemplate(final Container container)
            throws DotSecurityException, WebAssetException, DotDataException {
        return createTemplate(new TestContainerUUID(container, UUID));
    }

    private static Template createTemplate(final TestContainerUUID... containers)
            throws DotSecurityException, WebAssetException, DotDataException {
        final TemplateDataGen templateDataGen = new TemplateDataGen()
                .title("PageContextBuilderTemplate" + System.currentTimeMillis())
                .host(site);

        for (TestContainerUUID testContainerUUID : containers) {
            templateDataGen.withContainer(testContainerUUID.container, testContainerUUID.UUID)
                    .nextPersisted();
        }

        final Template template = templateDataGen.nextPersisted();

        PublishFactory.publishAsset(template, systemUser, false, false);
        return template;
    }

    private static Container createContainer()
            throws DotSecurityException, DotDataException, WebAssetException {
        return createContainer(5);
    }

    private static Container createContainer(final int maxContentlet)
            throws DotSecurityException, DotDataException, WebAssetException {
        Container container = new Container();
        final String containerName = "containerHTMLPageRenderedTest" + System.currentTimeMillis();

        container.setFriendlyName(containerName);
        container.setTitle(containerName);
        container.setOwner(systemUser.getUserId());
        container.setMaxContentlets(maxContentlet);

        final List<ContainerStructure> csList = new ArrayList<>();
        final ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(contentGenericType.id());
        cs.setCode("$!{body}");
        csList.add(cs);

        container = APILocator.getContainerAPI().save(container, csList, site, systemUser, false);
        PublishFactory.publishAsset(container, systemUser, false, false);

        return container;
    }

    private static FileAssetContainer createFileContainer()
            throws DotSecurityException, DotDataException {
        return createFileContainer(site);
    }

    private static FileAssetContainer createFileContainer(final Host host)
            throws DotSecurityException, DotDataException {

        final String containerName = "containerHTMLPageRenderedTest" + System.currentTimeMillis();
        FileAssetContainer container = new ContainerAsFileDataGen()
                .host(host)
                .folderName(containerName)
                .contentType(contentGenericType, "$!{body}")
                .nextPersisted();

        container = (FileAssetContainer) APILocator.getContainerAPI()
                .find(container.getInode(), systemUser, true);

        final Folder folder = APILocator.getFolderAPI()
                .findFolderByPath(container.getPath(), host, systemUser, true);
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
        contentFallbackDefaultValue = Config.getBooleanProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE,
                false);
        pageFallbackDefaultValue = Config.getBooleanProperty(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE,
                true);
        prepareGlobalData();
    }

    public static void prepareGlobalData() throws Exception {

        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        contentletAPI = APILocator.getContentletAPI();
        createTestPage();

        IntegrationTestInitService.getInstance().mockStrutsActionModule();

        final Host host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);

        persona = new PersonaDataGen().keyTag("persona" + System.currentTimeMillis())
                .hostFolder(host.getIdentifier()).nextPersisted();

        visitor = mock(Visitor.class);
        when(visitor.getPersona()).thenReturn(persona);
    }

    private static void createTestPage() throws Exception {

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
                .setProperty("body", String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content1"))
                .nextPersisted();

        contentlet1.setIndexPolicy(IndexPolicy.FORCE);
        contentlet1.setIndexPolicyDependencies(IndexPolicy.FORCE);
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
                .setProperty("body", String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content2"))
                .nextPersisted();

        contentlet2English.setIndexPolicy(IndexPolicy.FORCE);
        contentlet2English.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet2English.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet2English, systemUser, false);

        Contentlet contentlet2Spanish = contentletAPI.checkout(contentlet2English.getInode(),
                systemUser, false);
        contentlet2Spanish.setProperty("title", "content2Spa");
        contentlet2Spanish.setProperty("body", String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content2Spa"));
        contentlet2Spanish.setLanguageId(spanishLanguage.getId());
        contentlet2Spanish.setIndexPolicy(IndexPolicy.FORCE);
        contentlet2Spanish.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet2Spanish.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentlet2Spanish = contentletAPI.checkin(contentlet2Spanish, systemUser, false);

        contentlet2Spanish.setIndexPolicy(IndexPolicy.FORCE);
        contentlet2Spanish.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet2Spanish.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet2Spanish, systemUser, false);
        //Assign permissions
        addAnonymousPermissions(contentlet2Spanish);
        contentletsIds.add(contentlet2English.getIdentifier());

        //Create Contentlet in Spanish
        final Contentlet contentlet3 = new ContentletDataGen(contentGenericType.id())
                .languageId(spanishLanguage.getId())
                .folder(folder)
                .host(site)
                .setProperty("title", "content3Spa")
                .setProperty("body", String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content3Spa"))
                .nextPersisted();

        contentlet3.setIndexPolicy(IndexPolicy.FORCE);
        contentlet3.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet3.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet3, systemUser, false);
        //Assign permissions
        addAnonymousPermissions(contentlet3);
        contentletsIds.add(contentlet3.getIdentifier());

        //Create Contentlet to not default persona
        final Contentlet contentlet4 = new ContentletDataGen(contentGenericType.id())
                .languageId(1)
                .setProperty("title", "content4")
                .setProperty("body", String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content4"))
                .nextPersisted();

        contentlet4.setIndexPolicy(IndexPolicy.FORCE);
        contentlet4.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet4.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet4, systemUser, false);
        //Assign permissions
        addAnonymousPermissions(contentlet4);
        contentletsIds.add(contentlet4.getIdentifier());
    }


    private void createMultiTree(final String pageId, final String containerId)
            throws DotDataException {
        createMultiTree(pageId, containerId, UUID);
    }


    private void createMultiTree(final String pageId, final String containerId, final String UUID)
            throws DotDataException {

        final List<MultiTree> mTrees = new ArrayList<>();
        // english - content1
        MultiTree multiTree = new MultiTree(pageId, containerId, contentletsIds.get(0), UUID, 0);
        mTrees.add(multiTree);

        // english and spanish - content2/content2Spa
        multiTree = new MultiTree(pageId, containerId, contentletsIds.get(1), UUID, 0);
        mTrees.add(multiTree);

        // spanish  - content3Spa
        multiTree = new MultiTree(pageId, containerId, contentletsIds.get(2), UUID, 0);
        mTrees.add(multiTree);

        // english, custom persona - content4
        final String personaTag = persona.getKeyTag();
        final String personalization =
                Persona.DOT_PERSONA_PREFIX_SCHEME + StringPool.COLON + personaTag;

        multiTree = new MultiTree(pageId, containerId, contentletsIds.get(3), UUID, 0,
                personalization);
        mTrees.add(multiTree);

        APILocator.getMultiTreeAPI().saveMultiTrees(mTrees);
    }

    @AfterClass
    public static void restore() throws Exception {

        Logger.info(HTMLPageAssetRenderedTest.class,"Cleaning up ..............................");
        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, contentFallbackDefaultValue);
        Config.setProperty(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE, pageFallbackDefaultValue);


        TestDataUtils.waitForEmptyQueue();

        //Deleting the folder will delete all the pages inside it
        if (folder != null) {

            APILocator.getFolderAPI().delete(folder, systemUser, false);
        }

        TestDataUtils.waitForEmptyQueue();

        for (final String contentletId : contentletsIds) {

            final Contentlet contentlet = contentletAPI.findContentletByIdentifierAnyLanguage(
                    contentletId);
            if (null == contentlet) {
                Logger.warn(HTMLPageAssetRenderedTest.class,
                        "Contentlet with identifier " + contentletId + " not found to delete");
                continue;
            }
            else
            {
                Logger.info(HTMLPageAssetRenderedTest.class,"Deleting contentlet with identifier " + contentletId);
            }

            contentlet.setIndexPolicy(IndexPolicy.FORCE);
            contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
            contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            contentletAPI.destroy(contentlet, systemUser, false);
        }

        TestDataUtils.waitForEmptyQueue();
        Logger.info(HTMLPageAssetRenderedTest.class,"Cleanup complete ..............................");

    }

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to false and
     * DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to true And the page have version just in ENG And the
     * page have tree content, where: content1 is just in ENG version, content2 is in ENG and ESP
     * version, content 3 is just in ESP version Should: Since the page is requests in ENG version
     * it should be rendered with content1 and content2
     */
    @Test
    @UseDataProvider("cases")
    public void ContentFallbackFalse_PageFallbackTrue_PageEnglish_ViewEnglishContent1And2_ViewSpanishContent2And3(
            final TestContainerType containerType) throws Exception {

        TestContainerFactory containerFactory = TestContainerFactory.getInstance();

        final Container container = containerFactory.getContainer(containerType);
        final Template template = containerFactory.getTemplate(containerType);

        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, false);
        Config.setProperty(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE, true);

        final String pageName = "test1Page-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI()
                .getMultiTrees(pageEnglishVersion, container);
        assertNotNull(multiTrees);
        assertEquals(4, multiTrees.size());

        int contentletSpaCount = 0;
        for (final MultiTree multiTree : multiTrees) {
            try {

                final Contentlet contentletSpa = APILocator.getContentletAPI()
                        .findContentletByIdentifier(
                                multiTree.getContentlet(), true, spanishLanguage.getId(),
                                systemUser, false);
                if (null != contentletSpa) {
                    contentletSpaCount += 1;
                }
            } catch (DotContentletStateException ignored) {
            }
        }

        assertEquals(2, contentletSpaCount);

        //request page ENG version
        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
        assertTrue("ENG = " + html, html.contains("content1") && html.contains("content2"));

    }

    ///////

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} Given Scenario: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to false and
     * DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to true And the page have version just in ENG And the
     * page have tree content, where: content1 is just in ENG version, content2 is in ENG and ESP
     * version, content 3 is just in ESP version And the current language (the request language, is
     * spanish) ExpectedResult: Since the lang is spanish, the contents rendered will be only
     * spanish content on the page.
     */
    @Test
    @UseDataProvider("cases")
    public void render_spanish_contentlets_on_english_page(
            final TestContainerType containerType) throws Exception {

        TestContainerFactory containerFactory = TestContainerFactory.getInstance();

        final Container container = containerFactory.getContainer(containerType);
        final Template template = containerFactory.getTemplate(containerType);

        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, false);
        Config.setProperty(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE, true);

        final String pageName = "test1Page-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI()
                .getMultiTrees(pageEnglishVersion, container);
        assertNotNull(multiTrees);
        assertEquals(4, multiTrees.size());

        int contentletSpaCount = 0;
        for (final MultiTree multiTree : multiTrees) {
            try {

                final Contentlet contentletSpa = APILocator.getContentletAPI()
                        .findContentletByIdentifier(
                                multiTree.getContentlet(), true, spanishLanguage.getId(),
                                systemUser, false);
                if (null != contentletSpa) {
                    contentletSpaCount += 1;
                }
            } catch (DotContentletStateException ignored) {
            }
        }

        assertEquals(2, contentletSpaCount);

        //request page ESP version
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        final HttpSession httpSession = mockRequest.getSession();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest
                .setAttribute(WebKeys.HTMLPAGE_LANGUAGE, String.valueOf(spanishLanguage.getId()));
        httpSession.setAttribute(WebKeys.HTMLPAGE_LANGUAGE,
                String.valueOf(spanishLanguage.getId()));
        httpSession.setAttribute(WebKeys.HTMLPAGE_LANGUAGE + ".current",
                String.valueOf(spanishLanguage.getId()));
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);

        CacheLocator.getBlockPageCache().clearCache();
        CacheLocator.getBlockDirectiveCache().clearCache();
        CacheLocator.getHTMLPageCache().clearCache();

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        assertTrue("ESP = " + html, html.contains("content2Spa") && html.contains("content3Spa"));
    }
    ///////

    @NotNull
    private HTMLPageAsset createHtmlPageAsset(
            final Template template,
            final String pageName,
            final long languageId)

            throws DotSecurityException, DotDataException {
        return createHtmlPageAsset(site, template, pageName, languageId);
    }

    @NotNull
    private HTMLPageAsset createHtmlPageAsset(
            final Host host, final Template template,
            final String pageName,
            final long languageId)

            throws DotSecurityException, DotDataException {

        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder, template)
                .languageId(languageId)
                .pageURL(pageName)
                .title(pageName)
                .cacheTTL(0)
                .nextPersisted();

        pageEnglishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageEnglishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageEnglishVersion, systemUser, false);
        addAnonymousPermissions(pageEnglishVersion);
        return pageEnglishVersion;
    }

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to false and
     * DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to true And the page have version in ENG and ESP And
     * the page have tree content, where: content1 is just in ENG version, content2 is in ENG and
     * ESP version, content 3 is just in ESP version Should: If the page is requests in ENG version
     * it should be rendered with content1 and content2 If the page is requests in ESP version it
     * should be rendered with content3 and content2 (both in ESP version)
     */
    @Test
    @UseDataProvider("cases")
    public void ContentFallbackFalse_PageFallbackTrue_PageEnglishAndSpanish_ViewEnglishContent1And2_ViewSpanishContent2And3(
            final TestContainerType containerType) throws Exception {

        TestContainerFactory containerFactory = TestContainerFactory.getInstance();

        final Container container = containerFactory.getContainer(containerType);
        final Template template = containerFactory.getTemplate(containerType);

        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, false);
        Config.setProperty(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE, true);

        final String pageName = "test2Page-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        Contentlet pageSpanishVersion = contentletAPI.checkout(pageEnglishVersion.getInode(),
                systemUser, false);
        pageSpanishVersion.setLanguageId(spanishLanguage.getId());
        pageSpanishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageSpanishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        pageSpanishVersion = contentletAPI.checkin(pageSpanishVersion, systemUser, false);

        pageSpanishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageSpanishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageSpanishVersion, systemUser, false);
        addAnonymousPermissions(pageSpanishVersion);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
        assertTrue("ENG = " + html, html.contains("content1") && html.contains("content2"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
        assertTrue("ESP = " + html, html.contains("content2Spa") && html.contains("content3Spa"));

    }

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to false and
     * DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to true And the page have version in ESP And the page
     * have tree content, where: content1 is just in ENG version, content2 is in ENG and ESP
     * version, content 3 is just in ESP version Should: If the page is requests in ENG version it
     * should be thrown a {@link HTMLPageAssetNotFoundException} If the page is requests in ESP
     * version it should be rendered with content3 and content2 (both in ESP version)
     */
    @UseDataProvider("cases")
    public void ContentFallbackFalse_PageFallbackTrue_PageSpanish_ViewEnglish404_ViewSpanishContent2And3(
            final TestContainerType containerType) throws Exception {

        TestContainerFactory containerFactory = TestContainerFactory.getInstance();

        final Container container = containerFactory.getContainer(containerType);
        final Template template = containerFactory.getTemplate(containerType);

        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, false);
        Config.setProperty(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE, true);

        final String pageName = "test3Page-" + System.currentTimeMillis();
        final HTMLPageAsset pageSpanishVersion = createHtmlPageAsset(template, pageName,
                spanishLanguage.getId());

        createMultiTree(pageSpanishVersion.getIdentifier(), container.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
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
        assertTrue("ESP = " + html, html.contains("content3Spacontent2Spa"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
        } catch (HTMLPageAssetNotFoundException e) {
            //expected
        }
    }

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to false and
     * DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to false And the page have version in ENG And the
     * page have tree content, where: content1 is just in ENG version, content2 is in ENG and ESP
     * version, content 3 is just in ESP version Should: If the page is requests in ENG version it
     * should be rendered with content1 and content2 If the page is requests in ESP version it should
     * be thrown a {@link HTMLPageAssetNotFoundException}
     */
    @UseDataProvider("cases")
    public void ContentFallbackFalse_PageFallbackFalse_PageEnglish_ViewEnglishContent1And2_ViewSpanish404(
            final TestContainerType containerType) throws Exception {

        TestContainerFactory containerFactory = TestContainerFactory.getInstance();

        final Container container = containerFactory.getContainer(containerType);
        final Template template = containerFactory.getTemplate(containerType);

        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, false);
        Config.setProperty(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE, false);

        final String pageName = "test4Page-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
        assertTrue("ENG = " + html, html.contains("content2content1"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
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
        } catch (HTMLPageAssetNotFoundException e) {
            //expected
        }
    }

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: A Page has a content that have an Image field and this content is
     * using a FileAsset Image Should: the [field_variable]ImageURI variable should be equals to
     * /contentAsset/raw-data/[content_id]/fileAsset?language_id=1
     */
    @Test
    public void renderPageWithFileAssetImage()
            throws DotDataException, DotSecurityException, WebAssetException {

        final Host host = new SiteDataGen().nextPersisted();

        final Field imageField = new FieldDataGen()
                .type(ImageField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .field(imageField)
                .nextPersisted();

        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final File image = new File(
                Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
                        .getResource("images/test.jpg")).getFile());
        final Contentlet imageContentlet = new FileAssetDataGen(folder, image)
                .host(host)
                .nextPersisted();
        ContentletDataGen.publish(imageContentlet);

        Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(imageField.variable(), imageContentlet.getIdentifier())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .nextPersisted();

        ContentletDataGen.publish(contentlet);

        final Container container = new ContainerDataGen()
                .withContentType(contentType,
                        String.format("Image URI: $!{%sImageURI}", imageField.variable()))
                .nextPersisted();
        ContainerDataGen.publish(container);

        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(container, UUID)
                .next();

        final Folder folderTheme = new FolderDataGen().nextPersisted();
        final Contentlet theme = new ThemeDataGen()
                .site(host)
                .themesFolder(folderTheme)
                .nextPersisted();

        final Template template = new TemplateDataGen()
                .theme(theme)
                .drawedBody(templateLayout)
                .nextPersisted();
        TemplateDataGen.publish(template);

        final String pageName = "renderPageWithFileAssetImage-" + System.currentTimeMillis();
        final HTMLPageAsset page = createHtmlPageAsset(template, pageName, 1);

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet)
                .setInstanceID(UUID)
                .nextPersisted();

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);

        final String imageURIExpected = String.format(
                "/contentAsset/raw-data/%s/fileAsset?language_id=1",
                imageContentlet.getIdentifier()
        );
        assertTrue(html, html.contains(String.format("Image URI: %s", imageURIExpected)));
    }


    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: A Page has a content that have an Image field and this content is
     * using a DotAsset Image Should: the [field_variable]ImageURI variable should be equals to
     * /contentAsset/raw-data/[content_id]/fileAsset?language_id=1
     */
    @Test
    public void renderPageWithDotAssetImage()
            throws DotDataException, DotSecurityException, WebAssetException {

        final Host host = new SiteDataGen().nextPersisted();

        final Field imageField = new FieldDataGen()
                .type(ImageField.class)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .field(imageField)
                .nextPersisted();

        final File image = new File(
                Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
                        .getResource("images/test.jpg")).getFile());

        final ContentType dotAssetContentType = APILocator.getContentTypeAPI(
                        APILocator.systemUser())
                .save(ContentTypeBuilder.builder(DotAssetContentType.class)
                        .folder(FolderAPI.SYSTEM_FOLDER)
                        .host(Host.SYSTEM_HOST)
                        .name("name" + System.currentTimeMillis())
                        .owner(APILocator.systemUser().getUserId())
                        .build());

        final Contentlet imageContentlet = new ContentletDataGen(dotAssetContentType)
                .setProperty(DotAssetContentType.ASSET_FIELD_VAR, image)
                .nextPersisted();
        ContentletDataGen.publish(imageContentlet);

        Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(imageField.variable(), imageContentlet.getIdentifier())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .nextPersisted();

        ContentletDataGen.publish(contentlet);

        final Container container = new ContainerDataGen()
                .withContentType(contentType,
                        String.format("Image URI: $!{%sImageURI}", imageField.variable()))
                .nextPersisted();
        ContainerDataGen.publish(container);

        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(container, UUID)
                .next();

        final Folder folderTheme = new FolderDataGen().nextPersisted();
        final Contentlet theme = new ThemeDataGen()
                .site(host)
                .themesFolder(folderTheme)
                .nextPersisted();

        final Template template = new TemplateDataGen()
                .theme(theme)
                .drawedBody(templateLayout)
                .nextPersisted();
        TemplateDataGen.publish(template);

        final String pageName = "renderPageWithFileAssetImage-" + System.currentTimeMillis();
        final HTMLPageAsset page = createHtmlPageAsset(template, pageName, 1);

        new MultiTreeDataGen()
                .setPage(page)
                .setContainer(container)
                .setContentlet(contentlet)
                .setInstanceID(UUID)
                .nextPersisted();

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);

        final String imageURIExpected = String.format(
                "dA/%s",
                imageContentlet.getIdentifier()
        );
        assertTrue(html, html.contains(String.format("Image URI: %s", imageURIExpected)));
    }

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to false and
     * DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to false And the page have version in ENG and ESP And
     * the page have tree content, where: content1 is just in ENG version, content2 is in ENG and
     * ESP version, content 3 is just in ESP version Should: If the page is requests in ENG version
     * it should be rendered with content1 and content2 If the page is requests in ESP version it
     * should be rendered with content3 and content2 (both in ESP version)
     */
    @Test
    @UseDataProvider("cases")
    public void ContentFallbackFalse_PageFallbackFalse_PageEnglishAndSpanish_ViewEnglishContent1And2_ViewSpanishContent2And3(
            final TestContainerType containerType) throws Exception {

        final TestContainerFactory containerFactory = TestContainerFactory.getInstance();

        final Container container = containerFactory.getContainer(containerType);
        final Template template = containerFactory.getTemplate(containerType);
        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, false);
        Config.setProperty(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE, false);

        final String pageName = "test5Page-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        Contentlet pageSpanishVersion = contentletAPI.checkout(pageEnglishVersion.getInode(),
                systemUser, false);
        pageSpanishVersion.setLanguageId(spanishLanguage.getId());
        pageSpanishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageSpanishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        pageSpanishVersion = contentletAPI.checkin(pageSpanishVersion, systemUser, false);
        pageSpanishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageSpanishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageSpanishVersion, systemUser, false);
        addAnonymousPermissions(pageSpanishVersion);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
        assertTrue("Expected text is NOT present in generated HTML = " + html, html.contains("content1") && html.contains("content2"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
        assertTrue("ESP = " + html, html.contains("content2Spa") && html.contains("content3Spa"));
    }

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to true and
     * DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to true And the page have version in ENG and ESP And
     * the page have tree content, where: content1 is just in ENG version, content2 is in ENG and
     * ESP version, content 3 is just in ESP version Should: If the page is requests in ENG version
     * it should be rendered with content1 and content2 If the page is requests in ESP version it
     * should be rendered with content1 (ENG version), content3 and content2 (both in ESP version)
     */
    @Test
    @UseDataProvider("cases")
    public void ContentFallbackTrue_PageFallbackTrue_PageEnglishAndSpanish_ViewEnglishContent1And2_ViewSpanishContent1And2And3(
            final TestContainerType containerType) throws Exception {

        TestContainerFactory containerFactory = TestContainerFactory.getInstance();

        final Container container = containerFactory.getContainer(containerType);
        final Template template = containerFactory.getTemplate(containerType);

        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, true);
        Config.setProperty(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE, true);

        final String pageName = "test6Page-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);
        Contentlet pageSpanishVersion = contentletAPI.checkout(pageEnglishVersion.getInode(),
                systemUser, false);

        pageSpanishVersion.setLanguageId(spanishLanguage.getId());
        pageSpanishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        pageSpanishVersion = contentletAPI.checkin(pageSpanishVersion, systemUser, false);

        pageSpanishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageSpanishVersion, systemUser, false);
        addAnonymousPermissions(pageSpanishVersion);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
        assertTrue("ENG = " + html, html.contains("content1") && html.contains("content2"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
        assertTrue("ESP = " + html, html.contains("content3Spa")
                && html.contains("content2Spa") && html.contains("content1"));
    }

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE is set to true and
     * DEFAULT_PAGE_TO_DEFAULT_LANGUAGE is set to false And the page have version just in ENG And
     * the page have tree content, where: content1 is just in ENG version, content2 is in ENG and
     * ESP version, content 3 is just in ESP version Should: If the page is requests in ENG version
     * it should be rendered with content1 and content2 If the page is requests in ESP version it
     * should be thrown a {@link HTMLPageAssetNotFoundException}
     */
    @UseDataProvider("cases")
    public void ContentFallbackTrue_PageFallbackFalse_PageEnglish_ViewEnglishContent1And2_ViewSpanish404(
            final TestContainerType containerType) throws Exception {

        TestContainerFactory containerFactory = TestContainerFactory.getInstance();

        final Container container = containerFactory.getContainer(containerType);
        final Template template = containerFactory.getTemplate(containerType);

        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, true);
        Config.setProperty(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE, false);

        final String pageName = "test7Page-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
        assertTrue("ENG = " + html, html.contains("content2content1"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
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
        } catch (HTMLPageAssetNotFoundException e) {
            //expected
        }
    }

    /**
     * This test creates a widget content type, sets a value for the widget code, creates a
     * contentlet of this new widget and add it to a page. If you update the value widget code, and
     * hit again the page the new value should show up.
     *
     * @throws Exception if something goes wrong
     */
    @Test
    @UseDataProvider("cases")
    public void constantField_notUpdatedCache_whenChanged(
            final TestContainerType containerType) throws Exception {

        TestContainerFactory containerFactory = TestContainerFactory.getInstance();

        final Container container = containerFactory.getContainer(containerType);
        final Template template = containerFactory.getTemplate(containerType);

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
                    .orElseThrow();
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
                    .languageId(1).pageURL("testPageWidget" + System.currentTimeMillis())
                    .title("testPageWidget")
                    .nextPersisted();
            pageEnglishVersion.setIndexPolicy(IndexPolicy.WAIT_FOR);
            pageEnglishVersion.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
            contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            contentletAPI.publish(pageEnglishVersion, systemUser, false);
            addAnonymousPermissions(pageEnglishVersion);

            final MultiTree multiTree = new MultiTree(pageEnglishVersion.getIdentifier(),
                    container.getIdentifier(),
                    contentlet.getIdentifier(), UUID, 0);
            APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

            HttpServletRequest mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(
                            new MockHttpRequestIntegrationTest("localhost", "/").request())
                            .request())
                    .request();
            when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
            assertTrue(html, html.contains("original code"));

            fields = contentType.fields();
            codeField = (ImmutableConstantField) fields.stream()
                    .filter(field -> field.name().equalsIgnoreCase("widget code")).findFirst()
                    .orElseThrow();
            codeField = codeField.withValues("this has been changed");
            APILocator.getContentTypeFieldAPI().save(codeField, systemUser);
            contentType = APILocator.getContentTypeAPI(systemUser).save(contentType);

            mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(
                            new MockHttpRequestIntegrationTest("localhost", "/").request())
                            .request())
                    .request();
            when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
            assertTrue(html, html.contains("this has been changed"));
        } finally {
            APILocator.getContentTypeAPI(systemUser).delete(contentType);
        }
    }

    /**
     * This test is for when you archived a container that is being used on page, the page needs to
     * be resolved without issues. And when you unarchived and publish the container, the page needs
     * to show the content related to the container.
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
                    new MockAttributeRequest(
                            new MockHttpRequestIntegrationTest("localhost", "/").request())
                            .request())
                    .request();
            when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
            assertTrue(html, html.contains("content1") && html.contains("content2"));

            WebAssetFactory.unLockAsset(container);
            WebAssetFactory.archiveAsset(container, systemUser);
            CacheLocator.getVeloctyResourceCache().clearCache();

            mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(
                            new MockHttpRequestIntegrationTest("localhost", "/").request())
                            .request())
                    .request();
            when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
            assertTrue(html, html.isEmpty());

            WebAssetFactory.unArchiveAsset(container);
            WebAssetFactory.publishAsset(container, systemUser);
            CacheLocator.getVeloctyResourceCache().clearCache();

            mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(
                            new MockHttpRequestIntegrationTest("localhost", "/").request())
                            .request())
                    .request();
            when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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
            assertTrue(html, html.contains("content1") && html.contains("content2"));
        } finally {
            if (!(container instanceof FileAssetContainer)) {
                WebAssetFactory.unArchiveAsset(container);
                WebAssetFactory.publishAsset(container, systemUser);
            }
        }
    }

    /**
     * Method to test:
     * {@link
     * com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl#getPageHtml(PageContext,
     * HttpServletRequest, HttpServletResponse)} Given Scenario: Create a page with three contents
     * for default persona, and 1 content to another persona ExpectedResult: The page should return
     * the content according to the persona set into the request
     *
     * @throws Exception if something goes wrong
     */
    @Test
    @UseDataProvider("cases")
    public void shouldReturnPageHTMLForPersona(  final TestContainerType containerType) throws Exception {

        TestContainerFactory containerFactory = TestContainerFactory.getInstance();

        final Container container = containerFactory.getContainer(containerType);
        final Template template = containerFactory.getTemplate(containerType);

        final String pageName = "test5Page-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        when(mockRequest.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(site);
        when(mockRequest.getRequestURI()).thenReturn(pageEnglishVersion.getURI());

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        final HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(visitor);

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        assertTrue(html, html.contains("content4"));

        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        assertTrue(html, html.contains("content1") && html.contains("content2"));

    }

    private HttpSession createHttpSession(final HttpServletRequest mockRequest) {
        final HttpSession session = mock(HttpSession.class);
        when(mockRequest.getSession()).thenReturn(session);
        when(mockRequest.getSession(false)).thenReturn(session);
        when(mockRequest.getSession(true)).thenReturn(session);
        return session;
    }

    /**
     * Method to test:
     * {@link
     * com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl#getPageHtml(PageContext,
     * HttpServletRequest, HttpServletResponse)} Given Scenario: Create a page with legacy UUID
     * ExpectedResult: The page should return the right HTML
     *
     * @throws Exception the exception
     */
    @Test
    @UseDataProvider("cases")
    public void shouldReturnPageHTMLForLegacyUUID(  final TestContainerType containerType) throws Exception {

        final TestContainerFactory containerFactory = TestContainerFactory.getInstance();

        final Container container = containerFactory.getContainer(containerType);

        final String containerId = container.getIdentifier();

        //Create a Template
        final Template template = new TemplateDataGen().title(
                        "PageContextBuilderTemplate" + System.currentTimeMillis())
                .withContainer(containerId, ContainerUUID.UUID_LEGACY_VALUE).nextPersisted();
        PublishFactory.publishAsset(template, systemUser, false, false);

        final String pageName = "testPage-" + System.currentTimeMillis();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).languageId(1)
                .pageURL(pageName).title(pageName).nextPersisted();
        page.setIndexPolicy(IndexPolicy.WAIT_FOR);
        page.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        page.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(page, systemUser, false);

        final String pageId = page.getIdentifier();
        MultiTree multiTree = new MultiTree(pageId, containerId, contentletsIds.get(0),
                ContainerUUID.UUID_START_VALUE, 0);
        APILocator.getMultiTreeAPI().saveMultiTreeAndReorder(multiTree);

        multiTree = new MultiTree(pageId, containerId, contentletsIds.get(1),
                ContainerUUID.UUID_START_VALUE, 0);
        APILocator.getMultiTreeAPI().saveMultiTreeAndReorder(multiTree);

        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        when(mockRequest.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(site);
        when(mockRequest.getRequestURI()).thenReturn(page.getURI());

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        final HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        final String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        final String generatedHtml = String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content2")
                + String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content1");
        assertEquals("The content in the page is NOT the expected one", html, generatedHtml);
    }

    /**
     * Method to test:
     * {@link
     * com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl#getPageHtml(PageContext,
     * HttpServletRequest, HttpServletResponse)} Given Scenario: Create a page with legacy UUID and
     * MultiTree ExpectedResult: The page should return the right HTML
     *
     * @throws Exception the exception
     */
    @Test
    @UseDataProvider("cases")
    public void shouldReturnPageHTMLForLegacyUUIDAndMultiTree(  final TestContainerType containerType) throws Exception {

        TestContainerFactory containerFactory = TestContainerFactory.getInstance();

        final Container container = containerFactory.getContainer(containerType);

        final String containerId = container.getIdentifier();

        //Create a Template
        final Template template = new TemplateDataGen().title(
                        "PageContextBuilderTemplate" + System.currentTimeMillis())
                .withContainer(containerId, ContainerUUID.UUID_LEGACY_VALUE).nextPersisted();
        PublishFactory.publishAsset(template, systemUser, false, false);

        final String pageName = "testPage-" + System.currentTimeMillis();
        final HTMLPageAsset page = new HTMLPageDataGen(folder, template).languageId(1)
                .pageURL(pageName).title(pageName).nextPersisted();
        page.setIndexPolicy(IndexPolicy.WAIT_FOR);
        page.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        page.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(page, systemUser, false);

        final String pageId = page.getIdentifier();
        MultiTree multiTree = new MultiTree(pageId, containerId, contentletsIds.get(0),
                ContainerUUID.UUID_LEGACY_VALUE, 0);
        APILocator.getMultiTreeAPI().saveMultiTreeAndReorder(multiTree);

        multiTree = new MultiTree(pageId, containerId, contentletsIds.get(1),
                ContainerUUID.UUID_LEGACY_VALUE, 0);
        APILocator.getMultiTreeAPI().saveMultiTreeAndReorder(multiTree);

        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        when(mockRequest.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(site);
        when(mockRequest.getRequestURI()).thenReturn(page.getURI());

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        final HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        final String expectedHtml = String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content2")
                + String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content1");
        assertEquals("Expected HTML code was NOT found", html, expectedHtml);
    }

    /**
     * Method to test:
     * {@link
     * com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl#getPageHtml(PageContext,
     * HttpServletRequest, HttpServletResponse)} Given Scenario: Create a page with parseContainer
     * directive into its template, and request the HTML in EDIT_MODE ExpectedResult: should return
     * a UUID with the 'dotParser_' prefix
     *
     * @throws Exception the exception
     */
    @Test
    @UseDataProvider("cases")
    public void shouldReturnParserContainerUUID(  final TestContainerType containerType) throws Exception {

        TestContainerFactory containerFactory = TestContainerFactory.getInstance();

        final Container container = containerFactory.getContainer(containerType);
        final Template template = containerFactory.getTemplate(containerType);

        boolean defaultContentToDefaultLangOriginalValue =
                Config.getBooleanProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, false);
        try {
            Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, true);
            final String pageName = "test5Page-" + System.currentTimeMillis();
            final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

            createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier());

            final HttpServletRequest mockRequest = createHttpServletRequest(pageEnglishVersion);

            final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

            final HttpSession session = createHttpSession(mockRequest);

            when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);

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

            assertTrue(html.matches(regexExpected));
        } finally {
            Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE,
                    defaultContentToDefaultLangOriginalValue);
        }
    }


    @DataProvider(format = "%m page Host: %p[0] Template Host: %p[1] Container Host: %p[2]")
    public static Object[][] fileContainerCases() {

        // These are ok to pass since it is a function and is lazy evaluated.
        final Function2<FileAssetContainer, Host, String> relativePath =
                (FileAssetContainer container, Host host) -> container.getPath();
        final Function2<FileAssetContainer, Host, String> absolutePath =
                (FileAssetContainer container, Host host) -> "//" + host.getName()
                        + container.getPath();

        final Function2<Host, String, Template> advanceTemplate =
                HTMLPageAssetRenderedTest::createAdvancedTemplate;
        final Function2<Host, String, Template> drawedTemplate =
                HTMLPageAssetRenderedTest::createDrawedTemplate;

        // Need to lazy process these in the test.
        final TestHostType anotherHost = TestHostType.ANOTHER;
        final TestHostType defaultHost = TestHostType.DEFAULT;
        final TestHostType currentHost = TestHostType.CURRENT;

        /* We are not using this mocked request ?
        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        when(request.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(currentHost);

         */

        return new Object[][]{
                {currentHost, currentHost, currentHost, relativePath, advanceTemplate, true},
                {currentHost, currentHost, anotherHost, relativePath, advanceTemplate, false},
                {currentHost, anotherHost, anotherHost, relativePath, advanceTemplate, false},
                {currentHost, currentHost, defaultHost, relativePath, advanceTemplate, true},
                {currentHost, anotherHost, defaultHost, relativePath, advanceTemplate, true},

                {currentHost, currentHost, currentHost, absolutePath, advanceTemplate, true},
                {currentHost, currentHost, anotherHost, absolutePath, advanceTemplate, true},
                {currentHost, anotherHost, anotherHost, absolutePath, advanceTemplate, true},
                {currentHost, currentHost, defaultHost, absolutePath, advanceTemplate, true},
                {currentHost, anotherHost, defaultHost, absolutePath, advanceTemplate, true},

                {currentHost, currentHost, currentHost, relativePath, drawedTemplate, true},
                {currentHost, currentHost, anotherHost, relativePath, drawedTemplate, false},
                {currentHost, anotherHost, anotherHost, relativePath, drawedTemplate, false},
                {currentHost, currentHost, defaultHost, relativePath, drawedTemplate, true},
                {currentHost, anotherHost, defaultHost, relativePath, drawedTemplate, true},

                {currentHost, currentHost, currentHost, absolutePath, drawedTemplate, true},
                {currentHost, currentHost, anotherHost, absolutePath, drawedTemplate, true},
                {currentHost, anotherHost, anotherHost, absolutePath, drawedTemplate, true},
                {currentHost, currentHost, defaultHost, absolutePath, drawedTemplate, true},
                {currentHost, anotherHost, defaultHost, absolutePath, drawedTemplate, true}
        };
    }

    /**
     * Method to test:
     * {@link
     * com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetRenderedAPIImpl#getPageHtml(PageContext,
     * HttpServletRequest, HttpServletResponse)} Given Scenario: - Template, FileContainer and Page
     * in the same site or different site - Using Advance Template or not Advance Template - Using
     * relative or absolute path ExpectedResult: should work
     *
     * @throws Exception if something goes wrong
     */
    @Test
    @UseDataProvider("fileContainerCases")
    public void shouldRenderTemplateAndContainers(
            final TestHostType pageHostType,
            final TestHostType templateHostType,
            final TestHostType containerHostType,
            final Function2<FileAssetContainer, Host, String> pathConverter,
            final Function2<Host, String, Template> templateCreator,
            final boolean shouldWork)
            throws Exception {

        TestHostFactory hostFactory = TestHostFactory.getInstance();

        final Host pageHost = hostFactory.getHost(pageHostType);
        final Host templateHost = hostFactory.getHost(templateHostType);
        final Host containerHost = hostFactory.getHost(containerHostType);


        final FileAssetContainer container = createFileContainer(containerHost);
        final String path = pathConverter.apply(container, containerHost);
        final Template template = templateCreator.apply(templateHost, path);
        PublishFactory.publishAsset(template, systemUser, false, false);

        final String pageName = "testPage-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(pageHost, template, pageName,
                1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier(),
                template.isDrawed() ? ContainerUUID.UUID_START_VALUE :
                        ParseContainer.getDotParserContainerUUID(ContainerUUID.UUID_START_VALUE));

        final HttpServletRequest mockRequest = createHttpServletRequest(pageEnglishVersion);
        when(mockRequest.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(
                PageMode.LIVE.toString());

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final HttpSession session = createHttpSession(mockRequest);
        when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);
        when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(systemUser);

        final String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);

        if (shouldWork) {
            assertTrue(
                    String.format("Should has content: using %s path and %s Template", path,
                            template.isDrawed() ? "Drawed" : "Advanced"),
                    html.contains("content1") && html.contains("content2")
            );
        } else {
            Assert.assertFalse(
                    String.format("Should has content: using %s path and %s Template", path,
                            template.isDrawed() ? "Drawed" : "Advanced"),
                    html.contains("content1") && html.contains("content2")
            );
        }
    }

    private static Template createAdvancedTemplate(
            final Host templateHost,
            final String containerPath) {

        return new TemplateDataGen().title(
                        "PageContextBuilderTemplate" + System.currentTimeMillis())
                .host(templateHost)
                .withContainer(containerPath, ContainerUUID.UUID_START_VALUE)
                .nextPersisted();
    }

    private static Template createDrawedTemplate(final Host host, final String containerPath) {
        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(containerPath)
                .next();

        final Contentlet contentlet = new ThemeDataGen().nextPersisted();
        return new TemplateDataGen()
                .title("PageContextBuilderTemplate" + System.currentTimeMillis())
                .host(host)
                .drawedBody(templateLayout)
                .theme(contentlet)
                .nextPersisted();
    }

    //Data Provider for the Widget Pre-execute code test
    @DataProvider
    public static Object[] dataProviderWidgetPreExecuteCode() {
        return new Object[]{
                new WidgetPreExecuteCodeTestCase(PageMode.EDIT_MODE),
                new WidgetPreExecuteCodeTestCase(PageMode.PREVIEW_MODE),
                new WidgetPreExecuteCodeTestCase(PageMode.LIVE),
        };
    }

    public static class WidgetPreExecuteCodeTestCase {

        final PageMode pageMode;

        public WidgetPreExecuteCodeTestCase(final PageMode pageMode) {
            this.pageMode = pageMode;
        }

        @Override
        public String toString() {
            return "WidgetPreExecuteCodeTestCase{" +
                    "pageMode=" + pageMode +
                    '}';
        }
    }

    @Test
    @UseDataProvider("dataProviderWidgetPreExecuteCode")
    public void test_WidgetPreExecuteCodeShowRegardlessPageMode(
            final WidgetPreExecuteCodeTestCase testCase) throws Exception {

        boolean defaultContentToDefaultLangOriginalValue =
                Config.getBooleanProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, false);
        try {

            Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, true);
            //Update the preExecute Field to have some code in it
            final String preExecuteCode = "PreExecute Code Displayed";
            ContentType contentType = TestDataUtils.getWidgetLikeContentType();
            final Field preExecuteField = APILocator.getContentTypeFieldAPI()
                    .byContentTypeIdAndVar(contentType.id(),
                            WidgetContentType.WIDGET_PRE_EXECUTE_FIELD_VAR);
            APILocator.getContentTypeFieldAPI()
                    .save(FieldBuilder.builder(preExecuteField).values(preExecuteCode).build(),
                            systemUser);
            // Assert that the widget has set the pre-execute field
            assertEquals(preExecuteCode, APILocator.getContentTypeFieldAPI()
                    .byContentTypeIdAndVar(contentType.id(),
                            WidgetContentType.WIDGET_PRE_EXECUTE_FIELD_VAR)
                    .values());

            //Create Contentlet
            final Contentlet widgetContentlet = TestDataUtils.getWidgetContent(true, 1,
                    contentType.id());
            addAnonymousPermissions(widgetContentlet);

            //Create Container, Template and Page
            final Container container = createContainer();
            final Template template = createTemplate(container);

            final String pageName = "testPage-" + System.currentTimeMillis();
            final HTMLPageAsset page = createHtmlPageAsset(template, pageName, 1);

            //Add contentlet to the page
            MultiTree multiTree = new MultiTree(page.getIdentifier(), container.getIdentifier(),
                    widgetContentlet.getIdentifier(), UUID, 0);
            APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

            //Request page
            final HttpServletRequest mockRequest = createHttpServletRequest(page);
            final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
            final HttpSession session = createHttpSession(mockRequest);
            when(session.getAttribute(WebKeys.VISITOR)).thenReturn(null);
            when(session.getAttribute(WebKeys.CMS_USER)).thenReturn(systemUser);
            final String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                    PageContextBuilder.builder()
                            .setUser(systemUser)
                            .setPageUri(page.getURI())
                            .setPageMode(testCase.pageMode)
                            .build(),
                    mockRequest, mockResponse);

            //Page HTML must contain the pre-execute code
            assertTrue("Page Mode: " + testCase.pageMode + " html: " + html,
                    html.contains(preExecuteCode));
        } finally {
            Config.setProperty("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE",
                    defaultContentToDefaultLangOriginalValue);
        }
    }

    @NotNull
    private HttpServletRequest createHttpServletRequest(HTMLPageAsset pageEnglishVersion)
            throws DotDataException {
        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        when(mockRequest.getAttribute(WebKeys.CURRENT_HOST)).thenReturn(site);
        when(mockRequest.getRequestURI()).thenReturn(pageEnglishVersion.getURI());
        when(mockRequest.getParameter(WebKeys.PAGE_MODE_PARAMETER)).thenReturn(
                PageMode.EDIT_MODE.toString());
        when(mockRequest.getAttribute(com.liferay.portal.util.WebKeys.USER)).thenReturn(systemUser);
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

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: A container is added twice in a page Should: render the pge
     */
    @Test
    public void containerTwiceIntoPage() throws Exception {

        final Container container = createFileContainer();
        final Template template = createTemplate(
                new TestContainerUUID(container, "1"),
                new TestContainerUUID(container, "2")
        );

        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, false);
        Config.setProperty(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE, true);

        final String pageName = "test1Page-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier(), "1");
        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier(), "2");

        final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI()
                .getContainerMultiTrees(container.getIdentifier());
        assertNotNull(multiTrees);
        assertEquals(8, multiTrees.size());

        //request page ENG version
        final HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        // These are the contents of the repeated Containers: [ content1, content2 ] must be present twice
        final String expectedHtml = String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content1")
                + String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content2")
                + String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content1")
                + String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content2");
        assertEquals("Container added twice is NOT showing the expected HTML code", expectedHtml, html);
    }

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: A container is added twice in a page using a TemplateLayout Should:
     * render the pge
     */
    @Test
    public void containerTwiceIntoPageAndTemplateLayout() throws Exception {

        final FileAssetContainer container = createFileContainer();
        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(container.getPath())
                .withContainer(container.getPath())
                .next();

        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .host(site)
                .drawedBody(templateLayout)
                .theme(theme)
                .nextPersisted();

        Config.setProperty(DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE, false);
        Config.setProperty(DEFAULT_PAGE_TO_DEFAULT_LANGUAGE, true);

        final String pageName = "test1Page-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier(), "1");
        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier(), "2");

        final List<MultiTree> multiTrees = APILocator.getMultiTreeAPI()
                .getContainerMultiTrees(container.getIdentifier());
        assertNotNull(multiTrees);
        assertEquals(8, multiTrees.size());

        //request page ENG version
        final HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        final String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(pageEnglishVersion.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        // These are the contents of the repeated Containers: [ content1, content2 ] must be present twice
        final String generatedHtml = String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content1")
                + String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content2")
                + String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content1")
                + String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content2");
        assertTrue("The content in the page is NOT the expected one", html.contains(generatedHtml));
    }

    @Test
    public void pageWithSidebarShouldRender() throws Exception {

        final FileAssetContainer container = createFileContainer();
        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(container.getPath(), "1")
                .withContainerInSidebar(container.getPath(), "2")
                .next();

        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .host(site)
                .drawedBody(templateLayout)
                .theme(theme)
                .nextPersisted();

        final String pageName = "test_page_with_sidebar-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(template, pageName, 1);

        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier(), "1");
        createMultiTree(pageEnglishVersion.getIdentifier(), container.getIdentifier(), "2");

        //request page ENG version
        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
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

        final String toFind = String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content1")
                + String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, "content2");

        final int firstIndex = html.indexOf(toFind);
        final int secondIndex = html.indexOf(toFind, firstIndex + toFind.length());
        final int thirdIndex = html.indexOf(toFind, secondIndex + toFind.length());

        assertTrue("Expected HTML code was NOT found", firstIndex != -1 && secondIndex != -1 && firstIndex != secondIndex
                && thirdIndex == -1);
    }

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: A page has a template that is a file template design Should:
     * render the page
     */
    @Test
    public void pageWithFileTemplateDesignShouldRender() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();

        final FileAssetTemplate fileAssetTemplate = new TemplateAsFileDataGen()
                .host(host)
                .nextPersisted();

        final String pageName = "test_page_with_filetemplatedesign-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(host, fileAssetTemplate,
                pageName, 1);

        //request page ENG version
        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(host.getIdentifier());
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

        assertNotNull(html);
    }

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: A page has a template that is a file template advanced Should:
     * render the page
     */
    @Test
    public void pageWithFileTemplateAdvancedShouldRender() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();

        final FileAssetTemplate fileAssetTemplate = new TemplateAsFileDataGen()
                .designTemplate(false)
                .host(host)
                .nextPersisted();

        final String pageName = "test_page_with_filetemplateadvanced-" + System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = createHtmlPageAsset(host, fileAssetTemplate,
                pageName, 1);

        //request page ENG version
        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                .request();
        when(mockRequest.getParameter("host_id")).thenReturn(host.getIdentifier());


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

        assertNotNull(html);
    }

    /**
     * Method to test:
     * {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest,
     * HttpServletResponse)} When: A page has multiple personas version, and the container has a MAx
     * contentlet equals to 1 Should: render the page
     */
    @Test
    public void pageWithMultiplePersona() throws Exception {

        final Container container = createContainer(1);

        final TemplateLayout templateLayout = new TemplateLayoutDataGen()
                .withContainer(container)
                .next();

        final Contentlet theme = new ThemeDataGen().nextPersisted();
        final Template template = new TemplateDataGen()
                .host(site)
                .drawedBody(templateLayout)
                .theme(theme)
                .nextPersisted();

        final String pageName = "test1Page-" + System.currentTimeMillis();
        final HTMLPageAsset page = createHtmlPageAsset(template, pageName, 1);

        final Persona defaultPersona = APILocator.getPersonaAPI().getDefaultPersona();
        final Persona notDefaultPersona = new PersonaDataGen().nextPersisted();

        final Contentlet defaultPersonaContentlet = new ContentletDataGen(contentGenericType.id())
                .languageId(1)
                .folder(folder)
                .host(site)
                .setProperty("title", "content1")
                .setProperty("body", String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, defaultPersona.getKeyTag()))
                .nextPersisted();

        final Contentlet notDefaultPersonaContentlet = new ContentletDataGen(
                contentGenericType.id())
                .languageId(1)
                .folder(folder)
                .host(site)
                .setProperty("title", "content1")
                .setProperty("body", String.format(TestDataUtils.BLOCK_EDITOR_DUMMY_CUSTOM_CONTENT, notDefaultPersona.getKeyTag()))
                .nextPersisted();

        ContentletDataGen.publish(page);
        ContentletDataGen.publish(defaultPersonaContentlet);
        ContentletDataGen.publish(notDefaultPersonaContentlet);

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(page)
                .setContentlet(defaultPersonaContentlet)
                .setInstanceID("1")
                .nextPersisted();

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(page)
                .setContentlet(notDefaultPersonaContentlet)
                .setPersona(notDefaultPersona)
                .setInstanceID("1")
                .nextPersisted();

        //request page ENG version
        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request()).request()
        ).request();

        when(mockRequest.getParameter("host_id")).thenReturn(site.getIdentifier());
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        final String htmlWithDefaultPersona = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(
                PageContextBuilder.builder()
                        .setUser(systemUser)
                        .setPageUri(page.getURI())
                        .setPageMode(PageMode.LIVE)
                        .build(),
                mockRequest, mockResponse);
        assertTrue(htmlWithDefaultPersona.contains("dot:persona"));

        final Visitor visitor = new Visitor();
        visitor.setPersona(notDefaultPersona);
        mockRequest.getSession().setAttribute(WebKeys.VISITOR, visitor);

        final String htmlWithNoDefaultPersona = APILocator.getHTMLPageAssetRenderedAPI()
                .getPageHtml(
                        PageContextBuilder.builder()
                                .setUser(systemUser)
                                .setPageUri(page.getURI())
                                .setPageMode(PageMode.LIVE)
                                .build(),
                        mockRequest, mockResponse);
        assertTrue(htmlWithNoDefaultPersona.contains(notDefaultPersona.getKeyTag()));
    }

    private static class TestContainerUUID {

        Container container;
        String UUID;

        public TestContainerUUID(final Container container, final String UUID) {
            this.container = container;
            this.UUID = UUID;
        }
    }

    public enum TestContainerType {
        DEFAULT, FILE
    }
    private static class TestContainerFactory {

        private static final TestContainerFactory INSTANCE = new TestContainerFactory();
        private final Map<TestContainerType, Container> containerMap;
        private final Map<TestContainerType, Template> templateMap;

        public static TestContainerFactory getInstance() {
            return TestContainerFactory.INSTANCE;
        }

        TestContainerFactory() {
            Logger.info(TestContainerFactory.class, "Creating TestContainerFactory");
            containerMap = new HashMap<>();
            containerMap.put(TestContainerType.DEFAULT, Sneaky.sneak(
                    HTMLPageAssetRenderedTest::createContainer));
            containerMap.put(TestContainerType.FILE, Sneaky.sneak(
                    HTMLPageAssetRenderedTest::createFileContainer));

            templateMap = new HashMap<>();
            templateMap.put(TestContainerType.DEFAULT, Sneaky.sneak(() -> createTemplate(containerMap.get(TestContainerType.DEFAULT))));
            templateMap.put(TestContainerType.FILE,Sneaky.sneak(() -> createTemplate(containerMap.get(TestContainerType.FILE))));
        }

        Container getContainer(TestContainerType type) {
            if (type == null) {
                throw new IllegalArgumentException("Invalid container type");
            }
            return containerMap.get(type);
        }

        Template getTemplate(TestContainerType type) {
            if (type == null) {
                throw new IllegalArgumentException("Invalid container type");
            }
            return templateMap.get(type);
        }
    }
    private enum TestHostType {
        CURRENT, ANOTHER, DEFAULT
    }

    private static class TestHostFactory {
        // This works like a Holder and is initialized lazily on class load
        private static final TestHostFactory INSTANCE = new TestHostFactory();

        private final Map<TestHostType, Host> hostMap = new HashMap<>();

        public static TestHostFactory getInstance() {
            return TestHostFactory.INSTANCE;
        }

        private TestHostFactory() {
            Logger.info(TestContainerFactory.class, "Creating TestHostFactory");
            hostMap.put(TestHostType.CURRENT, site);
            hostMap.put(TestHostType.ANOTHER, new SiteDataGen().nextPersisted());
            hostMap.put(TestHostType.DEFAULT, Sneaky.sneak(() ->
                    APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), true)));
        }

        public Host getHost(TestHostType testType) {
            if (!hostMap.containsKey(testType)) {
                throw new IllegalArgumentException("Invalid host type");
            }
            return hostMap.get(testType);
        }
    }

    /**
     * Method to test: {@link HTMLPageAssetRenderedAPI#getPageHtml(PageContext, HttpServletRequest, HttpServletResponse)}
     * When: The page includes more than one script block regardless of the casing (lowercase, uppercase)
     * Should: Escape de close script like this <code></>\</script/></code>
     * @throws Exception
     */
    @Test
    public void renderPageWithScriptAndInnerNavigateMode() throws Exception {

        final String widgetCode = "<html>\n" +
                "This is a test\n" +
                "</html>\n" +
                "<script>console.log(\"AAAA\");</SCRIPT>\n" +
                "<script>console.log(\"BBB\");</script>\n";

        final Language language = new LanguageDataGen().nextPersisted();
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType widgetContentType = new ContentTypeDataGen()
                .createWidgetContentType(widgetCode)
                .nextPersisted();

        final Contentlet widget = new ContentletDataGen(widgetContentType)
                .host(host)
                .languageId(1)
                .setProperty("widgetTitle", "Testing NavigateEditModeWithScriptTag")
                .nextPersistedAndPublish();

        final Container container = new ContainerDataGen().nextPersisted();
        final Template template = new TemplateDataGen().withContainer(container.getIdentifier()).nextPersisted();

        ContainerDataGen.publish(container);
        TemplateDataGen.publish(template);
        
        final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(host, template)
                .languageId(1).pageURL("testPageWidget" + System.currentTimeMillis())
                .title("testPageWidget")
                .nextPersisted();

        contentletAPI.publish(pageEnglishVersion, systemUser, false);
        addAnonymousPermissions(pageEnglishVersion);

        final MultiTree multiTree = new MultiTreeDataGen()
                .setPage(pageEnglishVersion)
                .setContainer(container)
                .setContentlet(widget)
                .nextPersisted();


        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(
                        new MockHttpRequestIntegrationTest("localhost", "/").request())
                        .request())
                .request();

        when(mockRequest.getRequestURI()).thenReturn(pageEnglishVersion.getURI());
        when(mockRequest.getParameter("host_id")).thenReturn(host.getIdentifier());
        mockRequest.setAttribute(com.liferay.portal.util.WebKeys.USER, systemUser);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);

        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        final String html = APILocator.getHTMLPageAssetRenderedAPI()
                .getPageHtml(
                        PageContextBuilder.builder()
                                .setUser(systemUser)
                                .setPageUri(pageEnglishVersion.getURI())
                                .setPageMode(PageMode.NAVIGATE_EDIT_MODE)
                                .build(),
                        mockRequest, mockResponse);
        final String expected = "\"rendered\" : \"<div><html>\\nThis is a test\\n</html>\\n<script>console.log(\\\"AAAA\\\");\\</script\\>\\n<script>console.log(\\\"BBB\\\");\\</script\\>\\n</div>\"";
        assertTrue(html.contains(expected));



    }
}
