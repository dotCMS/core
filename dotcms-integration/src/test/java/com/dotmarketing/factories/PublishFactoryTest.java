package com.dotmarketing.factories;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.exception.WebAssetException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.links.model.Link;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import com.google.common.collect.Lists;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PageCacheParameters;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;

import com.liferay.portlet.ActionRequestImpl;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.dotcms.util.CollectionsUtils.list;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PublishFactoryTest extends IntegrationTestBase {

    static TemplateAPI templateAPI;
    static FolderAPI folderAPI;
    static LanguageAPI languageAPI;

    private static User systemUser;
    private static Host defaultHost;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        //We need license to test TTL and block cache.
        LicenseTestUtil.getLicense();

        templateAPI = APILocator.getTemplateAPI();
        folderAPI = APILocator.getFolderAPI();
        languageAPI = APILocator.getLanguageAPI();

        systemUser = APILocator.systemUser();
        defaultHost = APILocator.getHostAPI().findDefaultHost(systemUser, false);
    }

    @Test
    public void test_publishHTMLPage_should_remove_cached_page() throws Exception {

        Template template = null;
        Folder folder = null;
        HTMLPageAsset page = null;

        try {
            //Create a Template
            template = new Template();
            template.setTitle("Title");
            template.setBody("Body");
            template = templateAPI.saveTemplate(template, defaultHost, systemUser, false);

            //Create a Folder
            folder = folderAPI.createFolders(
                "/test_junit/test_" + UUIDGenerator.generateUuid().replaceAll("-", "_"), defaultHost,
                systemUser, false);

            //Create a Page with 1 hour TTL.
            page = new HTMLPageDataGen(folder, template).cacheTTL(3600).nextPersisted();

            final String languageId = String.valueOf(languageAPI.getDefaultLanguage().getId());
            final String userId = systemUser.getUserId();

            PageCacheParameters
                cacheParameters = new PageCacheParameters(page.getInode(), userId, languageId);

            final String dummyText = "This is dummy text";

            
            //Adding page to block cache.
            CacheLocator.getBlockPageCache().add(page, dummyText, cacheParameters);

            Thread.sleep(2000);//Added b/c of the Debounce change over the BlockPageCache

            String cachedPageText = CacheLocator.getBlockPageCache().get(page, cacheParameters);

            //Test that page is cached.
            Assert.assertNotNull(cachedPageText);
            Assert.assertEquals("Cached text should be the same than dummy text", dummyText, cachedPageText);

            
            //Publish Page.
            PublishFactory.publishHTMLPage(page, Lists.newArrayList(), systemUser, false);

            cacheParameters = new PageCacheParameters(page.getInode(), userId, languageId);
            
            
            cachedPageText = CacheLocator.getBlockPageCache().get(page, cacheParameters);

            //Page should be out of the cache after publish.
            Assert.assertNull(cachedPageText);

        } finally {
            //Clean up
            if (page != null) {
                HTMLPageDataGen.remove(page);
            }
            if (template != null) {
                templateAPI.delete(template, systemUser, false);
            }
            if (folder != null) {
                folderAPI.delete(folder, systemUser, false);
            }
        }
    }

    /***
     * Method to Test: {@link PublishFactory#publishHTMLPage(IHTMLPage, HttpServletRequest)}
     * When: Try to publish a page with a {@link Contentlet} with a future publish page
     * Should: Publish the page anywhere and send a notification saying that the content is not going to show in the live version
     * @throws Exception
     */
    @Test
    public void testPublishHTMLPageWithContentWithFuturePublishDate() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        com.dotcms.contenttype.model.field.Field publishField = new FieldDataGen()
                .name("Pub Date")
                .velocityVarName("sysPublishDate")
                .defaultValue(null)
                .type(DateTimeField.class)
                .indexed(true)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(publishField)
                .host(host)
                .publishDateFieldVarName(publishField.variable())
                .nextPersisted();

        final Container container = new ContainerDataGen()
                .withContentType(contentType, "")
                .site(host)
                .nextPersisted();

        final Template template = new TemplateDataGen()
                .site(host)
                .withContainer(container.getIdentifier())
                .nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);

        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id())
                .setProperty(publishField.variable(), tomorrow.getTime())
                .nextPersisted();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id())
                .nextPersisted();

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(htmlPageAsset)
                .setContentlet(contentlet_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(htmlPageAsset)
                .setContentlet(contentlet_2)
                .nextPersisted();

        final User systemUser = APILocator.systemUser();

        final List relatedNotPublished = new ArrayList();

        PublishFactory.getUnpublishedRelatedAssetsForPage(htmlPageAsset, relatedNotPublished,
                true, systemUser, false);

        ContentletVersionInfo contentletVersionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(
                htmlPageAsset.getIdentifier(), htmlPageAsset.getLanguageId()).get();

        Assert.assertNull(contentletVersionInfo.getLiveInode());

        final SystemMessageEventUtil systemMessageEventUtilMock = mock(SystemMessageEventUtil.class);

        PublishFactory.setSystemMessageEventUtil(systemMessageEventUtilMock);

        APILocator.getContentletAPI().publish(htmlPageAsset, systemUser, false);


        contentletVersionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(
                htmlPageAsset.getIdentifier(), htmlPageAsset.getLanguageId()).get();

        Assert.assertNotNull(contentletVersionInfo.getLiveInode());
    }

    /***
     * Method to Test: {@link PublishFactory#publishHTMLPage(IHTMLPage, HttpServletRequest)}
     * When: Try to publish a page with a {@link Contentlet} already expired
     * Should: Publish the page anywhere and send a notification saying that the content is not going to show in the live version
     * @throws Exception
     */
    @Test
    public void testPublishHTMLPageWithExperiredContent() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        com.dotcms.contenttype.model.field.Field expireField = new FieldDataGen()
                .name("Exp Date")
                .velocityVarName("sysExpDate")
                .defaultValue(null)
                .type(DateTimeField.class)
                .indexed(true)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(expireField)
                .host(host)
                .expireDateFieldVarName(expireField.variable())
                .nextPersisted();

        final Container container = new ContainerDataGen()
                .withContentType(contentType, "")
                .site(host)
                .nextPersisted();

        final Template template = new TemplateDataGen()
                .site(host)
                .withContainer(container.getIdentifier())
                .nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id())
                .setProperty(expireField.variable(), yesterday.getTime())
                .setProperty(Contentlet.DONT_VALIDATE_ME, true)
                .nextPersisted();

        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id())
                .nextPersisted();

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(htmlPageAsset)
                .setContentlet(contentlet_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(htmlPageAsset)
                .setContentlet(contentlet_2)
                .nextPersisted();

        final User systemUser = APILocator.systemUser();

        ContentletVersionInfo contentletVersionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(
                htmlPageAsset.getIdentifier(), htmlPageAsset.getLanguageId()).get();

        Assert.assertNull(contentletVersionInfo.getLiveInode());

        final SystemMessageEventUtil systemMessageEventUtilMock = mock(SystemMessageEventUtil.class);

        PublishFactory.setSystemMessageEventUtil(systemMessageEventUtilMock);

        APILocator.getContentletAPI().publish(htmlPageAsset, systemUser, false);


        contentletVersionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(
                htmlPageAsset.getIdentifier(), htmlPageAsset.getLanguageId()).get();

        Assert.assertNotNull(contentletVersionInfo.getLiveInode());

        assertExpiredContentErrorMessage(contentlet_1, systemMessageEventUtilMock);
    }

    /***
     * Method to Test: {@link PublishFactory#publishHTMLPage(IHTMLPage, HttpServletRequest)}
     * When: Try to publish a page with a {@link Contentlet} already expired and another {@link Contentlet} whit a future publish date
     * Should: Publish the page anywhere and send two notification
     * @throws Exception
     */
    @Test
    public void testPublishHTMLPageWithExperiredAndFutureContent() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        com.dotcms.contenttype.model.field.Field expireField = new FieldDataGen()
                .name("Exp Date")
                .velocityVarName("sysExpDate")
                .defaultValue(null)
                .type(DateTimeField.class)
                .indexed(true)
                .next();

        com.dotcms.contenttype.model.field.Field publishField = new FieldDataGen()
                .name("Pub Date")
                .velocityVarName("sysPublishDate")
                .defaultValue(null)
                .type(DateTimeField.class)
                .indexed(true)
                .next();

        final ContentType contentType = new ContentTypeDataGen()
                .field(expireField)
                .host(host)
                .publishDateFieldVarName(publishField.variable())
                .expireDateFieldVarName(expireField.variable())
                .nextPersisted();

        final Container container = new ContainerDataGen()
                .withContentType(contentType, "")
                .site(host)
                .nextPersisted();

        final Template template = new TemplateDataGen()
                .site(host)
                .withContainer(container.getIdentifier())
                .nextPersisted();

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        final Contentlet contentlet_1 = new ContentletDataGen(contentType.id())
                .setProperty(expireField.variable(), yesterday.getTime())
                .setProperty(Contentlet.DONT_VALIDATE_ME, true)
                .nextPersisted();

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);

        final Contentlet contentlet_2 = new ContentletDataGen(contentType.id())
                .setProperty(publishField.variable(), tomorrow.getTime())
                .nextPersisted();

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(htmlPageAsset)
                .setContentlet(contentlet_1)
                .nextPersisted();

        new MultiTreeDataGen()
                .setContainer(container)
                .setPage(htmlPageAsset)
                .setContentlet(contentlet_2)
                .nextPersisted();

        final User systemUser = APILocator.systemUser();

        final List relatedNotPublished = new ArrayList();
        PublishFactory.getUnpublishedRelatedAssetsForPage(htmlPageAsset, relatedNotPublished,
                true, systemUser, false);

        ContentletVersionInfo contentletVersionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(
                htmlPageAsset.getIdentifier(), htmlPageAsset.getLanguageId()).get();

        Assert.assertNull(contentletVersionInfo.getLiveInode());

        final SystemMessageEventUtil systemMessageEventUtilMock = mock(SystemMessageEventUtil.class);

        PublishFactory.setSystemMessageEventUtil(systemMessageEventUtilMock);

        APILocator.getContentletAPI().publish(htmlPageAsset, systemUser, false);

        contentletVersionInfo = APILocator.getVersionableAPI().getContentletVersionInfo(
                htmlPageAsset.getIdentifier(), htmlPageAsset.getLanguageId()).get();

        Assert.assertNotNull(contentletVersionInfo.getLiveInode());

        assertExpiredContentErrorMessage(contentlet_1, systemMessageEventUtilMock);
    }

    private void assertExpiredContentErrorMessage(Contentlet contentlet_1, SystemMessageEventUtil systemMessageEventUtilMock) {
        final SystemMessageBuilder messageBuilderExpiredContent = new SystemMessageBuilder()
                .setMessage(
                        String.format("The following contents in the Page have Expired Dates set to the past time. " +
                                "These contents will not be displayed in the live version of the Page: <ul>" +
                                "<li>%s</li></ul>", contentlet_1.getTitle()))
                .setSeverity(MessageSeverity.ERROR)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setLife(TimeUnit.SECONDS.toMillis(5));

        verify(systemMessageEventUtilMock, times(1)).pushMessage(
                eq(messageBuilderExpiredContent.create()),
                any()
        );
    }

    /***
     * Method to Test: {@link PublishFactory#publishHTMLPage(IHTMLPage, HttpServletRequest)}
     * When: Try to publish a page with a file Template that is not published.
     * Should: Publish the page and the file Template.
     */
    @Test
    public void test_publishHTMLPage_pageUsingFileTemplate_success()
            throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        FileAssetTemplate fileAssetTemplate = new TemplateAsFileDataGen().designTemplate(true)
                .host(host).nextPersisted();

        fileAssetTemplate = FileAssetTemplate.class.cast(APILocator.getTemplateAPI()
                .findWorkingTemplate(fileAssetTemplate.getIdentifier(),systemUser,false));

        Assert.assertFalse(APILocator.getTemplateAPI().isLive(fileAssetTemplate));

        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, fileAssetTemplate).nextPersisted();

        APILocator.getContentletAPI().publish(htmlPageAsset, systemUser, false);

        fileAssetTemplate = FileAssetTemplate.class.cast(APILocator.getTemplateAPI()
                .findWorkingTemplate(fileAssetTemplate.getIdentifier(),systemUser,false));

        Assert.assertTrue(APILocator.getTemplateAPI().isLive(fileAssetTemplate));
    }

    /***
     * Method to Test: {@link PublishFactory#getUnpublishedRelatedAssets(Folder, List, User, boolean)}
     * When: Try to publish a folder with a file that is not published.
     * Should: get the list of unpublished files.
     */
    @Test
    public void test_getUnpublishedRelatedAssets() throws DotDataException, DotSecurityException {
        final Host host = new SiteDataGen().nextPersisted();

        final Folder mainFolder = new FolderDataGen().site(host).nextPersisted();

        final Link link = new LinkDataGen().parent(mainFolder).nextPersisted(false);
        Template templateDataGen = new TemplateDataGen().nextPersisted();
        ContentType page = new HTMLPageDataGen(mainFolder, templateDataGen).nextPersisted().getContentType();
        final Folder subFolder = new FolderDataGen().parent(mainFolder).nextPersisted();

        final User systemUser = APILocator.systemUser();
        final List<Object> relatedNotPublished = new ArrayList<>();
         PublishFactory.getUnpublishedRelatedAssets(mainFolder,relatedNotPublished, systemUser, false);

         //iterate the response list. Folder is not retrieved because it is not published
        for(Object obj : relatedNotPublished) {
        	if(obj instanceof Contentlet) {
        		Contentlet contentlet = (Contentlet) obj;
        		Assert.assertEquals(page.id(), contentlet.getContentTypeId());
        	}
            if(obj instanceof Link) {
                //assert that the link is the one created above
                Assert.assertEquals(link, obj);
            }
        }
    }

    /***
     * Method to Test: {@link PublishFactory#publishAsset(Folder, User, boolean, boolean)}
     * When: Try to publish a folder with a file that is not published.
     * Should: Publish the file inside the folder.
     */
    @Test
    public void test_publishAsset() throws DotDataException, DotSecurityException, WebAssetException {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final Link link = new LinkDataGen(folder).nextPersisted(false);
        Template templateDataGen = new TemplateDataGen().nextPersisted();
        ContentType page = new HTMLPageDataGen(folder, templateDataGen).nextPersisted().getContentType();
        final Folder subFolder = new FolderDataGen().parent(folder).nextPersisted();
        final boolean response = PublishFactory.publishAsset(folder, systemUser, false, false);
        Assert.assertTrue(response);
    }
}
