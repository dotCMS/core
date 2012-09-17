package com.dotmarketing.beans;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.RelatedPermissionableGroup;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.categories.business.Categorizable;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;



/**
 * 
 * @author maria
 */
public class Identifier implements UUIDable,Serializable,Permissionable,Categorizable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1895228885287457403L;

	/**
	 * 
	 */


	public Identifier() {
	}

	//private String URI;
	
    private String id;
    
    private String assetName;
    
    private String assetType;
    
    private String parentPath;

    private String hostId;
    
    //private String inode;
    
	public String getInode() {
	   return getId();	
	}

	public String getId() {
		if (id != null) {
			if (id.contains("-")) {
				UUID uuid = UUID.fromString(id);
				return uuid.toString();
			}else {
				if(id.equals(Host.SYSTEM_HOST)){
					return id;
				}
				try {
					long oldId = Long.valueOf(id);
					return Long.valueOf(oldId).toString();
				} catch (Exception e) {
					return "";
				}
			}
		} else
			return "";
	}

	public void setId(String id) {
		if(id == null||id == "")
			this.id = null;
		else if (id.contains("-")) {
			UUID uuid = UUID.fromString(id);
			this.id = uuid.toString();
		}else if(!id.equals(Host.SYSTEM_HOST)){
			try {
				long oldId = Long.parseLong(id);
				this.id = Long.valueOf(oldId).toString();
			} catch (Exception e) {
				this.id = "";
			}
		}else if(id.equals(Host.SYSTEM_HOST)){
			this.id = id;
		}
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
	 * Returns the uRI.
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
	 * 
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
			}else{
				setAssetType("htmlpage");
				setParentPath(uRI.substring(0, uRI.lastIndexOf("/")+1));
				setAssetName(uRI.substring(uRI.lastIndexOf("/")+1));
			}
		}else if(UtilMethods.getFileExtension(uRI)!="" && !UtilMethods.getFileExtension(uRI).equals(Config.getStringProperty("VELOCITY_PAGE_EXTENSION", "dot"))){
			if(uRI.contains("http://")){
				setAssetType("links");
				setParentPath(uRI.substring(0, uRI.lastIndexOf("http://")));
				setAssetName(uRI.substring(uRI.lastIndexOf("http://")));
			}else if(!assetType.equals("links")) {
				setAssetType("file_asset");
				setParentPath(uRI.substring(0, uRI.lastIndexOf("/")+1));
				setAssetName(uRI.substring(uRI.lastIndexOf("/")+1));
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
	
	/*public void addChild(Inode i, String relationType) {
		Tree tree = TreeFactory.getTree(this.id, i.inode, relationType);
		if (!InodeUtils.isSet(tree.getParent()) || !InodeUtils.isSet(tree.getChild())){
			tree.setParent(this.id);
			tree.setChild(i.getInode());
			tree.setRelationType(relationType);
			TreeFactory.saveTree(tree);
		} else {
			tree.setRelationType(relationType);
			TreeFactory.saveTree(tree);
		}
	}*/
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
	
	public String getPath(){
	    if(getAssetName().equals("system folder") && getParentPath().equals("/System folder"))
	        return "/";
	    else {
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
	
	
	
	
	

}
