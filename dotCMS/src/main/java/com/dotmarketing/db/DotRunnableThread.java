package com.dotmarketing.db;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DotRunnableThread extends Thread {

  private final List<Runnable> listeners;
  private final List<Runnable> flushers;
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

  public DotRunnableThread(final List<Runnable> allListeners) {
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
      final List<Runnable>      otherListenerList = new ArrayList<>();
      final int batchSize = Config.getIntProperty("INDEX_COMMIT_LISTENER_BATCH_SIZE", 50);

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

            if (this.isOrdered (runner)) {
                otherListenerList.add(runner);
            } else {
                runner.run();
            }
          }
      }

      reindexList.add(contentToIndex);

      if (reindexList.isEmpty()) {

          otherListenerList.stream().forEach(Runnable::run);
      } else {
          for (final List<Contentlet> batchList : reindexList) {

              try {
                  APILocator.getContentletIndexAPI().indexContentList(batchList, null, false, new ActionListener<BulkResponse>() {
                      @Override
                      public void onResponse(BulkResponse bulkItemResponses) {
                          otherListenerList.stream().forEach(Runnable::run);
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
  }

  private boolean isOrdered(final Runnable runner) {

    return this.getOrder(runner) > 0;
  }

  private List<Runnable> getFlushers(final List<Runnable> allListeners) {
    return allListeners.stream().filter(this::isFlushCacheRunnable).collect(Collectors.toList());
  }

  private List<Runnable> getListeners(final List<Runnable> allListeners) {
    return allListeners.stream().filter(this::isNotFlushCacheRunnable).sorted(this::compare).collect(Collectors.toList());
  }

  private int compare(final Runnable runnable, final Runnable runnable1) {
    return this.getOrder(runnable).compareTo(this.getOrder(runnable1));
  }

  private Integer  getOrder(final Runnable runnable) {

    final int order = (runnable instanceof HibernateUtil.DotSyncRunnable)?
            HibernateUtil.DotSyncRunnable.class.cast(runnable).getOrder():0;

    return (runnable instanceof HibernateUtil.DotAsyncRunnable)?
            HibernateUtil.DotAsyncRunnable.class.cast(runnable).getOrder(): order;
  }

  private boolean isNotFlushCacheRunnable (final Runnable listener) {

      return !this.isFlushCacheRunnable(listener);
  }

  private boolean isFlushCacheRunnable (final Runnable listener) {

      return  (
                listener instanceof FlushCacheRunnable ||
                (listener instanceof HibernateUtil.DotAsyncRunnable
                        && HibernateUtil.DotAsyncRunnable.class.cast(listener).getRunnable() instanceof FlushCacheRunnable) ||
                (listener instanceof HibernateUtil.DotSyncRunnable
                        && HibernateUtil.DotSyncRunnable.class.cast(listener).getRunnable() instanceof FlushCacheRunnable)
              );
  }
}
