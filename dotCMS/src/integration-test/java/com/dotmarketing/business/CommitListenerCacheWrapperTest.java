package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;

import io.vavr.control.Try;

public class CommitListenerCacheWrapperTest {
    private static IdentifierAPI api;
    private static IdentifierCache cache;

    final static String identifierId = UUIDGenerator.generateUuid();
    final static String originalName = UUIDGenerator.generateUuid();
    final static String newName = UUIDGenerator.generateUuid();

    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        api = APILocator.getIdentifierAPI();
        cache = CacheLocator.getIdentifierCache();
    }

    @Test
    public void Testing_API_And_Cache_Visibility_In_A_Transaction() throws Throwable {
        final ExecutorService pool = Executors.newFixedThreadPool(1);

        try {
            final Host syshost = APILocator.getHostAPI().findSystemHost();

            final List<Throwable> cacheErrors = new ArrayList<>();

            // fake not yet created id and asset
            Contentlet fakeCont = new Contentlet();
            fakeCont.setInode(UUIDGenerator.generateUuid());
            fakeCont.setContentTypeId(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host").getInode());

            // now if we create an asset with that ID it should be cleared
            Identifier id = api.createNew(fakeCont, syshost, identifierId);

            // Save with the right name
            id.setAssetName(originalName);
            id = api.save(id);

            // Not in cache yet
            assertNull(cache.getIdentifier(identifierId));
            assertNull(cache.getIdentifier(syshost.getIdentifier(), "/" + originalName));

            // find method loads cache
            id = Try.of(() -> api.find(identifierId)).get();

            assertEquals(identifierId, cache.getIdentifier(identifierId).getId());
            assertEquals(originalName, cache.getIdentifier(identifierId).getAssetName());
            assertEquals(originalName, cache.getIdentifier(syshost.getIdentifier(), "/" + originalName).getAssetName());

            // fire off another thread that should only the old cache entries
            pool.execute(new OldCacheRunner(pool, cacheErrors));

            
            
            
            // ------------------------------------------------------------------------
            // start a transaction, thread running in background
            // ------------------------------------------------------------------------
            HibernateUtil.startTransaction();

            id = (Identifier) BeanUtils.cloneBean(id);

            // Save with a NEW name
            id.setAssetName(newName);
            id = api.save(id);

            // saved identifier has the NEW name
            assertTrue(id.getAssetName().equals(newName));

            // DB has the NEW name
            Identifier id2 = Try.of(() -> api.loadFromDb(identifierId)).get();
            assertTrue(id2.getAssetName().equals(newName));
            

            Thread.sleep(500);
            
            // Because we are in a transaction, the IdentifierAPI cache has not been flushed, so we get the old name
            id2 = Try.of(() -> api.find(identifierId)).get();
            assertTrue(id2.getAssetName().equals(originalName));

            // Cache has the OLD name (cache should not be flushed until after the commit
            assertEquals(originalName, cache.getIdentifier(syshost.getIdentifier(), "/" + originalName).getAssetName());
            assertEquals(originalName, cache.getIdentifier(identifierId).getAssetName());

            pool.shutdown();

            while(!pool.isTerminated()){
                Thread.sleep(50);
            }
            
            // if the OldCacheRunner got an error, throw it
            if (cacheErrors.size() > 0) {
                throw new AssertionError(cacheErrors.get(0));
            }
            // ------------------------------------------------------------------------
            // commit and everyone should see the new values
            // ------------------------------------------------------------------------
            HibernateUtil.commitTransaction();

            
            
            
            
            // this should load the identifier in both cache entries (by url and by id)
            id2= api.find(identifierId);
            assertTrue(id2.getAssetName().equals(newName));
            assertEquals(newName, cache.getIdentifier(syshost.getIdentifier(), "/" + newName).getAssetName());
            assertEquals(newName, cache.getIdentifier(identifierId).getAssetName());

            if (!cacheErrors.isEmpty()) {
                throw cacheErrors.get(0);
            }
        } finally {
            if (!pool.isShutdown()) {
                pool.shutdownNow();
            }

        }
    }

    class OldCacheRunner implements Runnable {

        final ExecutorService pool;
        final List<Throwable> errors;

        private OldCacheRunner(ExecutorService pool, List<Throwable> errors) {
            this.errors = (errors == null) ? new ArrayList<>() : errors;
            this.pool = pool;
        }

        @Override
        public void run() {

            Identifier id = Try.of(() -> api.find(identifierId)).get();
            try {
                // Thread should always have the OLD ID
                assertTrue(id.getAssetName().equals(originalName));
                assertEquals(originalName, cache.getIdentifier(identifierId).getAssetName());

            } catch (Throwable t) {
                errors.add(t);
            }
            Try.of(() -> {
                Thread.sleep(5);
                return true;
            }).get();

            if (!pool.isShutdown() && errors.isEmpty()) {
                pool.execute(new OldCacheRunner(pool, errors));
            } else if (!errors.isEmpty()) {
                errors.get(0).printStackTrace();
            }
        }

    }
}
