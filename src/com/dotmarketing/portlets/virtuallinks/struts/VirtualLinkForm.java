package com.dotmarketing.portlets.virtuallinks.struts;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.org.apache.struts.action.ActionErrors;
import com.dotcms.repackage.org.apache.struts.action.ActionMapping;
import com.dotcms.repackage.org.apache.struts.action.ActionMessage;
import com.dotcms.repackage.org.apache.struts.validator.ValidatorForm;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.util.Constants;
import com.liferay.util.GetterUtil;
import com.liferay.util.SystemProperties;


/** @author Hibernate CodeGenerator */
public class VirtualLinkForm extends ValidatorForm {

	private final String VANITY_REGEXP_PATTERN = GetterUtil.getString( SystemProperties.get( "HTTPVanityURL.regexp.pattern" ) );
    
	/** identifier field */
    private String inode;

    /** nullable persistent field */
    private String title;

    /** nullable persistent field */
    private String url;

    /** nullable persistent field */
    private String uri;

    /** nullable persistent field */
    private boolean active;

    /** nullable persistent field */
    private String htmlInode;
   
    /** nullable persistent field */
    private String hostId;

    /** default constructor */
    public VirtualLinkForm() {
    	active = true;
    	htmlInode = "";
    }
    
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.ADD)) {
        
        	ActionErrors errors = super.validate(mapping, request);
        	
            if(!UtilMethods.isSet(url) || url.trim().equals("/") || !RegEX.contains( url, VANITY_REGEXP_PATTERN ) || url.length() > 2000 )
            	errors.add("url", new ActionMessage("message.virtuallink.invalid.URL"));
            
            return errors;
        }
        return null;
    }
    
	/**
	 * @return Returns the active.
	 */
	public boolean isActive() {
		return active;
	}
	/**
	 * @param active The active to set.
	 */
	public void setActive(boolean active) {
		this.active = active;
	}
	/**
	 * @return Returns the inode.
	 */
	public String getInode() {
		if(InodeUtils.isSet(inode))
			return inode;
		
		return "";
	}
	/**
	 * @param inode The inode to set.
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}
	/**
	 * @return Returns the uri.
	 */
	public String getUri() {
		return uri;
	}
	/**
	 * @param uri The uri to set.
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}
	/**
	 * @return Returns the url.
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * @param url The url to set.
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	
	/**
	 * @return Returns the htmlInode.
	 */
	public String getHtmlInode() {
		return htmlInode;
	}
	/**
	 * @param htmlInode The htmlInode to set.
	 */
	public void setHtmlInode(String htmlInode) {
		this.htmlInode = htmlInode;
	}
	/**
	 * @return Returns the title.
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title The title to set.
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	
    public String getHostId() {
        return hostId;
    }
    public void setHostId(String hostId) {
        this.hostId = hostId;
    }
}
