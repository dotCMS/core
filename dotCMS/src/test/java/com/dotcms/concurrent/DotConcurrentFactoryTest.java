package com.dotcms.concurrent;

import com.dotcms.UnitTestBase;
import com.dotcms.concurrent.DotConcurrentFactory.SubmitterConfigBuilder;
import com.dotcms.content.elasticsearch.business.ElasticReadOnlyCommand;
import com.dotmarketing.util.json.JSONException;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DotConcurrentFactoryTest extends UnitTestBase {

    /**
     * Method to test: {@link DotSubmitter#submit(Runnable)}
     * Given Scenario: Just running task to see if works
     * ExpectedResult: The executeCheck should be called
     *
     */
    @Test
    public void testSubmit_Single_Submitter_Config() throws JSONException, ExecutionException, InterruptedException {

        final ElasticReadOnlyCommand esReadOnlyMonitor = mock(ElasticReadOnlyCommand.class);
        final DotConcurrentFactory dotConcurrentFactory =
                DotConcurrentFactory.getInstance();
        final DotSubmitter submitter =
                dotConcurrentFactory.getSingleSubmitter();
        submitter.submit(()-> esReadOnlyMonitor.executeCheck());

        verify(esReadOnlyMonitor).executeCheck();
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

}
