package com.dotmarketing.portlets.categories.model;


import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionSummary;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.liferay.portal.model.User;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.ws.rs.NotSupportedException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Category extends Inode implements Serializable, ManifestItem {

	private static final long serialVersionUID = 1L;

	/** persistent field */
	private String categoryName;
    private String description;
    private String key;
    private Integer sortOrder;
	private boolean active = true;
	private String keywords;
	private String categoryVelocityVarName;
	private Date modDate;

	/**
     * @return Returns the active.
     */
    public boolean isActive() {
        return this.active;
    }
    /**
     * @param active The active to set.
     */
    public void setActive(boolean active) {
        this.active = active;
    }
	/** default constructor */
	public Category() {
		super.setType("category");
		modDate = new Date();
	}

	/** minimal constructor */
	public Category(java.lang.String categoryName) {
		this.categoryName = categoryName;
	}

	public String getInode() {
		if(InodeUtils.isSet(this.inode))
    		return this.inode;

    	return "";
	}

	public void setInode(String inode) {
		this.inode = inode;
	}

	public java.lang.String getCategoryName() {
		return this.categoryName;
	}

	public void setCategoryName(java.lang.String categoryName) {
		this.categoryName = categoryName;
	}

	public Integer getSortOrder() {
		return this.sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		if(sortOrder==null) {
			this.sortOrder = 0;
		} else {
			this.sortOrder = sortOrder;
		}
	}
	public void setSortOrder(String sortOrder) {
		try {
			this.sortOrder = Integer.parseInt(sortOrder);
		} catch (Exception e) {
		}
	}



	/**
	 * Returns the description.
	 * @return String
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description.
	 * @param description The description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

    /**
     * @return Returns the key.
     */
    public String getKey() {
        return this.key;
    }
    /**
     * @param key The key to set.
     */
    public void setKey(String key) {
        this.key = key;
    }




    public String getKeywords() {
        return this.keywords;
    }
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }



	@CloseDBIfOpened
	public boolean hasActiveChildren() {
		DotConnect db = new DotConnect ();
		String query = "select count(*) as test_count from category cat where cat.inode in (select tree.child from tree tree where tree.parent = ? )  and cat.active = ?";
		db.setSQL(query);
		db.addParam(this.getInode());
		db.addParam(true);
		return db.getInt("test_count")>0;

	}

	public Map<String, Object> getMap () {
        HashMap<String, Object> map = new HashMap<> ();
        map.put("categoryName", this.getCategoryName());
        map.put("description", this.getDescription());
        map.put("key", this.getKey());
        map.put("keywords", this.getKeywords());
        map.put("sortOrder", this.getSortOrder());
        map.put("inode", this.getInode());
        return map;
    }

	public void setCategoryVelocityVarName(String categoryVelocityVarName) {
		this.categoryVelocityVarName = categoryVelocityVarName;
	}
	public String getCategoryVelocityVarName() {
		return categoryVelocityVarName;
	}

	public Date getModDate() {
		return modDate;
	}
	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}
	//The following methods are part of the permissionable interface
	//to define what kind of permissions are accepted by categories
	//and also how categories should behave in terms of cascading
    /**
     * @author David H Torres
     */
	@Override
	public List<PermissionSummary> acceptedPermissions() {
		List<PermissionSummary> accepted = new ArrayList<>();
		accepted.add(new PermissionSummary("use",
				"use-permission-description", PermissionAPI.PERMISSION_USE));
		accepted.add(new PermissionSummary("add-children",
				"add-children-permission-description", PermissionAPI.PERMISSION_CAN_ADD_CHILDREN));
		accepted.add(new PermissionSummary("edit",
				"edit-permission-description",
				PermissionAPI.PERMISSION_EDIT));
		accepted.add(new PermissionSummary("edit-permissions",
				"edit-permissions-permission-description",
				PermissionAPI.PERMISSION_EDIT_PERMISSIONS));
		return accepted;
	}

	@Override
	public Permissionable getParentPermissionable() throws DotDataException {
		CategoryAPI catAPI = APILocator.getCategoryAPI();
		UserAPI userAPI = APILocator.getUserAPI();
		List<Category> parentCategories;
		try {
			parentCategories = catAPI.getParents(this, userAPI.getSystemUser(), false);
		} catch (DotSecurityException e) {
			Logger.error(Category.class, e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		if(parentCategories.size() > 0) {
			return parentCategories.get(0);
		}else{
			User systemUser = APILocator.getUserAPI().getSystemUser();
			Host host = null;
			List<Tree> trees = TreeFactory.getTreesByChild(this);
			for(Tree tree : trees) {
				try{
					host = APILocator.getHostAPI().find(tree.getParent(), systemUser, false);
					if(host != null) break;
				}catch(Exception e){Logger.error(Category.class, e.getMessage(), e);}
			}
			if (host != null && InodeUtils.isSet(host.getInode())){
				return host;
			}
			try{
				host = APILocator.getHostAPI().findSystemHost(systemUser, false);
			}catch(Exception e){Logger.error(Category.class, e.getMessage(), e);}

			return host;
		}
	}

	@Override
	public boolean isParentPermissionable() {
		return true;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).
	       append("categoryName", categoryName).
	       append("description", description).
	       append("key", key).
	       append("sortOrder", sortOrder).
	       append("active", active).
	       append("keywords", keywords).
	       append("categoryVelocityVarName", categoryVelocityVarName).
	       toString();
	}

	@Override
	public ManifestInfo getManifestInfo() {

		return new ManifestInfoBuilder()
				.objectType(PusheableAsset.CATEGORY.getType())
				.id(this.getInode())
				.title(this.getCategoryName())
				.build();
	}
}
