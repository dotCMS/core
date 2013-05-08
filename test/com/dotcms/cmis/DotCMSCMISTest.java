package com.dotcms.cmis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.util.List;

import org.apache.chemistry.opencmis.commons.data.ObjectList;
import org.junit.Test;

import com.dotcms.enterprise.cmis.utils.CMISUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;

public class DotCMSCMISTest extends CMISBaseTest {

    @Test
    public void testCMISReadWrite () throws Exception {
    	Host dhost=APILocator.getHostAPI().findDefaultHost(user, false);
    	
        //Validations
        assertTrue( UtilMethods.isSet(getdefaultHostId()) && 
        		APILocator.getHostAPI().findDefaultHost(
        				APILocator.getUserAPI().getSystemUser(), false).getInode().equals(getdefaultHostId()));
        
        final String fname="CMISJunitTest" + new java.util.Date().getTime();
        String folderId = createFolder(fname); 
      //Validations
        assertNotNull( folderId );
        
        Folder newf = APILocator.getFolderAPI().findFolderByPath("/"+fname, dhost, user, false);
        assertNotNull(newf);
        assertTrue(InodeUtils.isSet(newf.getInode()));
        assertEquals(newf.getInode(),folderId);
        
        assertNotNull(createFile("test.txt", folderId));
        
        List<FileAsset> files = APILocator.getFileAssetAPI().findFileAssetsByFolder(newf, "", false, user, false);
        assertNotNull(files);
        assertEquals(1,files.size());
        assertEquals("test.txt",files.get(0).getFileName());
        
        assertTrue( ! doQuery("SELECT * FROM cmis:document WHERE cmis:name LIKE '%a%'").getNumItems().equals(BigInteger.valueOf(0)));
        
        assertTrue( ! doQuery("SELECT * FROM cmis:folder WHERE IN_FOLDER('" + CMISUtils.ROOT_ID + "')").getNumItems().equals(BigInteger.valueOf(0)));
        
        // testing if we can query a fresh contentlet
        
        Structure st=StructureCache.getStructureByVelocityVarName("fileAsset");
        Contentlet cont=new Contentlet();
        cont.setStructureInode(st.getInode());
        cont.setHost(dhost.getIdentifier());
        cont.setFolder(folderId);
        final String title = "cmis-file-"+UUIDGenerator.generateUuid()+".txt";
        cont.setStringProperty("title", title);
        cont.setStringProperty("fileName", title);
        File tmp=new File(APILocator.getFileAPI().getRealAssetPathTmpBinary()+File.separator+"cmis");
        if(!tmp.isDirectory()) tmp.mkdirs();
        File file=new File(tmp,title);
        file.createNewFile();
        FileWriter writer=new FileWriter(file,true);
        writer.write("this is the content of the file");
        writer.flush(); writer.close();
        cont.setBinary("fileAsset", file);
        cont.setLanguageId(1);
        cont = APILocator.getContentletAPI().checkin(cont,user,false);
        APILocator.getContentletAPI().isInodeIndexed(cont.getInode());
        
        ObjectList list = doQuery("SELECT * FROM cmis:document WHERE cmis:name='"+title+"'");
        assertEquals(BigInteger.valueOf(1),list.getNumItems());
        assertEquals(cont.getInode(),list.getObjects().get(0).getId());
    }
}
