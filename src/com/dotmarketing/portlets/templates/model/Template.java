package com.dotmarketing.portlets.templates.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
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
import com.liferay.portal.model.User;

/** @author Hibernate CodeGenerator */
public class Template extends WebAsset implements Serializable, Comparable {

	private static final long serialVersionUID = 1L;

	/** nullable persistent field */
	private String body;

    /** nullable persistent field */
	private String selectedimage;
    /** nullable persistent field */
	private String image;
	
	//	*********************** BEGIN GRAZIANO issue-12-dnd-template
	private Boolean drawed;
	
	private String drawedBody;
	
	private Integer countAddContainer;
	
	private Integer countContainers;
	
	private String headCode;
	//	*********************** END GRAZIANO issue-12-dnd-template

	/** default constructor */
	public Template() {
		this.image = "";
		super.setType("template");
	}

    public String getURI(Folder folder) {
    	String folderPath = "";
		try {
			folderPath = APILocator.getIdentifierAPI().find(folder).getPath();
		} catch (Exception e) {
			Logger.error(this,e.getMessage());
			throw new DotRuntimeException(e.getMessage(),e);
		} 
    	return folderPath + this.getInode();
    }

	/**
	 * @return Returns the image.
	 */
	public String getImage() {
		return image;
	}
	/**
	 * @param image The image to set.
	 */
	public void setImage(String image) {
		this.image = image;
	}
	/**
	 * @return Returns the selectedimage.
	 */
	public String getSelectedimage() {
		return selectedimage;
	}
	/**
	 * @param selectedimage The selectedimage to set.
	 */
	public void setSelectedimage(String selectedimage) {
		this.selectedimage = selectedimage;
	}
	/** nullable persistent field */
	private String header;

	/** nullable persistent field */
	private String footer;

	public String getInode() {
		if(InodeUtils.isSet(this.inode))
			return this.inode;
			
		return "";
	}

	/**
	 * Sets the inode.
	 * @param inode The inode to set
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}

	/**
	 * Returns the body.
	 * @return String
	 */
	public String getBody() {
		return body;
	}

	/**
	 * Sets the body.
	 * @param body The body to set
	 */
	public void setBody(String body) {
		this.body = body;
	}

	public void copy(Template currentTemplate) {
		this.body = currentTemplate.getBody();
		this.header = currentTemplate.getHeader();
		this.footer = currentTemplate.getFooter();
		super.copy(currentTemplate);
	}

	/**
	 * Returns the footer.
	 * @return String
	 */
	public String getFooter() {
		return footer;
	}

	/**
	 * Returns the header.
	 * @return String
	 */
	public String getHeader() {
		return header;
	}

	/**
	 * Sets the footer.
	 * @param footer The footer to set
	 */
	public void setFooter(String footer) {
		this.footer = footer;
	}

	/**
	 * Sets the header.
	 * @param header The header to set
	 */
	public void setHeader(String header) {
		this.header = header;
	}
	
	/**
	 * Identify the drawed template
	 * @return
	 */
	public Boolean isDrawed() {
		return drawed;
	}

	/**
	 * Sets the boolean for drawed template 
	 * @param drawed
	 */
	public void setDrawed(Boolean drawed) {
		if(null!=drawed)
			this.drawed = drawed;
		else
			this.drawed = false;
	}
		
	public String getDrawedBody() {
		return drawedBody;
	}

	public void setDrawedBody(String drawedBody) {
		this.drawedBody = drawedBody;
	}

	public Integer getCountAddContainer() {
		return countAddContainer;
	}

	public void setCountAddContainer(Integer countAddContainer) {
		this.countAddContainer = countAddContainer;
	}

	public Integer getCountContainers() {
		return countContainers;
	}

	public void setCountContainers(Integer countContainers) {
		this.countContainers = countContainers;
	}

	public String getHeadCode() {
		return headCode;
	}

	public void setHeadCode(String headCode) {
		this.headCode = headCode;
	}

	public int compareTo(Object compObject){

		if(!(compObject instanceof Template))return -1;
		
		Template template = (Template) compObject;
		return (template.getTitle().compareTo(this.getTitle()));

	}

    /**
     * @author David H Torres
     */
	@Override
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<PermissionSummary>();
		accepted.add(new PermissionSummary("view",
				"view-permission-description", PermissionAPI.PERMISSION_READ));
		accepted.add(new PermissionSummary("edit",
				"edit-permission-description", PermissionAPI.PERMISSION_WRITE));
		accepted.add(new PermissionSummary("publish",
				"publish-permission-description",
				PermissionAPI.PERMISSION_PUBLISH));
		accepted.add(new PermissionSummary("edit-permissions",
				"edit-permissions-permission-description",
				PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
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
			throw new DotRuntimeException(e.getMessage(), e);
		}
	}

}
