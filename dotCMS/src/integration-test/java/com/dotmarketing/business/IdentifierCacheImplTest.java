package com.dotmarketing.business;

import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.VariantDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.variant.VariantAPI;
import com.dotcms.variant.model.Variant;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

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

    /**
     * Method to test: {@link IdentifierCacheImpl#putContentVersionInfos(String, List)} and  {@link IdentifierCacheImpl#getContentVersionInfos(String)}
     * When: Add a list of {@link ContentletVersionInfo} is added to cache
     * Should: get it with {@link IdentifierCacheImpl#getContentVersionInfos(String)}
     */
    @Test
    public void putContentVersionInfos_and_getContentVersionInfos_successfully(){
        final String id = UUIDGenerator.generateUuid();
        final Language language1 = new LanguageDataGen().nextPersisted();
        final Language language2 = new LanguageDataGen().nextPersisted();
        final List<ContentletVersionInfo> listContentletVersionInfo = new ArrayList<>();

        ContentletVersionInfo contentletVersionInfo = new ContentletVersionInfo();
        contentletVersionInfo.setIdentifier(id);
        contentletVersionInfo.setLang(language1.getId());
        contentletVersionInfo.setVariant(VariantAPI.DEFAULT_VARIANT.name());
        listContentletVersionInfo.add(contentletVersionInfo);

        contentletVersionInfo = new ContentletVersionInfo();
        contentletVersionInfo.setIdentifier(id);
        contentletVersionInfo.setLang(language2.getId());
        contentletVersionInfo.setVariant(VariantAPI.DEFAULT_VARIANT.name());
        listContentletVersionInfo.add(contentletVersionInfo);

        CacheLocator.getIdentifierCache().putContentVersionInfos(id,listContentletVersionInfo);

        final List<ContentletVersionInfo> listContentletVersionInfoFromCache = CacheLocator.getIdentifierCache()
                        .getContentVersionInfos(id);

        assertNotNull(listContentletVersionInfoFromCache);
        assertEquals(listContentletVersionInfo.size(), listContentletVersionInfoFromCache.size());
    }


    /**
     * Method to test: {@link IdentifierCacheImpl#removeFromCacheByIdentifier(Identifier)}
     * When: Add a list of {@link ContentletVersionInfo} is added to cache and removed by the identifier
     * Should: all versions should be removed.
     */
    @Test
    public void removeFromCacheByIdentifier_successfully(){
        final String id = UUIDGenerator.generateUuid();
        final Language language1 = new LanguageDataGen().nextPersisted();
        final Language language2 = new LanguageDataGen().nextPersisted();
        final List<ContentletVersionInfo> listContentletVersionInfo = new ArrayList<>();

        ContentletVersionInfo contentletVersionInfo = new ContentletVersionInfo();
        contentletVersionInfo.setIdentifier(id);
        contentletVersionInfo.setLang(language1.getId());
        contentletVersionInfo.setVariant(VariantAPI.DEFAULT_VARIANT.name());
        listContentletVersionInfo.add(contentletVersionInfo);

        contentletVersionInfo = new ContentletVersionInfo();
        contentletVersionInfo.setIdentifier(id);
        contentletVersionInfo.setLang(language2.getId());
        contentletVersionInfo.setVariant(VariantAPI.DEFAULT_VARIANT.name());
        listContentletVersionInfo.add(contentletVersionInfo);

        CacheLocator.getIdentifierCache().putContentVersionInfos(id,listContentletVersionInfo);

        List<ContentletVersionInfo> listContentletVersionInfoFromCache = CacheLocator.getIdentifierCache()
                .getContentVersionInfos(id);

        assertNotNull(listContentletVersionInfoFromCache);
        assertEquals(listContentletVersionInfo.size(), listContentletVersionInfoFromCache.size());

        CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(id);

        listContentletVersionInfoFromCache = CacheLocator.getIdentifierCache()
                .getContentVersionInfos(id);
        assertNull(listContentletVersionInfoFromCache);
    }
}
