package com.dotcms.concurrent;

import com.dotcms.concurrent.lock.ClusterLockManager;
import com.dotcms.concurrent.lock.ClusterLockManagerFactory;
import com.dotcms.concurrent.lock.DotKeyLockManagerBuilder;
import com.dotcms.concurrent.lock.DotKeyLockManagerFactory;
import com.dotcms.concurrent.lock.IdentifierStripedLock;
import com.dotcms.util.ConversionUtils;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Factory for concurrent {@link Executor} & {@link DotSubmitter}
 * @author jsanca
 */
public class DotConcurrentFactory implements DotConcurrentFactoryMBean, Serializable {

    private static final int POOL_SIZE_VAL = 10;
    private static final int MAXPOOL_SIZE_VAL = 50;
    private static final int QUEUE_CAPACITY_VAL = Integer.MAX_VALUE;

    public static final String DOTCMS_CONCURRENT_CONDITIONAL_EXECUTOR_DEFAULT_SIZE  = "dotcms.concurrent.conditionalexecutor.default.size";
    /**
     * In case you want to override the {@link ThreadFactory}, by default using the {@link Executors}.defaultThreadFactory();
     */
    public static final String DOTCMS_CONCURRENT_THREADFACTORYCLASS     = "dotcms.concurrent.threadfactoryclass";

    /**
     * In case you want to override the allow core thread timeout, use this property. By default is false.
     */
    public static final String DOTCMS_CONCURRENT_ALLOWCORETHREADTIMEOUT = "dotcms.concurrent.allowcorethreadtimeout";

    /**
     * In case you want to override the pool size, use this property, by default it is 10.
     */
    public static final String DOTCMS_CONCURRENT_POOLSIZE               = "dotcms.concurrent.poolsize";

    /**
     * In case you want to override the max pool size, use this property, by default it is 50
     */
    public static final String DOTCMS_CONCURRENT_MAXPOOLSIZE            = "dotcms.concurrent.maxpoolsize";

    /**
     * In case you want to override the keep alive millis, use this property, by default it is 60000 millis (60 seconds)
     */
    public static final String DOTCMS_CONCURRENT_KEEPALIVEMILLIS        = "dotcms.concurrent.keepalivemillis";

    /**
     * In case you want to override the queue capacity, use this property, by default it is 100
     */
    public static final String DOTCMS_CONCURRENT_QUEUECAPACITY          = "dotcms.concurrent.queuecapacity";
    public static final String MBEAN_OBJECT_NAME = "org.dotcms:type=DotConcurrent";

    public static final String DOT_SYSTEM_THREAD_POOL = "dotSystemPool";

    public static final String DOT_SINGLE_SYSTEM_THREAD_POOL = "dotSingleSystemPool";

    public static final String BULK_ACTIONS_THREAD_POOL = "bulkActionsPool";

    public static final String LOCK_MANAGER = "IdentifierStripedLock";

    /**
     * Used to keep the instance of the submitter
     * Should be volatile to avoid thread-caching
     */
    private final Map<String, DotSubmitter> submitterMap =
            new ConcurrentHashMap<>();

    /**
     * Creator map
     */
    private final Map<String, SubmitterConfig> submitterConfigCreatorMap =
            new ConcurrentHashMap<>();

    /**
     * Keeps a concurrent delay queue (for subscribe or unsubscribe)
     */
    private final Map<Integer, BlockingQueue<DelayedDelegate>> delayQueueMap =
            new ConcurrentHashMap<>();

    private final DelayQueueConsumer delayQueueConsumer;

    private final int defaultPoolSize =
            Config.getIntProperty(DOTCMS_CONCURRENT_POOLSIZE, POOL_SIZE_VAL);

    private final boolean defaultAllowCoreThreadTimeOut =
            Config.getBooleanProperty(DOTCMS_CONCURRENT_ALLOWCORETHREADTIMEOUT, Boolean.FALSE);

    private final int defaultMaxPoolSize =
            Config.getIntProperty(DOTCMS_CONCURRENT_MAXPOOLSIZE, MAXPOOL_SIZE_VAL);

    private final long defaultKeepAliveMillis =
            Config.getLongProperty(DOTCMS_CONCURRENT_KEEPALIVEMILLIS, DateUtil.MINUTE_MILLIS);

    private final int defaultQueueCapacity =
            Config.getIntProperty(DOTCMS_CONCURRENT_QUEUECAPACITY, QUEUE_CAPACITY_VAL);

    private final RejectedExecutionHandler rejectedExecutionHandler =
            new ThreadPoolExecutor.AbortPolicy();

    public static final String SCHEDULER_COREPOOLSIZE = "SCHEDULER_CORE_POOL_SIZE";

    private final IdentifierStripedLock identifierStripedLock =
           new IdentifierStripedLock(DotKeyLockManagerBuilder.newLockManager(LOCK_MANAGER));

    // Cluster lock manager
    private final DotKeyLockManagerFactory clusterLockManagerFactory =
            new ClusterLockManagerFactory(); // todo: this should be overridable by osgi.

    // Stores the cluster lock manager by name
    private Map<String, ClusterLockManager<String>> clusterLockManagerMap =
            new ConcurrentHashMap<>();

    private static ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = null;

    /**
     * Returns the {@link ScheduledThreadPoolExecutor}
     * @return ScheduledThreadPoolExecutor
     */
    public static ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {

        if (null == scheduledThreadPoolExecutor) {

            synchronized (DotInitScheduler.class) {

                if (null == scheduledThreadPoolExecutor) {
                    final int corePoolSize = Config.getIntProperty(SCHEDULER_COREPOOLSIZE, 5);
                    scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor( corePoolSize,new ThreadFactoryBuilder().setDaemon(true).setNameFormat("dot-ScheduledPool-%d").build(),new ThreadPoolExecutor.CallerRunsPolicy() );
                }
            }
        }

        return scheduledThreadPoolExecutor;
    }


    final static ThreadFactory buildDefaultThreadFactory(final String executorName) {

        if (UtilMethods.isEmpty(executorName)) {
            new ThreadFactoryBuilder().setDaemon(true).setNameFormat("dotCMS-%d").build();
        }

        final String className = Config.getStringProperty(executorName + DOTCMS_CONCURRENT_THREADFACTORYCLASS, null);

        if (UtilMethods.isSet(className)) {
            Logger.debug(DotConcurrentFactory.class, "Using the thread factory implementation: " + className);

            return (ThreadFactory) ReflectionUtils.newInstance(className);
        }


        return new ThreadFactoryBuilder().setDaemon(true).setNameFormat("dot-" + executorName + "-%d").build();

    }

    private DotConcurrentFactory () {
        // singleton
        this.delayQueueConsumer = new DelayQueueConsumer();
        getScheduledThreadPoolExecutor().scheduleWithFixedDelay(this.delayQueueConsumer, 0,
                Config.getLongProperty("dotcms.concurrent.delayqueue.waitmillis", 100), TimeUnit.MILLISECONDS);
    }

    private void subscribeDelayQueue(final BlockingQueue<DelayedDelegate> delayedQueue) {

        this.delayQueueMap.put(delayedQueue.hashCode(), delayedQueue);
    }

    private void unsubscribeDelayQueue(final BlockingQueue<DelayedDelegate> delayedQueue) {

        if (null != delayedQueue) {
            this.delayQueueMap.remove(delayedQueue.hashCode());
        }
    }

    @Override
    public Map<String, Object> getStats(final String name) {

        final DotSubmitter dotConcurrent =
                this.submitterMap.get(name);

        return (null != dotConcurrent)?
                (dotConcurrent instanceof DotConcurrentImpl)?
                        Map.of(
                        "name",        name,
                        "threadPool",  DotConcurrentImpl.class.cast(dotConcurrent).getThreadPoolExecutor().toString(),
                        "maxPoolSize", DotConcurrentImpl.class.cast(dotConcurrent).getThreadPoolExecutor().getMaximumPoolSize(),
                        "keepAlive",   DotConcurrentImpl.class.cast(dotConcurrent).getThreadPoolExecutor().getKeepAliveTime(TimeUnit.MILLISECONDS),
                        "queue-length",DotConcurrentImpl.class.cast(dotConcurrent).getThreadPoolExecutor().getQueue().size(),
                        "activeCount", DotConcurrentImpl.class.cast(dotConcurrent).getThreadPoolExecutor().getActiveCount(),
                        "completedTaskCount", DotConcurrentImpl.class.cast(dotConcurrent).getThreadPoolExecutor().getCompletedTaskCount(),
                        "TaskCount", DotConcurrentImpl.class.cast(dotConcurrent).getThreadPoolExecutor().getTaskCount(),
                        "queue",       toString(DotConcurrentImpl.class.cast(dotConcurrent).getThreadPoolExecutor().getQueue()),
                        "isShutdown",  DotConcurrentImpl.class.cast(dotConcurrent).shutdown
                        ):
                        Map.of(
                                "name",        name,
                                "threadPool",  "noInfo",
                                "maxPoolSize", dotConcurrent.getMaxPoolSize(),
                                "keepAlive",   -1,
                                "queue-length","noInfo",
                                "activeCount", "noInfo",
                                "completedTaskCount", "noInfo",
                                "TaskCount", "noInfo",
                                "queue",       "noInfo",
                                "isShutdown",  dotConcurrent.isAborting()
                        )
                :Map.of(
                        "name",        name,
                        "threadPool",  "noInfo",
                        "maxPoolSize", -1,
                        "keepAlive",   -1,
                        "queue-length","noInfo",
                        "activeCount", "noInfo",
                        "completedTaskCount", "noInfo",
                        "TaskCount", "noInfo",
                        "queue",       "noInfo",
                        "isShutdown",  false
                );
    }

    private Object toString(final BlockingQueue<Runnable> queue) {

        final StringBuilder builder = new StringBuilder();

        if (null != queue) {

            final Iterator<Runnable> threadsOnQueue = queue.iterator();
            while (threadsOnQueue.hasNext()) {

                builder.append(threadsOnQueue.next());
            }
        }

        return builder.toString();
    }



    @Override
    public Boolean shutdown(final String name){
        final DotSubmitter dotConcurrent =
                this.submitterMap.get(name);
        if(null == dotConcurrent){
           return false;
        }
        dotConcurrent.shutdown();
        return true;
    }

    @Override
    public void shutdownAndDestroy() {

        this.delayQueueConsumer.stopQueue();
        this.submitterMap.values().stream().forEach(DotSubmitter::shutdown);
        this.submitterMap.clear();
        this.delayQueueMap.clear();
    }

    @Override
    public List<String> list() {
        return new ArrayList<>(submitterMap.keySet());
    }

    @Override
    public String getObjectName() {

        return MBEAN_OBJECT_NAME;
    }

    /**
     * Get the Future return without timeout and throws {@link DotConcurrentException} (which is runtime) on case of error, it helps to be used on lamdbas/
     * @param future
     * @param <T>
     * @return T
     */
    public static <T>T get (final Future<T> future) {

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new DotConcurrentException(e);
        }
    }

    private static class SingletonHolder {
        private static final DotConcurrentFactory INSTANCE = new DotConcurrentFactory();
    }
    /**
     * Get the instance.
     * @return DotConcurrentFactory
     */
    public static DotConcurrentFactory getInstance() {

        return DotConcurrentFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Get's the default submitter
     * @return DotSubmitter
     */
    public DotSubmitter getSubmitter () {

        return this.getSubmitter(DOT_SYSTEM_THREAD_POOL);
    } // getSubmitter.

    /**
     * Register a submitterConfig that will act as instantiator and gets the submitter
     * @param name    {@link String}
     * @param creator {@link SubmitterConfig}
     * @return DotSubmitter
     */
    public DotSubmitter getSubmitter (final String name, final SubmitterConfig creator) {

        this.registerSubmitterCreator(name, creator);
        return this.getSubmitter(name);
    }

    /**
     * Register submitter creator
     * @param name
     * @param creator
     */
    public void registerSubmitterCreator (final String name, final SubmitterConfig creator) {

        this.submitterConfigCreatorMap.putIfAbsent(name, creator);
    }

    /**
     * Get the default single thread submitter
     * @return DotSubmitter
     */
    public DotSubmitter getSingleSubmitter () {

        return this.getSingleSubmitter(DOT_SINGLE_SYSTEM_THREAD_POOL);
    }

    /**
     * Creates a default Conditional executor with the default size
     * @return ConditionalExecutor
     */
    public ConditionalSubmitter createConditionalSubmitter() {

        return new ConditionalSubmitterImpl(Config.getIntProperty("DOTCMS_CONCURRENT_CONDITIONAL_EXECUTOR_DEFAULT_SIZE", 10));
    }

    /**
     * Creates a default conditional executor with a given size
     * @param size {@link Integer}
     * @return ConditionalExecutor
     */
    public ConditionalSubmitter createConditionalSubmitter(final int size) {

        return new ConditionalSubmitterImpl(size);
    }

    /**
     * Creates a default conditional executor with a given size
     * @param size {@link Integer}
     * @return ConditionalExecutor
     */
    public ConditionalSubmitter createConditionalSubmitter(final int size, long secondsTimeOut) {

        return new ConditionalSubmitterImpl(size, secondsTimeOut, TimeUnit.SECONDS);
    }


    /**
     * Get the default single thread submitter by name
     * @param name {@link String} name of the {@link DotSubmitter}
     * @return DotSubmitter
     */
    public DotSubmitter getSingleSubmitter (final String name) {

        DotSubmitter submitter = null;

        if (!this.submitterMap.containsKey(name)) {

            synchronized (DotConcurrentFactory.class) {

                if (null == submitter) {

                    submitter = new DotSingleSubmitterImpl(name);
                    this.submitterMap.put(name, submitter);
                }
            }
        } else {

            submitter =
                    this.submitterMap.get(name);

            if (null != submitter && (submitter.isAborting())) { // if it is shutdown, create a new one

                synchronized (DotConcurrentFactory.class) {

                    submitter = new DotSingleSubmitterImpl(name);
                    this.submitterMap.put(name, submitter);
                }
            }
        }

        return submitter;
    }

    /**
     * Get's the submitter for a submitterName parameter
     * The submitterName is used as a prefix for all these properties:
     *
     * DOTCMS_CONCURRENT_THREADFACTORYCLASS
     * DOTCMS_CONCURRENT_ALLOWCORETHREADTIMEOUT
     * DOTCMS_CONCURRENT_POOLSIZE
     * DOTCMS_CONCURRENT_MAXPOOLSIZE
     * DOTCMS_CONCURRENT_KEEPALIVEMILLIS
     * DOTCMS_CONCURRENT_QUEUECAPACITY
     *
     * @param name {@link String} the submitter name is the preffix for the dotcms.concurrent properties
     * @return DotSubmitter
     */
    public DotSubmitter getSubmitter (final String name) {

        DotConcurrentImpl submitter = null;

        if (!this.submitterMap.containsKey(name)) {

            synchronized (DotConcurrentFactory.class) {

                if (null == submitter) {

                    submitter =
                            this.createDotConcurrent(name);
                }
            }
        } else {

            submitter =
                    (DotConcurrentImpl)this.submitterMap.get(name);

            if (null != submitter && (submitter.shutdown || submitter.threadPoolExecutor
                    .isTerminated())) { // if it is shutdown, create a new one

                synchronized (DotConcurrentFactory.class) {

                    submitter =
                            this.createDotConcurrent(name);
                }
            }
        }

        return submitter;
    } // getSubmitter.

    private DotConcurrentImpl createDotConcurrent (final String name) {

        final DotConcurrentImpl submitter = this.submitterConfigCreatorMap.containsKey(name)?
                new DotConcurrentImpl(
                        this.submitterConfigCreatorMap.get(name).getDefaultThreadFactory(name),
                        this.submitterConfigCreatorMap.get(name).getRejectedExecutionHandler(),
                        this.submitterConfigCreatorMap.get(name).getAllowCoreThreadTimeOut(),
                        this.submitterConfigCreatorMap.get(name).getPoolSize(),
                        this.submitterConfigCreatorMap.get(name).getMaxPoolSize(),
                        this.submitterConfigCreatorMap.get(name).getKeepAliveMillis(),
                        this.submitterConfigCreatorMap.get(name).getQueueCapacity(),
                        name
                ):
                new DotConcurrentImpl(
                        buildDefaultThreadFactory(name),
                        this.rejectedExecutionHandler,
                        Config.getBooleanProperty(name + DOTCMS_CONCURRENT_ALLOWCORETHREADTIMEOUT, this.defaultAllowCoreThreadTimeOut),
                        Config.getIntProperty (name  + DOTCMS_CONCURRENT_POOLSIZE,                 this.defaultPoolSize),
                        Config.getIntProperty (name  + DOTCMS_CONCURRENT_MAXPOOLSIZE,              this.defaultMaxPoolSize),
                        Config.getLongProperty(name  + DOTCMS_CONCURRENT_KEEPALIVEMILLIS,          this.defaultKeepAliveMillis),
                        Config.getIntProperty (name  + DOTCMS_CONCURRENT_QUEUECAPACITY,            this.defaultQueueCapacity),
                        name
                );

        this.submitterMap.put(name, submitter);

        return submitter;
    }

    /**
     * Gets or creates a cluster wide lock manager lock
     * @param name {@link String}
     * @return DotKeyLockManager
     */
    public ClusterLockManager<String> getClusterLockManager(final String name) {

        return this.clusterLockManagerMap.computeIfAbsent(name, key-> (ClusterLockManager) this.clusterLockManagerFactory.create(name));
    }

    /**
     * returns an singleton instance of the identifier strip locked manager;
     * @return
     */
    public IdentifierStripedLock getIdentifierStripedLock(){
       return this.identifierStripedLock;
    }

    /**
     * Create a composite completable futures and results any of the first results done of the futures parameter.
     * @param futures
     * @return
     * @param <T>
     */
    public <T> CompletableFuture<T> toCompletableAnyFuture(final Collection<Future<T>> futures) {

        return toCompletableAnyFuture(futures.toArray(new Future[futures.size()]));
    }

    /**
     * Create a composite completable futures and results any of the first results done of the futures parameter.
     * @param futures
     * @return
     * @param <T>
     */
    public <T> CompletableFuture<T> toCompletableAnyFuture(final Future<T>... futures) {

        final CompletableFuture<T>[] completableFutures = ConversionUtils.INSTANCE.convertToArray(
                this::toCompletableFuture, CompletableFuture.class, futures);

        return (CompletableFuture<T>) CompletableFuture.anyOf(completableFutures);
    }
    /**
     * Convert a simple future to a completable future
     * @param future
     * @return
     * @param <T>
     */
    public <T> CompletableFuture<T> toCompletableFuture(final Future<T> future) {

        return future.isDone()?
                toDoneFuture(future):
                CompletableFuture.supplyAsync(() -> {
                    try {
                        if (!future.isDone()) {
                            awaitFutureIsDoneInForkJoinPool(future);
                        }
                        return future.get();
                    } catch (final ExecutionException e) {
                        throw new DotRuntimeException(e);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new DotRuntimeException(e);
                    }
                });
    }

    private <T> CompletableFuture<T> toDoneFuture(final Future<T> future) {

        final CompletableFuture<T> completableFuture = new CompletableFuture<>();
        T result;
        try {
            result = future.get();
        } catch (final InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
            completableFuture.completeExceptionally(ex);
            return completableFuture;
        } catch (final Throwable ex) {
            completableFuture.completeExceptionally(ex);
            return completableFuture;
        }
        completableFuture.complete(result);
        return completableFuture;
    }

    private static void awaitFutureIsDoneInForkJoinPool(final Future<?> future)
            throws InterruptedException {
        ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
            @Override public boolean block() throws InterruptedException {
                try {
                    future.get();
                } catch (final ExecutionException e) {
                    throw new DotRuntimeException(e);
                }
                return true;
            }
            @Override public boolean isReleasable() {
                return future.isDone();
            }
        });
    }

    /**
     * {@link SubmitterConfig} builder
     */
    public static class SubmitterConfigBuilder {

        private ThreadFactory            threadFactory;
        private RejectedExecutionHandler rejectedExecutionHandler;
        private Boolean allowCoreThreadTimeOut;
        private Integer poolSize;
        private Integer maxPoolSize;
        private Long    keepAliveMillis;
        private Integer queueCapacity;

        public SubmitterConfigBuilder defaultThreadFactory(final ThreadFactory threadFactory) {
            this.threadFactory = threadFactory; return this;
        }

        public SubmitterConfigBuilder rejectedExecutionHandler(final RejectedExecutionHandler rejectedExecutionHandler) {
            this.rejectedExecutionHandler = rejectedExecutionHandler;  return this;
        }

        public SubmitterConfigBuilder allowCoreThreadTimeOut(final boolean allowCoreThreadTimeOut) {
            this.allowCoreThreadTimeOut = allowCoreThreadTimeOut; return this;
        }

        public SubmitterConfigBuilder poolSize (final int poolSize) {
            this.poolSize = poolSize; return this;
        }

        public SubmitterConfigBuilder maxPoolSize (final int maxPoolSize) {
            this.maxPoolSize = maxPoolSize; return this;
        }

        public SubmitterConfigBuilder keepAliveMillis(final long keepAliveMillis) {
            this.keepAliveMillis = keepAliveMillis; return this;
        }

        public SubmitterConfigBuilder queueCapacity(final int queueCapacity) {
            this.queueCapacity = queueCapacity; return this;
        }

        public SubmitterConfig build () {
            return new SubmitterConfig() {
                @Override
                public ThreadFactory getDefaultThreadFactory(final String name) {
                    return null != SubmitterConfigBuilder.this.threadFactory?
                            SubmitterConfigBuilder.this.threadFactory: buildDefaultThreadFactory(name);
                }

                @Override
                public RejectedExecutionHandler getRejectedExecutionHandler() {
                    return null != SubmitterConfigBuilder.this.rejectedExecutionHandler?
                            SubmitterConfigBuilder.this.rejectedExecutionHandler: SubmitterConfig.super.getRejectedExecutionHandler();
                }

                @Override
                public boolean getAllowCoreThreadTimeOut() {
                    return null != SubmitterConfigBuilder.this.allowCoreThreadTimeOut?
                            SubmitterConfigBuilder.this.allowCoreThreadTimeOut: SubmitterConfig.super.getAllowCoreThreadTimeOut();
                }

                @Override
                public int getPoolSize() {
                    return null != SubmitterConfigBuilder.this.poolSize?
                            SubmitterConfigBuilder.this.poolSize: SubmitterConfig.super.getPoolSize();
                }

                @Override
                public int getMaxPoolSize() {
                    return null != SubmitterConfigBuilder.this.maxPoolSize?
                            SubmitterConfigBuilder.this.maxPoolSize: SubmitterConfig.super.getMaxPoolSize();
                }

                @Override
                public long getKeepAliveMillis() {
                    return null != SubmitterConfigBuilder.this.keepAliveMillis?
                            SubmitterConfigBuilder.this.keepAliveMillis: SubmitterConfig.super.getKeepAliveMillis();
                }

                @Override
                public int getQueueCapacity() {
                    return null != SubmitterConfigBuilder.this.queueCapacity?
                            SubmitterConfigBuilder.this.queueCapacity: SubmitterConfig.super.getQueueCapacity();
                }
            };
        }
    }

    /**
     * In case you want to configure by code.
     */
    public interface SubmitterConfig {

        /**
         * Returns "Executors.defaultThreadFactory()"
         * @return ThreadFactory
         */
        default ThreadFactory getDefaultThreadFactory (final String name) {
            return getDefaultThreadFactory(name);
        }

        /**
         * Returns AbortPolicy
         * @return RejectedExecutionHandler
         */
        default RejectedExecutionHandler getRejectedExecutionHandler() {
            return new ThreadPoolExecutor.CallerRunsPolicy();
        }

        /**
         * By default does not allows allow core time out
         * @return boolean
         */
        default boolean getAllowCoreThreadTimeOut() {
            return Config.getBooleanProperty(DOTCMS_CONCURRENT_ALLOWCORETHREADTIMEOUT, Boolean.FALSE);
        }

        /**
         * Returns 10 as a default pool size
         * @return int
         */
        default int getPoolSize () {
            return Config.getIntProperty(DOTCMS_CONCURRENT_POOLSIZE, POOL_SIZE_VAL);
        }

        /**
         * Returns 50 as a default max pool size
         * @return int
         */
        default int getMaxPoolSize () {
            return Config.getIntProperty(DOTCMS_CONCURRENT_MAXPOOLSIZE, MAXPOOL_SIZE_VAL);
        }

        /**
         * Returns one minute as a keep alive
         * @return long
         */
        default long getKeepAliveMillis() {
            return Config.getLongProperty(DOTCMS_CONCURRENT_KEEPALIVEMILLIS, DateUtil.MINUTE_MILLIS);
        }

        /**
         * Returns 100 as a queue capacity
         * @return int
         */
        default int getQueueCapacity() {
            return Config.getIntProperty(DOTCMS_CONCURRENT_QUEUECAPACITY, QUEUE_CAPACITY_VAL);
        }
    }

    //// DelayQueueConsumer

    /**
     * This class is in charge of process all the delay queues
     */
    public class DelayQueueConsumer implements Runnable {

        private AtomicBoolean stop = new AtomicBoolean(false);

        public DelayQueueConsumer() {
            super();
        }

        public void stopQueue () {

            this.stop.set(true);
        }

        @Override
        public void run() {
            final List<DelayedDelegate> delayedDelegateList = new ArrayList<>();
            if (!this.stop.get()) {

                try {

                    final Collection<BlockingQueue<DelayedDelegate>> delayedQueues =
                            DotConcurrentFactory.this.delayQueueMap.values();
                    
                    for (final BlockingQueue<DelayedDelegate> queue: delayedQueues) {
                        Logger.debug(this.getClass(), ()->"DelayedDelegate queue size:"  + queue.size());
                        delayedDelegateList.clear();
                        // take up to 100 DelayedDelegates to run at a time
                        queue.drainTo(delayedDelegateList, 50); 
                        for (DelayedDelegate d: delayedDelegateList) {
                            d.executeDelegate();
                            Thread.sleep(5);
                        }
                        Thread.sleep(5);
                    }
                    
        
                } catch (Exception e) {
                    Logger.error(DotConcurrentFactory.class, e.getMessage(), e);
                }
            }
        }
    } // DelayQueueConsumer.

    /// DotSingleSubmitterImpl
    private final class DotSingleSubmitterImpl implements DotSubmitter {

        private final String name;
        private final ExecutorService executorService = Executors.newSingleThreadExecutor();

        public DotSingleSubmitterImpl(final String name) {
            this.name = name;
        }

        @Override
        public Future<?> submit(final Runnable command) {
            return this.executorService.submit(command);
        }

        @Override
        public void delay(final Runnable task, final long delay, final TimeUnit unit) {

            throw new UnsupportedOperationException("Delay not supported on single submitter");
        }

        @Override
        public Future<?> submit(final Runnable command, final long delay, final TimeUnit unit) {

            throw new UnsupportedOperationException("Submit Delay not supported on single submitter");
        }

        @Override
        public <T> Future<T> submit(final Callable<T> callable) {

            return this.executorService.submit(callable);
        }

        @Override
        public <T> Future<T> submit(Callable<T> callable, long delay, TimeUnit unit) {

            throw new UnsupportedOperationException(
                    "Submit Delay not supported on single submitter, name: " + this.name);
        }

        @Override
        public int getActiveCount() {
            return 1;
        }

        @Override
        public int getPoolSize() {
            return 1;
        }

        @Override
        public int getMaxPoolSize() {
            return 1;
        }

        @Override
        public void shutdown() {

            this.executorService.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return this.executorService.shutdownNow();
        }

        @Override
        public boolean isAborting() {
            return this.executorService.isTerminated() ||  this.executorService.isShutdown();
        }

        @Override
        public void waitForAll(long timeout, TimeUnit unit) throws ExecutionException {
            try {
                executorService.awaitTermination(timeout, unit);
            } catch (InterruptedException e) {
                throw new DotRuntimeException(e.getMessage() + ", name: " + this.name, e);
            }
        }

        @Override
        public void waitForAll() throws ExecutionException {
            while(executorService.isTerminated()) {
                waitForAll(10, TimeUnit.MINUTES);
            }
        }

        @Override
        public long getTaskCount() {
            throw new UnsupportedOperationException("Submit Delay not supported on single submitter, name: " + this.name);
        }

        @Override
        public void execute(final Runnable command) {

            this.executorService.execute(command);
        }
    }

    private final class DotThreadPoolExecutor extends ThreadPoolExecutor {

        private final String name;

        public DotThreadPoolExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit,
                                     final BlockingQueue<Runnable> workQueue, final String name) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, buildDefaultThreadFactory(name));
            this.name = name;
        }

        public DotThreadPoolExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit,
                                     final BlockingQueue<Runnable> workQueue, final ThreadFactory threadFactory, final String name) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
            this.name = name;
        }

        public DotThreadPoolExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit,
                                     final BlockingQueue<Runnable> workQueue, final RejectedExecutionHandler handler, final String name) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,buildDefaultThreadFactory(name), handler);
            this.name = name;
        }

        public DotThreadPoolExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit,
                                     final BlockingQueue<Runnable> workQueue, final ThreadFactory threadFactory, final RejectedExecutionHandler handler, final String name) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
            this.name = name;
        }

        @Override
        public String toString() {
            return "name = " + this.name + " {"+ super.toString() + "}";
        }
    }

    /// DotSubmitter
    private final class DotConcurrentImpl implements DotSubmitter {

        private final ThreadPoolExecutor threadPoolExecutor;
        private final BlockingQueue<DelayedDelegate> delayedQueue = new DelayQueue<>();
        private volatile boolean shutdown;

        /**
         * Constructor.
         * @param threadFactory              {@link ThreadFactory}
         * @param rejectedExecutionHandler   {@link RejectedExecutionHandler}
         * @param allowCoreThreadTimeOut     {@link Boolean}
         * @param poolSize                   {@link Integer}
         * @param maxPoolSize                {@link Integer}
         * @param keepAliveMillis            {@link Long}
         * @param queueCapacity              {@link Integer}
         */
        DotConcurrentImpl(
                final ThreadFactory threadFactory,
                final RejectedExecutionHandler rejectedExecutionHandler,
                final boolean allowCoreThreadTimeOut,
                final int poolSize,
                final int maxPoolSize,
                final long keepAliveMillis,
                final int queueCapacity,
                final String name
                ) {

            final BlockingQueue<Runnable> queue = this.createQueue(queueCapacity);
            this.threadPoolExecutor  = new DotThreadPoolExecutor(
                    poolSize, maxPoolSize, keepAliveMillis, TimeUnit.MILLISECONDS,
                    queue, threadFactory, rejectedExecutionHandler, name);

            if (allowCoreThreadTimeOut) {

                this.threadPoolExecutor.allowCoreThreadTimeOut
                        (allowCoreThreadTimeOut);
            }

            DotConcurrentFactory.this.subscribeDelayQueue (this.delayedQueue);

        } // DotConcurrentImpl.

        final BlockingQueue<Runnable> createQueue(final int queueCapacity) {

            return  (queueCapacity > 0)?
                new LinkedBlockingQueue<Runnable>(queueCapacity): new SynchronousQueue<Runnable>();
        }

        final ThreadPoolExecutor getThreadPoolExecutor() {

            return this.threadPoolExecutor;
        }

        @Override
        public int getPoolSize() {
            return this.threadPoolExecutor.getPoolSize();
        }

        @Override
        public int getMaxPoolSize() {
            return this.threadPoolExecutor.getMaximumPoolSize();
        }

        @Override
        public final void execute(final Runnable command) {

            final Executor executor =
                    this.getThreadPoolExecutor();

            try {

                if (null != command) {

                    executor.execute(command);
                }
            } catch (RejectedExecutionException ex) {

                throw new DotConcurrentException(ex.getMessage(), ex);
            }
        } // execute.

        @Override
        public final Future<?> submit(final Runnable task) {

            final ExecutorService executor =
                    this.getThreadPoolExecutor();
            Future<?> future = null;

            try {

                future =
                    executor.submit(task);
            } catch (RejectedExecutionException ex) {
                throw new DotConcurrentException(ex.getMessage(), ex);
            }

            return future;
        } // submit.

        @Override
        public final void delay(final Runnable task, final long delay, final TimeUnit unit) {

            try {
                this.delayedQueue.put(new DelayedDelegate(()-> {

                    task.run();
                }, delay, unit));
            } catch (InterruptedException e) {
                throw new DotConcurrentException(e.getMessage(), e);
            }
        }


        @Override
        public final Future<?> submit(final Runnable task, final long delay, final TimeUnit unit) {

            return this.submit(() -> {

                sleep(delay, unit);
                task.run();
            });
        }

        private void sleep(final long delay, final TimeUnit unit) {

            try {
                unit.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public final <T> Future<T> submit(final Callable<T> task) {

            final ExecutorService executor =
                    this.getThreadPoolExecutor();
            Future<T> future = null;

            try {

                future =
                    executor.submit(task);
            } catch (RejectedExecutionException ex) {

                throw new DotConcurrentException(ex.getMessage(), ex);
            }

            return future;
        } // submit.

        @Override
        public final <T> Future<T> submit(final Callable<T> callable, final long delay, final TimeUnit unit) {

            return this.submit(() -> {

                sleep(delay, unit);
                return callable.call();
            });
        }

        @Override
        public int getActiveCount() {

            return this.threadPoolExecutor.getActiveCount();
        }

        @Override
        public void shutdown() {

            this.threadPoolExecutor.shutdown();
            this.shutdown = true;
            this.delayedQueue.clear();
            DotConcurrentFactory.this.unsubscribeDelayQueue(this.delayedQueue);
        }

        @Override
        public List<Runnable> shutdownNow() {

            this.delayedQueue.clear();
            DotConcurrentFactory.this.unsubscribeDelayQueue(this.delayedQueue);
            this.shutdown = true;
            return this.threadPoolExecutor.shutdownNow();

        }

        @Override
        public boolean isAborting() {

            return threadPoolExecutor.isTerminated() ||  threadPoolExecutor.isShutdown() || threadPoolExecutor.isTerminating();
        }

        @Override
        public void waitForAll(final long timeout, final TimeUnit unit) {
            try {
                threadPoolExecutor.awaitTermination(timeout, unit);
            } catch (InterruptedException e) {
                throw new DotRuntimeException(e);
            }
        }

        @Override
        public void waitForAll(){
            while(!threadPoolExecutor.isTerminated()) {
                waitForAll(10, TimeUnit.MINUTES);
            }
        }

        public long getTaskCount() {
            return threadPoolExecutor.getTaskCount();
        }

        @Override
        public String toString() {

            return "DotConcurrentImpl{"   +
                    "threadPoolExecutor=" + threadPoolExecutor +
                    "\nMaximumPoolSize="  + threadPoolExecutor.getMaximumPoolSize() +
                    "\nkeepAliveMillis="  + threadPoolExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS) +
                    "\nqueue="            + threadPoolExecutor.getQueue() +
                    "\nshutdown="         + this.shutdown +
                    '}';
        }
    } // DotConcurrentImpl.

} // E:O:F:DotConcurrentFactory.
