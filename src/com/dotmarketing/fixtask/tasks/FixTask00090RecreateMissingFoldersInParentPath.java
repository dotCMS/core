package com.dotmarketing.fixtask.tasks;

import com.dotcms.repackage.com.thoughtworks.xstream.XStream;
import com.dotcms.repackage.com.thoughtworks.xstream.io.xml.DomDriver;
import com.dotmarketing.beans.FixAudit;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This task will iterate through each record in the identifier table and check
 * if there is a corresponding identifier entry for every folder in its parent path.
 * In the case that a missing folder is detected, it will be re-created.
 *
 */
public class FixTask00090RecreateMissingFoldersInParentPath implements FixTask {

	private static final String TASKNAME = "RecreateMissingFoldersInParentPath";

	private List<Map<String, String>> modifiedData = new ArrayList<Map<String, String>>();

	private int total;

	@Override
	public List<Map<String, Object>> executeFix() throws DotDataException, DotRuntimeException {
		Logger.info(FixTask00090RecreateMissingFoldersInParentPath.class, "Beginning RecreateMissingFoldersInParentPath");
		List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();

		if (!FixAssetsProcessStatus.getRunning()) {
			HibernateUtil.startTransaction();
			try {
				FixAssetsProcessStatus.startProgress();
				FixAssetsProcessStatus.setDescription("task 90: " + TASKNAME);

				try (Connection c = DbConnectionFactory.getConnection()) {
					try (PreparedStatement stmt = c.prepareStatement("SELECT * FROM identifier");
						 ResultSet rs = stmt.executeQuery()) {

						while (rs.next()) {
							Identifier identifier = getIdentifierFromDBRow(rs);
							recreateMissingFoldersInParentPath(identifier.getParentPath(), identifier.getHostId());
						}
					}
				}
				FixAssetsProcessStatus.setTotal(total);
				createFixAudit(returnValue, total);
				Logger.debug(FixTask00090RecreateMissingFoldersInParentPath.class, "Ending " + TASKNAME);
			} catch (Exception e) {
				Logger.debug(FixTask00090RecreateMissingFoldersInParentPath.class, "There was a problem during " + TASKNAME, e);
				HibernateUtil.rollbackTransaction();
				FixAssetsProcessStatus.setActual(-1);
			} finally {
				FixAssetsProcessStatus.stopProgress();
			}
		}
		return returnValue;
	}

	private void recreateMissingFoldersInParentPath(String parentPath, String hostId) throws SQLException, DotDataException, DotSecurityException {
		if(parentPath.equals("/") || parentPath.equals("/System folder")) return;
		LiteFolder folder = getFolderFromParentPath(parentPath, hostId);
		recreateMissingFolder(folder);
		recreateMissingFoldersInParentPath(folder.parentPath, folder.hostId);
 	}

	private void recreateMissingFolder(LiteFolder folder) throws SQLException, DotSecurityException, DotDataException {
		String sql = "SELECT COUNT(1) FROM identifier WHERE parent_path = ? AND asset_name = ? AND asset_type = ? and host_inode = ?";

		try (Connection c = DbConnectionFactory.getConnection();
			 PreparedStatement stmt = c.prepareStatement(sql)) {

			stmt.setObject(1, folder.parentPath);
			stmt.setObject(2, folder.name);
			stmt.setObject(3, LiteFolder.type);
			stmt.setObject(4, folder.hostId);

			try (ResultSet rs = stmt.executeQuery()) {
				int count = rs.getInt(1);

				if(count==0) {
					total++;
					createFolder(folder);
					FixAssetsProcessStatus.addAError();
				}
			}
		}
	}

	private void createFolder(LiteFolder folder) throws DotDataException, DotSecurityException {
		Folder f= new Folder();
		f.setName(folder.name);
		f.setTitle(folder.name);
		f.setShowOnMenu(false);
		f.setSortOrder(0);
		f.setFilesMasks("");
		f.setHostId(folder.hostId);
		f.setDefaultFileType(CacheLocator.getContentTypeCache().getStructureByVelocityVarName(APILocator.getFileAssetAPI().DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).getInode());
		Identifier identifier = createIdentifier(folder);
		f.setIdentifier(identifier.getId());
		APILocator.getFolderAPI().save(f, APILocator.getUserAPI().getSystemUser(), false);
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

	private LiteFolder getFolderFromParentPath(String parentPath, String hostId) {
		String[] parts = parentPath.split("/");
		String assetName=  parts[parts.length-1];
		String assetParentPath = parentPath.substring(0, parentPath.indexOf(assetName));
		return new LiteFolder().name(assetName).parentPath(assetParentPath).hostId(hostId);
	}

	private Identifier getIdentifierFromDBRow(ResultSet rs) throws SQLException {
		Identifier identifier = new Identifier();
		identifier.setId(rs.getString("id"));
		identifier.setParentPath(rs.getString("parent_path"));
		identifier.setAssetName(rs.getString("asset_name"));
		identifier.setHostId(rs.getString("host_inode"));
		identifier.setAssetType(rs.getString("asset_type"));
		return identifier;
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
			XStream _xstream = new XStream(new DomDriver());
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
					+ FixTask00090RecreateMissingFoldersInParentPath.class.getSimpleName() + ".xml");

			BufferedOutputStream _bout = null;
			try {
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
			} catch (FileNotFoundException e) {
				Logger.error(this, "Could not write to Fix Task status file.");
			}
			_xstream.toXML(modifiedData, _bout);
		}
		return modifiedData;
	}

	@Override
	public boolean shouldRun() {
		return true;
	}

	private class LiteFolder {
		private String name;
		private String parentPath;
		private String hostId;
		private static final String type = "folder" ;

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
}