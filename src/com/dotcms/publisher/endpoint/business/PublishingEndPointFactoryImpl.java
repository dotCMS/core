package com.dotcms.publisher.endpoint.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Logger;

public class PublishingEndPointFactoryImpl extends PublishingEndPointFactory {
	private PublishingEndPointCache cache = null;

	public PublishingEndPointFactoryImpl() {
		super();
		cache = CacheLocator.getPublishingEndPointCache();
	}
	
	public void ensureCacheIsLoaded() throws DotDataException{
		if(cache.isLoaded() == false) {
			getEndPoints();
		}
	}

	public List<PublishingEndPoint> getEndPoints() throws DotDataException {
		if(cache.isLoaded())
			return cache.getEndPoints();
		
		List<PublishingEndPoint> endPoints = new ArrayList<PublishingEndPoint>();
		DotConnect dc = new DotConnect();
		dc.setSQL(GET_END_POINTS);
		List<Map<String, Object>> res = dc.loadObjectResults();
		for(Map<String, Object> row : res){
			PublishingEndPoint endPoint = PublisherUtil.getObjectByMap(row);
			endPoints.add(endPoint);
			cache.add(endPoint);
		}
		cache.setLoaded(true);
		return endPoints;
	}
	
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

	public PublishingEndPoint getEndPointById(String id) throws DotDataException {
		ensureCacheIsLoaded();
		return cache.getEndPointById(id);
	}

	public void store(PublishingEndPoint anEndPoint) throws DotDataException {
		try{
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
		catch(DotDataException e) {
			Logger.debug(PublishingEndPointFactoryImpl.class, "Unexpected DotDataException in store method", e);
			throw e;
		}
	}

	public void update(PublishingEndPoint anEndPoint) throws DotDataException {
		try {
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
		catch(DotDataException e) {
			Logger.debug(PublishingEndPointFactoryImpl.class, "Unexpected DotDataException in update method", e);
			throw e;
		}
	}

	public void deleteEndPointById(String id) throws DotDataException {
		try {
			ensureCacheIsLoaded();
			DotConnect dc = new DotConnect();
			dc.setSQL(DELETE_END_POINT_BY_ID);
			dc.addParam(id);
			dc.loadResult();
			cache.removeEndPointById(id);
		}
		catch(DotDataException e) {
			Logger.debug(PublishingEndPointFactoryImpl.class, "Unexpected DotDataException in deleteEndPointById method", e);
			throw e;
		}	
	}

	public PublishingEndPoint getEnabledSendingEndPointByAddress(String address) throws DotDataException {
		ensureCacheIsLoaded();
		List<PublishingEndPoint> allEndPoints = getEndPoints();
		for(PublishingEndPoint endPoint : allEndPoints) {
			if(endPoint.getAddress().equals(address) && endPoint.isEnabled() && endPoint.isSending())
				return endPoint;
		}
		return null;
	}
	
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
}
