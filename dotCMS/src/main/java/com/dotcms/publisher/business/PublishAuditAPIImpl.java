package com.dotcms.publisher.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.mapper.PublishAuditStatusMapper;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementation class for the {@link PublishAuditAPI}.
 *
 * @author Alberto
 * @version N/A
 * @since Oct 18, 2012
 *
 */
public class PublishAuditAPIImpl extends PublishAuditAPI {

	private static PublishAuditAPIImpl instance= null;
	private PublishAuditStatusMapper mapper = null;
	//This query select all bundles that are pending for a final status (final statuses FAILED_TO_PUBLISH or SUCCESS)
	private static final String SELECT_PENDING_BUNDLES = "SELECT * FROM publishing_queue_audit " +
			"WHERE status = ? or status = ? or status = ? or status = ? or status = ? or status = ?";
	private static final String INSERT_PUBLISHING_QUEUE_AUDIT ="insert into publishing_queue_audit(bundle_id, status, status_pojo, status_updated, create_date) values(?,?,?,?,?)";
	private static final String UPDATE_STATUS_STATUSPOJO_BY_BUNDLEID ="update publishing_queue_audit set status = ?, status_pojo = ?  where bundle_id = ? ";
	private static final String UPDATE_ALL_BY_BUNDLEID ="update publishing_queue_audit set status = ?, status_pojo = ?, create_date = ?, status_updated = ? where bundle_id = ? ";
	private static final String DELETE_BY_BUNDLEID ="delete from publishing_queue_audit where bundle_id = ? ";
	private static final String SELECT_ALL_BY_BUNDLEID = "select * from publishing_queue_audit where bundle_id = ? ";
	private static final String SELECT_ALL_ORDER_BY_STATUSUPDATED_DESC = "SELECT * FROM publishing_queue_audit order by status_updated desc";
	private static final String SELECT_MAX_CREATEDATE_BY_STATUS_ISNOT_BUNDLING = "select max(c.create_date) as max_date from publishing_queue_audit c where c.status != ? ";
	private static final String SELECT_COUNT = "SELECT count(*) as count FROM publishing_queue_audit ";
	private static final String SELECT_BUNDLEID_BY_STATUS = "SELECT bundle_id from publishing_queue_audit WHERE status = ? ";
	private static final String OR_STATUS_CLAUSE = " or publishing_queue_audit.status = ? ";
	private static final String SELECT_BUNDLEID_BY_STATUS_AND_OWNER = "SELECT bundle_id from publishing_queue_audit join publishing_bundle on publishing_queue_audit.bundle_id = publishing_bundle.id "
			+ "WHERE publishing_bundle.owner= ? and (publishing_queue_audit.status = ? ";


	/**
	 * Returns a singleton instance of the {@link PublishAuditAPI}.
	 *
	 * @return A unique instance of {@link PublishAuditAPI}.
	 */
	public static PublishAuditAPIImpl getInstance() {
		if (instance == null) {
			instance = new PublishAuditAPIImpl();
		}
		return instance;
	}

	/**
	 * Protected class constructor.
	 */
	protected PublishAuditAPIImpl(){
		// Exists only to defeat instantiation.
		mapper = new PublishAuditStatusMapper();
	}

	@Override
	public void insertPublishAuditStatus(PublishAuditStatus pa)
			throws DotPublisherException {
	    boolean localt=false;
		if(getPublishAuditStatus(pa.getBundleId()) == null) {
			try{
				localt=HibernateUtil.startLocalTransactionIfNeeded();
				DotConnect dc = new DotConnect();
				dc.setSQL(INSERT_PUBLISHING_QUEUE_AUDIT);
				dc.addParam(pa.getBundleId());
				dc.addParam(pa.getStatus().getCode());

				dc.addParam(pa.getStatusPojo().getSerialized());
				dc.addParam(new Date());
				dc.addParam(new Date());

				dc.loadResult();

				if(localt) {
				    HibernateUtil.closeAndCommitTransaction();
				}
			}catch(Exception e){
			    if(localt) {
    				try {
    					HibernateUtil.rollbackTransaction();
    				} catch (DotHibernateException e1) {
    					Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e1);
    				}
			    }
				Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e);
				throw new DotPublisherException("Unable to add element to publish queue audit table:" + e.getMessage(), e);
			} finally {
				if(localt) {
					HibernateUtil.closeSessionSilently();
				}
			}
		}
	}

	@WrapInTransaction
    @Override
    public void updatePublishAuditStatus(String bundleId, Status newStatus, PublishAuditHistory history ) throws DotPublisherException {
        updatePublishAuditStatus(bundleId, newStatus, history, false );
    }

	@WrapInTransaction
	@Override
	public void updatePublishAuditStatus(String bundleId, Status newStatus, PublishAuditHistory history, Boolean updateDates ) throws DotPublisherException {
	    boolean local=false;
		try{
			local = HibernateUtil.startLocalTransactionIfNeeded();
			DotConnect dc = new DotConnect();
            if ( updateDates ) {
                dc.setSQL(UPDATE_ALL_BY_BUNDLEID);
            } else {
                dc.setSQL(UPDATE_STATUS_STATUSPOJO_BY_BUNDLEID);
            }
            dc.addParam(newStatus.getCode());

			if(history != null) {
				dc.addParam(history.getSerialized());
			} else {
				dc.addParam("");
			}
            if ( updateDates ) {
                dc.addParam( new Date() );
                dc.addParam( new Date() );
            }

			dc.addParam(bundleId);

			dc.loadResult();

			if(local) {
			    HibernateUtil.closeAndCommitTransaction();
			}
		}catch(Exception e){
		    if(local) {
    			try {
    				HibernateUtil.rollbackTransaction();
    			} catch (DotHibernateException e1) {
    				Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e1);
    			}
		    }
			Logger.debug(PublishAuditAPIImpl.class,e.getMessage(),e);
			throw new DotPublisherException(
					"Unable to update element in publish queue audit table:" +
					"with the following bundle_id "+bundleId+" "+ e.getMessage(), e);
		} finally {
			if (local) {
				HibernateUtil.closeSessionSilently();
			}
		}
	}

	@WrapInTransaction
	@Override
	public List<String> deletePublishAuditStatus(final List<String> bundleIds) throws DotPublisherException {

		final ImmutableList.Builder<String> deletedBundleIds = new ImmutableList.Builder<>();

		for (final String bundleId: bundleIds) {

			this.deletePublishAuditStatus(bundleId);
			deletedBundleIds.add(bundleId);
		}

		return deletedBundleIds.build();
	}

	@WrapInTransaction
	@Override
	public void deletePublishAuditStatus(final String bundleId) throws DotPublisherException {

		try {

			Logger.info(this, "Deleting the bundle: " + bundleId);
			
			new DotConnect()
					.setSQL(DELETE_BY_BUNDLEID)
					.addParam(bundleId)
					.loadResult();
		} catch(Exception e) {

			Logger.error(PublishAuditAPIImpl.class, "Unable to remove element in publish queue audit table:" +
					"with the following bundle_id "+bundleId, e);
			throw new DotPublisherException(
					"Unable to remove element in publish queue audit table:" +
					"with the following bundle_id "+bundleId+" "+ e.getMessage(), e);
		}
	}

	@Override
	@CloseDBIfOpened
	public PublishAuditStatus getPublishAuditStatus(String bundleId)
			throws DotPublisherException {

		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECT_ALL_BY_BUNDLEID);

			dc.addParam(bundleId);

			List<Map<String, Object>> res = dc.loadObjectResults();
			if(res.size() > 1) {
				throw new DotPublisherException("Found duplicate bundle status");
			} else {
				if(!res.isEmpty()) {
					return mapper.mapObject(res.get(0));
				}
				return null;
			}
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	@Override
	@CloseDBIfOpened
	public List<PublishAuditStatus> getAllPublishAuditStatus() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECT_ALL_ORDER_BY_STATUSUPDATED_DESC);

			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	@Override
	public List<PublishAuditStatus> getAllPublishAuditStatus(Integer limit, Integer offset) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECT_ALL_ORDER_BY_STATUSUPDATED_DESC);

			dc.setStartRow(offset);
			dc.setMaxRows(limit);

			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	@Override
	public Date getLastPublishAuditStatusDate() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECT_MAX_CREATEDATE_BY_STATUS_ISNOT_BUNDLING);

			dc.addParam(Status.BUNDLING.getCode());

			List<Map<String, Object>> res = dc.loadObjectResults();

			if(!res.isEmpty()) {
				return (Date) res.get(0).get("max_date");
			}
			return null;

		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	@Override
	public Integer countAllPublishAuditStatus() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECT_COUNT);
			return Integer.parseInt(dc.loadObjectResults().get(0).get("count").toString());
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	@Override
	public List<PublishAuditStatus> getPendingPublishAuditStatus() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECT_PENDING_BUNDLES);
			dc.addParam(Status.BUNDLE_SENT_SUCCESSFULLY.getCode());
			dc.addParam(Status.FAILED_TO_SEND_TO_SOME_GROUPS.getCode());
			dc.addParam(Status.FAILED_TO_SEND_TO_ALL_GROUPS.getCode());
			dc.addParam(Status.RECEIVED_BUNDLE.getCode());
			dc.addParam(Status.PUBLISHING_BUNDLE.getCode());
			dc.addParam(Status.WAITING_FOR_PUBLISHING.getCode());
			return mapper.mapRows(dc.loadObjectResults());
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	@WrapInTransaction
	@Override
    public PublishAuditStatus updateAuditTable ( String endpointId, String groupId, String bundleFolder ) throws DotPublisherException {
        return updateAuditTable( endpointId, groupId, bundleFolder, false );
    }

	@WrapInTransaction
	@Override
    public PublishAuditStatus updateAuditTable ( String endpointId, String groupId, String bundleFolder, Boolean updateDates ) throws DotPublisherException {

		//Status
		PublishAuditStatus status =  new PublishAuditStatus(bundleFolder);
		//History
		PublishAuditHistory historyPojo = new PublishAuditHistory();
		EndpointDetail detail = new EndpointDetail();
		detail.setStatus(PublishAuditStatus.Status.RECEIVED_BUNDLE.getCode());
		detail.setInfo("Received bundle");

		historyPojo.addOrUpdateEndpoint(groupId, endpointId, detail);
		status.setStatus(PublishAuditStatus.Status.RECEIVED_BUNDLE);
		status.setStatusPojo(historyPojo);

		PublishAuditStatus existing=PublishAuditAPI.getInstance().getPublishAuditStatus(status.getBundleId());
		if(existing!=null) {
		    // update if there is an existing record.
            PublishAuditAPI.getInstance().updatePublishAuditStatus( status.getBundleId(), status.getStatus(), status.getStatusPojo(), updateDates );
        } else {
    		//Insert in Audit table
    		PublishAuditAPI.getInstance().insertPublishAuditStatus(status);
		}

		return status;
	}

    @Override
    public boolean isPublishRetry(final String bundleId) {
        boolean isRetry = false;

        try {
            if (UtilMethods.isSet(bundleId)) {

                final PublishAuditStatus auditStatus = getPublishAuditStatus(bundleId);

                if (auditStatus != null &&
                        auditStatus.getStatusPojo() != null &&
                        auditStatus.getStatusPojo().getNumTries() > 0) {

                    isRetry = true;
                }

            }
        } catch (DotPublisherException e) {
            Logger.debug(this, "Error trying to find out if the bundle is a retry.",
                    e);
        }

        return isRetry;
    }

    @CloseDBIfOpened
	@Override
	public List<String> getBundleIdByStatus(final List<Status> statusList, final int limit, final int offset) throws DotDataException {
		List<String> bundleIds = new ArrayList<>();
		final DotConnect dc = new DotConnect();
		String sql = SELECT_BUNDLEID_BY_STATUS;
		if(statusList.size() > 1){
			for(int i = 1;i<statusList.size();i++){
				sql += OR_STATUS_CLAUSE;
			}
		}
		dc.setSQL(sql);
		dc.setMaxRows(limit);
		dc.setStartRow(offset);
		for(final Status status : statusList){
			dc.addParam(status.getCode());
		}

		for(final Map<String,Object> resultMap : dc.loadObjectResults()) {
			bundleIds.add((String) resultMap.get("bundle_id"));
		}

		return bundleIds;
	}

	@CloseDBIfOpened
	@Override
	public List<String> getBundleIdByStatusFilterByOwner(final List<Status> statusList,
			final int limit, final int offset, final String userId) throws DotDataException {
		List<String> bundleIds = new ArrayList<>();
		final DotConnect dc = new DotConnect();
		String sql = SELECT_BUNDLEID_BY_STATUS_AND_OWNER;
		if(statusList.size() > 1){
			for(int i = 1;i<statusList.size();i++){
				sql += OR_STATUS_CLAUSE;
			}
		}
		sql += ")";
		dc.setSQL(sql);
		dc.setMaxRows(limit);
		dc.setStartRow(offset);
		dc.addParam(userId);
		for(final Status status : statusList){
			dc.addParam(status.getCode());
		}

		for(final Map<String,Object> resultMap : dc.loadObjectResults()) {
			bundleIds.add((String) resultMap.get("bundle_id"));
		}

		return bundleIds;
	}

}
