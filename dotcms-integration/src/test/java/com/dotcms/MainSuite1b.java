package com.dotcms;

import com.dotcms.junit.MainBaseSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Integration test suite shard 2 of 7.
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
        com.dotmarketing.common.reindex.ReindexAPITest.class,

        com.dotcms.rendering.velocity.viewtools.content.util.ContentUtilsTest.class,
        com.dotcms.content.elasticsearch.business.ESContentFactoryImplTest.class,
        com.dotcms.enterprise.rules.RulesAPIImplIntegrationTest.class,
        com.dotcms.publishing.job.SiteSearchJobImplTest.class,
        com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldDataBaseUtilTest.class,
        com.dotmarketing.portlets.contentlet.business.HostFactoryImplTest.class,
        com.dotcms.graphql.datafetcher.page.ContentMapDataFetcherTest.class,
        com.dotcms.publisher.util.DependencyManagerTest.class,
        com.dotcms.rest.api.v1.asset.WebAssetHelperIntegrationTest.class,
        com.dotcms.enterprise.publishing.remote.StaticPushPublishBundleGeneratorTest.class,
        com.dotmarketing.portlets.contentlet.business.ContentletCheckInTest.class,
        com.dotcms.contenttype.business.RelationshipAPITest.class,
        com.dotcms.rest.api.v1.content.ContentResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.maintenance.MaintenanceResourceIntegrationTest.class,
        com.dotcms.rest.elasticsearch.ESContentResourcePortletTest.class,
        com.dotcms.experiments.business.RootIndexRegexUrlPatterStrategyIntegrationTest.class,
        com.dotmarketing.portlets.contentlet.util.ContentletUtilTest.class,
        com.dotcms.util.RelationshipUtilTest.class,
        com.dotcms.auth.providers.jwt.factories.ApiTokenAPITest.class,
        com.dotcms.enterprise.publishing.remote.HostBundlerHandlerTest.class,
        com.dotmarketing.portlets.templates.business.TemplateFactoryImplTest.class,
        com.dotmarketing.portlets.browser.ajax.BrowserAjaxTest.class,
        com.dotcms.contenttype.business.RelationshipFactoryImplTest.class,
        com.dotcms.rest.api.v1.container.ContainerResourceHostResolutionIT.class,
        com.dotcms.contenttype.test.DotAssetAPITest.class,
        com.dotmarketing.startup.runonce.Task05200WorkflowTaskUniqueKeyTest.class,
        com.dotmarketing.portlets.contentlet.transform.WidgetViewStrategyTest.class,
        com.dotcms.telemetry.collectors.experiment.CountPagesWithDraftExperimentsMetricTypeTest.class,
        com.dotcms.auth.providers.saml.v1.SAMLHelperTest.class,
        com.dotcms.analytics.track.collectors.SyncVanitiesCollectorTest.class,
        com.dotcms.analytics.track.collectors.AsyncVanitiesCollectorTest.class,
        com.dotmarketing.business.LayoutAPITest.class,
        com.dotmarketing.portlets.workflows.actionlet.SaveContentActionletTest.class,
        com.dotmarketing.business.PermissionBitFactoryImplTest.class,
        com.dotcms.rest.api.v1.contenttype.ContentTypeResourceUpdateMetadataTest.class,
        com.dotcms.analytics.track.collectors.FilesCollectorTest.class,
        com.dotmarketing.startup.runonce.Task210901UpdateDateTimezonesTest.class,
        com.dotcms.rest.api.v1.drive.ContentDriveWorkflowArchiveStepTest.class,
        com.dotcms.ai.workflow.OpenAIContentPromptActionletTest.class,
        com.dotmarketing.portlets.workflows.actionlet.MoveContentActionletTest.class,
        com.dotcms.uuid.shorty.LegacyShortyIdApiTest.class,
        com.dotcms.publisher.business.PublishQueueElementTransformerTest.class,
        com.dotmarketing.startup.runonce.Task05210CreateDefaultDotAssetTest.class,
        com.dotcms.security.multipart.ContentDispositionFileNameParserTest.class,
        com.dotcms.rest.api.v1.pushpublish.PushPublishFilterResourceTest.class,
        com.dotmarketing.startup.runonce.Task05170DefineFrontEndAndBackEndRolesTest.class,
        com.dotcms.rendering.velocity.VelocityMacroCacheTest.class,
        com.dotmarketing.util.PageModeTest.class,
        com.dotmarketing.servlets.ajax.AjaxDirectorServletIntegrationTest.class,
        com.dotcms.rest.api.v1.company.CompanyResourceIntegrationTest.class,
        com.dotcms.enterprise.publishing.remote.handler.ContentWorkflowHandlerTest.class,
        com.dotmarketing.quartz.job.PopulateContentletAsJSONJobTest.class,
        com.dotmarketing.startup.runonce.Task05030UpdateSystemContentTypesHostTest.class,
        com.dotmarketing.quartz.job.IntegrityDataGenerationJobTest.class,
        com.dotcms.cost.RequestCostReportTest.class,
        com.dotcms.rendering.velocity.viewtools.XmlToolTest.class,
        com.dotmarketing.startup.runonce.Task251212AddVersionColumnIndicesTableTest.class,
        com.dotcms.business.SystemAPITest.class,
        com.dotmarketing.startup.runonce.Task04335CreateSystemWorkflowTest.class,
        com.dotcms.dotpubsub.RedisPubSubImplTest.class,
        com.dotcms.tika.TikaUtilsTest.class,
        com.dotcms.enterprise.publishing.remote.bundler.LinkBundlerTest.class,
        com.dotcms.contenttype.test.KeyValueFieldUtilTest.class,
        com.dotmarketing.business.IdentifierCacheImplTest.class,
        com.dotcms.publisher.endpoint.business.PublishingEndPointFactoryImplTest.class,
        com.dotmarketing.startup.runonce.Task240102AlterVarcharLengthOfRelationTypeTest.class,
        com.dotmarketing.startup.runonce.Task05225RemoveLoadRecordsToIndexTest.class,
        com.dotmarketing.startup.runonce.Task210510UpdateStorageTableDropMetadataColumnTest.class,
        com.dotmarketing.startup.runonce.Task220928AddLookbackWindowColumnToExperimentTest.class,
        com.dotmarketing.startup.runonce.Task05305AddPushPublishFilterColumnTest.class,
        com.dotmarketing.portlets.rules.conditionlet.NumberOfTimesPreviouslyVisitedConditionletTest.class,
        com.dotmarketing.portlets.workflows.model.WorkflowSearcherTest.class,
        com.dotcms.rest.api.CorsFilterTest.class,
        com.dotcms.business.LocalTransactionAndCloseDBIfOpenedFactoryTest.class,
        com.dotmarketing.startup.runonce.Task05195CreatesDestroyActionAndAssignDestroyDefaultActionsToTheSystemWorkflowTest.class,
        com.dotcms.publishing.BundlerUtilTest.class,
        com.dotcms.security.multipart.BoundedBufferedReaderTest.class
})
public class MainSuite1b {

}
