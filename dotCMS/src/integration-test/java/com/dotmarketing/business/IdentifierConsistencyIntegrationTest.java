package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class IdentifierConsistencyIntegrationTest extends IntegrationTestBase {

    private static final String INSERT_IDENTIFIER_SQL = "INSERT INTO identifier (parent_path,asset_name,host_inode,asset_type,syspublish_date,sysexpire_date,id) values (?,?,?,?,?,?,?)";
    private static final String UPDATE_IDENTIFIER_SQL = "UPDATE identifier set parent_path=?, asset_name=?, host_inode=?, asset_type=?, syspublish_date=?, sysexpire_date=? where id=?";
    private static final String DELETE_IDENTIFIER_SQL = "DELETE FROM identifier where id=?";
    private static final String UPDATE_FOLDER = "UPDATE folder SET name = ? WHERE identifier = ? AND inode = ?";
    private static Host host;
    private static final String CONTENTLET = "contentlet";
    private static final String FOLDER = "folder";
    private static final String UPDATE_IDENTIFIER_FAIL = "updateIdentifier failed: ";

    @BeforeClass
    public static void init() throws Exception {
        IntegrationTestInitService.getInstance().init();
        host = createNewHost();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (null != host) {
            final User user = APILocator.systemUser();
            APILocator.getHostAPI().archive(host, user, false);
            APILocator.getHostAPI().delete(host, user, false);
        }
    }

    private static Host createNewHost() throws DotDataException, DotSecurityException {
        final long defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final User user = APILocator.systemUser();
        Host host2 = new Host();
        host2.setHostname("my.testsite-" + System.currentTimeMillis() + ".com");
        host2.setDefault(false);
        host2.setLanguageId(defaultLanguage);

        host2.setIndexPolicy(IndexPolicy.FORCE);
        host2 = APILocator.getHostAPI().save(host2, user, false);
        return host2;
    }


    @Test(expected = DotDataException.class)
    public void Test_Identifier_Insert_Case_Insensitive_Dupe_Asset_Name()
            throws DotDataException, DotRuntimeException, DotSecurityException {
        Identifier newAsset = null;
        final String prefix = System.currentTimeMillis() + "_";
        try {
            newAsset = insertIdentifier(prefix + "assetName", "/", CONTENTLET);
            insertIdentifier(prefix + "assetname", "/", CONTENTLET);
        } finally {
           if(newAsset != null){
              deleteIdentifier(newAsset.getId());
           }
        }
    }

    @Test(expected = DotDataException.class)
    public void Test_Identifier_Update_Dupe_Asset_Name_Expect_Name_Collision()
            throws DotDataException, DotRuntimeException {

        final Identifier identifier1 = insertIdentifier("anyAssetName", "/", CONTENTLET);
        final Identifier identifier2 = insertIdentifier("nonConflictingName", "/", CONTENTLET);
        try{
            updateIdentifier(identifier2, "anyassetname", "/");
        }finally{
            if (null != identifier1) {
                deleteIdentifier(identifier1.getId());
            }
            if (null != identifier2) {
                deleteIdentifier(identifier2.getId());
            }
        }
    }



    @Test
    public void Test_Identifier_Create_Folder_Tree_Then_Update_Update_Identifier_Non_existing_Path()
            throws DotDataException, DotSecurityException, SQLException {
        final String prefix = System.currentTimeMillis() + "_";
        final FolderAPI folderAPI = APILocator.getFolderAPI();
        final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
        final User user = APILocator.systemUser();
        final String rootFolderName = String.format("/%sroot", prefix);
        Folder root = null;

        root = folderAPI.createFolders(rootFolderName, host, user, false);

        final Folder folder1 = folderAPI
                .createFolders(root.getPath() + "child-1/child-1-1/child-1-1-1/child-1-1-1-1",
                        host, user, false);

        Logger.info(getClass(), () -> "Path1: " + folder1.getPath());

        final Folder folder2 = folderAPI
                .createFolders(root.getPath() + "child-2/child-2-1/child-2-1-1/child-2-1-1-1",
                        host, user, false);

        Logger.info(getClass(), () -> "Path2: " + folder2.getPath());

        final Folder f3 = folderAPI
                .createFolders(root.getPath() + "child-3/child-3-1/child-3-1-1/child-3-1-1-1",
                        host, user, false);

        Logger.info(getClass(), () -> "Path3: " + f3.getPath());

        final Folder testTargetFolder = folderAPI
                .findFolderByPath(root.getPath() + "child-3/child-3-1", host, user,
                        false);

        assertNotNull(testTargetFolder);

        Logger.info(getClass(), () -> "Path4: " + testTargetFolder.getPath());

        final Identifier testFolderIdentifier = identifierAPI
                .find(testTargetFolder.getIdentifier());

        assertEquals("should have been a folder", "folder", testFolderIdentifier.getAssetType());

        try {
            // Tests actually happen HERE!

            try {
                final String invalidParentPath = root.getPath() + "lol/";
                updateIdentifier(testFolderIdentifier, testFolderIdentifier.getAssetName(), invalidParentPath);
                fail("Parent '" + invalidParentPath
                        + "' does not exist and the update should have failed");
            } catch (DotDataException e) {
                Logger.info(getClass(),
                        () -> "This exception is expected: " + e.getMessage());
            }

        } finally {
                CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(testFolderIdentifier.getId());
                try {
                    Logger.info(getClass(), () -> "Running Cleanup! ");
                    folderAPI.delete(root, user, false);
                } catch (Exception e) {
                    e.printStackTrace();
                    Logger.error(getClass(), "Error running cleanup routine.", e);
                }

        }
    }

    //This Test Fails Because there is no such thing as a trigger in charge of renaming all sub parent_path identifiers when an identifier is updated directly
    //Updating the directly the identifier table is a bad idea.
    @Ignore
    @Test
    public void Test_Rename_Asset_Expect_SubPaths_Get_Renamed()
            throws DotDataException, DotSecurityException, SQLException {

        final String prefix = System.currentTimeMillis() + "_";
        final FolderAPI folderAPI = APILocator.getFolderAPI();
        final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
        final User user = APILocator.systemUser();
        final String rootFolderName = String.format("/%sroot", prefix);
        Folder root = null;
        try {
            root = folderAPI.createFolders(rootFolderName, host, user, false);

            final Folder f1 = folderAPI
                    .createFolders(root.getPath() + "level-1/level-2/level-3/",
                            host, user, false);

            final List<Identifier> identifiers1 = identifierAPI
                    .findByURIPattern(Identifier.ASSET_TYPE_FOLDER,
                            root.getPath() + "level-1/level-2", true, host);

             assertEquals(1, identifiers1.size());

            final List<Identifier> identifiers2 = identifierAPI
                    .findByURIPattern(Identifier.ASSET_TYPE_FOLDER,
                            root.getPath() + "level-1/level-2/level-3", true, host);

            final Identifier level2Identifier = identifiers1.get(0);
            final Identifier level3Identifier = identifiers2.get(0);

            level2Identifier.setAssetName("level2");
            identifierAPI.save(level2Identifier);

            final Identifier updatedLevel3 = identifierAPI.find(level3Identifier.getId());
            assertTrue(updatedLevel3.getParentPath().contains("level2"));

        } finally {
            if(null != root){
             folderAPI.delete(root, user, false);
            }
        }
    }

    @Test(expected = DotDataException.class)
    public void Test_Identifier_Insert_Subfolder_Expect_Name_Collision()
            throws DotDataException, DotSecurityException {
        Identifier parentFolderIdentifier = null;
        Identifier subFolderIdentifier = null;
        try {
            final String prefix = System.currentTimeMillis() + "_";
            final String parentFolderName = String.format("/%sanyFolder", prefix);
            parentFolderIdentifier = insertIdentifier(parentFolderName, "/", FOLDER);
            subFolderIdentifier = insertIdentifier("subFolder", parentFolderIdentifier.getPath(), FOLDER);
            insertIdentifier("SUBFOLDER", parentFolderIdentifier.getPath(), FOLDER);
            fail("should have failed inserting a subfolder.");
        } finally {
            if (null != subFolderIdentifier) {
                try {
                    deleteIdentifier(subFolderIdentifier.getId());
                } catch (Exception e) {
                    Logger.error(getClass(), UPDATE_IDENTIFIER_FAIL, e);
                }
            }
            if (null != parentFolderIdentifier) {
                try {
                    deleteIdentifier(parentFolderIdentifier.getId());
                } catch (Exception e) {
                    Logger.error(getClass(), UPDATE_IDENTIFIER_FAIL, e);
                }
            }
        }
    }

    @Test(expected = DotDataException.class)
    public void Test_Identifier_Insert_Subfolder_Then_Update_Expect_Name_Collision()
            throws DotDataException {
        Identifier parentFolderIdentifier = null;
        Identifier subFolderIdentifier1 = null;
        Identifier subFolderIdentifier2 = null;
        try {
            final String prefix = System.currentTimeMillis() + "_";
            final String parentFolderName = String.format("/%sanyFolder", prefix);
            parentFolderIdentifier = insertIdentifier(parentFolderName, "/", FOLDER);
            subFolderIdentifier1 = insertIdentifier("subFolder-1", parentFolderIdentifier.getPath(), FOLDER);
            subFolderIdentifier2 = insertIdentifier("subFolder-2", parentFolderIdentifier.getPath(), FOLDER);

            updateIdentifier(subFolderIdentifier2,"subFolder-1", parentFolderIdentifier.getPath());

            fail("should have failed updating subfolder for the name's already taken.");
        } finally {
            if (null != subFolderIdentifier1) {
                try {
                    deleteIdentifier(subFolderIdentifier1.getId());
                } catch (Exception e) {
                    Logger.error(getClass(), UPDATE_IDENTIFIER_FAIL, e);
                }
            }
            if (null != subFolderIdentifier2) {
                try {
                    deleteIdentifier(subFolderIdentifier2.getId());
                } catch (Exception e) {
                    Logger.error(getClass(), UPDATE_IDENTIFIER_FAIL, e);
                }
            }

            if (null != parentFolderIdentifier) {
                try {
                    deleteIdentifier(parentFolderIdentifier.getId());
                } catch (Exception e) {
                    Logger.error(getClass(), UPDATE_IDENTIFIER_FAIL, e);
                }
            }
        }
    }

    @Test(expected = DotDataException.class)
    public void Test_Identifier_Delete_Parent_Folder_With_Existing_Children_Expect_Error()
            throws DotDataException, DotSecurityException {
        Identifier parentFolderIdentifier = null;
        Identifier subFolderIdentifier = null;
        try {
            final String prefix = System.currentTimeMillis() + "_";
            final String parentFolderName = String.format("%sparentFolder", prefix);
            parentFolderIdentifier = insertIdentifier(parentFolderName, "/", FOLDER);
            subFolderIdentifier = insertIdentifier("subFolder", parentFolderIdentifier.getPath(), FOLDER);

            deleteIdentifier(parentFolderIdentifier.getId());

        } finally {
            if (null != subFolderIdentifier) {
                try {
                    deleteIdentifier(subFolderIdentifier.getId());
                } catch (Exception e) {
                    Logger.error(getClass(), UPDATE_IDENTIFIER_FAIL, e);
                }
            }
            if (null != parentFolderIdentifier) {
                try {
                    deleteIdentifier(parentFolderIdentifier.getId());
                } catch (Exception e) {
                    Logger.error(getClass(), UPDATE_IDENTIFIER_FAIL, e);
                }
            }
        }
    }


    @Test
    public void Test_Create_Folder_Tree_Then_Rename_Parent_Verify_Children_Are_Renamed()
            throws DotDataException, DotSecurityException {
        final String prefix = System.currentTimeMillis() + "_";
        final FolderAPI folderAPI = APILocator.getFolderAPI();
        final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
        final User user = APILocator.systemUser();
        final String rootFolderName = String.format("/%slevel-0", prefix);
        Folder folder0,folder1,folder2,folder3;
        folder0 = folder1 = folder2 = folder3 = null;
        try {
             folder0 = folderAPI
                    .createFolders(rootFolderName, host, user, false);

             folder1 = folderAPI
                    .createFolders(folder0.getPath() + "level-1", host, user, false);

             folder2 = folderAPI
                    .createFolders(folder1.getPath() + "level-2", host, user, false);

             folder3 = folderAPI
                    .createFolders(folder2.getPath() + "level-3", host, user, false);

            final String newFolderName = "newName";
            updateFolder(folder1.getIdentifier(), folder1.getInode(), newFolderName);
            final Identifier identifier = identifierAPI.loadFromDb(folder1.getIdentifier());
            assertEquals(identifier.getAssetName(),newFolderName);

            final Set<Identifier> subIdentifiers = collectSubIdentifiers(identifier);
            subIdentifiers.forEach(ident -> {
                 // System.out.println(String.format("%s, %s ",ident.getPath(), ident.getAssetName()));
                 assertTrue(ident.getPath().contains(newFolderName));
            });

        } finally {
            if (folder3 != null) {
                folderAPI.delete(folder3, user, false);
            }
            if (folder2 != null) {
                folderAPI.delete(folder2, user, false);
            }
            if (folder1 != null) {
                folderAPI.delete(folder1, user, false);
            }
            if (folder0 != null) {
                folderAPI.delete(folder0, user, false);
            }
        }
    }


    private Set<Identifier> collectSubIdentifiers(final Identifier identifier) throws DotDataException {
        final Set<Identifier> children = new HashSet<>();
        final String path =  identifier.getParentPath() + identifier.getAssetName() + "/";
        //System.out.println(path);
        final List<Identifier> identifiers = APILocator.getIdentifierAPI().findByParentPath(identifier.getHostId(),path);
        for(final Identifier id:identifiers){
            if(!id.getId().equals(identifier.getId())){
               children.addAll(collectSubIdentifiers(id));
            }
        }
        children.addAll(identifiers);
        return children;
    }


    @WrapInTransaction
    private Identifier insertIdentifier(final String assetName, final String parentPath, final String assetType)
            throws DotDataException, DotRuntimeException {

        Logger.debug(this,
                "Changing FileAsset CT FileName field for :" + DbConnectionFactory.getDBType());

        final String uuid = UUIDGenerator.generateUuid();
        final Identifier identifier = new Identifier(uuid);
        try {
            identifier.setParentPath(parentPath);
            identifier.setAssetName(assetName);
            identifier.setHostId(host.getIdentifier());
            identifier.setAssetType(assetType);
            identifier.setSysExpireDate(null);
            identifier.setSysPublishDate(null);

            final DotConnect dotConnect = new DotConnect();
            dotConnect.setSQL(INSERT_IDENTIFIER_SQL);

            dotConnect.addParam(identifier.getParentPath());
            dotConnect.addParam(identifier.getAssetName());
            dotConnect.addParam(identifier.getHostId());
            dotConnect.addParam(identifier.getAssetType());
            dotConnect.addParam(identifier.getSysPublishDate());
            dotConnect.addParam(identifier.getSysExpireDate());
            dotConnect.addParam(uuid);

            try {
                dotConnect.loadResult();
            } catch (DotDataException e) {
                Logger.error(getClass(), "insertIdentifier failed:" + e, e);
                throw new DotDataException(e);
            }
            return identifier;
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }

    }

    private Identifier updateIdentifier(final Identifier identifier, final String assetName,
            final String parentPath) throws DotDataException, DotRuntimeException {

        try {

            final Connection conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);
            try {

                identifier.setParentPath(parentPath);
                identifier.setAssetName(assetName);

                final DotConnect dotConnect = new DotConnect();
                dotConnect.setSQL(UPDATE_IDENTIFIER_SQL);

                dotConnect.addParam(identifier.getParentPath());
                dotConnect.addParam(identifier.getAssetName());
                dotConnect.addParam(identifier.getHostId());
                dotConnect.addParam(identifier.getAssetType());
                dotConnect.addParam(identifier.getSysPublishDate());
                dotConnect.addParam(identifier.getSysExpireDate());
                dotConnect.addParam(identifier.getId());

                try {
                    dotConnect.loadResult();
                } catch (DotDataException e) {
                    Logger.error(getClass(), UPDATE_IDENTIFIER_FAIL + e, e);
                    throw new DotDataException(e);
                }

            } finally {
                conn.setAutoCommit(false);
                conn.close();
            }
            return identifier;
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }

    }

    private void deleteIdentifier(final String identifier) throws DotDataException, DotRuntimeException {
        try {
            final Connection conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);
            try {
                final DotConnect dc = new DotConnect();
                dc.setSQL(DELETE_IDENTIFIER_SQL);

                dc.addParam(identifier);

                try {
                    dc.loadResult();
                } catch (DotDataException e) {
                    Logger.error(IdentifierFactoryImpl.class, UPDATE_IDENTIFIER_FAIL + e, e);
                    throw new DotDataException(e);
                }

            } finally {
                conn.setAutoCommit(false);
                conn.close();
            }
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }

    private void updateFolder(final String identifier, final String inode, final String newFolderName) throws DotDataException, DotRuntimeException {
        try {
            final Connection conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);
            try {
                final DotConnect dc = new DotConnect();
                dc.setSQL(UPDATE_FOLDER);
                dc.addParam(newFolderName);
                dc.addParam(identifier);
                dc.addParam(inode);

                try {
                    dc.loadResult();
                } catch (DotDataException e) {
                    Logger.error(IdentifierFactoryImpl.class, UPDATE_IDENTIFIER_FAIL + e, e);
                    throw new DotDataException(e);
                }

            } finally {
                conn.setAutoCommit(false);
                conn.close();
            }
        } catch (Exception e) {
            throw new DotDataException(e.getMessage(), e);
        }
    }


}
