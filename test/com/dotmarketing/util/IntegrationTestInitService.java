package com.dotmarketing.util;

import com.dotmarketing.servlets.test.ServletTestRunner;

/**
 * Sets up the web environment needed to execute integration tests without a server application
 * Created by nollymar on 9/29/16.
 */
public class IntegrationTestInitService {
    private static IntegrationTestInitService service = null;

    private static boolean initCompleted;

    private IntegrationTestInitService() {
        initCompleted = false;
    }

    public static IntegrationTestInitService getInstance() {
        if (service == null) {
            service = new IntegrationTestInitService();
        }

        return service;
    }

    public void init() throws Exception {
        if (!initCompleted && (System.getProperty("TEST-RUNNER") == null || !System.getProperty("TEST-RUNNER")
            .equals(ServletTestRunner.class.getCanonicalName()))) {
            TestingJndiDatasource.init();
            ConfigTestHelper._setupFakeTestingContext();

            initCompleted = true;
        }
    }
}
