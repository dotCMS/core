package com.dotmarketing.util.cleantasks;



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
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.portlets.cmsmaintenance.factories.CMSMaintenanceFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;


public class DeleteFileAssetsWithNoInode {

	private List  <HashMap<String, String>> modifiedData= new  ArrayList <HashMap<String,String>>();
	

	public List<Map<String, Object>> executeFix() throws DotDataException,
			DotRuntimeException {

		List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();
		Logger.info(CMSMaintenanceFactory.class,"Beginning fixAssetsInconsistencies");
		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 10: Deleting file assets with no inode");
			HibernateUtil.startTransaction();

			try {
				DotConnect dc = new DotConnect();
				int counter = 0;
				File file = null;
			    String selectInodesSQL = "select* from inode i where type = 'file_asset'";
				dc.setSQL(selectInodesSQL);
				List<HashMap<String, String>> results = dc.getResults();
				String query =  "select* from file_asset c where c.inode not in(select inode from inode)";
				dc.setSQL(query);
	
				
				List<String> fileAssetsListFromFileSystem = (List<String>) MaintenanceUtil.findFileAssetsCanBeParsed().get(0);
				List<String> fileAssetsInodesListFromFileSystem = (List<String>) MaintenanceUtil.findFileAssetsCanBeParsed().get(1);
				List<String> fileAssetsInodesList = new ArrayList<String>();
                HashMap <String,String> data= new HashMap <String,String>();
				for (HashMap<String, String> r : results) {
					fileAssetsInodesList.add(r.get("inode").toString());
				}
				results = null;
				int pathcount=1;
				for (int i = 0; i < fileAssetsInodesListFromFileSystem.size(); i++) {
					if (!fileAssetsInodesList.contains(fileAssetsInodesListFromFileSystem.get(i))) {
						file = new File(fileAssetsListFromFileSystem.get(i));
						Logger.debug(MaintenanceUtil.class, "Deleting "+ file.getPath() + "...");
						System.out.println("Deleting " + file.getPath() + "...");
						file.delete();
						data.put("path"+pathcount, file.getPath());
						modifiedData.add(data);
						FixAssetsProcessStatus.addAError();
						counter++;
						pathcount++;
					}
				}
				FixAssetsProcessStatus.setTotal(counter);
				
				getModifiedData();
				FixAudit Audit = new FixAudit();
				Audit.setTableName("contentlet");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(0);
				Audit.setAction("task 10: Deletes the file assets with no inode");
				HibernateUtil.save(Audit);
				HibernateUtil.commitTransaction();
				returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
				FixAssetsProcessStatus.stopProgress();
				Logger.debug(CMSMaintenanceFactory.class,
						"Ending fixAssetsInconsistencies");
			} catch (Exception e) {
				Logger.debug(CMSMaintenanceFactory.class,"There was a problem fixing asset inconsistencies", e);
				Logger.warn(CMSMaintenanceFactory.class,"There was a problem fixing asset inconsistencies", e);
				HibernateUtil.rollbackTransaction();
				FixAssetsProcessStatus.stopProgress();
				FixAssetsProcessStatus.setActual(-1);
			}
		}
		return returnValue;

	}
		 

	public List <HashMap<String, String>> getModifiedData() {

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
					+ "FixTask00010DeleteFileAssetsWithNoInode" + ".xml");

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
		
		DotConnect dc = new DotConnect();
	    String selectInodesSQL = "select* from inode i where type = 'file_asset'";
		dc.setSQL(selectInodesSQL);
		List<HashMap<String, String>> results =null;
		try {
			results = dc.getResults();
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
		}
		String query =  "select* from file_asset c where c.inode not in(select inode from inode)";
		dc.setSQL(query);
		int total = 0;
		List<String> fileAssetsInodesListFromFileSystem = (List<String>) MaintenanceUtil.findFileAssetsCanBeParsed().get(1);
		List<String> fileAssetsInodesList = new ArrayList<String>();
		for (HashMap<String, String> r : results) {
			fileAssetsInodesList.add(r.get("inode").toString());
		}
		results = null;
		for (int i = 0; i < fileAssetsInodesListFromFileSystem.size(); i++) {
			if (!fileAssetsInodesList.contains(fileAssetsInodesListFromFileSystem.get(i))) {
				total++;
			}
		}
		
		if (total>0)
		return true;
		
		else
        return false;
	}

}
