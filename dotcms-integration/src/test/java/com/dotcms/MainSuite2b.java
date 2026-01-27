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
import com.dotcms.ai.workflow.OpenAIAutoTagActionletTest;
import com.dotcms.ai.workflow.OpenAIContentPromptActionletTest;
import com.dotcms.analytics.attributes.CustomAttributeAPIImplTest;
import com.dotcms.analytics.attributes.CustomAttributeFactoryTest;
import com.dotcms.analytics.bayesian.BayesianAPIImplIT;
import com.dotcms.analytics.track.collectors.AsyncVanitiesCollectorTest;
import com.dotcms.analytics.track.collectors.BasicProfileCollectorTest;
import com.dotcms.analytics.track.collectors.FilesCollectorTest;
import com.dotcms.analytics.track.collectors.PageDetailCollectorTest;
import com.dotcms.analytics.track.collectors.PagesCollectorTest;
import com.dotcms.analytics.track.collectors.SyncVanitiesCollectorTest;
import com.dotcms.analytics.track.collectors.WebEventsCollectorServiceImplTest;
import com.dotcms.api.web.HttpServletRequestImpersonatorTest;
import com.dotcms.auth.providers.saml.v1.DotSamlResourceTest;
import com.dotcms.auth.providers.saml.v1.SAMLHelperTest;
import com.dotcms.business.SystemAPITest;
import com.dotcms.business.SystemTableFactoryTest;
import com.dotcms.cache.lettuce.DotObjectCodecTest;
import com.dotcms.cache.lettuce.LettuceCacheTest;
import com.dotcms.cache.lettuce.RedisClientTest;
import com.dotcms.cdi.SimpleDataProviderWeldRunnerInjectionIT;
import com.dotcms.cdi.SimpleInjectionIT;
import com.dotcms.cdi.SimpleJUnit4InjectionIT;
import com.dotcms.content.business.ObjectMapperTest;
import com.dotcms.content.business.json.ContentletJsonAPITest;
import com.dotcms.content.business.json.LegacyJSONObjectRenderTest;
import com.dotcms.content.elasticsearch.business.ESIndexAPITest;
import com.dotcms.content.elasticsearch.util.ESMappingUtilHelperTest;
import com.dotcms.content.model.hydration.MetadataDelegateTest;
import com.dotcms.contenttype.business.ContentTypeDestroyAPIImplTest;
import com.dotcms.contenttype.business.ContentTypeInitializerTest;
import com.dotcms.contenttype.business.DotAssetBaseTypeToContentTypeStrategyImplTest;
import com.dotcms.contenttype.business.StoryBlockAPITest;
import com.dotcms.contenttype.business.uniquefields.extratable.DBUniqueFieldValidationStrategyTest;
import com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldDataBaseUtilTest;
import com.dotcms.contenttype.test.DotAssetAPITest;
import com.dotcms.csspreproc.CSSCacheTest;
import com.dotcms.csspreproc.CSSPreProcessServletTest;
import com.dotcms.dotpubsub.PostgresPubSubImplTest;
import com.dotcms.dotpubsub.RedisPubSubImplTest;
import com.dotcms.ema.EMAWebInterceptorTest;
import com.dotcms.enterprise.cluster.ClusterFactoryTest;
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
import com.dotcms.mock.request.CachedParameterDecoratorTest;
import com.dotcms.publisher.bundle.business.BundleAPITest;
import com.dotcms.publisher.bundle.business.BundleFactoryTest;
import com.dotcms.publisher.business.PublishAuditAPITest;
import com.dotcms.publisher.receiver.BundlePublisherTest;
import com.dotcms.publisher.util.DependencyManagerTest;
import com.dotcms.publisher.util.PushedAssetUtilTest;
import com.dotcms.publishing.BundlerUtilTest;
import com.dotcms.publishing.PublisherFilterImplTest;
import com.dotcms.publishing.PushPublishFiltersInitializerTest;
import com.dotcms.publishing.manifest.CSVManifestBuilderTest;
import com.dotcms.publishing.manifest.CSVManifestReaderTest;
import com.dotcms.publishing.manifest.ManifestReaderFactoryTest;
import com.dotcms.publishing.manifest.ManifestUtilTest;
import com.dotcms.rendering.velocity.directive.DotParseTest;
import com.dotcms.rendering.velocity.servlet.VelocityServletIntegrationTest;
import com.dotcms.rendering.velocity.viewtools.DotTemplateToolTest;
import com.dotcms.rendering.velocity.viewtools.FileToolTest;
import com.dotcms.rendering.velocity.viewtools.JSONToolTest;
import com.dotcms.rendering.velocity.viewtools.MessageToolTest;
import com.dotcms.rendering.velocity.viewtools.XmlToolTest;
import com.dotcms.rendering.velocity.viewtools.content.StoryBlockMapTest;
import com.dotcms.rendering.velocity.viewtools.content.StoryBlockTest;
import com.dotcms.rest.BundleResourceTest;
import com.dotcms.rest.api.v1.announcements.AnnouncementsHelperIntegrationTest;
import com.dotcms.rest.api.v1.announcements.RemoteAnnouncementsLoaderIntegrationTest;
import com.dotcms.rest.api.v1.apps.AppsResourceTest;
import com.dotcms.rest.api.v1.apps.SiteViewPaginatorIntegrationTest;
import com.dotcms.rest.api.v1.apps.view.AppsInterpolationTest;
import com.dotcms.rest.api.v1.asset.AssetPathResolverImplIntegrationTest;
import com.dotcms.rest.api.v1.asset.WebAssetHelperIntegrationTest;
import com.dotcms.rest.api.v1.authentication.ResetPasswordTokenUtilTest;
import com.dotcms.rest.api.v1.folder.FolderResourceTest;
import com.dotcms.rest.api.v1.menu.MenuResourceTest;
import com.dotcms.rest.api.v1.publishing.PublishingResourceIntegrationTest;
import com.dotcms.rest.api.v1.pushpublish.PushPublishFilterResourceTest;
import com.dotcms.rest.api.v1.system.ConfigurationHelperTest;
import com.dotcms.rest.api.v1.system.permission.PermissionResourceIntegrationTest;
import com.dotcms.rest.api.v1.taillog.TailLogResourceTest;
import com.dotcms.rest.api.v1.user.UserResourceIntegrationTest;
import com.dotcms.saml.IdentityProviderConfigurationFactoryTest;
import com.dotcms.saml.SamlConfigurationServiceTest;
import com.dotcms.security.apps.AppsCacheImplTest;
import com.dotcms.security.multipart.BoundedBufferedReaderTest;
import com.dotcms.security.multipart.ContentDispositionFileNameParserTest;
import com.dotcms.security.multipart.SecureFileValidatorTest;
import com.dotcms.storage.FileMetadataAPITest;
import com.dotcms.storage.StoragePersistenceAPITest;
import com.dotcms.storage.repository.HashedLocalFileRepositoryManagerTest;
import com.dotcms.timemachine.business.TimeMachineAPITest;
import com.dotcms.translate.GoogleTranslationServiceIntegrationTest;
import com.dotcms.util.content.json.PopulateContentletAsJSONUtilTest;
import com.dotcms.variant.VariantAPITest;
import com.dotcms.variant.VariantFactoryTest;
import com.dotcms.variant.business.VariantCacheTest;
import com.dotmarketing.beans.HostTest;
import com.dotmarketing.business.IdentifierCacheImplTest;
import com.dotmarketing.business.PermissionBitFactoryImplTest;
import com.dotmarketing.business.VersionableFactoryImplTest;
import com.dotmarketing.business.helper.PermissionHelperTest;
import com.dotmarketing.common.db.DBTimeZoneCheckTest;
import com.dotmarketing.filters.AutoLoginFilterTest;
import com.dotmarketing.filters.CMSUrlUtilIntegrationTest;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.osgi.GenericBundleActivatorIntegrationTest;
import com.dotmarketing.portlets.browser.BrowserUtilTest;
import com.dotmarketing.portlets.browser.ajax.BrowserAjaxTest;
import com.dotmarketing.portlets.categories.business.CategoryFactoryTest;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactoryTest;
import com.dotmarketing.portlets.containers.business.ContainerFactoryImplTest;
import com.dotmarketing.portlets.contentlet.business.ContentletCacheImplTest;
import com.dotmarketing.portlets.contentlet.model.ContentletDependenciesTest;
import com.dotmarketing.portlets.contentlet.model.IntegrationResourceLinkTest;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPIImplIntegrationTest;
import com.dotmarketing.portlets.fileassets.business.FileAssetFactoryIntegrationTest;
import com.dotmarketing.portlets.folders.business.FolderFactoryImplTest;
import com.dotmarketing.portlets.folders.model.FolderTest;
import com.dotmarketing.portlets.htmlpages.business.render.HTMLPageAssetRenderedAPIImplIntegrationTest;
import com.dotmarketing.portlets.templates.business.FileAssetTemplateUtilTest;
import com.dotmarketing.portlets.templates.business.TemplateFactoryImplTest;
import com.dotmarketing.portlets.workflows.actionlet.MoveContentActionletTest;
import com.dotmarketing.portlets.workflows.actionlet.PushNowActionletTest;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionletIntegrationTest;
import com.dotmarketing.portlets.workflows.actionlet.VelocityScriptActionletAbortTest;
import com.dotmarketing.portlets.workflows.model.TestWorkflowAction;
import com.dotmarketing.quartz.DotStatefulJobTest;
import com.dotmarketing.quartz.job.CleanUpFieldReferencesJobTest;
import com.dotmarketing.quartz.job.DropOldContentVersionsJobTest;
import com.dotmarketing.quartz.job.IntegrityDataGenerationJobTest;
import com.dotmarketing.quartz.job.PopulateContentletAsJSONJobTest;
import com.dotmarketing.quartz.job.PruneTimeMachineBackupJobTest;
import com.dotmarketing.startup.StartupTasksExecutorDataTest;
import com.dotmarketing.startup.StartupTasksExecutorTest;
import com.dotmarketing.startup.runalways.Task00050LoadAppsSecretsTest;
import com.dotmarketing.startup.runonce.Task05195CreatesDestroyActionAndAssignDestroyDefaultActionsToTheSystemWorkflowTest;
import com.dotmarketing.startup.runonce.Task05210CreateDefaultDotAssetTest;
import com.dotmarketing.startup.runonce.Task05225RemoveLoadRecordsToIndexTest;
import com.dotmarketing.startup.runonce.Task05305AddPushPublishFilterColumnTest;
import com.dotmarketing.startup.runonce.Task05350AddDotSaltClusterColumnTest;
import com.dotmarketing.startup.runonce.Task05370AddAppsPortletToLayoutTest;
import com.dotmarketing.startup.runonce.Task05380ChangeContainerPathToAbsoluteTest;
import com.dotmarketing.startup.runonce.Task05390MakeRoomForLongerJobDetailTest;
import com.dotmarketing.startup.runonce.Task05395RemoveEndpointIdForeignKeyInIntegrityResolverTablesIntegrationTest;
import com.dotmarketing.startup.runonce.Task201013AddNewColumnsToIdentifierTableTest;
import com.dotmarketing.startup.runonce.Task201014UpdateColumnsValuesInIdentifierTableTest;
import com.dotmarketing.startup.runonce.Task201102UpdateColumnSitelicTableTest;
import com.dotmarketing.startup.runonce.Task210218MigrateUserProxyTableTest;
import com.dotmarketing.startup.runonce.Task210319CreateStorageTableTest;
import com.dotmarketing.startup.runonce.Task210321RemoveOldMetadataFilesTest;
import com.dotmarketing.startup.runonce.Task210506UpdateStorageTableTest;
import com.dotmarketing.startup.runonce.Task210510UpdateStorageTableDropMetadataColumnTest;
import com.dotmarketing.startup.runonce.Task210520UpdateAnonymousEmailTest;
import com.dotmarketing.startup.runonce.Task210527DropReviewFieldsFromContentletTableTest;
import com.dotmarketing.startup.runonce.Task210719CleanUpTitleFieldTest;
import com.dotmarketing.startup.runonce.Task210802UpdateStructureTableTest;
import com.dotmarketing.startup.runonce.Task210805DropUserProxyTableTest;
import com.dotmarketing.startup.runonce.Task210816DeInodeRelationshipTest;
import com.dotmarketing.startup.runonce.Task210901UpdateDateTimezonesTest;
import com.dotmarketing.startup.runonce.Task211007RemoveNotNullConstraintFromCompanyMXColumnTest;
import com.dotmarketing.startup.runonce.Task211012AddCompanyDefaultLanguageTest;
import com.dotmarketing.startup.runonce.Task211101AddContentletAsJsonColumnTest;
import com.dotmarketing.startup.runonce.Task211103RenameHostNameLabelTest;
import com.dotmarketing.startup.runonce.Task220202RemoveFKStructureFolderConstraintTest;
import com.dotmarketing.startup.runonce.Task220203RemoveFolderInodeConstraintTest;
import com.dotmarketing.startup.runonce.Task220214AddOwnerAndIDateToFolderTableTest;
import com.dotmarketing.startup.runonce.Task220215MigrateDataFromInodeToFolderTest;
import com.dotmarketing.startup.runonce.Task220330ChangeVanityURLSiteFieldTypeTest;
import com.dotmarketing.startup.runonce.Task220401CreateClusterLockTableTest;
import com.dotmarketing.startup.runonce.Task220402UpdateDateTimezonesTest;
import com.dotmarketing.startup.runonce.Task220413IncreasePublishedPushedAssetIdColTest;
import com.dotmarketing.startup.runonce.Task220512UpdateNoHTMLRegexValueTest;
import com.dotmarketing.startup.runonce.Task220606UpdatePushNowActionletNameTest;
import com.dotmarketing.startup.runonce.Task220822CreateVariantTableTest;
import com.dotmarketing.startup.runonce.Task220824CreateDefaultVariantTest;
import com.dotmarketing.startup.runonce.Task220825CreateVariantFieldTest;
import com.dotmarketing.startup.runonce.Task220829CreateExperimentsTableTest;
import com.dotmarketing.startup.runonce.Task220912UpdateCorrectShowOnMenuPropertyTest;
import com.dotmarketing.startup.runonce.Task220928AddLookbackWindowColumnToExperimentTest;
import com.dotmarketing.startup.runonce.Task221007AddVariantIntoPrimaryKeyTest;
import com.dotmarketing.startup.runonce.Task230110MakeSomeSystemFieldsRemovableByBaseTypeTest;
import com.dotmarketing.startup.runonce.Task230328AddMarkedForDeletionColumnTest;
import com.dotmarketing.startup.runonce.Task230426AlterVarcharLengthOfLockedByColTest;
import com.dotmarketing.startup.runonce.Task230523CreateVariantFieldInContentletIntegrationTest;
import com.dotmarketing.startup.runonce.Task230701AddHashIndicesToWorkflowTablesTest;
import com.dotmarketing.startup.runonce.Task230707CreateSystemTableTest;
import com.dotmarketing.startup.runonce.Task230713IncreaseDisabledWysiwygColumnSizeTest;
import com.dotmarketing.startup.runonce.Task231109AddPublishDateToContentletVersionInfoTest;
import com.dotmarketing.startup.runonce.Task240102AlterVarcharLengthOfRelationTypeTest;
import com.dotmarketing.startup.runonce.Task240111AddInodeAndIdentifierLeftIndexesTest;
import com.dotmarketing.startup.runonce.Task240112AddMetadataColumnToStructureTableTest;
import com.dotmarketing.startup.runonce.Task240131UpdateLanguageVariableContentTypeTest;
import com.dotmarketing.startup.runonce.Task240513UpdateContentTypesSystemFieldTest;
import com.dotmarketing.startup.runonce.Task240530AddDotAIPortletToLayoutTest;
import com.dotmarketing.startup.runonce.Task240606AddVariableColumnToWorkflowTest;
import com.dotmarketing.startup.runonce.Task241013RemoveFullPathLcColumnFromIdentifierTest;
import com.dotmarketing.startup.runonce.Task241015ReplaceLanguagesWithLocalesPortletTest;
import com.dotmarketing.startup.runonce.Task241016AddCustomLanguageVariablesPortletToLayoutTest;
import com.dotmarketing.startup.runonce.Task250107RemoveEsReadOnlyMonitorJobTest;
import com.dotmarketing.startup.runonce.Task250113CreatePostgresJobQueueTablesTest;
import com.dotmarketing.startup.runonce.Task250828CreateCustomAttributeTableTest;
import com.dotmarketing.util.ConfigUtilsTest;
import com.dotmarketing.util.HashBuilderTest;
import com.dotmarketing.util.ITConfigTest;
import com.dotmarketing.util.MaintenanceUtilTest;
import com.dotmarketing.util.ResourceCollectorUtilTest;
import com.dotmarketing.util.TestConfig;
import com.dotmarketing.util.UtilMethodsITest;
import com.dotmarketing.util.contentlet.pagination.PaginatedContentletsIntegrationTest;
import com.liferay.portal.language.LanguageUtilTest;
import org.apache.felix.framework.OSGIUtilTest;
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
        GenericBundleActivatorIntegrationTest.class,
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
        SimpleDataProviderWeldRunnerInjectionIT.class,
        SimpleJUnit4InjectionIT.class,
        LegacyJSONObjectRenderTest.class,
        Task241013RemoveFullPathLcColumnFromIdentifierTest.class,
        Task250113CreatePostgresJobQueueTablesTest.class,
        UniqueFieldDataBaseUtilTest.class,
        DBUniqueFieldValidationStrategyTest.class,
        Task241013RemoveFullPathLcColumnFromIdentifierTest.class,
        Task241013RemoveFullPathLcColumnFromIdentifierTest.class,
        Task241015ReplaceLanguagesWithLocalesPortletTest.class,
        Task241016AddCustomLanguageVariablesPortletToLayoutTest.class,
        WebEventsCollectorServiceImplTest.class,
        BasicProfileCollectorTest.class,
        PagesCollectorTest.class,
        PageDetailCollectorTest.class,
        FilesCollectorTest.class,
        SyncVanitiesCollectorTest.class,
        AsyncVanitiesCollectorTest.class,
        HttpServletRequestImpersonatorTest.class,
        Task250107RemoveEsReadOnlyMonitorJobTest.class,
        com.dotmarketing.business.VersionableAPITest.class,
        com.dotmarketing.business.UserAPITest.class,
        com.dotmarketing.business.portal.PortletAPIImplTest.class,
        com.dotmarketing.business.web.LanguageWebApiTest.class,
        com.dotmarketing.business.IdentifierFactoryTest.class,
        com.dotmarketing.business.IdentifierAPITest.class,
        com.dotmarketing.business.CommitListenerCacheWrapperTest.class,
        com.dotmarketing.business.RoleAPITest.class,
        com.dotmarketing.business.IdentifierConsistencyIntegrationTest.class,
        com.dotmarketing.business.LayoutAPITest.class,
        com.dotmarketing.business.PermissionAPIIntegrationTest.class,
        com.dotmarketing.business.PermissionAPITest.class,
        com.dotmarketing.servlets.BinaryExporterServletTest.class,
        com.dotmarketing.servlets.ShortyServletAndTitleImageTest.class,
        com.dotmarketing.servlets.ajax.AjaxDirectorServletIntegrationTest.class,
        FocalPointAPITest.class,
        com.dotmarketing.tag.business.TagAPITest.class,
        OSGIUtilTest.class,
        CleanUpFieldReferencesJobTest.class,
        CachedParameterDecoratorTest.class,
        ContainerFactoryImplTest.class,
        TemplateFactoryImplTest.class,
        TestConfig.class,
        FolderTest.class,
        PublishAuditAPITest.class,
        BundleFactoryTest.class,
        com.dotcms.security.apps.SecretsStoreKeyStoreImplTest.class,
        AppsResourceTest.class,
        AppsCacheImplTest.class,
        VelocityServletIntegrationTest.class,
        DotAssetAPITest.class,
        DotAssetBaseTypeToContentTypeStrategyImplTest.class,
        FileAssetAPIImplIntegrationTest.class,
        FileAssetFactoryIntegrationTest.class,
        UserResourceIntegrationTest.class,
        IntegrationResourceLinkTest.class,
        HashBuilderTest.class,
        LanguageUtilTest.class,
        FolderResourceTest.class,
        com.dotmarketing.common.reindex.ReindexThreadTest.class,
        com.dotmarketing.common.reindex.ReindexAPITest.class,
        com.dotmarketing.common.db.DotDatabaseMetaDataTest.class,
        com.dotmarketing.common.db.ParamsSetterTest.class,
        com.dotmarketing.cms.urlmap.URLMapAPIImplTest.class,
        com.dotmarketing.factories.PublishFactoryTest.class,
        com.dotmarketing.factories.WebAssetFactoryTest.class,
        com.dotmarketing.db.DbConnectionFactoryTest.class,
        com.dotmarketing.db.DbConnectionFactoryUtilTest.class,
        com.dotmarketing.db.HibernateUtilTest.class,
        com.dotmarketing.quartz.job.BinaryCleanupJobTest.class,

        com.dotmarketing.fixTasks.FixTask00085FixEmptyParentPathOnIdentifierTest.class,
        com.dotmarketing.startup.runonce.Task05170DefineFrontEndAndBackEndRolesTest.class,
        com.dotmarketing.startup.runonce.Task04375UpdateCategoryKeyTest.class,
        com.dotmarketing.startup.runonce.Task04335CreateSystemWorkflowTest.class,
        com.dotmarketing.startup.runonce.Task04375UpdateColorsTest.class,
        com.dotmarketing.startup.runonce.Task05160MultiTreeAddPersonalizationColumnAndChangingPKTest.class,
        com.dotmarketing.startup.runonce.Task05035LanguageTableIdentityOffTest.class,
        com.dotmarketing.startup.runonce.Task05165CreateContentTypeWorkflowActionMappingTableTest.class,
        com.dotmarketing.startup.runonce.Task05070AndTask05080Test.class,
        com.dotmarketing.startup.runonce.Task05030UpdateSystemContentTypesHostTest.class,
        com.dotmarketing.startup.runonce.Task05050FileAssetContentTypeReadOnlyFileNameTest.class,
        com.dotmarketing.startup.runonce.Task05190UpdateFormsWidgetCodeFieldTest.class,
        com.dotmarketing.startup.runalways.Task00001LoadSchemaIntegrationTest.class,
        com.dotmarketing.startup.runonce.Task05200WorkflowTaskUniqueKeyTest.class,
        Task05195CreatesDestroyActionAndAssignDestroyDefaultActionsToTheSystemWorkflowTest.class,
        Task05210CreateDefaultDotAssetTest.class,
        DotAssetAPITest.class,
        DotAssetBaseTypeToContentTypeStrategyImplTest.class,
        FileAssetAPIImplIntegrationTest.class,
        FileAssetFactoryIntegrationTest.class,
        UserResourceIntegrationTest.class,
        IntegrationResourceLinkTest.class,
        HashBuilderTest.class,
        LanguageUtilTest.class,
        FolderResourceTest.class,
        Task05225RemoveLoadRecordsToIndexTest.class,
        PublisherFilterImplTest.class,
        PushPublishFiltersInitializerTest.class,
        PushPublishFilterResourceTest.class,
        PublishingResourceIntegrationTest.class,
        PushNowActionletTest.class,
        Task05305AddPushPublishFilterColumnTest.class,
        CMSMaintenanceFactoryTest.class,
        Task05350AddDotSaltClusterColumnTest.class,
        PostgresPubSubImplTest.class,
        DotParseTest.class,
        TestWorkflowAction.class,
        SamlConfigurationServiceTest.class,
        ClusterFactoryTest.class,
        ESMappingUtilHelperTest.class,
        BundleResourceTest.class,
        IdentityProviderConfigurationFactoryTest.class,
        EMAWebInterceptorTest.class,
        GoogleTranslationServiceIntegrationTest.class,
        Task240131UpdateLanguageVariableContentTypeTest.class,
        PushedAssetUtilTest.class,
        OpenAIAutoTagActionletTest.class,
        Task250828CreateCustomAttributeTableTest.class,
        CustomAttributeAPIImplTest.class,
        CustomAttributeFactoryTest.class,
        PermissionResourceIntegrationTest.class,
})

public class MainSuite2b {
}
