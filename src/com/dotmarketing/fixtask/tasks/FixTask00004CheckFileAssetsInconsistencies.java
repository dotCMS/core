package com.dotmarketing.fixtask.tasks;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.FixAudit;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


public class FixTask00004CheckFileAssetsInconsistencies  implements FixTask {

	private List<Map<String, String>> modifiedData= new  ArrayList <Map<String,String>>();
	
	

	public List <Map <String,Object>> executeFix() throws DotDataException, DotRuntimeException {

		Logger.info(CMSMaintenanceFactory.class,
				"Beginning fixAssetsInconsistencies");
        List <Map <String,Object>>  returnValue = new ArrayList <Map <String,Object>>();
		int counter = 0;

		final String fix2FileAssetQuery = "select c.* from file_asset c, inode i where i.inode = c.inode and c.identifier = ? order by live desc, mod_date desc";
		final String fix3FileAssetQuery = "update file_asset set working = ? where inode = ?";

		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 4: check the working and live versions of file assets for inconsistencies");			
			HibernateUtil.startTransaction();
			try {
				DotConnect db = new DotConnect();

				String query = "select distinct ident.* " + "from identifier ident, "
						+ "inode i, " + "file_asset c "
						+ "where ident.id = c.identifier and "
						+ "ident.id not in (select ident.id "
						+ "from identifier id, " + "inode i, "
						+ "file_asset c "
						+ "where c.identifier = ident.id and "
						+ "i.inode = c.inode and " + "c.working = "
						+ DbConnectionFactory.getDBTrue() + ") and "
						+ "i.type = 'file_asset' and " + "i.inode = c.inode";
				Logger.debug(CMSMaintenanceFactory.class,
						"Running query for file assets: " + query);
				db.setSQL(query);
				List<HashMap<String, String>> fileAssetIds = db.getResults();
				Logger.debug(CMSMaintenanceFactory.class, "Found "
						+ fileAssetIds.size() + " File Assets");
				int total = fileAssetIds.size();
				
				Logger.info(CMSMaintenanceFactory.class,
						"Total number of assets: " + total);
				FixAssetsProcessStatus.setTotal(total);

				// Check the working and live versions of contentlets
				String identifierInode;
				List<HashMap<String, String>> versions;
				HashMap<String, String> version;
				String versionWorking;
				String DbConnFalseBoolean = DbConnectionFactory.getDBFalse()
						.trim().toLowerCase();

				char DbConnFalseBooleanChar;
				if (DbConnFalseBoolean.charAt(0) == '\'')
					DbConnFalseBooleanChar = DbConnFalseBoolean.charAt(1);
				else
					DbConnFalseBooleanChar = DbConnFalseBoolean.charAt(0);

				String inode;

				// Check the working and live versions of file assets
				Logger.info(CMSMaintenanceFactory.class,
						"Verifying working and live versions for "
								+ fileAssetIds.size() + " file_assets");
				for (HashMap<String, String> identifier : fileAssetIds) {
					identifierInode = identifier.get("id");

					Logger.debug(CMSMaintenanceFactory.class,
							"identifier inode " + identifierInode);
					Logger.debug(CMSMaintenanceFactory.class, "Running query: "
							+ fix2FileAssetQuery);

					db.setSQL(fix2FileAssetQuery);
					db.addParam(identifierInode);
					versions = db.getResults();
					modifiedData.addAll(versions);

					if (0 < versions.size()) {
						version = versions.get(0);
						versionWorking = version.get("working").trim()
								.toLowerCase();

						inode = version.get("inode");
						Logger.debug(CMSMaintenanceFactory.class,
								"Non Working File inode : " + inode);
						Logger.debug(CMSMaintenanceFactory.class,
								"Running query: " + fix3FileAssetQuery);
						db.setSQL(fix3FileAssetQuery);
						if(DbConnectionFactory.getDBType()==DbConnectionFactory.POSTGRESQL)
							 db.addParam(true);
						else db.addParam(DbConnectionFactory.getDBTrue().replaceAll("'", ""));
						db.addParam(inode);
						db.getResult();

						FixAssetsProcessStatus.addAError();
						counter++;

					}

					FixAssetsProcessStatus.addActual();
				}
				getModifiedData();
				FixAudit Audit= new FixAudit();
				Audit.setTableName("file_asset");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(total);
				Audit.setAction("Check the working and live versions of file assets for inconsistencies and fix them");
				HibernateUtil.save(Audit);
				HibernateUtil.commitTransaction();				
				returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
				FixAssetsProcessStatus.stopProgress();
				Logger.debug(CMSMaintenanceFactory.class,
						"Ending fixAssetsInconsistencies");
			} catch (Exception e) {
				Logger.debug(CMSMaintenanceFactory.class,
						"There was a problem fixing asset inconsistencies", e);
				Logger.warn(CMSMaintenanceFactory.class,
						"There was a problem fixing asset inconsistencies", e);				
				HibernateUtil.rollbackTransaction();
				FixAssetsProcessStatus.stopProgress();
				FixAssetsProcessStatus.setActual(-1);
			}
		}

		 return returnValue;
	}

	
	public List <Map<String, String>>getModifiedData() {
		
		if (modifiedData.size() > 0) {
			XStream _xstream = new XStream(new DomDriver());
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
			String lastmoddate = sdf.format(date);
			File _writing = null;

			if (!new File(ConfigUtils.getBackupPath()+File.separator+"fixes").exists()) {
				new File(ConfigUtils.getBackupPath()+File.separator+"fixes").mkdir();
			}
			_writing = new File(ConfigUtils.getBackupPath()+File.separator+"fixes" + java.io.File.separator + lastmoddate + "_"
					+ "FixTask00004CheckFileAssetsnconsistencies" + ".xml");

			BufferedOutputStream _bout = null;
			try {
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
			} catch (FileNotFoundException e) {

			}
			_xstream.toXML(modifiedData, _bout);
		}
		return modifiedData;
	}
	

	public boolean shouldRun() {

		DotConnect db = new DotConnect();

		String query = "select distinct ident.* " + "from identifier ident, "
				+ "inode i, " + "file_asset c "
				+ "where ident.id = c.identifier and "
				+ "ident.id not in (select ident.id " + "from identifier ident, "
				+ "inode i, " + "file_asset c "
				+ "where c.identifier = ident.id and "
				+ "i.inode = c.inode and " + "c.working = "
				+ DbConnectionFactory.getDBTrue() + ") and "
				+ "i.type = 'file_asset' and " + "i.inode = c.inode";

		db.setSQL(query);
		List<HashMap<String, String>> fileAssetIds =null;
		try {
			fileAssetIds = db.getResults();
		} catch (DotDataException e) {
		  Logger.error(this, e.getMessage(), e);
		}
		int total = fileAssetIds.size();
		if (total > 0)
			return true;

		else
			return false;
	}

}
