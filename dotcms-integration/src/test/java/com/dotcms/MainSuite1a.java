package com.dotcms;

import com.dotcms.junit.MainBaseSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Integration test suite shard 1 of 7.
 *
 * Shards are balanced on measured per-class test time so the slowest shard
 * bounds the CI critical path as tightly as possible. When adding a test,
 * put it in the shard with the lowest total time rather than appending here
 * by habit - see .github/test-matrix.yml for the shard list.
 *
 * Classes are fully qualified so that rebalancing does not churn imports.
 */
@RunWith(MainBaseSuite.class)
@SuiteClasses({

        // Data-scanning tests run FIRST on purpose.
        // Integration tests accumulate content and never clean up, so anything
        // that walks the whole dataset (executeUpgrade, findAll*) costs
        // O(all content created so far). Scheduled late these pay for every
        // preceding test's leftovers. Keep new full-scan tests in this block.
        com.dotmarketing.startup.runonce.Task240306MigrateLegacyLanguageVariablesTest.class,
        com.dotmarketing.factories.MultiTreeAPITest.class,

        com.dotcms.vanityurl.business.VanityUrlAPITest.class,
        com.dotcms.enterprise.publishing.remote.bundler.DependencyBundlerTest.class,
        com.dotmarketing.filters.FiltersTest.class,
        com.dotcms.experiments.business.web.ExperimentWebAPIImplIntegrationTest.class,
        com.dotmarketing.business.PermissionAPITest.class,
        com.dotcms.rest.api.v1.page.PageRenderSourcesResourceTest.class,
        com.dotcms.rest.api.v1.folder.FolderResourceTest.class,
        com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTest.class,
        com.dotcms.security.apps.AppsAPIImplTest.class,
        com.dotcms.rendering.velocity.viewtools.content.ContentMapTest.class,
        com.dotcms.contenttype.business.FieldAPITest.class,
        com.dotcms.graphql.business.GraphqlAPITest.class,
        com.dotmarketing.util.contentlet.pagination.PaginatedContentletsIntegrationTest.class,
        com.dotcms.rest.api.v1.publishing.PublishingResourceIntegrationTest.class,
        com.dotmarketing.servlets.ShortyServletAndTitleImageTest.class,
        com.dotcms.rendering.velocity.viewtools.ContainerWebAPIIntegrationTest.class,
        com.dotcms.rendering.velocity.viewtools.FileToolTest.class,
        com.dotmarketing.portlets.structure.factories.FieldFactoryTest.class,
        com.dotcms.analytics.track.collectors.WebEventsCollectorServiceImplTest.class,
        com.dotmarketing.portlets.workflows.actionlet.EmailActionletTest.class,
        com.dotcms.rendering.velocity.viewtools.WorkflowToolTest.class,
        com.liferay.portal.ejb.UserUtilTest.class,
        com.dotmarketing.quartz.job.StartEndScheduledExperimentsJobTest.class,
        com.dotcms.publishing.PublisherAPIImplTest.class,
        com.dotcms.keyvalue.busines.KeyValueAPIImplTest.class,
        com.dotcms.rest.api.v1.drive.ContentDriveFieldFilterTest.class,
        com.dotcms.telemetry.collectors.experiment.CountVariantsInAllDraftExperimentsMetricTypeTest.class,
        com.dotcms.publisher.business.PublishAuditAPITest.class,
        com.dotcms.util.pagination.ContainerPaginatorTest.class,
        com.dotcms.ai.client.AIProxyClientTest.class,
        com.dotcms.rendering.velocity.viewtools.navigation.NavToolCacheTest.class,
        com.dotmarketing.portlets.contentlet.transform.BinaryToMapTransformerTest.class,
        com.dotcms.ai.workflow.OpenAIGenerateImageActionletTest.class,
        com.dotcms.timemachine.business.TimeMachineAPITest.class,
        com.dotcms.storage.StoragePersistenceAPITest.class,
        com.dotcms.rest.api.v2.contenttype.FieldResourceTest.class,
        com.dotmarketing.db.HibernateUtilTest.class,
        com.dotmarketing.quartz.job.EncryptPlainPasswordsJobTest.class,
        com.dotmarketing.business.helper.PermissionHelperTest.class,
        com.dotmarketing.startup.runonce.Task230426AlterVarcharLengthOfLockedByColTest.class,
        com.dotmarketing.startup.runonce.Task201014UpdateColumnsValuesInIdentifierTableTest.class,
        com.dotmarketing.startup.runonce.Task250826AddIndexesToUniqueFieldsTableTest.class,
        com.dotmarketing.startup.runonce.Task210506UpdateStorageTableTest.class,
        com.dotmarketing.business.RoleAPITest.class,
        com.dotcms.publisher.assets.business.PushedAssetsAPITest.class,
        com.dotmarketing.business.CommitListenerCacheWrapperTest.class,
        com.dotmarketing.portlets.workflows.actionlet.CopyActionletTest.class,
        com.dotcms.enterprise.publishing.remote.CategoryBundlerHandlerTest.class,
        com.dotcms.contenttype.business.DotAssetBaseTypeToContentTypeStrategyImplTest.class,
        com.dotcms.util.TimeMachineUtilTest.class,
        com.dotmarketing.portlets.rules.conditionlet.CurrentSessionLanguageConditionletTest.class,
        com.dotmarketing.quartz.job.PruneTimeMachineBackupJobTest.class,
        com.dotcms.saml.SamlConfigurationServiceTest.class,
        com.dotcms.graphql.datafetcher.FolderCollectionDataFetcherTest.class,
        com.dotmarketing.startup.runonce.Task241013RemoveFullPathLcColumnFromIdentifierTest.class,
        com.dotmarketing.startup.runonce.Task210527DropReviewFieldsFromContentletTableTest.class,
        com.dotcms.graphql.DotGraphQLHttpServletTest.class,
        com.dotmarketing.util.ITConfigTest.class,
        com.dotmarketing.startup.runonce.Task230630CreateRunningIdsExperimentFieldIntegrationTest.class,
        com.dotcms.business.SystemTableFactoryTest.class,
        com.dotmarketing.startup.runonce.Task210802UpdateStructureTableTest.class,
        com.dotmarketing.business.web.LanguageWebApiTest.class,
        com.dotcms.analytics.track.collectors.BasicProfileCollectorTest.class,
        com.dotmarketing.startup.runonce.Task220824CreateDefaultVariantTest.class,
        com.dotmarketing.portlets.folders.model.FolderTest.class,
        com.dotmarketing.startup.runonce.Task260615AlterClusterIdLengthTest.class,
        com.dotmarketing.startup.runonce.Task220214AddOwnerAndIDateToFolderTableTest.class,
        com.dotmarketing.startup.runonce.Task230707CreateSystemTableTest.class,
        com.dotcms.business.interceptor.InterceptorHandlerTest.class,
        com.dotcms.content.business.ObjectMapperTest.class,
        com.dotmarketing.startup.runonce.Task05370AddAppsPortletToLayoutTest.class,
        com.dotmarketing.startup.runonce.Task210520UpdateAnonymousEmailTest.class,
        com.dotcms.storage.Chainable404StorageCacheTest.class,
        com.dotmarketing.portlets.rules.conditionlet.VisitorOperatingSystemConditionletTest.class,
        com.dotcms.security.apps.AppsCacheImplTest.class,
        com.dotcms.api.web.HttpServletRequestImpersonatorTest.class
})
public class MainSuite1a {

}
