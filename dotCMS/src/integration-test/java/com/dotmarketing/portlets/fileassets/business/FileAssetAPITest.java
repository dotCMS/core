package com.dotmarketing.portlets.fileassets.business;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;

public class FileAssetAPITest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
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
    
    /**
     * This tests that file assets saved to a random folder, e.g. /dasfasd/ will be returned from the database
     * @throws Exception
     */
    @Test
    public void test_that_file_asset_from_db_works()
            throws Exception {

        final User user = APILocator.systemUser();
        final Folder parentFolder = new FolderDataGen().nextPersisted();
    
        List<String> fileNames = new ArrayList<>();
        final int fileAssetSize=3;
        
        for(int i=0;i<fileAssetSize;i++) {
            final java.io.File file = java.io.File.createTempFile("blah" + i, ".txt");
            fileNames.add(file.getName());
            FileUtil.write(file, "helloworld");
            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(parentFolder, file);
            fileAssetDataGen.setPolicy(IndexPolicy.DEFER);
            fileAssetDataGen.nextPersisted();
        }
        
        FileAssetSearcher searcher = FileAssetSearcher.builder().user(user).folder(parentFolder).respectFrontendRoles(false).build();
                        

        List<FileAsset> assets = APILocator.getFileAssetAPI().findFileAssetsByDB(searcher);
        assert(assets.size()==fileAssetSize);
        assets.forEach(a-> {
            assert(fileNames.contains(a.getFileName()));
        });

    }
    
    /**
     * This tests that file assets saved to a random folder, e.g. /dasfasd/ will be returned from the database
     * @throws Exception
     */
    @Test
    public void test_that_file_asset_from_db_respects_live_working_flag()
            throws Exception {

        final User user = APILocator.systemUser();
        final Folder parentFolder = new FolderDataGen().nextPersisted();
    
        final List<String> fileNames = new ArrayList<>();
        final int fileAssetSize=3;
        
        for(int i=0;i<fileAssetSize;i++) {
            final java.io.File file = java.io.File.createTempFile("blah" + i, ".txt");
            fileNames.add(file.getName());
            FileUtil.write(file, "helloworld");
            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(parentFolder, file);
            fileAssetDataGen.setPolicy(IndexPolicy.DEFER);
            fileAssetDataGen.nextPersisted();
        }
        
        FileAssetSearcher searcher = FileAssetSearcher.builder().user(user).folder(parentFolder).respectFrontendRoles(false).build();
                        
        // we have all the working files
        assert(APILocator.getFileAssetAPI().findFileAssetsByDB(searcher).size() == fileAssetSize);

        
        
        searcher = FileAssetSearcher.builder().live(true).user(user).folder(parentFolder).respectFrontendRoles(false).build();
        
        
        // there are no live files
        assert(APILocator.getFileAssetAPI().findFileAssetsByDB(searcher).size() == 0);


        
        
        
    }
    
    
    
    
    /**
     * This tests that file assets saved to a folder are not readable unless the user has read permissions to the parent folder itself
     * @throws Exception
     */
    @Test
    public void test_that_file_asset_from_db_respects_folder_permissions()
            throws Exception {

        final User user = APILocator.getUserAPI().getAnonymousUser();
        final Folder parentFolder = new FolderDataGen().nextPersisted();
    
        List<String> fileNames = new ArrayList<>();
        final int fileAssetSize=3;
        
        for(int i=0;i<fileAssetSize;i++) {
            final java.io.File file = java.io.File.createTempFile("blah" + i, ".txt");
            fileNames.add(file.getName());
            FileUtil.write(file, "helloworld");
            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(parentFolder, file);
            fileAssetDataGen.setPolicy(IndexPolicy.DEFER);
            fileAssetDataGen.nextPersisted();
        }
        
        FileAssetSearcher searcher = FileAssetSearcher.builder().user(user).folder(parentFolder).respectFrontendRoles(false).build();
                        

        try {
            APILocator.getFileAssetAPI().findFileAssetsByDB(searcher);
        }
        catch(DotRuntimeException e) {
            assertTrue("We should have thrown a DotRuntimeException", e!=null);
            assertTrue("This should have a good message", e.getMessage().contains("does not have permission to view the parent folder"));
            return;
        }

        assertTrue("this should have thrown a DotRuntimeException", false);

    }
    
    /**
     * This tests that file assets saved to the system folder, e.g. / will be returned from the database
     * as exepcteds
     * 
     * @throws Exception
     */
    @Test
    public void test_that_we_return_files_from_host_system_folder() throws Exception {

        final User user = APILocator.systemUser();
        final Folder parentFolder = APILocator.getFolderAPI().findSystemFolder();

        List<String> fileNames = new ArrayList<>();
        final int fileAssetSize = 3;

        for (int i = 0; i < fileAssetSize; i++) {
            final java.io.File file = java.io.File.createTempFile("blah" + i, ".txt");
            fileNames.add(file.getName());
            FileUtil.write(file, "helloworld");
            final FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(parentFolder, file);
            fileAssetDataGen.setPolicy(IndexPolicy.DEFER);
            fileAssetDataGen.nextPersisted();
        }

        FileAssetSearcher searcher = FileAssetSearcher.builder().user(user)
                        .host(APILocator.getHostAPI().findDefaultHost(user, false)).respectFrontendRoles(false).build();

        List<String> assetNames = APILocator.getFileAssetAPI().findFileAssetsByDB(searcher).stream()
                        .map(c -> c.getFileName()).collect(Collectors.toList());


        assert (assetNames.size() > -fileAssetSize);
        fileNames.forEach(f -> {
            assert (assetNames.contains(f));
        });

    }

}
