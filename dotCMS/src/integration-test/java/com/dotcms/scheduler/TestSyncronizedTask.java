package com.dotcms.scheduler;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

public class TestSyncronizedTask extends DotSyncronizedTask {

    final int increment;
    
    public final static int getTotalIncrement() {
        return Try.of(() -> Integer.parseInt(System.getProperty("SyncronizedTestTask_increment", "0"))).getOrElse(0);
    }
    
    public TestSyncronizedTask(int inc) {
        this.increment=inc;
    }
        
    @Override
    public void execute() {

        try {
        if(System.getProperty("SyncronizedTestTaskRunning")!=null) {
            throw new DotRuntimeException("There is already a SyncronizedTestTask Running");
            
        }
        
        System.setProperty("SyncronizedTestTaskRunning", "true");
        
        
        
        
        int runs = getTotalIncrement() ;
        Logger.warn(this.getClass(), "TestSyncronizedTask runs:" + runs + ", adding:" + increment );
        runs+=increment;
        
        System.setProperty("SyncronizedTestTask_increment", String.valueOf(runs));
        
        }
        finally {
            System.clearProperty("SyncronizedTestTaskRunning");
            
        }
    }

}
