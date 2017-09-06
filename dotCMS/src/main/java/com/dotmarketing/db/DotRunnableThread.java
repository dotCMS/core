package com.dotmarketing.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Config;

public class DotRunnableThread extends Thread {

    final List<DotRunnable> listeners;

    public DotRunnableThread(List<DotRunnable> listeners) {

        this.listeners = listeners;
    }

    @Override
    public void run() {
        try {
            LocalTransaction.wrap(this::_internalRunner);
        } catch (Exception dde) {
            throw new DotStateException(dde);
        }
    }

    private void _internalRunner() {
        final Set<String> reindexInodes = new HashSet<String>();
        List<Contentlet> contentToIndex = new ArrayList<Contentlet>();


        final List<List<Contentlet>> listOfLists = new ArrayList<List<Contentlet>>();
        final int batchSize = Config.getIntProperty("INDEX_COMMIT_LISTENER_BATCH_SIZE", 50);
        for (DotRunnable runner : listeners) {
            if (runner instanceof ReindexRunnable) {
                ReindexRunnable rrunner = (ReindexRunnable) runner;
                if (rrunner.getAction().equals(ReindexRunnable.Action.REMOVING)) {
                    rrunner.run();
                    continue;
                }
                List<Contentlet> cons = rrunner.getReindexIds();
                for (Contentlet con : cons) {
                    if (!reindexInodes.contains(con.getInode())) {
                        reindexInodes.add(con.getInode());
                        contentToIndex.add(con);
                        if (contentToIndex.size() == batchSize) {
                            listOfLists.add(contentToIndex);
                            contentToIndex = new ArrayList<Contentlet>();
                        }
                    }
                }
            } else {
                runner.run();
            }
        }
        listOfLists.add(contentToIndex);
        for (List<Contentlet> batchList : listOfLists) {
            new ReindexRunnable(batchList, ReindexRunnable.Action.ADDING, null, false) {
            }.run();
        }
    }
}