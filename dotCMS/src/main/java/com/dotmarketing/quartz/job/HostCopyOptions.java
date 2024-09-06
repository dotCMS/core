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
	private boolean copyHostVariables = true;
	private final boolean copyContentTypes;
	
	public HostCopyOptions(boolean copyAll) {
		this.copyAll = copyAll;
		this.copyTemplatesAndContainers = true;
		this.copyFolders = true;
		this.copyLinks = true;
		this.copyContentOnPages = true;
		this.copyContentOnHost = true;
		this.copyHostVariables = true;
		this.copyContentTypes = true;
	}

	public HostCopyOptions(boolean copyTemplatesAndContainers, boolean copyFolders,
						   boolean copyLinks, boolean copyContentOnPages, boolean copyContentOnHost, 
						   boolean copyHostVariables, final boolean copyContentTypes) {
		super();
		this.copyTemplatesAndContainers = copyTemplatesAndContainers;
		this.copyFolders = copyFolders;
		if(!copyFolders && copyLinks)
			throw new java.lang.IllegalArgumentException("Can't have a non copy folders while having copy links options on");
		this.copyLinks = copyLinks;
		this.copyContentOnPages = copyContentOnPages;
		this.copyContentOnHost = copyContentOnHost;
		this.copyHostVariables = copyHostVariables;
		this.copyContentTypes = copyContentTypes;
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

	public boolean isCopyHostVariables() {
		return copyHostVariables;
	}

	/**
	 * Indicates whether Content Types from the source Site must be copied over to the new Site or
	 * not.
	 *
	 * @return If Content Types must be copied, returns {@code true}.
	 */
	public boolean isCopyContentTypes() {
		return this.copyContentTypes;
	}

}	