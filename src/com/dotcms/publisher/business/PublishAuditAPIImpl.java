package com.dotcms.publisher.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;


public class PublishAuditAPIImpl extends PublishAuditAPI {
	
	private static PublishAuditAPIImpl instance= null;
	public static PublishAuditAPIImpl getInstance() {
		if(instance==null)
			instance = new PublishAuditAPIImpl();
		
		return instance;
	}
	
	protected PublishAuditAPIImpl(){
		// Exists only to defeat instantiation.
	}
	
	private final String MANDATORY_FIELDS= 
			"bundle_id, "+
			"status, "+ 
			"status_pojo, "+
			"status_updated, "+
			"create_date ";
	
	private final String MANDATORY_PLACE_HOLDER = "?,?,?,?,?" ;

	private final String PGINSERTSQL="insert into publishing_queue_audit("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";
	private final String MYINSERTSQL="insert into publishing_queue_audit("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";
	private final String MSINSERTSQL="insert into publishing_queue_audit("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";
	private final String OCLINSERTSQL="insert into publishing_queue_audit("+MANDATORY_FIELDS+") values("+MANDATORY_PLACE_HOLDER+")";
	
	
	@Override
	public void insertPublishAuditStatus(PublishAuditStatus pa)
			throws DotPublisherException {
		if(getPublishAuditStatus(pa.getBundleId()) == null) {
			try{
				HibernateUtil.startTransaction();
				DotConnect dc = new DotConnect();
				
				if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
					dc.setSQL(PGINSERTSQL);
				} else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
					dc.setSQL(MYINSERTSQL);
				} else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
					dc.setSQL(MSINSERTSQL);
				} else {
					dc.setSQL(OCLINSERTSQL);
				}
				
				dc.addParam(pa.getBundleId());
				dc.addParam(pa.getStatus().getCode());
				dc.addParam("");//status_pojo TODO
				dc.addParam(new Date());
				dc.addParam(new Date());
				
				dc.loadResult();
				
				HibernateUtil.commitTransaction();			
			}catch(Exception e){
	
				try {
					HibernateUtil.rollbackTransaction();
				} catch (DotHibernateException e1) {
					Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e1);
				}			
				Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e);
				throw new DotPublisherException("Unable to add element to publish queue audit table:" + e.getMessage(), e);
			}
		}
	}
	
	private final String PGUPDATESQL="update publishing_queue_audit set status = ? where bundle_id = ? ";
	private final String MYUPDATESQL="update publishing_queue_audit set status = ? where bundle_id = ? ";
	private final String MSUPDATESQL="update publishing_queue_audit set status = ? where bundle_id = ? ";
	private final String OCLUPDATESQL="update publishing_queue_audit set status = ? where bundle_id = ? ";
	
	@Override
	public void updatePublishAuditStatus(String bundleId, Status newStatus)
			throws DotPublisherException {
		try{
			HibernateUtil.startTransaction();
			DotConnect dc = new DotConnect();
			
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PGUPDATESQL);
			} else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYUPDATESQL);
			} else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSUPDATESQL);
			} else {
				dc.setSQL(OCLUPDATESQL);
			}
			
			dc.addParam(newStatus.getCode());
			dc.addParam(bundleId);
			
			dc.loadResult();
			
			HibernateUtil.commitTransaction();			
		}catch(Exception e){

			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e1);
			}			
			Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException(
					"Unable to update element in publish queue audit table:" +
					"with the following bundle_id "+bundleId+" "+ e.getMessage(), e);
		}
	}
	
	private final String PGDELETESQL="delete from publishing_queue_audit where bundle_id = ? ";
	private final String MYDELETESQL="delete from publishing_queue_audit where bundle_id = ? ";
	private final String MSDELETESQL="delete from publishing_queue_audit where bundle_id = ? ";
	private final String OCLDELETESQL="delete from publishing_queue_audit where bundle_id = ? ";

	@Override
	public void deletePublishAuditStatus(String bundleId)
			throws DotPublisherException {
		try{
			HibernateUtil.startTransaction();
			DotConnect dc = new DotConnect();
			
			if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.POSTGRESQL)){
				dc.setSQL(PGDELETESQL);
			} else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MYSQL)){
				dc.setSQL(MYDELETESQL);
			} else if(DbConnectionFactory.getDBType().equals(DbConnectionFactory.MSSQL)){
				dc.setSQL(MSDELETESQL);
			} else {
				dc.setSQL(OCLDELETESQL);
			}
			
			dc.addParam(bundleId);
			
			dc.loadResult();
			
			HibernateUtil.commitTransaction();			
		}catch(Exception e){

			try {
				HibernateUtil.rollbackTransaction();
			} catch (DotHibernateException e1) {
				Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e1);
			}			
			Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException(
					"Unable to remove element in publish queue audit table:" +
					"with the following bundle_id "+bundleId+" "+ e.getMessage(), e);
		}
	}
	
	private final String SELECTSQL=
			"SELECT * "+
			"FROM publishing_queue_audit a where a.bundle_id = ? ";
	
	@Override
	public List<Map<String,Object>> getPublishAuditStatus(String bundleId)
			throws DotPublisherException {
		
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECTSQL);
			
			dc.addParam(bundleId);
			
			return dc.loadObjectResults();
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}finally{
			DbConnectionFactory.closeConnection();
		}
	}

	
}
