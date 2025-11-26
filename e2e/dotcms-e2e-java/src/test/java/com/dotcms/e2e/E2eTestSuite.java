package com.dotcms.e2e;

import com.dotcms.e2e.test.ContentPagesTests;
import com.dotcms.e2e.test.ContentSearchTests;
import com.dotcms.e2e.test.LoginTests;
import com.dotcms.e2e.test.SiteLayoutContainersTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * This class defines a test suite for end-to-end tests in the dotCMS application.
 * It uses JUnit 5's @Suite and @SelectClasses annotations to specify the test classes
 * that should be included in the suite.
 *
 * To run this suite, simply execute:
 * <pre>
 *  ./mvnw -pl :dotcms-e2e-java verify -De2e.test.skip=false -Dit.test=E2eTestSuite
 * </pre>
 *
 * @author vico
 */
@Suite
@SelectClasses({
        LoginTests.class/*,
        ContentPagesTests.class,
        ContentSearchTests.class,
        SiteLayoutContainersTests.class*/
})
public class E2eTestSuite {
}
