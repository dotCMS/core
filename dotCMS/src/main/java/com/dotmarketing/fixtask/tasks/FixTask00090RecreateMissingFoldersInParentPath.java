package com.dotmarketing.fixtask.tasks;

import static com.dotmarketing.portlets.folders.business.FolderAPI.SYSTEM_FOLDER_PARENT_PATH;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotcms.util.CloseUtils;
import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.beans.FixAudit;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.thoughtworks.xstream.XStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This task will iterate through each record in the identifier table and check if there is a
 * corresponding identifier entry for every folder in its parent path. In the case that a missing
 * folder is detected, it will be re-created.
 */
public class FixTask00090RecreateMissingFoldersInParentPath implements FixTask {

    private static final String TASKNAME = "RecreateMissingFoldersInParentPath";

    private List<Map<String, String>> modifiedData = new ArrayList<>();

    private int total;

    @Override
    public List<Map<String, Object>> executeFix() throws DotDataException, DotRuntimeException {
        Logger.info(FixTask00090RecreateMissingFoldersInParentPath.class,
                "Beginning RecreateMissingFoldersInParentPath");
        List<Map<String, Object>> returnValue = new ArrayList<>();

        if (!FixAssetsProcessStatus.getRunning()) {

            try {
                FixAssetsProcessStatus.startProgress();
                FixAssetsProcessStatus.setDescription("task 90: " + TASKNAME);

                try (Connection c = DbConnectionFactory.getConnection()) {
                    try (PreparedStatement stmt = c.prepareStatement(
                            "SELECT DISTINCT parent_path, host_inode FROM identifier");
                            ResultSet rs = stmt.executeQuery()) {

                        while (rs.next()) {
                            LiteIdentifier identifier = getIdentifierFromDBRow(rs);
                            recreateMissingFoldersInParentPath(identifier.parentPath,
                                    identifier.hostId);
                        }
                    }
                }
                FixAssetsProcessStatus.setTotal(total);
                createFixAudit(returnValue, total);
                Logger.debug(FixTask00090RecreateMissingFoldersInParentPath.class,
                        "Ending " + TASKNAME);
            } catch (Exception e) {
                Logger.error(FixTask00090RecreateMissingFoldersInParentPath.class,
                        "There was a problem during " + TASKNAME, e);
                FixAssetsProcessStatus.setActual(-1);
            } finally {
                FixAssetsProcessStatus.stopProgress();
            }
        }
        return returnValue;
    }

    @VisibleForTesting
    protected void recreateMissingFoldersInParentPath(String parentPath, String hostId)
            throws SQLException, DotDataException, DotSecurityException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(parentPath));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(hostId));

		if (parentPath.equals("/") || parentPath.equals(SYSTEM_FOLDER_PARENT_PATH)) {
			return;
		}
        List<LiteFolder> folders = getFoldersFromParentPath(parentPath, hostId);
        recreateMissingFolders(folders);
    }

    @VisibleForTesting
    protected void recreateMissingFolders(List<LiteFolder> folders)
            throws SQLException, DotSecurityException, DotDataException {
        for (LiteFolder folder : folders) {
            if (isFolderIdentifierMissing(folder)) {
                createFolder(folder);
                total++;
                FixAssetsProcessStatus.addAErrorFixed();
            }
        }
    }

    @VisibleForTesting
    protected boolean isFolderIdentifierMissing(LiteFolder folder) throws SQLException {
        String sql = "SELECT COUNT(1) FROM identifier WHERE lower(parent_path) = ? AND lower(asset_name) = ? AND asset_type = ? and host_inode = ?";

        boolean missing = false;

        try (PreparedStatement stmt = DbConnectionFactory.getConnection().prepareStatement(sql)) {
            stmt.setObject(1, folder.parentPath.toLowerCase());
            stmt.setObject(2, folder.name.toLowerCase());
            stmt.setObject(3, LiteFolder.type);
            stmt.setObject(4, folder.hostId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    missing = (count == 0);
                }
            }
        }

        return missing;
    }

    @VisibleForTesting
    protected void createFolder(LiteFolder folder)
            throws DotDataException, DotSecurityException, SQLException {
        try {
            DbConnectionFactory.getConnection().setAutoCommit(false);
            Folder f = new Folder();
            f.setName(folder.name);
            f.setTitle(folder.name);
            f.setShowOnMenu(false);
            f.setSortOrder(0);
            f.setFilesMasks("");
            f.setHostId(folder.hostId);
            f.setDefaultFileType(CacheLocator.getContentTypeCache().getStructureByVelocityVarName(
                            APILocator.getFileAssetAPI().DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME)
                    .getInode());
            Identifier identifier = createIdentifier(folder);
            f.setIdentifier(identifier.getId());
            APILocator.getFolderAPI().save(f, APILocator.getUserAPI().getSystemUser(), false);
            DbConnectionFactory.getConnection().commit();
        } catch (Exception e) {
            DbConnectionFactory.getConnection().rollback();
            throw e;
        } finally {
            DbConnectionFactory.getConnection().setAutoCommit(true);
        }
    }

    private Identifier createIdentifier(LiteFolder folder) throws DotDataException {
        Identifier identifier = new Identifier();
        identifier.setAssetType("folder");
        identifier.setAssetName(folder.name);
        identifier.setHostId(folder.hostId);
        identifier.setParentPath(folder.parentPath);
        APILocator.getIdentifierAPI().save(identifier);
        return identifier;
    }

    @VisibleForTesting
    protected List<LiteFolder> getFoldersFromParentPath(String parentPath, String hostId) {
        List<LiteFolder> folders = new ArrayList<>();
        String[] parts = parentPath.split("/");
        StringBuilder folderParentPath = new StringBuilder("/");

        for (int i = 0; i < parts.length - 1; i++) {
            LiteFolder folder = new LiteFolder();

            if (i > 0) {
                folderParentPath.append(parts[i]).append("/");
            }

            folder.parentPath(folderParentPath.toString());
            folder.name(parts[i + 1]);
            folder.hostId(hostId);
            folders.add(folder);
        }

        return folders;
    }

    private LiteIdentifier getIdentifierFromDBRow(ResultSet rs) throws SQLException {
        return new LiteIdentifier()
                .parentPath(rs.getString("parent_path"))
                .hostId(rs.getString("host_inode"));
    }

    private void createFixAudit(List<Map<String, Object>> returnValue, int total) throws Exception {
        FixAudit Audit = new FixAudit();
        Audit.setTableName("field");
        Audit.setDatetime(new Date());
        Audit.setRecordsAltered(total);
        Audit.setAction("task 90: " + TASKNAME);
        HibernateUtil.save(Audit);
        returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
    }

    @Override
    public List<Map<String, String>> getModifiedData() {
        if (modifiedData.size() > 0) {
            XStream xStreamInstance = XStreamHandler.newXStreamInstance();
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
            String lastmoddate = sdf.format(date);
            File _writing = null;
            if (!new File(ConfigUtils.getBackupPath() + File.separator
                    + "fixes").exists()) {
                new File(ConfigUtils.getBackupPath() + File.separator + "fixes")
                        .mkdirs();
            }
            _writing = new File(ConfigUtils.getBackupPath() + File.separator
                    + "fixes" + File.separator + lastmoddate + "_"
                    + FixTask00090RecreateMissingFoldersInParentPath.class.getSimpleName()
                    + ".xml");

            BufferedOutputStream _bout = null;
            try {
                _bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()));
            } catch (IOException e) {
                Logger.error(this, "Could not write to Fix Task status file.");
            }
            try {
                xStreamInstance.toXML(modifiedData, _bout);
            } finally {
                CloseUtils.closeQuietly(_bout);
            }
        }
        return modifiedData;
    }

    @Override
    public boolean shouldRun() {
        return true;
    }

    @VisibleForTesting
    protected static class LiteFolder {

        protected String name;
        protected String parentPath;
        protected String hostId;
        private static final String type = "folder";

        private LiteFolder name(String name) {
            this.name = name;
            return this;
        }

        private LiteFolder parentPath(String parentPath) {
            this.parentPath = parentPath;
            return this;
        }

        private LiteFolder hostId(String hostId) {
            this.hostId = hostId;
            return this;
        }
    }

    private class LiteIdentifier {

        private String parentPath;
        private String hostId;

        private LiteIdentifier parentPath(String parentPath) {
            this.parentPath = parentPath;
            return this;
        }

        private LiteIdentifier hostId(String hostId) {
            this.hostId = hostId;
            return this;
        }
    }
}