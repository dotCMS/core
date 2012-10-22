package com.dotcms.publisher.business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;

/**
 * Implement the PublishQueueAPI abstract class methods
 * @author Oswaldo
 *
 */
public class PublisherAPIImpl extends PublisherAPI{

	private static final int _ASSET_LENGTH_LIMIT = 20;
	private static final String SPACE = " ";
	private static final String IDENTIFIER = "identifier:";
	private static PublisherAPIImpl instance= null;
	
	public static PublisherAPIImpl getInstance() {
		if(instance==null){
			instance = new PublisherAPIImpl();
		}	
		
		return instance;
	}
	protected PublisherAPIImpl(){
		// Exists only to defeat instantiation.
	}
	
	private static final String MANDATORY_FIELDS= 
										"operation, "+
										"asset, "+ 
										"entered_date, "+
										"language_id, "+
										"in_error, "+ 
										"publish_date, "+ 
										"server_id, "+ 
										"type, "+ 
										"bundle_id, " +
										"target";
	private static final String MANDATORY_PLACE_HOLDER = "?,?,?,?,?,?,?,?,?,?" ;
	
//	
	//"last_results, "+ 
	//	"last_try,  "+
	//	"num_of_tries, "+ 
	
	private static final String PGINSERTSQL="insert into publishing_queue("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";
	private static final String MYINSERTSQL="insert into publishing_queue("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";
	private static final String MSINSERTSQL="insert into publishing_queue("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";
	private static final String OCLINSERTSQL="insert into publishing_queue("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";
	
	
	public void addContentsToPublish(List<String> identifiers, String bundleId) throws DotPublisherException {		
		if(identifiers != null) {
			
			try{
				HibernateUtil.startTransaction();
				for(String identifier: identifiers) {
					DotConnect dc = new DotConnect();
					if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
						dc.setSQL(PGINSERTSQL);
					} else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
						dc.setSQL(MYINSERTSQL);
					} else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
						dc.setSQL(MSINSERTSQL);
					} else{
						dc.setSQL(OCLINSERTSQL);
					}
					
					dc.addParam(PublisherAPI.ADD_OR_UPDATE_ELEMENT);
					dc.addObject(identifier); //asset
					dc.addParam(new Date());
					dc.addObject(1);
					dc.addParam(DbConnectionFactory.getDBFalse());	//in error field
					
					//TODO How do I get new columns value?	
					dc.addParam(new Date());
					dc.addObject(null);
					dc.addObject(null);
					dc.addObject(bundleId);
					dc.addObject(null);
					
					dc.loadResult();	
				}
				
				HibernateUtil.commitTransaction();			
			}catch(Exception e){
	
				try {
					HibernateUtil.rollbackTransaction();
				} catch (DotHibernateException e1) {
					Logger.debug(PublisherAPIImpl.class,e.getMessage(),e1);
				}			
				Logger.debug(PublisherAPIImpl.class,e.getMessage(),e);
				throw new DotPublisherException("Unable to add element to publish queue table:" + e.getMessage(), e);
			}finally{
				DbConnectionFactory.closeConnection();
			}
		}
	}
	
	public void addContentsToUnpublish(List<String> identifiers, String bundleId) throws DotPublisherException {		
		List<String> assets = null;
		if(identifiers != null) {
		
			
			try{
				HibernateUtil.startTransaction();
				for(String identifier: identifiers) {
					DotConnect dc = new DotConnect();
					if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
						dc.setSQL(PGINSERTSQL);
					} else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
						dc.setSQL(MYINSERTSQL);
					} else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
						dc.setSQL(MSINSERTSQL);
					} else{
						dc.setSQL(OCLINSERTSQL);
					}
					
					dc.addParam(PublisherAPI.DELETE_ELEMENT);
					dc.addObject(identifier); //asset
					dc.addParam(new Date());
					dc.addObject(1);
					dc.addParam(DbConnectionFactory.getDBFalse());	//in error field
					
					//TODO How do I get new columns value?	
					dc.addParam(new Date());
					dc.addObject(null);
					dc.addObject(null);
					dc.addObject(bundleId);
					dc.addObject(null);
					
					dc.loadResult();	
				}
				
				HibernateUtil.commitTransaction();			
			}catch(Exception e){
	
				try {
					HibernateUtil.rollbackTransaction();
				} catch (DotHibernateException e1) {
					Logger.debug(PublisherAPIImpl.class,e.getMessage(),e1);
				}			
				Logger.debug(PublisherAPIImpl.class,e.getMessage(),e);
				throw new DotPublisherException("Unable to add element to publish queue table:" + e.getMessage(), e);
			}finally{
				DbConnectionFactory.closeConnection();
			}
		}
	}
	
	private List<String> prepareAssets(List<Contentlet> contents, boolean isLive) {
		StringBuilder assetBuffer = new StringBuilder();
		List<String> assets;
		assets = new ArrayList<String>();
		
		if(contents.size() == 1) {
			assetBuffer.append("+"+IDENTIFIER+contents.get(0).getIdentifier());
			if(isLive)
				assets.add(assetBuffer.toString() +" +live:true");
			else
				assets.add(assetBuffer.toString() +" +working:true");
			
		} else {
			int counter = 1;
			Contentlet c = null;
			for(int ii = 0; ii < contents.size(); ii++) {
				c = contents.get(ii);
				
				assetBuffer.append(IDENTIFIER+c.getIdentifier());
				assetBuffer.append(SPACE);
				
				if(counter == _ASSET_LENGTH_LIMIT || (ii+1 == contents.size())) {
					if(isLive)
						assets.add("+("+assetBuffer.toString()+") +live:true");
					else
						assets.add("+("+assetBuffer.toString()+") +working:true");
					
					assetBuffer = new StringBuilder();
					counter = 0;
				} else
					counter++;
			}
		}
		return assets;
	}
	
	private static final String TREE_QUERY = "select * from tree where child = ? or parent = ?";
	/**
	 * Get tree data of a content
	 * @param indentifier
	 * @return
	 * @throws DotPublisherException 
	 */
	public List<Map<String,Object>> getContentTreeMatrix(String id) throws DotPublisherException {
		List<Map<String,Object>> res = null;
		DotConnect dc=new DotConnect();
		dc.setSQL(TREE_QUERY);
		dc.addParam(id);
		dc.addParam(id);
		
		try {
			res = dc.loadObjectResults();
		} catch (Exception e) {
			Logger.debug(PublisherAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException("Unable find tree:" + e.getMessage(), e);
		} finally {
			DbConnectionFactory.closeConnection();
		}
		
		return res;
	}
	
	
	private static final String MULTI_TREE_QUERY = "select * from multi_tree where child = ?";
	/**
	 * Get multi tree data of a content
	 * @param indentifier
	 * @return
	 */
	public List<Map<String,Object>> getContentMultiTreeMatrix(String id) throws DotPublisherException {
		List<Map<String,Object>> res = null;
		DotConnect dc=new DotConnect();
		dc.setSQL(MULTI_TREE_QUERY);
		dc.addParam(id);
		
		try {
			res = dc.loadObjectResults();
		} catch (Exception e) {
			Logger.debug(PublisherAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException("Unable find multi tree:" + e.getMessage(), e);
		} finally {
			DbConnectionFactory.closeConnection();
		}
		
		return res;
	}
	
	private static final String PSGETENTRIESBYSTATUS = 
			"SELECT a.bundle_id, p.entered_date, p.asset, a.status, p.operation "+
			"FROM publishing_queue p, publishing_queue_audit a "+
			"where p.bundle_id = a.bundle_id "+
			"and a.status = ? ";
	private static final String MYGETENTRIESBYSTATUS = 
			"SELECT a.bundle_id, p.entered_date, p.asset, a.status, p.operation "+
			"FROM publishing_queue p, publishing_queue_audit a "+
			"where p.bundle_id = a.bundle_id "+
			"and a.status = ? ";
	private static final String MSGETENTRIESBYSTATUS = 
			"SELECT a.bundle_id, p.entered_date, p.asset, a.status, p.operation "+
			"FROM publishing_queue p, publishing_queue_audit a "+
			"where p.bundle_id = a.bundle_id "+
			"and a.status = ? ";
	private static final String OCLGETENTRIESBYSTATUS = 
			"SELECT a.bundle_id, p.entered_date, p.asset, a.status, p.operation "+
			"FROM publishing_queue p, publishing_queue_audit a "+
			"where p.bundle_id = a.bundle_id "+
			"and a.status = ? ";
	
	public List<Map<String,Object>> getQueueElementsByStatus(Status status) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSGETENTRIESBYSTATUS);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYGETENTRIESBYSTATUS);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSGETENTRIESBYSTATUS);
			}else{
				dc.setSQL(OCLGETENTRIESBYSTATUS);
			}
			
			dc.addParam(status.getCode());
			
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	private static final String PSGETENTRIES = 
			"SELECT * "+
			"FROM publishing_queue p order by bundle_id ";
	private static final String MYGETENTRIES = 
			"SELECT * "+
			"FROM publishing_queue p order by bundle_id ";
	private static final String MSGETENTRIES = 
			"SELECT * "+
			"FROM publishing_queue p order by bundle_id ";
	private static final String OCLGETENTRIES = 
			"SELECT * "+
			"FROM publishing_queue p order by bundle_id ";
	
	public List<Map<String,Object>> getQueueElements() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSGETENTRIES);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYGETENTRIES);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSGETENTRIES);
			}else{
				dc.setSQL(OCLGETENTRIES);
			}
			
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	private static final String PSCOUNTENTRIES="select count(*) as count from publishing_queue ";
	private static final String MYCOUNTENTRIES="select count(*) as count from publishing_queue ";
	private static final String MSCOUNTENTRIES="select count(*) as count from publishing_queue ";
	private static final String OCLCOUNTENTRIES="select count(*) as count from publishing_queue ";
	
	public List<Map<String,Object>> countQueueElements() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSCOUNTENTRIES);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYCOUNTENTRIES);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSCOUNTENTRIES);
			}else{
				dc.setSQL(OCLCOUNTENTRIES);
			}
			
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}

	private static final String PSGETENTRIESGROUPED=
			"SELECT a.bundle_id, p.entered_date, a.status, p.operation " +
			"FROM publishing_queue p, publishing_queue_audit a " +
			"where p.bundle_id = a.bundle_id group by bundle_id ";
	private static final String MYGETENTRIESGROUPED=
			"SELECT a.bundle_id, p.entered_date, a.status, p.operation " +
			"FROM publishing_queue p, publishing_queue_audit a " +
			"where p.bundle_id = a.bundle_id group by bundle_id ";
	private static final String MSGETENTRIESGROUPED=
			"SELECT a.bundle_id, p.entered_date, a.status, p.operation " +
			"FROM publishing_queue p, publishing_queue_audit a " +
			"where p.bundle_id = a.bundle_id group by bundle_id ";
	private static final String OCLGETENTRIESGROUPED=
			"SELECT a.bundle_id, p.entered_date, a.status, p.operation " +
			"FROM publishing_queue p, publishing_queue_audit a " +
			"where p.bundle_id = a.bundle_id group by bundle_id ";
	
	
	public List<Map<String,Object>> getQueueElementsGroupByBundleId() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSGETENTRIESGROUPED);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYGETENTRIESGROUPED);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSGETENTRIESGROUPED);
			}else{
				dc.setSQL(OCLGETENTRIESGROUPED);
			}
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	public List<Map<String,Object>> getQueueElementsGroupByBundleId(String offset, String limit) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSGETENTRIESGROUPED);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYGETENTRIESGROUPED);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSGETENTRIESGROUPED);
			}else{
				dc.setSQL(OCLGETENTRIESGROUPED);
			}
			
			dc.setStartRow(offset);
			dc.setMaxRows(limit);
			
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	
	private static final String PSGETBUNDLES="select distinct(bundle_id) as bundle_id from publishing_queue ";
	private static final String MYGETBUNDLES="select distinct(bundle_id) as bundle_id from publishing_queue ";
	private static final String MSGETBUNDLES="select distinct(bundle_id) as bundle_id from publishing_queue ";
	private static final String OCLGETBUNDLES="select distinct(bundle_id) as bundle_id from publishing_queue ";
	
	/**
	 * get bundle_ids available
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public List<Map<String,Object>> getQueueBundleIds() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSGETBUNDLES);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYGETBUNDLES);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSGETBUNDLES);
			}else{
				dc.setSQL(OCLGETBUNDLES);
			}
			
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	private static final String PSGETENTRIESBYBUNDLE= 
			"SELECT * "+
			"FROM publishing_queue p where bundle_id = ? order by asset ";
	private static final String MYGETENTRIESBYBUNDLE = 
			"SELECT * "+
			"FROM publishing_queue p where bundle_id = ? order by asset ";
	private static final String MSGETENTRIESBYBUNDLE = 
			"SELECT * "+
			"FROM publishing_queue p where bundle_id = ? order by asset ";
	private static final String OCLGETENTRIESBYBUNDLE = 
			"SELECT * "+
			"FROM publishing_queue p where bundle_id = ? order by asset ";
	
	/**
	 * get queue elements by bundle_id
	 * @return List<Map<String,Object>>
	 * @throws DotPublisherException
	 */
	public List<Map<String,Object>> getQueueElementsByBundleId(String bundleId) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSGETENTRIESBYBUNDLE);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYGETENTRIESBYBUNDLE);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSGETENTRIESBYBUNDLE);
			}else{
				dc.setSQL(OCLGETENTRIESBYBUNDLE);
			}
			
			dc.addParam(bundleId);
			
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	private static final String PSCOUNTENTRIESGROUPED="select count(distinct(bundle_id)) as count from publishing_queue ";
	private static final String MYCOUNTENTRIESGROUPED="select count(distinct(bundle_id)) as count from publishing_queue ";
	private static final String MSCOUNTENTRIESGROUPED="select count(distinct(bundle_id)) as count from publishing_queue ";
	private static final String OCLCOUNTENTRIESGROUPED="select count(distinct(bundle_id)) as count from publishing_queue ";
	
	public List<Map<String,Object>> countQueueElementsGroupByBundleId() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PSCOUNTENTRIESGROUPED);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYCOUNTENTRIESGROUPED);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSCOUNTENTRIESGROUPED);
			}else{
				dc.setSQL(OCLCOUNTENTRIESGROUPED);
			}
			
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}


	/**
	 * update element from publishing_queue table by id
	 */
	private static final String PSUPDATEELEMENTFROMQUEUESQL="UPDATE publishing_queue SET last_try=?, num_of_tries=?, in_error=?, last_results=? where id=?";
	private static final String MYUPDATEELEMENTFROMQUEUESQL="UPDATE publishing_queue SET last_try=?, num_of_tries=?, in_error=?, last_results=? where id=?";
	private static final String MSUPDATEELEMENTFROMQUEUESQL="UPDATE publishing_queue SET last_try=?, num_of_tries=?, in_error=?, last_results=? where id=?"; 
	private static final String OCLUPDATEELEMENTFROMQUEUESQL="UPDATE publishing_queue SET last_try=?, num_of_tries=?, in_error=?, last_results=? where id=?";
	/**
	 * update element from publishing_queue table by id
	 * @param id ID of the element in the publishing_queue
	 * @param next_try date of the next intent to execute the query
	 * @param in_error bolean indication if there was an error
	 * @param last_results error message
	 * @throws DotPublisherException
	 */
	public void updateElementStatusFromPublishQueueTable(long id, Date last_try,int num_of_tries, boolean in_error,String last_results ) throws DotPublisherException{
		try{
			HibernateUtil.startTransaction();
			DotConnect dc = new DotConnect();			
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				/*Validate if the table doesn't exist then is created*/				
				dc.setSQL(PSUPDATEELEMENTFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYUPDATEELEMENTFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSUPDATEELEMENTFROMQUEUESQL);
			}else{
				dc.setSQL(OCLUPDATEELEMENTFROMQUEUESQL);
			}
			dc.addParam(last_try);
			dc.addParam(num_of_tries);
			dc.addParam(in_error);
			dc.addParam(last_results);
			dc.addParam(id);
			dc.loadResult();
			HibernateUtil.commitTransaction();
		}catch(Exception e){
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.debug(PublisherAPIImpl.class,e.getMessage(),e1);
			}
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to update element "+id+" :"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}

	/**
	 * Delete element from publishing_queue table by id
	 */
	private static final String PSDELETEELEMENTFROMQUEUESQL="DELETE FROM publishing_queue where asset=?";
	private static final String MYDELETEELEMENTFROMQUEUESQL="DELETE FROM publishing_queue where asset=?";
	private static final String MSDELETEELEMENTFROMQUEUESQL="DELETE FROM publishing_queue where asset=?"; 
	private static final String OCLDELETEELEMENTFROMQUEUESQL="DELETE FROM publishing_queue where asset=?";
	/**
	 * Delete element from publishing_queue table by bundleId
	 * @param id ID of the element in the table
	 * @return boolean
	 * @throws DotPublisherException
	 */
	public void deleteElementFromPublishQueueTable(String identifier) throws DotPublisherException{
		try{
			HibernateUtil.startTransaction();
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				/*Validate if the table doesn't exist then is created*/				
				dc.setSQL(PSDELETEELEMENTFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYDELETEELEMENTFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSDELETEELEMENTFROMQUEUESQL);
			}else{
				dc.setSQL(OCLDELETEELEMENTFROMQUEUESQL);
			}
			dc.addParam(identifier);
			dc.loadResult();
			HibernateUtil.commitTransaction();
		}catch(Exception e){
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.debug(PublisherAPIImpl.class,e.getMessage(),e1);
			}
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to delete element "+identifier+" :"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	/**
	 * Delete element(s) from publishing_queue table by id
	 */
	private static final String PSDELETEELEMENTSFROMQUEUESQL="DELETE FROM publishing_queue where bundle_id=?";
	private static final String MYDELETEELEMENTSFROMQUEUESQL="DELETE FROM publishing_queue where bundle_id=?";
	private static final String MSDELETEELEMENTSFROMQUEUESQL="DELETE FROM publishing_queue where bundle_id=?"; 
	private static final String OCLDELETEELEMENTSFROMQUEUESQL="DELETE FROM publishing_queue where bundle_id=?";
	/**
	 * Delete element from publishing_queue table by bundleId
	 * @param id ID of the element in the table
	 * @return boolean
	 * @throws DotPublisherException
	 */
	public void deleteElementsFromPublishQueueTable(String bundleId) throws DotPublisherException{
		try{
			HibernateUtil.startTransaction();
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				/*Validate if the table doesn't exist then is created*/				
				dc.setSQL(PSDELETEELEMENTSFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYDELETEELEMENTSFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSDELETEELEMENTSFROMQUEUESQL);
			}else{
				dc.setSQL(OCLDELETEELEMENTSFROMQUEUESQL);
			}
			dc.addParam(bundleId);
			dc.loadResult();
			HibernateUtil.commitTransaction();
		}catch(Exception e){
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.debug(PublisherAPIImpl.class,e.getMessage(),e1);
			}
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to delete element(s) "+bundleId+" :"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}
	
	private static final String PSDELETEALLELEMENTFROMQUEUESQL="DELETE FROM publishing_queue";
	private static final String MYDELETEALLELEMENTFROMQUEUESQL="DELETE FROM publishing_queue";
	private static final String MSDELETEALLELEMENTFROMQUEUESQL="DELETE FROM publishing_queue"; 
	private static final String OCLDELETEALLELEMENTFROMQUEUESQL="DELETE FROM publishing_queue";
	/**
	 * Delete all elements from publishing_queue table
	 * @return boolean
	 */
	public void deleteAllElementsFromPublishQueueTable() throws DotPublisherException{
		try{
			HibernateUtil.startTransaction();
			DotConnect dc = new DotConnect();
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				/*Validate if the table doesn't exist then is created*/				
				dc.setSQL(PSDELETEALLELEMENTFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYDELETEALLELEMENTFROMQUEUESQL);
			}else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSDELETEALLELEMENTFROMQUEUESQL);
			}else{
				dc.setSQL(OCLDELETEALLELEMENTFROMQUEUESQL);
			}
			dc.loadResult();
			HibernateUtil.commitTransaction();
		}catch(Exception e){
			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.debug(PublisherAPIImpl.class,e.getMessage(),e1);
			}
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to delete elements :"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}

}
