package com.dotcms.publisher.endpoint.business;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotcms.repackage.org.apache.commons.net.util.SubnetUtils;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

/**
 * Implementation class for the {@link PublishingEndPointFactory}.
 * 
 * @author Brent Griffin
 * @since Jan 29, 2013
 *
 */
public class PublishingEndPointFactoryImpl extends PublishingEndPointFactory {

	private PublishingEndPointCache cache = null;

	/**
	 * Default class constructor.
	 */
	public PublishingEndPointFactoryImpl() {
		super();
		cache = CacheLocator.getPublishingEndPointCache();
	}

	/**
	 * Makes sure that the Publishing End-point cache contains data before
	 * performing several data operations.
	 * 
	 * @throws DotDataException
	 *             An error occurred when populating the cache's data.
	 */
	public void ensureCacheIsLoaded() throws DotDataException{
		if(cache.isLoaded() == false) {
			getEndPoints();
		}
	}

	@Override
	public List<PublishingEndPoint> getEndPoints() throws DotDataException {
		if(cache.isLoaded()) {
			return cache.getEndPoints();
		}
		List<PublishingEndPoint> endPoints = new ArrayList<PublishingEndPoint>();
		DotConnect dc = new DotConnect();
		dc.setSQL(GET_END_POINTS);
		List<Map<String, Object>> res = dc.loadObjectResults();
		Map<String, PublishingEndPoint> endPointsMap = new HashMap<String, PublishingEndPoint>();
		for(Map<String, Object> row : res){
			PublishingEndPoint endPoint = PublisherUtil.getObjectByMap(row);
			endPoints.add(endPoint);
			endPointsMap.put(endPoint.getId(), endPoint);
		}
		cache.addAll(endPointsMap);
		cache.setLoaded(true);
		return endPoints;
	}

	@Override
	public List<PublishingEndPoint> getReceivingEndPoints() throws DotDataException {
		ensureCacheIsLoaded();
		List<PublishingEndPoint> endPoints = new ArrayList<PublishingEndPoint>();
		List<PublishingEndPoint> allEndPoints = getEndPoints();
		for(PublishingEndPoint endPoint : allEndPoints) {
			if(endPoint.isSending() == false)
				endPoints.add(endPoint);
		}
		return endPoints;
	}

	@Override
	public PublishingEndPoint getEndPointById(String id) throws DotDataException {
		return getEndPoints().stream().filter(endPoint -> endPoint.getId().equals(id)).findFirst().orElse(null);
	}

	@Override
	public void store(PublishingEndPoint anEndPoint) throws DotDataException {
		ensureCacheIsLoaded();
		anEndPoint.setId(UUID.randomUUID().toString());
		DotConnect dc = new DotConnect();
		dc.setSQL(SET_END_POINT);
		dc.addParam(anEndPoint.getId());
		dc.addParam(anEndPoint.getGroupId());
		dc.addParam(anEndPoint.getServerName().toString());
		dc.addParam(anEndPoint.getAddress());
		dc.addParam(anEndPoint.getPort());
		dc.addParam(anEndPoint.getProtocol());
		dc.addParam(anEndPoint.isEnabled());
		dc.addParam(anEndPoint.getAuthKey().toString());
		dc.addParam(anEndPoint.isSending());
		dc.loadResult();
		cache.clearCache(); // clear cache to make sure that all nodes in the cluster update
	}

	@Override
	public void update(PublishingEndPoint anEndPoint) throws DotDataException {
		ensureCacheIsLoaded();
		DotConnect dc = new DotConnect();
		dc.setSQL(UPDATE_END_POINT);
		dc.addParam(anEndPoint.getGroupId());
		dc.addParam(anEndPoint.getServerName().toString());
		dc.addParam(anEndPoint.getAddress());
		dc.addParam(anEndPoint.getPort());
		dc.addParam(anEndPoint.getProtocol());
		dc.addParam(anEndPoint.isEnabled());
		dc.addParam(anEndPoint.getAuthKey().toString());
		dc.addParam(anEndPoint.isSending());
		dc.addParam(anEndPoint.getId());
		dc.loadResult();
		cache.clearCache();		//clear cache to make sure all nodes in the cluster update
	}

	@Override
	public void deleteEndPointById(String id) throws DotDataException {
		ensureCacheIsLoaded();
		DotConnect dc = new DotConnect();
		dc.setSQL(DELETE_END_POINT_BY_ID);
		dc.addParam(id);
		dc.loadResult();
		cache.removeEndPointById(id);
	}

	@Override
	public PublishingEndPoint getEnabledSendingEndPointByAddress(String address) throws DotDataException {
		ensureCacheIsLoaded();
		List<PublishingEndPoint> allEndPoints = getEndPoints();
		String ipOrNetMask;
		boolean match;
		for(PublishingEndPoint endPoint : allEndPoints) {
			ipOrNetMask = endPoint.getAddress();
			if (ipOrNetMask.contains("/")) {
				match = new SubnetUtils(ipOrNetMask).getInfo().isInRange(address);
			} else {
				match = isMatchingEndpoint(ipOrNetMask, address);
			}
			if(match && endPoint.isEnabled() && endPoint.isSending())
				return endPoint;
		}
		return null;
	}

	/**
	 * 
	 * @param endPointAddress
	 * @param requestAddress
	 * @return
	 */
	boolean isMatchingEndpoint(String endPointAddress, String requestAddress) {
    	boolean result = false;
    	try {
    		InetAddress endPointInetAddress = getInetAddress(endPointAddress);
    		InetAddress requestInetAddress = getInetAddress(requestAddress);
    		result = endPointInetAddress.equals(requestInetAddress);
    	}
    	catch(UnknownHostException e) {
			Logger.error(this.getClass(),
					String.format("Unable to compare endpoints: endPointAddress=[%s], requestAddress=[%s]",
							endPointAddress, requestAddress),
					e);
    	}
    	return result;
	}

	/**
	 * 
	 * @param address
	 * @return
	 * @throws UnknownHostException
	 */
	private InetAddress getInetAddress(String address) throws UnknownHostException {
    	try {
    		return InetAddress.getByName(address);
    	}
    	catch(UnknownHostException e) {
			Logger.error(this.getClass(), String.format("Unable to resolve inetAddress for: [%s]", address));
    		throw e;
    	}
	}

	@Override
	public List<PublishingEndPoint> getSendingEndPointsByEnvironment(String environmentId) throws DotDataException {
		ensureCacheIsLoaded();
		List<PublishingEndPoint> endPoints = new ArrayList<PublishingEndPoint>();
		for(PublishingEndPoint endPoint : getEndPoints()) {
			if(endPoint.getGroupId().equals(environmentId) && endPoint.isSending()==false) {
				endPoints.add(endPoint);
			}
		}
		return endPoints;
	}

	@Override
	public List<PublishingEndPoint> getEnabledReceivingEndPoints() throws DotDataException {
		ensureCacheIsLoaded();
		List<PublishingEndPoint> receiverEndPoints = new ArrayList<PublishingEndPoint>();
		List<PublishingEndPoint> allEndPoints = getEndPoints();
		for(PublishingEndPoint endPoint : allEndPoints) {
			if(endPoint.isSending() == false && endPoint.isEnabled()) {
				receiverEndPoints.add(endPoint);
			}
		}
		return receiverEndPoints;
	}

	@Override
	public List<String> findSendGroups() throws DotDataException {
		ensureCacheIsLoaded();
		List<String> sendGroups = new ArrayList<String>();
		List<PublishingEndPoint> allEndPoints = getEndPoints();
		for(PublishingEndPoint endPoint : allEndPoints) {
			if(endPoint.getGroupId() != null && sendGroups.contains(endPoint.getGroupId()) == false) {
				sendGroups.add(endPoint.getGroupId());
			}
		}
		return sendGroups;
	}

	@Override
	public PublishingEndPoint getEndPointByName(String name) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(SELECT_END_POINT_BY_NAME);
		dc.addParam(name);
		List<Map<String, Object>> res = dc.loadObjectResults();
		PublishingEndPoint e = null;
		if(res!=null && !res.isEmpty()) {
			Map<String, Object> row = res.get(0);
			e = PublisherUtil.getObjectByMap(row);
		}
		return e;
	}

}
