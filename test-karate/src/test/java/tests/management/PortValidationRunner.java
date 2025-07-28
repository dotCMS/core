package tests.management;

import com.intuit.karate.junit5.Karate;

/**
 * Test runner for comprehensive management port validation and security tests.
 * 
 * These tests verify:
 * 1. Security isolation between regular and management ports
 * 2. Proxy header validation and support
 * 3. Error handling and user-friendly messages
 * 4. HTTP method restrictions
 * 5. Security bypass prevention
 * 6. Performance under concurrent access
 * 7. Configuration validation
 * 
 * To run these specific tests only (with Docker):
 * ./mvnw -pl :dotcms-test-karate verify -Dkarate.test.skip=false -Dit.test=PortValidationRunner
 * 
 * To run all management tests by specific runners (with Docker):
 * ./mvnw -pl :dotcms-test-karate verify -Dkarate.test.skip=false -Dit.test=ManagementEndpointsRunner,PortValidationRunner
 * 
 * To run specific security tests within this runner:
 * ./mvnw -pl :dotcms-test-karate verify -Dkarate.test.skip=false -Dit.test=PortValidationRunner -Dkarate.options="--tags @security"
 * 
 * Note: These tests require Docker services and use Maven Failsafe plugin (-Dit.test, not -Dtest)
 */
public class PortValidationRunner {
    
    @Karate.Test
    Karate testPortValidation() {
        return Karate.run("PortValidation").relativeTo(getClass());
    }
}