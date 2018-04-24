package com.dotmarketing.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkResponse;

public class DotRunnableThread extends Thread {

  private final List<DotRunnable> listeners;
  private final List<DotRunnable> flushers;
  private final Thread networkCacheFlushThread = new Thread("NetworkCacheFlushThread") {
    @Override
    public void run() {
      try {
        Thread.sleep(Config.getLongProperty("NETWORK_CACHE_FLUSH_DELAY", 3000));
      } catch (InterruptedException e) {
        Logger.warn(this.getClass(), e.getMessage());
      }
      flushers.forEach(runner -> runner.run());
    }
  };

  public DotRunnableThread(final List<DotRunnable> allListeners) {
    this.listeners = getListeners(allListeners);
    this.flushers = getFlushers(allListeners);
  }

  @Override
  public void run() {
    
    this.networkCacheFlushThread.start();
    
    try {
      LocalTransaction.wrap(this::internalRunner);
    } catch (Exception dde) {
      throw new DotStateException(dde);
    }
  }

  private void internalRunner() {

      final Set<String> reindexInodes = new HashSet<>();
      List<Contentlet> contentToIndex = new ArrayList<>();


      final List<List<Contentlet>> reindexList        = new ArrayList<>();
      final List<DotRunnable>      otherListenerList = new ArrayList<>();
      final int batchSize = Config.getIntProperty("INDEX_COMMIT_LISTENER_BATCH_SIZE", 50);

      for (final DotRunnable runner : listeners) {

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

            if (this.isOrdered (runner)) {
                otherListenerList.add(runner);
            } else {
                runner.run();
            }
          }
      }

      reindexList.add(contentToIndex);

      for (final List<Contentlet> batchList : reindexList) {

          try {
              APILocator.getContentletIndexAPI().indexContentList(batchList, null, false, new ActionListener<BulkResponse>() {
                  @Override
                  public void onResponse(BulkResponse bulkItemResponses) {
                      otherListenerList.stream().forEach(DotRunnable::run);
                  }

                  @Override
                  public void onFailure(Exception e) {
                      Logger.error(this, e.getMessage(), e);
                  }
              });
          } catch (DotDataException e) {
              Logger.error(this, e.getMessage(), e);
          }
      }
  }

  private boolean isOrdered(final DotRunnable runner) {

    return this.getOrder(runner) > 0;
  }

  private List<DotRunnable> getFlushers(final List<DotRunnable> allListeners) {
    return allListeners.stream().filter(this::isFlushCacheRunnable).collect(Collectors.toList());
  }

  private List<DotRunnable> getListeners(final List<DotRunnable> allListeners) {
    return allListeners.stream().filter(this::isNotFlushCacheRunnable).sorted(this::compare).collect(Collectors.toList());
  }

  private int compare(final DotRunnable runnable, final DotRunnable runnable1) {
    return this.getOrder(runnable).compareTo(this.getOrder(runnable1));
  }

  private Integer  getOrder(final DotRunnable runnable) {

    final int order = (runnable instanceof HibernateUtil.DotSyncRunnable)?
            HibernateUtil.DotSyncRunnable.class.cast(runnable).getOrder():0;

    return (runnable instanceof HibernateUtil.DotAsyncRunnable)?
            HibernateUtil.DotAsyncRunnable.class.cast(runnable).getOrder(): order;
  }

  private boolean isNotFlushCacheRunnable (final DotRunnable listener) {

      return !this.isFlushCacheRunnable(listener);
  }

  private boolean isFlushCacheRunnable (final DotRunnable listener) {

      return  (
                listener instanceof FlushCacheRunnable ||
                (listener instanceof HibernateUtil.DotAsyncRunnable
                        && HibernateUtil.DotAsyncRunnable.class.cast(listener).getRunnable() instanceof FlushCacheRunnable) ||
                (listener instanceof HibernateUtil.DotSyncRunnable
                        && HibernateUtil.DotSyncRunnable.class.cast(listener).getRunnable() instanceof FlushCacheRunnable)
              );
  }
}
