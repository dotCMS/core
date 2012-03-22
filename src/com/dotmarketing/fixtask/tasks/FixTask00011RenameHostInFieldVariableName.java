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
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class FixTask00011RenameHostInFieldVariableName implements FixTask {

	private List <Map<String, String>> modifiedData = new ArrayList<Map<String,String>>();
	private final static String selectInodesSQL = "select inode from field where lower(rtrim(ltrim(velocity_var_name))) = 'host'";
	private final static String updateQuery = "update field set velocity_var_name = 'contentHost' where inode = ?";
	
	@SuppressWarnings({ "unchecked", "deprecation" })
	public List<Map<String, Object>> executeFix() throws DotDataException, DotRuntimeException {
		List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();
		Logger.info(FixTask00011RenameHostInFieldVariableName.class,"Beginning RenameHostInFieldVariableName");
		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 11: Renaming structure fields with variable name 'host'");			
			HibernateUtil.startTransaction();
			
			try {
				DotConnect dc = new DotConnect();
				int counter = 0;
				dc.setSQL(selectInodesSQL);
				List<HashMap<String, String>> results = dc.getResults();
				
				HashMap <String,String> data;
				for (HashMap<String, String> result: results) {
					data = new HashMap<String, String>();
					data.put("" + (counter + 1), "inode=" + result.get("inode"));
					modifiedData.add(data);
					
					dc.setSQL(updateQuery);
					dc.addParam(result.get("inode"));
					dc.getResult();
					
					++counter;
				}
				
				FixAssetsProcessStatus.setTotal(counter);
				
				getModifiedData();
				FixAudit Audit = new FixAudit();
				Audit.setTableName("field");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(results.size());
				Audit.setAction("task 11: Renaming structure fields with variable name 'host'");
				HibernateUtil.save(Audit);
				HibernateUtil.commitTransaction();
				MaintenanceUtil.flushCache();
				MaintenanceUtil.deleteStaticFileStore();
				returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
				FixAssetsProcessStatus.stopProgress();
				Logger.debug(FixTask00011RenameHostInFieldVariableName.class, "Ending RenameHostInFieldVariableName");
			} catch (Exception e) {
				Logger.debug(FixTask00011RenameHostInFieldVariableName.class,"There was a problem fixing fields with variable name 'host'", e);
				Logger.warn(FixTask00011RenameHostInFieldVariableName.class,"There was a problem fixing fields with variable name 'host'", e);
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
			
			
			if (!new java.io.File(ConfigUtils.getBackupPath()+File.separator+"fixes").exists()) {
				new java.io.File(ConfigUtils.getBackupPath()+File.separator+"fixes").mkdir();
			}
			
			_writing = new java.io.File(ConfigUtils.getBackupPath()+File.separator+"fixes");
			
			if (!_writing.exists()) {
				_writing.mkdirs();
			}
			
			_writing = new java.io.File(ConfigUtils.getBackupPath()+File.separator+"fixes" + java.io.File.separator + lastmoddate + "_"
					+ "FixTask00011RenameHostInFieldVariableName" + ".xml");
			
			BufferedOutputStream _bout = null;
			try {
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
			} catch (FileNotFoundException e) {
			}
			_xstream.toXML(modifiedData, _bout);
		}
		return modifiedData;
	}
	
	@SuppressWarnings({ "unchecked", "deprecation" })
	public boolean shouldRun() {
		DotConnect dc = new DotConnect();
		dc.setSQL(selectInodesSQL);
		List<HashMap<String, String>> results =null;
		try {
			results = dc.getResults();
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
		}
		
		if (0 < results.size())
			return true;
		else
			return false;
	}
}