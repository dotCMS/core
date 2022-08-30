package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.util.UUIDGenerator;
import org.junit.Test;

public class IdentifierCacheTest {

    @Test
    public void contentletVersionCache(){
        final IdentifierCache identifierCache = CacheLocator.getIdentifierCache();

        assertEquals(IdentifierCacheImpl.class, identifierCache.getClass());

        final String identifier = UUIDGenerator.generateUuid();
        final long langId = 1;
        final String variantId = "1";

        ContentletVersionInfo contentletVersionInfo = new ContentletVersionInfo();
        contentletVersionInfo.setIdentifier(identifier);
        contentletVersionInfo.setLang(langId);
        contentletVersionInfo.setVariant(variantId);

        identifierCache.addContentletVersionInfoToCache(contentletVersionInfo);

        final ContentletVersionInfo contentVersionInfoFromCache = identifierCache.getContentVersionInfo(
                identifier, langId);

        assertEquals(identifier, contentVersionInfoFromCache.getIdentifier());
        assertEquals(langId, contentVersionInfoFromCache.getLang());
        assertEquals(variantId, contentVersionInfoFromCache.getVariant());

        identifierCache.removeContentletVersionInfoToCache(identifier, langId);

        assertNull(identifierCache.getContentVersionInfo(identifier, langId));
    }
}
