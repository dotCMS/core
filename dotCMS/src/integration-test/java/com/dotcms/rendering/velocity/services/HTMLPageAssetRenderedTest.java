package com.dotcms.rendering.velocity.services;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.PublishFactory;
import com.dotmarketing.factories.WebAssetFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.render.HTMLPageAssetNotFoundException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.*;
import com.liferay.portal.ejb.AddressPool;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;

public class HTMLPageAssetRenderedTest {

    private static String contentGenericId;
    private static String containerId;
    private static Template template;
    private static User systemUser;
    private static final String contentFallbackProperty = "DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE";
    private static final String pageFallbackProperty = "DEFAULT_PAGE_TO_DEFAULT_LANGUAGE";
    private static final boolean contentFallbackDefaultValue = Config.getBooleanProperty(contentFallbackProperty,false);
    private static final boolean pageFallbackDefaultValue =Config.getBooleanProperty(pageFallbackProperty,true);
    private static Folder folder;
    private static final List<String> contentletsIds = new ArrayList<String>();
    private static ContentletAPI contentletAPI;
    private static final String UUID = UUIDGenerator.generateUuid();


    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.systemUser();
        contentletAPI = APILocator.getContentletAPI();
        createTestPage();

    }

    private static void createTestPage() throws Exception{
        //Create test folder
        folder =  new FolderDataGen().nextPersisted();

        //Get ContentGeneric Content-Type
        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(systemUser);
        final ContentType contentGenericType = contentTypeAPI.find("webPageContent");
        contentGenericId = contentGenericType.id();

        //Get a Container that includes ContentGeneric
        final List<Container> container = APILocator.getContainerAPI().findContainersForStructure(contentGenericId,true);
        containerId = container.get(0).getIdentifier();

        //Create a Template
        template = new TemplateDataGen().title("PageContextBuilderTemplate"+System.currentTimeMillis())
                .withContainer(containerId,UUID).nextPersisted();
        PublishFactory.publishAsset(template, systemUser, false, false);

        //Create Contentlet in English
        final Contentlet contentlet1 = new ContentletDataGen(contentGenericId)
                .languageId(1)
                .setProperty("title", "content1")
                .setProperty("body", "content1")
                .nextPersisted();

        contentlet1.setIndexPolicy(IndexPolicy.FORCE);
        contentlet1.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet1.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet1, systemUser, false);
        contentletsIds.add(contentlet1.getIdentifier());

        //Create Contentlet with English and Spanish Versions
        final Contentlet contentlet2English = new ContentletDataGen(contentGenericId)
                .languageId(1)
                .setProperty("title", "content2")
                .setProperty("body", "content2")
                .nextPersisted();

        contentlet2English.setIndexPolicy(IndexPolicy.FORCE);
        contentlet2English.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet2English.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet2English, systemUser, false);

        Contentlet contentlet2Spanish = contentletAPI.find(contentlet2English.getInode(), systemUser, false);
        contentlet2Spanish.setProperty("title","content2Spa");
        contentlet2Spanish.setProperty("body","content2Spa");
        contentlet2Spanish.setInode("");
        contentlet2Spanish.setLanguageId(2);
        contentlet2Spanish.setIndexPolicy(IndexPolicy.FORCE);
        contentlet2Spanish.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet2Spanish.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentlet2Spanish = contentletAPI.checkin(contentlet2Spanish, systemUser, false);

        contentlet2Spanish.setIndexPolicy(IndexPolicy.FORCE);
        contentlet2Spanish.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet2Spanish.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet2Spanish, systemUser, false);
        contentletsIds.add(contentlet2English.getIdentifier());

        //Create Contentlet in Spanish
        final Contentlet contentlet3 = new ContentletDataGen(contentGenericId)
                .languageId(2)
                .setProperty("title", "content3")
                .setProperty("body", "content3")
                .nextPersisted();

        contentlet3.setIndexPolicy(IndexPolicy.FORCE);
        contentlet3.setIndexPolicyDependencies(IndexPolicy.FORCE);
        contentlet3.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(contentlet3, systemUser, false);
        contentletsIds.add(contentlet3.getIdentifier());
    }


    private void createMultiTree(final String pageId) throws DotSecurityException, DotDataException {

        MultiTree multiTree = new MultiTree(pageId, containerId, contentletsIds.get(0),UUID,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        multiTree = new MultiTree(pageId, containerId, contentletsIds.get(1),UUID,0);
        APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

        multiTree = new MultiTree(pageId, containerId, contentletsIds.get(2),UUID,0);
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

        if(template != null){
            APILocator.getTemplateAPI().delete(template,systemUser,false);
        }

        for(final String contentletId : contentletsIds){
            final Contentlet contentlet = contentletAPI.findContentletByIdentifierAnyLanguage(contentletId);

            contentlet.setIndexPolicy(IndexPolicy.FORCE);
            contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
            contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);

            contentletAPI.unpublish(contentlet,systemUser,false);
            contentletAPI.archive(contentlet,systemUser,false);
            contentletAPI.delete(contentlet,systemUser,false);
        }

    }

    /**
     * ContentFallback False
     * PageFallback True
     *
     * Page English
     *
     * English -> 1 & 2
     * Spanish -> 2 & 3
     *
     */
    @Test
    public void ContentFallbackFalse_PageFallbackTrue_PageEnglish_ViewEnglishContent1And2_ViewSpanishContent2And3() throws Exception{
        System.out.println("---------------------------");
        Config.setProperty(contentFallbackProperty,false);
        Config.setProperty(pageFallbackProperty,true);

        final String pageName = "test1Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder,template).languageId(1).pageURL(pageName).title(pageName).nextPersisted();
        pageEnglishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageEnglishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageEnglishVersion, systemUser, false);

        createMultiTree(pageEnglishVersion.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        Logger.error(this,"TEST Page Identifier: " + pageEnglishVersion.getIdentifier());
        Logger.error(this,"TEST GetMultitree True: " + APILocator.getMultiTreeAPI().getPageMultiTrees(pageEnglishVersion,true));
        Logger.error(this,"TEST GetMultitree False: " + APILocator.getMultiTreeAPI().getPageMultiTrees(pageEnglishVersion,false));

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(), PageMode.PREVIEW_MODE);
        Assert.assertTrue("ENG = "+html , html.contains("content2content1"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "2");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(), PageMode.PREVIEW_MODE);
        Assert.assertTrue("ESP = "+html , html.contains("content3content2Spa"));

        System.out.println("---------------------------");
    }

    /**
     * ContentFallback False
     * PageFallback True
     *
     * Page English & Spanish
     *
     * English -> 1 & 2
     * Spanish -> 2 & 3
     *
     */

    public void ContentFallbackFalse_PageFallbackTrue_PageEnglishAndSpanish_ViewEnglishContent1And2_ViewSpanishContent2And3() throws Exception{

        Config.setProperty(contentFallbackProperty,false);
        Config.setProperty(pageFallbackProperty,true);

        final String pageName = "test2Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder,template).languageId(1).pageURL(pageName).title(pageName).nextPersisted();
        pageEnglishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageEnglishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageEnglishVersion, systemUser, false);
        Contentlet pageSpanishVersion = contentletAPI.find(pageEnglishVersion.getInode(),systemUser,false);
        pageSpanishVersion.setInode("");
        pageSpanishVersion.setLanguageId(2);
        pageSpanishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageSpanishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        pageSpanishVersion = contentletAPI.checkin(pageSpanishVersion,systemUser,false);

        pageSpanishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageSpanishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageSpanishVersion,systemUser,false);

        createMultiTree(pageEnglishVersion.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        Logger.error(this,"TEST Page Identifier: " + pageEnglishVersion.getIdentifier());
        Logger.error(this,"TEST GetMultitree True: " + APILocator.getMultiTreeAPI().getPageMultiTrees(pageEnglishVersion,true));
        Logger.error(this,"TEST GetMultitree False: " + APILocator.getMultiTreeAPI().getPageMultiTrees(pageEnglishVersion,false));

        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(), PageMode.PREVIEW_MODE);
        Assert.assertTrue("ENG = "+html , html.contains("content2content1"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "2");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(), PageMode.PREVIEW_MODE);
        Assert.assertTrue("ESP = "+html , html.contains("content3content2Spa"));

    }

    /**
     * ContentFallback False
     * PageFallback True
     *
     * Page Spanish
     *
     * English -> 404
     * Spanish -> 2 & 3
     *
     * @throws Exception
     */
//    @Test (expected = HTMLPageAssetNotFoundException.class)
    public void ContentFallbackFalse_PageFallbackTrue_PageSpanish_ViewEnglish404_ViewSpanishContent2And3() throws Exception{

        Config.setProperty(contentFallbackProperty,false);
        Config.setProperty(pageFallbackProperty,true);

        final String pageName = "test3Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageSpanishVersion = new HTMLPageDataGen(folder,template).languageId(2).pageURL(pageName).title(pageName).nextPersisted();
        pageSpanishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageSpanishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageSpanishVersion, systemUser, false);

        createMultiTree(pageSpanishVersion.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "2");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageSpanishVersion.getURI(), PageMode.PREVIEW_MODE);
        Assert.assertTrue("ESP = "+html , html.contains("content3content2Spa"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageSpanishVersion.getURI(), PageMode.PREVIEW_MODE);
    }

    /**
     * ContentFallback False
     * PageFallback False
     *
     * Page English
     *
     * English -> 1 & 2
     * Spanish -> 404
     *
     */
//    @Test (expected = HTMLPageAssetNotFoundException.class)
    public void ContentFallbackFalse_PageFallbackFalse_PageEnglish_ViewEnglishContent1And2_ViewSpanish404() throws Exception{

        Config.setProperty(contentFallbackProperty,false);
        Config.setProperty(pageFallbackProperty,false);

        final String pageName = "test4Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder,template).languageId(1).pageURL(pageName).title(pageName).nextPersisted();
        pageEnglishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageEnglishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageEnglishVersion, systemUser, false);

        createMultiTree(pageEnglishVersion.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(), PageMode.PREVIEW_MODE);
        Assert.assertTrue("ENG = "+html , html.contains("content2content1"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "2");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(), PageMode.PREVIEW_MODE);
    }

    /**
     * ContentFallback False
     * PageFallback False
     *
     * Page English & Spanish
     *
     * English -> 1 & 2
     * Spanish -> 2 & 3
     *
     */
//    @Test
    public void ContentFallbackFalse_PageFallbackFalse_PageEnglishAndSpanish_ViewEnglishContent1And2_ViewSpanishContent2And3() throws Exception{

        Config.setProperty(contentFallbackProperty,false);
        Config.setProperty(pageFallbackProperty,false);

        final String pageName = "test5Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder,template).languageId(1).pageURL(pageName).title(pageName).nextPersisted();
        pageEnglishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageEnglishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageEnglishVersion, systemUser, false);
        Contentlet pageSpanishVersion = contentletAPI.find(pageEnglishVersion.getInode(),systemUser,false);
        pageSpanishVersion.setInode("");
        pageSpanishVersion.setLanguageId(2);
        pageSpanishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageSpanishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        pageSpanishVersion = contentletAPI.checkin(pageSpanishVersion,systemUser,false);
        pageSpanishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageSpanishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageSpanishVersion,systemUser,false);

        createMultiTree(pageEnglishVersion.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(), PageMode.PREVIEW_MODE);
        Assert.assertTrue("ENG = "+html , html.contains("content2content1"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "2");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(), PageMode.PREVIEW_MODE);
        Assert.assertTrue("ESP = "+html , html.contains("content3content2Spa"));
    }

    /**
     * ContentFallback True
     * PageFallback True
     *
     * Page English & Spanish
     *
     * English -> 1 & 2
     * Spanish -> 1 & 2 & 3
     *
     */
//    @Test
    public void ContentFallbackTrue_PageFallbackTrue_PageEnglishAndSpanish_ViewEnglishContent1And2_ViewSpanishContent1And2And3() throws Exception{

        Config.setProperty(contentFallbackProperty,true);
        Config.setProperty(pageFallbackProperty,true);

        final String pageName = "test6Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder,template).languageId(1).pageURL(pageName).title(pageName).nextPersisted();
        pageEnglishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageEnglishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageEnglishVersion, systemUser, false);
        Contentlet pageSpanishVersion = contentletAPI.find(pageEnglishVersion.getInode(),systemUser,false);

        pageSpanishVersion.setInode("");
        pageSpanishVersion.setLanguageId(2);
        pageSpanishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        pageSpanishVersion = contentletAPI.checkin(pageSpanishVersion,systemUser,false);

        pageSpanishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageSpanishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageSpanishVersion,systemUser,false);

        createMultiTree(pageEnglishVersion.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(), PageMode.PREVIEW_MODE);
        Assert.assertTrue("ENG = "+html , html.contains("content2content1"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "2");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(), PageMode.PREVIEW_MODE);
        Assert.assertTrue("ESP = "+html , html.contains("content3content2Spacontent1"));
    }

    /**
     * ContentFallback True
     * PageFallback False
     *
     * Page English
     *
     * English -> 1 & 2
     * Spanish -> 404
     *
     */
//    @Test (expected = HTMLPageAssetNotFoundException.class)
    public void ContentFallbackTrue_PageFallbackFalse_PageEnglish_ViewEnglishContent1And2_ViewSpanish404() throws Exception{

        Config.setProperty(contentFallbackProperty,true);
        Config.setProperty(pageFallbackProperty,false);

        final String pageName = "test7Page-"+System.currentTimeMillis();
        final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder,template).languageId(1).pageURL(pageName).title(pageName).nextPersisted();
        pageEnglishVersion.setIndexPolicy(IndexPolicy.FORCE);
        pageEnglishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
        pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        contentletAPI.publish(pageEnglishVersion, systemUser, false);

        createMultiTree(pageEnglishVersion.getIdentifier());

        HttpServletRequest mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        String html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(), PageMode.PREVIEW_MODE);
        Assert.assertTrue("ENG = "+html , html.contains("content2content1"));

        mockRequest = new MockSessionRequest(
                new MockAttributeRequest(new MockHttpRequest("localhost", "/").request()).request())
                .request();
        mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "2");
        HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
        html = APILocator.getHTMLPageAssetRenderedAPI().getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(), PageMode.PREVIEW_MODE);
    }

    /**
     * This test creates a widget content type, sets a value for the widget code,
     * creates a contentlet of this new widget and add it to a page.
     * If you update the value widget code, and hit again the page the new value should show up.
     *
     * @throws Exception
     */
//    @Test
    public void constantField_notUpdatedCache_whenChanged() throws Exception{

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

            contentlet.setIndexPolicy(IndexPolicy.FORCE);
            contentlet.setIndexPolicyDependencies(IndexPolicy.FORCE);
            contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            contentletAPI.publish(contentlet, systemUser, false);

            final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder, template)
                    .languageId(1).pageURL("testPageWidget").title("testPageWidget")
                    .nextPersisted();
            pageEnglishVersion.setIndexPolicy(IndexPolicy.FORCE);
            pageEnglishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
            contentlet.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            contentletAPI.publish(pageEnglishVersion, systemUser, false);

            MultiTree multiTree = new MultiTree(pageEnglishVersion.getIdentifier(), containerId,
                    contentlet.getIdentifier(), UUID, 0);
            APILocator.getMultiTreeAPI().saveMultiTree(multiTree);

            HttpServletRequest mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                            .request())
                    .request();
            mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
            final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
            String html = APILocator.getHTMLPageAssetRenderedAPI()
                    .getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(),
                            PageMode.PREVIEW_MODE);
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
            mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
            html = APILocator.getHTMLPageAssetRenderedAPI()
                    .getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(),
                            PageMode.PREVIEW_MODE);
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
//    @Test
    public void containerArchived_PageShouldResolve() throws Exception {

        final Container container = APILocator.getContainerAPI()
                .getWorkingContainerById(containerId, systemUser, false);

        try {
            final String pageName = "testPageContainer-" + System.currentTimeMillis();
            final HTMLPageAsset pageEnglishVersion = new HTMLPageDataGen(folder, template)
                    .languageId(1)
                    .pageURL(pageName)
                    .title(pageName)
                    .nextPersisted();

            pageEnglishVersion.setIndexPolicy(IndexPolicy.FORCE);
            pageEnglishVersion.setIndexPolicyDependencies(IndexPolicy.FORCE);
            pageEnglishVersion.setBoolProperty(Contentlet.IS_TEST_MODE, true);
            contentletAPI.publish(pageEnglishVersion, systemUser, false);

            createMultiTree(pageEnglishVersion.getIdentifier());

            HttpServletRequest mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                            .request())
                    .request();
            mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
            final HttpServletResponse mockResponse = mock(HttpServletResponse.class);
            String html = APILocator.getHTMLPageAssetRenderedAPI()
                    .getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(),
                            PageMode.LIVE);
            Assert.assertTrue(html, html.contains("content2content1"));

            WebAssetFactory.unLockAsset(container);
            WebAssetFactory.archiveAsset(container, systemUser);
            CacheLocator.getVeloctyResourceCache().clearCache();

            mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                            .request())
                    .request();
            mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
            html = APILocator.getHTMLPageAssetRenderedAPI()
                    .getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(),
                            PageMode.LIVE);
            Assert.assertTrue(html, html.isEmpty());

            WebAssetFactory.unArchiveAsset(container);
            WebAssetFactory.publishAsset(container, systemUser);
            CacheLocator.getVeloctyResourceCache().clearCache();

            mockRequest = new MockSessionRequest(
                    new MockAttributeRequest(new MockHttpRequest("localhost", "/").request())
                            .request())
                    .request();
            mockRequest.setAttribute(WebKeys.HTMLPAGE_LANGUAGE, "1");
            HttpServletRequestThreadLocal.INSTANCE.setRequest(mockRequest);
            html = APILocator.getHTMLPageAssetRenderedAPI()
                    .getPageHtml(mockRequest, mockResponse, systemUser, pageEnglishVersion.getURI(),
                            PageMode.LIVE);
            Assert.assertTrue(html, html.contains("content2content1"));
        }finally {
            WebAssetFactory.unArchiveAsset(container);
            WebAssetFactory.publishAsset(container, systemUser);
        }
    }

}