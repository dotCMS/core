package com.dotcms;

import com.dotcms.content.index.VersionedIndicesAPITest;
import com.dotcms.content.index.opensearch.OpenSearchClientConfigTest;
import com.dotcms.content.index.opensearch.OpenSearchClientProviderIntegrationTest;
import com.dotcms.junit.MainBaseSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * OpenSearch Upgrade Test Suite
 *
 * This test suite contains integration tests specifically designed for testing
 * connectivity and functionality with the OpenSearch 3.x upgrade container.
 *
 * This suite runs only when the 'opensearch-upgrade' Maven profile is activated,
 * which:
 * - Starts the opensearch-upgrade Docker container on port 9201
 * - Configures test environment to use OpenSearch 3.x endpoints
 * - Sets appropriate system properties for OpenSearch upgrade testing
 *
 * Usage:
 * ./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dopensearch.upgrade.test=true
 *
 * @author fabrizio
 */
@RunWith(MainBaseSuite.class)
@SuiteClasses({
        VersionedIndicesAPITest.class,
        OpenSearchClientProviderIntegrationTest.class,
        OpenSearchClientConfigTest.class
})
public class OpenSearchUpgradeSuite {
}