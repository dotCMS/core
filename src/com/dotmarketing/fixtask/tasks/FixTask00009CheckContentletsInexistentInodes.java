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


public class FixTask00009CheckContentletsInexistentInodes implements FixTask {

	private List  <Map<String, String>> modifiedData= null;
	

	public List <Map <String,Object>>executeFix()throws DotDataException, DotRuntimeException {

		List <Map <String,Object>>  returnValue =  new ArrayList <Map <String,Object>>();
		Logger.info(CMSMaintenanceFactory.class,
				"Beginning fixAssetsInconsistencies");
		
		int counter = 0;


		final String fixContentletQuery = "delete from contentlet where inode = ?";

		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 9: check for contentlets  that points to inexistent inodes and deleted them");			
			HibernateUtil.startTransaction();
			try {
				DotConnect db = new DotConnect();

				int total=0;
				String query = "select * from contentlet c where c.inode not in (select inode from inode where type='contentlet')";
				Logger.debug(CMSMaintenanceFactory.class,
						"Running query for Contentlets: " + query);
				db.setSQL(query);
				List<Map<String, String>> contentletIds = db.getResults();
				
				modifiedData=contentletIds;
				getModifiedData();
				Logger.debug(CMSMaintenanceFactory.class, "Found "
						+ contentletIds.size() + " Contentlets");
				total += contentletIds.size();

				
				Logger.info(CMSMaintenanceFactory.class,
						"Total number of assets: " + total);
				FixAssetsProcessStatus.setTotal(total);

				// Check the working and live versions of contentlets
				String identifierInode;
			


				String inode;

				Logger.info(CMSMaintenanceFactory.class,"deleting " + contentletIds.size()+" contentlets that point to inexistent inodes ");
				for (Map<String, String> identifier : contentletIds) {
					identifierInode = identifier.get("inode");

					Logger.debug(CMSMaintenanceFactory.class,"identifier inode " + identifierInode);

						inode = identifier.get("inode");
						Logger.debug(CMSMaintenanceFactory.class,"Non Working Contentlet inode : " + inode);
						Logger.debug(CMSMaintenanceFactory.class,"Running query: " + fixContentletQuery);
						db.setSQL(fixContentletQuery);
						db.addParam(inode);
						db.getResult();

						FixAssetsProcessStatus.addAError();
						counter++;
					}
				
				FixAssetsProcessStatus.addActual();
				FixAudit Audit= new FixAudit();
				Audit.setTableName("contentlet");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(total);
				Audit.setAction("Delete contentlets that points to inexistent inodes");
				HibernateUtil.save(Audit);				
				HibernateUtil.commitTransaction();
				returnValue.add( FixAssetsProcessStatus.getFixAssetsMap());
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
	


	public List <Map<String, String>> getModifiedData() {

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
					+ "FixTask00009CheckContentletsInconsistencies" + ".xml");

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

		String query = "select c.inode from contentlet c where c.inode not in (select inode from inode where type='contentlet') ";
		
		db.setSQL(query);
		List<HashMap<String, String>> contentletIds =null;
		try {
			contentletIds = db.getResults();
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
		}
		Logger.debug(CMSMaintenanceFactory.class, "Found "
				+ contentletIds.size() + " Contentlets");
		int total = contentletIds.size();
		
		if (total>0)
		return true;
		
		else
        return false;
	}

}
