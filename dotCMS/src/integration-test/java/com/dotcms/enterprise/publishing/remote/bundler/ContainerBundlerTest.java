package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.*;
import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.DotBundleException;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.publishing.PublisherConfig;
import com.dotcms.test.util.FileTestUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.FileUtil;
import com.liferay.portal.model.User;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotcms.util.CollectionsUtils.set;
import static org.mockito.Mockito.mock;

@RunWith(DataProviderRunner.class)
public class ContainerBundlerTest {

    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    @DataProvider
    public static Object[] containers() throws Exception {
        prepare();
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType contentType = new ContentTypeDataGen().host(host).nextPersisted();
        final Container containerWithoutContentType = new ContainerDataGen()
                .site(host)
                .clearContentTypes()
                .nextPersisted();

        final Container containerWithContentType = new ContainerDataGen()
                .site(host)
                .withContentType(contentType, "")
                .nextPersisted();

        final Container containerLive = new ContainerDataGen()
            .site(host)
            .withContentType(contentType, "")
            .nextPersisted();
        ContainerDataGen.publish(containerLive);

        final Container containerWithDifferentVersions = new ContainerDataGen()
                .site(host)
                .clearContentTypes()
                .nextPersisted();
        containerWithDifferentVersions.setInode("");
        ContainerDataGen.publish(containerLive);

        return new TestCase[]{
               new TestCase(containerWithoutContentType,
                        "/bundlers-test/container/container_without_content_type.containers.container.xml"),
                new TestCase(containerWithContentType),
                new TestCase(containerLive),
                new TestCase(containerWithDifferentVersions, "/bundlers-test/container/container_without_content_type.containers.container.xml")
        };
    }

    /**
     * Method to Test: {@link ContentTypeBundler#generate(File, BundlerStatus)}
     * When: Add a ContentType inthe {@link PushPublisherConfig}
     * Should:
     * - create the ContentType file in the bundle directory
     */
    @Test
    @UseDataProvider("containers")
    public void addContainerInBundle(final TestCase testCase)
            throws DotBundleException, IOException, DotSecurityException, DotDataException {

        final Container container =  testCase.container;

        final BundlerStatus status = mock(BundlerStatus.class);
        final ContainerBundler bundler = new ContainerBundler();
        final File bundleRoot = FileUtil.createTemporaryDirectory("ContentTypeBundlerTest_addContentTypeInBundle_");

        final FilterDescriptor filterDescriptor = new FileDescriptorDataGen().nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.setContainers(set(container.getIdentifier()));
        config.setOperation(PublisherConfig.Operation.PUBLISH);

        new BundleDataGen()
                .pushPublisherConfig(config)
                .addAssets(list(container))
                .filter(filterDescriptor)
                .nextPersisted();

        bundler.setConfig(config);
        bundler.generate(bundleRoot, status);

        final User systemUser = APILocator.systemUser();
        final Container workingContainer = APILocator.getContainerAPI()
                .getWorkingContainerById(container.getIdentifier(), systemUser, false);

        FileTestUtil.assertBundleFile(bundleRoot, workingContainer, testCase.expectedFilePath);

        final Container liveContainer = APILocator.getContainerAPI()
                .getLiveContainerById(container.getIdentifier(), systemUser, false);

        if(liveContainer != null && !liveContainer.getInode().equals(workingContainer.getInode())){
            FileTestUtil.assertBundleFile(bundleRoot, liveContainer, testCase.expectedFilePath);
        }
    }

    private static class TestCase{
        Container container;
        String expectedFilePath;

        public TestCase(final Container container, final String expectedFilePath) {
            this.container = container;
            this.expectedFilePath = expectedFilePath;
        }

        public TestCase(final Container container) {
            this(container, "/bundlers-test/container/container.containers.container.xml");
        }
    }
}
