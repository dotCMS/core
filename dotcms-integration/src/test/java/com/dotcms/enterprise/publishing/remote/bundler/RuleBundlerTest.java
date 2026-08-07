package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.datagen.*;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.manifest.ManifestBuilder;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.FileUtil;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.set;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class RuleBundlerTest {

    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    @DataProvider
    public static Object[] rules() throws Exception {
        prepare();

        // Deliberately creates no persisted data. A @DataProvider is evaluated when
        // the suite is CONSTRUCTED - MainBaseSuite builds every runner up front - so
        // anything persisted here exists long before this test runs, and the test
        // classes scheduled in between can destroy it. That is exactly how this test
        // used to fail ("rule is null") once it was scheduled late in a suite.
        // Fixtures are created inside the test instead; keep this method stateless.
        return new TestCase[]{
                new TestCase(false),
                new TestCase(true)
        };
    }

    /**
     * Creates the Rule under test immediately before it is used, so the fixture
     * cannot be invalidated by unrelated tests running earlier in the suite.
     *
     * @param attachedToPage whether the Rule should be bound to an HTML page
     * @return a freshly persisted Rule
     */
    private Rule createRule(final boolean attachedToPage) {
        if (!attachedToPage) {
            return new RuleDataGen().nextPersisted();
        }

        final Host host = new SiteDataGen().nextPersisted();
        final Template template = new TemplateDataGen().host(host).nextPersisted();
        final HTMLPageAsset htmlPageAsset = new HTMLPageDataGen(host, template).nextPersisted();
        return new RuleDataGen().page(htmlPageAsset).nextPersisted();
    }


    /**
     * Method to Test: {@link RuleBundler#generate(BundleOutput, BundlerStatus)}
     * When: Add a {@link Rule} in a bundle
     * Should:
     * - The file should be create in: <bundle_root_path>/live/<container_host_name>/<rule_id>.rule.xml
     */
    @Test
    @UseDataProvider("rules")
    public void addRuleInBundle(final TestCase testCase)
            throws DotBundleException, IOException, DotSecurityException, DotDataException {

        final Rule rule = createRule(testCase.attachedToPage);

        final BundlerStatus status = mock(BundlerStatus.class);
        final RuleBundler bundler = new RuleBundler();

        final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen().nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();

        try (ManifestBuilder manifestBuilder = new TestManifestBuilder()) {
            config.setManifestBuilder(manifestBuilder);
            config.add(rule, PusheableAsset.RULE, StringPool.BLANK);
            config.setOperation(PublisherConfig.Operation.PUBLISH);

            final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config);

            new BundleDataGen()
                    .pushPublisherConfig(config)
                    .addAssets(list(rule))
                    .filter(filterDescriptor)
                    .nextPersisted();

            bundler.setConfig(config);
            bundler.generate(directoryBundleOutput, status);

            FileTestUtil.assertBundleFile(directoryBundleOutput.getFile(), rule,
                    testCase.expectedFilePath);
        }
    }

    private static class TestCase{
        final boolean attachedToPage;
        final String expectedFilePath;

        public TestCase(final boolean attachedToPage, final String expectedFilePath) {
            this.attachedToPage = attachedToPage;
            this.expectedFilePath = expectedFilePath;
        }

        public TestCase(final boolean attachedToPage) {
            this(attachedToPage, "/bundlers-test/rule/rule.rule.xml");
        }

        // Without this the parameterised test reports as
        // "addRuleInBundle[0: RuleBundlerTest$TestCase@5c2004ac]", which says nothing
        // about which case failed.
        @Override
        public String toString() {
            return attachedToPage ? "rule attached to a page" : "standalone rule";
        }
    }
}
