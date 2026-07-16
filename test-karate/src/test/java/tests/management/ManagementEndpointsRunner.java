package tests.management;

import com.intuit.karate.junit5.Karate;

/**
 * Test runner for management endpoint port restriction tests.
 * 
 * These tests verify that:
 * 1. Management endpoints (/dotmgt/*) are blocked on regular port (8080)
 * 2. Management endpoints work correctly on management port (8090)
 * 3. Proper error messages are returned when endpoints are not accessible
 * 4. Proxy header handling works correctly for management endpoints
 * 
 * To run these specific tests only (with Docker):
 * ./mvnw -pl :dotcms-test-karate verify -Dkarate.test.skip=false -Dit.test=ManagementEndpointsRunner
 * 
 * To run all management tests by specific runners (with Docker):
 * ./mvnw -pl :dotcms-test-karate verify -Dkarate.test.skip=false -Dit.test=ManagementEndpointsRunner,PortValidationRunner
 * 
 * Note: These tests require Docker services and use Maven Failsafe plugin (-Dit.test, not -Dtest)
 */
public class ManagementEndpointsRunner {
    
    @Karate.Test
    Karate testManagementEndpoints() {
        return Karate.run("ManagementEndpoints").relativeTo(getClass());
    }
}