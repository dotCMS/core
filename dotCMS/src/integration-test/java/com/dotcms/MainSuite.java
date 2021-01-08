package com.dotcms;

import com.dotcms.auth.providers.saml.v1.DotSamlResourceTest;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImplTest;
import com.dotcms.content.elasticsearch.business.ElasticsearchUtilTest;
import com.dotcms.content.elasticsearch.util.ESMappingUtilHelperTest;
import com.dotcms.contenttype.business.DotAssetBaseTypeToContentTypeStrategyImplTest;
import com.dotcms.contenttype.test.DotAssetAPITest;
import com.dotcms.ema.EMAWebInterceptorTest;
import com.dotcms.enterprise.HTMLDiffUtilTest;
import com.dotcms.enterprise.cluster.ClusterFactoryTest;
import com.dotcms.enterprise.publishing.remote.PushPublishBundleGeneratorTest;
import com.dotcms.enterprise.rules.RulesAPIImplIntegrationTest;
import com.dotcms.graphql.DotGraphQLHttpServletTest;
import com.dotcms.junit.MainBaseSuite;
import com.dotcms.mock.request.CachedParameterDecoratorTest;
import com.dotcms.publisher.bundle.business.BundleAPITest;
import com.dotcms.publisher.bundle.business.BundleFactoryTest;
import com.dotcms.publisher.business.PublishAuditAPITest;
import com.dotcms.publisher.util.DependencyManagerTest;
import com.dotcms.publishing.PublisherFilterImplTest;
import com.dotcms.publishing.PushPublishFiltersInitializerTest;
import com.dotcms.publishing.job.SiteSearchJobImplTest;
import com.dotcms.rendering.velocity.directive.DotParseTest;
import com.dotcms.rendering.velocity.servlet.VelocityServletIntegrationTest;
import com.dotcms.rendering.velocity.viewtools.DotTemplateToolTest;
import com.dotcms.rendering.velocity.viewtools.JSONToolTest;
import com.dotcms.rest.BundlePublisherResourceIntegrationTest;
import com.dotcms.rest.BundleResourceTest;
import com.dotcms.rest.IntegrityResourceIntegrationTest;
import com.dotcms.rest.api.v1.apps.AppsResourceTest;
import com.dotcms.rest.api.v1.apps.view.AppsInterpolationTest;
import com.dotcms.rest.api.v1.folder.FolderResourceTest;
import com.dotcms.rest.api.v1.pushpublish.PushPublishFilterResourceTest;
import com.dotcms.rest.api.v1.user.UserResourceIntegrationTest;
import com.dotcms.saml.IdentityProviderConfigurationFactoryTest;
import com.dotcms.saml.SamlConfigurationServiceTest;
import com.dotcms.security.apps.AppsAPIImplTest;
import com.dotcms.storage.FileMetadataAPITest;
import com.dotcms.storage.StoragePersistenceAPITest;
import com.dotcms.security.apps.AppsCacheImplTest;
import com.dotcms.translate.GoogleTranslationServiceIntegrationTest;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactoryTest;
import com.dotmarketing.portlets.containers.business.ContainerFactoryImplTest;
import com.dotmarketing.portlets.containers.business.ContainerStructureFinderStrategyResolverTest;
import com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPIImplIntegrationTest;
import com.dotmarketing.portlets.contentlet.model.IntegrationResourceLinkTest;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPIImplIntegrationTest;
import com.dotmarketing.portlets.fileassets.business.FileAssetFactoryIntegrationTest;
import com.dotmarketing.portlets.folders.business.FolderFactoryImplTest;
import com.dotmarketing.portlets.templates.business.TemplateFactoryImplTest;
import com.dotmarketing.portlets.folders.model.FolderTest;
import com.dotmarketing.portlets.workflows.actionlet.PushNowActionletTest;
import com.dotmarketing.portlets.workflows.model.TestWorkflowAction;
import com.dotmarketing.quartz.DotStatefulJobTest;
import com.dotmarketing.quartz.job.CleanUpFieldReferencesJobTest;
import com.dotmarketing.startup.StartupTasksExecutorTest;
import com.dotmarketing.startup.runonce.Task05195CreatesDestroyActionAndAssignDestroyDefaultActionsToTheSystemWorkflowTest;
import com.dotmarketing.startup.runonce.Task05210CreateDefaultDotAssetTest;
import com.dotmarketing.startup.runonce.Task05225RemoveLoadRecordsToIndexTest;
import com.dotmarketing.startup.runonce.Task05305AddPushPublishFilterColumnTest;
import com.dotmarketing.startup.runonce.Task05350AddDotSaltClusterColumnTest;
import com.dotmarketing.startup.runonce.Task05370AddAppsPortletToLayoutTest;
import com.dotmarketing.startup.runonce.Task05380ChangeContainerPathToAbsoluteTest;
import com.dotmarketing.startup.runonce.Task05390MakeRoomForLongerJobDetailTest;
import com.dotmarketing.startup.runonce.Task05395RemoveEndpointIdForeignKeyInIntegrityResolverTablesIntegrationTest;
import com.dotmarketing.startup.runalways.Task00050LoadAppsSecretsTest;
import com.dotmarketing.startup.runonce.Task201013AddNewColumnsToIdentifierTableTest;
import com.dotmarketing.startup.runonce.Task201014UpdateColumnsValuesInIdentifierTableTest;
import com.dotmarketing.startup.runonce.Task201102UpdateColumnSitelicTableTest;
import com.dotmarketing.util.ConfigTest;
import com.dotmarketing.util.HashBuilderTest;
import com.dotmarketing.quartz.job.IntegrityDataGenerationJobTest;
import com.dotmarketing.util.TestConfig;
import com.liferay.portal.language.LanguageUtilTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/* grep -l -r "@Test" dotCMS/src/integration-test */
/* ./gradlew integrationTest -Dtest.single=com.dotcms.MainSuite */
//@RunWith(Suite.class)



@RunWith(MainBaseSuite.class)
@SuiteClasses({
        org.apache.velocity.runtime.parser.node.SimpleNodeTest.class,
        com.liferay.portal.ejb.UserLocalManagerTest.class,
        com.liferay.portal.ejb.UserUtilTest.class,
        com.liferay.util.LocaleUtilTest.class,
        com.dotcms.languagevariable.business.LanguageVariableAPITest.class,
        com.dotcms.filters.interceptor.jwt.JsonWebTokenInterceptorIntegrationTest.class,
        com.dotcms.publishing.PublisherAPITest.class,
        com.dotcms.publishing.remote.RemoteReceiverLanguageResolutionTest.class,
        com.dotcms.cluster.business.ServerAPIImplTest.class,
        com.dotcms.cache.KeyValueCacheImplTest.class,
        com.dotcms.enterprise.publishing.remote.handler.RuleBundlerHandlerTest.class,
        com.dotcms.enterprise.publishing.remote.CategoryBundlerHandlerTest.class,
        com.dotcms.enterprise.publishing.remote.HostBundlerHandlerTest.class,
        com.dotcms.enterprise.priv.ESSearchProxyTest.class,
        com.dotcms.util.pagination.ContentTypesPaginatorTest.class,
        com.dotcms.util.marshal.MarshalUtilsIntegrationTest.class,
        com.dotcms.util.RelationshipUtilTest.class,
        com.dotcms.util.ImportUtilTest.class,
        com.dotmarketing.util.PageModeTest.class,
        com.dotmarketing.business.web.UserWebAPIImplTest.class,
        com.dotcms.auth.providers.jwt.JsonWebTokenUtilsIntegrationTest.class,
        com.dotcms.auth.providers.jwt.factories.ApiTokenAPITest.class,
        com.dotcms.auth.providers.jwt.services.JsonWebTokenServiceIntegrationTest.class,
        com.dotcms.publisher.util.DependencySetTest.class,
        com.dotcms.publisher.business.PublisherTest.class,
        com.dotcms.publisher.endpoint.bean.PublishingEndPointTest.class,
        com.dotcms.publisher.endpoint.business.PublishingEndPointAPITest.class,
        com.dotcms.publisher.endpoint.business.PublishingEndPointFactoryImplTest.class,
        com.dotcms.publisher.assets.business.PushedAssetsAPITest.class,
        com.dotcms.notification.business.NotificationAPITest.class,
        com.dotcms.business.LocalTransactionAndCloseDBIfOpenedFactoryTest.class,
        com.dotcms.content.elasticsearch.business.IndiciesFactoryTest.class,
        com.dotcms.content.elasticsearch.business.ESIndexSpeedTest.class,
        com.dotcms.content.elasticsearch.business.ESSiteSearchAPITest.class,
        com.dotcms.content.elasticsearch.business.ESMappingAPITest.class,
        com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplTest.class,
        com.dotcms.content.elasticsearch.business.ES6UpgradeTest.class,
        com.dotcms.content.elasticsearch.business.ESContentFactoryImplTest.class,
        com.dotcms.keyvalue.busines.KeyValueAPIImplTest.class,
        com.dotcms.keyvalue.business.KeyValueAPITest.class,
        com.dotcms.tika.TikaUtilsTest.class,
        com.dotcms.http.CircuitBreakerUrlTest.class,
        com.dotcms.visitor.filter.logger.VisitorLoggerTest.class,
        com.dotcms.visitor.filter.characteristics.VisitorCharacterTest.class,
        com.dotcms.graphql.business.GraphqlAPITest.class,
        com.dotcms.contenttype.test.ContentTypeTest.class,
        com.dotcms.contenttype.test.DeleteFieldJobTest.class,
        com.dotcms.contenttype.test.ContentTypeAPIImplTest.class,
        com.dotcms.contenttype.test.ContentTypeBuilderTest.class,
        com.dotcms.contenttype.test.ContentTypeFactoryImplTest.class,
        com.dotcms.contenttype.test.ContentTypeImportExportTest.class,
        com.dotcms.contenttype.test.FieldFactoryImplTest.class,
        com.dotcms.contenttype.test.JsonContentTypeTransformerTest.class,
        com.dotcms.contenttype.test.ContentResourceTest.class,
        com.dotcms.contenttype.test.FieldBuilderTest.class,
        com.dotcms.contenttype.test.KeyValueFieldUtilTest.class,
        com.dotcms.contenttype.test.ContentTypeResourceTest.class,
        com.dotcms.contenttype.business.RelationshipAPITest.class,
        com.dotcms.contenttype.business.FieldAPITest.class,
        com.dotcms.contenttype.business.RelationshipFactoryImplTest.class,
        com.dotcms.contenttype.model.field.layout.FieldUtilTest.class,
        com.dotcms.contenttype.model.field.layout.FieldLayoutColumnSerializerTest.class,
        com.dotcms.contenttype.model.field.layout.FieldLayoutSerializerTest.class,
        com.dotcms.contenttype.model.field.layout.FieldLayoutRowSerializerTest.class,
        com.dotcms.contenttype.model.field.layout.FieldLayoutTest.class,
        com.dotcms.workflow.helper.TestSystemActionMappingsHandlerMerger.class,
        com.dotcms.concurrent.lock.DotKeyLockManagerTest.class,
        com.dotcms.concurrent.DotConcurrentFactoryTest.class,
        com.dotcms.rendering.velocity.VelocityMacroCacheTest.class,
        com.dotcms.rendering.velocity.VelocityUtilTest.class,
        com.dotcms.rendering.velocity.viewtools.navigation.NavToolTest.class,
        com.dotcms.rendering.velocity.viewtools.navigation.NavToolCacheTest.class,
        com.dotcms.rendering.velocity.viewtools.content.ContentMapTest.class,
        com.dotcms.rendering.velocity.viewtools.content.util.ContentUtilsTest.class,
        com.dotcms.rendering.velocity.viewtools.content.ContentToolTest.class,
        com.dotcms.rendering.velocity.viewtools.WorkflowToolTest.class,
        com.dotcms.browser.BrowserAPITest.class,
        com.dotcms.rendering.velocity.viewtools.LanguageWebAPITest.class,
        com.dotcms.rendering.velocity.viewtools.ContainerWebAPIIntegrationTest.class,
        com.dotcms.rendering.velocity.services.VelocityResourceKeyTest.class,
        com.dotcms.rendering.velocity.services.HTMLPageAssetRenderedTest.class,
        com.dotcms.uuid.shorty.ShortyIdApiTest.class,
        DotGraphQLHttpServletTest.class,
        com.dotcms.rest.TagResourceIntegrationTest.class,
        com.dotcms.rest.MapToContentletPopulatorTest.class,
        com.dotcms.rest.WebResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.configuration.ConfigurationResourceTest.class,
        com.dotcms.rest.api.v1.page.NavResourceTest.class,
        com.dotcms.rest.api.v1.page.PageResourceTest.class,
        com.dotcms.rest.api.v1.temp.TempFileResourceTest.class,
        com.dotcms.rest.api.v1.content.ContentVersionResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.container.ContainerResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.theme.ThemeResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.vtl.VTLResourceIntegrationTest.class,
        com.dotcms.rest.api.v1.contenttype.ContentTypeResourceIssue15124Test.class,
        com.dotcms.rest.api.v1.contenttype.FieldResourceTest.class,
        com.dotcms.rest.api.v1.contenttype.FieldVariableResourceTest.class,
        com.dotcms.rest.api.v1.contenttype.ContentTypeResourceTest.class,
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
        com.dotmarketing.portlets.contentlet.business.HostAPITest.class,
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
        com.dotmarketing.portlets.folders.business.FolderAPITest.class,
        com.dotmarketing.portlets.containers.business.ContainerAPIImplTest.class,
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
        com.dotmarketing.business.UserProxyFactoryTest.class,
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
        com.dotcms.osgi.OSGIUtilTest.class,
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
        ESContentletAPIImplTest.class,
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
        RulesAPIImplIntegrationTest.class,
        FileAssetAPIImplIntegrationTest.class,
        FileAssetFactoryIntegrationTest.class,
        UserResourceIntegrationTest.class,
        SiteSearchJobImplTest.class,
        IntegrationResourceLinkTest.class,
        HTMLDiffUtilTest.class,
        HashBuilderTest.class,
        ElasticsearchUtilTest.class,
        LanguageUtilTest.class,
        FolderResourceTest.class,
        Task05225RemoveLoadRecordsToIndexTest.class,
        PushPublishBundleGeneratorTest.class,
        BundleFactoryTest.class,
        PublisherFilterImplTest.class,
        PushPublishFiltersInitializerTest.class,
        PushPublishFilterResourceTest.class,
        PushNowActionletTest.class,
        Task05305AddPushPublishFilterColumnTest.class,
        CMSMaintenanceFactoryTest.class,
        Task05350AddDotSaltClusterColumnTest.class,
        DotParseTest.class,
        TestWorkflowAction.class,
        SamlConfigurationServiceTest.class,
        ClusterFactoryTest.class,
        ESMappingUtilHelperTest.class,
        BundleResourceTest.class,
        IdentityProviderConfigurationFactoryTest.class,
        EMAWebInterceptorTest.class,
        GoogleTranslationServiceIntegrationTest.class,
        Task05380ChangeContainerPathToAbsoluteTest.class,
        DotTemplateToolTest.class,
        ContentletWebAPIImplIntegrationTest.class,
        Task05370AddAppsPortletToLayoutTest.class,
        FolderFactoryImplTest.class,
        DotSamlResourceTest.class,
        DotStatefulJobTest.class,
        IntegrityDataGenerationJobTest.class,
        BundleAPITest.class,
        Task05390MakeRoomForLongerJobDetailTest.class,
        IntegrityDataGenerationJobTest.class,
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
        com.dotcms.rest.api.v1.template.TemplateResourceTest.class,
        Task201102UpdateColumnSitelicTableTest.class,
        DependencyManagerTest.class,
        com.dotcms.rest.api.v1.versionable.VersionableResourceTest.class
})
public class MainSuite {

}
