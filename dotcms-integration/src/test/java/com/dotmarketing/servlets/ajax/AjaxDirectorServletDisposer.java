package com.dotmarketing.servlets.ajax;

/**
 * Interface for disposing things created in the test {@link AjaxDirectorServletIntegrationTest}
 *
 * Any disposing needed in a testcase should implement the {@link #dispose()} method
 */

interface AjaxDirectorServletDisposer {
    void dispose();
}
