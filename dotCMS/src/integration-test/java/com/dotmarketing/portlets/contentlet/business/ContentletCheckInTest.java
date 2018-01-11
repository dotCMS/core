package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.repackage.org.apache.commons.io.FileUtils;

import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.menubuilders.RefreshMenus;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.liferay.portal.model.User;

/**
 * Created by Jonathan Gamba.
 * Date: 3/20/12
 * Time: 12:12 PM
 */
public class ContentletCheckInTest {

    private static ContentletAPI contentletAPI;
    private static FileAssetAPI fileAssetAPI;
    private static FolderAPI folderAPI;
    private static HostAPI hostAPI;
    private static LanguageAPI languageAPI;
    private static ShortyIdAPI shortyIdAPI;
  
  public ContentletCheckInTest() {
      
  }
  
  @BeforeClass
  public static void prepare () throws Exception{

      ContentletBaseTest.prepare();

      hostAPI          = APILocator.getHostAPI();
      folderAPI        = APILocator.getFolderAPI();
      languageAPI      = APILocator.getLanguageAPI();
      contentletAPI    = APILocator.getContentletAPI();
      fileAssetAPI     = APILocator.getFileAssetAPI();
      shortyIdAPI      = APILocator.getShortyAPI();
  }
  
  @Test
  public void checkinInvalidFileContent () throws Exception {

      Folder folder1  = null;

      boolean respectFrontendRoles=false;
      String fileTypeId=APILocator.getContentTypeAPI(APILocator.systemUser()).find("fileAsset").id();
      String uuid1 = shortyIdAPI.randomShorty();
      String uuid3 = shortyIdAPI.randomShorty();
      
      User user = APILocator.systemUser();
      Host host = hostAPI.findDefaultHost(user, false);
      try {
          folder1 = folderAPI.createFolders("/" + uuid1, host, user, false);
          Language lang = languageAPI.getDefaultLanguage();
          folder1.setFilesMasks("*.txt");
          folder1.setDefaultFileType(fileTypeId);
          folderAPI.save(folder1, user, false);

          File file = File.createTempFile(uuid3, ".txt");
          try (FileWriter writer = new FileWriter(file)) {
              while (file.length() < 1024 * 10) {
                  writer.write("Im writing\n");
              }
          }

          FileAsset asset = new FileAsset();
          asset.setLanguageId(lang.getId());
          asset.setBinary("fileAsset", file);
          asset.setTitle(file.getName());
          asset.setFileName(file.getName());
          asset.setContentTypeId(fileTypeId);
          asset.setHost(host.getIdentifier());
          asset.setFolder(folder1.getIdentifier());
          Contentlet con = contentletAPI.checkin(asset, user, true);

          Contentlet con2 = contentletAPI.find(con.getInode(), user, true);

          assert (con.equals(con2));

          FileAsset fileAsset = fileAssetAPI.fromContentlet(con2);

          // this fails becuase the name does not match the folder's file mask
          // but it leaves the original file removed from the index
          fileAssetAPI.renameFile(fileAsset, "test.fail", user, respectFrontendRoles);
          assert (!folderAPI.getContent(folder1, user, respectFrontendRoles)
                  .isEmpty());

      }finally{
          if (folder1 != null) {
              folderAPI.delete(folder1, user, false);
          }
      }
  }

  // Mostly borrowed from com.dotmarketing.portlets.fileassets.business.FileAssetAPIImpl.renameFile(Contentlet, String, User, boolean)
  public void moveImportedAsset(Contentlet contentlet, FileAsset fileAssetCont, String newName) throws DotStateException, DotDataException, DotSecurityException, IOException {
      Identifier id = APILocator.getIdentifierAPI().find(contentlet);
      Host host = APILocator.getHostAPI().find(id.getHostId(), APILocator.getUserAPI().getSystemUser(), false);
      Folder folder = APILocator.getFolderAPI().findFolderByPath(id.getParentPath(), host, APILocator.getUserAPI().getSystemUser(), false);

      if(!APILocator.getFileAssetAPI().fileNameExists(host, folder, newName, id.getId())){                
          File oldFile = contentlet.getBinary(FileAssetAPI.BINARY_FIELD);
          File newFile = new File(oldFile.getPath().substring(0,oldFile.getPath().indexOf(oldFile.getName()))+newName);

          try {
              APILocator.getContentletIndexAPI().removeContentFromIndex(contentlet);

              FileUtils.copyFile(oldFile, newFile);
              contentlet.setInode(null);
              contentlet.setFolder(folder.getInode());
              contentlet.setBinary(FileAssetAPI.BINARY_FIELD, newFile);
              contentlet.setStringProperty(FileAssetAPI.TITLE_FIELD, newName);
              contentlet.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, newName);
              contentlet= APILocator.getContentletAPI().checkin(contentlet, APILocator.getUserAPI().getSystemUser(), false);

              APILocator.getContentletIndexAPI().addContentToIndex(contentlet);

              APILocator.getVersionableAPI().setLive(contentlet);

              RefreshMenus.deleteMenu(folder);
              CacheLocator.getNavToolCache().removeNav(folder.getHostId(), folder.getInode());
              CacheLocator.getIdentifierCache().removeFromCacheByVersionable(contentlet);

          } catch (Exception e) {
              Logger.error(this, "Unable to rename file asset to "+ newName + " for asset " + id.getId(), e);
              throw e;
          } finally {
              if (newFile != null) {
                  FileUtils.deleteQuietly(newFile);
              }
          }
      }
  }

}