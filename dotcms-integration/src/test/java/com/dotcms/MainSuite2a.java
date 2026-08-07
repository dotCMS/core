package com.dotcms;

import com.dotcms.junit.MainBaseSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Integration test suite shard 3 of 7.
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
        com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplMappingTimeoutIT.class,

        com.dotcms.browser.BrowserAPITest.class,
        com.dotcms.contenttype.business.ContentTypeDestroyAPIImplTest.class,
        com.dotmarketing.portlets.contentlet.business.HostAPITest.class,
        com.dotcms.contenttype.test.ContentResourceTest.class,
        com.dotmarketing.portlets.contentlet.model.ContentletIntegrationTest.class,
        com.dotcms.languagevariable.business.LanguageVariableAPITest.class,
        com.dotmarketing.quartz.job.DropOldContentVersionsJobTest.class,
        com.dotcms.experiments.business.ExperimentUrlPatternCalculatorIntegrationTest.class,
        com.dotcms.rest.api.v1.versionable.VersionableResourceTest.class,
        com.dotcms.enterprise.publishing.remote.PushPublishBundleGeneratorTest.class,
        com.dotmarketing.portlets.fileassets.business.FileAssetAPITest.class,
        com.dotcms.content.elasticsearch.business.ESSiteSearchAPITest.class,
        com.dotcms.contenttype.test.ContentTypeResourceTest.class,
        com.dotcms.rest.api.v1.authentication.ResetPasswordTokenUtilTest.class,
        com.dotcms.contenttype.test.FieldFactoryImplTest.class,
        com.dotcms.rest.api.v1.folder.FolderResourceSearchTest.class,
        com.dotcms.keyvalue.business.KeyValueAPITest.class,
        com.dotcms.publisher.business.PublisherAPIImplTest.class,
        com.dotcms.experiments.business.IndexRegexUrlPatterStrategyIntegrationTest.class,
        com.dotmarketing.portlets.personas.business.PersonaAPITest.class,
        com.dotcms.graphql.datafetcher.RelationshipFieldDataFetcherTest.class,
        com.dotcms.ai.viewtool.SearchToolTest.class,
        com.dotcms.graphql.datafetcher.page.VanityURLFetcherTest.class,
        com.dotmarketing.startup.runalways.Task00050LoadAppsSecretsTest.class,
        com.dotcms.contenttype.test.ContentTypeImportExportTest.class,
        com.dotcms.integritycheckers.ContentPageIntegrityCheckerTest.class,
        com.dotmarketing.portlets.fileassets.business.FileAssetAPIImplIntegrationTest.class,
        com.dotcms.csspreproc.CSSCacheTest.class,
        com.dotcms.analytics.track.collectors.PageDetailCollectorTest.class,
        com.dotcms.telemetry.collectors.experiment.CountVariantsInAllRunningExperimentsMetricTypeTest.class,
        com.dotcms.integritycheckers.ContentFileAssetIntegrityCheckerTest.class,
        com.dotcms.rest.api.v1.drive.ContentDriveHelperContentletAPIComparisonTest.class,
        com.dotmarketing.portlets.structure.transform.ContentletRelationshipsTransformerTest.class,
        com.dotcms.notification.business.NotificationAPITest.class,
        com.dotcms.jitsu.validators.AnalyticsValidatorUtilTest.class,
        com.dotcms.rest.api.v1.temp.TempFileResourceTest.class,
        com.dotmarketing.business.portal.PortletAPIImplTest.class,
        com.dotcms.rest.api.v1.workflow.WorkflowResourceResponseCodeIntegrationTest.class,
        com.dotcms.rendering.velocity.VelocityUtilTest.class,
        com.dotmarketing.image.focalpoint.FocalPointAPITest.class,
        com.dotmarketing.business.IdentifierConsistencyIntegrationTest.class,
        com.dotcms.rest.api.v1.menu.MenuResourceTest.class,
        com.dotcms.publishing.manifest.CSVManifestReaderTest.class,
        com.dotmarketing.common.db.ParamsSetterTest.class,
        com.dotcms.enterprise.publishing.remote.bundler.ContentBundlerTest.class,
        org.apache.velocity.runtime.parser.node.SimpleNodeTest.class,
        com.dotmarketing.portlets.contentlet.action.ImportContentletsActionSmokeTest.class,
        com.dotcms.cache.lettuce.RedisClientTest.class,
        com.dotmarketing.filters.AutoLoginFilterTest.class,
        com.dotmarketing.startup.runonce.Task210218MigrateUserProxyTableTest.class,
        com.dotcms.ai.util.ContentToStringUtilTest.class,
        com.dotcms.workflow.helper.TestSystemActionMappingsHandlerMerger.class,
        com.dotmarketing.startup.runonce.Task220512UpdateNoHTMLRegexValueTest.class,
        com.dotcms.rendering.velocity.ASTMethodTest.class,
        org.apache.velocity.tools.view.tools.CookieToolTest.class,
        com.dotmarketing.servlets.InitRunnerTest.class,
        com.dotcms.contenttype.model.field.layout.FieldLayoutRowSerializerTest.class,
        com.dotmarketing.portlets.languagesmanager.business.LanguageFactoryIntegrationTest.class,
        com.dotmarketing.startup.runonce.Task211012AddCompanyDefaultLanguageTest.class,
        com.dotmarketing.startup.runonce.Task251103AddStylePropertiesColumnInMultiTreeTest.class,
        com.dotmarketing.startup.runonce.Task210319CreateStorageTableTest.class,
        com.dotmarketing.startup.StartupTasksExecutorDataTest.class,
        com.dotcms.business.bytebuddy.ByteBuddyAdviceWeavingTest.class,
        com.dotcms.content.elasticsearch.business.IndiciesFactoryTest.class,
        com.dotmarketing.db.DbConnectionFactoryTest.class,
        com.dotcms.visitor.filter.logger.VisitorLoggerTest.class,
        com.dotmarketing.startup.StartupTasksExecutorTest.class,
        com.dotmarketing.startup.runonce.Task240513UpdateContentTypesSystemFieldTest.class,
        com.dotmarketing.startup.runonce.Task210316UpdateLayoutIconsTest.class,
        com.dotmarketing.startup.runonce.Task220829CreateExperimentsTableTest.class,
        com.dotcms.cdi.SimpleDataProviderWeldRunnerInjectionIT.class,
        com.dotcms.analytics.attributes.CustomAttributeFactoryTest.class,
        com.dotmarketing.cache.FolderCacheImplIntegrationTest.class,
        com.dotcms.cache.KeyValueCacheImplTest.class,
        com.dotcms.publishing.manifest.ManifestUtilTest.class,
        com.dotmarketing.business.IdentifierAPITest.class,
        com.dotcms.rendering.velocity.viewtools.MessageToolTest.class,
        com.dotcms.rendering.js.JsEngineTest.class,
        com.dotmarketing.portlets.rules.conditionlet.ConditionletOSGIFTest.class,
        com.dotmarketing.startup.runonce.Task210321RemoveOldMetadataFilesTest.class,
        com.dotcms.enterprise.publishing.staticpublishing.AWSS3PublisherTest.class,
        com.dotmarketing.portlets.contentlet.model.ContentletDependenciesTest.class,
        com.dotmarketing.startup.runonce.Task05160MultiTreeAddPersonalizationColumnAndChangingPKTest.class,
        com.dotmarketing.startup.runalways.Task00001LoadSchemaIntegrationTest.class
})
public class MainSuite2a {

}
