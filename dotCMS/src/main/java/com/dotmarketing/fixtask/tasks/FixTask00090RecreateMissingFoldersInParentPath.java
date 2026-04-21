package com.dotmarketing.fixtask.tasks;

import static com.dotmarketing.portlets.folders.business.FolderAPI.SYSTEM_FOLDER_PARENT_PATH;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

                final List<LiteIdentifier> identifiers = new ArrayList<>();
                final Set<String> folderKeyCache;
                try (Connection c = DbConnectionFactory.getConnection()) {
                    // Load all existing folder identifier keys into memory once to avoid
                    // issuing one SELECT per path segment (N×M query pattern).
                    folderKeyCache = loadExistingFolderKeys(c);
                    Logger.info(FixTask00090RecreateMissingFoldersInParentPath.class,
                            "Loaded " + folderKeyCache.size() + " existing folder identifier keys into cache.");

                    try (PreparedStatement stmt = c.prepareStatement(
                            "SELECT DISTINCT parent_path, host_inode FROM identifier WHERE asset_type <> 'folder'");
                            ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            identifiers.add(getIdentifierFromDBRow(rs));
                        }
                    }
                }
                // Process outside the try-with-resources so the outer ResultSet is fully
                // closed before createFolder() runs — avoids autoCommit mutation on an
                // active cursor sharing the same ThreadLocal connection.
                for (LiteIdentifier identifier : identifiers) {
                    recreateMissingFoldersInParentPath(identifier.parentPath,
                            identifier.hostId, folderKeyCache);
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
    protected void recreateMissingFoldersInParentPath(String parentPath, String hostId,
            Set<String> folderKeyCache) throws DotDataException, DotSecurityException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(parentPath));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(hostId));

		if (parentPath.equals("/") || parentPath.equals(SYSTEM_FOLDER_PARENT_PATH)) {
			return;
		}
        List<LiteFolder> folders = getFoldersFromParentPath(parentPath, hostId);
        recreateMissingFolders(folders, folderKeyCache);
    }

    @VisibleForTesting
    protected void recreateMissingFolders(List<LiteFolder> folders, Set<String> folderKeyCache)
            throws DotSecurityException, DotDataException {
        for (LiteFolder folder : folders) {
            if (isFolderIdentifierMissing(folder, folderKeyCache)) {
                createFolder(folder);
                if (folderKeyCache != null) {
                    folderKeyCache.add(
                            folderKey(folder.hostId, folder.parentPath.toLowerCase(), folder.name.toLowerCase()));
                }
                total++;
                FixAssetsProcessStatus.addAErrorFixed();
            }
        }
    }

    @VisibleForTesting
    protected boolean isFolderIdentifierMissing(LiteFolder folder, Set<String> folderKeyCache) {
        return !folderKeyCache.contains(
                folderKey(folder.hostId, folder.parentPath.toLowerCase(), folder.name.toLowerCase()));
    }

    @VisibleForTesting
    protected void createFolder(LiteFolder folder) throws DotDataException, DotSecurityException {
        LocalTransaction.wrap(() -> {
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
        });
    }

    /**
     * Loads all existing folder identifier keys from the database into a Set for O(1) in-memory
     * lookup. This avoids issuing one SELECT per path segment during the fix loop.
     * Key format: {@code hostId\0lowerParentPath\0lowerAssetName} (NUL-separated)
     */
    private Set<String> loadExistingFolderKeys(final Connection c) throws SQLException {
        final Set<String> keys = new HashSet<>();
        try (PreparedStatement stmt = c.prepareStatement(
                "SELECT host_inode, lower(parent_path), lower(asset_name) FROM identifier WHERE asset_type = 'folder'");
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                keys.add(folderKey(rs.getString(1), rs.getString(2), rs.getString(3)));
            }
        }
        return keys;
    }

    private static String folderKey(final String hostId, final String lowerParentPath,
            final String lowerAssetName) {
        return hostId + "\0" + lowerParentPath + "\0" + lowerAssetName;
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