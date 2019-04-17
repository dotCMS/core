package com.dotmarketing.db;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This thread is charge of running the commit listener in async or sync mode.
 */
public class DotRunnableThread implements Runnable {

    private static final int INDEX_ONLY = 0;
    private static final int RUN_ONLY_LISTENERS = 1;
    private static final int INDEX_AND_RUN_LISTENERS = 2;

    public static final  String INDEX_COMMIT_LISTENER_BATCH_SIZE = "INDEX_COMMIT_LISTENER_BATCH_SIZE";

    private final List<Runnable> listeners;
    private final boolean        isSync;


    public DotRunnableThread(final List<Runnable> allListeners) {
        this(allListeners, false);
    }

    public DotRunnableThread(final List<Runnable> allListeners, final boolean isSync) {
        this.isSync         = isSync;
        this.listeners      = allListeners;
    }

    @Override
    public void run() {

        try {

            Logger.debug(this, ()-> "Running the thread: "
                    + Thread.currentThread().getName() + (this.isSync?" in Sync":"in Async")
                    + " Mode");

            if (UtilMethods.isSet(this.listeners)) {

                LocalTransaction.wrap(this::internalRunner);
            }
        } catch (Exception dde) {
            throw new DotStateException(dde);
        }
    }

    private void internalRunner() {

        final Set<String> reindexInodes          = new HashSet<>();
        final List<List<Contentlet>> reindexList = new ArrayList<>();
        final List<Runnable> otherListenerList   = new ArrayList<>();
        final int batchSize                      = Config.getIntProperty(INDEX_COMMIT_LISTENER_BATCH_SIZE, 50);
        List<Contentlet> contentToIndex          = new ArrayList<>();


        for (final Runnable runner : listeners) {

            if (runner instanceof ReindexRunnable) {

                final ReindexRunnable reindexRunnable = (ReindexRunnable) runner;

                if (ReindexRunnable.Action.REMOVING.equals(reindexRunnable.getAction())) {

                    reindexRunnable.run();
                    continue;
                }

                for (final Contentlet contentlet : reindexRunnable.getReindexIds()) {

                    if (reindexInodes.add(contentlet.getInode())) {

                        contentToIndex.add(contentlet);

                        if (contentToIndex.size() == batchSize) {

                            reindexList.add(contentToIndex);
                            contentToIndex = new ArrayList<>();
                        }
                    }
                }
            } else {

                if (this.isOrdered(runner)) {
                    otherListenerList.add(runner);
                } else {
                    runner.run();
                }
            }
        }

        // If there is some contentlet left
        if (UtilMethods.isSet(contentToIndex)) {

            reindexList.add(contentToIndex);
        }

        if (reindexList.isEmpty()) {

            otherListenerList.stream().forEach(Runnable::run);
        } else {

            this.indexContentList(reindexList, otherListenerList);
        }
    }

    private void indexContentList(final List<List<Contentlet>> reindexList,
                                  final List<Runnable> otherListenerList) {

        for (int i = 0; i < reindexList.size(); ++i) {

            try {

                int action = INDEX_ONLY;
                final List<Contentlet> batchList = reindexList.get(i);

                if (i == reindexList.size() - 1) { // if it is the last one batch

                    action = (UtilMethods.isSet(batchList)) ?
                            UtilMethods.isSet(otherListenerList) ?
                                    INDEX_AND_RUN_LISTENERS : INDEX_ONLY
                            : RUN_ONLY_LISTENERS;
                }

                switch (action) {

                    case RUN_ONLY_LISTENERS:
                        otherListenerList.stream().forEach(Runnable::run);
                        break;

                    case INDEX_AND_RUN_LISTENERS:
                        APILocator.getContentletIndexAPI().addContentToIndex(batchList);
                        break;

                    default:
                        APILocator.getContentletIndexAPI().addContentToIndex(batchList);
                }
            } catch (DotDataException e) {
                Logger.error(this, e.getMessage(), e);
            }
        }
    }

    private static class ReindexActionListeners implements ActionListener<BulkResponse> {

        private final List<Runnable> listeners;

        public ReindexActionListeners(final List<Runnable> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void onResponse(BulkResponse bulkItemResponses) {
            listeners.stream().forEach(Runnable::run);
        }

        @Override
        public void onFailure(final Exception e) {
            Logger.error(this, e.getMessage(), e);
        }
    }


    private boolean isOrdered(final Runnable runner) {

        return this.getOrder(runner) > 0;
    }

    private Integer getOrder(final Runnable runnable) {

        final int order = (runnable instanceof HibernateUtil.DotSyncRunnable) ?
                HibernateUtil.DotSyncRunnable.class.cast(runnable).getOrder() : 0;

        return (runnable instanceof HibernateUtil.DotOrderedRunnable) ?
                HibernateUtil.DotOrderedRunnable.class.cast(runnable).getOrder() : order;
    }

}
