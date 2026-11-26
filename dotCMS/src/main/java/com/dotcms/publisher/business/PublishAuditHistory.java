package com.dotcms.publisher.business;

import com.dotcms.util.xstream.XStreamHandler;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.thoughtworks.xstream.XStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PublishAuditHistory implements Serializable {
	private static final long serialVersionUID = 1L;
	
	//This map contains the endpoints group
	//each group can have one or more endpoints
	private Map<String, Map<String, EndpointDetail>> endpointsMap;
	
	private Date bundleStart;
	private Date bundleEnd;
	private Date publishStart;
	private Date publishEnd;
	private int numTries = 0;
	private Map<String, String> assets;
	
	public PublishAuditHistory() {
		assets = new HashMap<>();
		endpointsMap = new HashMap<>();
	}
	
	
	public Map<String, Map<String, EndpointDetail>> getEndpointsMap() {
		return endpointsMap;
	}
	public void setEndpointsMap(Map<String, Map<String, EndpointDetail>> endpointsMap) {
		this.endpointsMap = endpointsMap;
	}
	
	
	public Date getBundleStart() {
		return bundleStart;
	}


	public void setBundleStart(Date bundleStart) {
		this.bundleStart = bundleStart;
	}


	public Date getBundleEnd() {
		return bundleEnd;
	}


	public void setBundleEnd(Date bundleEnd) {
		this.bundleEnd = bundleEnd;
	}


	public Date getPublishStart() {
		return publishStart;
	}


	public void setPublishStart(Date publishStart) {
		this.publishStart = publishStart;
	}


	public Date getPublishEnd() {
		return publishEnd;
	}


	public void setPublishEnd(Date publishEnd) {
		this.publishEnd = publishEnd;
	}


	public Map<String, String> getAssets() {
		return assets;
	}
	public void setAssets(Map<String, String> assets) {
		this.assets = assets;
	}
	
	public int getNumTries() {
		return numTries;
	}


	public void setNumTries(int numTries) {
		this.numTries = numTries;
	}
	
	public void addNumTries(){
		this.numTries++;
	}


	public void addOrUpdateEndpoint(String groupId, String endpointId, EndpointDetail detail) {
		Map<String, EndpointDetail> groupMap = endpointsMap.get(groupId);
		if(groupMap == null) {
			groupMap = new HashMap<>();
			groupMap.put(endpointId, detail);
			endpointsMap.put(groupId, groupMap);
		} else if(groupMap.get(endpointId) == null) {
			groupMap.put(endpointId, detail);
		} else {
			EndpointDetail temp = groupMap.get(endpointId);
			temp.setInfo(detail.getInfo());
			temp.setStatus(detail.getStatus());
		}
	}
	
	/**
	 * Tells whether this history already holds a result for the given endpoint.
	 *
	 * @param groupId    the id of the environment the endpoint belongs to
	 * @param endpointId the id of the endpoint
	 * @return true if the environment has an entry for the endpoint
	 */
	public boolean hasEndpoint(final String groupId, final String endpointId) {
		final Map<String, EndpointDetail> groupMap = endpointsMap.get(groupId);
		return groupMap != null && groupMap.containsKey(endpointId);
	}

	/**
	 * Tells whether this history holds a result for the endpoint that is not a failure. A publisher
	 * keeps such a result (for example one the queue job merged from a receiver) and replaces a
	 * failed one, so an endpoint that failed on an earlier attempt and works now does not stay failed.
	 * A failure is a failed publish, a bundle that could not be sent, a refused token or a missing
	 * license, the same results the queue job counts as failed.
	 *
	 * @param groupId    the id of the environment the endpoint belongs to
	 * @param endpointId the id of the endpoint
	 * @return true if the endpoint has an entry and that entry is not a failure
	 */
	public boolean hasEndpointWithoutFailure(final String groupId, final String endpointId) {
		final Map<String, EndpointDetail> groupMap = endpointsMap.get(groupId);
		final EndpointDetail detail = groupMap != null ? groupMap.get(endpointId) : null;
		return detail != null && !isFailed(detail);
	}

	/**
	 * Tells whether an endpoint result is a failure: a failed publish, a bundle that could not be
	 * sent, a refused token or a missing license.
	 *
	 * @param detail the result of one endpoint
	 * @return true if the result is one of those failures
	 */
	public static boolean isFailed(final EndpointDetail detail) {
		final int code = detail.getStatus();
		return code == PublishAuditStatus.Status.FAILED_TO_PUBLISH.getCode()
				|| code == PublishAuditStatus.Status.FAILED_TO_SENT.getCode()
				|| code == PublishAuditStatus.Status.INVALID_TOKEN.getCode()
				|| code == PublishAuditStatus.Status.LICENSE_REQUIRED.getCode();
	}

	static final XStreamHandler.Cached xstream = new XStreamHandler.Cached(null, null);
	public String getSerialized() {


        String xml=xstream.get().toXML(this);
        return xml;
	}
	
	public static PublishAuditHistory getObjectFromString(String serializedString) {
		PublishAuditHistory ret = null;
		if(UtilMethods.isSet(serializedString)){
			try{
				return (PublishAuditHistory) xstream.get().fromXML(serializedString);
			}
			catch(Exception e){
				Logger.error(PublishAuditHistory.class, e.getMessage(), e);
			}
		}else{
			Logger.debug(PublishAuditHistory.class, "Publishing Audit History Doesn't Exist");
		}
		return ret;
	}
}


