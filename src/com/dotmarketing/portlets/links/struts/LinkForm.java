package com.dotmarketing.portlets.links.struts;


import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.portlets.links.model.Link.LinkType;
import com.liferay.portal.util.Constants;

/** @author Hibernate CodeGenerator */
public class LinkForm extends ValidatorForm {

	private static final long serialVersionUID = 1L;

    private String parent;

    private String url;
    
    private static final java.util.ArrayList<String> protocals  = new java.util.ArrayList<String>();

    private String protocal;

    private String target;
    
	/*** WEB ASSET FIELDS FOR THE FORM ***/
    private String title;

    private String friendlyName;

    private boolean showOnMenu;

    private String linkType;
    
    private String linkCode;

    private int sortOrder;

    /*** WEB ASSET FIELDS FOR THE FORM ***/
    private String internalLinkIdentifier;

    private String owner;

    public LinkForm() {
    	protocals.add("http://");
    	protocals.add("https://");
    	protocals.add("mailto:");
    	protocals.add("ftp://");
    	protocals.add("javascript:");
    	linkType = LinkType.INTERNAL.toString();
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
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
	 * Returns the title.
	 * @return String
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 * @param title The title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Returns the showOnMenu.
	 * @return boolean
	 */
	public boolean isShowOnMenu() {
		return showOnMenu;
	}

	/**
	 * Sets the showOnMenu.
	 * @param showOnMenu The showOnMenu to set
	 */
	public void setShowOnMenu(boolean showOnMenu) {
		this.showOnMenu = showOnMenu;
	}

	/**
	 * Returns the sortOrder.
	 * @return int
	 */
	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * Sets the sortOrder.
	 * @param sortOrder The sortOrder to set
	 */
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.ADD)) {
            return super.validate(mapping, request);
        }
        return null;
    }

	/**
	 * Returns the friendlyName.
	 * @return String
	 */
	public String getFriendlyName() {
		return friendlyName;
	}

	/**
	 * Sets the friendlyName.
	 * @param friendlyName The friendlyName to set
	 */
	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}

	/**
	 * Returns the target.
	 * @return String
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * Returns the url.
	 * @return String
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Sets the target.
	 * @param target The target to set
	 */
	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * Sets the url.
	 * @param url The url to set
	 */
	public void setUrl(String url) {
		this.url = url;
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
	 * Returns the protocals.
	 * @return java.util.ArrayList
	 */
	public static java.util.ArrayList getProtocals() {
		return protocals;
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
	public void setLinkType(LinkType type) {
		this.linkType = type == null?LinkType.INTERNAL.toString():type.toString();
	}

	/**
	 * Sets the type of link.
	 * @param type
	 */
	public void setLinkType(String type) {
		this.linkType = type == null?LinkType.INTERNAL.toString():type;
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
		else
			linkType = LinkType.EXTERNAL.toString();
	}
	
 	public String getURI() {
		
		StringBuffer workingURI = new StringBuffer();
		if(this.protocal!=null){
			workingURI.append(this.protocal);
		}
		if(this.url!=null){
			workingURI.append(this.url);
		}
		if(this.target!=null){
			workingURI.append(this.target);
		}
		return workingURI.toString();
	}

	/**
	 * Returns the internallink.
	 * @return String
	 */
	public String getInternalLinkIdentifier() {
		return internalLinkIdentifier;
	}

	/**
	 * Sets the internallink.
	 * @param internallink The internallink to set
	 */
	public void setInternalLinkIdentifier(String internallink) {
		this.internalLinkIdentifier = internallink;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
	
}
