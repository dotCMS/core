package com.dotcms.util;


import com.dotcms.UnitTestBase;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class OptionalBooleanTest  extends UnitTestBase {

    @Test
    public void testNull(){

        final OptionalBoolean optionalBoolean = OptionalBoolean.of(null);
        final AtomicBoolean changeOnFalseGet = new AtomicBoolean(false);
        final AtomicBoolean changeOnFalse = new AtomicBoolean(false);

        optionalBoolean.orElse(() -> changeOnFalse.set(true));

        assertFalse(optionalBoolean.isPresent());
        assertEquals(null, optionalBoolean.orElseGet(() -> changeOnFalseGet.set(true)));
        assertTrue(changeOnFalseGet.get());
        assertTrue(changeOnFalse.get());

        try {

            optionalBoolean.get();
            fail("Should try exception");
        } catch (Exception e) {}
    }

    @Test
    public void testTrue(){

        final OptionalBoolean optionalBoolean = OptionalBoolean.of(true);
        final AtomicBoolean changeOnTrueGet = new AtomicBoolean(false);
        final AtomicBoolean changeOnTrue = new AtomicBoolean(false);

        optionalBoolean.ifTrue(() -> changeOnTrue.set(true));

        assertTrue(optionalBoolean.isPresent());
        assertEquals(true, optionalBoolean.ifTrueGet(() -> changeOnTrueGet.set(true)));
        assertTrue(changeOnTrueGet.get());
        assertTrue(changeOnTrue.get());
        assertTrue(optionalBoolean.get());
    }

    @Test
    public void testFalse(){

        final OptionalBoolean optionalBoolean = OptionalBoolean.of(false);
        final AtomicBoolean changeOnFalseGet = new AtomicBoolean(false);
        final AtomicBoolean changeOnFalse = new AtomicBoolean(false);

        optionalBoolean.orElse(() -> changeOnFalse.set(true));

        assertTrue(optionalBoolean.isPresent());
        assertEquals(false, optionalBoolean.orElseGet(() -> changeOnFalseGet.set(true)));
        assertTrue(changeOnFalseGet.get());
        assertTrue(changeOnFalse.get());
        assertFalse(optionalBoolean.get());
    }

    @Test
    public void testChain(){

        final OptionalBoolean optionalBoolean = OptionalBoolean.of(false);
        final AtomicBoolean changeOnTrue  = new AtomicBoolean(false);
        final AtomicBoolean changeOnFalse = new AtomicBoolean(false);

        assertTrue(optionalBoolean.isPresent());
        assertFalse(optionalBoolean.ifTrue(() -> changeOnTrue.set(true)).orElse(() -> changeOnFalse.set(true)).get());

        assertFalse(changeOnTrue.get());
        assertTrue(changeOnFalse.get());
    }

}
