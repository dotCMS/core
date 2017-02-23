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
	private boolean copyLinks = true;
	private boolean copyContentOnPages = true;
	private boolean copyContentOnHost = true;
	private boolean copyVirtualLinks = true;
	private boolean copyHostVariables = true;
	
	public HostCopyOptions(boolean copyAll) {
		this.copyAll = copyAll;
		this.copyTemplatesAndContainers = true;
		this.copyFolders = true;
		this.copyLinks = true;
		this.copyContentOnPages = true;
		this.copyContentOnHost = true;
		this.copyVirtualLinks = true;
		this.copyHostVariables = true;
	}

	public HostCopyOptions(boolean copyTemplatesAndContainers, boolean copyFolders,
						   boolean copyLinks, boolean copyContentOnPages, boolean copyContentOnHost, boolean copyVirtualLinks,
						   boolean copyHostVariables) {
		super();
		this.copyTemplatesAndContainers = copyTemplatesAndContainers;
		this.copyFolders = copyFolders;
		if(!copyFolders && copyLinks)
			throw new java.lang.IllegalArgumentException("Can't have a non copy folders while having copy links options on");
		this.copyLinks = copyLinks;
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

	public boolean isCopyLinks() {
		return copyLinks;
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