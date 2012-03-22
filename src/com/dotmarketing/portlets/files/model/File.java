package com.dotmarketing.portlets.files.model;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

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
import com.dotmarketing.portlets.fileassets.business.IFileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

/** @author Hibernate CodeGenerator */
public class File extends WebAsset implements Serializable,IFileAsset {

	private static final long serialVersionUID = 1L;
	public static final String UNKNOWN_MIME_TYPE = "unknown";

	/** identifier field */
	private String parent;

	/** nullable persistent field */
	private String fileName;

	/** nullable persistent field */
	private int size;

	/** nullable persistent field */
	private int height;

	/** nullable persistent field */
	private int width;

	/** nullable persistent field */
	private String mimeType;

	/** nullable persistent field */
	private String author;

	/** nullable persistent field */
	private java.util.Date publishDate;

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

	private String myPath = null;
	
	
	
	
	/** default constructor */
	public File() {
		super.setType("file_asset");	
		super.setModDate(new java.util.Date());
		publishDate = new java.util.Date();
	}

	public String getURI(Folder folder) {
		String uri ="";
		try {
			uri = APILocator.getIdentifierAPI().find(folder).getPath() + this.getFileName();
		} catch (Exception e) {
			Logger.debug(File.class,e.toString());
		} 
		return uri;
	}
	public String getURI() {
		Identifier id = new Identifier();
		try {
			id = APILocator.getIdentifierAPI().find(this);
		} catch (Exception e) {
			Logger.debug(File.class,e.toString());
		} 
		return id.getURI();


	}

	public String getPath() 
	{
		// avoid going to db if there is nothing to check
		if(!UtilMethods.isSet(inode)){
			return null;
		}
		if(myPath == null){
			String path = "";
			try
			{

				Folder folder = APILocator.getFolderAPI().findParentFolder(this,APILocator.getUserAPI().getSystemUser(),false);
				if(InodeUtils.isSet(folder.getInode()))
				{
					path = APILocator.getIdentifierAPI().find(folder).getPath();
				}
			}
			catch(Exception ex)
			{
				Logger.debug(File.class,ex.toString());
			}

			myPath = path;
		}
		return myPath;
	}


	public String getInode() {
		if(InodeUtils.isSet(this.inode))
    		return this.inode;
    	
    	return "";
	}

	public void setInode(String inode) {
		this.inode = inode;
	}
	public java.lang.String getFileName() {
		return this.fileName;
	}

	public void setFileName(java.lang.String fileName) {
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

	public boolean equals(Object other) {
		if ( !(other instanceof File) ) return false;
		File castOther = (File) other;
		return new EqualsBuilder()
		.append(this.inode, castOther.inode)
		.isEquals();
	}

	public int hashCode() {
		return new HashCodeBuilder()
		.append(inode)
		.toHashCode();
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
		 if(InodeUtils.isSet(parent))
			 return parent;
		 
		 return "";
	 }

	 /**
	  * Sets the parent.
	  * @param parent The parent to set
	  */
	 public void setParent(String parent) {
		 this.parent = parent;
	 }

	 //Every Web Asset should implement this method!!!
	 public void copy(File newFile) {
		 this.setParent(newFile.getParent());
		 this.setFileName(newFile.getFileName());
		 this.setSize(newFile.getSize());
		 this.setMimeType(newFile.getMimeType());
		 this.setWidth(newFile.getWidth());
		 this.setHeight(newFile.getHeight());
		 super.copy(newFile);
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
	  * Returns the height.
	  * @return int
	  */
	 public int getHeight() {
		 return height;
	 }

	 /**
	  * Returns the width.
	  * @return int
	  */
	 public int getWidth() {
		 return width;
	 }

	 public String getExtension(){
		 return UtilMethods.getFileExtension(fileName);

	 }

	 public java.lang.String getNameOnly() {
		 return UtilMethods.getFileName(fileName);
	 }

	 /**
	  * Sets the height.
	  * @param height The height to set
	  */
	 public void setHeight(int height) {
		 this.height = height;
	 }

	 /**
	  * Sets the width.
	  * @param width The width to set
	  */
	 public void setWidth(int width) {
		 this.width = width;
	 }

	 public int compareTo(File compObject){

		 if(!(compObject instanceof File))return -1;

		 File file = (File) compObject;
		 return (file.getFileName().compareTo(this.getFileName()));

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
		 Logger.debug(this, "\n\nFile setCategories=" + categories.length);
		 this.categories = categories;
	 }

	 /**
	  * This method returns a map with all the file attributes 
	 * @throws DotSecurityException 
	 * @throws DotDataException 
	 * @throws DotStateException 
	  */
	 public Map<String, Object> getMap () throws DotStateException, DotDataException, DotSecurityException {
		 Map<String, Object> map = super.getMap();
		 map.put("parent", parent);
		 map.put("fileName", fileName);
		 map.put("extension", getExtension());
		 map.put("size", size);
		 map.put("height", height);
		 map.put("width", width);
		 map.put("maxHeight", maxHeight);
		 map.put("maxSize", maxSize);
		 map.put("publishDate", publishDate);
		 map.put("author", author);
		 map.put("maxWidth", maxWidth);
		map.put("minHeight", minHeight);
		map.put("categories", categories);
		map.put("isContent", false);
		return map;
	}

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
			Folder parentFolder = APILocator.getFolderAPI().findParentFolder(this,APILocator.getUserAPI().getSystemUser(),false);
			
			if(parentFolder != null && InodeUtils.isSet(parentFolder.getInode()))
				return parentFolder;
			
			Host host =  APILocator.getHostAPI().findParentHost(this, APILocator.getUserAPI().getSystemUser(), false);
	
			if(host != null && InodeUtils.isSet(host.getIdentifier()))
				return host;
		
			return APILocator.getHostAPI().findSystemHost(APILocator.getUserAPI().getSystemUser(), false);
		} catch (DotSecurityException e) {
			Logger.error(File.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}

	}

	public long getFileSize() {

		return new Long(this.getSize());
	}

	public InputStream getFileInputStream() throws FileNotFoundException {
	
		return null;
	}

	public java.io.File getFileAsset() {

		return null;
	}

	public int getMenuOrder() {
		return getSortOrder();
	}

	public void setMenuOrder(int sortOrder) {
		setSortOrder(sortOrder);
		
	}	
	
	
	
	

}
