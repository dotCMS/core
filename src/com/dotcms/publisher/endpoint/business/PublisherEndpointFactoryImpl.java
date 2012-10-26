package com.dotcms.publisher.endpoint.business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

}
