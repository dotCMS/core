package com.dotcms;

import com.dotcms.analytics.bayesian.BayesianAPIImplTest;
import com.dotcms.auth.providers.saml.v1.DotSamlResourceTest;
import com.dotcms.auth.providers.saml.v1.SAMLHelperTest;
import com.dotcms.cache.lettuce.DotObjectCodecTest;
import com.dotcms.cache.lettuce.LettuceCacheTest;
import com.dotcms.cache.lettuce.RedisClientTest;
import com.dotcms.content.business.ObjectMapperTest;
import com.dotcms.content.business.json.ContentletJsonAPITest;
import com.dotcms.content.elasticsearch.business.ESIndexAPITest;
import com.dotcms.content.elasticsearch.business.ElasticsearchUtilTest;
import com.dotcms.content.elasticsearch.util.ESMappingUtilHelperTest;
import com.dotcms.content.model.hydration.MetadataDelegateTest;
import com.dotcms.contenttype.business.ContentTypeDestroyAPIImplTest;
import com.dotcms.contenttype.business.ContentTypeInitializerTest;
import com.dotcms.contenttype.business.DotAssetBaseTypeToContentTypeStrategyImplTest;
import com.dotcms.contenttype.business.StoryBlockAPITest;
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
import com.dotcms.filters.interceptor.meta.MetaWebInterceptorTest;
import com.dotcms.integritycheckers.HostIntegrityCheckerTest;
import com.dotcms.integritycheckers.IntegrityUtilTest;
import com.dotcms.junit.MainBaseSuite;
import com.dotcms.mail.MailAPIImplTest;
import com.dotcms.mock.request.CachedParameterDecoratorTest;
import com.dotcms.publisher.bundle.business.BundleAPITest;
import com.dotcms.publisher.bundle.business.BundleFactoryTest;
import com.dotcms.publisher.business.PublishAuditAPITest;
import com.dotcms.publisher.receiver.BundlePublisherTest;
import com.dotcms.publisher.util.DependencyManagerTest;
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
import com.dotcms.rest.BundlePublisherResourceIntegrationTest;
import com.dotcms.rest.BundleResourceTest;
import com.dotcms.rest.IntegrityResourceIntegrationTest;
import com.dotcms.rest.api.v1.apps.AppsResourceTest;
import com.dotcms.rest.api.v1.apps.view.AppsInterpolationTest;
import com.dotcms.rest.api.v1.assets.AssetPathResolverImplTest;
import com.dotcms.rest.api.v1.authentication.ResetPasswordTokenUtilTest;
import com.dotcms.rest.api.v1.folder.FolderResourceTest;
import com.dotcms.rest.api.v1.menu.MenuResourceTest;
import com.dotcms.rest.api.v1.pushpublish.PushPublishFilterResourceTest;
import com.dotcms.rest.api.v1.system.ConfigurationHelperTest;
import com.dotcms.rest.api.v1.taillog.TailLogResourceTest;
import com.dotcms.rest.api.v1.user.UserResourceIntegrationTest;
import com.dotcms.saml.IdentityProviderConfigurationFactoryTest;
import com.dotcms.saml.SamlConfigurationServiceTest;
import com.dotcms.security.ContentSecurityPolicyUtilTest;
import com.dotcms.security.apps.AppsAPIImplTest;
import com.dotcms.security.apps.AppsCacheImplTest;
import com.dotcms.security.multipart.BoundedBufferedReaderTest;
import com.dotcms.security.multipart.ContentDispositionFileNameParserTest;
import com.dotcms.security.multipart.SecureFileValidatorTest;
import com.dotcms.storage.FileMetadataAPITest;
import com.dotcms.storage.StoragePersistenceAPITest;
import com.dotcms.storage.repository.HashedLocalFileRepositoryManagerTest;
import com.dotcms.translate.GoogleTranslationServiceIntegrationTest;
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
import com.dotmarketing.filters.CMSUrlUtilTest;
import com.dotmarketing.image.filter.ImageFilterAPIImplTest;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.osgi.GenericBundleActivatorTest;
import com.dotmarketing.portlets.browser.BrowserUtilTest;
import com.dotmarketing.portlets.browser.ajax.BrowserAjaxTest;
import com.dotmarketing.portlets.categories.business.CategoryFactoryTest;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactoryTest;
import com.dotmarketing.portlets.containers.business.ContainerFactoryImplTest;
import com.dotmarketing.portlets.containers.business.ContainerStructureFinderStrategyResolverTest;
import com.dotmarketing.portlets.contentlet.business.ContentletCacheImplTest;
import com.dotmarketing.portlets.contentlet.model.ContentletDependenciesTest;
import com.dotmarketing.portlets.contentlet.model.IntegrationResourceLinkTest;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPIImplIntegrationTest;
import com.dotmarketing.portlets.fileassets.business.FileAssetFactoryIntegrationTest;
import com.dotmarketing.portlets.folders.business.FolderFactoryImplTest;
import com.dotmarketing.portlets.folders.model.FolderTest;
import com.dotmarketing.portlets.templates.business.FileAssetTemplateUtilTest;
import com.dotmarketing.portlets.templates.business.TemplateFactoryImplTest;
import com.dotmarketing.portlets.workflows.actionlet.MoveContentActionletTest;
import com.dotmarketing.portlets.workflows.actionlet.PushNowActionletTest;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionletIntegrationTest;
import com.dotmarketing.portlets.workflows.actionlet.VelocityScriptActionletAbortTest;
import com.dotmarketing.portlets.workflows.model.TestWorkflowAction;
import com.dotmarketing.portlets.workflows.util.WorkflowEmailUtilTest;
import com.dotmarketing.quartz.DotStatefulJobTest;
import com.dotmarketing.quartz.job.CleanUpFieldReferencesJobTest;
import com.dotmarketing.quartz.job.IntegrityDataGenerationJobTest;
import com.dotmarketing.quartz.job.PopulateContentletAsJSONJobTest;
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
import com.dotmarketing.startup.runonce.Task220825MakeSomeSystemFieldsRemovableTest;
import com.dotmarketing.startup.runonce.Task220829CreateExperimentsTableTest;
import com.dotmarketing.startup.runonce.Task220912UpdateCorrectShowOnMenuPropertyTest;
import com.dotmarketing.startup.runonce.Task220928AddLookbackWindowColumnToExperimentTest;
import com.dotmarketing.startup.runonce.Task230110MakeSomeSystemFieldsRemovableByBaseTypeTest;
import com.dotmarketing.startup.runonce.Task230328AddMarkedForDeletionColumnTest;
import com.dotmarketing.startup.runonce.Task230426AlterVarcharLengthOfLockedByColTest;
import com.dotmarketing.util.ConfigTest;
import com.dotmarketing.util.HashBuilderTest;
import com.dotmarketing.util.MaintenanceUtilTest;
import com.dotmarketing.util.ResourceCollectorUtilTest;
import com.dotmarketing.util.TestConfig;
import com.dotmarketing.util.UtilMethodsITest;
import com.dotmarketing.util.ZipUtilTest;
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
        com.dotcms.rest.api.v1.workflow.WorkflowResourceResponseCodeIntegrationTest.class,
        com.dotcms.rest.api.v1.workflow.WorkflowResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.workflow.WorkflowResourceLicenseIntegrationTest.class,
        com.dotcms.rest.api.v1.authentication.ResetPasswordResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.authentication.CreateJsonWebTokenResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.relationships.RelationshipsResourceTest.class,
        com.dotcms.rest.api.v2.contenttype.FieldResourceTest.class,
        com.dotcms.rest.api.v3.contenttype.FieldResourceTest.class,
        com.dotcms.rest.api.v3.contenttype.MoveFieldFormTest.class,
        com.dotcms.rest.api.CorsFilterTest.class,
        com.dotcms.rest.elasticsearch.ESContentResourcePortletTest.class,
        com.dotcms.filters.VanityUrlFilterTest.class,
        com.dotcms.vanityurl.business.VanityUrlAPITest.class,
        com.dotmarketing.portlets.fileassets.business.FileAssetAPITest.class,
        com.dotmarketing.portlets.languagesmanager.business.LanguageAPITest.class,
        com.dotmarketing.portlets.languagesmanager.business.LanguageFactoryIntegrationTest.class,
        com.dotmarketing.portlets.linkchecker.business.LinkCheckerAPITest.class,
        com.dotmarketing.portlets.contentlet.util.ContentletUtilTest.class,
        com.dotmarketing.portlets.contentlet.business.ContentletCheckInTest.class,
        com.dotmarketing.portlets.contentlet.business.ContentletFactoryTest.class,
        ContainerStructureFinderStrategyResolverTest.class,
        com.dotmarketing.portlets.contentlet.business.ContentletAPITest.class,
        com.dotmarketing.portlets.contentlet.model.ContentletIntegrationTest.class,
        com.dotmarketing.portlets.contentlet.transform.BinaryToMapTransformerTest.class,
        com.dotmarketing.portlets.contentlet.transform.ContentletTransformerTest.class,
        com.dotmarketing.portlets.contentlet.ajax.ContentletAjaxTest.class,
        com.dotmarketing.portlets.workflows.business.SaveContentDraftActionletTest.class,
        com.dotmarketing.portlets.workflows.business.WorkflowFactoryTest.class,
        com.dotmarketing.portlets.workflows.business.SaveContentActionletTest.class,
        com.dotmarketing.portlets.workflows.business.WorkflowAPIMultiLanguageTest.class,
        com.dotmarketing.portlets.workflows.business.WorkflowAPITest.class,
        com.dotmarketing.portlets.workflows.model.WorkflowSearcherTest.class,
        com.dotmarketing.portlets.workflows.model.SystemActionWorkflowActionMappingTest.class,
        com.dotmarketing.portlets.workflows.actionlet.FourEyeApproverActionletTest.class,
        com.dotmarketing.portlets.workflows.actionlet.SaveContentActionletTest.class,
        com.dotmarketing.portlets.workflows.actionlet.SaveContentActionletWithTagsTest.class,
        com.dotmarketing.portlets.workflows.actionlet.CopyActionletTest.class,
        com.dotmarketing.portlets.workflows.actionlet.VelocityScriptActionletTest.class,
        com.dotmarketing.portlets.personas.business.PersonaAPITest.class,
        com.dotmarketing.portlets.personas.business.DeleteMultiTreeUsedPersonaTagJobTest.class,
        com.dotmarketing.portlets.links.business.MenuLinkAPITest.class,
        com.dotmarketing.portlets.links.factories.LinkFactoryTest.class,
        com.dotmarketing.portlets.rules.conditionlet.ConditionletOSGIFTest.class,
        com.dotmarketing.portlets.rules.conditionlet.CurrentSessionLanguageConditionletTest.class,
        com.dotmarketing.portlets.rules.conditionlet.NumberOfTimesPreviouslyVisitedConditionletTest.class,
        com.dotmarketing.portlets.rules.conditionlet.UsersBrowserLanguageConditionletTest.class,
        com.dotmarketing.portlets.rules.conditionlet.UsersSiteVisitsConditionletTest.class,
        com.dotmarketing.portlets.rules.conditionlet.VisitorOperatingSystemConditionletTest.class,
        com.dotmarketing.portlets.rules.conditionlet.VisitedUrlConditionletTest.class,
        com.dotmarketing.portlets.rules.business.RulesCacheFTest.class,
        com.dotmarketing.portlets.templates.business.TemplateAPITest.class,
        com.dotmarketing.portlets.containers.business.ContainerAPIImplTest.class,
        com.dotmarketing.portlets.folders.business.FolderAPITest.class,
        com.dotmarketing.portlets.containers.business.ContainerAPITest.class,
        com.dotmarketing.portlets.containers.business.FileAssetContainerUtilTest.class,
        com.dotmarketing.portlets.htmlpages.business.HTMLPageAPITest.class,
        com.dotmarketing.portlets.structure.factories.StructureFactoryTest.class,
        com.dotmarketing.portlets.structure.factories.FieldFactoryTest.class,
        com.dotmarketing.portlets.structure.model.ContentletRelationshipsTest.class,
        com.dotmarketing.portlets.structure.transform.ContentletRelationshipsTransformerTest.class,
        com.dotmarketing.portlets.categories.business.CategoryAPITest.class,
        com.dotmarketing.filters.FiltersTest.class,
        com.dotmarketing.business.VersionableAPITest.class,
        com.dotmarketing.business.UserAPITest.class,
        com.dotmarketing.business.portal.PortletAPIImplTest.class,
        com.dotmarketing.business.cache.provider.guava.CachePerformanceTest.class,
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
        com.dotmarketing.common.reindex.ReindexThreadTest.class,
        com.dotmarketing.common.reindex.ReindexAPITest.class,
        com.dotmarketing.common.db.DotDatabaseMetaDataTest.class,
        com.dotmarketing.common.db.ParamsSetterTest.class,
        com.dotmarketing.cms.urlmap.URLMapAPIImplTest.class,
        com.dotmarketing.factories.PublishFactoryTest.class,
        com.dotmarketing.factories.WebAssetFactoryTest.class,
        com.dotmarketing.factories.MultiTreeAPITest.class,
        com.dotmarketing.db.DbConnectionFactoryTest.class,
        com.dotmarketing.db.DbConnectionFactoryUtilTest.class,
        com.dotmarketing.db.HibernateUtilTest.class,
        com.dotmarketing.quartz.job.BinaryCleanupJobTest.class,
        FocalPointAPITest.class,
        com.dotmarketing.tag.business.TagAPITest.class,
        OSGIUtilTest.class,
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
        CleanUpFieldReferencesJobTest.class,
        CachedParameterDecoratorTest.class,
        ContainerFactoryImplTest.class,
        TemplateFactoryImplTest.class,
        TestConfig.class,
        ConfigTest.class,
        FolderTest.class,
        PublishAuditAPITest.class,
        BundleFactoryTest.class,
        com.dotcms.security.apps.SecretsStoreKeyStoreImplTest.class,
        AppsAPIImplTest.class,
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
        ElasticsearchUtilTest.class,
        LanguageUtilTest.class,
        FolderResourceTest.class,
        Task05225RemoveLoadRecordsToIndexTest.class,
        PublisherFilterImplTest.class,
        PushPublishFiltersInitializerTest.class,
        PushPublishFilterResourceTest.class,
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
        GoogleTranslationServiceIntegrationTest.class
})

public class MainSuite2a {

}
