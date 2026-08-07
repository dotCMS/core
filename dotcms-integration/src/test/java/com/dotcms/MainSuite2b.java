package com.dotcms;

import com.dotcms.junit.MainBaseSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Integration test suite shard 4 of 7.
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
        com.dotmarketing.quartz.job.CleanUpFieldReferencesJobTest.class,
        com.dotmarketing.common.reindex.ReindexThreadTest.class,

        com.dotcms.rest.api.v1.page.PageResourceTest.class,
        com.dotcms.util.ImportUtilTest.class,
        com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplTest.class,
        com.dotcms.contenttype.business.StoryBlockAPITest.class,
        com.dotcms.rendering.velocity.viewtools.content.ContentToolTest.class,
        com.dotcms.rest.MapToContentletPopulatorTest.class,
        com.dotcms.rest.api.v1.template.TemplateResourceTest.class,
        com.dotcms.publisher.business.PublisherTest.class,
        com.dotcms.content.elasticsearch.business.ESMappingAPITest.class,
        org.apache.felix.framework.OSGIUtilTest.class,
        com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPIImplIntegrationTest.class,
        com.dotcms.contenttype.business.uniquefields.extratable.DBUniqueFieldValidationStrategyTest.class,
        com.dotcms.rest.api.v1.theme.ThemeResourceIntegrationTest.class,
        com.dotcms.rendering.velocity.viewtools.content.BinaryMapTest.class,
        com.dotcms.content.elasticsearch.business.ES6UpgradeTest.class,
        com.dotcms.rest.api.v2.tags.TagResourceIntegrationTest.class,
        com.dotcms.ai.api.OpenAIVisionAPIImplTest.class,
        com.dotmarketing.business.UserAPITest.class,
        com.dotcms.rest.api.v1.announcements.AnnouncementsHelperIntegrationTest.class,
        com.dotmarketing.portlets.linkchecker.business.LinkCheckerAPITest.class,
        com.dotcms.contenttype.business.FileAssetBaseTypeToContentTypeStrategyImplTest.class,
        com.dotcms.publishing.remote.RemoteReceiverLanguageResolutionTest.class,
        com.dotmarketing.portlets.structure.model.ContentletRelationshipsTest.class,
        com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactoryTest.class,
        com.dotcms.rest.api.v1.content.search.strategies.GlobalSearchAttributeStrategyMatchingTest.class,
        com.dotmarketing.portlets.contentlet.model.IntegrationResourceLinkTest.class,
        com.dotcms.ai.viewtool.AIViewToolTest.class,
        com.dotcms.telemetry.collectors.experiment.CountVariantsInAllEndedExperimentsMetricTypeTest.class,
        com.dotmarketing.startup.runonce.Task250604UpdateFolderInodesTest.class,
        com.dotcms.rendering.velocity.viewtools.DotTemplateToolTest.class,
        com.dotmarketing.startup.runonce.Task220330ChangeVanityURLSiteFieldTypeTest.class,
        com.dotcms.rendering.velocity.viewtools.ContentSearchToolTest.class,
        com.dotcms.rendering.velocity.viewtools.XsltToolTest.class,
        com.dotcms.telemetry.collectors.theme.TotalSizeOfFilesPerThemeMetricTypeTest.class,
        com.dotcms.contenttype.test.FieldBuilderTest.class,
        com.dotcms.rest.api.v1.drive.ContentDriveWorkflowFilterTest.class,
        com.dotcms.rest.api.v1.maintenance.ClusterLogCollectorTest.class,
        com.dotmarketing.portlets.workflows.actionlet.SaveContentActionletWithTagsTest.class,
        com.dotcms.contenttype.test.ContentTypeBuilderTest.class,
        com.dotmarketing.portlets.workflows.business.WorkflowAPIMultiLanguageTest.class,
        com.dotcms.publisher.endpoint.bean.PublishingEndPointTest.class,
        com.dotcms.integritycheckers.HostIntegrityCheckerTest.class,
        com.dotmarketing.startup.runonce.Task220825CreateVariantFieldTest.class,
        com.dotmarketing.startup.runonce.Task240606AddVariableColumnToWorkflowTest.class,
        com.dotcms.variant.VariantFactoryTest.class,
        com.dotcms.rest.WebResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.relationships.RelationshipsResourceTest.class,
        com.dotcms.contenttype.model.field.layout.FieldLayoutColumnSerializerTest.class,
        com.dotcms.saml.IdentityProviderConfigurationFactoryTest.class,
        com.dotmarketing.startup.runonce.Task210816DeInodeRelationshipTest.class,
        com.dotmarketing.portlets.rules.business.RulesCacheFTest.class,
        com.dotcms.integritycheckers.FolderIntegrityCheckerTest.class,
        com.dotcms.enterprise.publishing.remote.bundler.ContainerBundlerTest.class,
        com.dotcms.publishing.manifest.ManifestReaderFactoryTest.class,
        com.dotcms.publishing.manifest.CSVManifestBuilderTest.class,
        com.liferay.portal.language.LanguageUtilTest.class,
        com.dotmarketing.common.db.DotDatabaseMetaDataTest.class,
        com.dotmarketing.startup.runonce.Task211101AddContentletAsJsonColumnTest.class,
        com.dotcms.enterprise.publishing.remote.bundler.WorkflowBundlerTest.class,
        com.dotmarketing.quartz.job.BinaryCleanupJobTest.class,
        com.dotcms.enterprise.publishing.remote.bundler.TemplateBundlerTest.class,
        com.dotcms.rendering.velocity.viewtools.JSONToolTest.class,
        com.dotmarketing.startup.runonce.Task240530AddDotAIPortletToLayoutTest.class,
        com.dotcms.enterprise.publishing.remote.bundler.RuleBundlerTest.class,
        com.dotcms.rest.api.v1.system.ConfigurationHelperTest.class,
        com.dotmarketing.startup.runonce.Task05035LanguageTableIdentityOffTest.class,
        com.dotcms.publisher.bundle.business.BundleFactoryImplTest.class,
        com.dotcms.rest.api.v1.apps.view.AppsInterpolationTest.class,
        com.dotcms.analytics.track.RequestMatcherTest.class,
        com.dotmarketing.startup.runonce.Task260720AddDefaultBaseTypeToFolderTableTest.class,
        com.dotmarketing.util.ResourceCollectorUtilTest.class,
        com.dotmarketing.startup.runonce.Task250107RemoveEsReadOnlyMonitorJobTest.class,
        com.dotcms.mock.request.CachedParameterDecoratorTest.class,
        com.dotcms.cache.lettuce.DotObjectCodecTest.class,
        com.dotcms.storage.repository.HashedLocalFileRepositoryManagerTest.class,
        com.dotmarketing.startup.runonce.Task240112AddMetadataColumnToStructureTableTest.class,
        com.dotmarketing.util.ConfigUtilsTest.class
})
public class MainSuite2b {

}
