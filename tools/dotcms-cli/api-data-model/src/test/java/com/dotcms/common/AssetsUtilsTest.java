package com.dotcms.common;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AssetsUtilsTest {

    @Test
    void Test_URLIsFolder() {

        Assertions.assertTrue(AssetsUtils.URLIsFolder("//demo.dotcms.com/images"));
        Assertions.assertTrue(AssetsUtils.URLIsFolder("//demo.dotcms.com/images/"));
        Assertions.assertTrue(AssetsUtils.URLIsFolder("//demo.dotcms.com/images/blogs"));
        Assertions.assertTrue(AssetsUtils.URLIsFolder("//demo.dotcms.com"));
        Assertions.assertTrue(AssetsUtils.URLIsFolder("//demo.dotcms.com/"));
        Assertions.assertFalse(AssetsUtils.URLIsFolder("//demo.dotcms.com/images/blogs/shark-feeding.jpg"));
        Assertions.assertFalse(AssetsUtils.URLIsFolder("//demo.dotcms.com/icon.png"));
        Assertions.assertFalse(AssetsUtils.URLIsFolder("//demo.dotcms.com/v.vtl"));
    }

}
