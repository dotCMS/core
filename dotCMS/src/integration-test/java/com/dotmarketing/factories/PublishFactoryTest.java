package com.dotmarketing.factories;

import com.google.common.collect.Lists;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.datagen.HTMLPageDataGen;
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
}
