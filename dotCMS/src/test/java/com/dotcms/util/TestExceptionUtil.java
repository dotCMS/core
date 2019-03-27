package com.dotcms.util;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TestExceptionUtil {

    @Test
    public void TestExceptionIsCausedBy () {
        DotSecurityException sec = new DotSecurityException("Root cause");

        Assert.assertTrue(ExceptionUtil.causedBy(sec, DotSecurityException.class));

        Exception wrapException = new Exception(sec);

        Assert.assertTrue(ExceptionUtil.causedBy(wrapException, DotSecurityException.class));
    }

    @Test
    public void TestExceptionIsNotCausedBy () {
        DotSecurityException sec = new DotSecurityException("Root cause");

        Assert.assertFalse(ExceptionUtil.causedBy(sec, DotDataException.class));

        Exception wrapException = new Exception(sec);

        Assert.assertFalse(ExceptionUtil.causedBy(wrapException, DotDataException.class));
    }

    @Test
    public void TestExceptionIsCausedByMany () {

        try {
            throwTimeoutException();
        } catch (IOException e) {

            Assert.assertTrue(ExceptionUtil.causedBy(e, TimeoutException.class, ExecutionException.class));
        }

        try {
            throwExecutionException();
        } catch (IOException e) {

            Assert.assertTrue(ExceptionUtil.causedBy(e,  TimeoutException.class, ExecutionException.class));
            Assert.assertTrue(ExceptionUtil.causedBy(e,  Exception.class));
            Assert.assertFalse(ExceptionUtil.causedBy(e, IllegalArgumentException.class, NumberFormatException.class));
        }
    }


    private void throwTimeoutException () throws IOException {

        try {

            throw new TimeoutException("Error");
        } catch (TimeoutException e) {

            throw new IOException(e.getMessage(), e);
        }
    }

    private void throwExecutionException () throws IOException {

        try {

            throw new ExecutionException(new Exception("error"));
        } catch (ExecutionException e) {

            throw new IOException(e.getMessage(), e);
        }
    }
}
