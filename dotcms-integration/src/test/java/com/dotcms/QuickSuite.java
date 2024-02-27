package com.dotcms;

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
import com.dotcms.integritycheckers.FolderIntegrityCheckerTest;
import com.dotcms.junit.MainBaseSuite;
import com.dotcms.publisher.bundle.business.BundleFactoryImplTest;
import com.dotcms.publisher.business.PublishQueueElementTransformerTest;
import com.dotcms.publisher.util.DependencyModDateUtilTest;
import com.dotcms.publishing.job.SiteSearchJobImplTest;
import com.dotcms.rendering.js.JsEngineTest;
import com.dotcms.rendering.velocity.viewtools.XsltToolTest;
import com.dotcms.uuid.shorty.LegacyShortyIdApiTest;
import com.dotmarketing.cache.FolderCacheImplIntegrationTest;
import com.dotmarketing.portlets.contentlet.business.HostFactoryImplTest;
import com.dotmarketing.portlets.contentlet.business.web.ContentletWebAPIImplIntegrationTest;
import com.dotmarketing.quartz.job.StartEndScheduledExperimentsJobTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/* grep -l -r "@Test" dotCMS/src/integration-test */
/* ./gradlew integrationTest -Dtest.single=com.dotcms.MainSuite */


@RunWith(MainBaseSuite.class)
@SuiteClasses({
        FolderCacheImplIntegrationTest.class
})

public class QuickSuite {

}
