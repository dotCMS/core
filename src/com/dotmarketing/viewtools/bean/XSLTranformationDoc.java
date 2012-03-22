package com.dotmarketing.viewtools.bean;

/**
 * Object that manipulate XML with XSL transformation
 * @author Oswaldo
 *
 */
public class XSLTranformationDoc {

	//DOTCMS - 3800
	private String identifier;
	private String inode; 
	private String xmlTransformation;
	private long ttl;
	private String xmlPath;
	private String xslPath;

	/**
	 * Get the XSL File identifier of the object in cache
	 * @return long
	 */	
	public String getIdentifier() {
		return identifier;
	}
	/**
	 * Get the XSL File identifier of the object in cache
	 * @param identifier
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	/**
	 * Get the XSLPath of the object in cache
	 * @return String
	 */
	public String getXslPath() {
		return xslPath;
	}
	/**
	 * Set the XSLPath of the object in cache
	 * @param xslPath
	 */
	public void setXslPath(String xslPath) {
		this.xslPath = xslPath;
	}
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
	 * Get the XML transformed with the XSL
	 * @return String
	 */
	public String getXmlTransformation() {
		return xmlTransformation;
	}
	/**
	 * Set the XML transformed with the XSL
	 * @param xmlTransformation
	 */
	public void setXmlTransformation(String xmlTransformation) {
		this.xmlTransformation = xmlTransformation;
	}
	/**
	 * Get the inode of the XSL used in the transformation
	 * @return long
	 */
	public String getInode() {
		return inode;
	}
	/**
	 * Set the inode of the XSL used in the transformation
	 * @param inode
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}



}
