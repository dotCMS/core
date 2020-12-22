package com.dotcms.concurrent;

import static org.junit.Assert.*;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import com.dotmarketing.util.Logger;

public class DebouncerTest {

    
    
    
    
    @Test
    public void test_that_debouncer_prevents_multiple_runs() throws InterruptedException {
        Debouncer debouncer = new Debouncer(0L);
        
        // debounce for 10 seconds
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        
        //sleep 7 seconds
        Thread.sleep(7*1000);


        assertTrue("assert that we have only run once", debouncer.runCount==1);

    }

    
    @Test
    public void test_that_debouncer_reschedules_the_runnable() throws InterruptedException {
        Debouncer debouncer = new Debouncer(0L);
        
        
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 1, TimeUnit.SECONDS);
        Thread.sleep(100);
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        
        Thread.sleep(3000);
        assertTrue("assert that we have not run yet", debouncer.runCount==0);
        
        Thread.sleep(4000);
        assertTrue("assert that we have only run once", debouncer.runCount==1);
        
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 1, TimeUnit.SECONDS);
        Thread.sleep(100);
       
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 1, TimeUnit.SECONDS);
        Thread.sleep(2000);



        assertTrue("assert that we have run twice", debouncer.runCount==2);

    }

    
    
    
    
    
    
}
