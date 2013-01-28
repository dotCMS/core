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

public class PublisherEndpointFactoryImpl extends PublisherEndpointFactory {
	private PublishingEndPointCache cache = null;

	public PublisherEndpointFactoryImpl() {
		super();
		cache = CacheLocator.getPublishingEndPointCache();
	}
	
	public void ensureCacheIsLoaded() throws DotDataException{
		if(cache.isLoaded() == false) {
			getEndpoints();
		}
	}

	public List<PublishingEndPoint> getEndpoints() throws DotDataException {
		if(cache.isLoaded())
			return cache.getEndPoints();
		
		List<PublishingEndPoint> endpoints = new ArrayList<PublishingEndPoint>();
		DotConnect dc = new DotConnect();
		dc.setSQL(GET_ENDPOINTS);
		List<Map<String, Object>> res = dc.loadObjectResults();
		for(Map<String, Object> row : res){
			PublishingEndPoint endPoint = PublisherUtil.getObjectByMap(row);
			endpoints.add(endPoint);
			cache.add(endPoint);
		}
		cache.setLoaded(true);
		return endpoints;
	}
	
	public List<PublishingEndPoint> getReceivingEndpoints() throws DotDataException {
		ensureCacheIsLoaded();
		List<PublishingEndPoint> endPoints = new ArrayList<PublishingEndPoint>();
		List<PublishingEndPoint> allEndPoints = getEndpoints();
		for(PublishingEndPoint endPoint : allEndPoints) {
			if(endPoint.isSending() == false)
				endPoints.add(endPoint);
		}
		return endPoints;		
	}

	public PublishingEndPoint getEndpointById(String id) throws DotDataException {
		ensureCacheIsLoaded();
		return cache.getEndPointById(id);
	}

	public void store(PublishingEndPoint anEndpoint) throws DotDataException {
		try{
			ensureCacheIsLoaded();
			anEndpoint.setId(UUID.randomUUID().toString());
			DotConnect dc = new DotConnect();
			dc.setSQL(SET_ENDPOINT);
			dc.addParam(anEndpoint.getId());
			dc.addParam(anEndpoint.getGroupId());
			dc.addParam(anEndpoint.getServerName().toString());
			dc.addParam(anEndpoint.getAddress());
			dc.addParam(anEndpoint.getPort());
			dc.addParam(anEndpoint.getProtocol());
			dc.addParam(anEndpoint.isEnabled());
			dc.addParam(anEndpoint.getAuthKey().toString());
			dc.addParam(anEndpoint.isSending());
			dc.loadResult();
			cache.add(anEndpoint);
		}
		catch(DotDataException e) {
			Logger.debug(PublisherEndpointFactoryImpl.class, "Unexpected DotDataException in store method", e);
			throw e;
		}
	}

	public void update(PublishingEndPoint anEndpoint) throws DotDataException {
		try {
			ensureCacheIsLoaded();
			DotConnect dc = new DotConnect();
			dc.setSQL(UPDATE_ENDPOINT);
			dc.addParam(anEndpoint.getGroupId());
			dc.addParam(anEndpoint.getServerName().toString());
			dc.addParam(anEndpoint.getAddress());
			dc.addParam(anEndpoint.getPort());
			dc.addParam(anEndpoint.getProtocol());
			dc.addParam(anEndpoint.isEnabled());
			dc.addParam(anEndpoint.getAuthKey().toString());
			dc.addParam(anEndpoint.isSending());
			dc.addParam(anEndpoint.getId());
			dc.loadResult();
			cache.removeEndPointById(anEndpoint.getId());
			cache.add(anEndpoint);
		}
		catch(DotDataException e) {
			Logger.debug(PublisherEndpointFactoryImpl.class, "Unexpected DotDataException in update method", e);
			throw e;
		}
	}

	public void deleteEndpointById(String id) throws DotDataException {
		try {
			ensureCacheIsLoaded();
			DotConnect dc = new DotConnect();
			dc.setSQL(DELETE_ENDPOINT_BY_ID);
			dc.addParam(id);
			dc.loadResult();
			cache.removeEndPointById(id);
		}
		catch(DotDataException e) {
			Logger.debug(PublisherEndpointFactoryImpl.class, "Unexpected DotDataException in deletEndpointById method", e);
			throw e;
		}	
	}

	public PublishingEndPoint getSenderEndpointByAddress(String address) throws DotDataException {
		ensureCacheIsLoaded();
		List<PublishingEndPoint> allEndPoints = getEndpoints();
		for(PublishingEndPoint endPoint : allEndPoints) {
			if(endPoint.getAddress().equals(address) && endPoint.isEnabled() && endPoint.isSending())
				return endPoint;
		}
		return null;
	}
	
	public List<PublishingEndPoint> getReceiverEndpoints() throws DotDataException {
		ensureCacheIsLoaded();
		List<PublishingEndPoint> receiverEndPoints = new ArrayList<PublishingEndPoint>();
		List<PublishingEndPoint> allEndPoints = getEndpoints();
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
		List<PublishingEndPoint> allEndPoints = getEndpoints();
		for(PublishingEndPoint endPoint : allEndPoints) {
			if(endPoint.getGroupId() != null && sendGroups.contains(endPoint.getGroupId()) == false) {
				sendGroups.add(endPoint.getGroupId());
			}
		}
		return sendGroups;
	}
}
