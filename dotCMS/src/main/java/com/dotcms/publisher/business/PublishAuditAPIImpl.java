package com.dotcms.publisher.business;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.business.WrapInTransaction;
import com.dotcms.publisher.business.PublishAuditStatus.Status;
import com.dotcms.publisher.mapper.PublishAuditStatusMapper;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;

/**
 * Implementation class for the {@link PublishAuditAPI}.
 *
 * @author Alberto
 * @version N/A
 * @since Oct 18, 2012
 *
 */
public class PublishAuditAPIImpl extends PublishAuditAPI {

	public static int NO_LIMIT_ASSETS = -1;

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
	private static final String SELECT_ALL_BY_BUNDLE_ID_QUERY = "SELECT * FROM publishing_queue_audit WHERE LOWER(bundle_id) LIKE ? ORDER BY status_updated DESC";
	private static final String SELECT_MAX_CREATEDATE_BY_STATUS_ISNOT_BUNDLING = "select max(c.create_date) as max_date from publishing_queue_audit c where c.status != ? ";
	private static final String SELECT_COUNT = "SELECT count(*) as count FROM publishing_queue_audit ";
	private static final String SELECT_COUNT_BY_BUNDLE_ID_QUERY = "SELECT COUNT(*) as count FROM publishing_queue_audit WHERE LOWER(bundle_id) LIKE ?";
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
		Logger.debug(PublishAuditAPIImpl.class,"Inserting audit table for bundle: " + pa.getBundleId());
		Logger.debug(PublishAuditAPIImpl.class,"Status: " + pa.getStatus().name());
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
		Logger.debug(PublishAuditAPIImpl.class,"Updating audit table for bundle: " + bundleId);
		Logger.debug(PublishAuditAPIImpl.class,"Status: " + newStatus.name());
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

		return getPublishAuditStatus(bundleId, NO_LIMIT_ASSETS);
	}

	public PublishAuditStatus getPublishAuditStatus(String bundleId, int assetsLimit) throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECT_ALL_BY_BUNDLEID);

			dc.addParam(bundleId);

			List<Map<String, Object>> res = dc.loadObjectResults();
			if(res.size() > 1) {
				throw new DotPublisherException("Found duplicate bundle status");
			} else {
				if(!res.isEmpty()) {
					final Map<String, Object> publishAuditStatusMap = res.get(0);
					final LimitedAssetResult limitedAssetResult = limitAssets(
							publishAuditStatusMap.get("status_pojo").toString(), assetsLimit);

					putStatusPojoAndNumberOfAssets(publishAuditStatusMap,
							limitedAssetResult.newStatusPojo, limitedAssetResult.numberTotalOfAssets);
					return mapper.mapObject(publishAuditStatusMap);
				}
				return null;
			}
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	private void putStatusPojoAndNumberOfAssets(
			final Map<String, Object> publishAuditStatusMap,
			final String newStatusPojo,
			final int numberTotalOfAssets) {
		publishAuditStatusMap.put("status_pojo", newStatusPojo);
		publishAuditStatusMap.put("total_number_of_assets", numberTotalOfAssets);
	}

	@Override
	@CloseDBIfOpened
	public List<PublishAuditStatus> getAllPublishAuditStatus() throws DotPublisherException {
		try{
			DotConnect dc = new DotConnect();
			dc.setSQL(SELECT_ALL_ORDER_BY_STATUSUPDATED_DESC);

			return mapper.mapRows(
				dc.loadObjectResults().stream()
						.map(publishAuditStatusMap -> {
							final LimitedAssetResult limitedAssetResult = limitAssets(
									publishAuditStatusMap.get("status_pojo").toString(), NO_LIMIT_ASSETS);
							putStatusPojoAndNumberOfAssets(publishAuditStatusMap,
									limitedAssetResult.newStatusPojo, limitedAssetResult.numberTotalOfAssets);
							return publishAuditStatusMap;
						})
						.collect(Collectors.toList())
			);
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	@Override
	public List<PublishAuditStatus> getAllPublishAuditStatus(Integer limit, Integer offset) throws DotPublisherException {
		return getAllPublishAuditStatus(limit, offset, NO_LIMIT_ASSETS);
	}

	/**
	 * Get all the {@link PublishAuditStatus} limiting the number of assets of each one.
	 *
	 * @param limit Max number of PublishAuditStatus to return
	 * @param offset
	 * @param limitAssets Max number of asset to return for each PublishAuditStatus, if the value is
	 *                       equals to -1 then all the assets are returned
	 * @return
	 * @throws DotPublisherException
	 */
	public List<PublishAuditStatus> getAllPublishAuditStatus(final int limit,
			final int offset, final int limitAssets) throws DotPublisherException {
		return getPublishAuditStatus(limit, offset, limitAssets, null);
	}

	/**
	 * Get the {@link PublishAuditStatus} that meet the given filter,
	 * limiting the number of assets of each one.
	 * @param limit limit of rows for retrieved page
	 * @param offset offset of rows for retrieved page
	 * @param limitAssets max limit of assets to retrieve for each {@link PublishAuditStatus}
	 * @param filter filter to apply to the query or null if no filter
	 * @return List of {@link PublishAuditStatus}
	 * @throws DotPublisherException if any error occurs
	 */
	@CloseDBIfOpened
	@Override
	public List<PublishAuditStatus> getPublishAuditStatus(
			final int limit, final int offset, final int limitAssets, final String filter)
			throws DotPublisherException {
		try{
			final DotConnect dc = getPublishAuditFilterQuery(filter,
					SELECT_ALL_BY_BUNDLE_ID_QUERY, SELECT_ALL_ORDER_BY_STATUSUPDATED_DESC);

			dc.setStartRow(offset);
			dc.setMaxRows(limit);

			return mapper.mapRows(
					dc.loadObjectResults().stream()
							.map(publishAuditStatusMap -> {
								final LimitedAssetResult limitedAssetResult = limitAssets(
										publishAuditStatusMap.get("status_pojo").toString(), limitAssets);
								putStatusPojoAndNumberOfAssets(publishAuditStatusMap,
										limitedAssetResult.newStatusPojo, limitedAssetResult.numberTotalOfAssets);
								return publishAuditStatusMap;
							})
							.collect(Collectors.toList())
			);
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	/**
	 * Limit the assets in <code>statusPojoAsString</code>.
	 *
	 * The <code>statusPojoAsString</code> string represent {@link PublishAuditStatus#getStatusPojo()}
	 * object as a xml format, it xml file can be use to get a {@link PublishAuditHistory}
	 * using the {@link PublishAuditHistory#getObjectFromString(String)} method.
	 *
	 * This xml has the follow format:
	 * <pre>
	 * <com.dotcms.publisher.business.PublishAuditHistory>
	 *   <endpointsMap/>
	 *   <numTries>0</numTries>
	 *   <assets>
	 *     <entry>
	 *       <string>efdd6642b609219ec94586f01fad90d7</string>
	 *       <string>CONTENTLET</string>
	 *     </entry>
	 *     <entry>
	 *       <string>3a991e4048f98a5420d1efa8cea7568b</string>
	 *       <string>CONTENT_TYPE</string>
	 *     </entry>
	 *     <entry>
	 *       <string>1c79263ca2c80c827c3c7d9daa0894f5</string>
	 *       <string>CONTENTLET</string>
	 *     </entry>
	 *     <entry>
	 *       <string>b05c0582d3301c3ef499975287b2f1b9</string>
	 *       <string>CONTENTLET</string>
	 *     </entry>
	 *     <entry>
	 *       <string>daf9d6e3aa8e1bc6d93bbd7df164d8e3</string>
	 *       <string>CONTENTLET</string>
	 *     </entry>
	 *   </assets>
	 * </com.dotcms.publisher.business.PublishAuditHistory>
	 * </pre>
	 *
	 * The <assets> element into the <code>statusPojoAsString</code> xml represents thee assets into the
	 * {@link PublishAuditHistory} object, each entry element is a assets into th bundle, the first
	 * string child is the Id and the second one is the type.
	 *
	 * the <assets> element in the <code>statusPojoAsString</code> xml file is change,
	 * according to the number of child into it:
	 *
	 * - If the number of entry is the same that the <code>limitAssets</code> parameter then the xml
	 * is not changed.
	 * - If the number of entry is lees than <code>limitAssets</code> parameter then the xml
	 * 	 is not changed.
	 * - If the number of entry is greater than <code>limitAssets</code> parameter then the
	 *   different between the total od entries and the <code>limitAssets</code> value is remove from the
	 *   assets element.
	 *
	 * @param statusPojoAsString
	 * @param limitAssets
	 * @return a {@link LimitedAssetResult} object with the new xml if it was changed and with the total
	 * number of entries
	 */
	private LimitedAssetResult limitAssets(final String statusPojoAsString, final int limitAssets) {

		final LimitedAssetResult limitedAssetResult = new LimitedAssetResult();

		final String assetsAsString = StringUtils
				.substringBetween(statusPojoAsString, "<assets>", "</assets>");
		final String[] entries = StringUtils
				.substringsBetween(assetsAsString, "<entry>", "</entry>");

		if (UtilMethods.isSet(entries) && limitAssets != NO_LIMIT_ASSETS) {
			final String entriesLimitedAsString = Arrays
					.stream(entries.length > limitAssets ?
							Arrays.copyOfRange(entries, 0, limitAssets) : entries)
					.map(arrayItem -> "<entry>" + arrayItem + "</entry>")
					.collect(Collectors.joining());

			final String statusPojoAsStringLimited = statusPojoAsString
					.replace(assetsAsString, entriesLimitedAsString);
			limitedAssetResult.newStatusPojo = statusPojoAsStringLimited;

		} else {
			limitedAssetResult.newStatusPojo = statusPojoAsString;
		}

		limitedAssetResult.numberTotalOfAssets = UtilMethods.isSet(entries) ? entries.length : 0;
		return limitedAssetResult;
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
		return countPublishAuditStatus(null);
	}

	/**
	 * Count filtered {@link PublishAuditStatus}
	 * @param filter filter to apply to the query or null if no filter
	 * @return number of rows that match the filter
	 * @throws DotPublisherException if any error occurs
	 */
	@CloseDBIfOpened
	@Override
	public Integer countPublishAuditStatus(String filter) throws DotPublisherException {
		try{
			final DotConnect dc = getPublishAuditFilterQuery(filter,
					SELECT_COUNT_BY_BUNDLE_ID_QUERY, SELECT_COUNT);

			return Integer.parseInt(dc.loadObjectResults().get(0).get("count").toString());
		}catch(Exception e){
			Logger.debug(PublisherUtil.class,e.getMessage(),e);
			throw new DotPublisherException("Unable to get list of elements with error:"+e.getMessage(), e);
		}
	}

	/**
	 * Get the query to filter the {@link PublishAuditStatus} by the given filter.
	 * @param filter filter to apply to the query or null if no filter
	 * @param selectWithFilterByBundleId query to filter by bundle id
	 * @param selectWithoutFilter 		query to retrieve all the elements
	 * @return the query to filter the {@link PublishAuditStatus} by the given filter
	 */
	private DotConnect getPublishAuditFilterQuery(final String filter,
			final String selectWithFilterByBundleId, final String selectWithoutFilter) {
		final String sanitizedFilter = SQLUtil.sanitizeParameter(filter);
		DotConnect dc = new DotConnect();
		if (UtilMethods.isSet(sanitizedFilter)) {
			dc.setSQL(selectWithFilterByBundleId);
			dc.addParam("%" + sanitizedFilter.toLowerCase() + "%");
		} else {
			dc.setSQL(selectWithoutFilter);
		}
		return dc;
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
		Logger.debug(PublishAuditAPIImpl.class,"Updating audit table for bundle: " + bundleFolder);
		Logger.debug(PublishAuditAPIImpl.class,"Status: " + status.getStatus().name());
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

	/**
	 * Result from the {@link PublishAuditAPIImpl#limitAssets(String, int)} method
	 */
	private static class LimitedAssetResult {
		String newStatusPojo;
		int numberTotalOfAssets;
	}
}
