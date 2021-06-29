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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.FileUtil;
import com.liferay.portal.model.User;
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
public class FolderBundlerTest {

    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    @DataProvider
    public static Object[] folders() throws Exception {
        prepare();
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();

        final Folder parentFolder = new FolderDataGen().site(host).nextPersisted();
        final Folder folderWithParent = new FolderDataGen()
                .parent(parentFolder)
                .site(host)
                .nextPersisted();

        final Folder folderWithDoubleParent = new FolderDataGen()
                .parent(folderWithParent)
                .site(host)
                .nextPersisted();

        return new TestCase[]{
                new TestCase(folder),
                new TestCase(folderWithParent),
                new TestCase(folderWithDoubleParent)
        };
    }


    /**
     * Method to Test: {@link FolderBundler#generate(BundleOutput, BundlerStatus)}
     * When: Add a {@link Folder} in a bundle
     * Should:
     * - The file should be create in:
     * For Live Version: <bundle_root_path>/ROOT/<folder_path>/.../<folder_id>.folder.xml
     */
    @Test
    @UseDataProvider("folders")
    public void addFolderInBundle(final TestCase testCase)
            throws DotBundleException, IOException, DotSecurityException, DotDataException {

        Folder folder =  testCase.folder;

        final BundlerStatus status = mock(BundlerStatus.class);
        final FolderBundler bundler = new FolderBundler();

        final FilterDescriptor filterDescriptor = new FilterDescriptorDataGen().nextPersisted();

        final PushPublisherConfig config = new PushPublisherConfig();
        config.add(folder, PusheableAsset.FOLDER);
        config.setOperation(PublisherConfig.Operation.PUBLISH);

        new BundleDataGen()
                .pushPublisherConfig(config)
                .addAssets(list(folder))
                .filter(filterDescriptor)
                .nextPersisted();

        final DirectoryBundleOutput directoryBundleOutput = new DirectoryBundleOutput(config);

        bundler.setConfig(config);
        bundler.generate(directoryBundleOutput, status);

        final User systemUser = APILocator.systemUser();

        while(folder != null && !folder.isSystemFolder()) {
            FileTestUtil.assertBundleFile(directoryBundleOutput.getFile(), folder, testCase.expectedFilePath);

            folder = APILocator.getFolderAPI().findParentFolder(folder, systemUser, false);
        }
    }

    private static class TestCase{
        Folder folder;
        String expectedFilePath;

        public TestCase(final Folder folder, final String expectedFilePath) {
            this.folder = folder;
            this.expectedFilePath = expectedFilePath;
        }

        public TestCase(final Folder folder) {
            this(folder, "/bundlers-test/folder/folder.folder.xml");
        }
    }
}
