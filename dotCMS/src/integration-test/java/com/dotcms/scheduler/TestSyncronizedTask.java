package com.dotcms.scheduler;

import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;

public class TestSyncronizedTask extends DotSyncronizedTask{

    private final static String SYSTEM_INCREMENT_KEY = "TestSyncronizedTaskKey";
    private final static String TASK_RUNNING_KEY = "TestSyncronizedTaskRunning";
    public static int getTotalIncrement() {
        return Try.of(() -> Integer.parseInt((String) System.getProperty(SYSTEM_INCREMENT_KEY))).getOrElse(0);

    }
    
    
    
    
    final int myIncrement;
    
    public TestSyncronizedTask(int inc) {
        this.myIncrement=inc;
    }
    
    
    @Override
    public void execute() {
        if(System.getProperty(TASK_RUNNING_KEY)!=null) {
            throw new DotRuntimeException("Another task is already running");
        }
        try {
            System.setProperty(TASK_RUNNING_KEY, "true");

            System.err.println("TestSyncronizedTask start:" +getTotalIncrement() + " adding:" + myIncrement );
            
            
            int currentNumber = getTotalIncrement() + myIncrement;
            System.setProperty(SYSTEM_INCREMENT_KEY, String.valueOf(currentNumber));
        
        
        }
        finally {
            System.clearProperty(TASK_RUNNING_KEY);
            
        }
        
        
        
    }
    
    
    
    
    

}
