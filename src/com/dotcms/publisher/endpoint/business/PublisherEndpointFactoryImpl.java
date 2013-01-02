package com.dotcms.publisher.endpoint.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.publisher.endpoint.bean.PublishingEndPoint;
import com.dotcms.publisher.util.PublisherUtil;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;

public class PublisherEndpointFactoryImpl extends PublisherEndpointFactory {

	public List<PublishingEndPoint> getEndpoints() throws DotDataException {
		List<PublishingEndPoint> endpoints = new ArrayList<PublishingEndPoint>();
		DotConnect dc = new DotConnect();
		dc.setSQL(GET_ENDPOINTS);
		List<Map<String, Object>> res = dc.loadObjectResults();
		for(Map<String, Object> row : res)
			endpoints.add(PublisherUtil.getObjectByMap(row));
		return endpoints;
	}
	
	public List<PublishingEndPoint> getReceivingEndpoints() throws DotDataException {
		List<PublishingEndPoint> endpoints = new ArrayList<PublishingEndPoint>();
		DotConnect dc = new DotConnect();
		dc.setSQL(GET_RECEIVING_ENDPOINTS);
		dc.addParam(false);
		List<Map<String, Object>> res = dc.loadObjectResults();
		for(Map<String, Object> row : res)
			endpoints.add(PublisherUtil.getObjectByMap(row));
		return endpoints;
	}

	public PublishingEndPoint getEndpointById(String id) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(GET_ENDPOINT_BY_ID);
		dc.addParam(id);
		List<Map<String, Object>> res = dc.loadObjectResults();
		if(res.size()>0){
			return PublisherUtil.getObjectByMap(res.get(0));
		}else
			return null;
	}

	public void store(PublishingEndPoint anEndpoint) throws DotDataException {
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
	}

	public void update(PublishingEndPoint anEndpoint) throws DotDataException {
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
	}

	public void deleteEndpointById(String id) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(DELETE_ENDPOINT_BY_ID);
		dc.addParam(id);
		dc.loadResult();
	}

	public PublishingEndPoint getSenderEndpointByAddress(String address) throws DotDataException {
		DotConnect dc = new DotConnect();
		dc.setSQL(GET_SENDER_ENDPOINT_BY_ADDRESS);
		dc.addParam(true);
		dc.addParam(address);
		dc.addParam(true);
		List<Map<String, Object>> res = dc.loadObjectResults();
		if(res.size()>0){
			return PublisherUtil.getObjectByMap(res.get(0));
		}else
			return null;
		}

	public List<PublishingEndPoint> getReceiverEndpoints() throws DotDataException {
		List<PublishingEndPoint> receiverEndpoints = new ArrayList<PublishingEndPoint>();
		DotConnect dc = new DotConnect();
		dc.setSQL(GET_RECEIVER_ENDPOINTS);
		dc.addParam(false);
		dc.addParam(true);
		List<Map<String, Object>> res = dc.loadObjectResults();
		for(Map<String, Object> singleEndpoint : res)
			receiverEndpoints.add(PublisherUtil.getObjectByMap(singleEndpoint));
		return receiverEndpoints;
	}

	@Override
	public List<String> findSendGroups() throws DotDataException {
		List<String> sendGroups = new ArrayList<String>();
		
		DotConnect dc = new DotConnect();
		dc.setSQL(SELECT_ENDPOINT_GROUPS);
		List<Map<String, Object>> res = dc.loadObjectResults();
		for(Map<String, Object> group : res){
			if(group.get("group_name") != null)
				sendGroups.add(group.get("group_name").toString());
			else
				sendGroups.add("");
		}
		return sendGroups;
	}
}
