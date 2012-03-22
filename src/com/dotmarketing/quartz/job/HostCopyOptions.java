package com.dotmarketing.quartz.job;

import java.io.Serializable;

/**
 * Class used to specify the copy options when copying assets from one host to other
 * @author David H Torres
 *
 */
public class HostCopyOptions implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private boolean copyAll = true;
	private boolean copyTemplatesAndContainers = true;
	private boolean copyFolders = true;
	private boolean copyFiles = true;
	private boolean copyPages = true;
	private boolean copyContentOnPages = true;
	private boolean copyContentOnHost = true;
	private boolean copyVirtualLinks = true;
	private boolean copyHostVariables = true;
	
	public HostCopyOptions(boolean copyAll) {
		this.copyAll = copyAll;
		this.copyTemplatesAndContainers = true;
		this.copyFolders = true;
		this.copyFiles = true;
		this.copyPages = true;
		this.copyContentOnPages = true;
		this.copyContentOnHost = true;
		this.copyVirtualLinks = true;
		this.copyHostVariables = true;
	}

	public HostCopyOptions(boolean copyTemplatesAndContainers, boolean copyFolders, boolean copyFiles,
			boolean copyPages, boolean copyContentOnPages, boolean copyContentOnHost, boolean copyVirtualLinks,
			boolean copyHostVariables) {
		super();
		this.copyTemplatesAndContainers = copyTemplatesAndContainers;
		this.copyFolders = copyFolders;
		if(!copyFolders && (copyFiles || copyPages))
			throw new java.lang.IllegalArgumentException("Can't have a non copy folders while having a copy files or copy pages options on");
		this.copyFiles = copyFiles;
		this.copyPages = copyPages;
		this.copyContentOnPages = copyContentOnPages;
		this.copyContentOnHost = copyContentOnHost;
		this.copyVirtualLinks = copyVirtualLinks;
		this.copyHostVariables = copyHostVariables;
		this.copyAll = false;
	}

	public boolean isCopyAll() {
		return copyAll;
	}

	public boolean isCopyTemplatesAndContainers() {
		return copyTemplatesAndContainers;
	}

	public boolean isCopyFolders() {
		return copyFolders;
	}

	public boolean isCopyFiles() {
		return copyFiles;
	}

	public boolean isCopyPages() {
		return copyPages;
	}

	public boolean isCopyContentOnPages() {
		return copyContentOnPages;
	}

	public boolean isCopyContentOnHost() {
		return copyContentOnHost;
	}

	public boolean isCopyVirtualLinks() {
		return copyVirtualLinks;
	}

	public boolean isCopyHostVariables() {
		return copyHostVariables;
	}
}	