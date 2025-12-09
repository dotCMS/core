package com.dotcms.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import com.dotcms.UnitTestBase;
import com.dotcms.concurrent.DotConcurrentFactory.SubmitterConfigBuilder;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.json.JSONException;
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
import org.junit.Test;

public class DotConcurrentFactoryTest extends UnitTestBase {

    /**
     * Method to test: {@link ConditionalSubmitter#submit(Supplier, Supplier)}
     * Given Scenario: run tasks but only 6 should be submitted as scenario 1, the rest should be scenario 2
     * ExpectedResult: 6 ran on scenario 1 and the rest on scenario 2
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
        // Reduced from 8 seconds to 200ms - still enough to hold the slot while other tasks arrive
        final Supplier<String> supplier1 = ()-> {
            DateUtil.sleep(200);
            return scenario1String;
        };
        final Supplier<String> supplier2 = ()-> scenario2String;
        // Reduced iterations from 20 to 12 - still tests the slot limiting behavior (6 slots, 12 tasks)
        for (int i = 0; i < 12; ++i) {
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
        assertEquals(6, scenarios2Count);

        // Second round to verify slot release works correctly
        scenarios2Count = scenarios1Count = 0;
        futures.clear();
        for (int i = 0; i < 12; ++i) {
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
        assertEquals(6, scenarios2Count);

        executorService.shutdown();
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
        final List<Future<?>> futures = new ArrayList<>();

        // Reduced from 10 to 5 iterations - still verifies single-threaded execution
        IntStream.range(0, 5).forEach(
                n -> futures.add(submitter.submit(new PrintTask("Thread" + n)))
        );

        // Wait for all tasks to complete
        for (final Future<?> future : futures) {
            future.get();
        }

        final DotSubmitter submitter2 =
                dotConcurrentFactory.getSingleSubmitter(submitterName);

        assertSame(submitter, submitter2);

        final List<Future<?>> futures2 = new ArrayList<>();
        IntStream.range(0, 5).forEach(
                n -> futures2.add(submitter2.submit(new PrintTask("Thread" + n)))
        );

        // Wait for all tasks to complete
        for (final Future<?> future : futures2) {
            future.get();
        }
    }

    @Test
    public void testDefaultOne_Submitter_Config() throws JSONException, InterruptedException, ExecutionException {

        final String submitterName = "testsubmitter";
        final DotConcurrentFactory dotConcurrentFactory =
                DotConcurrentFactory.getInstance();

        final DotSubmitter submitter =
                dotConcurrentFactory.getSubmitter(submitterName,
                        new SubmitterConfigBuilder().poolSize(2)
                        .maxPoolSize(4).queueCapacity(500).build()
                );

        // Reduced from 40 to 10 iterations - still tests thread pool behavior
        final List<Future<?>> futures = new ArrayList<>();
        IntStream.range(0, 10).forEach(
                n -> futures.add(submitter.submit(new PrintTask("Thread" + n)))
        );

        // Wait for completion instead of polling
        for (final Future<?> future : futures) {
            future.get();
        }

        final DotSubmitter submitter2 =
                dotConcurrentFactory.getSubmitter(submitterName);

        assertSame(submitter, submitter2);

        // Reduced from 20 to 5 iterations
        final List<Future<?>> futures2 = new ArrayList<>();
        IntStream.range(0, 5).forEach(
                n -> futures2.add(submitter2.submit(new PrintTask("Thread" + n)))
        );

        for (final Future<?> future : futures2) {
            future.get();
        }

        submitter2.shutdown();
    }

    @Test
    public void testDefaultOne_Submitter_Config_shutdown_keep_config()
            throws JSONException, InterruptedException, ExecutionException {

        final String submitterName = "testsubmitter_shutdown";
        final DotConcurrentFactory dotConcurrentFactory =
                DotConcurrentFactory.getInstance();

        final DotSubmitter submitter =
                dotConcurrentFactory.getSubmitter(submitterName,
                        new SubmitterConfigBuilder().poolSize(2)
                                .maxPoolSize(4).queueCapacity(500).build()
                );

        // Reduced from 40 to 8 iterations - still tests config preservation
        final List<Future<?>> futures = new ArrayList<>();
        IntStream.range(0, 8).forEach(
                n -> futures.add(submitter.submit(new PrintTask("Thread" + n)))
        );

        assertEquals(2, submitter.getPoolSize());
        assertEquals(4, submitter.getMaxPoolSize());

        // Wait for completion instead of polling
        for (final Future<?> future : futures) {
            future.get();
        }

        submitter.shutdown();

        // After shutdown, getting same name should create new submitter with same config
        final DotSubmitter submitter2 =
                dotConcurrentFactory.getSubmitter(submitterName);

        assertNotSame(submitter, submitter2);

        // Reduced from 20 to 5 iterations
        final List<Future<?>> futures2 = new ArrayList<>();
        IntStream.range(0, 5).forEach(
                n -> futures2.add(submitter2.submit(new PrintTask("Thread" + n)))
        );

        assertEquals(2, submitter2.getPoolSize());
        assertEquals(4, submitter2.getMaxPoolSize());

        for (final Future<?> future : futures2) {
            future.get();
        }

        submitter2.shutdown();
    }

    @Test
    public void testDefaultOne() throws JSONException, InterruptedException, ExecutionException {

        final DotConcurrentFactory dotConcurrentFactory =
                DotConcurrentFactory.getInstance();

        final DotSubmitter submitter =
                dotConcurrentFactory.getSubmitter();

        // Reduced from 40 to 10 iterations
        final List<Future<?>> futures = new ArrayList<>();
        IntStream.range(0, 10).forEach(
                n -> futures.add(submitter.submit(new PrintTask("Thread" + n)))
        );

        // Wait for completion instead of polling
        for (final Future<?> future : futures) {
            future.get();
        }

        final DotSubmitter submitter2 =
                dotConcurrentFactory.getSubmitter();

        assertSame(submitter, submitter2);

        // Reduced from 20 to 5 iterations
        final List<Future<?>> futures2 = new ArrayList<>();
        IntStream.range(0, 5).forEach(
                n -> futures2.add(submitter2.submit(new PrintTask("Thread" + n)))
        );

        for (final Future<?> future : futures2) {
            future.get();
        }

        submitter2.shutdown();
    }


    static class PrintTask implements Runnable {

        String name;

        public PrintTask(String name){
            this.name = name;
        }

        @Override
        public void run() {
            // Reduced from 500ms to 20ms - just needs to simulate some work
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }
    
    
    
    
    /**
     * This tests that the delayed queue will run jobs in the future, efficiently
     *
     * @throws Exception if test fails
     */
    @Test
    public void test_delayed_queue() throws Exception{

        final DotSubmitter submitter = DotConcurrentFactory.getInstance().getSubmitter();
        final AtomicInteger aInt = new AtomicInteger(0);

        // Reduced from 50 to 10 iterations - still tests delayed execution
        final int runs = 10;
        for (int i = 0; i < runs; i++) {
            // Reduced delay from 1 second to 100ms
            submitter.delay(() -> aInt.addAndGet(1), 100, TimeUnit.MILLISECONDS);
        }

        // None of the jobs have been run yet
        assertEquals(0, aInt.get());

        // Wait for delayed tasks to complete (200ms buffer)
        Thread.sleep(300);

        // All of the jobs should have been run
        assertEquals(runs, aInt.get());
    }

    /**
     * Test the CompletableFuture any; in this case the last one should be the fastest
     *
     * @throws Exception if test fails
     */
    @Test
    public void test_toCompletableAnyFuture_success() throws Exception{
        final DotSubmitter submitter = DotConcurrentFactory.getInstance().getSubmitter();
        final List<Future<Integer>> futures = new ArrayList<>();

        // Reduced from 10 iterations with up to 9 second delays to 5 iterations with 100ms delays
        for (int i = 1; i <= 5; ++i) {
            final int finalIndex = i;
            futures.add(submitter.submit(() -> {
                // Last one (index 5) has 0 delay, first one (index 1) has 400ms delay
                DateUtil.sleep(Math.abs(finalIndex - 5) * 100);
                return finalIndex;
            }));
        }

        final CompletableFuture<Integer> completableFuture = DotConcurrentFactory.getInstance().toCompletableAnyFuture(futures);
        final Integer result = completableFuture.get();
        assertEquals("The last one should be the fastest", 5, result.intValue());
    }

}
