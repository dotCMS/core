/**
 * 
 */
package com.dotmarketing.webdav;

import java.util.Date;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.files.model.File;

public class Summary {
	public Summary () {
		setCreateDate(new Date());
		setModifyDate(new Date());
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	public Date getCreateDate() {
		return createDate;
	}
	public void setFolder(boolean isFolder) {
		this.isFolder = isFolder;
	}
	public boolean isFolder() {
		return isFolder;
	}
	public void setModifyDate(Date modifyDate) {
		this.modifyDate = modifyDate;
	}
	public Date getModifyDate() {
		return modifyDate;
	}
	public void setLength(long length) {
		this.length = length;
	}
	public long getLength() {
		return length;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getPath() {
		return path;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setHost(Host host) {
		this.host = host;
	}
	public Host getHost() {
		return host;
	}
	public void setFile(IFileAsset file) {
		this.file = file;
	}
	public IFileAsset getFile() {
		return file;
	}
	private Date createDate;
	private boolean isFolder;
	private Date modifyDate;
	private long length;
	private String path;
	private String name;
	private Host host;
	private IFileAsset file;
	
}