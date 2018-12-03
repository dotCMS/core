package com.dotcms.concurrent.lock;

import com.dotmarketing.util.DateUtil;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class DotKeyLockManagerTest {

    private static final int SLEEP = 25;

    private static final int INPUT_SIZE = 60;

    private static final int THREAD_POOL_SIZE = 100;

    private final ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final ReentrantLock globalLock = new ReentrantLock();
    private final CountDownLatch countDownLatch = new CountDownLatch(INPUT_SIZE);

    private static Map<String,MutableInt> seedWork(final int num){
        final ImmutableMap.Builder<String, MutableInt> builder = new ImmutableMap.Builder<>();
        for(int i=0; i<= num; i++){
            builder.put(""+ i, new MutableInt(0) );
        }
        return builder.build();
    }

    class FineGrainedLockTask implements Callable<String> {

        private final String key;
        private final MutableInt criticalResource;
        private final DotKeyLockManager <String, String> manager;
        private final CountDownLatch countDownLatch;
        private final boolean useTry;

        FineGrainedLockTask(final String key, final MutableInt criticalResource, final DotKeyLockManager <String, String> manager, final CountDownLatch countDownLatch, boolean useTry) {
            this.key = key;
            this.criticalResource = criticalResource;
            this.manager = manager;
            this.countDownLatch = countDownLatch;
            this.useTry = useTry;
        }

        @Override
        public String call() throws Exception {
            try {
                if (useTry) {
                    return manager.tryLock(key, () -> {
                        DateUtil.sleep(SLEEP);
                        criticalResource.increment();
                        return key + " , " + criticalResource;
                    });
                } else {
                    return manager.lock(key, () -> {
                        DateUtil.sleep(SLEEP);
                        criticalResource.increment();
                        return key + " , " + criticalResource;
                    });
                }
            } catch (Throwable e) {
                throw new Exception(e);
            } finally {
                countDownLatch.countDown();
            }
        }
    }

    class SynchronizedTask implements Callable<String> {

        private final String key;
        private final MutableInt criticalResource;
        private final CountDownLatch countDownLatch;

        SynchronizedTask(final String key, final MutableInt criticalResource, final CountDownLatch countDownLatch) {
            this.key = key;
            this.criticalResource = criticalResource;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public String call() throws Exception {
            synchronized (DotKeyLockManagerTest.this){
            try {
                DateUtil.sleep(SLEEP);
                criticalResource.increment();
                return  key + " , " + criticalResource;
            } finally {
                countDownLatch.countDown();
            }}
        }
    }

    class RenetrantLockTask implements Callable<String> {

        private final String key;
        private final MutableInt criticalResource;

        RenetrantLockTask(final String key, final MutableInt criticalResource) {
            this.key = key;
            this.criticalResource = criticalResource;
        }

        @Override
        public String call() throws Exception {
                globalLock.lock();
                try {
                    DateUtil.sleep(SLEEP);
                    criticalResource.increment();
                    return  key + " , " + criticalResource;
                } finally {
                    globalLock.unlock();
                    countDownLatch.countDown();
                }

        }
    }

    @Test
    public void test_speed_StripedLockManager() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(INPUT_SIZE);
        final DotKeyLockManager<String, String> manager = DotKeyLockManagerBuilder.newStripedLockManager();
        final Map<String,MutableInt> seed = seedWork(INPUT_SIZE);
        final boolean useTry = true;

        final List<FineGrainedLockTask> taskList = new ArrayList<>();

        seed.forEach((s, mutableInt) -> {
            taskList.add(new FineGrainedLockTask(s, mutableInt, manager, countDownLatch, useTry));
        });

        seed.forEach((s, mutableInt) -> {
            taskList.add(new FineGrainedLockTask(s, mutableInt, manager, countDownLatch, useTry));
        });

        seed.forEach((s, mutableInt) -> {
            taskList.add(new FineGrainedLockTask(s, mutableInt, manager, countDownLatch, useTry));
        });

        final long t1 = System.currentTimeMillis();

        final List<Future<String>> futures = pool.invokeAll(taskList);
        countDownLatch.await();

        final long t2 = System.currentTimeMillis();

        final long t = t2 - t1;

        System.out.println(
                "Duration using fine-grained locking: " + TimeUnit.MILLISECONDS.toSeconds(t) + " seconds. ");

        for (final Future<String> future : futures) {
            future.get();
        };

        seed.forEach((s, mutableInt) -> {
            System.out.println(s + " : " + mutableInt.toString());
        });

    }

    @Test
    public void test_speed_Synchronized_Lock() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(INPUT_SIZE);

        final Map<String,MutableInt> seed = seedWork(INPUT_SIZE);

        final List<SynchronizedTask> taskList = new ArrayList<>();

        seed.forEach((s, mutableInt) -> {
            taskList.add(new SynchronizedTask(s, mutableInt, countDownLatch));
        });

        seed.forEach((s, mutableInt) -> {
            taskList.add(new SynchronizedTask(s, mutableInt, countDownLatch));
        });

        seed.forEach((s, mutableInt) -> {
            taskList.add(new SynchronizedTask(s, mutableInt, countDownLatch));
        });

        final long t1 = System.currentTimeMillis();

        final List<Future<String>> futures = pool.invokeAll(taskList);
        countDownLatch.await();

        final long t2 = System.currentTimeMillis();

        final long t = t2 - t1;

        System.out.println(
                "Duration using synchronized single monitor: " + TimeUnit.MILLISECONDS.toSeconds(t)  + " seconds. " ) ;

        for (final Future<String> future : futures) {
            future.get();
        };

        seed.forEach((s, mutableInt) -> {
            System.out.println(s + " : " + mutableInt.toString());
        });
    }


    @Test
    public void test_Sped_Global_Reentrant_Lock() throws Exception {

        final Map<String,MutableInt> seed = seedWork(INPUT_SIZE);

        final List<RenetrantLockTask> taskList = new ArrayList<>();

        seed.forEach((s, mutableInt) -> {
            taskList.add(new RenetrantLockTask(s, mutableInt));
        });

        seed.forEach((s, mutableInt) -> {
            taskList.add(new RenetrantLockTask(s, mutableInt));
        });

        seed.forEach((s, mutableInt) -> {
            taskList.add(new RenetrantLockTask(s, mutableInt));
        });

        final long t1 = System.currentTimeMillis();

        final List<Future<String>> futures = pool.invokeAll(taskList);
        countDownLatch.await();

        final long t2 = System.currentTimeMillis();

        final long t = t2 - t1;

        System.out.println(
                "Duration using one single re-entrant lock: " + TimeUnit.MILLISECONDS.toSeconds(t) + " seconds. ");

        for (final Future<String> future : futures) {
            future.get();
        };

        seed.forEach((s, mutableInt) -> {
            System.out.println(s + " : " + mutableInt.toString());
        });
    }

}
