package com.dotcms.csspreproc;

import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestDataUtils.TestFile;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import graphql.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class CSSCacheTest {

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Given Scenario: Create a file asset and add it to the CSSCache, re-save the file asset
     * ExpectedResult: Should remove the entry from the CSSCache
     */
    @Test
    public void test_checkinContentlet_shouldRemoveCache() throws Exception{
        final Contentlet contentlet = TestDataUtils.getFileAssetContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                TestFile.TEXT);
        final String uri = APILocator.getFileAssetAPI().fromContentlet(contentlet).getURI();
        final CachedCSS entry = new CachedCSS();
        entry.hostId = contentlet.getHost();
        entry.uri = uri;
        entry.live = contentlet.isLive();
        CacheLocator.getCSSCache().add(entry);
        CachedCSS cachedCSS = CacheLocator.getCSSCache().get(contentlet.getHost(),uri,contentlet.isLive(),APILocator.systemUser());
        Assert.assertNotNull(cachedCSS);

        APILocator.getContentletAPI().checkin(APILocator.getContentletAPI().checkout(contentlet.getInode(),APILocator.systemUser(),false),APILocator.systemUser(),false);
        cachedCSS = CacheLocator.getCSSCache().get(contentlet.getHost(),uri,contentlet.isLive(),APILocator.systemUser());
        Assert.assertNull(cachedCSS);
    }

    /**
     * Given Scenario: Create a file asset and add it to the CSSCache, unpublish the file asset
     * ExpectedResult: Should remove the entry from the CSSCache
     */
    @Test
    public void test_unpublishContentlet_shouldRemoveCache() throws Exception{
        final Contentlet contentlet = TestDataUtils.getFileAssetContent(true,
                APILocator.getLanguageAPI().getDefaultLanguage().getId(),
                TestFile.TEXT);
        final String uri = APILocator.getFileAssetAPI().fromContentlet(contentlet).getURI();
        final CachedCSS entry = new CachedCSS();
        entry.hostId = contentlet.getHost();
        entry.uri = uri;
        entry.live = contentlet.isLive();
        CacheLocator.getCSSCache().add(entry);
        CachedCSS cachedCSS = CacheLocator.getCSSCache().get(contentlet.getHost(),uri,contentlet.isLive(),APILocator.systemUser());
        Assert.assertNotNull(cachedCSS);

        APILocator.getContentletAPI().unpublish(contentlet,APILocator.systemUser(),false);
        cachedCSS = CacheLocator.getCSSCache().get(contentlet.getHost(),uri,contentlet.isLive(),APILocator.systemUser());
        Assert.assertNull(cachedCSS);
    }

}
