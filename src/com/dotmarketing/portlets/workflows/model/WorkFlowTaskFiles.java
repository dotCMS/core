package com.dotmarketing.portlets.workflows.model;

import java.io.Serializable;

public class WorkFlowTaskFiles  implements Serializable{
	
	String id;
	
	String workflowtaskId;
	
	String fileInode;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public String getWorkflowtaskId() {
		return workflowtaskId;
	}
	public void setWorkflowtaskId(String workflowtaskId) {
		this.workflowtaskId = workflowtaskId;
	}
	public String getFileInode() {
		return fileInode;
	}
	public void setFileInode(String fileInode) {
		this.fileInode = fileInode;
	}
	
}
