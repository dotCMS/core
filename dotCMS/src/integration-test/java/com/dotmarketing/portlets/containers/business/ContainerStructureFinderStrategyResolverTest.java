package com.dotmarketing.portlets.containers.business;

import static org.junit.Assert.assertEquals;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.business.ContainerStructureFinderStrategyResolver.PathContainerStructureFinderStrategyImpl;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.util.FileUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ContainerStructureFinderStrategyResolverTest {


    @BeforeClass
    public static void prepare() throws Exception {

        IntegrationTestInitService.getInstance().init();
    }


    /**
     * This tests that file based container content types are based on the file name, and not the
     * contentlet title. We create a file based container and verify the content type name. Then we
     * change the title on the contentlet (bt do not change the file name) which should not make a
     * difference. The content type returned should still be the same
     * 
     * @throws Exception
     */
    @Test
    public void get_container_structure_from_file_name_() throws Exception {

        Folder folder = APILocator.getFolderAPI().findSystemFolder();
        java.io.File file = java.io.File.createTempFile("texto", ".vtl");
        FileUtil.write(file, "helloworld");

        final String fileName = file.getName().replace(".vtl", "");
        Contentlet con = new FileAssetDataGen(folder, file).nextPersisted();
        FileAsset fileAsset = APILocator.getFileAssetAPI().fromContentlet(con);
        ContainerStructureFinderStrategyResolver a = new ContainerStructureFinderStrategyResolver();
        PathContainerStructureFinderStrategyImpl longNamedStrategy =
                        new ContainerStructureFinderStrategyResolver().new PathContainerStructureFinderStrategyImpl();

        assertEquals(longNamedStrategy.getVelocityVarName(fileAsset), fileName);

        // change the title, it should still work
        fileAsset.setTitle("thisIsNotMyName.vtl");
        fileAsset.setInode(null);
        con = APILocator.getContentletAPI().checkin(fileAsset, APILocator.systemUser(), false);
        fileAsset = APILocator.getFileAssetAPI().fromContentlet(con);

        assertEquals(longNamedStrategy.getVelocityVarName(fileAsset), fileName);
    }

    /**
     * Method to test: {@link FileAssetContainerUtil#fromAssets(Host, Folder, List, boolean, boolean)}
     * Given Scenario: Creates a container with a default layout
     * ExpectedResult: Expected result that the result should contains the default container layout
     * @throws Exception
     */
    @Test
    public void test_default_layout() throws Exception {

        //Getting the current default host
        final String pathRoot             = "/application/containers/test"+System.currentTimeMillis();
        final Host currentDefaultHost     = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Folder containerFolder      = this.createAppContainerTest(currentDefaultHost, pathRoot);
        final String contentTypeVariable  = "testfa"+System.currentTimeMillis();
        ContentType fileAssetContentType  = ImmutableFileAssetContentType.builder()
                .name("FileAssetTestContentType").variable(contentTypeVariable).build();
        fileAssetContentType              = APILocator.getContentTypeAPI(APILocator.systemUser()).save(fileAssetContentType);
        final List<FileAsset> assets      = Arrays.asList(
                this.createFileAsset("container", ".vtl",
                        "$dotJSON.put(\"title\", \"Test No VTL and All Content Types\")\n" +
                                "$dotJSON.put(\"max_contentlets\", 25)\n" +
                                "$dotJSON.put(\"useDefaultLayout\",\"*\")", containerFolder, fileAssetContentType),
                this.createFileAsset("default_container", ".vtl", "$title", containerFolder, fileAssetContentType));

        final ContainerStructureFinderStrategyResolver containerStructureFinderStrategyResolver =
                new ContainerStructureFinderStrategyResolver();

        final Container container = APILocator.getContainerAPI()
                .getWorkingContainerByFolderPath(pathRoot, currentDefaultHost, APILocator.systemUser(), false);
        final Optional<ContainerStructureFinderStrategy> containerStructureFinderStrategy =
                containerStructureFinderStrategyResolver.get(container);

        Assert.assertTrue(containerStructureFinderStrategy.isPresent());

        final  List<ContainerStructure> containerStructures = containerStructureFinderStrategy.get().apply(container);

        Assert.assertNotNull(containerStructures);
        Assert.assertFalse(containerStructures.isEmpty());

        final List<ContentType> contentTypes = APILocator.getContentTypeAPI(APILocator.systemUser()).findAll();
        for (final ContentType contentType : contentTypes) {

            Assert.assertTrue("Content Type: " + contentType.variable() + " does not included on the container",
                    containerStructures.stream().anyMatch(containerStructure -> containerStructure.getStructureId().equals(contentType.id())));
        }
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
                APILocator.getContentletAPI().checkin(fileAsset, APILocator.systemUser(), false));
    }

    private Folder createAppContainerTest(final Host currentDefaultHost, final String pathRoot) throws DotDataException, DotSecurityException {

        final Folder rootFolder = APILocator.getFolderAPI().createFolders(pathRoot, currentDefaultHost, APILocator.systemUser(), false);
        return rootFolder;
    }

}
