package com.dotmarketing.beans;

import static com.dotmarketing.portlets.folders.business.FolderAPI.SYSTEM_FOLDER_ASSET_NAME;
import static com.dotmarketing.portlets.folders.business.FolderAPI.SYSTEM_FOLDER_PARENT_PATH;

import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.categories.business.Categorizable;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * An Identifier uniquely represents a content in dotCMS. In its simplest form, it's a UUID, which
 * is either guaranteed to be different or is, at least, extremely likely to be different from any
 * other UUID.
 * 
 * @author maria
 */
public class Identifier implements UUIDable,Serializable,Permissionable,Categorizable {

	private static final long serialVersionUID = 1895228885287457403L;

	public static final String ASSET_TYPE_FOLDER = "folder";
	public static final String ASSET_TYPE_HTML_PAGE = "htmlpage";
	public static final String ASSET_TYPE_CONTENTLET = "contentlet";

	public Identifier() {
	}

	public Identifier(final String id) {
		setId(id);
	}

	private String id;
    
    private String assetName;
    
    private String assetType;
    
    private String parentPath;

    private String hostId;
    
    private Date sysPublishDate;
    private Date sysExpireDate;
    
    /**
     * @deprecated As of 2016-05-16, replaced by {@link #getId()}
     */
    @Deprecated
	public String getInode() {
	   return getId();	
	}

	public String getId() {
		return UtilMethods.isSet(id) ? id : null;
	}
	
	public boolean exists(){
	   return UtilMethods.isSet(id);
	}
	

	public void setId(String id) {
	    this.id = UtilMethods.isEmpty(id) ? null : id;
	}

	public void setInode(String inode) {
		setId(inode);
	}

	public String getAssetName() {
		return assetName;
	}

	public void setAssetName(String assetName) {
		this.assetName = assetName;
	}

	public String getAssetType() {
		return assetType;
	}

	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}

	public String getParentPath() {
		return parentPath;
	}

	public void setParentPath(String parentPath) {
		this.parentPath = parentPath;	
	}

	public String getHostId() {
		return hostId;
	}

	public void setHostId(String hostId) {
		this.hostId = hostId;
	}
		
	/**
	 * Returns the URI, which is a concatenation
	 * of parent path (folder) and asset name.
	 * useful for retrieving file/page URLs
	 * 
	 * @return String
	 */
	public String getURI() {
		if(UtilMethods.isSet(parentPath))
			return getParentPath()+ getAssetName();
		else
		    return getAssetName();
		
	}

	/**
	 * Sets the uRI.
	 * IMPORTANT HTML Pages as content should NOT be calling this method.  It is handled in the VersionableAPI   
	 * @param uRI
	 *            The uRI to set
	 */
	public void setURI(String uRI) {
		if(uRI.contains("content")&& !uRI.contains("/")){
			setAssetType("contentlet");
			setParentPath("/");
     		setAssetName(this.id + ".content");
		}else if(uRI.contains("template")&& !uRI.contains("/")){
			setAssetType("template");
			setParentPath("/");
			setAssetName(this.id + ".template");
		}else if(uRI.contains("containers")&& !uRI.contains("/")){
			setAssetType("containers");
			setParentPath("/");
			setAssetName(this.id + ".containers");
		}else if(UtilMethods.getFileExtension(uRI).equals(Config.getStringProperty("VELOCITY_PAGE_EXTENSION", "dot"))){
			if(uRI.contains("http://")){
				setAssetType("links");
				setParentPath(uRI.substring(0, uRI.lastIndexOf("http://")));
				setAssetName(uRI.substring(uRI.lastIndexOf("http://")));
			}
		}else if(UtilMethods.getFileExtension(uRI)!="" && !UtilMethods.getFileExtension(uRI).equals(Config.getStringProperty("VELOCITY_PAGE_EXTENSION", "dot"))){
			if(uRI.contains("http://")){
				setAssetType("links");
				setParentPath(uRI.substring(0, uRI.lastIndexOf("http://")));
				setAssetName(uRI.substring(uRI.lastIndexOf("http://")));
			}
		}else{
			setAssetType("links");
			setParentPath(uRI.substring(0, uRI.lastIndexOf("/")+1));
			setAssetName(uRI.substring(uRI.lastIndexOf("/")+1));
		}
	}
	public void addChild(Inode i) {
		Tree tree = TreeFactory.getTree(this.id, i.inode, "child");
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
			tree.setParent(this.id);
			tree.setChild(i.getInode());
			tree.setRelationType("child");
			TreeFactory.saveTree(tree);
		}
	}

	public void addChild(Identifier i, String relationType, int sortOrder) {
		Tree tree = TreeFactory.getTree(this.id, i.id, relationType);
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
			tree.setParent(this.id);
			tree.setChild(i.getInode());
			tree.setRelationType(relationType);
			tree.setTreeOrder(sortOrder);
			TreeFactory.saveTree(tree);
		} else {
			tree.setRelationType(relationType);
			tree.setTreeOrder(sortOrder);
			TreeFactory.saveTree(tree);
		}
	}
	
	public boolean deleteChild(Inode child) {
		Tree tree = TreeFactory.getTree(this.id, child.getInode(), "child");
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())) {
			return false;
		}
		TreeFactory.deleteTree(tree);
		return true;
	}

	public List<PermissionSummary> acceptedPermissions() {
		return null;
	}

	public String getOwner() {
		return null;
	}

	public Permissionable getParentPermissionable() throws DotDataException {
		return null;
	}

	public String getPermissionId() {
		return getInode();
	}

	public String getPermissionType() {
		return this.getClass().getCanonicalName();
	}

	public boolean isParentPermissionable() {
		return false;
	}

	public List<RelatedPermissionableGroup> permissionDependencies(
			int requiredPermission) {
		return null;
	}

	public void setOwner(String owner) {
	}

	public String getCategoryId() {
	   return getInode();
	}
	
	   /**
     * Returns the Identifier URI, 
     * based on its parent path (folder) and asset name.
     * Appends a forward slash for folder ids.
     * Useful for retrieving file/page URLs
     * 
     * @return String
     * @see getURI
     */
	public String getPath(){
	    if((SYSTEM_FOLDER_ASSET_NAME).equals(getAssetName()) && (SYSTEM_FOLDER_PARENT_PATH).equals(getParentPath())){
	        return "/";
	    } else {
    		String x = getParentPath() + getAssetName();
    		if("folder".equals(assetType)){
    			if(! x.endsWith("/")){
    				x= x + "/";
    			}
    		}
    		return x;
	    }
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Identifier other = (Identifier) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

    public Date getSysPublishDate() {
        return sysPublishDate;
    }

    public void setSysPublishDate(Date sysPublishDate) {
        this.sysPublishDate = sysPublishDate;
    }

    public Date getSysExpireDate() {
        return sysExpireDate;
    }

    public void setSysExpireDate(Date sysExpireDate) {
        this.sysExpireDate = sysExpireDate;
    }

    @Override
    public String toString() {
        return "Identifier [id=" + id + ", assetName=" + assetName + ", assetType=" + assetType + ", parentPath=" + parentPath
                        + ", hostId=" + hostId + ", sysPublishDate=" + sysPublishDate + ", sysExpireDate=" + sysExpireDate + "]";
    }

}
