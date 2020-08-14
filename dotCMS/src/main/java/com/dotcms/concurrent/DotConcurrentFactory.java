package com.dotcms.concurrent;

import com.dotcms.concurrent.lock.DotKeyLockManagerBuilder;
import com.dotcms.concurrent.lock.IdentifierStripedLock;
import com.dotcms.concurrent.scheduler.DotScheduler;
import com.dotcms.concurrent.scheduler.DotSchedulerImpl;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.init.DotInitScheduler;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dotcms.util.CollectionsUtils.map;

/**
 * Factory for concurrent {@link Executor} & {@link DotSubmitter}
 * @author jsanca
 */
public class DotConcurrentFactory implements DotConcurrentFactoryMBean, Serializable {

    private static final int POOL_SIZE_VAL = 10;
    private static final int MAXPOOL_SIZE_VAL = 50;
    private static final int QUEUE_CAPACITY_VAL = 100;


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

    public static final String BULK_ACTIONS_THREAD_POOL = "bulkActionsPool";

    public static final String LOCK_MANAGER = "IdentifierStripedLock";

    /**
     * Used to keep the instance of the submitter
     * Should be volatile to avoid thread-caching
     */
    private final Map<String, DotConcurrentImpl> submitterMap =
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

    private final ThreadFactory defaultThreadFactory =
            this.getDefaultThreadFactory();


    private final IdentifierStripedLock identifierStripedLock =
           new IdentifierStripedLock(DotKeyLockManagerBuilder.newLockManager(LOCK_MANAGER));

    private final DotScheduler scheduler = new DotSchedulerImpl();

    private final ThreadFactory getDefaultThreadFactory () {

        ThreadFactory threadFactory = null;
        final String className =
                Config.getStringProperty
                        (DOTCMS_CONCURRENT_THREADFACTORYCLASS, null);

        if (UtilMethods.isSet(className)) {

            threadFactory =
                    (ThreadFactory) ReflectionUtils.newInstance
                            (className);
        }

        if (null == threadFactory) {

            threadFactory = Executors.defaultThreadFactory();
        }

        return threadFactory;
    }


    private final ThreadFactory getDefaultThreadFactory (final String executorName) {

        ThreadFactory threadFactory = null;
        final String className =
                Config.getStringProperty
                        (executorName + DOTCMS_CONCURRENT_THREADFACTORYCLASS, null);

        if (UtilMethods.isSet(className)) {

            threadFactory =
                    (ThreadFactory) ReflectionUtils.newInstance
                            (className);

            if (null != threadFactory && Logger.isDebugEnabled(this.getClass())) {

                Logger.debug(this.getClass(), "Using the thread factory implementation: " + executorName);
            }
        }

        if (null == threadFactory) {

            threadFactory = this.defaultThreadFactory;
        }


        return threadFactory;
    }

    private DotConcurrentFactory () {
        // singleton
        this.delayQueueConsumer = new DelayQueueConsumer();
        DotInitScheduler.getScheduledThreadPoolExecutor().scheduleWithFixedDelay(this.delayQueueConsumer, 0,
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

        final DotConcurrentImpl dotConcurrent =
                this.submitterMap.get(name);

        return (null != dotConcurrent)?
                map(
                        "name",        name,
                        "threadPool",  dotConcurrent.getThreadPoolExecutor().toString(),
                        "maxPoolSize", dotConcurrent.getThreadPoolExecutor().getMaximumPoolSize(),
                        "keepAlive",   dotConcurrent.getThreadPoolExecutor().getKeepAliveTime(TimeUnit.MILLISECONDS),
                        "queue",       dotConcurrent.getThreadPoolExecutor().getQueue().toString(),
                        "isShutdown",  dotConcurrent.shutdown
                ):
                map(
                        "name",        name,
                        "threadPool",  "noInfo",
                        "maxPoolSize", -1,
                        "keepAlive",   -1,
                        "queue",       "noInfo",
                        "isShutdown",  false
                );
    }

    @Override
    public Boolean shutdown(final String name){
        final DotConcurrentImpl dotConcurrent =
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
                    this.submitterMap.get(name);

            if (null != submitter && (submitter.shutdown || submitter.threadPoolExecutor
                    .isTerminated())) { // if it is shutdown, create a new one

                synchronized (DotConcurrentFactory.class) {

                    submitter =
                            this.createDotConcurrent(name);
                }
            }
        }

        return submitter;
    } // getBean.

    private DotConcurrentImpl createDotConcurrent (final String name) {

        DotConcurrentImpl submitter =
                new DotConcurrentImpl(
                        this.getDefaultThreadFactory(name),
                        this.rejectedExecutionHandler,
                        Config.getBooleanProperty(name + DOTCMS_CONCURRENT_ALLOWCORETHREADTIMEOUT, this.defaultAllowCoreThreadTimeOut),
                        Config.getIntProperty (name  + DOTCMS_CONCURRENT_POOLSIZE,                 this.defaultPoolSize),
                        Config.getIntProperty (name  + DOTCMS_CONCURRENT_MAXPOOLSIZE,              this.defaultMaxPoolSize),
                        Config.getLongProperty(name  + DOTCMS_CONCURRENT_KEEPALIVEMILLIS,          this.defaultKeepAliveMillis),
                        Config.getIntProperty (name  + DOTCMS_CONCURRENT_QUEUECAPACITY,            this.defaultQueueCapacity)
                );

        this.submitterMap.put(name, submitter);

        return submitter;
    }

    /**
     * returns an singleton instance of the identifier strip locked manager;
     * @return
     */
    public IdentifierStripedLock getIdentifierStripedLock(){
       return this.identifierStripedLock;
    }

    /**
     * Returns the Scheduler
     * @return DotScheduler
     */
    public DotScheduler getScheduler () {

        return scheduler;
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
                final int queueCapacity
                ) {

            final BlockingQueue<Runnable> queue = this.createQueue(queueCapacity);
            this.threadPoolExecutor  = new ThreadPoolExecutor(
                    poolSize, maxPoolSize, keepAliveMillis, TimeUnit.MILLISECONDS,
                    queue, threadFactory, rejectedExecutionHandler);

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
