package com.dotmarketing.portlets.containers.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/** @author Hibernate CodeGenerator */
public class Container extends WebAsset implements Serializable {

	private static final long serialVersionUID = 1L;

	/** nullable persistent field */
	private String code;

	/** nullable persistent field */
	private int maxContentlets;

	/** nullable persistent field */
	private boolean useDiv;

	/** nullable persistent field */
	private String sortContentletsBy;

	private String preLoop;
	private String postLoop;
	private boolean staticify;

	private String luceneQuery;
	private String notes;

	private String structureInode;

	public String getURI(Folder folder) {
		String folderPath = "";
		try {
			folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
		} catch (Exception e) {
			Logger.error(this, e.getMessage());
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return folderPath + this.getInode();
	}

	/** default constructor */
	public Container() {
		super.setType("containers");
	}

	public String getInode() {
		if (InodeUtils.isSet(this.inode))
			return this.inode;

		return "";
	}

	/**
	 * Returns the code.
	 *
	 * @return String
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Sets the code.
	 *
	 * @param code
	 *            The code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Sets the inode.
	 *
	 * @param inode
	 *            The inode to set
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}

	// Every Web Asset should implement this method!!!
	public void copy(Container newContainer) {
		this.setCode(newContainer.getCode());
		this.setMaxContentlets(newContainer.getMaxContentlets());
		this.setPreLoop(newContainer.getPreLoop());
		this.setPostLoop(newContainer.getPostLoop());
		this.setLuceneQuery(newContainer.getLuceneQuery());
		this.setNotes(newContainer.getNotes());
		this.setStaticify(newContainer.getStaticify());
		this.setSortContentletsBy(newContainer.getSortContentletsBy());
		this.setUseDiv(newContainer.isUseDiv());
		super.copy(newContainer);
	}

	/**
	 * Returns the maxContentlets.
	 *
	 * @return int
	 */
	public int getMaxContentlets() {
		return maxContentlets;
	}

	/**
	 * Sets the maxContentlets.
	 *
	 * @param maxContentlets
	 *            The maxContentlets to set
	 */
	public void setMaxContentlets(int maxContentlets) {
		this.maxContentlets = maxContentlets;
	}

	/**
	 * Returns the sortContentletsBy.
	 *
	 * @return String
	 */
	public String getSortContentletsBy() {
		return sortContentletsBy;
	}

	/**
	 * Sets the sortContentletsBy.
	 *
	 * @param sortContentletsBy
	 *            The sortContentletsBy to set
	 */
	public void setSortContentletsBy(String sortContentletsBy) {
		this.sortContentletsBy = sortContentletsBy;
	}

	/**
	 * Returns the useDiv.
	 *
	 * @return boolean
	 */
	public boolean isUseDiv() {
		return useDiv;
	}

	/**
	 * Sets the useDiv.
	 *
	 * @param useDiv
	 *            The useDiv to set
	 */
	public void setUseDiv(boolean useDiv) {
		this.useDiv = useDiv;
	}

	public int compareTo(Container contObject) {

		Container container = (Container) contObject;
		return (container.getTitle().compareTo(this.getTitle()));

	}

	/**
	 * Returns the postLoop.
	 *
	 * @return String
	 */
	public String getPostLoop() {
		return postLoop;
	}

	/**
	 * Returns the preLoop.
	 *
	 * @return String
	 */
	public String getPreLoop() {
		return preLoop;
	}

	/**
	 * Sets the postLoop.
	 *
	 * @param postLoop
	 *            The postLoop to set
	 */
	public void setPostLoop(String postLoop) {
		this.postLoop = postLoop;
	}

	/**
	 * Sets the preLoop.
	 *
	 * @param preLoop
	 *            The preLoop to set
	 */
	public void setPreLoop(String preLoop) {
		this.preLoop = preLoop;
	}

	/**
	 * Returns the staticify.
	 *
	 * @return boolean
	 */
	public boolean isStaticify() {
		return staticify;
	}

	/**
	 * Returns the staticify.
	 *
	 * @return boolean
	 */
	public boolean getStaticify() {
		return staticify;
	}

	/**
	 * Sets the staticify.
	 *
	 * @param staticify
	 *            The staticify to set
	 */
	public void setStaticify(boolean staticify) {
		this.staticify = staticify;
	}

	public String getStructureInode() {
		return structureInode;
	}

	public void setStructureInode(String structureInode) {
		this.structureInode = structureInode;
	}

	public String getLuceneQuery() {
		return luceneQuery;
	}

	public void setLuceneQuery(String luceneQuery) {
		this.luceneQuery = luceneQuery;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public String getNotes() {
		if (UtilMethods.isSet(this.notes))
			return this.notes;
		return "";
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	@Override
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
		accepted.add(new PermissionSummary("view", "view-permission-description", PermissionAPI.PERMISSION_READ));
		accepted.add(new PermissionSummary("edit", "edit-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("publish", "publish-permission-description", PermissionAPI.PERMISSION_PUBLISH));
		accepted.add(new PermissionSummary("edit-permissions", "edit-permissions-permission-description", PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		return accepted;
	}

	public Permissionable getParentPermissionable() throws DotDataException {

		try {
			User systemUser = APILocator.getUserAPI().getSystemUser();
			HostAPI hostAPI = APILocator.getHostAPI();
			Host host = hostAPI.findParentHost(this, systemUser, false);

			if (host == null) {
				host = hostAPI.findSystemHost(systemUser, false);
			}
			return host;
		} catch (DotSecurityException e) {
			Logger.error(Container.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public boolean equals(Object o) {
		return ( o instanceof Container && UtilMethods.isSet(((Container) o).getInode())
				&& ((Container) o).getInode().equals(this.getInode()) );
	}

}
