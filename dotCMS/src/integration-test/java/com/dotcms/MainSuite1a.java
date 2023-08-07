package com.dotcms;

import com.dotcms.content.elasticsearch.business.ESContentletAPIImplTest;
import com.dotcms.contenttype.business.SiteAndFolderResolverImplTest;
import com.dotcms.enterprise.publishing.remote.PushPublishBundleGeneratorTest;
import com.dotcms.enterprise.publishing.remote.bundler.DependencyBundlerTest;
import com.dotcms.enterprise.publishing.remote.bundler.RuleBundlerTest;
import com.dotcms.enterprise.publishing.staticpublishing.StaticPublisherIntegrationTest;
import com.dotcms.enterprise.rules.RulesAPIImplIntegrationTest;
import com.dotcms.experiments.business.ExperimentAPIImpIntegrationTest;
import com.dotcms.experiments.business.web.ExperimentWebAPIImplIntegrationTest;
import com.dotcms.graphql.DotGraphQLHttpServletTest;
import com.dotcms.integritycheckers.ContentFileAssetIntegrityCheckerTest;
import com.dotcms.integritycheckers.ContentPageIntegrityCheckerTest;
import com.dotcms.integritycheckers.FolderIntegrityCheckerTest;
import com.dotcms.integritycheckers.HostIntegrityCheckerTest;
import com.dotcms.junit.MainBaseSuite;
import com.dotcms.publisher.business.PublishQueueElementTransformerTest;
import com.dotcms.publisher.util.DependencyModDateUtilTest;
import com.dotcms.publishing.job.SiteSearchJobImplTest;
import com.dotcms.rendering.velocity.viewtools.XsltToolTest;
import com.dotcms.storage.FileMetadataAPITest;
import com.dotcms.uuid.shorty.LegacyShortyIdApiTest;
import com.dotmarketing.cache.FolderCacheImplIntegrationTest;
import com.dotmarketing.portlets.contentlet.business.HostFactoryImplTest;
import com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPIImplIntegrationTest;
import com.dotmarketing.quartz.job.StartEndScheduledExperimentsJobTest;
import com.dotmarketing.startup.runonce.Task220825CreateVariantFieldTest;
import com.dotmarketing.startup.runonce.Task221007AddVariantIntoPrimaryKeyTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/* grep -l -r "@Test" dotCMS/src/integration-test */
/* ./gradlew integrationTest -Dtest.single=com.dotcms.MainSuite */


@RunWith(MainBaseSuite.class)
@SuiteClasses({
        StartEndScheduledExperimentsJobTest.class,
        RulesAPIImplIntegrationTest.class,
        Task220825CreateVariantFieldTest.class,
        Task221007AddVariantIntoPrimaryKeyTest.class,
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
        com.dotcms.publisher.business.PublisherAPIImplTest.class,
        PublishQueueElementTransformerTest.class,
        com.dotmarketing.util.PageModeTest.class,
        com.dotmarketing.business.web.UserWebAPIImplTest.class,
        com.dotcms.auth.providers.jwt.JsonWebTokenUtilsIntegrationTest.class,
        com.dotcms.auth.providers.jwt.factories.ApiTokenAPITest.class,
        com.dotcms.auth.providers.jwt.services.JsonWebTokenServiceIntegrationTest.class,
        DependencyModDateUtilTest.class,
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
        com.dotcms.content.elasticsearch.business.ContentletIndexAPIImplTest.class,
        com.dotcms.content.elasticsearch.business.ES6UpgradeTest.class,
        com.dotcms.content.elasticsearch.business.ESContentFactoryImplTest.class,
        FolderIntegrityCheckerTest.class,
        HostFactoryImplTest.class,
})

public class MainSuite1a {

}
