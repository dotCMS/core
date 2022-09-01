package com.dotcms.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DotPreconditionUnitTest {

    @Test
    public void NotThrowException(){
        DotPreconditions.checkArgument(true, IllegalArgumentException.class, "This is a %s",
                "Test");

    }

    @Test(expected = IllegalStateException.class)
    public void throwException(){
        try {
            DotPreconditions.checkArgument(false, IllegalStateException.class, "This is a %s",
                    "Test");
        }catch (IllegalStateException e) {
            assertEquals("This is a Test", e.getMessage());
            throw e;
        }

    }

}
