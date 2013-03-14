package com.dotcms.publisher.business;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class PublishAuditHistory implements Serializable {
	private static final long serialVersionUID = 1L;
	
	//This map contains the enpoints group
	//each group can have one or more endpoints
	private Map<String, Map<String, EndpointDetail>> endpointsMap;
	
	private Date bundleStart;
	private Date bundleEnd;
	private Date publishStart;
	private Date publishEnd;
	private int numTries = 0;
	private Map<String, String> assets;
	
	public PublishAuditHistory() {
		assets = new HashMap<String, String>();
		endpointsMap = new HashMap<String, Map<String,EndpointDetail>>();
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
			groupMap = new HashMap<String, EndpointDetail>();
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
	
	public String getSerialized() {
		XStream xstream=new XStream(new DomDriver());
	       
        String xml=xstream.toXML(this);
        return xml;
	}
	
	public static PublishAuditHistory getObjectFromString(String serializedString) {
		PublishAuditHistory ret = null;
		XStream xstream=new XStream(new DomDriver());
		if(UtilMethods.isSet(serializedString)){
			try{
				return (PublishAuditHistory) xstream.fromXML(serializedString);
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


