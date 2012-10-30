package com.dotcms.publisher.business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class PublishAuditHistory implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Map<String, EndpointDetail> endpointsMap;
	private Date bundleStart;
	private Date bundleEnd;
	private Date publishStart;
	private Date publishEnd;
	private List<String> assets;
	
	public PublishAuditHistory() {
		assets = new ArrayList<String>();
		endpointsMap = new HashMap<String,EndpointDetail>();
	}
	
	
	public Map<String, EndpointDetail> getEndpointsMap() {
		return endpointsMap;
	}
	public void setEndpointsMap(Map<String, EndpointDetail> endpointsMap) {
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


	public List<String> getAssets() {
		return assets;
	}
	public void setAssets(List<String> assets) {
		this.assets = assets;
	}
	
	public void addOrUpdateEndpoint(String endpointId, EndpointDetail detail) {
		EndpointDetail temp = endpointsMap.get(endpointId);
		if(temp != null) {
			temp.setInfo(detail.getInfo());
			temp.setStatus(detail.getStatus());
		} else {
			endpointsMap.put(endpointId, detail);
		}
	}
	
	public String getSerialized() {
		XStream xstream=new XStream(new DomDriver());
	       
        String xml=xstream.toXML(this);
        return xml;
	}
	
	public static PublishAuditHistory getObjectFromString(String serializedString) {
		XStream xstream=new XStream(new DomDriver());
		return (PublishAuditHistory) xstream.fromXML(serializedString);
	}
}


