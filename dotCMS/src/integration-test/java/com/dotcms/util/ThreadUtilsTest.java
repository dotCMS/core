package com.dotcms.util;


import com.dotmarketing.util.ThreadUtils;
import graphql.Assert;
import org.junit.Test;

public class ThreadUtilsTest {

    @Test
    public void Test_Get_Thread_Compare_With_Current_Thread() {
        final Thread currentThread = Thread.currentThread();
        final Thread byNameThread = ThreadUtils.getThread(currentThread.getName());
        assert currentThread == byNameThread;
    }

    @Test
    public void Test_Get_Thread_Non_Existing_Thread_Name() {
        final Thread byNameThread = ThreadUtils.getThread("NonExistingThreadName");
        Assert.assertNull(byNameThread);
    }

    @Test
    public void Test_Get_Thread_When_Null_Is_Passed() {
        final Thread byNameThread = ThreadUtils.getThread(null);
        Assert.assertNull(byNameThread);
    }

    @Test
    public void Test_Sleep() {
        final long t1 = System.currentTimeMillis();
        ThreadUtils.sleep(100L);
        final long t2 = System.currentTimeMillis();
        assert t2 - t1 >= 100L;
    }

}
