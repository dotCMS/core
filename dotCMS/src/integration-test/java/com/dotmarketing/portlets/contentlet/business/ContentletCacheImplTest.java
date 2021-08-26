package com.dotmarketing.portlets.contentlet.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dotcms.contenttype.business.FieldAPIImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

public class ContentletCacheImplTest {

    private static ContentletCache contentletCache;

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        contentletCache = CacheLocator.getContentletCache();
    }

    /**
     * Methods to test: {@link ContentletCache#add(Contentlet)} and {@link ContentletCache#get(String)}
     * <p>
     * Given scenario: When being part of a transaction, the cache should return the current contentlet or null,
     * this with the idea of avoiding messing up the existing cache with data that has not been persisted
     * <p>
     * Expected result: The method {@link ContentletCache#add(Contentlet)} should return the current contentlet
     * and the method {@link ContentletCache#get(String)} should return null
     * @throws DotHibernateException
     */
    @Test
    public void test_cache_does_not_get_dirty() throws DotHibernateException {
        final Contentlet contentlet = TestDataUtils.getDotAssetLikeContentlet();

        HibernateUtil.startTransaction();
        //As it is in a transaction, the same contentlet is returned
        final Contentlet result = contentletCache.add(contentlet);
        assertNotNull(result);
        assertEquals(contentlet.getInode(), result.getInode());

        //As it is in a transaction, null is returned
        assertNull(contentletCache.get(contentlet.getInode()));

        HibernateUtil.closeAndCommitTransaction();
    }
}
