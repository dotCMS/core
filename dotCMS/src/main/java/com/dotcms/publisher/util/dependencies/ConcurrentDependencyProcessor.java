package com.dotcms.publisher.util.dependencies;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Process the Assets to calculate dependency in a multi-threading environment
 */
public class ConcurrentDependencyProcessor implements DependencyProcessor {

    private static class DependencyProcessorItem {
        static final  String FINISHED_ASSET_KEY = "finished";
        static final DependencyProcessorItem FINISHED_DEPENDENCY_PROCESSOR_ITEM =
                new DependencyProcessorItem(FINISHED_ASSET_KEY, null);

        Object asset;
        PusheableAsset pusheableAsset;

        public DependencyProcessorItem(Object asset, PusheableAsset pusheableAsset) {
            this.asset = asset;
            this.pusheableAsset = pusheableAsset;
        }
    }

    private BlockingQueue<DependencyProcessorItem> queue;
    private Map<PusheableAsset, Set<String>> assetsRequestToProcess;
    private AtomicInteger finishReceived;
    private DotSubmitter submitter;

    private Map<PusheableAsset, Consumer<Object>> consumerDependencies;

    ConcurrentDependencyProcessor() {
        queue = new LinkedBlockingDeque();
        assetsRequestToProcess = new ConcurrentHashMap<>();
        finishReceived = new AtomicInteger(0);
    }

    /**
     * Add a asset to process
     *
     * @param asset
     * @param pusheableAsset
     */
    @Override
    public synchronized void addAsset(final Object asset, final PusheableAsset pusheableAsset) {

        Logger.debug(ConcurrentDependencyProcessor.class, () -> String.format("%s: Putting %s in %s",
                Thread.currentThread().getName(), asset, pusheableAsset));

        queue.add(new DependencyProcessorItem(asset, pusheableAsset));
    }

    /**
     * The current thread wait until all the dependencies are processed
     * @throws ExecutionException
     */
    public void waitUntilResolveAllDependencies() throws ExecutionException {
        final String submitterName = "DependencyManagerSubmitter" + Thread.currentThread().getName();
        submitter = DotConcurrentFactory.getInstance().getSubmitter(submitterName,
                new DotConcurrentFactory.SubmitterConfigBuilder()
                        .poolSize(
                                Config.getIntProperty("MIN_NUMBER_THREAD_TO_EXECUTE_BUNDLER", 10))
                        .maxPoolSize(Config.getIntProperty("MAX_NUMBER_THREAD_TO_EXECUTE_BUNDLER", 40))
                        .queueCapacity(Config.getIntProperty("QUEUE_CAPACITY_TO_EXECUTE_BUNDLER", Integer.MAX_VALUE))
                        .build()
        );

        try {
            while (!isFinish()) {
                try {
                    Logger.debug(DependencyManager.class, () -> "Waiting for more assets");
                    final DependencyProcessorItem dependencyProcessorItem = queue.take();
                    Logger.debug(ConcurrentDependencyProcessor.class,
                            () -> "Taking one " + dependencyProcessorItem.asset);
                    if (!dependencyProcessorItem
                            .equals(DependencyProcessorItem.FINISHED_DEPENDENCY_PROCESSOR_ITEM)) {
                        submitter.submit(new DependencyRunnable(dependencyProcessorItem));
                    } else {
                        finishReceived.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            Logger.debug(ConcurrentDependencyProcessor.class, "DependencyProcessor Finished");
        } finally {
            submitter.shutdownNow();
        }
    }

    private synchronized void sendFinishNotification() {
        queue.add(DependencyProcessorItem.FINISHED_DEPENDENCY_PROCESSOR_ITEM);
    }

    private boolean isFinish() {
        return queue.isEmpty() && isAllTaskFinished();
    }

    private synchronized boolean isAllTaskFinished() {
        final long taskCount = submitter.getTaskCount();
        final int nFinishReceived = finishReceived.get();
        final Integer nRequest = assetsRequestToProcess.values().stream()
                .map(set -> set.size())
                .reduce(0, Integer::sum);

        return taskCount == nRequest && taskCount == nFinishReceived;
    }

    public void addProcessor(final PusheableAsset pusheableAsset, final Consumer<Object> consumer) {
        consumerDependencies.put(pusheableAsset, consumer);
    }

    private class DependencyRunnable implements Runnable {
        final DependencyProcessorItem dependencyProcessorItem;

        DependencyRunnable(final DependencyProcessorItem dependencyProcessorItem) {
            this.dependencyProcessorItem = dependencyProcessorItem;
        }

        @Override
        public void run() {
            try {
                final PusheableAsset pusheableAsset = dependencyProcessorItem.pusheableAsset;
                Logger.debug(ConcurrentDependencyProcessor.class,
                        () -> String.format("%s : We have something to process - %s %s",
                                Thread.currentThread().getName(), dependencyProcessorItem.asset, pusheableAsset));

                consumerDependencies.get(pusheableAsset).accept(dependencyProcessorItem.asset);
            } finally {
                sendFinishNotification();
                DbConnectionFactory.closeConnection();
            }
        }
    }
}
