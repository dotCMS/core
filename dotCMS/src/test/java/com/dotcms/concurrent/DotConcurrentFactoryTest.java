package com.dotcms.concurrent;

import com.dotcms.UnitTestBase;
import com.dotcms.concurrent.DotConcurrentFactory.SubmitterConfigBuilder;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DotConcurrentFactoryTest extends UnitTestBase {

    /**
     * Method to test: {@link ConditionalSubmitter#submit(Supplier, Supplier)}
     * Given Scenario: run 100 task but only 6 should be submitted as a scenario 1, the rest should be scenario 2
     * ExpectedResult: 6 ran on scenario 1 and 94 on scenario 2
     *
     */
    @Test
    public void test_Conditional_Submitter() throws  ExecutionException, InterruptedException {

        final String scenario1String = "scenario1";
        final String scenario2String = "scenario2";
        final int slots = 6;
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        final ConditionalSubmitter conditionalExecutor = DotConcurrentFactory.getInstance().createConditionalSubmitter(slots);
        final List<Future<String>> futures = new ArrayList<>();
        int scenarios1Count = 0;
        int scenarios2Count = 0;
        final Supplier<String> supplier1 = ()-> {

            System.out.println("Supplier1, a " + System.currentTimeMillis());
            DateUtil.sleep(DateUtil.EIGHT_SECOND_MILLIS);
            System.out.println("Supplier1, b " + System.currentTimeMillis());
            return scenario1String;
        };
        final Supplier<String> supplier2 = ()-> scenario2String;
        for (int i = 0; i < 20; ++i) {

            futures.add(executorService.submit(()-> conditionalExecutor.submit(supplier1, supplier2)));
        }

        for (final Future<String> future: futures) {

            final String result = future.get();
            if (scenario2String.equals(result)) {
                scenarios2Count++;
            } else {
                scenarios1Count++;
            }
        }

        assertEquals(6, scenarios1Count);
        assertEquals(14, scenarios2Count);

        scenarios2Count = scenarios1Count = 0;
        futures.clear();
        for (int i = 0; i < 20; ++i) {

            futures.add(executorService.submit(()-> conditionalExecutor.submit(supplier1, supplier2)));
        }

        for (final Future<String> future: futures) {

            final String result = future.get();
            if (scenario2String.equals(result)) {
                scenarios2Count++;
            } else {
                scenarios1Count++;
            }
        }

        assertEquals(6, scenarios1Count);
        assertEquals(14, scenarios2Count);

    }

    /**
     * Method to test: {@link DotSubmitter#submit(Runnable)}
     * Given Scenario: Running several task into a single thread executor
     * ExpectedResult: All the threads should be called
     *
     */
    @Test
    public void testDefaultOne_Single_Submitter_Config() throws JSONException, ExecutionException, InterruptedException {

        final String submitterName = "testsinglesubmitter";
        final DotConcurrentFactory dotConcurrentFactory =
                DotConcurrentFactory.getInstance();

        final DotSubmitter submitter =
                dotConcurrentFactory.getSingleSubmitter(submitterName);
        final List<Future> futures = new ArrayList<>();
        System.out.println(submitter);

        IntStream.range(0, 10).forEach(
                n -> {
                    futures.add(submitter.submit(new PrintTask("Thread" + n)));
                }
        );

        //check active thread, if zero then shut down the thread pool
        for (final Future future : futures) {

            future.get();
        }

        System.out.print("Staring a new one submitter");

        final DotSubmitter submitter2 =
                dotConcurrentFactory.getSingleSubmitter(submitterName);

        System.out.println(submitter2);

        assertTrue(submitter == submitter2);

        final List<Future> futures2 = new ArrayList<>();
        IntStream.range(0, 10).forEach(
                n -> {
                    futures2.add(submitter2.submit(new PrintTask("Thread" + n)));
                }
        );

        //check active thread, if zero then shut down the thread pool
        for (final Future future : futures2) {

            future.get();
        }
    }

    @Test
    public void testDefaultOne_Submitter_Config() throws JSONException{

        final String submitterName = "testsubmitter";
        final DotConcurrentFactory dotConcurrentFactory =
                DotConcurrentFactory.getInstance();

        final DotSubmitter submitter =
                dotConcurrentFactory.getSubmitter(submitterName,
                        new SubmitterConfigBuilder().poolSize(2)
                        .maxPoolSize(4).queueCapacity(500).build()
                );

        System.out.println(submitter);

        IntStream.range(0, 40).forEach(
                n -> {

                    if (n % 10 == 0) {

                        System.out.println(submitter);
                    }
                    submitter.execute(new PrintTask("Thread" + n));
                }
        );

        //check active thread, if zero then shut down the thread pool
        for (;;) {
            int count = submitter.getActiveCount();
            System.out.println("Active Threads : " + count);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (count == 0) {
                //submitter.shutdown();
                break;
            }
        }

        System.out.print("Staring a new one submitter");

        final DotSubmitter submitter2 =
                dotConcurrentFactory.getSubmitter(submitterName);

        System.out.println(submitter2);

        assertTrue(submitter == submitter2);

        IntStream.range(0, 20).forEach(
                n -> {
                    submitter2.execute(new PrintTask("Thread" + n));
                }
        );

        //check active thread, if zero then shut down the thread pool
        for (;;) {
            int count = submitter2.getActiveCount();
            System.out.println("Active Threads : " + count);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (count == 0) {
                submitter2.shutdown();
                break;
            }
        }

    }

    @Test
    public void testDefaultOne_Submitter_Config_shutdown_keep_config() throws JSONException{

        final String submitterName = "testsubmitter";
        final DotConcurrentFactory dotConcurrentFactory =
                DotConcurrentFactory.getInstance();

        final DotSubmitter submitter =
                dotConcurrentFactory.getSubmitter(submitterName,
                        new SubmitterConfigBuilder().poolSize(2)
                                .maxPoolSize(4).queueCapacity(500).build()
                );

        System.out.println(submitter);

        IntStream.range(0, 40).forEach(
                n -> {

                    if (n % 10 == 0) {

                        System.out.println(submitter);
                    }
                    submitter.execute(new PrintTask("Thread" + n));
                }
        );

        assertEquals(2, submitter.getPoolSize());
        assertEquals(4, submitter.getMaxPoolSize());

        //check active thread, if zero then shut down the thread pool
        for (;;) {
            int count = submitter.getActiveCount();
            System.out.println("Active Threads : " + count);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (count == 0) {
                //submitter.shutdown();
                break;
            }
        }

        submitter.shutdown();

        System.out.print("Staring a new one submitter");

        final DotSubmitter submitter2 =
                dotConcurrentFactory.getSubmitter(submitterName);

        System.out.println(submitter2);

        assertFalse(submitter == submitter2);

        IntStream.range(0, 20).forEach(
                n -> {
                    submitter2.execute(new PrintTask("Thread" + n));
                }
        );

        assertEquals(2, submitter2.getPoolSize());
        assertEquals(4, submitter2.getMaxPoolSize());

        //check active thread, if zero then shut down the thread pool
        for (;;) {
            int count = submitter2.getActiveCount();
            System.out.println("Active Threads : " + count);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (count == 0) {
                submitter2.shutdown();
                break;
            }
        }

    }

    @Test
    public void testDefaultOne() throws JSONException{

        final DotConcurrentFactory dotConcurrentFactory =
                DotConcurrentFactory.getInstance();

        final DotSubmitter submitter =
                dotConcurrentFactory.getSubmitter();

        System.out.println(submitter);

        IntStream.range(0, 40).forEach(
                n -> {

                    if (n % 10 == 0) {

                        System.out.println(submitter);
                    }
                    submitter.execute(new PrintTask("Thread" + n));
                }
        );

        //check active thread, if zero then shut down the thread pool
        for (;;) {
            int count = submitter.getActiveCount();
            System.out.println("Active Threads : " + count);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (count == 0) {
                //submitter.shutdown();
                break;
            }
        }

        System.out.print("Staring a new one submitter");

        final DotSubmitter submitter2 =
                dotConcurrentFactory.getSubmitter();

        System.out.println(submitter2);

        assertTrue(submitter == submitter2);

        IntStream.range(0, 20).forEach(
                n -> {
                    submitter2.execute(new PrintTask("Thread" + n));
                }
        );

        //check active thread, if zero then shut down the thread pool
        for (;;) {
            int count = submitter2.getActiveCount();
            System.out.println("Active Threads : " + count);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (count == 0) {
                submitter2.shutdown();
                break;
            }
        }

    }


    class PrintTask implements Runnable {

        String name;

        public PrintTask(String name){
            this.name = name;
        }

        @Override
        public void run() {

            System.out.println(name + " is running");

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println(name + " is running");
        }

    }
    
    
    
    
    /**
     * This tests that the delayed queue will run jobs in the future, efficiently
     * 
     * @throws Exception
     */
    @Test
    public void test_delayed_queue() throws Exception{
        
        // will kill it from the cache in some time.
        final DotSubmitter submitter = DotConcurrentFactory.getInstance().getSubmitter();
        final AtomicInteger aInt=new AtomicInteger(0);
        
        // add 50 to the atomicInteger, 3 seconds in the future
        int runs = 50;
        for(int i=0;i<runs;i++) {
            submitter.delay(()-> aInt.addAndGet(1), 1, TimeUnit.SECONDS);
        }
        
        // None of the jobs have been run
        assert(aInt.get()==0);
        
        // rest.  I must rest
        Thread.sleep(2000);
        
        // all of the jobs have been run
        assert(aInt.get()==50);
    }

    /**
     * Test the CompleatableFuture any; in this case the last one should be the fastest
     *
     * @throws Exception
     */
    @Test
    public void test_toCompletableAnyFuture_success() throws Exception{
        // will kill it from the cache in some time.
        final DotSubmitter submitter = DotConcurrentFactory.getInstance().getSubmitter();
        final List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 1; i <= 10; ++i) {
            final int finalIndex = i;
            futures.add(submitter.submit(() ->{

                DateUtil.sleep(Math.abs(finalIndex - 10) * 1000);
                return finalIndex;
            }));
        }

        final CompletableFuture<Integer> completableFuture = DotConcurrentFactory.getInstance().toCompletableAnyFuture(futures);
        final Integer result = completableFuture.get();
        assertEquals("The last one should be the fastest", 10, result.intValue());
    }

}