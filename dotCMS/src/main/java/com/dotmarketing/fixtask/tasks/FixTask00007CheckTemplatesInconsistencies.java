package com.dotmarketing.fixtask.tasks;


import com.dotmarketing.beans.FixAudit;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FixTask00007CheckTemplatesInconsistencies  implements FixTask {

	
	private List <Map<String, String>> modifiedData= new  ArrayList <Map<String,String>>();

	public List <Map <String,Object>> executeFix() throws DotDataException, DotRuntimeException {

		Logger.info(CMSMaintenanceFactory.class,
				"Beginning fixAssetsInconsistencies");
	    List <Map <String,Object>> returnValue = new ArrayList <Map <String,Object>>();
		int counter = 0;

		final String fix2TemplatesQuery = "select c.* from template c, inode i where i.inode = c.inode and c.identifier = ? order by mod_date desc";
		final String fix3TemplatesQuery = "update template_version_info set working_inode = ? where identifier = ?";

		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 7: check the working and live versions of templates for inconsistencies");			
			HibernateUtil.startTransaction();
			try {
				DotConnect db = new DotConnect();

				String query = "select distinct ident.* " + "from identifier ident, "
						+ "inode i, " + "template c "
						+ "where ident.id = c.identifier and "
						+ "ident.id not in (select ident.id "
						+ "from identifier ident, " + "inode i, " + "template c, " + "template_version_info tvi "
						+ "where c.identifier = ident.id and "
						+ "i.inode = c.inode and " + "tvi.working_inode = c.inode) and "	
						+ "i.type = 'template' and " + "i.inode = c.inode";

				Logger.debug(CMSMaintenanceFactory.class,
						"Running query for templates: " + query);
				db.setSQL(query);
				List<HashMap<String, String>> templateIds = db.getResults();

				Logger.debug(CMSMaintenanceFactory.class, "Found "
						+ templateIds.size() + " Templates");
				int total = templateIds.size();

				Logger.info(CMSMaintenanceFactory.class,
						"Total number of assets: " + total);
				FixAssetsProcessStatus.setTotal(total);

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

				// Logger.info("Step 7: " + template.size());

				// Check the working and live versions of templates
				Logger.info(CMSMaintenanceFactory.class,
						"Verifying working and live versions for "
								+ templateIds.size() + " templates");
				for (HashMap<String, String> identifier : templateIds) {
					identifierInode = identifier.get("id");

					Logger.debug(CMSMaintenanceFactory.class,
							"identifier inode " + identifierInode);
					Logger.debug(CMSMaintenanceFactory.class, "Running query: "
							+ fix2TemplatesQuery);

					db.setSQL(fix2TemplatesQuery);
					db.addParam(identifierInode);
					versions = db.getResults();
					modifiedData.addAll(versions);

					if (0 < versions.size()) {
						version = versions.get(0);
						//versionWorking = version.get("working").trim().toLowerCase();

						inode = version.get("inode");
						Logger.debug(CMSMaintenanceFactory.class,
								"Non Working Template inode : " + inode);
						Logger.debug(CMSMaintenanceFactory.class,
								"Running query: " + fix3TemplatesQuery);
						db.setSQL(fix3TemplatesQuery);						
						db.addParam(inode);
						db.addParam(identifierInode);
						db.getResult();

						FixAssetsProcessStatus.addAErrorFixed();
						counter++;
					}

					FixAssetsProcessStatus.addActual();
				}
				getModifiedData();
				FixAudit Audit= new FixAudit();
				Audit.setTableName("template");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(total);
				Audit.setAction("Check the working and live versions of templates for inconsistencies and fix them");
				HibernateUtil.save(Audit);				
				HibernateUtil.closeAndCommitTransaction();
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

	
	public List <Map<String, String>> getModifiedData()  {
		
		if (modifiedData.size() > 0) {
			XStream _xstream = new XStream(new DomDriver());
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
			String lastmoddate = sdf.format(date);
			File _writing = null;

			if (!new File(ConfigUtils.getBackupPath()+File.separator+"fixes").exists()) {
				new File(ConfigUtils.getBackupPath()+File.separator+"fixes").mkdirs();
			}
			_writing = new File(ConfigUtils.getBackupPath()+File.separator+"fixes" + java.io.File.separator  + lastmoddate + "_"
					+ "FixTask00007CheckTemplatesInconsistencies" + ".xml");

			try (BufferedOutputStream _bout = new BufferedOutputStream(Files.newOutputStream(_writing.toPath()))){
				_xstream.toXML(modifiedData, _bout);
			} catch (IOException e) {
				Logger.error(this, "Error trying to get modified data from XML.", e);
			}
		}
		return modifiedData;
	}

	public boolean shouldRun() {
		DotConnect db = new DotConnect();

		String query = "select distinct ident.* " + "from identifier ident, "
				+ "inode i, " + "template c "
				+ "where ident.id = c.identifier and "
				+ "ident.id not in (select ident.id " + "from identifier ident, "
				+ "inode i, " + "template c, " + "template_version_info tvi "
				+ "where c.identifier = ident.id and "
				+ "i.inode = c.inode and " + "tvi.working_inode = c.inode) and "
				+ "i.type = 'template' and " + "i.inode = c.inode";

		db.setSQL(query);
		List<HashMap<String, String>> templateIds =null;
		try {
			templateIds = db.getResults();
		} catch (DotDataException e) {
		  Logger.error(this,e.getMessage(), e);
		}
		int total = templateIds.size();
		if (total > 0)
			return true;
		else
			return false;

	}

}
