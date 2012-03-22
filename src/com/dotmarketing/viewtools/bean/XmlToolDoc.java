package com.dotmarketing.viewtools.bean;

import com.dotmarketing.viewtools.XmlTool;



/**
 * Object that manipulate XmlTool in the Cache
 * @author Oswaldo Gallango
 * @version 1.0
 */
public class XmlToolDoc {

	private XmlTool xmlTool;
	private long ttl;
	private String xmlPath;
	
	/**
	 * Get the XMLPath of the object in cache
	 * @return String
	 */
	public String getXmlPath() {
		return xmlPath;
	}
	/**
	 * Set the XMLPath of the object in cache
	 * @param xmlPath
	 */
	public void setXmlPath(String xmlPath) {
		this.xmlPath = xmlPath;
	}
	/**
	 * Get the Time to Live of the object in cache
	 * @return long
	 */
	public long getTtl() {
		return ttl;
	}
	/**
	 * Set the Time to Live of the object in cache
	 * @param ttl
	 */
	public void setTtl(long ttl) {
		this.ttl = ttl;
	}
	/**
	 * Get the XmlTool of the object in cache
	 * @return xmlTool
	 */
	public XmlTool getXmlTool() {
		return xmlTool;
	}
	/**
	 * Set the XmlTool of the object in cache
	 * @param xmlTool
	 */
	public void setXmlTool(XmlTool xmlTool) {
		this.xmlTool = xmlTool;
	}
	
}
