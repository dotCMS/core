package com.dotmarketing.business;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;

import io.vavr.control.Try;

public class CommitListenerCacheWrapperTest {
    private static IdentifierAPI api;
    private static IdentifierCache cache;

    final static String rightId = UUIDGenerator.generateUuid();
    final static String wrongId = UUIDGenerator.generateUuid();
    final static String contentId = UUIDGenerator.generateUuid();
    
    @BeforeClass
    public static void prepare() throws Exception {
        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        api = APILocator.getIdentifierAPI();
        cache = CacheLocator.getIdentifierCache();
    }

    @Test
    public void testing404() throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(1);

        try {
            final Host syshost = APILocator.getHostAPI().findSystemHost();



            // fake not yet created id and asset
            Contentlet fakeCont = new Contentlet();
            fakeCont.setInode(contentId);
            fakeCont.setStructureInode(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host").getInode());

            // now if we create an asset with that ID it should be cleared
            Identifier id = api.createNew(fakeCont, syshost, rightId);
            assertNull(cache.getIdentifier(rightId));
            assertNull(cache.getIdentifier(syshost.getIdentifier(), "/content." + fakeCont.getInode()));
            pool.execute(new EndlessRunner(pool));
            // load cache
            id = Try.of(() -> api.find(rightId)).get();
            assertTrue(rightId.equals(id.getId()));
            

            // this should load the identifier in both cache entries (by url and by id)
            api.find(rightId);
            assertEquals(rightId, cache.getIdentifier(rightId).getId());
            assertEquals(rightId, cache.getIdentifier(APILocator.systemHost().getIdentifier(), "/content." + fakeCont.getInode()).getId());

        } finally {
            pool.shutdownNow();

        }
    }

    class EndlessRunner implements Runnable {

        final ExecutorService pool;
        final List<Throwable> errors;

        public EndlessRunner(ExecutorService pool) {
            this(pool, new ArrayList<>());
        }

        private EndlessRunner(ExecutorService pool, List<Throwable> errors) {
            this.errors = (errors == null) ? new ArrayList<>() : errors;
            this.pool = pool;
        }

        @Override
        public void run() {
            System.err.println("Running a thread!");
            Identifier id = Try.of(() -> api.find(rightId)).get();
            assertTrue(rightId.equals(id.getId()));
            assertEquals(rightId, cache.getIdentifier(APILocator.systemHost().getIdentifier(), "/content." + contentId).getId());
            
            Try.of(() -> {
                Thread.sleep(100);
                return true;
            });

            if (!pool.isShutdown()) {
                pool.execute(new EndlessRunner(pool, errors));
            }
        }

    }
}
