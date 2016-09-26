package com.dotmarketing.portlets.structure.model;

public class SimpleStructureURLMap {

	private final String inode;
	private final String URLMapPattern;
	
	public SimpleStructureURLMap(String inode,String URLMapPattern) {
		this.inode = inode;
		this.URLMapPattern = URLMapPattern;
	}
	

	public String getInode() {
		return inode;
	}

	public String getURLMapPattern() {
		return URLMapPattern;
	}
}
