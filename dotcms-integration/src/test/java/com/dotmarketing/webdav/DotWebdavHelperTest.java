package com.dotmarketing.webdav;

import static com.dotcms.unittest.TestUtil.upperCaseRandom;
import static org.mockito.Mockito.when;

import com.bradmcevoy.http.FileResource;
import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.util.FileUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class DotWebdavHelperTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private final DotWebdavHelper helper = new DotWebdavHelper();

    @Test
    public void Test_Get_Folder_Resource_Then_Get_File_Resource() throws IOException, DotDataException, DotSecurityException {
        final SiteDataGen siteDataGen = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host host = siteDataGen.nextPersisted();
        final Folder parent = folderDataGen.site(host).nextPersisted();
        final Folder child = folderDataGen.parent(parent).nextPersisted();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");
        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(child, file);
        fileAssetDataGen.nextPersisted();
        final String folderPath = String.format("http://localhost:8080/webdav/live/1/%s/%s/",host.getName(),parent.getName());
        final Resource folderResource = helper.getResourceFromURL(folderPath);
        Assert.assertNotNull(folderResource);
        Assert.assertTrue(folderResource instanceof FolderResource);
        final String fileResourcePath = String.format("http://localhost:8080/webdav/live/1/%s/%s/%s/%s",host.getName(),parent.getName(),child.getName(),file.getName());
        final Resource fileResource = helper.getResourceFromURL(fileResourcePath);
        Assert.assertNotNull(fileResource);
        Assert.assertTrue(fileResource instanceof FileResource);
    }


    @Test
    public void Test_Get_Folder_Resource_Then_Get_File_Resource_Shuffled_Casing() throws IOException, DotDataException, DotSecurityException {
        final SiteDataGen siteDataGen = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host host = siteDataGen.nextPersisted();
        final Folder parent = folderDataGen.site(host).nextPersisted();
        final Folder child = folderDataGen.parent(parent).nextPersisted();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");
        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(child, file);
        fileAssetDataGen.nextPersisted();
        final String folderPath = upperCaseRandom(String.format("http://localhost:8080/webdav/live/1/%s/%s/",host.getName(),parent.getName()), 8);

        final Resource folderResource = helper.getResourceFromURL(folderPath);
        Assert.assertNotNull(folderResource);
        Assert.assertTrue(folderResource instanceof FolderResource);
        final String fileResourcePath = upperCaseRandom(String.format("http://localhost:8080/webdav/live/1/%s/%s/%s/%s",host.getName(),parent.getName(),child.getName(),file.getName()),8);
        final Resource fileResource = helper.getResourceFromURL(fileResourcePath);
        Assert.assertNotNull(fileResource);
        Assert.assertTrue(fileResource instanceof FileResource);
    }

    @Test
    public void Test_Get_Folder_Resource_For_Non_Existing_Path() throws IOException, DotDataException, DotSecurityException {
        String path = "http://localhost:8080/webdav/live/1/demo.dotcms.com/images/black.png";
        final Resource folderResource = helper.getResourceFromURL(path);
        Assert.assertNull(folderResource);
    }

    @Test
    public void Test_Same_Resource_For_Paths_With_Different_Casing() throws IOException, DotDataException, DotSecurityException {
        final SiteDataGen siteDataGen = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host host = siteDataGen.nextPersisted();
        final Folder parent = folderDataGen.site(host).nextPersisted();
        final Folder child = folderDataGen.parent(parent).nextPersisted();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");
        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(child, file);
        fileAssetDataGen.nextPersisted();
        final String path = String.format("http://localhost:8080/webdav/live/1/%s/%s/%s/%s",host.getName(),parent.getName(),child.getName(),file.getName());
        final String fileResourcePath1 = upperCaseRandom(path,8);
        final String fileResourcePath2 = upperCaseRandom(path,10);
        Assert.assertNotEquals(fileResourcePath1,fileResourcePath2);
        Assert.assertTrue(helper.isSameResourceURL(fileResourcePath1,fileResourcePath2, file.getName()));
    }

    /**
     * Method to test: DotWebdavHelper.setResourceContent
     * <p>
     * Given Scenario: A file is created with no content, then the same file is updated also with no
     * content.
     * <p>
     * ExpectedResult: The file is created and updated without exceptions.
     *
     * @throws Exception if an error occurs while updating the file with no content.
     */
    @Test
    public void Test_publishing_existing_file_with_empty_file_should_not_fail() throws Exception {

        final SiteDataGen siteDataGen = new SiteDataGen();
        final Host host = siteDataGen.nextPersisted();
        final var user = APILocator.getUserAPI().getSystemUser();

        final var resourceUri = String.format("/webdav/live/1/%s/test-file.vtl", host.getName());

        try (MockedStatic<HttpManager> mocked = Mockito.mockStatic(HttpManager.class)) {

            // Create a mocked request object
            Request mockedRequest = Mockito.mock(Request.class);
            when(mockedRequest.getUserAgentHeader()).thenReturn("Cyberduck");
            mocked.when(HttpManager::request).thenReturn(mockedRequest);

            try (InputStream emptyFileInputStream =
                    new ByteArrayInputStream(new byte[0])) {// Empty file

                // First publish, the contentlet should be created
                helper.setResourceContent(resourceUri, emptyFileInputStream, "",
                        null, java.util.Calendar.getInstance().getTime(), user, true);

                // Second publish, the contentlet should be updated without exceptions
                helper.setResourceContent(resourceUri, emptyFileInputStream, "",
                        null, java.util.Calendar.getInstance().getTime(), user, true);

                // Third publish, the contentlet should be updated without exceptions
                helper.setResourceContent(resourceUri, emptyFileInputStream, "",
                        null, java.util.Calendar.getInstance().getTime(), user, true);
            }
        }
    }

}
