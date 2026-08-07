package com.dotcms;

import com.dotcms.junit.MainBaseSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Integration test suite shard 6 of 7.
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
        com.dotmarketing.portlets.templates.business.TemplateAPITest.class,

        com.dotcms.content.elasticsearch.business.ESContentletAPIImplTest.class,
        com.dotcms.rendering.velocity.services.HTMLPageAssetRenderedTest.class,
        com.dotcms.content.elasticsearch.business.ESIndexAPITest.class,
        com.dotmarketing.portlets.folders.business.FolderAPITest.class,
        com.dotmarketing.cms.urlmap.URLMapAPIImplTest.class,
        com.dotcms.storage.FileMetadataAPITest.class,
        com.dotmarketing.portlets.htmlpages.business.HTMLPageAPITest.class,
        com.dotmarketing.portlets.languagesmanager.business.LanguageAPITest.class,
        com.dotmarketing.portlets.containers.business.FileAssetContainerUtilTest.class,
        com.dotmarketing.portlets.browser.BrowserUtilTest.class,
        com.dotmarketing.business.VersionableAPITest.class,
        com.dotcms.content.business.json.ContentletJsonAPITest.class,
        com.dotcms.enterprise.priv.ESSearchProxyTest.class,
        com.dotcms.enterprise.publishing.PublishDateUpdaterIntegrationTest.class,
        com.dotmarketing.business.PermissionAPIIntegrationTest.class,
        com.dotcms.rest.api.v2.asset.WebAssetResourceV2IntegrationTest.class,
        com.dotcms.rest.api.v1.system.permission.PermissionResourceIntegrationTest.class,
        com.dotmarketing.business.VersionableFactoryImplTest.class,
        com.dotcms.rest.api.v1.contenttype.ContentTypeResourceTest.class,
        com.dotmarketing.portlets.contentlet.business.ContentletFactoryTest.class,
        com.dotmarketing.portlets.templates.business.FileAssetTemplateUtilTest.class,
        com.dotmarketing.filters.CMSUrlUtilIntegrationTest.class,
        com.dotcms.rest.api.v3.contenttype.FieldResourceTest.class,
        com.dotcms.rest.api.v1.contenttype.ContentTypeResourceIssue15124Test.class,
        com.dotcms.graphql.datafetcher.page.NumberContentsDataFetcherTest.class,
        com.dotcms.analytics.track.collectors.PagesCollectorTest.class,
        com.dotcms.contenttype.test.DeleteFieldJobTest.class,
        com.dotcms.contenttype.test.JsonContentTypeTransformerTest.class,
        com.dotcms.telemetry.collectors.experiment.CountPagesWithRunningExperimentsMetricTypeTest.class,
        com.dotcms.telemetry.collectors.experiment.CountVariantsInAllScheduledExperimentsMetricTypeTest.class,
        com.dotcms.contenttype.business.SiteAndFolderResolverImplTest.class,
        com.dotmarketing.portlets.workflows.business.SaveContentActionletTest.class,
        com.dotcms.ai.viewtool.EmbeddingsToolTest.class,
        com.dotcms.ai.app.ConfigServiceTest.class,
        com.dotcms.rendering.velocity.viewtools.WebsiteToolTest.class,
        com.dotmarketing.osgi.GenericBundleActivatorIntegrationTest.class,
        com.dotmarketing.portlets.containers.business.ContainerStructureFinderStrategyResolverTest.class,
        com.dotcms.publisher.bundle.business.BundleAPITest.class,
        com.dotcms.publisher.bundle.business.BundleFactoryTest.class,
        com.dotmarketing.factories.WebAssetFactoryTest.class,
        com.dotmarketing.portlets.containers.business.ContainerFactoryImplTest.class,
        com.dotcms.rest.api.v1.container.ContainerResourceIntegrationTest.class,
        com.dotcms.auth.providers.jwt.JsonWebTokenUtilsIntegrationTest.class,
        com.dotcms.ai.viewtool.CompletionsToolTest.class,
        com.dotcms.enterprise.publishing.remote.handler.RuleBundlerHandlerTest.class,
        com.dotmarketing.portlets.workflows.business.WorkflowFactoryTest.class,
        com.dotcms.contenttype.model.field.layout.FieldLayoutTest.class,
        com.dotcms.rest.api.v1.asset.AssetPathResolverImplIntegrationTest.class,
        com.dotmarketing.portlets.rules.conditionlet.VisitedUrlConditionletTest.class,
        com.dotcms.analytics.attributes.CustomAttributeAPIImplTest.class,
        com.dotcms.publisher.endpoint.business.PublishingEndPointAPITest.class,
        com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMappingTest.class,
        com.dotcms.publishing.PushPublishFiltersInitializerTest.class,
        com.dotcms.rendering.velocity.services.VelocityResourceKeyTest.class,
        com.dotcms.cluster.business.ServerAPIImplTest.class,
        com.dotcms.enterprise.publishing.remote.bundler.ContentTypeBundlerTest.class,
        com.dotmarketing.common.db.DBTimeZoneCheckTest.class,
        com.dotmarketing.factories.TreeFactoryTest.class,
        com.dotmarketing.startup.runonce.Task220202RemoveFKStructureFolderConstraintTest.class,
        com.dotmarketing.startup.runonce.Task05395RemoveEndpointIdForeignKeyInIntegrityResolverTablesIntegrationTest.class,
        com.dotmarketing.startup.runonce.Task220215MigrateDataFromInodeToFolderTest.class,
        com.dotcms.mail.MailAPIImplTest.class,
        com.dotmarketing.portlets.workflows.actionlet.VelocityScriptActionletAbortTest.class,
        com.dotmarketing.startup.runonce.Task230701AddHashIndicesToWorkflowTablesTest.class,
        com.dotmarketing.startup.runonce.Task220413IncreasePublishedPushedAssetIdColTest.class,
        com.dotmarketing.startup.runonce.Task220912UpdateCorrectShowOnMenuPropertyTest.class,
        com.dotmarketing.startup.runonce.Task250828CreateCustomAttributeTableTest.class,
        com.dotcms.util.marshal.MarshalUtilsIntegrationTest.class,
        com.dotmarketing.startup.runonce.Task220402UpdateDateTimezonesTest.class,
        com.dotmarketing.startup.runonce.Task05350AddDotSaltClusterColumnTest.class,
        com.dotcms.rest.AuditPublishingResourceTest.class,
        com.dotmarketing.startup.runonce.Task241015ReplaceLanguagesWithLocalesPortletTest.class,
        com.dotmarketing.startup.runonce.Task210805DropUserProxyTableTest.class,
        com.dotmarketing.beans.HostTest.class,
        com.dotmarketing.startup.runonce.Task211007RemoveNotNullConstraintFromCompanyMXColumnTest.class,
        com.dotmarketing.common.db.DotConnectTest.class,
        com.dotmarketing.startup.runonce.Task04375UpdateColorsTest.class,
        com.dotmarketing.db.DbConnectionFactoryUtilTest.class
})
public class MainSuite3b {

}
