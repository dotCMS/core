package com.dotcms.publisher.myTest;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.folders.model.Folder;

public class FolderWrapper {
	private Folder folder;
	private Identifier indentifier;
	
	public FolderWrapper(Folder folder, Identifier identifier) {
		this.folder = folder;
		this.indentifier = identifier;
	}
	
	public Folder getFolder() {
		return folder;
	}
	public void setFolder(Folder folder) {
		this.folder = folder;
	}
	public Identifier getIndentifier() {
		return indentifier;
	}
	public void setIndentifier(Identifier indentifier) {
		this.indentifier = indentifier;
	}
}
