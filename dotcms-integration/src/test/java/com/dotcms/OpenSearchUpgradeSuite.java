package com.dotcms;

import com.dotcms.content.elasticsearch.business.DeactivateIndexEmptyStoreIT;
import com.dotcms.content.elasticsearch.business.MigrationPhaseStoreBootstrapIT;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplPhaseSwitchIntegrationTest;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplMidReindexRollbackIT;
import com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplMigrationIntegrationTest;
import com.dotcms.content.index.opensearch.ContentFactoryIndexOperationsOSIntegrationTest;
import com.dotcms.content.index.opensearch.ContentletIndexOperationsOSIntegrationTest;
import com.dotcms.content.index.opensearch.OSCreateContentIndexIntegrationTest;
import com.dotcms.content.index.opensearch.OSMappingAPIImplIntegrationTest;
import com.dotcms.content.index.VersionedIndicesAPITest;
import com.dotcms.content.index.opensearch.OSIndexAPIImplIntegrationTest;
import com.dotcms.content.index.opensearch.OSIndexAPIImplWaitReadyIT;
import com.dotcms.content.index.opensearch.OSClientConfigTest;
import com.dotcms.content.index.opensearch.OSClientProviderIntegrationTest;
import com.dotcms.content.index.opensearch.OSSearchAPIImplIntegrationTest;
import com.dotcms.content.index.opensearch.OSSiteSearchAPIIntegrationTest;
import com.dotcms.enterprise.publishing.sitesearch.SiteSearchDualWriteRouterIT;
import com.dotcms.junit.MainBaseSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * OpenSearch Upgrade Test Suite
 * <p>
 * This test suite contains integration tests specifically designed for testing
 * connectivity and functionality with the OpenSearch 3.x upgrade container.
 * <p>
 * This suite runs only when the 'opensearch-upgrade' Maven profile is activated,
 * which:
 * - Starts the opensearch-upgrade Docker container on port 9201
 * - Configures test environment to use OpenSearch 3.x endpoints
 * - Sets appropriate system properties for OpenSearch upgrade testing
 * <p>
 * Usage:
 * ./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dopensearch.upgrade.test=true
 *
 * @author fabrizio
 */
@RunWith(MainBaseSuite.class)
@SuiteClasses({
        VersionedIndicesAPITest.class,
        OSIndexAPIImplIntegrationTest.class,
        OSIndexAPIImplWaitReadyIT.class,
        OSMappingAPIImplIntegrationTest.class,
        ContentletIndexOperationsOSIntegrationTest.class,
        OSCreateContentIndexIntegrationTest.class,
        ContentFactoryIndexOperationsOSIntegrationTest.class,
        OSClientProviderIntegrationTest.class,
        OSClientConfigTest.class,
        ContentletIndexAPIImplMigrationIntegrationTest.class,
        ContentletIndexAPIImplPhaseSwitchIntegrationTest.class,
        ContentletIndexAPIImplMidReindexRollbackIT.class,
        MigrationPhaseStoreBootstrapIT.class,
        DeactivateIndexEmptyStoreIT.class,
        OSSearchAPIImplIntegrationTest.class,
        OSSiteSearchAPIIntegrationTest.class,
        SiteSearchDualWriteRouterIT.class
})
public class OpenSearchUpgradeSuite {
}