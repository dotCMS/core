package com.dotmarketing.factories;

import com.dotcms.api.system.event.message.MessageSeverity;
import com.dotcms.api.system.event.message.MessageType;
import com.dotcms.api.system.event.message.SystemMessageEventUtil;
import com.dotcms.api.system.event.message.builder.SystemMessageBuilder;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.google.common.collect.Lists;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.BlockPageCache;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.templates.business.TemplateAPI;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.dotcms.util.CollectionsUtils.list;
import static org.mockito.Matchers.any;
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

            final BlockPageCache.PageCacheParameters
                cacheParameters = new BlockPageCache.PageCacheParameters(userId, languageId, "", "", "");

            final String dummyText = "This is dummy text";

            //Adding page to block cache.
            CacheLocator.getBlockPageCache().add(page, dummyText, cacheParameters);

            String cachedPageText = CacheLocator.getBlockPageCache().get(page, cacheParameters);

            //Test that page is cached.
            Assert.assertNotNull(cachedPageText);
            Assert.assertEquals("Cached text should be the same than dummy text", dummyText, cachedPageText);

            //Publish Page.
            PublishFactory.publishHTMLPage(page, Lists.newArrayList(), systemUser, false);

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

        assertFutureContentErrorMessage(contentlet_1, systemMessageEventUtilMock);
    }

    private void assertFutureContentErrorMessage(Contentlet contentlet_1, SystemMessageEventUtil systemMessageEventUtilMock) {
        final SystemMessageBuilder messageBuilder = new SystemMessageBuilder()
                .setMessage(
                        String.format("The following contents in the Page have Publish Dates set to a future time. " +
                        "These contents will not be displayed in the live version of the Page until their respective " +
                        "Publish Dates: <ul>" +
                        "<li>%s</li></ul>", contentlet_1.getTitle()))
                .setSeverity(MessageSeverity.ERROR)
                .setType(MessageType.SIMPLE_MESSAGE)
                .setLife(TimeUnit.SECONDS.toMillis(5));

        verify(systemMessageEventUtilMock, times(1)).pushMessage(
                eq(messageBuilder.create()),
                any()
         );
    }

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

        assertFutureContentErrorMessage(contentlet_2, systemMessageEventUtilMock);
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
}
