package com.dotmarketing.portlets.contentlet.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Jason Tesser
 * @author David Tores
 *
 */
public class ContentletAndBinary extends Contentlet{

	private static final long serialVersionUID = 1L;
	
	public List<Map<String,Object>> BinaryFilesList = new ArrayList<Map<String,Object>>();

	public List<Map<String, Object>> getBinaryFilesList() {
		return BinaryFilesList;
	}
	public void setBinaryFilesList(List<Map<String, Object>> binaryFilesList) {
		BinaryFilesList = binaryFilesList;
	}

	public void setMap(Map<String, Object> map) {
		this.map = map;
	}
	

}