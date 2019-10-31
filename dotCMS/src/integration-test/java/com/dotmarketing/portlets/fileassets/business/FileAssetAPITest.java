package com.dotmarketing.portlets.fileassets.business;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotEquals;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.util.List;
import java.util.Optional;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileAssetAPITest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void Test_Modify_Underlying_File_Name_Then_Recover_File_Then_Expect_Match()
            throws Exception {

        final User user = APILocator.systemUser();
        final Folder parentFolder = new FolderDataGen().nextPersisted();

        // Lets make up some fictional data.

        final java.io.File file = java.io.File.createTempFile("blah", ".txt");
        FileUtil.write(file, "helloworld");

        // file paths must be the same at this point.

        final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(parentFolder, file);
        final Contentlet fileAssetContentlet = fileAssetDataGen.nextPersisted();
        final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
        final java.io.File savedFile = (java.io.File) fileAssetContentlet.get("fileAsset");
        final String realPath = fileAssetAPI
                .getRealAssetPath(fileAssetContentlet.getInode(), savedFile.getName());

        assertEquals("file paths must match ", realPath, savedFile.getCanonicalPath());

        final FileAsset fileAsset = fileAssetAPI.fromContentlet(fileAssetContentlet);

        assertEquals("file names must match", fileAsset.getUnderlyingFileName(),
                fileAsset.getName());

        //Now rename the physical file name

        final String newExpectedUnderlyingFileName = "newExpectedUnderlyingFileName";
        fileAsset.setUnderlyingFileName(newExpectedUnderlyingFileName);

        CacheLocator.getContentletCache().remove(fileAsset);

        final List<FileAsset> files = fileAssetAPI
                .findFileAssetsByFolder(parentFolder, user, false);
        final Optional<FileAsset> optional = files.stream()
                .filter(f -> f.getInode().equals(fileAsset.getInode())).findAny();
        assertTrue("file asset is missing ", optional.isPresent());

        final FileAsset recoveredFileAsset = optional.get();

        final String recoveredUnderlyingFileName = recoveredFileAsset.getUnderlyingFileName();
        assertEquals("file names must match", newExpectedUnderlyingFileName,
                recoveredUnderlyingFileName);

        assertNotEquals(recoveredFileAsset.getFileName(),
                recoveredFileAsset.getUnderlyingFileName());

    }


    @Test
    public void Test_Modify_Identifier_File_Name_Then_Recover_File_Then_Expect_Mismatch()
            throws Exception {

        final User user = APILocator.systemUser();
        final Folder parentFolder = new FolderDataGen().nextPersisted();

        // Lets make up some fictional data.

        final java.io.File file = java.io.File.createTempFile("blah", ".txt");
        FileUtil.write(file, "helloworld");

        // file paths must be the same at this point.

        final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(parentFolder, file);
        final Contentlet fileAssetContentlet = fileAssetDataGen.nextPersisted();
        final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();

        final String newExpectedIdentifierName = "newExpectedIdentifierName";

        Contentlet contentlet = APILocator.getContentletAPI()
                .checkout(fileAssetContentlet.getInode(), user, false);
        contentlet.getMap().put(FileAssetAPI.FILE_NAME_FIELD, newExpectedIdentifierName);
        contentlet.setInode(null);

        contentlet = APILocator.getContentletAPI().checkin(contentlet, user, false);
        final FileAsset fileAsset = fileAssetAPI.fromContentlet(contentlet);

        final List<FileAsset> files = fileAssetAPI
                .findFileAssetsByFolder(parentFolder, user, false);
        final Optional<FileAsset> optional = files.stream()
                .filter(f -> f.getIdentifier().equals(fileAsset.getIdentifier())).findAny();
        assertTrue("file asset is missing ", optional.isPresent());

        final FileAsset recoveredFileAsset = optional.get();

        assertNotEquals(recoveredFileAsset.getFileName(),
                recoveredFileAsset.getUnderlyingFileName());
        assertEquals("We expected the recovered content to have the new name ", newExpectedIdentifierName, recoveredFileAsset.getFileName());

        final java.io.File savedFile = (java.io.File) fileAssetContentlet.get("fileAsset");
        final String recoveredRealPath = fileAssetAPI
                .getRealAssetPath(recoveredFileAsset.getInode(),
                        recoveredFileAsset.getUnderlyingFileName());
        assertTrue(
                "Even tough the logical file name has changed the physical file name should remain the same.",
                recoveredRealPath.endsWith(savedFile.getName()));


    }

    @Test
    public void Test_Rename_File_Asset_Then_Recover_File_Then_Expect_Match()
            throws Exception {

        final User user = APILocator.systemUser();
        final Folder parentFolder = new FolderDataGen().nextPersisted();

        // Lets make up some fictional data.

        final java.io.File file1 = java.io.File.createTempFile("blah", ".txt");
        FileUtil.write(file1, "helloworld");
        final long originalSize = file1.length();

        // file paths must be the same at this point.

        final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(parentFolder, file1);
        final Contentlet fileAssetContentlet = fileAssetDataGen.nextPersisted();
        final FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();

        final String newExpectedIdentifierName = "newExpectedIdentifierName";

        //Re-name the identifier the File-asset Logical name. Which is separated from the physical file name.
        assertTrue(fileAssetAPI.renameFile(fileAssetContentlet, newExpectedIdentifierName, user, false));

        final FileAsset fileAsset = fileAssetAPI.fromContentlet(fileAssetContentlet);

        final List<FileAsset> files = fileAssetAPI
                .findFileAssetsByFolder(parentFolder, user, false);
        final Optional<FileAsset> optional = files.stream()
                .filter(f -> f.getIdentifier().equals(fileAsset.getIdentifier())).findAny();
        assertTrue("file asset is missing ", optional.isPresent());

        final FileAsset recoveredFileAsset = optional.get();

        assertNotEquals(recoveredFileAsset.getFileName(),
                recoveredFileAsset.getUnderlyingFileName());
        assertEquals("", newExpectedIdentifierName + ".txt", recoveredFileAsset.getFileName());


    }

  /**
   * file assets should be stored in cache once they've been hydrated. This method tests to make sure
   * that our cache is returning a fileAsset if it has been stored as one
   * 
   * @throws Exception
   */

    @Test
    public void Test_That_File_Asset_Gets_Stored_in_Cache_and_is_Not_Rebuilt_Everytime()
        throws Exception {

      final Folder parentFolder = new FolderDataGen().nextPersisted();

      // Lets make up some fictional data.

      final java.io.File file = java.io.File.createTempFile("blah", ".txt");
      FileUtil.write(file, "helloworld");
      
      final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(parentFolder, file);
      final Contentlet con = fileAssetDataGen.nextPersisted();
  
      
      // content from the content API will be content, first hit
      Contentlet con1 = APILocator.getContentletAPI().find(con.getInode(), APILocator.systemUser(), false);
      assertTrue("Content should not be a file asset", (!(con1 instanceof FileAsset)));
      
      // content from the content cache will be content, first hit
      Contentlet con2 = CacheLocator.getContentletCache().get(con.getInode());
      assertTrue("Contentlet from find comes from cache", con1==con2);
      
      // if you pipe that through the fileAssetAPI, it becomes a new file asset
      FileAsset asset  = APILocator.getFileAssetAPI().find(con.getInode(), APILocator.systemUser(), false);
      assertTrue("FileAsset should be a file asset", (asset instanceof FileAsset));
      
      // it is not just a mutated version of the contentlet
      assertTrue("FileAsset is a new object, not a mutated contentlet", (asset != con2));
      
      
      FileAsset asset2  = APILocator.getFileAssetAPI().find(con.getInode(), APILocator.systemUser(), false);
      assertTrue("FileAssets should be the same Object", asset == asset2);
      
      
      FileAsset asset3  = APILocator.getFileAssetAPI().fromContentlet(asset2);
      assertTrue("FileAssets should be the same Object", asset3 == asset2);
      
      Contentlet asset4 = APILocator.getContentletAPI().find(con.getInode(), APILocator.systemUser(), false);
      assertTrue("Content should not be a file asset", (asset4 instanceof FileAsset));
      assertTrue("FileAssets should be the same Object", asset3 == asset4);
      
    }

}
