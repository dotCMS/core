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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.FileUtil;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.set;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class HostBundlerTest {


    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] hosts() throws Exception {
        prepare();

        final Host host = new SiteDataGen().nextPersisted(false);
        final Host liveHost = new SiteDataGen().nextPersisted(true);

        final Host hostWithDifferentVersions = new SiteDataGen().nextPersisted(true);
        final Contentlet checkout = ContentletDataGen.checkout(hostWithDifferentVersions);
        ContentletDataGen.checkin(checkout);
        final Host liveHostVersion =
                APILocator.getHostAPI().find(hostWithDifferentVersions.getIdentifier(), APILocator.systemUser(), false);

        return new HostBundlerTest.TestCase[]{
                new HostBundlerTest.TestCase(host),
                new HostBundlerTest.TestCase(liveHost),
                new HostBundlerTest.TestCase(list(hostWithDifferentVersions, liveHostVersion))
        };
    }


    /**
     * Method to Test: {@link HostBundler#generate(BundleOutput, BundlerStatus)}
     * When: Add a {@link Host} in a bundle
     * Should:
     * - The file should be create in:
     * For Live Version: <bundle_root_path>/live/SYSTEM HOST/<default_language_id>/<host_inode>.host.xml
     * For Working: <bundle_root_path>/working/SYSTEM HOST/<default_language_id>/<host_inode>.host.xml
     *
     * If the Host has live and working version then to files will be created
     */
    @Test
    @UseDataProvider("hosts")
    public void addHostInBundle(final HostBundlerTest.TestCase testCase)
            throws DotBundleException, IOException, DotSecurityException, DotDataException {

        final List<Host> hosts = testCase.hosts;

        final BundlerStatus status = mock(BundlerStatus.class);
        final HostBundler bundler = new HostBundler();

        final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen().nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();

        try (ManifestBuilder manifestBuilder = new TestManifestBuilder()) {
            config.setManifestBuilder(manifestBuilder);
            hosts.stream().forEach(host -> config.add(host, PusheableAsset.SITE, ""));
            config.setOperation(PublisherConfig.Operation.PUBLISH);

            final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config);

            new BundleDataGen()
                    .pushPublisherConfig(config)
                    .addAssets(list(hosts.get(0)))
                    .filter(filterDescriptor)
                    .nextPersisted();

            bundler.setConfig(config);
            bundler.generate(directoryBundleOutput, status);

            for (final Host host : hosts) {
                FileTestUtil.assertBundleFile(directoryBundleOutput.getFile(), host, testCase.expectedFilePath);
            }
        }
    }

    private static class TestCase{
        List<Host> hosts;
        String expectedFilePath;

        public TestCase(final Host host) {
            this(list(host));
        }

        public TestCase(final List<Host> hosts, final String expectedFilePath) {
            this.hosts = hosts;
            this.expectedFilePath = expectedFilePath;
        }

        public TestCase(final List<Host> hosts) {
            this(hosts, "/bundlers-test/hos");
        }
    }
}
