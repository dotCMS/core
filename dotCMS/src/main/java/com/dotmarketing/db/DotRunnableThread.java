package com.dotmarketing.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;

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


    final List<List<Contentlet>> listOfLists = new ArrayList<>();
    final int batchSize = Config.getIntProperty("INDEX_COMMIT_LISTENER_BATCH_SIZE", 50);
    for (final DotRunnable runner : listeners) {
      if (runner instanceof ReindexRunnable) {
        ReindexRunnable rrunner = (ReindexRunnable) runner;
        if (rrunner.getAction().equals(ReindexRunnable.Action.REMOVING)) {
          rrunner.run();
          continue;
        }
        final List<Contentlet> cons = rrunner.getReindexIds();
        for (Contentlet con : cons) {
          if (reindexInodes.add(con.getInode())) {
            contentToIndex.add(con);
            if (contentToIndex.size() == batchSize) {
              listOfLists.add(contentToIndex);
              contentToIndex = new ArrayList<>();
            }
          }
        }
      } else {
        runner.run();
      }
    }
    listOfLists.add(contentToIndex);
    for (final List<Contentlet> batchList : listOfLists) {
      new ReindexRunnable(batchList, ReindexRunnable.Action.ADDING, null, false) {}.run();
    }
  }

  private List<DotRunnable> getFlushers(final List<DotRunnable> allListeners) {
    return allListeners.stream().filter(listener -> listener instanceof FlushCacheRunnable).collect(Collectors.toList());
  }

  private List<DotRunnable> getListeners(final List<DotRunnable> allListeners) {
    return allListeners.stream().filter(listener -> (listener instanceof FlushCacheRunnable == false))
        .collect(Collectors.toList());
  }
}
