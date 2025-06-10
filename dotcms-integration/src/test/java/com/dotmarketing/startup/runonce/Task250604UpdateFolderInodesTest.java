package com.dotmarketing.startup.runonce;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Role;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.UUIDGenerator;
import java.sql.SQLException;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class Task250604UpdateFolderInodesTest extends IntegrationTestBase {

    static Host host;

    @BeforeClass
    public static void setup() throws Exception {

        // Setting web app environment
        IntegrationTestInitService.getInstance().init();

        //make sure that the task has run
        Task250604UpdateFolderInodes task = new Task250604UpdateFolderInodes();
        if(task.forceRun()) {
            task.executeUpgrade();
        }


        host = new SiteDataGen().nextPersisted(true);

        Role role = new RoleDataGen()
                .name("test-role" + UUIDGenerator.shorty())
                .key("test-role" + UUIDGenerator.shorty())
                .description("Test Role Task250604UpdateFolderInodesTest")
                .nextPersisted();



        List<Permission> perms = List.of(
            new Permission(Folder.class.getCanonicalName(),host.getIdentifier(), role.getId(), PermissionLevel.READ.getType()),
            new Permission(Folder.class.getCanonicalName(),host.getIdentifier(), role.getId(), PermissionLevel.EDIT.getType()),
            new Permission(Folder.class.getCanonicalName(),host.getIdentifier(), role.getId(), PermissionLevel.PUBLISH.getType())
        );



        APILocator.getPermissionAPI().save(perms, host, APILocator.systemUser(),false);


    }



    @AfterClass
    public static void killoff() throws Exception {

       // APILocator.getHostAPI().delete(host, APILocator.systemUser(), false);
        DbConnectionFactory.closeSilently();


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



    @CloseDBIfOpened
    private String insertBadData() throws DotDataException, SQLException, DotSecurityException {

        String shorty = UUIDGenerator.shorty();



        new DotConnect().setSQL("INSERT INTO identifier "
                + "(id, parent_path,asset_name,host_inode,asset_type,create_date) VALUES "
                + "(?,?,?,?,?,?)")
                .addParam(badIdentifier(shorty))
                .addParam("/")
                .addParam(badAssetName(shorty))
                .addParam(host.getIdentifier())
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

    @CloseDBIfOpened
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


    @CloseDBIfOpened
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

    @CloseDBIfOpened
    @Test
    public void test_folder_asset_gen() throws Exception {

        Folder folder = new FolderDataGen().site(host).name("test-folder" + UUIDGenerator.shorty()).nextPersisted();


        assertTrue("The inode for a new folder should be the same as the identifier: " + folder.getInode() + " / " + folder.getIdentifier(),
                folder.getInode().equals(folder.getIdentifier()));

        String folderId=new DotConnect()
                .setSQL("SELECT id FROM identifier WHERE id = ?")
                .addParam(folder.getIdentifier())
                .loadObjectResults().get(0).get("id").toString();


        String folderInode=new DotConnect()
                .setSQL("SELECT inode FROM folder WHERE inode = ?")
                .addParam(folder.getIdentifier())
                .loadObjectResults().get(0).get("inode").toString();


        assertTrue("The identifier should match the identifier table:" + folder.getIdentifier() + " / " + folderId,
                folder.getIdentifier().equals(folderId));


        assertTrue("The identifier should match the folder.inode table:",
                folder.getIdentifier().equals(folderInode));

        DbConnectionFactory.closeSilently();


    }


    /**
     * This is an important test to ensure that folders whose identifiers are
     * updated by the startup task maintain any individual permissions that have been set on them.
     * @throws Exception
     */

    @CloseDBIfOpened
    @Test
    public void test_folders_whose_ids_are_updated_maintain_permissions() throws Exception {

        // create a bad folder with an identifier and inode that do not match
        String badShorty = insertBadData();

        // load bad folder
        Folder folder = APILocator.getFolderAPI().find(badIdentifier(badShorty), APILocator.systemUser(), false);

        final String originalFolderId = folder.getIdentifier();

        assertNotNull("The folder should not be null", folder.getIdentifier());

        // assign permissions to the bad folder
        Permissionable parent = APILocator.getPermissionAPI().findParentPermissionable(folder);
        APILocator.getPermissionAPI().permissionIndividually(parent,folder,APILocator.systemUser());


        // test that the bad folder has permissions
        List<Permission> perms = APILocator.getPermissionAPI().getPermissions(folder);
        int origPermSize = perms.size();
        assertTrue("The folder should have permissions", origPermSize==3);


        // run task to fix the bad folder data
        Task250604UpdateFolderInodes task = new Task250604UpdateFolderInodes();

        // Check that it should run now
        assertTrue(task.forceRun());

        // Run the task
        task.executeUpgrade();

        // Check that the task has fixed the bad data
        assertFalse(task.forceRun());


        // load the folder again, it should have a new identifier
        folder = APILocator.getFolderAPI().find(badInode(badShorty), APILocator.systemUser(), false);

        assertNotNull("The newFolder should not be null", folder.getIdentifier());

        assertNotEquals("The folder should have a new identifier", originalFolderId, folder.getIdentifier());


        // load the permissions for the newly fixed folder, should have the same permissions as before
        perms = APILocator.getPermissionAPI().getPermissions(folder);

        assertTrue("The folder should have permissions", !perms.isEmpty());

        assertTrue("The folder should have the same number of permissions as before", perms.size() == origPermSize);



    }



    @CloseDBIfOpened
    @Test
    public void test_trying_to_save_bad_folder() throws Exception {

        String badShorty = UUIDGenerator.shorty();
        String inode = badInode(badShorty);
        String identifier = badIdentifier(badShorty);

        Identifier id = new Identifier();
        id.setId(identifier);
        id.setAssetType("folder");
        id.setHostId(host.getIdentifier());
        id.setAssetName(badAssetName(badShorty));
        id.setCreateDate(new java.util.Date());
        id.setParentPath("/");
        APILocator.getIdentifierAPI().save(id);





        Folder folder = new Folder();
        folder.setInode(inode);
        folder.setIdentifier(identifier);
        folder.setName(badAssetName(badShorty));
        folder.setTitle(badAssetName(badShorty));
        folder.setOwner(APILocator.getUserAPI().getSystemUser().getUserId());
        folder.setHostId(host.getIdentifier());
        folder.setModDate(new java.util.Date());
        folder.setIDate(new java.util.Date());



        APILocator.getFolderAPI().save(folder, APILocator.systemUser(), false);

        // assert that we have not added any bad data
        assertFalse(new Task250604UpdateFolderInodes().forceRun());



    }



}
