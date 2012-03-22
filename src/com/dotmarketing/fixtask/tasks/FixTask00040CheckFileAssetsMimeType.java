/**
 * 
 */
package com.dotmarketing.fixtask.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.UtilMethods;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * @author jasontesser
 *
 */
public class FixTask00040CheckFileAssetsMimeType implements FixTask {
	
	private List<Map<String, String>> modifiedData = new ArrayList<Map<String,String>>();
	
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> executeFix() throws DotDataException, DotRuntimeException {
		String filesWithInvalidMimeType = "SELECT * " +
										  "FROM file_asset " +
										  "WHERE mime_type = '' OR " +
												"mime_type IS NULL";
		
		String updateFileWithInvalidMimeType = "UPDATE file_asset " +
											   "SET mime_type = ? " +
											   "WHERE inode = ?";
		
		String addFileAssetMimeTypeNotNullPostgres = "ALTER TABLE file_asset ALTER COLUMN mime_type SET NOT NULL";
		String addFileAssetMimeTypeNotNullMySQL = "ALTER TABLE file_asset MODIFY COLUMN mime_type VARCHAR(255) NOT NULL";
		String addFileAssetMimeTypeNotNullOracle = "ALTER TABLE file_asset MODIFY mime_type VARCHAR2(255) NOT NULL";
		String addFileAssetMimeTypeNotNullMSSQL = "ALTER TABLE file_asset ALTER COLUMN mime_type VARCHAR(255) NOT NULL";
		
		List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();
		Logger.info(FixTask00040CheckFileAssetsMimeType.class,"Beginning CheckFileAssetsMimeType");
		
		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 40: CheckFileAssetsMimeType");
			HibernateUtil.startTransaction();
			int total = 0;
			 try {
		       DotConnect dc = new DotConnect();
		       dc.setSQL(filesWithInvalidMimeType);
		       List<Map<String, String>> filesWithInvalidMimeTypeResult = dc.loadResults();
		    
		       total = total + filesWithInvalidMimeTypeResult.size();
		       FixAssetsProcessStatus.setTotal(total);
		
		       if ((filesWithInvalidMimeTypeResult != null) && (0 < filesWithInvalidMimeTypeResult.size())) {
			
		    	   Map<String, String> fileWithInvalidMimeType;
		    	   String mimeType;
		    	   for (int i = 0; i < filesWithInvalidMimeTypeResult.size(); ++i) {
					 fileWithInvalidMimeType = filesWithInvalidMimeTypeResult.get(i);
					 mimeType = Config.CONTEXT.getMimeType(fileWithInvalidMimeType.get("file_name"));
					
					 if (!UtilMethods.isSet(mimeType)) {
						mimeType = com.dotmarketing.portlets.files.model.File.UNKNOWN_MIME_TYPE;
					 }
					
					 modifiedData.add(fileWithInvalidMimeType);
					
					dc.setSQL(updateFileWithInvalidMimeType);
					dc.addParam(mimeType);
					dc.addParam(fileWithInvalidMimeType.get("inode"));
					dc.loadResult();
				}
				
				if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)) {
					dc.setSQL(addFileAssetMimeTypeNotNullPostgres);
					dc.loadResult();
				} else if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)) {
					dc.setSQL(addFileAssetMimeTypeNotNullMySQL);
					dc.loadResult();
				} else if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.ORACLE)) {
					dc.setSQL(addFileAssetMimeTypeNotNullOracle);
					dc.loadResult();
				} else if (DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)) {
					dc.setSQL(addFileAssetMimeTypeNotNullMSSQL);
					dc.loadResult();
				}
		  }
		    FixAudit Audit = new FixAudit();
			Audit.setTableName("file_asset");
			Audit.setDatetime(new Date());
			Audit.setRecordsAltered(total);
			Audit.setAction("task 40: Fixed CheckFileAssetsMimeType");
			HibernateUtil.save(Audit);
			HibernateUtil.commitTransaction();
			MaintenanceUtil.flushCache();

			returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
			FixAssetsProcessStatus.stopProgress();
			Logger.debug(FixTask00040CheckFileAssetsMimeType.class,"Ending CheckFileAssetsMimeType");
	     } catch (Exception e) {
		   Logger.error(this, "Unable to check file assets with invalid mime type", e);
		   HibernateUtil.rollbackTransaction();
		   modifiedData.clear();
		   FixAssetsProcessStatus.stopProgress();
		   FixAssetsProcessStatus.setActual(-1);
		}
	  }
	return returnValue;
	}
	
	public List<Map<String, String>> getModifiedData() {
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
					+ "FixTask00040CheckFileAssetsMimeType" + ".xml");

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
		return true;
	}
}