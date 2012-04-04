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
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.fixtask.FixTask;
import com.dotmarketing.portlets.cmsmaintenance.ajax.FixAssetsProcessStatus;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.MaintenanceUtil;
import com.dotmarketing.util.UtilMethods;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class FixTask00021CheckOrphanedAssets implements FixTask {
	
	private List <Map<String, String>> modifiedData= new  ArrayList <Map<String, String>>();


	@SuppressWarnings({ "unchecked", "deprecation" })
	public List<Map<String, Object>> executeFix() throws DotDataException,
			DotRuntimeException {
		List<Map<String, Object>> returnValue = new ArrayList<Map<String, Object>>();
		Logger.info(FixTask00021CheckOrphanedAssets.class,"Beginning CheckOrphanedAssets");
		DotConnect dc = new DotConnect();
		/*Host host = null;
		String hostId = "";
		Folder folder = null;
		FolderAPI folderAPI = APILocator.getFolderAPI();*/
		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 21: CheckOrphanedAssets");
			HibernateUtil.startTransaction();
			int total=0;
		     try {
				//User user = APILocator.getUserAPI().getSystemUser();
				/*String query = "SELECT * FROM inode WHERE (type='file_asset' or type='htmlpage') " +
				   			   "and inode NOT IN (SELECT child FROM tree WHERE parent in (SELECT inode from folder))";*/
			    String hostQuery = "select distinct host_inode from identifier";
			    dc.setSQL(hostQuery);
			    List<HashMap<String, String>> hosts = dc.getResults();
			    for(HashMap<String, String> host : hosts){
				   String hostInode = host.get("host_inode");
				   if(UtilMethods.isSet(hostInode)){
					   String query = " SELECT * FROM identifier WHERE (asset_type='file_asset' or asset_type='htmlpage') " + 
					   			      " and host_inode = ? " +
					   			      " and parent_path NOT IN (SELECT path FROM folder where host_inode = ?) ";

					   dc.setSQL(query);
					   dc.addParam(hostInode);
					   dc.addParam(hostInode);
					   List<HashMap<String, String>> assetIds = dc.getResults();
					   total = total + assetIds.size();
					   FixAssetsProcessStatus.setTotal(total);
					   for(HashMap<String, String> asset:assetIds){
						   String identifier = asset.get("id");
						   if(APILocator.getIdentifierAPI().isIdentifier(identifier)){
							   deleteOrphanedAsset(asset.get("asset_type"),asset.get("id"),dc);	
							   
							   CacheLocator.getFileCache().clearCache();
							   CacheLocator.getHTMLPageCache().clearCache();
							   CacheLocator.getIdentifierCache().clearCache();
						   }
					   }
				   }
			    }
			    FixAudit Audit = new FixAudit();
				Audit.setTableName("identifier");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(total);
				Audit.setAction("task 21: Fixed CheckOrphanedAssets");
				HibernateUtil.save(Audit);
				HibernateUtil.commitTransaction();
				MaintenanceUtil.flushCache();

				returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
				FixAssetsProcessStatus.stopProgress();
				Logger.debug(FixTask00021CheckOrphanedAssets.class,"Ending CheckOrphanedAssets");
					/*if(APILocator.getIdentifierAPI().isIdentifier(identifier)){
							//Identifier ident = (Identifier) InodeFactory.getInode(identifier, Identifier.class);
						Identifier ident = APILocator.getIdentifierAPI().find(identifier);
							hostId = ident.getHostId();
							if(hostId == null){
								host = APILocator.getHostAPI().findDefaultHost(user,false);
								hostId = host.getInode();
							}
							String uri = ident.getURI();
							if(UtilMethods.isSet(uri)){
								int index = uri.lastIndexOf("/");
								
								if (-1 < index);
									uri = uri.substring(0, index);
							}
							folder = FolderFactory.getFolderByPath(uri, hostId);
							HibernateUtil.startTransaction();
							if(folderAPI.doesFolderExist(folder.getPath(),hostId)){
						      dc.setSQL("Insert into tree(child,parent,relation_type,tree_order) values(?,?,?,?)");
							  dc.addParam(asset.get("inode"));
							  dc.addParam(folder.getInode());
							  dc.addParam("child");
							  dc.addParam(0);
							  dc.loadResult();
							  dc.setSQL("select * from tree where child = ? and parent = ?");
							  dc.addParam(asset.get("inode"));
							  dc.addParam(folder.getInode());
							  modifiedData.addAll(dc.loadResults());
							}else{
							  deleteOrphanedAsset(asset.get("type"),asset.get("id"),dc);	
							  FileCache.clearCache();
							  CacheLocator.getHTMLPageCache().clearCache();
							  IdentifierCache.clearCache();
							}	
						}*/						
			} catch (Exception e) {
				Logger.error(this, "Unable to execute CheckOrphanedAssets Task",e);
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
			_writing = new File(ConfigUtils.getBackupPath()+File.separator+"fixes" + java.io.File.separator  + lastmoddate + "_"
					+ "FixTask00021CheckOrphanedAssets" + ".xml");

			BufferedOutputStream _bout = null;
			try {
				_bout = new BufferedOutputStream(new FileOutputStream(_writing));
			} catch (FileNotFoundException e) {

			}
			_xstream.toXML(modifiedData, _bout);
		}
		return modifiedData;
	}
	
	@SuppressWarnings("unchecked")
	private void deleteOrphanedAsset(String type,String identifier,DotConnect dc){
		
		String deleteInodes = "DELETE FROM inode WHERE inode IN (select inode from htmlpage where identifier = ? ) " +
							  "OR inode IN (select inode from file_asset where identifier = ?)";
		
		String deleteIdentifier = "DELETE FROM identifier WHERE id = ?";
		
		String deleteHTMLPages = "DELETE FROM htmlpage WHERE identifier = ?";
		
		String deleteFileAssets = "DELETE FROM file_asset WHERE identifier = ?";
		
		String InodesToDelete = "SELECT * FROM inode WHERE inode IN (select inode from htmlpage where identifier = ? ) " +
								"OR inode IN (select inode from file_asset where identifier = ?)";
		
		String IdentifierToDelete = "SELECT * FROM identifier WHERE id = ?";
		
		String HTMLPagesToDelete = "SELECT * FROM htmlpage WHERE identifier = ?";
		
		String FileAssetsToDelete = "SELECT * FROM file_asset WHERE identifier = ?";
		
		try {
			 dc.setSQL("DELETE FROM tree WHERE child IN (select inode from htmlpage where identifier = ? ) " +
					   "OR child IN (select inode from file_asset where identifier = ?)");
			 dc.addParam(identifier);
			 dc.addParam(identifier);
			 dc.loadResult();
			 dc.setSQL(InodesToDelete);
			 dc.addParam(identifier);
			 dc.addParam(identifier);
			 modifiedData = dc.loadResults();
			 dc.setSQL(IdentifierToDelete);
			 dc.addParam(identifier);
			 modifiedData = dc.loadResults();
			if(type.equalsIgnoreCase("file_asset")){
				dc.setSQL(FileAssetsToDelete);
			    dc.addParam(identifier);
			    ArrayList<HashMap<String, String>> assetList = dc.loadResults();
			    if(assetList.size()> 0){
			    	 modifiedData = dc.loadResults();
			    }else{
			    	dc.setSQL("SELECT * from identifier where id = ?");
			    	dc.addParam(identifier);
			    	modifiedData.addAll(dc.loadResults());
			    }
			    if(assetList.size()>0){
			      //DELETE Orphaned File Assets	
			      dc.setSQL(deleteFileAssets);	
			      dc.addParam(identifier);
			      dc.loadResult();
			      dc.setSQL(deleteIdentifier);
			      dc.addParam(identifier);
			      dc.loadResult();
			      dc.setSQL(deleteInodes);
			      dc.addParam(identifier);
			      dc.addParam(identifier);
			      dc.loadResult();
			    }else{
			    	dc.setSQL("DELETE from identifier where id = ?");
			    	dc.addParam(identifier);
			    	dc.loadResult();
			    }
			 }else{
				 dc.setSQL(HTMLPagesToDelete);
				 dc.addParam(identifier);
				 modifiedData = dc.loadResults();
				 //DELETE Orphaned HTMLPage Assets
				 dc.setSQL(deleteHTMLPages);	
			      dc.addParam(identifier);
			      dc.loadResult();
			      dc.setSQL(deleteIdentifier);
			      dc.addParam(identifier);
			      dc.loadResult();
			      dc.setSQL(deleteInodes);
			      dc.addParam(identifier);
			      dc.addParam(identifier);
			      dc.loadResult();
			 }		
		} catch (Exception e) {
			throw new DotRuntimeException("Unable to delete Orphaned Asset",e);
		}
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	public boolean shouldRun() {
		DotConnect dc = new DotConnect();
		/*String query = "SELECT * FROM inode WHERE (type='file_asset' or type='htmlpage') " +
				       "and inode NOT IN (SELECT child FROM tree WHERE parent in (SELECT inode from folder))";*/
		int total = 0;
		String hostQuery = " select distinct host_inode from identifier ";
		dc.setSQL(hostQuery);
		List<HashMap<String, String>> hosts =null;
		try {
			hosts = dc.getResults();
			for(HashMap<String, String> host : hosts){
				String hostInode = host.get("host_inode");
				if(UtilMethods.isSet(hostInode)){
					String query = " SELECT * FROM identifier WHERE (asset_type='file_asset' or asset_type='htmlpage') " + 
					   			   " and host_inode = ? " +
					   			   " and parent_path NOT IN (SELECT path FROM folder where host_inode = ?) ";

					dc.setSQL(query);
					dc.addParam(hostInode);
					dc.addParam(hostInode);
					List<HashMap<String, String>> assetIds ;
					assetIds = dc.getResults();
					total = total + assetIds.size();
				}
			}
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
		}
		
		if (total > 0)
		  return true;
		else
		  return false;
	}
}
