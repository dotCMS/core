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

/**
 * @author jasontesser
 *
 */
public class FixTask00030DeleteOrphanedAssets implements FixTask {
	
	private List<Map<String, String>> modifiedData = new ArrayList<Map<String,String>>();
	
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> executeFix() throws DotDataException, DotRuntimeException {
		String identifiersToDelete = "SELECT * " +
									 "FROM identifier " +
									 "WHERE host_inode NOT IN (SELECT identifier " +
															   "FROM contentlet " +
															   "WHERE structure_inode = (SELECT inode " +
																						 "FROM structure " +
																						 "WHERE velocity_var_name='Host'))";
		
		String inodesToDelete = "SELECT * FROM inode WHERE inode in(select inode from htmlpage where identifier = ? ) " +
								"OR inode in(select inode from links where identifier = ?) " + 
								"OR inode in(select inode from file_asset where identifier = ?)";
		
		String htmlPagesToDelete = "SELECT * FROM htmlpage WHERE identifier = ? ";
		String linksToDelete = "SELECT * FROM links WHERE identifier = ? ";
		String fileAssetsToDelete = "SELECT * FROM file_asset WHERE identifier = ? ";
		
		String deleteTrees = "DELETE FROM tree WHERE child IN (select inode from htmlpage where identifier = ? ) " +
							 "OR child IN(select inode from links where identifier = ?) " + 
							 "OR child IN(select inode from file_asset where identifier = ?) " +
							 "OR parent IN(select inode from htmlpage where identifier = ?) " +
							 "OR parent IN(select inode from links where identifier = ?) " + 
							 "OR parent IN(select inode from file_asset where identifier = ?)" ;
		
		String deleteInodes = "DELETE FROM inode WHERE inode in(select inode from htmlpage where identifier = ? ) " +
							  "OR inode in(select inode from links where identifier = ?) " + 
							  "OR inode in(select inode from file_asset where identifier = ?)";
		
		String deleteIdentifier = "DELETE FROM identifier WHERE id = ?";
		
		String deleteHTMLPages = "DELETE FROM htmlpage WHERE identifier = ? ";
		String deleteLinks = "DELETE FROM links WHERE identifier = ? ";
		String deleteFileAssets = "DELETE FROM file_asset WHERE identifier = ? ";
		
		List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();
		Logger.info(FixTask00030DeleteOrphanedAssets.class,"Beginning DeleteOrphanedAssets");
		
		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 30: DeleteOrphanedAssets");
			HibernateUtil.startTransaction();
			int total = 0;
			try {
			   DotConnect dc = new DotConnect();
		       dc.setSQL(identifiersToDelete);
		       List<Map<String, String>> identifiersToDeleteResult = dc.loadResults();
		    
		       total = total + identifiersToDeleteResult.size();
			   FixAssetsProcessStatus.setTotal(total);
		
		       if ((identifiersToDeleteResult != null) && (0 < identifiersToDeleteResult.size())) {
			 
				 modifiedData = identifiersToDeleteResult;
				
				 List<Map<String, String>> inodesToDeleteResult;
				 List<Map<String, String>> assetsToDeleteResult;
				 Map<String, String> identifierToDelete;
				 boolean assetDeleted;
				
				 for (int i = 0; i < identifiersToDeleteResult.size(); ++i) {
					identifierToDelete = identifiersToDeleteResult.get(i);
					assetDeleted = false;
					dc.setSQL(inodesToDelete);
					dc.addParam(identifierToDelete.get("id"));
					dc.addParam(identifierToDelete.get("id"));
					dc.addParam(identifierToDelete.get("id"));
					inodesToDeleteResult = dc.loadResults();
					
					if ((inodesToDeleteResult != null) && (0 < inodesToDeleteResult.size())) {
						modifiedData.addAll(inodesToDeleteResult);
						
						if (inodesToDeleteResult.get(0).get("type").equals("htmlpage")) {
							Logger.debug(this, "Deleting orphan HTMLPage with Identifier='" + identifierToDelete.get("id") + "'");
							
							dc.setSQL(htmlPagesToDelete);
							dc.addParam(identifierToDelete.get("id"));
							assetsToDeleteResult = dc.loadResults();
							
							if ((assetsToDeleteResult != null) && (0 < assetsToDeleteResult.size())) {
								modifiedData.addAll(assetsToDeleteResult);
								
								dc.setSQL(deleteHTMLPages);
								dc.addParam(identifierToDelete.get("id"));
								dc.loadResult();
								
								assetDeleted = true;
							}
						} else if (inodesToDeleteResult.get(0).get("type").equals("links")) {
							Logger.debug(this, "Deleting orphan Link with Identifier='" + identifierToDelete.get("id") + "'");
							
							dc.setSQL(linksToDelete);
							dc.addParam(identifierToDelete.get("id"));
							assetsToDeleteResult = dc.loadResults();
							
							if ((assetsToDeleteResult != null) && (0 < assetsToDeleteResult.size())) {
								modifiedData.addAll(assetsToDeleteResult);
								
								dc.setSQL(deleteLinks);
								dc.addParam(identifierToDelete.get("id"));
								dc.loadResult();
								
								assetDeleted = true;
							}
						} else if (inodesToDeleteResult.get(0).get("type").equals("file_asset")) {
							Logger.debug(this, "Deleting orphan File Asset with Identifier='" + identifierToDelete.get("id") + "'");
							
							dc.setSQL(fileAssetsToDelete);
							dc.addParam(identifierToDelete.get("id"));
							assetsToDeleteResult = dc.loadResults();
							
							if ((assetsToDeleteResult != null) && (0 < assetsToDeleteResult.size())) {
								modifiedData.addAll(assetsToDeleteResult);
								
								dc.setSQL(deleteFileAssets);
								dc.addParam(identifierToDelete.get("id"));
								dc.loadResult();
								
								assetDeleted = true;
							}
						}
						
						if (assetDeleted) {
							dc.setSQL(deleteTrees);
							dc.addParam(identifierToDelete.get("id"));
							dc.addParam(identifierToDelete.get("id"));
							dc.addParam(identifierToDelete.get("id"));
							dc.addParam(identifierToDelete.get("id"));
							dc.addParam(identifierToDelete.get("id"));
							dc.addParam(identifierToDelete.get("id"));
							dc.loadResult();
							
							dc.setSQL(deleteInodes);
							dc.addParam(identifierToDelete.get("id"));
							dc.addParam(identifierToDelete.get("id"));
							dc.addParam(identifierToDelete.get("id"));
							dc.loadResult();
						}
					}
					
					if (assetDeleted) {
						dc.setSQL(deleteIdentifier);
						dc.addParam(identifierToDelete.get("id"));
						dc.loadResult();
						
						Logger.debug(this, "Delete completed");
					}
				}
		     }
		        FixAudit Audit = new FixAudit();
				Audit.setTableName("identifier");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(total);
				Audit.setAction("task 30: Fixed DeleteOrphanedAssets");
				HibernateUtil.save(Audit);
				HibernateUtil.commitTransaction();
				MaintenanceUtil.flushCache();

				returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
				FixAssetsProcessStatus.stopProgress();
				Logger.debug(FixTask00030DeleteOrphanedAssets.class,"Ending DeleteOrphanedAssets");
			} catch (Exception e) {
				Logger.error(this, "Unable to clean orphaned assets", e);
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
					+ "FixTask00030DeleteOrphanedAssets" + ".xml");

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