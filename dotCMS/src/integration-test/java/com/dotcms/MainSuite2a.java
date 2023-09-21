package com.dotcms;

import com.dotcms.content.elasticsearch.business.ElasticsearchUtilTest;
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
import com.dotmarketing.startup.runonce.Task05195CreatesDestroyActionAndAssignDestroyDefaultActionsToTheSystemWorkflowTest;
import com.dotmarketing.startup.runonce.Task05210CreateDefaultDotAssetTest;
import com.dotmarketing.startup.runonce.Task05225RemoveLoadRecordsToIndexTest;
import com.dotmarketing.startup.runonce.Task05305AddPushPublishFilterColumnTest;
import com.dotmarketing.startup.runonce.Task05350AddDotSaltClusterColumnTest;
import com.dotmarketing.util.HashBuilderTest;
import com.dotmarketing.util.TestConfig;
import com.liferay.portal.language.LanguageUtilTest;
import org.apache.felix.framework.OSGIUtilTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/* grep -l -r "@Test" dotCMS/src/integration-test */
/* ./gradlew integrationTest -Dtest.single=com.dotcms.MainSuite */


@RunWith(MainBaseSuite.class)
@SuiteClasses({
        com.dotcms.rest.api.v1.workflow.WorkflowResourceResponseCodeIntegrationTest.class
})

public class MainSuite2a {

}
