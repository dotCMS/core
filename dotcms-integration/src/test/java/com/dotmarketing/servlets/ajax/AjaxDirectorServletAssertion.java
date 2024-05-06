package com.dotmarketing.servlets.ajax;

/**
 * Interface for assertions to be executed as a part of the {@link AjaxDirectorServletIntegrationTest}
 *
 * Any assertion needed in a testcase should implement the {@link #executeAssertion} method
 */

interface AjaxDirectorServletAssertion {
    void executeAssertion();
}
