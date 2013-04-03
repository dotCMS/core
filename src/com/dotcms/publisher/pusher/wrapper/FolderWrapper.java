package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.folders.model.Folder;

public class FolderWrapper {
	private Folder folder;
	private Identifier folderId;
	private Host host;
	private Identifier hostId;
	private Operation operation;
	public FolderWrapper() {}

	public FolderWrapper(Folder folder, Identifier folderId, Host host, Identifier hostId, Operation operation) {
		this.folder = folder;
		this.folderId = folderId;
		this.host = host;
		this.hostId = hostId;
		this.operation = operation;
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
	
	public Operation getOperation() {
		return operation;
	}
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
}
