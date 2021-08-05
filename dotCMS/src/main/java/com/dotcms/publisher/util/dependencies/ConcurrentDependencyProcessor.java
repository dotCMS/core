package com.dotcms.publisher.util.dependencies;

import com.dotcms.concurrent.DotConcurrentFactory;
import com.dotcms.concurrent.DotSubmitter;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.eclipse.jetty.util.ConcurrentHashSet;

/**
 * it allow process a set of Assets to calculate its dependency in a multi-threading environment.
 *
 * You can register {@link Consumer} for each {@link PusheableAsset} using the
 * {@link ConcurrentDependencyProcessor#addProcessor(PusheableAsset, Consumer)} method:
 *
 * <pre>{@code
 *  ConcurrentDependencyProcessor processor = new ConcurrentDependencyProcessor();
 *   dependencyProcessor.addProcessor(PusheableAsset.CONTENTLET, (content) -> {
 *      try {
 *          processContentDependency((Contentlet) content);
 *      } catch (DotBundleException e) {
 *          Logger.error(DependencyManager.class, e.getMessage());
 *          throw new DotRuntimeException(e);
 *      }
 *   });
 * }</pre>
 *
 * Also you can add a Asset to be process like:
 *
 * <pre>{@code
 *   Contentlet contentlet = ...;
 *   dependencyProcessor.addAsset(PusheableAsset.CONTENTLET, contentlet);
 * }</pre>
 *
 * In the before code the contentlet it is going to be process by the <code>processContentDependency</code>
 * method.
 */
public class ConcurrentDependencyProcessor implements DependencyProcessor {

    private List<Throwable> errors;

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
    private Map<PusheableAsset, Consumer<Object>> consumerDependencies = new HashMap<>();

    ConcurrentDependencyProcessor() {
        queue = new LinkedBlockingDeque();
        assetsRequestToProcess = new ConcurrentHashMap<>();
        finishReceived = new AtomicInteger(0);
    }

    /**
     * Add a asset to process its dependencies, using the right {@link Consumer} according to the
     * <code>pusheableAsset</codee>.
     * To register a {@link Consumer} use the {@link ConcurrentDependencyProcessor#addProcessor(PusheableAsset, Consumer)}
     * method.
     *
     * @param asset
     * @param pusheableAsset
     *
     * @see ConcurrentDependencyProcessor#addProcessor(PusheableAsset, Consumer)
     */
    @Override
    public synchronized void addAsset(final Object asset, final PusheableAsset pusheableAsset) {

        final boolean added = addRequestToProcess(asset, pusheableAsset);

        if (added) {
            queue.add(new DependencyProcessorItem(asset, pusheableAsset));
        }
    }

    private <T> boolean addRequestToProcess(final T asset, final PusheableAsset pusheableAsset) {
        final String assetKey = DependencyManager.getBundleKey(asset);
        Set<String> set = assetsRequestToProcess.get(pusheableAsset);

        if (set == null) {
            set = new ConcurrentHashSet<>();
            assetsRequestToProcess.put(pusheableAsset, set);
        }

        return set.add(assetKey);

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
                    Logger.debug(ConcurrentDependencyProcessor.class, () -> "Waiting for more assets");
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

            if (this.errors != null && !this.errors.isEmpty()) {
                throw new ExecutionException(this.errors.get(0));
            }

            Logger.info(ConcurrentDependencyProcessor.class, "DependencyProcessor Finished");
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

    /**
     * Add a consumer
     *
     * @param pusheableAsset to be process by <code>consumer</code>
     * @param consumer
     */
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

                final Consumer<Object> proccesor = consumerDependencies.get(pusheableAsset);

                if (UtilMethods.isSet(proccesor)) {
                    proccesor.accept(dependencyProcessorItem.asset);
                }
            } catch(Exception e) {
                addError(e);
            } finally {
                sendFinishNotification();
                DbConnectionFactory.closeConnection();
            }
        }
    }

    private void addError(Exception error) {
        if (!UtilMethods.isSet(this.errors)) {
            this.errors = new ArrayList<>();
        }

        this.errors.add(error);
    }
}
