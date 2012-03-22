package com.dotmarketing.portlets.contentlet.action;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

public class ImportAuditUtil {

	public static final int STATUS_PENDING = 10;
	public static final int STATUS_COMPLETED = 20;
	public static final int STATUS_USERSTOPPED = 30;
	
	public static LRUMap cancelledImports = new LRUMap(50); 
	
	/**
	 * Should only be used when the system is starting to clean imports
	 * that were running when the system restarted itself.
	 */
	public static void voidValidateAuditTableOnStartup(){
		DotConnect dc = new DotConnect();
		dc.setSQL("DELETE FROM import_audit WHERE serverid=? AND (last_inode is null OR last_inode = '')");
		try {
			dc.addParam(APILocator.getDistributedJournalAPI().getServerId());
			dc.loadResult();
		} catch (DotDataException e) {
			Logger.error(ImportAuditUtil.class,e.getMessage(),e);
		}
		try {
			dc.setSQL("UPDATE import_audit SET status=? where serverid=?");
			dc.addParam(STATUS_COMPLETED);
			dc.addParam(APILocator.getDistributedJournalAPI().getServerId());
		}catch (Exception e) {
			Logger.error(ImportAuditUtil.class,e.getMessage(),e);
		}
		
	}
	
	public static Boolean isImportfinished (Long Id){
		DotConnect db = new DotConnect();
		db.setSQL("SELECT coalesce(status,0) as status FROM import_audit where id = ?");
		db.addParam(Id);
		int status = db.getInt("status");
		if(status == STATUS_PENDING){
			return false;
		}
		return true;
	}
	
	public static void cancelImport(long importId){
		DotConnect dc = new DotConnect();
		try {
			dc.setSQL("UPDATE import_audit SET status=? where id=?");
			dc.addParam(STATUS_USERSTOPPED);
			dc.addParam(importId);
			dc.loadResult();
			cancelledImports.put(importId, Calendar.getInstance().getTime());
		}catch (Exception e) {
			Logger.error(ImportAuditUtil.class,e.getMessage(),e);
		}
	}
	
	public static HashMap<String, List<String>> loadImportResults(Long Id){
		DotConnect db = new DotConnect();
		db.setSQL("SELECT id,last_inode, warnings, errors, results, messages FROM import_audit where id= ?");
		db.addParam(Id);
		List dbResults=null;
		try {
			dbResults=db.loadResults();
			//dbResults=db.loadResults();
		} catch (DotDataException e) {
			Logger.error(ImportAuditUtil.class,e.getMessage(),e);

		};
		HashMap<String, List<String>> resultsList = new HashMap<String, List<String>>();
		if(dbResults.size()>0){
	       String [] variables={"warnings","errors","messages", "results", "lastInode"}; 
	       for(String variable :variables){
	    	   resultsList.put(variable, new ArrayList<String>());   
	       }
	       for(String variable :variables){
	    	   String dbValues=(String)((HashMap<String, Object>)dbResults.get(0)).get(variable);
	    	   if(UtilMethods.isSet(dbValues)){
	    		  String [] values=dbValues.split(";");
	    		  for(String value :values ){
	    			  resultsList.get(variable).add(value); 
	    		  }
	    	   }  
	       }
       } else{
    	   return  null; 
       } 
       return  resultsList;
	}
	
	public static ImportAuditResults loadAuditResults(String userId){
		DotConnect db = new DotConnect();
		db.setSQL("SELECT id, start_date, userid, filename,last_inode FROM import_audit where status = ?");
		db.addParam(STATUS_PENDING);
		List<Map<String, Object>> imps;
		try {
			imps = db.loadObjectResults();
		} catch (DotDataException e) {
			Logger.error(ImportAuditUtil.class,e.getMessage(),e);
			return null;
		}
		if(imps == null || imps.size() < 1){
			return null;
		}
		ImportAuditResults ret = new ImportAuditResults();
		List<Map<String, Object>> l = ret.getUserRecords();
		Map<String, Object> map1 = new HashMap<String, Object>();
		for (Map<String, Object> map : imps) {
			if(UtilMethods.isSet(map.get("last_inode")) && APILocator.getContentletAPI().isInodeIndexed(map.get("last_inode").toString(),1)){
				db.setSQL("UPDATE import_audit SET status=? where id=?");
				db.addParam(STATUS_COMPLETED);
				db.addParam(map.get("id"));
				try {
					db.loadResult();
				} catch (DotDataException e) {
					Logger.error(ImportAuditUtil.class,e.getMessage(),e);
				}
			}else{
				if(map.get("userid").toString().trim().equals(userId)){
					l.add(map);
				}else{
					ret.setOtherUsersJobs(ret.getOtherUsersJobs() + 1);
				}
			}
		}
		ret.setUserRecords(l);
		return ret;
	}
	
	/**
	 * Will update audit table with passed in id
	 * @param lastInode
	 * @param total
	 * @param id
	 */
	public static void updateAuditRecord(String lastInode, int total, long id,HashMap <String, List<String>> importResults){
		DotConnect db = new DotConnect();
		if(importResults!=null){
			String warnings="";
			String errors="";
			String messages="";
			String results="";

			if(importResults.get("messages").size() > 0){				
			List<String>data= importResults.get("messages");
				for( String value:data){
			 		messages+=value+";";
				}
		    }
			if(importResults.get("errors").size() > 0){
			 List<String>data= importResults.get("errors");
				 for( String value:data){
					 errors+=value+";";
				 }	
			}
			if(importResults.get("warnings").size() > 0){
			List<String>data= importResults.get("warnings");
				 for( String value:data){
					 warnings+=value+";";
				 }
			}
			if(importResults.get("results").size() > 0){
			List<String>data= importResults.get("results");
				for( String value:data){
					results+=value+";";
				}
			}
			db.setSQL("UPDATE import_audit SET last_inode=?,records_to_import=?,warnings=?,errors=?,messages=?,results=? WHERE id=?");
			db.addParam(lastInode);
			db.addParam(total);
			db.addParam(warnings);
			db.addParam(errors);
			db.addParam(messages);
			db.addParam(results);

			db.addParam(id);
			try {
				db.loadResult();
			} catch (DotDataException e) {
				Logger.error(ImportAuditUtil.class,e.getMessage(),e);
			}
		}else{
			db.setSQL("UPDATE import_audit SET last_inode=?,records_to_import=? WHERE id=?");
			db.addParam(lastInode);
			db.addParam(total);
			db.addParam(id);
			try {
				db.loadResult();
			} catch (DotDataException e) {
				Logger.error(ImportAuditUtil.class,e.getMessage(),e);
			}
		}
	}
	
	/**
	 * Will create an audit record returning you its ID
	 * @param userId
	 * @param filename
	 * @return
	 */
	public static long createAuditRecord(String userId, String filename){
		DotConnect db = new DotConnect();
		db.setSQL("SELECT max(id) as max FROM import_audit");
		ArrayList<Map<String, String>> ret;
		try {
			ret = db.loadResults();
		} catch (DotDataException e1) {
			Logger.error(ImportAuditUtil.class,e1.getMessage(),e1);
			return 0;
		}
		Long maximum=null;
		if (ret != null && ret.size() > 0
				&& UtilMethods.isSet(ret.get(0).get("max"))) {	
			 maximum = Long.parseLong(ret.get(0).get("max"))+1;
			}
		else{
			maximum= Long.parseLong("1");
		}
		db.setSQL("INSERT INTO import_audit( id, start_date, userid, filename, status, serverid)VALUES (?, ?, ?, ?, ?,?)");
		db.addParam(maximum);
		db.addParam(new Date());
		db.addParam(userId);
		db.addParam(filename);
		db.addParam(STATUS_PENDING);
		db.addParam(APILocator.getDistributedJournalAPI().getServerId());
		try {
			db.loadResult();
		} catch (DotDataException e) {
			Logger.error(ImportAuditUtil.class,e.getMessage(),e);
			return 0;
		}
		return maximum;
	}
	
	public static class ImportAuditResults{
		private List<Map<String, Object>> userRecords = new ArrayList<Map<String,Object>>();
		private long otherUsersJobs = 0;
		
		public ImportAuditResults() {
			// TODO Auto-generated constructor stub
		}
		
		/**
		 * @return the userRecords
		 */
		public List<Map<String, Object>> getUserRecords() {
			return userRecords;
		}
		/**
		 * @param userRecords the userRecords to set
		 */
		public void setUserRecords(List<Map<String, Object>> userRecords) {
			this.userRecords = userRecords;
		}
		/**
		 * @return the otherUsersJobs
		 */
		public long getOtherUsersJobs() {
			return otherUsersJobs;
		}
		/**
		 * @param otherUsersJobs the otherUsersJobs to set
		 */
		public void setOtherUsersJobs(long otherUsersJobs) {
			this.otherUsersJobs = otherUsersJobs;
		}
	}
	
	public static void setAuditRecordCompleted(long id){
		DotConnect dc = new DotConnect();
		try {
			dc.setSQL("UPDATE import_audit SET status=? where id=?");
			dc.addParam(STATUS_COMPLETED);
			dc.addParam(id);
			dc.loadResult();
		}catch (Exception e) {
			Logger.error(ImportAuditUtil.class,e.getMessage(),e);
		}
	}
	
}
