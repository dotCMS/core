package com.dotmarketing.portlets.folders.struts;


import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.InodeUtils;
import com.liferay.portal.util.Constants;

public class FolderForm extends ValidatorForm {

	private static final long serialVersionUID = 1L;

	/** nullable persistent field */
    private String inode;
    
    /** nullable persistent field */
    private String name;

    /** nullable persistent field */
    private String path;

    /** nullable persistent field */
    private String title;
    
    /** nullable persistent field */
    private int sortOrder;

    /** nullable persistent field */
    private boolean showOnMenu;

    /** nullable persistent field */
    private String hostId;
    
    private String filesMasks;
    
    private String owner;
    
    private String defaultFileType;

    /** default constructor */
    public FolderForm() {
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
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the path.
	 * @return String
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Returns the sortOrder.
	 * @return int
	 */
	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * Sets the inode.
	 * @param inode The inode to set
	 */
	public void setInode(String inode) {
		this.inode = inode;
	}

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the path.
	 * @param path The path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Sets the sortOrder.
	 * @param sortOrder The sortOrder to set
	 */
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
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

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.ADD)) {
			return super.validate(mapping, request);
        }
        return null;
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
	 * @return Returns the hostId.
	 */
	public String getHostId() {
		return hostId;
	}
	/**
	 * @param hostId The hostId to set.
	 */
	public void setHostId(String hostId) {
		this.hostId = hostId;
	}

	public String getFilesMasks() {
		return filesMasks;
	}

	public void setFilesMasks(String filesMasks) {
		this.filesMasks = filesMasks;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
	
	public String getDefaultFileType() {
		return defaultFileType;
	}

	public void setDefaultFileType(String defaultFileType) {
		this.defaultFileType = defaultFileType;
	}

	
	
}
