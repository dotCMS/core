package com.liferay.util;

import com.dotcms.UnitTestBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MathUtilTest extends UnitTestBase {

    @Test
    public void testHappyPath(){

        long result = MathUtil.sumAndModule("abc123-efj456".toCharArray(), 10);

        assertEquals( 7, result );


        result = MathUtil.sumAndModule("xxxaa-aa678-efj456".toCharArray(), 10);

        assertEquals( 1, result );
    }
}
