package com.dotcms.util;

import com.dotcms.UnitTestBase;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.junit.Test;

import java.util.Optional;
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

        assertTrue( FunctionUtils.ifOrElse(true, ()-> atomicBoolean.set(true), () -> atomicBoolean.set(false)));
        assertTrue( atomicBoolean.get() );

        assertFalse( FunctionUtils.ifOrElse(()-> false, ()-> atomicBoolean.set(true), () -> atomicBoolean.set(false)));
        assertFalse( atomicBoolean.get() );
    }

    @Test
    public void ifElseTestWithEmptyOptional()  {

        final Optional<Contentlet> optionalContentlet = Optional.empty();
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        assertFalse( FunctionUtils.ifOrElse(optionalContentlet, v-> atomicBoolean.set(true), () -> atomicBoolean.set(false)));
        assertFalse( atomicBoolean.get() );
    }

    @Test
    public void ifElseTestWithNullableOptional()  {

        final Optional<Contentlet> optionalContentlet = Optional.ofNullable(null);
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        assertFalse( FunctionUtils.ifOrElse(optionalContentlet, v-> atomicBoolean.set(true), () -> atomicBoolean.set(false)));
        assertFalse( atomicBoolean.get() );
    }

    @Test
    public void ifElseTestWithValidOptional()  {

        final Optional<Contentlet> optionalContentlet = Optional.ofNullable(new Contentlet());
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        assertTrue( FunctionUtils.ifOrElse(optionalContentlet, v-> atomicBoolean.set(true), () -> atomicBoolean.set(false)));
        assertTrue( atomicBoolean.get() );
    }

}
