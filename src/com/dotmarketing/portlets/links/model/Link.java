
package com.dotmarketing.portlets.links.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.IdentifierFactory;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/** @author Hibernate CodeGenerator */
public class Link extends WebAsset implements Serializable, Comparable {

	public enum LinkType {
		INTERNAL,
		EXTERNAL,
		CODE
	}
	
    private static final long serialVersionUID = 1L;

    /** identifier field */
    private String parent = "";    
    
    private String protocal = "";

    private String url = "";

    private String target = "";
    
    private String internalLinkIdentifier = "";
    
    private String linkType = "";
    
    private String linkCode = "";
    
    private String hostId;

    /** default constructor */
    public Link() {
    	super.setType("links");	
    }

    public String getInode() {
    	if(InodeUtils.isSet(this.inode))
    		return this.inode;
    	
    	return "";
    }

	/**
	 * Returns the parent.
	 * @return String
	 */
	public String getParent() {
		return parent;
	}

	/**
	 * Sets the parent.
	 * @param parent The parent to set
	 */
	public void setParent(String parent) {
		this.parent = parent;
	}
	


	/**
	 * Sets the inode.
	 * @param inode The inode to set
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}

	//Every Web Asset should implement this method!!!
	public void copy(Link newLink) {
	    this.setParent(newLink.getParent());
		this.setTitle(newLink.getTitle());
		this.setUrl(newLink.getUrl());
		this.setTarget(newLink.getTarget());
		this.setType(newLink.getType());
		this.setProtocal(newLink.getProtocal());
		this.setInternalLinkIdentifier(newLink.getInternalLinkIdentifier());
	    super.copy(newLink);
	}

	/**
	 * Returns the url.
	 * @return String
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Sets the url.
	 * @param url The url to set
	 */
	public void setUrl(String url) {
		this.url = url;

	}
	
	/**
	 * Returns the target.
	 * @return String
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * Sets the target.
	 * @param target The target to set
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	public int compareTo(Object compObject){
		if(!(compObject instanceof Link))return -1;
		
		Link link = (Link) compObject;
		return (link.getTitle().compareTo(this.getTitle()));
	}


	/**
	 * Returns the protocal.
	 * @return String
	 */
	public String getProtocal() {
		return protocal;
	}

	/**
	 * Sets the protocal.
	 * @param protocal The protocal to set
	 */
	public void setProtocal(String protocal) {
		this.protocal = protocal;
	}

	/**
	 * Returns the type of link.
	 * @return boolean
	 */
	public String getLinkType() {
		return linkType;
	}

	/**
	 * Sets the type of link.
	 * @param type
	 */
	public void setLinkType(String type) {
		this.linkType = type;
	}
	
	public String getLinkCode() {
		return linkCode;
	}

	public void setLinkCode(String linkCode) {
		this.linkCode = linkCode;
	}

	/**
	 * @return
	 * @deprecated use link type property instead
	 */
	public boolean isInternal () {
		return linkType.equalsIgnoreCase(LinkType.INTERNAL.toString());
	}

	/**
	 * 
	 * @param internal
	 * @deprecated use link type property instead
	 */
	public void setInternal (boolean internal) {
		if(internal)
			linkType = LinkType.INTERNAL.toString();
		else if(!UtilMethods.isSet(linkType))
			linkType = LinkType.EXTERNAL.toString();
	}

	/**
	 * Returns the protocal.
	 * @return String
	 */
	public String getWorkingURL() {
		
		StringBuffer workingURL = new StringBuffer();
		if(this.url == null)
			this.url = "";
		if(this.protocal!=null && this.url.indexOf("http://")<0 && this.url.indexOf("https://")<0 && this.url.indexOf("mailto:")<0 && this.url.indexOf("ftp://")<0 && this.url.indexOf("javascript:")<0){
			workingURL.append(this.protocal);
		}
		if(this.url!=null){
			workingURL.append(this.url);
		}
		return workingURL.toString();
	}

 	public String getURI(Folder folder) {
       String uri ="";
       try {
		 uri = APILocator.getIdentifierAPI().find(folder).getPath() + this.getInode();
	   } catch (Exception e) {
		   Logger.error(Link.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
	   } 
		return uri;
	}
 	
 	public String getURI() throws DotStateException, DotDataException {
		Identifier id = APILocator.getIdentifierAPI().find(this);
		return id.getURI();
	}


	/**
	 * Returns the internalLinkIdentifier.
	 * @return String
	 */
	public String getInternalLinkIdentifier() {
		return internalLinkIdentifier;
	}

	/**
	 * Sets the internalLinkIdentifier.
	 * @param internalLinkIdentifier The internalLinkIdentifier to set
	 */
	public void setInternalLinkIdentifier(String internalLinkIdentifier) {
		this.internalLinkIdentifier = internalLinkIdentifier;
	}
	
    public String getHostId() {
		return hostId;
	}

	public void setHostId(String hostId) {
		this.hostId = hostId;
	}

	public Map<String, Object> getMap() throws DotStateException, DotDataException, DotSecurityException {
        Map<String, Object> map = super.getMap();

        map.put("parent", parent);
        map.put("title", getTitle());
        map.put("protocol", protocal);
        map.put("url", url);
        map.put("target", target);
        map.put("internalLinkIdentifier", internalLinkIdentifier);
        map.put("linkType", linkType.toString());
        
        return map;
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

	@Override
	public Permissionable getParentPermissionable() throws DotDataException {

		try {
			User systemUser = APILocator.getUserAPI().getSystemUser();

			Folder parentFolder = (Folder) APILocator.getFolderAPI().findParentFolder(this,systemUser,false);

			if (parentFolder != null && InodeUtils.isSet(parentFolder.getInode()))
				return parentFolder;

			Host host;
			host = APILocator.getHostAPI().findParentHost(this, systemUser, false);

			if (host != null && InodeUtils.isSet(host.getInode()))
				return host;

			return APILocator.getHostAPI().findSystemHost(systemUser, false);

		} catch (DotSecurityException e) {
			Logger.error(Link.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		
	}

	
}

