package com.dotcms.publisher.pusher.wrapper;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.folders.model.Folder;

public class FolderWrapper {
	private Folder folder;
	private Identifier folderId;
	private Host host;
	private Identifier hostId;
	
	public FolderWrapper() {}

	public FolderWrapper(Folder folder, Identifier folderId, Host host, Identifier hostId) {
		this.folder = folder;
		this.folderId = folderId;
		this.host = host;
		this.hostId = hostId;
	}
	
	public Folder getFolder() {
		return folder;
	}
	public void setFolder(Folder folder) {
		this.folder = folder;
	}
	
	public Host getHost() {
		return host;
	}

	public void setHost(Host host) {
		this.host = host;
	}

	public Identifier getFolderId() {
		return folderId;
	}

	public void setFolderId(Identifier folderId) {
		this.folderId = folderId;
	}

	public Identifier getHostId() {
		return hostId;
	}

	public void setHostId(Identifier hostId) {
		this.hostId = hostId;
	}
}
