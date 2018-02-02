package com.dotmarketing.portlets.templates.design.bean;

import java.io.Serializable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PreviewFileAsset implements Serializable{
	
	private String inode;
	private String parent;
	private String realFileSystemPath;
	private boolean contentlet;
	
	public String getInode() {
		return inode;
	}
	
	public void setInode(String inode) {
		this.inode = inode;
	}
	
	public String getParent() {
		return parent;
	}
	
	public void setParent(String parent) {
		this.parent = parent;
	}
	
	public boolean isContentlet() {
		return contentlet;
	}
	
	public void setContentlet(boolean contentlet) {
		this.contentlet = contentlet;
	}

	public String getRealFileSystemPath() {
		return realFileSystemPath;
	}

	public void setRealFileSystemPath(String realFileSystemPath) {
		this.realFileSystemPath = realFileSystemPath;
	}	
    @Override
    public String toString() {
       try {
           return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }
	
}
