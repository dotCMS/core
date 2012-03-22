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


public class FixTask00008CheckTreeInconsistencies  implements FixTask {
	

	private List <Map<String, String>> modifiedData= null;

	
	public List <Map <String,Object>> executeFix() throws DotDataException,
			DotRuntimeException {
		
		Logger.info(CMSMaintenanceFactory.class, "Beginning fixAssetsInconsistencies");
	    List <Map <String,Object>>  returnValue = new ArrayList <Map <String,Object>>();
		int counter = 0;
		
		
		//final String fix2TreeQuery = "select child,parent,relation_type from tree left join inode on tree.child  = inode.inode where inode.inode is null";
		final String fix2TreeQuery = "select child,parent,relation_type from tree left join inode on tree.child  = inode.inode left join identifier " +
		 							 "on tree.child = identifier.id where inode.inode is null and identifier.id is null ";
		//final String fix3TreeQuery = "select child,parent,relation_type from tree left join inode on tree.parent = inode.inode where inode.inode is null";
		final String fix3TreeQuery = "select child,parent,relation_type from tree left join inode on tree.parent = inode.inode left join identifier " +
		 							 "on tree.parent = identifier.id where inode.inode is null and identifier.id is null";
		final String fix4TreeQuery = "delete from tree where child = ? and parent = ? and relation_type = ?";
		
		if (!FixAssetsProcessStatus.getRunning()) {
			FixAssetsProcessStatus.startProgress();
			FixAssetsProcessStatus.setDescription("task 8: check the working and live versions of tree entries for inconsistencies");			
			HibernateUtil.startTransaction();
			try {
				DotConnect db = new DotConnect();

				//Tree Query (Child)
				String query =  fix2TreeQuery;
				Logger.debug(CMSMaintenanceFactory.class, "Running query for tree: " + query);
				db.setSQL(query);
				List<Map<String, String>> treeChildren = db.getResults();
				Logger.debug(CMSMaintenanceFactory.class, "Found " + treeChildren.size() + " Tree");
				int total = treeChildren.size();
				
				//Tree Query (Child)
				query =  fix3TreeQuery;
				Logger.debug(CMSMaintenanceFactory.class,"Running query for tree: " + query);
				db.setSQL(query);
				List<HashMap<String, String>> treeParents = db.getResults();
				Logger.debug(CMSMaintenanceFactory.class,"Found " + treeParents.size() + " Tree");
				total += treeParents.size();				
				
				Logger.info(CMSMaintenanceFactory.class,"Total number of assets: " + total);
				FixAssetsProcessStatus.setTotal(total);
				
				long inodeInode;
				long parentIdentifierInode;

				
				String identifierInode;
				List<HashMap<String, String>> versions;
				HashMap<String, String> version;
				String versionWorking;
				String DbConnFalseBoolean = DbConnectionFactory.getDBFalse().trim().toLowerCase();
				
				char DbConnFalseBooleanChar;
				if (DbConnFalseBoolean.charAt(0) == '\'')
					DbConnFalseBooleanChar = DbConnFalseBoolean.charAt(1);
				else
					DbConnFalseBooleanChar = DbConnFalseBoolean.charAt(0);
				
				String inode;
			
				//Check the tree entries that doesn't have a child o parent in the inode table
				treeChildren.addAll(treeParents);
				modifiedData=treeChildren;
				getModifiedData();
				Logger.info(CMSMaintenanceFactory.class,"Fixing " + treeChildren.size()+ " tree entries");
				for (Map<String, String> tree : treeChildren) 
				{				    
				    Logger.debug(CMSMaintenanceFactory.class,"Running query: "+ fix4TreeQuery);					
				    try
				    {
				    	db.setSQL(fix4TreeQuery);
				    	db.addParam(tree.get("child"));
				    	db.addParam(tree.get("parent"));
				    	db.addParam(tree.get("relation_type"));
				    	db.getResults();
				    }
				    catch(Exception ex)
				    {
				    	FixAssetsProcessStatus.addAError();
				    	counter++;
				    }
				    FixAssetsProcessStatus.addActual();
				}
				FixAudit Audit= new FixAudit();
				Audit.setTableName("contentlet");
				Audit.setDatetime(new Date());
				Audit.setRecordsAltered(total);
				Audit.setAction("Check the tree entries that doesn't have a child o parent in the inode table and deleted them");
				HibernateUtil.save(Audit);				
				HibernateUtil.commitTransaction();
				returnValue.add(FixAssetsProcessStatus.getFixAssetsMap());
				FixAssetsProcessStatus.stopProgress();
				Logger.debug(CMSMaintenanceFactory.class, "Ending fixAssetsInconsistencies");
			} catch(Exception e) {
				Logger.debug(CMSMaintenanceFactory.class,"There was a problem fixing asset inconsistencies",e);
				Logger.warn(CMSMaintenanceFactory.class,"There was a problem fixing asset inconsistencies",e);				
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
			_writing = new File(ConfigUtils.getBackupPath()+File.separator+"fixes" + java.io.File.separator + lastmoddate + "_"
					+ "FixTask00008CheckTreeInconsistencies" + ".xml");

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
		
		//final String fix2TreeQuery = "select child,parent,relation_type from tree left join inode on tree.child  = inode.inode where inode.inode is null";
		//final String fix3TreeQuery = "select child,parent,relation_type from tree left join inode on tree.parent = inode.inode where inode.inode is null";
		final String fix2TreeQuery = "select child,parent,relation_type from tree left join inode on tree.child  = inode.inode left join identifier " +
									 "on tree.child = identifier.id where inode.inode is null and identifier.id is null ";
		
		final String fix3TreeQuery = "select child,parent,relation_type from tree left join inode on tree.parent = inode.inode left join identifier " +
									 "on tree.parent = identifier.id where inode.inode is null and identifier.id is null";



		//Tree Query (Child)
		String query =  fix2TreeQuery;
		Logger.debug(CMSMaintenanceFactory.class, "Running query for tree: " + query);
		db.setSQL(query);
		List<HashMap<String, String>> treeChildren =null ;
		try {
			treeChildren = db.getResults();
		} catch (DotDataException e) {
			Logger.error(this, e.getMessage(), e);
		}
		Logger.debug(CMSMaintenanceFactory.class, "Found " + treeChildren.size() + " Tree");
		int total = treeChildren.size();
		
		//Tree Query (Child)
		query =  fix3TreeQuery;
		Logger.debug(CMSMaintenanceFactory.class,"Running query for tree: " + query);
		db.setSQL(query);
		List<HashMap<String, String>> treeParents =null ;
		try {
			treeParents = db.getResults();
		} catch (DotDataException e) {
			Logger.error(this,e.getMessage(), e);
		}
		Logger.debug(CMSMaintenanceFactory.class,"Found " + treeParents.size() + " Tree");
		total += treeParents.size();				
		
		
		FixAssetsProcessStatus.setTotal(total);
		if (total > 0)
			return true;
		else
			return false;
	}

}
