package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.datagen.*;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.publishing.output.BundleOutput;
import com.dotcms.publishing.output.DirectoryBundleOutput;
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
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
public class LinkBundlerTest {


    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] links() throws Exception {
        prepare();

        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Link link = new LinkDataGen(folder)
                .hostId(host.getIdentifier())
                .nextPersisted();

        Link liveLink = new LinkDataGen(folder)
                .hostId(host.getIdentifier())
                .nextPersisted(true);

        return new LinkBundlerTest.TestCase[]{
                new LinkBundlerTest.TestCase(link),
                new LinkBundlerTest.TestCase(liveLink)
        };
    }


    /**
     * Method to Test: {@link LinkBundler#generate(BundleOutput, BundlerStatus)}
     * When: Add a {@link Link} in a bundle
     * Should:
     * - The file should be create in:
     * For Live Version: <bundle_root_path>/live/<link_host_name>/<link_id>.link.xml
     * For Working: <bundle_root_path>/working/<link_host_name>/<link_id>.link.xml
     *
     * If the Link has live and working version then to files will be created
     */
    @Test
    @UseDataProvider("links")
    public void addLinkInBundle(final LinkBundlerTest.TestCase testCase)
            throws DotBundleException, IOException, DotSecurityException, DotDataException {

        final List<Link> links = testCase.links;

        final BundlerStatus status = mock(BundlerStatus.class);
        final LinkBundler bundler = new LinkBundler();

        final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen().nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.add(links.get(0), PusheableAsset.LINK, "");
        config.setOperation(PublisherConfig.Operation.PUBLISH);

        final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config);

        new BundleDataGen()
                .pushPublisherConfig(config)
                .addAssets(list(links.get(0)))
                .filter(filterDescriptor)
                .nextPersisted();

        bundler.setConfig(config);
        bundler.generate(directoryBundleOutput, status);

        for (final Link link : links) {
            FileTestUtil.assertBundleFile(directoryBundleOutput.getFile(), link, testCase.expectedFilePath);
        }
    }

    private static class TestCase{
        List<Link> links;
        String expectedFilePath;

        public TestCase(final Link link) {
            this(list(link));
        }

        public TestCase(final List<Link> links, final String expectedFilePath) {
            this.links = links;
            this.expectedFilePath = expectedFilePath;
        }

        public TestCase(final List<Link> links) {
            this(links, "/bundlers-test/hos");
        }
    }
}
