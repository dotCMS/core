package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.IntegrationTestBase;
import com.dotcms.exception.ExceptionUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import java.sql.Connection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;

public class IdentifierConsistencyIntegrationTest extends IntegrationTestBase {

    private static final String INSERT_IDENTIFIER_SQL = "INSERT INTO identifier (parent_path,asset_name,host_inode,asset_type,syspublish_date,sysexpire_date,id) values (?,?,?,?,?,?,?)";
    private static final String UPDATE_IDENTIFIER_SQL = "UPDATE identifier set parent_path=?, asset_name=?, host_inode=?, asset_type=?, syspublish_date=?, sysexpire_date=? where id=?";
    private static final String DELETE_IDENTIFIER_SQL = "DELETE FROM identifier where id=?";
    private static final String UPDATE_FOLDER = "UPDATE FOLDER SET name = ? WHERE identifier = ? AND inode = ?";

    private static final String CONTENTLET = "contentlet";
    private static final String FOLDER = "folder";

    @BeforeClass
    public static void init() throws Exception {
        IntegrationTestInitService.getInstance().init();
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
    public void Test_Identifier_Create_Folder_Tree_Then_Update_Parent_Path_Lower_Case_Upper_Case_Non_existing_Path()
            throws DotDataException, DotSecurityException {
        final String prefix = System.currentTimeMillis() + "_";
        final FolderAPI folderAPI = APILocator.getFolderAPI();
        final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
        final User user = APILocator.systemUser();
        final Host host = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
        final String rootFolderName = String.format("/%sroot2", prefix);
        Folder root = null;
        try {
            root = folderAPI.createFolders(rootFolderName, host, user, false);

            final Folder f1 = folderAPI
                    .createFolders(root.getPath() + "child-1/child-1-1/child-1-1-1/child-1-1-1-1",
                            host, user, false);

            Logger.info(getClass(), () -> "Path1: " + f1.getPath());

            final Folder f2 = folderAPI
                    .createFolders(root.getPath() + "child-2/child-2-1/child-2-1-1/child-2-1-1-1",
                            host, user, false);

            Logger.info(getClass(), () -> "Path2: " + f2.getPath());

            final Folder f3 = folderAPI
                    .createFolders(root.getPath() + "child-3/child-3-1/child-3-1-1/child-3-1-1-1",
                            host, user, false);

            Logger.info(getClass(), () -> "Path3: " + f3.getPath());

            final Folder testTargetFolder = folderAPI
                    .findFolderByPath(root.getPath() + "child-3/child-3-1", host, user,
                            false);

            assertNotNull(testTargetFolder);

            Logger.info(getClass(), () -> "Path4: " + testTargetFolder.getPath());

            final Identifier testFolderIdentifier = identifierAPI.find(testTargetFolder.getIdentifier());

            assertEquals("should have been a folder", "folder", testFolderIdentifier.getAssetType());

            try {
                final Connection conn = DbConnectionFactory.getDataSource().getConnection();
                conn.setAutoCommit(true);
                try {
                    // Tests actually happens HERE!
                    try {
                        updateIdentifier(testFolderIdentifier, testFolderIdentifier.getAssetName(), root.getPath() + "child-2/");
                        // If I update parent folder, all children identifiers are properly renamed.
                        // But if I rename parent path directly on the identifiers table the change isn't propagated downstream.

                        //final Set<Identifier> subIdentifiers = collectSubIdentifiers(testFolderIdentifier);
                        //assertFalse(subIdentifiers.isEmpty());

                    } catch (DotDataException e) {
                        fail("Exception shouldn't have been raised.  Parent Path is valid" + e
                                .getMessage());
                    }

                    try {
                        updateIdentifier(testFolderIdentifier, testFolderIdentifier.getAssetName(), root.getPath() + "Child-2/");
                    } catch (DotDataException e) {
                        final Throwable trss = ExceptionUtil.getRootCause(e);
                        fail("Exception shouldn't have been raised.  Parent Path is valid" + trss.getMessage());
                    }

                    // This one should fail!
                    try {
                        final String invalidParentPath =
                                root.getPath() + "lololololololololololololololololo/";
                        updateIdentifier(testFolderIdentifier, testFolderIdentifier.getAssetName(), invalidParentPath);
                        fail("Parent '" + invalidParentPath
                                + "' does not exist and the update should have failed");
                    } catch (DotDataException e) {
                        Logger.info(getClass(),
                                () -> "This exception is expected: " + e.getMessage());
                    }

                } finally {
                    conn.setAutoCommit(false);
                    conn.close();
                }
            } catch (Exception e) {
                Logger.error(getClass(), "updateIdentifier failed", e);
            }
        } finally {
            if (root != null) {
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
                    Logger.error(getClass(), "updateIdentifier failed", e);
                }
            }
            if (null != parentFolderIdentifier) {
                try {
                    deleteIdentifier(parentFolderIdentifier.getId());
                } catch (Exception e) {
                    Logger.error(getClass(), "updateIdentifier failed", e);
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
                    Logger.error(getClass(), "updateIdentifier failed", e);
                }
            }
            if (null != subFolderIdentifier2) {
                try {
                    deleteIdentifier(subFolderIdentifier2.getId());
                } catch (Exception e) {
                    Logger.error(getClass(), "updateIdentifier failed", e);
                }
            }

            if (null != parentFolderIdentifier) {
                try {
                    deleteIdentifier(parentFolderIdentifier.getId());
                } catch (Exception e) {
                    Logger.error(getClass(), "updateIdentifier failed", e);
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
            final String parentFolderName = String.format("/%sparentFolder", prefix);
            parentFolderIdentifier = insertIdentifier(parentFolderName, "/", FOLDER);
            subFolderIdentifier = insertIdentifier("subFolder", parentFolderIdentifier.getPath(), FOLDER);

            deleteIdentifier(parentFolderIdentifier.getId());

        } finally {
            if (null != subFolderIdentifier) {
                try {
                    deleteIdentifier(subFolderIdentifier.getId());
                } catch (Exception e) {
                    Logger.error(getClass(), "updateIdentifier failed", e);
                }
            }
            if (null != parentFolderIdentifier) {
                try {
                    deleteIdentifier(parentFolderIdentifier.getId());
                } catch (Exception e) {
                    Logger.error(getClass(), "updateIdentifier failed", e);
                }
            }
        }
    }


    @Test
    public void Test_Folder_Create_Folder_Tree_Then_Rename_Parent_Verify_Children_Are_Renamed()
            throws DotDataException, DotSecurityException {
        final String prefix = System.currentTimeMillis() + "_";
        final FolderAPI folderAPI = APILocator.getFolderAPI();
        final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
        final User user = APILocator.systemUser();
        final Host host = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
        final String rootFolderName = String.format("/%slevel-0", prefix);
        Folder f0,f1,f2,f3;
        f0 = f1 = f2 = f3 = null;
        try {
             f0 = folderAPI
                    .createFolders(rootFolderName, host, user, false);

             f1 = folderAPI
                    .createFolders(f0.getPath() + "level-1", host, user, false);

             f2 = folderAPI
                    .createFolders(f1.getPath() + "level-2", host, user, false);

             f3 = folderAPI
                    .createFolders(f2.getPath() + "level-3", host, user, false);

            final String newFolderName = "newName";
            updateFolder(f1.getIdentifier(), f1.getInode(), newFolderName);
            final Identifier identifier = identifierAPI.loadFromDb(f1.getIdentifier());
            assertEquals(identifier.getAssetName(),newFolderName);

            final Set<Identifier> subIdentifiers = collectSubIdentifiers(identifier);
            subIdentifiers.forEach(ident -> {
                 // System.out.println(String.format("%s, %s ",ident.getPath(), ident.getAssetName()));
                 assertTrue(ident.getPath().contains(newFolderName));
            });

        } finally {
            if (f3 != null) {
                folderAPI.delete(f3, user, false);
            }
            if (f2 != null) {
                folderAPI.delete(f2, user, false);
            }
            if (f1 != null) {
                folderAPI.delete(f1, user, false);
            }
            if (f0 != null) {
                folderAPI.delete(f0, user, false);
            }
        }
    }


    private Set<Identifier> collectSubIdentifiers(Identifier identifier) throws DotDataException {
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


    private Identifier insertIdentifier(final String assetName, final String parentPath, final String assetType)
            throws DotDataException, DotRuntimeException {

        Logger.debug(this,
                "Changing FileAsset CT FileName field for :" + DbConnectionFactory.getDBType());

        final String id = UUIDGenerator.generateUuid();
        final Identifier identifier = new Identifier(id);
        try {
            final Host host = APILocator.getHostAPI().findByName("demo.dotcms.com", APILocator.systemUser(), false);

            final Connection conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);
            try {

                identifier.setParentPath(parentPath);
                identifier.setAssetName(assetName);
                identifier.setHostId(host.getIdentifier());
                identifier.setAssetType(assetType);
                identifier.setSysExpireDate(null);
                identifier.setSysPublishDate(null);

                final DotConnect dc = new DotConnect();
                dc.setSQL(INSERT_IDENTIFIER_SQL);

                dc.addParam(identifier.getParentPath());
                dc.addParam(identifier.getAssetName());
                dc.addParam(identifier.getHostId());
                dc.addParam(identifier.getAssetType());
                dc.addParam(identifier.getSysPublishDate());
                dc.addParam(identifier.getSysExpireDate());
                dc.addParam(id);

                try {
                    dc.loadResult();
                } catch (DotDataException e) {
                    Logger.error(getClass(), "insertIdentifier failed:" + e, e);
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

    private Identifier updateIdentifier(final Identifier identifier, final String assetName,
            final String parentPath) throws DotDataException, DotRuntimeException {

        try {

            final Connection conn = DbConnectionFactory.getDataSource().getConnection();
            conn.setAutoCommit(true);
            try {

                identifier.setParentPath(parentPath);
                identifier.setAssetName(assetName);

                final DotConnect dc = new DotConnect();
                dc.setSQL(UPDATE_IDENTIFIER_SQL);

                dc.addParam(identifier.getParentPath());
                dc.addParam(identifier.getAssetName());
                dc.addParam(identifier.getHostId());
                dc.addParam(identifier.getAssetType());
                dc.addParam(identifier.getSysPublishDate());
                dc.addParam(identifier.getSysExpireDate());
                dc.addParam(identifier.getId());

                try {
                    dc.loadResult();
                } catch (DotDataException e) {
                    Logger.error(getClass(), "updateIdentifier failed:" + e, e);
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
                    Logger.error(IdentifierFactoryImpl.class, "updateIdentifier failed:" + e, e);
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
                    Logger.error(IdentifierFactoryImpl.class, "updateFolder failed:" + e, e);
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
