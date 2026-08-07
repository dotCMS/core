package com.dotcms;

import com.dotcms.junit.MainBaseSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Integration test suite shard 7 of 7.
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
        com.dotcms.content.elasticsearch.util.ESMappingUtilHelperTest.class,

        com.dotcms.contenttype.test.ContentTypeAPIImplTest.class,
        com.dotmarketing.portlets.htmlpages.business.render.HTMLPageAssetRenderedAPIImplIntegrationTest.class,
        com.dotcms.contenttype.test.ContentTypeFactoryImplTest.class,
        com.dotcms.publisher.util.DependencyModDateUtilTest.class,
        com.dotmarketing.portlets.workflows.business.WorkflowAPITest.class,
        com.dotmarketing.tag.business.TagAPITest.class,
        com.dotcms.rest.api.v1.workflow.WorkflowResourceIntegrationTest.class,
        com.dotcms.rendering.velocity.viewtools.navigation.NavToolTest.class,
        com.dotcms.rest.api.v1.contenttype.FieldResourceTest.class,
        com.dotcms.variant.VariantAPITest.class,
        com.dotcms.rendering.velocity.servlet.VelocityServletIntegrationTest.class,
        com.dotcms.jobs.business.api.JobQueueManagerAPITest.class,
        com.dotcms.contenttype.business.StoryBlockValidationTest.class,
        com.dotcms.dotpubsub.PostgresPubSubImplTest.class,
        com.dotmarketing.portlets.categories.business.CategoryAPITest.class,
        com.dotcms.telemetry.collectors.MetricTimeoutTest.class,
        com.dotcms.rest.api.v1.publishing.BundleManagementResourceIntegrationTest.class,
        com.dotmarketing.business.IdentifierFactoryTest.class,
        com.dotcms.rendering.velocity.directive.DotParseTest.class,
        com.dotmarketing.portlets.contentlet.ajax.ContentletAjaxTest.class,
        com.dotcms.ai.listener.EmbeddingContentListenerTest.class,
        com.dotcms.rest.api.v1.content.ContentVersionResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.authentication.ResetPasswordResourceIntegrationTest.class,
        com.dotcms.rest.TagResourceIntegrationTest.class,
        com.dotmarketing.portlets.workflows.actionlet.VelocityScriptActionletTest.class,
        com.dotmarketing.portlets.categories.business.CategoryFactoryTest.class,
        com.dotcms.rest.api.v1.page.NavResourceTest.class,
        com.dotcms.publishing.PublisherAPITest.class,
        com.dotcms.telemetry.collectors.experiment.CountPagesWithArchivedExperimentsMetricTypeTest.class,
        com.dotcms.publisher.business.PublisherQueueJobTest.class,
        com.dotmarketing.portlets.workflows.actionlet.PushNowActionletTest.class,
        com.dotmarketing.portlets.workflows.business.SaveContentDraftActionletTest.class,
        com.dotcms.rest.api.v1.apps.SiteViewPaginatorIntegrationTest.class,
        com.dotcms.contenttype.business.ContentTypeInitializerTest.class,
        com.dotcms.rest.api.v1.user.UserResourceIntegrationTest.class,
        com.dotmarketing.business.SecondaryCategoryPermissionTest.class,
        com.dotcms.storage.FileStorageAPITest.class,
        com.dotcms.publisher.util.PushedAssetUtilTest.class,
        com.dotcms.rest.api.v1.workflow.WorkflowResourceLicenseIntegrationTest.class,
        com.dotcms.graphql.datafetcher.page.RunningExperimentFetcherTest.class,
        com.dotcms.contenttype.model.field.layout.FieldUtilTest.class,
        com.dotmarketing.util.MaintenanceUtilTest.class,
        com.dotmarketing.portlets.contentlet.business.ContentletCacheImplTest.class,
        com.dotmarketing.startup.runonce.Task230523CreateVariantFieldInContentletIntegrationTest.class,
        com.dotmarketing.fixtask.tasks.FixTask00090RecreateMissingFoldersInParentPathTest.class,
        com.dotcms.rest.api.v1.authentication.CreateJsonWebTokenResourceIntegrationTest.class,
        com.dotcms.content.business.json.LegacyJSONObjectRenderTest.class,
        com.dotcms.visitor.filter.characteristics.VisitorCharacterTest.class,
        com.dotmarketing.fixTasks.FixTask00085FixEmptyParentPathOnIdentifierTest.class,
        com.dotcms.rendering.velocity.viewtools.content.StoryBlockTest.class,
        com.dotmarketing.business.web.UserWebAPIImplTest.class,
        com.dotcms.auth.providers.jwt.services.JsonWebTokenServiceIntegrationTest.class,
        com.dotmarketing.startup.runonce.Task260407AddBaseTypeColumnToIdentifierTest.class,
        com.dotmarketing.quartz.QuartzUtilsTest.class,
        com.dotcms.analytics.bayesian.BayesianAPIImplIT.class,
        com.dotcms.rest.api.v1.configuration.ConfigurationResourceTest.class,
        com.dotcms.enterprise.publishing.remote.bundler.FolderBundlerTest.class,
        com.dotmarketing.portlets.personas.business.DeleteMultiTreeUsedPersonaTagJobTest.class,
        com.dotmarketing.util.UtilMethodsITest.class,
        com.dotmarketing.startup.runonce.Task230110MakeSomeSystemFieldsRemovableByBaseTypeTest.class,
        com.dotmarketing.startup.runonce.Task201102UpdateColumnSitelicTableTest.class,
        com.dotmarketing.startup.runonce.Task220822CreateVariantTableTest.class,
        com.dotmarketing.startup.runonce.Task231109AddPublishDateToContentletVersionInfoTest.class,
        com.dotcms.contenttype.test.StoryBlockUtilTest.class,
        com.dotmarketing.startup.runonce.Task260505AddPluginsPortletToMenuTest.class,
        com.dotmarketing.startup.runonce.Task230328AddMarkedForDeletionColumnTest.class,
        com.dotmarketing.startup.runonce.Task260320AddPluginsPortletToMenuTest.class,
        com.dotcms.filters.interceptor.meta.MetaWebInterceptorTest.class,
        com.dotmarketing.startup.runonce.Task220606UpdatePushNowActionletNameTest.class,
        com.dotmarketing.portlets.workflows.model.TestWorkflowAction.class,
        com.dotmarketing.startup.runonce.Task211103RenameHostNameLabelTest.class,
        com.dotmarketing.startup.runonce.Task241016AddCustomLanguageVariablesPortletToLayoutTest.class,
        com.dotmarketing.startup.runonce.Task240111AddInodeAndIdentifierLeftIndexesTest.class,
        com.dotmarketing.startup.runonce.Task210719CleanUpTitleFieldTest.class,
        com.dotcms.enterprise.publishing.staticpublishing.LanguageFolderTest.class,
        com.dotcms.security.multipart.SecureFileValidatorTest.class,
        com.dotcms.publishing.PublisherFilterImplTest.class,
        com.dotcms.enterprise.cluster.ClusterFactoryTest.class
})
public class MainSuite4a {

}
