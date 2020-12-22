package com.dotcms.concurrent;

import static org.junit.Assert.*;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import com.dotmarketing.util.Logger;

public class DebouncerTest {

    
    
    
    
    @Test
    public void test() throws InterruptedException {
        Debouncer debouncer = new Debouncer(0L);
        
        // debounce for 10 seconds
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        debouncer.debounce("testingKey", ()->{Logger.info("DebouncerTest","running debouncer test" );}, 5, TimeUnit.SECONDS);
        
        Thread.sleep(7*1000);


        assertTrue("assert that we have only run once", debouncer.runCount==1);
        
        
        
        
        
    }

}
