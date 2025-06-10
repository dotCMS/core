package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UUIDGenerator;
import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;


public class Task250604UpdateFolderInodesTest extends IntegrationTestBase {

    Host host;

    @BeforeClass
    public static void setup() throws Exception {

        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        //make sure that the task has run
        Task250604UpdateFolderInodes task = new Task250604UpdateFolderInodes();
        if(task.forceRun()) {
            task.executeUpgrade();
        }
    }




    String badIdentifier(String shorty) {
        return "bad-identifier-" + shorty;
    }
    String badInode(String shorty) {
        return "bad-inode-" + shorty;
    }
    String badAssetName(String shorty) {
        return "bad-asset-name-" + shorty;
    }



    @WrapInTransaction
    private String insertBadData() throws DotDataException, SQLException, DotSecurityException {

        String shorty = UUIDGenerator.shorty();

        Host host = APILocator.getHostAPI().findDefaultHost(APILocator.getUserAPI().getSystemUser(), false);

        new DotConnect().setSQL("INSERT INTO identifier "
                + "(id, parent_path,asset_name,host_inode,asset_type,create_date) VALUES "
                + "(?,?,?,?,?,?)")
                .addParam(badIdentifier(shorty))
                .addParam("/")
                .addParam(badAssetName(shorty))
                .addParam(host.getInode())
                .addParam("folder")
                .addParam(new java.util.Date())
                .loadResult();




        new DotConnect().setSQL("INSERT INTO folder (inode, identifier,name,title,mod_date,idate,owner) VALUES (?, ?, ?, ?, ?, ?, ?)")
                .addParam(badInode(shorty))
                .addParam(badIdentifier(shorty))
                .addParam( badAssetName(shorty))
                .addParam("bad-title-" + shorty)
                .addParam(new java.util.Date())
                .addParam(new java.util.Date())
                .addParam(APILocator.getUserAPI().getSystemUser().getUserId())
                .loadResult();

        return shorty;

    }


    @Test
    public void testTask250604UpdateFolderInodes() throws Exception {

        // Check that we are starting with a clean slate
        Task250604UpdateFolderInodes task = new Task250604UpdateFolderInodes();
        assertFalse(task.forceRun());

        // Insert bad data
        String shorty = insertBadData();


        // Check that it should run now
        assertTrue(task.forceRun());


        // Run the task
        task.executeUpgrade();

        // Check that the task has fixed the bad data
        assertFalse(task.forceRun());

    }


    @Test
    public void test_system_folder_works() throws Exception {

        String inode = new DotConnect()
                .setSQL("SELECT inode FROM folder WHERE inode = '"+ Folder.SYSTEM_FOLDER+"'")
                .getString("inode");


        assertTrue("The inode for the system folder should be 'SYSTEM_FOLDER', but was: " + inode,  Folder.SYSTEM_FOLDER.equals(inode));



        String identifier = new DotConnect()
                .setSQL("SELECT identifier FROM folder WHERE identifier = '"+ Folder.SYSTEM_FOLDER+"'")
                .getString("identifier");
        assertTrue("The identifier for the system folder should be 'SYSTEM_FOLDER', but was: " + identifier,  Folder.SYSTEM_FOLDER.equals(identifier));


    }


    @Test
    public void test_folder_asset_gen() throws Exception {




    }




}
