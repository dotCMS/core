package com.dotcms.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocationUtilsTest {

    @Test
    void Test_URLIsFolder() {
        Assertions.assertTrue(LocationUtils.URLIsFolder("//demo.dotcms.com/images"));
        Assertions.assertTrue(LocationUtils.URLIsFolder("//demo.dotcms.com/images/"));
        Assertions.assertTrue(LocationUtils.URLIsFolder("//demo.dotcms.com/images/blogs"));
        Assertions.assertTrue(LocationUtils.URLIsFolder("//demo.dotcms.com"));
        Assertions.assertTrue(LocationUtils.URLIsFolder("//demo.dotcms.com/"));
        Assertions.assertFalse(LocationUtils.URLIsFolder("//demo.dotcms.com/images/blogs/shark-feeding.jpg"));
        Assertions.assertFalse(LocationUtils.URLIsFolder("//demo.dotcms.com/icon.png"));
        Assertions.assertFalse(LocationUtils.URLIsFolder("//demo.dotcms.com/v.vtl"));
    }

}
