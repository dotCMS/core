package com.dotcms;

import com.dotcms.junit.MainBaseSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Integration test suite shard 5 of 7.
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
        com.dotmarketing.portlets.containers.business.ContainerAPIImplTest.class,
        com.dotcms.ema.EMAWebInterceptorTest.class,

        com.dotmarketing.portlets.contentlet.business.ContentletAPITest.class,
        com.dotcms.experiments.business.ExperimentAPIImpIntegrationTest.class,
        com.dotcms.util.content.json.PopulateContentletAsJSONUtilTest.class,
        com.dotcms.rest.api.v1.apps.AppsResourceTest.class,
        com.dotmarketing.quartz.DotStatefulJobTest.class,
        com.dotmarketing.startup.runonce.Task05380ChangeContainerPathToAbsoluteTest.class,
        com.dotcms.uuid.shorty.ShortyIdApiTest.class,
        com.dotcms.rest.api.v1.taillog.TailLogResourceTest.class,
        com.dotmarketing.portlets.containers.business.ContainerAPITest.class,
        com.dotmarketing.portlets.contentlet.transform.ContentletTransformerTest.class,
        com.dotmarketing.factories.PublishFactoryTest.class,
        com.dotmarketing.portlets.structure.factories.StructureFactoryTest.class,
        com.dotcms.rest.BundleResourceTest.class,
        com.dotmarketing.servlets.BinaryExporterServletTest.class,
        com.dotcms.enterprise.publishing.bundler.URLMapBundlerTest.class,
        com.dotcms.rest.api.v1.vtl.VTLResourceIntegrationTest.class,
        com.dotcms.filters.VanityUrlFilterTest.class,
        com.dotmarketing.portlets.fileassets.business.FileAssetFactoryIntegrationTest.class,
        com.dotcms.integritycheckers.IntegrityUtilTest.class,
        com.dotcms.concurrent.lock.DotKeyLockManagerTest.class,
        com.dotcms.rest.StoryBlockMarkdownPopulatorTest.class,
        com.dotmarketing.portlets.workflows.actionlet.FourEyeApproverActionletTest.class,
        com.dotmarketing.business.DeterministicIdentifierAPITest.class,
        com.dotmarketing.portlets.folders.business.FolderFactoryImplTest.class,
        com.dotcms.translate.GoogleTranslationServiceIntegrationTest.class,
        com.dotcms.util.pagination.ContentTypesPaginatorTest.class,
        com.dotcms.rest.api.v1.drive.ContentDriveKeywordSearchTest.class,
        com.dotcms.telemetry.collectors.experiment.CountVariantsInAllArchivedExperimentsMetricTypeTest.class,
        com.dotcms.telemetry.collectors.experiment.CountPagesWithScheduledExperimentsMetricTypeTest.class,
        com.dotcms.content.elasticsearch.business.ESIndexSpeedTest.class,
        com.dotcms.telemetry.collectors.experiment.CountPagesWithAllEndedExperimentsMetricTypeTest.class,
        com.dotcms.content.model.hydration.MetadataDelegateTest.class,
        com.dotcms.rendering.velocity.viewtools.content.StoryBlockMapTest.class,
        com.dotcms.graphql.datafetcher.CategoryFieldDataFetcherTest.class,
        com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionletIntegrationTest.class,
        com.dotcms.ai.workflow.OpenAIAutoTagActionletTest.class,
        com.dotcms.csspreproc.CSSPreProcessServletIT.class,
        com.dotmarketing.portlets.links.factories.LinkFactoryTest.class,
        com.dotmarketing.portlets.links.business.MenuLinkAPITest.class,
        com.dotmarketing.sitesearch.viewtool.SiteSearchWebAPITest.class,
        com.dotcms.enterprise.publishing.remote.handler.ContentHandlerTest.class,
        com.liferay.portal.ejb.UserLocalManagerTest.class,
        com.dotmarketing.startup.runonce.Task05190UpdateFormsWidgetCodeFieldTest.class,
        com.dotcms.contenttype.test.ContentTypeTest.class,
        com.dotcms.rendering.velocity.viewtools.LanguageWebAPITest.class,
        com.dotcms.auth.providers.saml.v1.DotSamlResourceTest.class,
        com.dotcms.rest.api.v1.announcements.RemoteAnnouncementsLoaderIntegrationTest.class,
        com.dotcms.contenttype.model.field.layout.FieldLayoutSerializerTest.class,
        com.liferay.util.LocaleUtilTest.class,
        com.dotcms.publisher.receiver.BundlePublisherTest.class,
        com.dotcms.enterprise.publishing.remote.bundler.HostBundlerTest.class,
        com.dotcms.security.apps.SecretsStoreKeyStoreImplTest.class,
        com.dotcms.analytics.metrics.QueryParameterValuesTransformerTest.class,
        com.dotmarketing.portlets.rules.RuleAPITest.class,
        com.dotmarketing.startup.runonce.Task250113CreatePostgresJobQueueTablesTest.class,
        com.dotcms.enterprise.publishing.remote.handler.HandlerUtilTest.class,
        com.dotmarketing.startup.runonce.Task221007AddVariantIntoPrimaryKeyTest.class,
        com.dotcms.cache.lettuce.LettuceCacheTest.class,
        com.dotmarketing.startup.runonce.Task220203RemoveFolderInodeConstraintTest.class,
        com.dotmarketing.startup.runonce.Task05165CreateContentTypeWorkflowActionMappingTableTest.class,
        com.dotmarketing.startup.runonce.Task05070AndTask05080Test.class,
        com.dotmarketing.startup.runonce.Task240131UpdateLanguageVariableContentTypeTest.class,
        com.dotmarketing.startup.runonce.Task201013AddNewColumnsToIdentifierTableTest.class,
        com.dotmarketing.startup.runonce.Task04375UpdateCategoryKeyTest.class,
        com.dotmarketing.startup.runonce.Task260206AddUsagePortletToMenuTest.class,
        com.dotmarketing.portlets.rules.conditionlet.UsersBrowserLanguageConditionletTest.class,
        com.dotmarketing.startup.runonce.Task230713IncreaseDisabledWysiwygColumnSizeTest.class,
        com.dotcms.rest.api.v3.contenttype.MoveFieldFormTest.class,
        com.dotmarketing.startup.runonce.Task220401CreateClusterLockTableTest.class,
        com.dotcms.variant.business.VariantCacheTest.class,
        com.dotmarketing.startup.runonce.Task05050FileAssetContentTypeReadOnlyFileNameTest.class,
        com.dotcms.cdi.SimpleInjectionIT.class,
        com.dotmarketing.portlets.rules.conditionlet.UsersSiteVisitsConditionletTest.class,
        com.dotcms.cdi.SimpleJUnit4InjectionIT.class,
        com.dotmarketing.util.TestConfig.class,
        com.dotmarketing.startup.runonce.Task05390MakeRoomForLongerJobDetailTest.class,
        com.dotmarketing.util.HashBuilderTest.class
})
public class MainSuite3a {

}
