package com.dotcms.e2e;

import com.dotcms.e2e.test.LoginTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        LoginTests.class/*,
        ContentPagesTests.class,
        ContentSearchTests.class,
        SiteLayoutContainersTests.class*/
})
public class E2eTestSuite {
}
