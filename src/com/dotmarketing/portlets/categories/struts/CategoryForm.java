package com.dotmarketing.portlets.categories.struts;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.portal.util.Constants;


/** @author Hibernate CodeGenerator */

public class CategoryForm extends ValidatorForm {
	
	private CategoryAPI categoryAPI = APILocator.getCategoryAPI();
	
    private static final long serialVersionUID = 1L;
    private String inode;
    private String parent;
    private String description;
    private String keywords;
    private String key;
    private String categoryName;
    private boolean active;
    private int sortOrder;
    private String categoryVelocityVarName;

    public String getKeywords() {
        return this.keywords;
    }
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    /** full constructor */
    public CategoryForm(java.lang.String categoryName, int sortOrder) {
        this.categoryName = categoryName;
        this.sortOrder = sortOrder;
    }

    /** default constructor */
    public CategoryForm() {
    }

    /** minimal constructor */
    public CategoryForm(java.lang.String categoryName) {
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

    public int getSortOrder() {
        return this.sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String toString() {
        return new ToStringBuilder(this).append("inode", getInode()).toString();
    }

    public boolean equals(Object other) {
        if (!(other instanceof CategoryForm)) {
            return false;
        }

        CategoryForm castOther = (CategoryForm) other;

        return new EqualsBuilder().append(this.getInode(), castOther.getInode()).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder().append(getInode()).toHashCode();
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

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        
    	User user = null;
		try {
			user = com.liferay.portal.util.PortalUtil.getUser(request);
		} catch (Exception e1) {
			Logger.error(this, e1.getMessage(), e1);
		}
    	
		Logger.debug(this, "validating CategoryForm: " + request.getParameter("cmd"));

        if ((request.getParameter("cmd") != null) && request.getParameter("cmd").equals(Constants.ADD)) {
            ActionErrors ae = super.validate(mapping, request);

            Logger.debug(this, "action errors: " + ae);

            if (UtilMethods.isSet(getKey())) {
                Category cat = null;
				try {
					cat = categoryAPI.findByKey(getKey(), user, false);

	                if (cat != null && (InodeUtils.isSet(cat.getInode())) && (!cat.getInode().equalsIgnoreCase(this.inode))) {
	                    ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.category.folder.taken"));
	                    return ae;
	                }
				} catch (DotDataException e) {
                    ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.system.error"));
				} catch (DotSecurityException e) {
                    ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.system.error"));
				}
            }

            if ( !UtilMethods.isSet( getCategoryName() ) ) {
                ae.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.category.folder.mandatoryname"));
                return ae;
            }

        }

        return null;
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
        if(key!=null){
            key = key.replaceAll("/", "").replaceAll(" ", "_").toLowerCase();
        }
        this.key = key;
    }

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

    public CategoryAPI getCategoryAPI() {
		return categoryAPI;
	}

	public void setCategoryAPI(CategoryAPI categoryAPI) {
		this.categoryAPI = categoryAPI;
	}
	public void setCategoryVelocityVarName(String categoryVelocityVarName) {
		this.categoryVelocityVarName = categoryVelocityVarName;
	}
	public String getCategoryVelocityVarName() {
		return categoryVelocityVarName;
	}


}
