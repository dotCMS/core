package com.dotmarketing.portlets.contentlet.business;

import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.FileWriter;
import org.junit.BeforeClass;
import org.junit.Test;

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

      final boolean respectFrontendRoles=false;
      final String fileTypeId=APILocator.getContentTypeAPI(APILocator.systemUser()).find("fileAsset").id();
      final String uuid1 = shortyIdAPI.randomShorty();
      final String uuid3 = shortyIdAPI.randomShorty();
      
      final User user = APILocator.systemUser();
      final Host host = hostAPI.findDefaultHost(user, false);
      try {
          folder1 = folderAPI.createFolders("/" + uuid1, host, user, false);
          final Language lang = languageAPI.getDefaultLanguage();
          folder1.setFilesMasks("*.txt");
          folder1.setDefaultFileType(fileTypeId);
          folderAPI.save(folder1, user, false);

          File file = File.createTempFile(uuid3, ".txt");
          try (FileWriter writer = new FileWriter(file)) {
              while (file.length() < 1024 * 10) {
                  writer.write("Im writing\n");
              }
          }

          final FileAsset asset = new FileAsset();
          asset.setLanguageId(lang.getId());
          asset.setBinary("fileAsset", file);
          asset.setTitle(file.getName());
          asset.setFileName(file.getName());
          asset.setContentTypeId(fileTypeId);
          asset.setHost(host.getIdentifier());
          asset.setFolder(folder1.getIdentifier());
          final Contentlet con = contentletAPI.checkin(asset, user, true);

          final Contentlet con2 = contentletAPI.find(con.getInode(), user, true);

          assert (con.equals(con2));

          final FileAsset fileAsset = fileAssetAPI.fromContentlet(con2);

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
}