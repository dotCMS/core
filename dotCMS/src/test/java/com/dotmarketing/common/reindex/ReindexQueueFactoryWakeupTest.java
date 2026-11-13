package com.dotmarketing.common.reindex;

import static org.junit.Assert.assertEquals;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.withSettings;

import com.dotcms.business.SystemCache;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.util.Config;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/** Queue writes must wake selected peers even when the local unpause is a no-op. */
public class ReindexQueueFactoryWakeupTest {

    private final ReindexQueueFactory factory = new ReindexQueueFactory();

    @Test
    public void reindexNotifiesPeersOnlyWhenCommitCallbackRuns() throws Exception {
        assertWakeup(false, false);
    }

    @Test
    public void deleteNotifiesPeersOnlyWhenCommitCallbackRuns() throws Exception {
        assertWakeup(true, false);
    }

    @Test
    public void manualUnpauseDoesNotWakeLocalOrRemoteWorkers() throws Exception {
        assertWakeup(false, true);
        assertWakeup(true, true);
    }

    private void assertWakeup(final boolean delete, final boolean manual) throws Exception {
        final SystemCache cache = mock(SystemCache.class);
        try (MockedStatic<ReindexThread> worker = mockStatic(ReindexThread.class);
                MockedStatic<Config> config = mockStatic(Config.class);
                MockedStatic<DbConnectionFactory> database = mockStatic(DbConnectionFactory.class);
                MockedStatic<HibernateUtil> transactions = mockStatic(HibernateUtil.class);
                MockedStatic<CacheLocator> caches = mockStatic(CacheLocator.class);
                MockedConstruction<DotConnect> queries = mockConstruction(DotConnect.class,
                        withSettings().defaultAnswer(RETURNS_SELF))) {
            config.when(() -> Config.getBooleanProperty("ALLOW_MANUAL_REINDEX_UNPAUSE", false))
                    .thenReturn(manual);
            database.when(DbConnectionFactory::now).thenReturn(new Date());
            caches.when(CacheLocator::getSystemCache).thenReturn(cache);

            final int count = delete
                    ? factory.addIdentifierDelete(List.of("content"),
                            ReindexQueueFactory.Priority.NORMAL.dbValue())
                    : factory.addIdentifierReindex(List.of("content"));

            assertEquals(1, count);
            assertEquals(1, queries.constructed().size());
            verifyNoInteractions(cache);
            if (manual) {
                worker.verifyNoInteractions();
                transactions.verifyNoInteractions();
            } else {
                worker.verify(ReindexThread::unpause);
                final ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
                transactions.verify(() -> HibernateUtil.addCommitListener(
                        eq("wakeReindexingServers"), callback.capture()));
                callback.getValue().run();
                verify(cache).remove("REINDEX_THREAD_PAUSED");
            }
        }
    }
}
