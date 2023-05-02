package com.dotcms.util;


import com.dotmarketing.util.ThreadUtils;
import graphql.Assert;
import org.junit.Test;

public class ThreadUtilsTest {

    /**
     * Given scenario: We pass current thread name
     * Expected result: we expect the current thread to be returned
     */
    @Test
    public void Test_Get_Thread_Compare_With_Current_Thread() {
        final Thread currentThread = Thread.currentThread();
        final Thread byNameThread = ThreadUtils.getThread(currentThread.getName());
        assert currentThread == byNameThread;
    }

    /**
     * Given scenario: We pass a non-existing thread name
     * Expected result: we expect null to be returned
     */
    @Test
    public void Test_Get_Thread_Non_Existing_Thread_Name() {
        final Thread byNameThread = ThreadUtils.getThread("NonExistingThreadName");
        Assert.assertNull(byNameThread);
    }

    /**
     * Given scenario: We pass null as thread name
     * Expected result: We expect null to be returned
     */
    @Test
    public void Test_Get_Thread_When_Null_Is_Passed() {
        final Thread byNameThread = ThreadUtils.getThread(null);
        Assert.assertNull(byNameThread);
    }

    /**
     * Given scenario: Testing that sleep works
     * Expected result: We expect the thread to sleep for at least 100ms
     */
    @Test
    public void Test_Sleep() {
        final long t1 = System.currentTimeMillis();
        ThreadUtils.sleep(100L);
        final long t2 = System.currentTimeMillis();
        assert t2 - t1 >= 100L;
    }

}
