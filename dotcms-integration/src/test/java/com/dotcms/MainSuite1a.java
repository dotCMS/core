package com.dotcms;

import com.dotcms.ai.workflow.OpenAIGenerateImageActionletTest;
import com.dotcms.analytics.track.RequestMatcherTest;
import com.dotcms.content.elasticsearch.business.ESContentletAPIImplTest;
import com.dotcms.contenttype.business.SiteAndFolderResolverImplTest;
import com.dotcms.enterprise.publishing.remote.PushPublishBundleGeneratorTest;
import com.dotcms.enterprise.publishing.remote.bundler.DependencyBundlerTest;
import com.dotcms.enterprise.publishing.remote.bundler.RuleBundlerTest;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTest;
import com.dotcms.enterprise.rules.RulesAPIImplIntegrationTest;
import com.dotcms.experiments.business.ExperimentAPIImpIntegrationTest;
import com.dotcms.experiments.business.ExperimentUrlPatternCalculatorIntegrationTest;
import com.dotcms.experiments.business.web.ExperimentWebAPIImplIntegrationTest;
import com.dotcms.graphql.DotGraphQLHttpServletTest;
import com.dotcms.integritycheckers.ContentFileAssetIntegrityCheckerTest;
import com.dotcms.integritycheckers.ContentPageIntegrityCheckerTest;
import com.dotcms.integritycheckers.FolderIntegrityCheckerTest;
import com.dotcms.integritycheckers.HostIntegrityCheckerTest;
import com.dotcms.junit.MainBaseSuite;
import com.dotcms.publisher.bundle.business.BundleFactoryImplTest;
import com.dotcms.publisher.business.PublishQueueElementTransformerTest;
import com.dotcms.publisher.util.DependencyModDateUtilTest;
import com.dotcms.publishing.job.SiteSearchJobImplTest;
import com.dotcms.rendering.js.JsEngineTest;
import com.dotcms.rendering.velocity.viewtools.XsltToolTest;
import com.dotcms.storage.FileMetadataAPITest;
import com.dotcms.uuid.shorty.LegacyShortyIdApiTest;
import com.dotmarketing.cache.FolderCacheImplIntegrationTest;
import com.dotmarketing.portlets.contentlet.business.HostFactoryImplTest;
import com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPIImplIntegrationTest;
import com.dotmarketing.portlets.workflows.actionlet.EmailActionletTest;
import com.dotmarketing.quartz.job.StartEndScheduledExperimentsJobTest;
import com.dotmarketing.startup.runonce.Task220825CreateVariantFieldTest;
import com.dotmarketing.startup.runonce.Task221007AddVariantIntoPrimaryKeyTest;
import com.dotmarketing.startup.runonce.Task240306MigrateLegacyLanguageVariablesTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/* grep -l -r "@Test" dotCMS/src/integration-test */
/* ./gradlew integrationTest -Dtest.single=com.dotcms.MainSuite */


@RunWith(MainBaseSuite.class)
@SuiteClasses({
        StartEndScheduledExperimentsJobTest.class,
        RulesAPIImplIntegrationTest.class,
        ESContentletAPIImplTest.class,
        ExperimentAPIImpIntegrationTest.class,
        ExperimentWebAPIImplIntegrationTest.class,
        ContentletWebAPIImplIntegrationTest.class, // moved to top because of failures on GHA
        DependencyBundlerTest.class, // moved to top because of failures on GHA
        SiteAndFolderResolverImplTest.class, //Moved up to avoid conflicts with CT deletion
        FolderCacheImplIntegrationTest.class,
        StaticPublisherIntegrationTest.class,
        com.dotcms.publishing.PublisherAPIImplTest.class,
        SiteSearchJobImplTest.class,
        XsltToolTest.class,
        PushPublishBundleGeneratorTest.class,
        LegacyShortyIdApiTest.class,
        RuleBundlerTest.class,
        com.dotcms.content.elasticsearch.business.ESMappingAPITest.class,
        org.apache.velocity.runtime.parser.node.SimpleNodeTest.class,
        com.liferay.portal.ejb.UserLocalManagerTest.class,
        com.liferay.portal.ejb.UserUtilTest.class,
        com.liferay.util.LocaleUtilTest.class,
        com.dotcms.languagevariable.business.LanguageVariableAPITest.class,
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
        com.dotcms.publisher.business.PublisherAPIImplTest.class,
        PublishQueueElementTransformerTest.class,
        com.dotmarketing.util.PageModeTest.class,
        com.dotmarketing.business.web.UserWebAPIImplTest.class,
        com.dotcms.auth.providers.jwt.JsonWebTokenUtilsIntegrationTest.class,
        com.dotcms.auth.providers.jwt.factories.ApiTokenAPITest.class,
        com.dotcms.auth.providers.jwt.services.JsonWebTokenServiceIntegrationTest.class,
        DependencyModDateUtilTest.class,
        com.dotcms.publisher.business.PublisherTest.class,
        com.dotcms.enterprise.publishing.PublishDateUpdaterIntegrationTest.class,
        com.dotcms.publisher.endpoint.bean.PublishingEndPointTest.class,
        com.dotcms.publisher.endpoint.business.PublishingEndPointAPITest.class,
        com.dotcms.publisher.endpoint.business.PublishingEndPointFactoryImplTest.class,
        com.dotcms.publisher.assets.business.PushedAssetsAPITest.class,
        com.dotcms.notification.business.NotificationAPITest.class,
        com.dotcms.business.LocalTransactionAndCloseDBIfOpenedFactoryTest.class,
        FolderIntegrityCheckerTest.class,
        HostFactoryImplTest.class,
        BundleFactoryImplTest.class,
        ExperimentUrlPatternCalculatorIntegrationTest.class,
        JsEngineTest.class,
        Task240306MigrateLegacyLanguageVariablesTest.class,
        EmailActionletTest.class,
        OpenAIGenerateImageActionletTest.class,
        RequestMatcherTest.class,
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
})

public class MainSuite1a {

}
