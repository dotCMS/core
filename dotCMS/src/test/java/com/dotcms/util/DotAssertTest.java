package com.dotcms.util;

import com.dotcms.UnitTestBase;
import org.junit.Assert;
import org.junit.Test;

/**
 * CollectionsUtils unit test.
 * @author jsanca
 */
public class DotAssertTest extends UnitTestBase {


    /**
     * Testing the isTrue
     *
     */
    @Test
    public void isTrueTest()  {

        try {

            DotAssert.isTrue(false, "Uppss");
            Assert.fail("It must throws an IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }

        DotAssert.isTrue(true, "Right");

    }


}