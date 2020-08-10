package com.dotcms.scheduler;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;
import io.vavr.control.Try;

public class TestSyncronizedTask extends DotSyncronizedTask {

    
    public final static int getNumberOfRuns() {
        return Try.of(() -> Integer.parseInt(System.getProperty("SyncronizedTestTask_runs", "0"))).getOrElse(0);
    }
    
    
    
    @Override
    public void execute() {

        try {
        if(System.getProperty("SyncronizedTestTaskRunning")!=null) {
            throw new DotRuntimeException("There is already a SyncronizedTestTask Running");
            
        }
        
        System.setProperty("SyncronizedTestTaskRunning", "true");
        
        
        
        
        int runs = getNumberOfRuns() ;
        Logger.warn(this.getClass(), "SyncronizedTestTask runs:" + runs );
        
        
        System.setProperty("SyncronizedTestTask_runs", String.valueOf(++runs));
        
        }
        finally {
            System.clearProperty("SyncronizedTestTaskRunning");
            
        }
    }

}
