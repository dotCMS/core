package com.dotcms;

import com.dotcms.ai.workflow.OpenAIAutoTagActionletTest;
import com.dotcms.content.elasticsearch.util.ESMappingUtilHelperTest;
import com.dotcms.contenttype.business.DotAssetBaseTypeToContentTypeStrategyImplTest;
import com.dotcms.contenttype.test.DotAssetAPITest;
import com.dotcms.dotpubsub.PostgresPubSubImplTest;
import com.dotcms.ema.EMAWebInterceptorTest;
import com.dotcms.enterprise.cluster.ClusterFactoryTest;
import com.dotcms.junit.MainBaseSuite;
import com.dotcms.mock.request.CachedParameterDecoratorTest;
import com.dotcms.publisher.bundle.business.BundleFactoryTest;
import com.dotcms.publisher.business.PublishAuditAPITest;
import com.dotcms.publisher.util.PushedAssetUtilTest;
import com.dotcms.publishing.PublisherFilterImplTest;
import com.dotcms.publishing.PushPublishFiltersInitializerTest;
import com.dotcms.rendering.velocity.directive.DotParseTest;
import com.dotcms.rendering.velocity.servlet.VelocityServletIntegrationTest;
import com.dotcms.rest.BundleResourceTest;
import com.dotcms.rest.api.v1.apps.AppsResourceTest;
import com.dotcms.rest.api.v1.folder.FolderResourceTest;
import com.dotcms.rest.api.v1.pushpublish.PushPublishFilterResourceTest;
import com.dotcms.rest.api.v1.user.UserResourceIntegrationTest;
import com.dotcms.saml.IdentityProviderConfigurationFactoryTest;
import com.dotcms.saml.SamlConfigurationServiceTest;
import com.dotcms.security.apps.AppsAPIImplTest;
import com.dotcms.security.apps.AppsCacheImplTest;
import com.dotcms.translate.GoogleTranslationServiceIntegrationTest;
import com.dotmarketing.image.focalpoint.FocalPointAPITest;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactoryTest;
import com.dotmarketing.portlets.containers.business.ContainerFactoryImplTest;
import com.dotmarketing.portlets.containers.business.ContainerStructureFinderStrategyResolverTest;
import com.dotmarketing.portlets.contentlet.model.IntegrationResourceLinkTest;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPIImplIntegrationTest;
import com.dotmarketing.portlets.fileassets.business.FileAssetFactoryIntegrationTest;
import com.dotmarketing.portlets.folders.model.FolderTest;
import com.dotmarketing.portlets.templates.business.TemplateFactoryImplTest;
import com.dotmarketing.portlets.workflows.actionlet.PushNowActionletTest;
import com.dotmarketing.portlets.workflows.model.TestWorkflowAction;
import com.dotmarketing.quartz.job.CleanUpFieldReferencesJobTest;
import com.dotmarketing.startup.runonce.Task05225RemoveLoadRecordsToIndexTest;
import com.dotmarketing.startup.runonce.Task05305AddPushPublishFilterColumnTest;
import com.dotmarketing.startup.runonce.Task05350AddDotSaltClusterColumnTest;
import com.dotmarketing.startup.runonce.Task240131UpdateLanguageVariableContentTypeTest;
import com.dotmarketing.util.HashBuilderTest;
import com.dotmarketing.util.TestConfig;
import com.liferay.portal.language.LanguageUtilTest;
import org.apache.felix.framework.OSGIUtilTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/* grep -l -r "@Test" dotCMS/src/integration-test */
/* ./gradlew integrationTest -Dtest.single=com.dotcms.MainSuite */

/**
 * NOTE: LET'S AVOID ADDING MORE TESTS TO THIS SUITE, THIS ONE IS TAKING ALMOST TWICE THE TIME TO RUN THAN THE OTHERS
 */
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
        com.dotmarketing.portlets.contentlet.transform.WidgetViewStrategyTest.class,
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
        com.dotmarketing.factories.MultiTreeAPITest.class,
        com.dotmarketing.portlets.categories.business.CategoryAPITest.class,
        com.dotmarketing.filters.FiltersTest.class
})
public class MainSuite2a {

}
