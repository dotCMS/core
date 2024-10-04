package com.dotcms;

import com.dotcms.ai.app.AIModelsTest;
import com.dotcms.ai.app.ConfigServiceTest;
import com.dotcms.ai.client.AIProxyClientTest;
import com.dotcms.ai.listener.EmbeddingContentListenerTest;
import com.dotcms.ai.validator.AIAppValidatorTest;
import com.dotcms.ai.viewtool.AIViewToolTest;
import com.dotcms.ai.viewtool.CompletionsToolTest;
import com.dotcms.ai.viewtool.EmbeddingsToolTest;
import com.dotcms.ai.viewtool.SearchToolTest;
import com.dotcms.ai.workflow.OpenAIContentPromptActionletTest;
import com.dotcms.auth.providers.saml.v1.DotSamlResourceTest;
import com.dotcms.auth.providers.saml.v1.SAMLHelperTest;
import com.dotcms.bayesian.BayesianAPIImplIT;
import com.dotcms.business.SystemAPITest;
import com.dotcms.business.SystemTableFactoryTest;
import com.dotcms.cache.lettuce.DotObjectCodecTest;
import com.dotcms.cache.lettuce.LettuceCacheTest;
import com.dotcms.cache.lettuce.RedisClientTest;
import com.dotcms.cdi.SimpleInjectionIT;
import com.dotcms.content.business.ObjectMapperTest;
import com.dotcms.content.business.json.ContentletJsonAPITest;
import com.dotcms.content.business.json.LegacyJSONObjectRenderTest;
import com.dotcms.content.elasticsearch.business.ESIndexAPITest;
import com.dotcms.content.model.hydration.MetadataDelegateTest;
import com.dotcms.contenttype.business.ContentTypeDestroyAPIImplTest;
import com.dotcms.contenttype.business.ContentTypeInitializerTest;
import com.dotcms.contenttype.business.StoryBlockAPITest;
import com.dotcms.csspreproc.CSSCacheTest;
import com.dotcms.csspreproc.CSSPreProcessServletTest;
import com.dotcms.dotpubsub.RedisPubSubImplTest;
import com.dotcms.enterprise.publishing.bundler.URLMapBundlerTest;
import com.dotcms.enterprise.publishing.remote.StaticPushPublishBundleGeneratorTest;
import com.dotcms.enterprise.publishing.remote.bundler.ContainerBundlerTest;
import com.dotcms.enterprise.publishing.remote.bundler.ContentBundlerTest;
import com.dotcms.enterprise.publishing.remote.bundler.ContentTypeBundlerTest;
import com.dotcms.enterprise.publishing.remote.bundler.FolderBundlerTest;
import com.dotcms.enterprise.publishing.remote.bundler.HostBundlerTest;
import com.dotcms.enterprise.publishing.remote.bundler.LinkBundlerTest;
import com.dotcms.enterprise.publishing.remote.bundler.TemplateBundlerTest;
import com.dotcms.enterprise.publishing.remote.bundler.WorkflowBundlerTest;
import com.dotcms.enterprise.publishing.remote.handler.ContentHandlerTest;
import com.dotcms.enterprise.publishing.remote.handler.ContentWorkflowHandlerTest;
import com.dotcms.enterprise.publishing.remote.handler.HandlerUtilTest;
import com.dotcms.enterprise.publishing.staticpublishing.AWSS3PublisherTest;
import com.dotcms.enterprise.publishing.staticpublishing.LanguageFolderTest;
import com.dotcms.experiments.business.IndexRegexUrlPatterStrategyIntegrationTest;
import com.dotcms.experiments.business.RootIndexRegexUrlPatterStrategyIntegrationTest;
import com.dotcms.filters.interceptor.meta.MetaWebInterceptorTest;
import com.dotcms.integritycheckers.ContentFileAssetIntegrityCheckerTest;
import com.dotcms.integritycheckers.ContentPageIntegrityCheckerTest;
import com.dotcms.integritycheckers.HostIntegrityCheckerTest;
import com.dotcms.integritycheckers.IntegrityUtilTest;
import com.dotcms.jobs.business.api.JobQueueManagerAPITest;
import com.dotcms.junit.MainBaseSuite;
import com.dotcms.mail.MailAPIImplTest;
import com.dotcms.publisher.bundle.business.BundleAPITest;
import com.dotcms.publisher.receiver.BundlePublisherTest;
import com.dotcms.publisher.util.DependencyManagerTest;
import com.dotcms.publishing.BundlerUtilTest;
import com.dotcms.publishing.manifest.CSVManifestBuilderTest;
import com.dotcms.publishing.manifest.CSVManifestReaderTest;
import com.dotcms.publishing.manifest.ManifestReaderFactoryTest;
import com.dotcms.publishing.manifest.ManifestUtilTest;
import com.dotcms.rendering.velocity.viewtools.DotTemplateToolTest;
import com.dotcms.rendering.velocity.viewtools.FileToolTest;
import com.dotcms.rendering.velocity.viewtools.JSONToolTest;
import com.dotcms.rendering.velocity.viewtools.MessageToolTest;
import com.dotcms.rendering.velocity.viewtools.XmlToolTest;
import com.dotcms.rendering.velocity.viewtools.content.StoryBlockMapTest;
import com.dotcms.rendering.velocity.viewtools.content.StoryBlockTest;
import com.dotcms.rest.BundlePublisherResourceIntegrationTest;
import com.dotcms.rest.IntegrityResourceIntegrationTest;
import com.dotcms.rest.api.v1.announcements.AnnouncementsHelperIntegrationTest;
import com.dotcms.rest.api.v1.announcements.RemoteAnnouncementsLoaderIntegrationTest;
import com.dotcms.rest.api.v1.apps.SiteViewPaginatorIntegrationTest;
import com.dotcms.rest.api.v1.apps.view.AppsInterpolationTest;
import com.dotcms.rest.api.v1.asset.AssetPathResolverImplIntegrationTest;
import com.dotcms.rest.api.v1.asset.WebAssetHelperIntegrationTest;
import com.dotcms.rest.api.v1.authentication.ResetPasswordTokenUtilTest;
import com.dotcms.rest.api.v1.menu.MenuResourceTest;
import com.dotcms.rest.api.v1.system.ConfigurationHelperTest;
import com.dotcms.rest.api.v1.taillog.TailLogResourceTest;
import com.dotcms.security.multipart.BoundedBufferedReaderTest;
import com.dotcms.security.multipart.ContentDispositionFileNameParserTest;
import com.dotcms.security.multipart.SecureFileValidatorTest;
import com.dotcms.storage.FileMetadataAPITest;
import com.dotcms.storage.StoragePersistenceAPITest;
import com.dotcms.storage.repository.HashedLocalFileRepositoryManagerTest;
import com.dotcms.timemachine.business.TimeMachineAPITest;
import com.dotcms.util.content.json.PopulateContentletAsJSONUtilTest;
import com.dotcms.variant.VariantAPITest;
import com.dotcms.variant.VariantFactoryTest;
import com.dotcms.variant.business.VariantCacheTest;
import com.dotmarketing.beans.HostTest;
import com.dotmarketing.business.DeterministicIdentifierAPITest;
import com.dotmarketing.business.IdentifierCacheImplTest;
import com.dotmarketing.business.PermissionBitFactoryImplTest;
import com.dotmarketing.business.VersionableFactoryImplTest;
import com.dotmarketing.business.helper.PermissionHelperTest;
import com.dotmarketing.common.db.DBTimeZoneCheckTest;
import com.dotmarketing.filters.AutoLoginFilterTest;
import com.dotmarketing.filters.CMSUrlUtilIntegrationTest;
import com.dotmarketing.osgi.GenericBundleActivatorTest;
import com.dotmarketing.portlets.browser.BrowserUtilTest;
import com.dotmarketing.portlets.browser.ajax.BrowserAjaxTest;
import com.dotmarketing.portlets.categories.business.CategoryFactoryTest;
import com.dotmarketing.portlets.contentlet.business.ContentletCacheImplTest;
import com.dotmarketing.portlets.contentlet.model.ContentletDependenciesTest;
import com.dotmarketing.portlets.folders.business.FolderFactoryImplTest;
import com.dotmarketing.portlets.htmlpages.business.render.HTMLPageAssetRenderedAPIImplIntegrationTest;
import com.dotmarketing.portlets.templates.business.FileAssetTemplateUtilTest;
import com.dotmarketing.portlets.workflows.actionlet.MoveContentActionletTest;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionletIntegrationTest;
import com.dotmarketing.portlets.workflows.actionlet.VelocityScriptActionletAbortTest;
import com.dotmarketing.quartz.DotStatefulJobTest;
import com.dotmarketing.quartz.job.DropOldContentVersionsJobTest;
import com.dotmarketing.quartz.job.IntegrityDataGenerationJobTest;
import com.dotmarketing.quartz.job.PopulateContentletAsJSONJobTest;
import com.dotmarketing.quartz.job.PruneTimeMachineBackupJobTest;
import com.dotmarketing.startup.StartupTasksExecutorDataTest;
import com.dotmarketing.startup.StartupTasksExecutorTest;
import com.dotmarketing.startup.runalways.Task00050LoadAppsSecretsTest;
import com.dotmarketing.startup.runonce.*;
import com.dotmarketing.util.ConfigUtilsTest;
import com.dotmarketing.util.ITConfigTest;
import com.dotmarketing.util.MaintenanceUtilTest;
import com.dotmarketing.util.ResourceCollectorUtilTest;
import com.dotmarketing.util.UtilMethodsITest;
import com.dotmarketing.util.contentlet.pagination.PaginatedContentletsIntegrationTest;
import org.apache.velocity.tools.view.tools.CookieToolTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/* grep -l -r "@Test" dotCMS/src/integration-test */
/* ./gradlew integrationTest -Dtest.single=com.dotcms.MainSuite */

@RunWith(MainBaseSuite.class)
@SuiteClasses({
        Task220825CreateVariantFieldTest.class,
        Task221007AddVariantIntoPrimaryKeyTest.class,
        com.dotcms.rest.api.v1.template.TemplateResourceTest.class,
        Task230630CreateRunningIdsExperimentFieldIntegrationTest.class,
        HTMLPageAssetRenderedAPIImplIntegrationTest.class,
        Task05380ChangeContainerPathToAbsoluteTest.class,
        DotTemplateToolTest.class,
        Task05370AddAppsPortletToLayoutTest.class,
        FolderFactoryImplTest.class,
        DotSamlResourceTest.class,
        DotStatefulJobTest.class,
        IntegrityDataGenerationJobTest.class,
        BundleAPITest.class,
        Task05390MakeRoomForLongerJobDetailTest.class,
        Task05395RemoveEndpointIdForeignKeyInIntegrityResolverTablesIntegrationTest.class,
        JSONToolTest.class,
        BundlePublisherResourceIntegrationTest.class,
        IntegrityResourceIntegrationTest.class,
        Task00050LoadAppsSecretsTest.class,
        StoragePersistenceAPITest.class,
        FileMetadataAPITest.class,
        StartupTasksExecutorTest.class,
        Task201013AddNewColumnsToIdentifierTableTest.class,
        Task201014UpdateColumnsValuesInIdentifierTableTest.class,
        AppsInterpolationTest.class,
        Task201102UpdateColumnSitelicTableTest.class,
        DependencyManagerTest.class,
        com.dotcms.rest.api.v1.versionable.VersionableResourceTest.class,
        GenericBundleActivatorTest.class,
        SAMLHelperTest.class,
        PermissionHelperTest.class,
        ResetPasswordTokenUtilTest.class,
        ContainerBundlerTest.class,
        ContentTypeBundlerTest.class,
        FolderBundlerTest.class,
        HostBundlerTest.class,
        LinkBundlerTest.class,
        TemplateBundlerTest.class,
        WorkflowBundlerTest.class,
        AutoLoginFilterTest.class,
        Task210218MigrateUserProxyTableTest.class,
        com.dotmarketing.startup.runonce.Task210316UpdateLayoutIconsTest.class,
        Task210319CreateStorageTableTest.class,
        Task210321RemoveOldMetadataFilesTest.class,
        DBTimeZoneCheckTest.class,
        ContentHandlerTest.class,
        ESIndexAPITest.class,
        FileAssetTemplateUtilTest.class,
        Task210506UpdateStorageTableTest.class,
        Task210520UpdateAnonymousEmailTest.class,
        Task210510UpdateStorageTableDropMetadataColumnTest.class,
        StaticPushPublishBundleGeneratorTest.class,
        CookieToolTest.class,
        CSVManifestBuilderTest.class,
        MoveContentActionletTest.class,
        DeterministicIdentifierAPITest.class,
        Task210527DropReviewFieldsFromContentletTableTest.class,
        ContentletCacheImplTest.class,
        HostTest.class,
        FileToolTest.class,
        Task210719CleanUpTitleFieldTest.class,
        Task210802UpdateStructureTableTest.class,
        MaintenanceUtilTest.class,
        BundlePublisherTest.class,
        CategoryFactoryTest.class,
        Task210805DropUserProxyTableTest.class,
        Task210816DeInodeRelationshipTest.class,
        ConfigurationHelperTest.class,
        CSVManifestReaderTest.class,
        Task210901UpdateDateTimezonesTest.class,
        DotObjectCodecTest.class,
        RedisClientTest.class,
        LettuceCacheTest.class,
        RedisPubSubImplTest.class,
        ManifestReaderFactoryTest.class,
        ResourceCollectorUtilTest.class,
        Task211007RemoveNotNullConstraintFromCompanyMXColumnTest.class,
        Task211012AddCompanyDefaultLanguageTest.class,
        HostIntegrityCheckerTest.class,
        MetaWebInterceptorTest.class,
        BrowserUtilTest.class,
        Task211101AddContentletAsJsonColumnTest.class,
        ContentletJsonAPITest.class,
        VelocityScriptActionletAbortTest.class,
        StoryBlockMapTest.class,
        HandlerUtilTest.class,
        Task211103RenameHostNameLabelTest.class,
        MessageToolTest.class,
        XmlToolTest.class,
        LanguageFolderTest.class,
        MailAPIImplTest.class,
        CSSCacheTest.class,
        com.dotcms.rendering.velocity.viewtools.content.BinaryMapTest.class,
        IntegrityUtilTest.class,
        Task220202RemoveFKStructureFolderConstraintTest.class,
        ContentBundlerTest.class,
        ObjectMapperTest.class,
        URLMapBundlerTest.class,
        PermissionBitFactoryImplTest.class,
        Task220203RemoveFolderInodeConstraintTest.class,
        Task220214AddOwnerAndIDateToFolderTableTest.class,
        Task220215MigrateDataFromInodeToFolderTest.class,
        Task220330ChangeVanityURLSiteFieldTypeTest.class,
        Task220402UpdateDateTimezonesTest.class,
        Task220413IncreasePublishedPushedAssetIdColTest.class,
        com.dotcms.util.pagination.ContainerPaginatorTest.class,
        ContentDispositionFileNameParserTest.class,
        SecureFileValidatorTest.class,
        BoundedBufferedReaderTest.class,
        ContentWorkflowHandlerTest.class,
        Task220512UpdateNoHTMLRegexValueTest.class,
        MetadataDelegateTest.class,
        Task220401CreateClusterLockTableTest.class,
        Task220606UpdatePushNowActionletNameTest.class,
        BundlerUtilTest.class,
        MenuResourceTest.class,
        AWSS3PublisherTest.class,
        ContentTypeInitializerTest.class,
        CSSPreProcessServletTest.class,
        VariantFactoryTest.class,
        VariantAPITest.class,
        PaginatedContentletsIntegrationTest.class,
        Task220824CreateDefaultVariantTest.class,
        Task220822CreateVariantTableTest.class,
        Task220829CreateExperimentsTableTest.class,
        StoryBlockTest.class,
        IdentifierCacheImplTest.class,
        VariantCacheTest.class,
        VersionableFactoryImplTest.class,
        Task220928AddLookbackWindowColumnToExperimentTest.class,
        TailLogResourceTest.class,
        BayesianAPIImplIT.class,
        ContentletDependenciesTest.class,
        SaveContentAsDraftActionletIntegrationTest.class,
        StoryBlockAPITest.class,
        UtilMethodsITest.class,
        Task220912UpdateCorrectShowOnMenuPropertyTest.class,
        HashedLocalFileRepositoryManagerTest.class,
        ManifestUtilTest.class,
        Task230110MakeSomeSystemFieldsRemovableByBaseTypeTest.class,
        BrowserAjaxTest.class,
        PopulateContentletAsJSONUtilTest.class,
        PopulateContentletAsJSONJobTest.class,
        ContentTypeDestroyAPIImplTest.class,
        Task230328AddMarkedForDeletionColumnTest.class,
        StartupTasksExecutorDataTest.class,
        Task230426AlterVarcharLengthOfLockedByColTest.class,
        AssetPathResolverImplIntegrationTest.class,
        WebAssetHelperIntegrationTest.class,
        SystemTableFactoryTest.class,
        Task230707CreateSystemTableTest.class,
        SystemAPITest.class,
        Task230701AddHashIndicesToWorkflowTablesTest.class,
        Task230713IncreaseDisabledWysiwygColumnSizeTest.class,
        ContentPageIntegrityCheckerTest.class,
        IndexRegexUrlPatterStrategyIntegrationTest.class,
        RootIndexRegexUrlPatterStrategyIntegrationTest.class,
        SiteViewPaginatorIntegrationTest.class,
        Task230523CreateVariantFieldInContentletIntegrationTest.class,
        DropOldContentVersionsJobTest.class,
        Task231109AddPublishDateToContentletVersionInfoTest.class,
        Task240102AlterVarcharLengthOfRelationTypeTest.class,
        Task240111AddInodeAndIdentifierLeftIndexesTest.class,
        AnnouncementsHelperIntegrationTest.class,
        RemoteAnnouncementsLoaderIntegrationTest.class,
        Task240112AddMetadataColumnToStructureTableTest.class,
        AIViewToolTest.class,
        SearchToolTest.class,
        EmbeddingsToolTest.class,
        CompletionsToolTest.class,
        AIModelsTest.class,
        ConfigServiceTest.class,
        AIProxyClientTest.class,
        AIAppValidatorTest.class,
        TimeMachineAPITest.class,
        Task240513UpdateContentTypesSystemFieldTest.class,
        PruneTimeMachineBackupJobTest.class,
        CMSUrlUtilIntegrationTest.class,
        ContentFileAssetIntegrityCheckerTest.class,
        ITConfigTest.class,
        Task240530AddDotAIPortletToLayoutTest.class,
        EmbeddingContentListenerTest.class,
        Task240606AddVariableColumnToWorkflowTest.class,
        OpenAIContentPromptActionletTest.class,
        JobQueueManagerAPITest.class,
        ConfigUtilsTest.class,
        SimpleInjectionIT.class,
        LegacyJSONObjectRenderTest.class
})

public class MainSuite2b {

}
