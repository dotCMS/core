package com.dotmarketing.portlets.folder.business;

import org.junit.Assert;
import org.junit.Test;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

public class FolderAPITest {
    
    @Test
    public void renameFolder() throws Exception {
        User user = APILocator.getUserAPI().getSystemUser();
        Host demo = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
        Folder ftest = APILocator.getFolderAPI().createFolders("/folderTest"+System.currentTimeMillis(), demo, user, false);
        Folder ftest1 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1", demo, user, false);
        Folder ftest2 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1/ff2", demo, user, false);
        Folder ftest3 = APILocator.getFolderAPI().createFolders(ftest.getPath()+"/ff1/ff2/ff3", demo, user, false);
        
        // get identifiers to cache
        APILocator.getIdentifierAPI().find(ftest);
        APILocator.getIdentifierAPI().find(ftest1);
        APILocator.getIdentifierAPI().find(ftest2);
        APILocator.getIdentifierAPI().find(ftest3);
        
        Assert.assertTrue(APILocator.getFolderAPI().renameFolder(ftest, "folderTestXX"+System.currentTimeMillis(), user, false));
        
        // those should be cleared from cache
        Assert.assertNull(APILocator.getIdentifierAPI().loadFromCache(ftest1.getIdentifier()));
        Assert.assertNull(APILocator.getIdentifierAPI().loadFromCache(ftest2.getIdentifier()));
        Assert.assertNull(APILocator.getIdentifierAPI().loadFromCache(ftest3.getIdentifier()));
        
        // make sure the rename is properly propagated on children (that's done in a db trigger)
        Identifier ident=APILocator.getIdentifierAPI().find(ftest),ident1=APILocator.getIdentifierAPI().find(ftest1),
                ident2=APILocator.getIdentifierAPI().find(ftest2),ident3=APILocator.getIdentifierAPI().find(ftest3);
        Assert.assertTrue(ident.getAssetName().startsWith("folderTestXX"));
        Assert.assertEquals(ident.getPath(),ident1.getParentPath());
        Assert.assertEquals(ident1.getPath(),ident2.getParentPath());
        Assert.assertEquals(ident2.getPath(),ident3.getParentPath());
        
    }

}
