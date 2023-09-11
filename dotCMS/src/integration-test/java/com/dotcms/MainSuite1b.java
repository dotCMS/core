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
import com.dotcms.junit.MainBaseSuite;
import com.dotcms.publisher.business.PublishQueueElementTransformerTest;
import com.dotcms.publisher.util.DependencyModDateUtilTest;
import com.dotcms.publishing.job.SiteSearchJobImplTest;
import com.dotcms.rendering.velocity.viewtools.XsltToolTest;
import com.dotcms.uuid.shorty.LegacyShortyIdApiTest;
import com.dotmarketing.cache.FolderCacheImplIntegrationTest;
import com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPIImplIntegrationTest;
import com.dotmarketing.quartz.QuartzUtilsTest;
import com.dotmarketing.quartz.job.StartEndScheduledExperimentsJobTest;
import com.dotmarketing.startup.runonce.Task220825CreateVariantFieldTest;
import com.dotmarketing.startup.runonce.Task221007AddVariantIntoPrimaryKeyTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/* grep -l -r "@Test" dotCMS/src/integration-test */
/* ./gradlew integrationTest -Dtest.single=com.dotcms.MainSuite */


@RunWith(MainBaseSuite.class)
@SuiteClasses({
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
        com.dotcms.analytics.metrics.QueryParameterValuesGetterTest.class,
        com.dotcms.analytics.metrics.QueryParameterValuesTransformerTest.class,
        QuartzUtilsTest.class
})

public class MainSuite1b {

}
