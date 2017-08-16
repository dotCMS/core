package com.dotcms.util;

import com.dotcms.UnitTestBase;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FunctionUtilsTest extends UnitTestBase {

    @Test
    public void ifTrueTest()  {

        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        assertTrue( FunctionUtils.ifTrue(true, ()-> atomicBoolean.set(true)));
        assertTrue( atomicBoolean.get() );

        assertTrue( FunctionUtils.ifTrue(()-> true, ()-> atomicBoolean.set(true)));
        assertTrue( atomicBoolean.get() );
    }

    @Test
    public void ifElseTest()  {

        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        assertTrue( FunctionUtils.ifElse(true, ()-> atomicBoolean.set(true), () -> atomicBoolean.set(false)));
        assertTrue( atomicBoolean.get() );

        assertFalse( FunctionUtils.ifElse(()-> false, ()-> atomicBoolean.set(true), () -> atomicBoolean.set(false)));
        assertFalse( atomicBoolean.get() );
    }


}
