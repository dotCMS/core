package com.dotmarketing.portlets.containers.business;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.datagen.ContainerAsFileDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.util.FileUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class FileAssetContainerUtilTest extends ContentletBaseTest {

    /**
     * Method to Test: {@link FileAssetContainerUtil#getFullPath(FileAssetContainer)}
     * When: Create a new {@link FileAssetContainer}
     * should: return the full path
     *
     * @throws Exception
     */
    @Test
    public void testGetFullPathMethod() throws Exception {

        final Host host = new SiteDataGen().nextPersisted();

        final FileAssetContainer fileAssetContainer = new ContainerAsFileDataGen()
                .host(host)
                .nextPersisted();

        final FileAssetContainer container = (FileAssetContainer) APILocator.getContainerAPI()
                .find(fileAssetContainer.getInode(), APILocator.systemUser(), true);

        final String fullPath = FileAssetContainerUtil.getInstance().getFullPath(container);

        final String expected = "//" + host.getHostname() + (container).getPath();

        Assert.assertEquals(expected, fullPath);
    }

    @Test
    public void test_getPathFromFullPath_wrong_host_on_full_path() throws Exception {

        final String relativePath = FileAssetContainerUtil.getInstance().getPathFromFullPath("demo.dotcms.com","//global.dotcms.com/application/container/test-container");

        Assert.assertNotNull(relativePath);
        Assert.assertEquals(relativePath, "//global.dotcms.com/application/container/test-container");
    }

    @Test
    public void test_getPathFromFullPath_right_host_on_full_path() throws Exception {

        final String relativePath = FileAssetContainerUtil.getInstance().getPathFromFullPath("demo.dotcms.com","//demo.dotcms.com/application/container/test-container");

        Assert.assertNotNull(relativePath);
        Assert.assertEquals(relativePath, "/application/container/test-container");
    }

    @Test
    public void test_getPathFromFullPath_any_host_on_relative_path() throws Exception {

        final String relativePath = FileAssetContainerUtil.getInstance().getPathFromFullPath("demo.dotcms.com","/application/container/test-container");

        Assert.assertNotNull(relativePath);
        Assert.assertEquals(relativePath, "/application/container/test-container");
    }

    @Test
    public void test_getHost_null_default_expected() throws Exception {

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Host host        = FileAssetContainerUtil.getInstance().getHost(null);

        Assert.assertNotNull(host);
        Assert.assertEquals(defaultHost.getHostname(), host.getHostname());
    }

    @Test
    public void test_getHost_blank_default_expected() throws Exception {

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Host host        = FileAssetContainerUtil.getInstance().getHost("");

        Assert.assertNotNull(host);
        Assert.assertEquals(defaultHost.getHostname(), host.getHostname());
    }

    @Test
    public void test_getHost_ramdom_default_expected() throws Exception {

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Host host        = FileAssetContainerUtil.getInstance().getHost("/xxxxxx");

        Assert.assertNotNull(host);
        Assert.assertEquals(defaultHost.getHostname(), host.getHostname());
    }

    @Test
    public void test_getHost_demo_default_demo() throws Exception {

        //Getting the current default host
        final Host currentDefaultHost = APILocator.getHostAPI()
                .findDefaultHost(APILocator.systemUser(), false);

        try {
            //Create test site and make it default
            final Host site = new SiteDataGen().nextPersisted();
            APILocator.getHostAPI().makeDefault(site, APILocator.systemUser(), false);

            final Host host = FileAssetContainerUtil.getInstance()
                    .getHost("//" + site.getHostname() + "/application/containers/test/");

            Assert.assertNotNull(host);
            Assert.assertEquals(site.getHostname(), host.getHostname());
        } finally {
            APILocator.getHostAPI().makeDefault(currentDefaultHost, APILocator.systemUser(), false);
        }
    }

    /**
     * Method to test: {@link FileAssetContainerUtil#fromAssets(Host, Folder, List, boolean, boolean)}
     * Given Scenario: Creates a container with a default layout
     * ExpectedResult: Expected result that the result should contains the default container layout
     * @throws Exception
     */
    @Test
    public void test_fromAssets_default_layout() throws Exception {

        //Getting the current default host
        final Host currentDefaultHost     = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final String pathRoot   = "/application/containers/test"+System.currentTimeMillis();
        final Folder containerFolder = APILocator.getFolderAPI().createFolders(pathRoot, currentDefaultHost, APILocator.systemUser(), false);
        final String contentTypeVariable  = "testfa"+System.currentTimeMillis();
        ContentType fileAssetContentType  = ImmutableFileAssetContentType.builder()
                .name("FileAssetTestContentType").variable(contentTypeVariable).build();
        fileAssetContentType              = contentTypeAPI.save(fileAssetContentType);
        final List<FileAsset> assets      = Arrays.asList(
                this.createFileAsset("container", ".vtl",
                        "$dotJSON.put(\"title\", \"Test VTL and Valid and Invalid Content Types\")\n" +
                        "$dotJSON.put(\"max_contentlets\", 25)\n" +
                        "$dotJSON.put(\"useDefaultLayout\",\"*\")", containerFolder, fileAssetContentType),
                this.createFileAsset("default_container", ".vtl", "$title", containerFolder, fileAssetContentType));
        final boolean showLive            = false;
        final boolean includeHostOnPath   = false;

        final Container container = FileAssetContainerUtil.getInstance().fromAssets(currentDefaultHost,
                containerFolder, assets, showLive, includeHostOnPath);

        Assert.assertNotNull(container);
        Assert.assertTrue(container instanceof FileAssetContainer);
        Assert.assertEquals(1   , FileAssetContainer.class.cast(container).getContainerStructuresAssets().size());
        Assert.assertNotNull(FileAssetContainer.class.cast(container).getDefaultContainerLayoutAsset());
        Assert.assertEquals("default_container.vtl",FileAssetContainer.class.cast(container).getDefaultContainerLayoutAsset().getFileName());
    }

    private FileAsset createFileAsset (final String fileName1,
                                       final String fileExtension,
                                       final String fileContent,
                                       final Folder root1,
                                       final ContentType fileAssetContentType) throws Exception {

        final FileAsset fileAsset = new FileAsset();
        final File tempFile1      = File.createTempFile(fileName1, fileExtension);
        FileUtil.write(tempFile1, fileContent);
        final String fileNameField1 = fileName1 + fileExtension;

        fileAsset.setContentType(fileAssetContentType);
        fileAsset.setFolder(root1.getInode());
        fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, tempFile1);
        fileAsset.setStringProperty(FileAssetAPI.HOST_FOLDER_FIELD, root1.getInode());
        fileAsset.setStringProperty(FileAssetAPI.TITLE_FIELD, fileNameField1);
        fileAsset.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileNameField1);
        fileAsset.setIndexPolicy(IndexPolicy.FORCE);

        // Create a piece of content for the default host
        return APILocator.getFileAssetAPI().fromContentlet(
                APILocator.getContentletAPI().checkin(fileAsset, user, false));
    }

    /**
     * Method to test: {@link FileAssetContainerUtil#fromAssets(Host, Folder, List, boolean,
     * boolean)} Given Scenario: Creates a container, but deletes the physical file container.vtl
     * ExpectedResult: No Exception should be thrown but the container will be with the default
     * config, so the config set on the container.vtl should not accessible.
     */
    @Test
    public void test_fromAssets_File_Physical_Deleted() throws Exception {

        //Getting the current default host
        final Host currentDefaultHost = APILocator.getHostAPI()
                .findDefaultHost(APILocator.systemUser(), false);
        final String pathRoot = "/application/containers/test" + System.currentTimeMillis();
        final Folder containerFolder = APILocator.getFolderAPI()
                .createFolders(pathRoot, currentDefaultHost, APILocator.systemUser(), false);
        final String contentTypeVariable = "testfa" + System.currentTimeMillis();
        ContentType fileAssetContentType = ImmutableFileAssetContentType.builder()
                .name("FileAssetTestContentType").variable(contentTypeVariable).build();
        fileAssetContentType = contentTypeAPI.save(fileAssetContentType);

        final FileAsset fileAsset = new FileAsset();
        final File tempFile1 = File.createTempFile("container", ".vtl");
        FileUtil.write(tempFile1, "$dotJSON.put(\"title\", \"Test VTL Deleted File NPE\")\n" +
                "$dotJSON.put(\"max_contentlets\", 25)");
        final String fileNameField1 = "container" + ".vtl";
        fileAsset.setContentType(fileAssetContentType);
        fileAsset.setFolder(containerFolder.getInode());
        fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, tempFile1);
        fileAsset.setStringProperty(FileAssetAPI.HOST_FOLDER_FIELD, containerFolder.getInode());
        fileAsset.setStringProperty(FileAssetAPI.TITLE_FIELD, fileNameField1);
        fileAsset.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, fileNameField1);
        fileAsset.setIndexPolicy(IndexPolicy.FORCE);

        final List<FileAsset> assets = Arrays.asList(APILocator.getFileAssetAPI()
                .fromContentlet(APILocator.getContentletAPI().checkin(fileAsset, user, false)));
        File.class.cast(APILocator.getContentletAPI()
                .findContentletByIdentifierAnyLanguage(fileAsset.getIdentifier(), false).getMap()
                .get("fileAsset")).delete();

        final boolean showLive = false;
        final boolean includeHostOnPath = false;

        final Container container = FileAssetContainerUtil.getInstance()
                .fromAssets(currentDefaultHost,
                        containerFolder, assets, showLive, includeHostOnPath);

        Assert.assertNotNull(container);
        Assert.assertTrue(container instanceof FileAssetContainer);
        Assert.assertNotEquals(25, container.getMaxContentlets());
        Assert.assertNotEquals("Test VTL Deleted File NPE", container.getTitle());
    }
}
