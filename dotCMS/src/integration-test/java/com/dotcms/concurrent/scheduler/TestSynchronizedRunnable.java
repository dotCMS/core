package com.dotcms.concurrent.scheduler;

import com.dotmarketing.exception.DotRuntimeException;
import io.vavr.control.Try;

import java.io.Serializable;


public class TestSynchronizedRunnable implements Runnable, Serializable {

    private final static String SYSTEM_INCREMENT_KEY = "TestSyncronizedTaskKey";
    private final static String TASK_RUNNING_KEY = "TestSyncronizedTaskRunning";

    private final int myIncrement;

    public static int getTotalIncrement() {
        return Try.of(() -> Integer.parseInt(System.getProperty(SYSTEM_INCREMENT_KEY))).getOrElse(0);
    }

    public TestSynchronizedRunnable(int myIncrement) {
        this.myIncrement = myIncrement;
    }

    @Override
    public void run() {
        if (System.getProperty(TASK_RUNNING_KEY) != null) {
            throw new DotRuntimeException("Another task is already running");
        }
        try {
            System.setProperty(TASK_RUNNING_KEY, "true");
            System.out.println("TestSyncronizedTask start:" +getTotalIncrement() + " adding:" + myIncrement );
            int currentNumber = getTotalIncrement() + myIncrement;
            System.setProperty(SYSTEM_INCREMENT_KEY, String.valueOf(currentNumber));
        } finally {
            System.clearProperty(TASK_RUNNING_KEY);
        }
    }

}
