package com.dotcms.concurrent;

import com.dotcms.repackage.org.apache.commons.lang.StringUtils;
import com.dotcms.util.ReflectionUtils;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import static com.dotcms.util.CollectionsUtils.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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


    /**
     * Used to keep the instance of the JWT Service.
     * Should be volatile to avoid thread-caching
     */
    private final Map<String, DotConcurrentImpl> submitterMap =
            new ConcurrentHashMap<>();

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
    public String getObjectName() {

        return MBEAN_OBJECT_NAME;
    }

    private static class SingletonHolder {
        private static final DotConcurrentFactory INSTANCE = new DotConcurrentFactory();
    }
    /**
     * Get the instance.
     * @return JsonWebTokenFactory
     */
    public static DotConcurrentFactory getInstance() {

        return DotConcurrentFactory.SingletonHolder.INSTANCE;
    } // getInstance.

    /**
     * Get's the default submitter
     * @return DotSubmitter
     */
    public DotSubmitter getSubmitter () {

        return this.getSubmitter(StringUtils.EMPTY);
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

            if (null != submitter && submitter.shutdown) { // if it is shutdown, create a new one

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

    private final class DotConcurrentImpl implements DotSubmitter {

        private final ThreadPoolExecutor threadPoolExecutor;
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
        public int getActiveCount() {

            return this.threadPoolExecutor.getActiveCount();
        }

        @Override
        public void shutdown() {

            this.threadPoolExecutor.shutdown();
            this.shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {

            this.shutdown = true;
            return this.threadPoolExecutor.shutdownNow();
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
