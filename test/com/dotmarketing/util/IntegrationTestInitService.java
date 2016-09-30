package com.dotmarketing.util;

import com.dotmarketing.servlets.test.ServletTestRunner;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sets up the web environment needed to execute integration tests without a server application
 * Created by nollymar on 9/29/16.
 */
public class IntegrationTestInitService {
    private static IntegrationTestInitService service = new IntegrationTestInitService();

    private static AtomicBoolean initCompleted;

    private IntegrationTestInitService() {
        initCompleted = new AtomicBoolean(false);
    }

    public static IntegrationTestInitService getInstance() {
        return service;
    }

    public void init() throws Exception {
        if (!initCompleted.get() && (System.getProperty("TEST-RUNNER") == null || !System.getProperty("TEST-RUNNER")
            .equals(ServletTestRunner.class.getCanonicalName()))) {
            TestingJndiDatasource.init();
            ConfigTestHelper._setupFakeTestingContext();

            initCompleted.set(true);
        }
    }
}
