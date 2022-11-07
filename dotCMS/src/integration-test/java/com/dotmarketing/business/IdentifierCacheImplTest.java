package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

public class IdentifierCacheImplTest {

    @BeforeClass
    public static void init() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link IdentifierCacheImpl#addContentletVersionInfoToCache(ContentletVersionInfo)}
     * When: Add a {@link ContentletVersionInfo} into cache
     * Should: geet it with {@link IdentifierCacheImpl#getContentVersionInfo(String, long)}
     */
    @Test
    public void addIntoCache(){
        final String id = UUIDGenerator.generateUuid();
        final Language language = new LanguageDataGen().nextPersisted();

        final ContentletVersionInfo contentletVersionInfo = new ContentletVersionInfo();
        contentletVersionInfo.setIdentifier(id);
        contentletVersionInfo.setLang(language.getId());
        contentletVersionInfo.setVariant(VariantAPI.DEFAULT_VARIANT.name());

        CacheLocator.getIdentifierCache().addContentletVersionInfoToCache(contentletVersionInfo);

        final ContentletVersionInfo contentletVersionInfoFromCache = CacheLocator.getIdentifierCache()
                .getContentVersionInfo(id, language.getId());
        assertNotNull(contentletVersionInfoFromCache);
        assertEquals(contentletVersionInfo, contentletVersionInfoFromCache);

        final ContentletVersionInfo contentletVersionInfoFromCache2 = CacheLocator.getIdentifierCache()
                .getContentVersionInfo(id, language.getId(), VariantAPI.DEFAULT_VARIANT.name());
        assertNotNull(contentletVersionInfoFromCache2);
        assertEquals(contentletVersionInfo, contentletVersionInfoFromCache2);
    }

    /**
     * Method to test: {@link IdentifierCacheImpl#addContentletVersionInfoToCache(ContentletVersionInfo)}
     * When: Add a {@link ContentletVersionInfo} with {@link Variant} into cache
     * Should: geet it with {@link IdentifierCacheImpl#getContentVersionInfo(String, long, String)}
     */
    @Test
    public void addIntoCacheWithVariant(){
        final String id = UUIDGenerator.generateUuid();
        final Language language = new LanguageDataGen().nextPersisted();
        final Variant variant = new VariantDataGen().nextPersisted();

        final ContentletVersionInfo contentletVersionInfo = new ContentletVersionInfo();
        contentletVersionInfo.setIdentifier(id);
        contentletVersionInfo.setLang(language.getId());
        contentletVersionInfo.setVariant(variant.name());

        CacheLocator.getIdentifierCache().addContentletVersionInfoToCache(contentletVersionInfo);

        final ContentletVersionInfo contentletVersionInfoFromCache = CacheLocator.getIdentifierCache()
                .getContentVersionInfo(id, language.getId(), variant.name());

        assertNotNull(contentletVersionInfoFromCache);
        assertEquals(contentletVersionInfo, contentletVersionInfoFromCache);
    }
}
