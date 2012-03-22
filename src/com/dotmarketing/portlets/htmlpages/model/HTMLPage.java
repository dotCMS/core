package com.dotmarketing.portlets.htmlpages.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

/** @author Hibernate CodeGenerator */
public class HTMLPage extends WebAsset implements Serializable, Comparable {

    private static final long serialVersionUID = 1L;

    /** identifier field */
    private String parent;

    /** nullable persistent field */
    private String metadata;

    /** nullable persistent field */
    private java.util.Date startDate;

    /** nullable persistent field */
    private java.util.Date endDate;

    /** nullable persistent field */
    private String webStartDate;

    /** nullable persistent field */
    private String webEndDate;

    /** nullable persistent field */
    private String pageUrl;

    /** nullable persistent field */
    private boolean httpsRequired;

    /** nullable persistent field */
    private String redirect;

    private long cacheTTL;
    private String seoKeywords;
    private String seoDescription;
    
    private String templateId;
   
    public long getCacheTTL() {
		return cacheTTL;
	}

	public void setCacheTTL(long cacheTTL) {
		this.cacheTTL = cacheTTL;
	}

	public String getSeoKeywords() {
		return seoKeywords;
	}

	public void setSeoKeywords(String seoKeywords) {
		this.seoKeywords = seoKeywords;
	}

	public String getSeoDescription() {
		return seoDescription;
	}

	public void setSeoDescription(String seoDescription) {
		this.seoDescription = seoDescription;
	}

	/** default constructor */
    public HTMLPage() {
    	super.setType("htmlpage");	
    	startDate = new java.util.Date();
    	endDate = new java.util.Date();
    	metadata = com.dotmarketing.util.Config.getStringProperty("METADATA_DEFAULT")==null?"":com.dotmarketing.util.Config.getStringProperty("METADATA_DEFAULT");
    }

    public String getURI(Folder folder) throws DotStateException,DotDataException {
    	Identifier identifier = APILocator.getIdentifierAPI().find(folder);
    	if(APILocator.getFolderAPI().findSystemFolder().equals(folder))
    		return '/' + this.getPageUrl();
    	else {
    		String parentPath=identifier.getParentPath();
    		return parentPath + (parentPath.endsWith("/") ? "":"/") + 
    		   identifier.getAssetName() + '/' + this.getPageUrl();
    	}
    }

    public String getURI() throws DotStateException, DotDataException {
        Identifier id = APILocator.getIdentifierAPI().find(this);
        return id.getURI();
    }
    
	public void copy(HTMLPage currentHTMLPage) {
		this.metadata = currentHTMLPage.getMetadata();
		this.startDate = currentHTMLPage.getStartDate();
		this.endDate = currentHTMLPage.getEndDate();
		this.pageUrl = currentHTMLPage.getPageUrl();
		this.httpsRequired = currentHTMLPage.isHttpsRequired();
		this.redirect = currentHTMLPage.getRedirect();
		super.copy(currentHTMLPage);
	}

	/**
	 * Returns the endDate.
	 * @return java.util.Date
	 */
	public java.util.Date getEndDate() {
		return endDate;
	}

	/**
	 * Returns the httpsRequired.
	 * @return boolean
	 */
	public boolean isHttpsRequired() {
		return httpsRequired;
	}

	/**
	 * Returns the inode.
	 * @return String
	 */
	public String getInode() {
		if(InodeUtils.isSet(inode))
			return inode;
		
		return "";
	}

	/**
	 * Returns the metadata.
	 * @return String
	 */
	public String getMetadata() {
		return metadata;
	}

	/**
	 * Returns the pageUrl.
	 * @return String
	 */
	public String getPageUrl() {
		return pageUrl;
	}

	/**
	 * Returns the parent.
	 * @return String
	 */
	public String getParent() {
		return parent;
	}

	/**
	 * Returns the redirect.
	 * @return String
	 */
	public String getRedirect() {
		return redirect;
	}

	/**
	 * Returns the startDate.
	 * @return java.util.Date
	 */
	public java.util.Date getStartDate() {
		return startDate;
	}

	/**
	 * Sets the endDate.
	 * @param endDate The endDate to set
	 */
	public void setEndDate(java.util.Date endDate) {
		this.endDate = endDate;
	}

	/**
	 * Sets the httpsRequired.
	 * @param httpsRequired The httpsRequired to set
	 */
	public void setHttpsRequired(boolean httpsRequired) {
		this.httpsRequired = httpsRequired;
	}

	/**
	 * Sets the inode.
	 * @param inode The inode to set
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}

	/**
	 * Sets the metadata.
	 * @param metadata The metadata to set
	 */
	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

	/**
	 * Sets the pageUrl.
	 * @param pageUrl The pageUrl to set
	 */
	public void setPageUrl(String pageUrl) {
		this.pageUrl = pageUrl;
	}

	/**
	 * Sets the parent.
	 * @param parent The parent to set
	 */
	public void setParent(String parent) {
		this.parent = parent;
	}

	/**
	 * Sets the redirect.
	 * @param redirect The redirect to set
	 */
	public void setRedirect(String redirect) {
		this.redirect = redirect;
	}

	/**
	 * Sets the startDate.
	 * @param startDate The startDate to set
	 */
	public void setStartDate(java.util.Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * Returns the webEndDate.
	 * @return String
	 */
	public String getWebEndDate() {
		return webEndDate;
	}

	/**
	 * Returns the webStartDate.
	 * @return String
	 */
	public String getWebStartDate() {
		return webStartDate;
	}

	/**
	 * Sets the webEndDate.
	 * @param webEndDate The webEndDate to set
	 */
	public void setWebEndDate(String webEndDate) {
		this.webEndDate = webEndDate;
	}

	/**
	 * Sets the webStartDate.
	 * @param webStartDate The webStartDate to set
	 */
	public void setWebStartDate(String webStartDate) {
		this.webStartDate = webStartDate;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public int compareTo(Object compObject){

		if(!(compObject instanceof HTMLPage))return -1;
		
		HTMLPage htmlPage = (HTMLPage) compObject;
		return (htmlPage.getTitle().compareTo(this.getTitle()));

	}
	
    public Map<String, Object> getMap () throws DotStateException, DotDataException, DotSecurityException {
        Map<String, Object> map = super.getMap();
        map.put("parent", parent);
        map.put("metadata", metadata);
        map.put("startDate", startDate);
        map.put("endDate", endDate);
        map.put("webStartDate", webStartDate);
        map.put("webEndDate", webEndDate);
        map.put("pageUrl", pageUrl);
        map.put("httpsRequired", httpsRequired);
        map.put("redirect", redirect);
        return map;
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

	@Override
	public Permissionable getParentPermissionable() throws DotDataException {

		try {
			HTMLPageAPI pageAPI = APILocator.getHTMLPageAPI();
			HTMLPage base = this;
			if(!this.isWorking())
				base = pageAPI.loadWorkingPageById(this.getIdentifier(), APILocator.getUserAPI().getSystemUser(), false); 
					
			User systemUser = APILocator.getUserAPI().getSystemUser();
			
			Folder parentFolder=null;
			
			if(base!=null)
			parentFolder = pageAPI.getParentFolder(base);

			if (parentFolder != null && InodeUtils.isSet(parentFolder.getInode()))
				return parentFolder;
			
			Host host=null;
			
			if(base!=null)
			host = APILocator.getHostAPI().findParentHost(base, systemUser, false);

			if (host != null && InodeUtils.isSet(host.getInode()))
				return host;

			return APILocator.getHostAPI().findSystemHost(systemUser, false);
		} catch (DotSecurityException e) {
			Logger.error(HTMLPage.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

	}
	
}
