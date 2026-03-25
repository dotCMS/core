package com.dotmarketing.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DotDataExceptionTest {

    @Test
    public void testThrowableConstructor_preservesMessage() {
        final RuntimeException cause = new RuntimeException("original cause message");
        final DotDataException exception = new DotDataException(cause);
        assertEquals("DotDataException(Throwable) must preserve the cause's message",
                "original cause message", exception.getMessage());
    }

    @Test
    public void testThrowableConstructor_preservesCause() {
        final RuntimeException cause = new RuntimeException("cause");
        final DotDataException exception = new DotDataException(cause);
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testThrowableConstructor_nullCauseMessage() {
        final RuntimeException cause = new RuntimeException();
        final DotDataException exception = new DotDataException(cause);
        assertNull("When cause has no message, DotDataException message should also be null",
                exception.getMessage());
    }

    @Test
    public void testStringConstructor_preservesMessage() {
        final DotDataException exception = new DotDataException("direct message");
        assertEquals("direct message", exception.getMessage());
    }

    @Test
    public void testStringThrowableConstructor_preservesMessage() {
        final RuntimeException cause = new RuntimeException("cause");
        final DotDataException exception = new DotDataException("explicit message", cause);
        assertEquals("explicit message", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
