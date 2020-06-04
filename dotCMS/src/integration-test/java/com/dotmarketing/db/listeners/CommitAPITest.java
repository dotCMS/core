package com.dotmarketing.db.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.dotcms.repackage.net.sf.hibernate.HibernateException;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

public class CommitAPITest {

    @DataProvider
    public static Object[] testCases() {

        final ReindexListener reindexRunnable=new ReindexListener(Collections.emptyList(), ReindexListener.Action.ADDING) {};

        final FlushCacheListener flushCacheRunnable = new FlushCacheListener() {
            public void run() {}
            public String key() {return "key" + UUID.randomUUID();}
        };

        return new AddCommitListenerTestCase[] {
                new AddCommitListenerTestCase.Builder()
                        .asyncReindexCommitListeners(true)
                        .dotRunnable(reindexRunnable)
                        .expectedAsyncListeners(1)
                        .expectedSyncListeners(0)
                        .createReindexCommitListenerTestCase(),

                new AddCommitListenerTestCase.Builder()
                        .asyncReindexCommitListeners(false)
                        .dotRunnable(reindexRunnable)
                        .expectedAsyncListeners(0)
                        .expectedSyncListeners(1)
                        .createReindexCommitListenerTestCase(),

                new AddCommitListenerTestCase.Builder()
                        .asyncReindexCommitListeners(false)
                        .asyncCommitListeners(true)
                        .dotRunnable(reindexRunnable)
                        .expectedAsyncListeners(0)
                        .expectedSyncListeners(1)
                        .createReindexCommitListenerTestCase(),

                new AddCommitListenerTestCase.Builder()
                        .asyncReindexCommitListeners(true)
                        .asyncCommitListeners(false)
                        .dotRunnable(flushCacheRunnable)
                        .expectedAsyncListeners(0)
                        .expectedSyncListeners(1)
                        .createReindexCommitListenerTestCase(),

                new AddCommitListenerTestCase.Builder()
                        .asyncReindexCommitListeners(false)
                        .asyncCommitListeners(true)
                        .dotRunnable(flushCacheRunnable)
                        .expectedAsyncListeners(1)
                        .expectedSyncListeners(0)
                        .createReindexCommitListenerTestCase(),
        };
    }
    @Test
    public void test() {
        fail("Not yet implemented");
    }
    @Test
    @UseDataProvider("testCases")
    public void testAddCommitListener(final AddCommitListenerTestCase testCase) throws DotHibernateException,
            HibernateException, SQLException {
        HibernateUtil.getSession().connection().setAutoCommit(false);

        final boolean originalAsyncReindexCommitListenersValue = Config.getBooleanProperty("ASYNC_REINDEX_COMMIT_LISTENERS", true);
        final boolean originalAsyncCommitListenersValue = Config.getBooleanProperty("ASYNC_COMMIT_LISTENERS", true);

        Config.setProperty("ASYNC_REINDEX_COMMIT_LISTENERS", testCase.isAsyncReindexCommitListeners());
        Config.setProperty("ASYNC_COMMIT_LISTENERS", testCase.isAsyncCommitListeners());

        try {

            CommitAPI.getInstance().addCommitListenerAsync(new CommitListener() {
                
                @Override
                public void run() {
                    testCase.getDotRunnable().run();
                    
                }
                
                @Override
                public String key() {
                    return new UUIDGenerator().uuid();
                }
            });

            final Map<String, DotListener> asyncCommitListeners = CommitAPI.getInstance().asyncCommitListeners.get();
            final Map<String, DotListener> syncCommitListeners = CommitAPI.getInstance().syncCommitListeners.get();
            assertEquals(testCase.getExpectedAsyncListeners(), asyncCommitListeners.size());
            assertEquals(testCase.getExpectedSyncListeners(), syncCommitListeners.size());
            HibernateUtil.getSession().connection().setAutoCommit(true);

        } finally {
            Config.setProperty("ASYNC_REINDEX_COMMIT_LISTENERS", originalAsyncReindexCommitListenersValue);
            Config.setProperty("ASYNC_COMMIT_LISTENERS", originalAsyncCommitListenersValue);
        }
    }

}
