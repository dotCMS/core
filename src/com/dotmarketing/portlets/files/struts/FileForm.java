package com.dotmarketing.portlets.files.struts;


import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;

/** @author Hibernate CodeGenerator */
public class FileForm extends ValidatorForm {

    /** identifier field */
    //private long inode;

    private static final long serialVersionUID = 1L;

	/** identifier field */
    private String parent;

    /** nullable persistent field */
    private String fileName;

	/** nullable persistent field */
	private String _EXT_3_fileName;
	
	private String _imageToolSaveFile;
	
    public String get_imageToolSaveFile() {
		return _imageToolSaveFile;
	}

	public void set_imageToolSaveFile(String _imageToolSaveFile) {
		this._imageToolSaveFile = _imageToolSaveFile;
	}

	/** nullable persistent field */
    private int size;

    /** nullable persistent field */
    private String mimeType;

    /** nullable persistent field */
    private String author;

    /** nullable persistent field */
    private java.util.Date publishDate;

    /** nullable persistent field */
    private String webPublishDate;

	/*** WEB ASSET FIELDS FOR THE FORM ***/
    /** nullable persistent field */
    private String title;

    /** nullable persistent field */
    private String friendlyName;

    /** nullable persistent field */
    private boolean showOnMenu;

    /** nullable persistent field */
    private int sortOrder;
	/*** WEB ASSET FIELDS FOR THE FORM ***/

    /** nullable persistent field */
    private String selectedparent;

    /** nullable persistent field */
    private String selectedparentPath;

    /** nullable persistent field */
	private int maxSize;
    /** nullable persistent field */
	private int maxHeight;
    /** nullable persistent field */
	private int maxWidth;
    /** nullable persistent field */
	private int minHeight;
	
    /** nullable persistent field */
    private String[] categories;
    
    private String owner;  // dotcms 472
    

	
    /** default constructor */
    public FileForm() {
    }

    public java.lang.String getFileName() {
        return this.fileName;
    }

    public void setFileName(java.lang.String fileName) {
    	if (UtilMethods.isSet(fileName)) {
    		if (fileName.contains("/"))
    			fileName = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length());
   		 	if (fileName.contains("\\")) 
    		 	fileName = fileName.substring(fileName.lastIndexOf("\\") + 1, fileName.length());
    		fileName = fileName.replaceAll("'","");   		
    	}
        this.fileName = fileName;
    }
    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
    }
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

	/**
	 * Returns the maxHeight.
	 * @return int
	 */
	public int getMaxHeight() {
		return maxHeight;
	}

	/**
	 * Returns the maxSize.
	 * @return int
	 */
	public int getMaxSize() {
		return maxSize;
	}

	/**
	 * Returns the maxWidth.
	 * @return int
	 */
	public int getMaxWidth() {
		return maxWidth;
	}

	/**
	 * Returns the minHeight.
	 * @return int
	 */
	public int getMinHeight() {
		return minHeight;
	}

	/**
	 * Sets the maxHeight.
	 * @param maxHeight The maxHeight to set
	 */
	public void setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
	}

	/**
	 * Sets the maxSize.
	 * @param maxSize The maxSize to set
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * Sets the maxWidth.
	 * @param maxWidth The maxWidth to set
	 */
	public void setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
	}

	/**
	 * Sets the minHeight.
	 * @param minHeight The minHeight to set
	 */
	public void setMinHeight(int minHeight) {
		this.minHeight = minHeight;
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
	 * Returns the mimeType.
	 * @return String
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Sets the mimeType.
	 * @param mimeType The mimeType to set
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
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
		ActionErrors ae = new ActionErrors();
		try {
			HostAPI hostAPI = APILocator.getHostAPI();
			User systemUser = APILocator.getUserAPI().getSystemUser();
			if (request.getParameter("cmd") != null && request.getParameter("cmd").equals(Constants.ADD)) {
				String inode = request.getParameter("parent");
				Folder parentFolder = APILocator.getFolderAPI().find(inode, systemUser, false);
				if (!InodeUtils.isSet(parentFolder.getInode())) {
					Host host;
					host = (Host) hostAPI.find(inode, systemUser, false);
					if (host != null) {
						ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("message.folder.ishostfolder"));
					}
				}
			}
		} catch (DotDataException e) {
			Logger.error(FileForm.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);

		} catch (DotSecurityException e) {
			Logger.error(FileForm.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return ae;
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
	 * Returns the selectedparent.
	 * @return String
	 */
	public String getSelectedparent() {
		return selectedparent;
	}

	/**
	 * Returns the selectedparentPath.
	 * @return String
	 */
	public String getSelectedparentPath() {
		return selectedparentPath;
	}

	/**
	 * Sets the selectedparent.
	 * @param selectedparent The selectedparent to set
	 */
	public void setSelectedparent(String selectedparent) {
		this.selectedparent = selectedparent;
	}

	/**
	 * Sets the selectedparentPath.
	 * @param selectedparentPath The selectedparentPath to set
	 */
	public void setSelectedparentPath(String selectedparentPath) {
		this.selectedparentPath = selectedparentPath;
	}

	/**
	 * Returns the author.
	 * @return String
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Returns the publishDate.
	 * @return java.util.Date
	 */
	public java.util.Date getPublishDate() {
		return publishDate;
	}

	/**
	 * Returns the webPublishDate.
	 * @return String
	 */
	public String getWebPublishDate() {
		return webPublishDate;
	}

	/**
	 * Sets the author.
	 * @param author The author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * Sets the publishDate.
	 * @param publishDate The publishDate to set
	 */
	public void setPublishDate(java.util.Date publishDate) {
		this.publishDate = publishDate;
	}

	/**
	 * Sets the webPublishDate.
	 * @param webPublishDate The webPublishDate to set
	 */
	public void setWebPublishDate(String webPublishDate) {
		this.webPublishDate = webPublishDate;
		try {
			this.publishDate = new SimpleDateFormat("MM/dd/yyyy").parse(webPublishDate);			
		} catch(ParseException ex) {

		}		
	}

	/**
	 * Returns the categories.
	 * @return String[]
	 */
	public String[] getCategories() {
		return categories;
	}

	/**
	 * Sets the categories.
	 * @param categories The categories to set
	 */
	public void setCategories(String[] categories) {
        Logger.debug(this, "\n\nFileForm setCategories=" + categories.length);
		this.categories = categories;
	}

	/**
	 * @return
	 */
	public String get_EXT_3_fileName() {
		return _EXT_3_fileName;
	}

	/**
	 * @param string
	 */
	public void set_EXT_3_fileName(String string) {
		_EXT_3_fileName = string;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
	
	

}
