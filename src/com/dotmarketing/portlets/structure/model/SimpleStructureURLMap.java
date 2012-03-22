package com.dotmarketing.portlets.structure.model;

public class SimpleStructureURLMap {

	private String inode;
	private String URLMapPattern;
	
	public SimpleStructureURLMap(String inode,String URLMapPattern) {
		this.inode = inode;
		this.URLMapPattern = URLMapPattern;
	}
	
	public void setInode(String inode) {
		this.inode = inode;
	}
	public String getInode() {
		return inode;
	}
	public void setURLMapPattern(String uRLMapPattern) {
		URLMapPattern = uRLMapPattern;
	}
	public String getURLMapPattern() {
		return URLMapPattern;
	}
}
