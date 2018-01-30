package com.dotcms.util;

import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.Assert;
import org.junit.Test;

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
}
