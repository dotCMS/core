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
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * @author jasontesser
 *
 */
public class FixTask00020DeleteOrphanedIdentifiers implements FixTask{

	private List <Map<String, String>>  modifiedData= new  ArrayList <Map<String,String>>();
	
	public List<Map<String, Object>> executeFix() throws DotDataException,
			DotRuntimeException {
		/*String inodesToDelete = "SELECT * from inode where (type = 'identifier' and inode not in (SELECT inode FROM identifier)) OR identifier in (SELECT inode from inode where type = 'identifier' and inode not in (SELECT inode FROM identifier))";
		String treesToDelete = "SELECT * from tree where child IN (SELECT inode from inode where type = 'identifier' and inode not in (SELECT inode FROM identifier)) OR parent IN (SELECT inode from inode where type = 'identifier' and inode not in (SELECT inode FROM identifier))";
		
		String deleteTreesToDelete = "DELETE FROM tree where child IN (SELECT inode from inode where type = 'identifier' and inode not in (SELECT inode FROM identifier)) OR parent IN (SELECT inode from inode where type = 'identifier' and inode not in (SELECT inode FROM identifier))";
		String deleteInodesToDelete = "DELETE FROM inode where (type = 'identifier' and inode not in (SELECT inode FROM identifier)) OR identifier in (SELECT inode from inode where type = 'identifier' and inode not in (SELECT inode FROM identifier))";
		*/
		List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();
		Logger.info(FixTask00020DeleteOrphanedIdentifiers.class,"Beginning DeleteOrphanedIdentifiers");
		
		String identifiersToDelete = "select * from identifier where (asset_type='contentlet' and id not in(select identifier from contentlet )) " + 
                                "OR (asset_type='htmlpage' and id not in(select identifier from htmlpage)) " +
                                "OR (asset_type='file_asset' and id not in(select identifier from file_asset)) " +
                                "OR (asset_type='links' and id not in(select identifier from links)) " +
                                "OR (asset_type='containers' and id not in(select identifier from containers)) " +
                                "OR (asset_type='template' and id not in(select identifier from template)) ";
		
		String treesToDelete = "SELECT * from tree where child IN (SELECT id from identifier where asset_type='contentlet' and id NOT IN(select identifier from contentlet)) " + 
        					   "OR child in (SELECT id from identifier where asset_type='htmlpage' and id NOT IN(select identifier from htmlpage)) " + 
        					   "OR child in (SELECT id from identifier where asset_type='file_asset' and id NOT IN(select identifier from file_asset)) " + 
        					   "OR child in (SELECT id from identifier where asset_type='links' and id NOT IN(select identifier from links)) " + 
        					   "OR child in (SELECT id from identifier where asset_type='containers' and id NOT IN(select identifier from containers)) " +
        					   "OR child in (SELECT id from identifier where asset_type='template' and id NOT IN(select identifier from template)) " +                       
        					   "OR parent in (SELECT id from identifier where asset_type='contentlet' and id NOT IN(select identifier from contentlet)) " + 
        					   "OR parent in (SELECT id from identifier where asset_type='htmlpage' and id NOT IN(select identifier from htmlpage)) " + 
        					   "OR parent in (SELECT id from identifier where asset_type='file_asset' and id NOT IN(select identifier from file_asset)) " + 
        					   "OR parent in (SELECT id from identifier where asset_type='links' and id NOT IN(select identifier from links)) " + 
        					   "OR parent in (SELECT id from identifier where asset_type='containers' and id NOT IN(select identifier from containers)) " +
        					   "OR parent in (SELECT id from identifier where asset_type='template' and id NOT IN(select identifier from template)) ";
		
		String deleteTreesToDelete = "DELETE from tree where child IN (SELECT id from identifier where asset_type='contentlet' and id NOT IN(select identifier from contentlet)) " + 
		   					         "OR child in (SELECT id from identifier where asset_type='htmlpage' and id NOT IN(select identifier from htmlpage)) " + 
		   					         "OR child in (SELECT id from identifier where asset_type='file_asset' and id NOT IN(select identifier from file_asset)) " + 
		   					         "OR child in (SELECT id from identifier where asset_type='links' and id NOT IN(select identifier from links)) " + 
		   					         "OR child in (SELECT id from identifier where asset_type='containers' and id NOT IN(select identifier from containers)) " +
		   					         "OR child in (SELECT id from identifier where asset_type='template' and id NOT IN(select identifier from template)) " +                       
		   					         "OR parent in (SELECT id from identifier where asset_type='contentlet' and id NOT IN(select identifier from contentlet)) " + 
		   					         "OR parent in (SELECT id from identifier where asset_type='htmlpage' and id NOT IN(select identifier from htmlpage)) " + 
		   					         "OR parent in (SELECT id from identifier where asset_type='file_asset' and id NOT IN(select identifier from file_asset)) " + 
		   					         "OR parent in (SELECT id from identifier where asset_type='links' and id NOT IN(select identifier from links)) " + 
		   					         "OR parent in (SELECT id from identifier where asset_type='containers' and id NOT IN(select identifier from containers)) " +
		   					         "OR parent in (SELECT id from identifier where asset_type='template' and id NOT IN(select identifier from template)) ";
		
		String deleteIdentifiersToDelete = "DELETE FROM identifier where (asset_type='contentlet' and id not in(select identifier from contentlet )) " + 
        							       "OR (asset_type='htmlpage' and id not in(select identifier from htmlpage)) " +
        							       "OR (asset_type='file_asset' and id not in(select identifier from file_asset)) " +
        							       "OR (asset_type='links' and id not in(select identifier from links)) " +
        							       "OR (asset_type='containers' and id not in(select identifier from containers)) " +
        							       "OR (asset_type='template' and id not in(select identifier from template)) ";
     if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 20: DeleteOrphanedIdentifiers");
			HibernateUtil.startTransaction();
			int total=0;	
			int error=0;
		    try {
				DotConnect dc = new DotConnect();
				dc.setSQL(treesToDelete);
				modifiedData = dc.loadResults();
				total = total + dc.getResults().size();
				dc.setSQL(identifiersToDelete);
				modifiedData.addAll(dc.loadResults());
				total = total + dc.getResults().size();
				FixAssetsProcessStatus.setTotal(total);
				getModifiedData();
				if(total > 0){
				  try{
					HibernateUtil.startTransaction();
					dc.executeStatement(deleteTreesToDelete);
					dc.executeStatement(deleteIdentifiersToDelete);
					FixAssetsProcessStatus.setError(total);
					/*if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
						deleteInodesInMySQL(dc);
					}else{
					    dc.executeStatement(deleteIdentifiersToDelete);
					}*/
					HibernateUtil.commitTransaction();
				 }catch (Exception e) {
					Logger.error(this, "Unable to clean orphaned identifiers",e);
					HibernateUtil.rollbackTransaction();
					modifiedData.clear();
				  }
				}
				FixAudit Audit = new FixAudit();
				Audit.setTableName("identifier");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(total);
				Audit.setAction("task 20: Fixed DeleteOrphanedIdentifiers");
				HibernateUtil.save(Audit);
				HibernateUtil.commitTransaction();
				MaintenanceUtil.flushCache();

				returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
				FixAssetsProcessStatus.stopProgress();
				Logger.debug(FixTask00020DeleteOrphanedIdentifiers.class,"Ending DeleteOrphanedIdentifiers");
			} catch (Exception e) {
				Logger.debug(FixTask00020DeleteOrphanedIdentifiers.class,"There was a problem during DeleteOrphanedIdentifiers", e);
				HibernateUtil.rollbackTransaction();
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
					+ "FixTask00020DeleteOrphanedIdentifiers" + ".xml");

			BufferedOutputStream _bout = null;
			try {
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
			} catch (FileNotFoundException e) {

			}
			_xstream.toXML(modifiedData, _bout);
		}
		return modifiedData;
	}
	private void deleteInodesInMySQL(DotConnect dc)throws DotDataException,
				DotRuntimeException {
		int count = 0;
		try {
			dc.setSQL("SELECT count(*) size from inode where type = 'identifier' and inode not in (SELECT inode FROM identifier)");
			List<HashMap<String, String>> rs = dc.loadResults();
			int size = Integer.parseInt(rs.get(0).get("size"));
			if(size > 500)
			  count = (int)Math.ceil(size/500.00);
			else
			   count=1;
			for(int i=0;i<count;i++){
				
				dc.setSQL("SELECT inode from inode where type = 'identifier' and " 
						+ " inode not in (SELECT inode FROM identifier) order by inode"
						+ " limit 500 offset " + i*500);
				
			    List<HashMap<String, String>> identifiers = dc.loadResults();
				StringBuilder identCondition = new StringBuilder(128);
			    identCondition.ensureCapacity(32);
				identCondition.append("");

				for (HashMap<String, String> inode : identifiers) {
					if (0 < identCondition.length())
						identCondition.append(",'" + inode.get("inode")+"'");
					else
						identCondition.append("'"+inode.get("inode")+"'");
				}
				if(identCondition.length()>0)
				  dc.executeStatement("DELETE FROM inode where (type = 'identifier' and inode not in (SELECT inode FROM identifier)) "
							        + "OR identifier in (" +identCondition + ")");
			}
		} catch (Exception e) {
			Logger.error(this, "Unable to clean orphaned identifiers",e);
			HibernateUtil.rollbackTransaction();
			modifiedData.clear();
		} 
		
	}
	public boolean shouldRun() {
		return true;
	}

}
