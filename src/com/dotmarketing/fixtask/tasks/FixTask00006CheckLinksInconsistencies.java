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


public class FixTask00006CheckLinksInconsistencies  implements FixTask {

	
	private List  <Map<String, String>>  modifiedData = new ArrayList <Map<String,String>>();
	
	public List <Map <String,Object>> executeFix() throws DotDataException, DotRuntimeException {

		Logger.info(CMSMaintenanceFactory.class,
				"Beginning fixAssetsInconsistencies");
		 List <Map <String,Object>> returnValue = new ArrayList <Map <String,Object>>();
		int counter = 0;

		final String fix2LinksQuery = "select c.* from links c, inode i where i.inode = c.inode and c.identifier = ? order by mod_date desc";
		final String fix3LinksQuery = "update link_version_info set working_inode = ? where identifier = ?";

		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 6: check the working and live versions of links for inconsistencies");			
			HibernateUtil.startTransaction();
			try {
				DotConnect db = new DotConnect();

				String query = "select distinct ident.* " + "from identifier ident, "
						+ "inode i, " + "links c "
						+ "where ident.id = c.identifier and "
						+ "ident.id not in (select ident.id "
						+ "from identifier ident, " + "inode i, " + "links c, " + "link_version_info lvi "
						+ "where c.identifier = ident.id and "
						+ "i.inode = c.inode and " + "lvi.working_inode = c.inode) and "						
						+ "i.type = 'links' and " + "i.inode = c.inode";
				Logger.debug(CMSMaintenanceFactory.class,
						"Running query for links: " + query);
				db.setSQL(query);
				List<HashMap<String, String>> linkIds = db.getResults();
				Logger.debug(CMSMaintenanceFactory.class, "Found "
						+ linkIds.size() + " Links");
				int total = linkIds.size();

				Logger.info(CMSMaintenanceFactory.class,
						"Total number of assets: " + total);
				FixAssetsProcessStatus.setTotal(total);

				long inodeInode;
				long parentIdentifierInode;

				// Check the working and live versions of contentlets
				String identifierInode;
				List<HashMap<String, String>> versions;
				HashMap<String, String> version;
				//String versionWorking;
				String DbConnFalseBoolean = DbConnectionFactory.getDBFalse()
						.trim().toLowerCase();

				char DbConnFalseBooleanChar;
				if (DbConnFalseBoolean.charAt(0) == '\'')
					DbConnFalseBooleanChar = DbConnFalseBoolean.charAt(1);
				else
					DbConnFalseBooleanChar = DbConnFalseBoolean.charAt(0);

				String inode;

				// Check the working and live versions of links
				Logger.info(CMSMaintenanceFactory.class,
						"Verifying working and live versions for "
								+ linkIds.size() + " links");
				for (HashMap<String, String> identifier : linkIds) {
					identifierInode = identifier.get("id");

					Logger.debug(CMSMaintenanceFactory.class,
							"identifier inode " + identifierInode);
					Logger.debug(CMSMaintenanceFactory.class, "Running query: "
							+ fix2LinksQuery);

					db.setSQL(fix2LinksQuery);
					db.addParam(identifierInode);
					versions = db.getResults();
					modifiedData.addAll(versions );
					

					if (0 < versions.size()) {
						version = versions.get(0);
						//versionWorking = version.get("working").trim().toLowerCase();

						inode = version.get("inode");
						Logger.debug(CMSMaintenanceFactory.class,
								"Non Working Link inode : " + inode);
						Logger.debug(CMSMaintenanceFactory.class,
								"Running query: " + fix3LinksQuery);
						db.setSQL(fix3LinksQuery);						
						db.addParam(inode);
						db.addParam(identifierInode);
						db.getResult();

						FixAssetsProcessStatus.addAError();
						counter++;
					}

					FixAssetsProcessStatus.addActual();
				}
				getModifiedData();
				FixAudit Audit= new FixAudit();
				Audit.setTableName("links");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(total);
				Audit.setAction("Check the working and live versions of links for inconsistencies and fix them");
				HibernateUtil.save(Audit);				
				HibernateUtil.commitTransaction();
				returnValue .add(FixAssetsProcessStatus.getFixAssetsMap());
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

	
	public List <Map<String, String>> getModifiedData()  {
		
		if (modifiedData.size() > 0) {
			XStream _xstream = new XStream(new DomDriver());
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
			String lastmoddate = sdf.format(date);
			File _writing = null;

			if (!new File(ConfigUtils.getBackupPath()+File.separator+"fixes").exists()) {
				new File(ConfigUtils.getBackupPath()+File.separator+"fixes").mkdir();
			}
			_writing = new File(ConfigUtils.getBackupPath()+File.separator+"fixes" + java.io.File.separator  + lastmoddate + "_"
					+ "FixTask00006CheckLinksInconsistencies" + ".xml");

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
				+ "inode i, " + "links c "
				+ "where ident.id = c.identifier and "
				+ "ident.id not in (select ident.id " + "from identifier ident, "
				+ "inode i, " + "links c, " + "link_version_info lvi "
				+ "where c.identifier = ident.id and "
				+ "i.inode = c.inode and " + "lvi.working_inode = c.inode) and "
				+ "i.type = 'links' and " + "i.inode = c.inode";
		db.setSQL(query);
		List<HashMap<String, String>> linkIds =null; 
		try {
			linkIds = db.getResults();
		} catch (DotDataException e) {
			Logger.error(this,e.getMessage(), e);
		}
		int total = linkIds.size();
		if (total > 0)
			return true;
		else
			return false;
	}

}
