package com.dotmarketing.portlets.contentlet.struts;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys;
import com.liferay.portal.util.Constants;

public class ContentletForm extends ValidatorForm {

    private static final long serialVersionUID = 1L;

    public static final String INODE_KEY = "inode";
    public static final String LANGUAGEID_KEY = "languageId";
    public static final String STRUCTURE_INODE_KEY = "stInode";
    public static final String LAST_REVIEW_KEY = "lastReview";
    public static final String NEXT_REVIEW_KEY = "nextReview";
    public static final String REVIEW_INTERNAL_KEY = "reviewInternal";
    public static final String DISABLED_WYSIWYG_KEY = "disabledWYSIWYG";
    public static final String LOCKED_KEY = "locked";
    public static final String ARCHIVED_KEY = "archived";
    public static final String LIVE_KEY = "live";
    public static final String WORKING_KEY = "working";
    public static final String MOD_DATE_KEY = "modDate";
    public static final String MOD_USER_KEY = "modUser";
    public static final String OWNER_KEY = "owner";
    public static final String IDENTIFIER_KEY = "identifier";
    public static final String SORT_ORDER_KEY = "sortOrder";
    public static final String HAS_VALIDATION_ERRORS = "hasvalidationerrors";
    public static final String HOST_FOLDER_KEY = "hostOrFolder";
    
    private Map<String, Object> map = new HashMap<String, Object>();     
    
	/** identifier field */    	
	private List AllStructures;
	
	private String[] categories;
	
	private boolean allowChange = true; 
    
    private boolean reviewContent;
    private String reviewIntervalNum;
    private String reviewIntervalSelect;

    private String taskAssignment;
    private String taskComments;
    
    /** default constructor */
    public ContentletForm() {
    	setInode("");
    	setIdentifier("");
    	setLanguageId(0);
    	setStructureInode("");
    	setWorking(false);
    	setArchived(false);
    	setSortOrder(0);
    	setLocked(false);
    	setLive(false);
    	setAllowChange(false);
    	setHasvalidationerrors(false);
    }
   
    public String getCategoryId() {
    	return getInode();
    }
    
    public String getVersionId() {
    	return getIdentifier();
    }
    
    public String getVersionType() {
    	return new String("content");
    }
    
    public void setVersionId(String versionId) {
    	setIdentifier(versionId);
    }
    
    public String getInode() {
    	if(InodeUtils.isSet((String)map.get(INODE_KEY)))
    		return (String)map.get(INODE_KEY);
    	
    	return "";
    }

    public void setInode(String inode) {
        map.put(INODE_KEY, inode);
    }
    
    public long getLanguageId() {
    	return (Long)map.get(LANGUAGEID_KEY);
    }

    public void setLanguageId(long languageId) {
        map.put(LANGUAGEID_KEY, languageId);
    }
    
    public Boolean getHasvalidationerrors() {
    	return (Boolean)map.get(HAS_VALIDATION_ERRORS);
    	
    }

    public void setHasvalidationerrors(boolean Hasvalidationerrors) {
        map.put(LANGUAGEID_KEY, Hasvalidationerrors);
    }

    public String getStructureInode() {
        return (String)map.get(STRUCTURE_INODE_KEY);
    }

    public void setStructureInode(String structureInode) {
    	map.put(STRUCTURE_INODE_KEY, structureInode);   
    }
    
    public Structure getStructure() {
    	Structure structure = null;
    	structure = StructureCache.getStructureByInode(getStructureInode());
        return structure;
    }

    public Date getLastReview() {
    	return (Date)map.get(LAST_REVIEW_KEY);
    }

    public void setLastReview(Date lastReview) {
    	map.put(LAST_REVIEW_KEY, lastReview);
    }

    public Date getNextReview() {
    	return (Date)map.get(NEXT_REVIEW_KEY);
    }

    public void setNextReview(Date nextReview) {
    	map.put(NEXT_REVIEW_KEY, nextReview);
    }

    public String getReviewInterval() {
    	return (String)map.get(REVIEW_INTERNAL_KEY);
    }

    public void setReviewInterval(String reviewInterval) {
    	map.put(REVIEW_INTERNAL_KEY, reviewInterval);
    }

    public int hashCode() {
        return new HashCodeBuilder().append(getInode()).toHashCode();
    }
    
	public String getDisabledWysiwyg() {
		if((ArrayList<String>)map.get(DISABLED_WYSIWYG_KEY) == null)
			return "";
		else	
			return UtilMethods.arrayToString((ArrayList<String>)map.get(DISABLED_WYSIWYG_KEY));		
	}

//	public void setDisabledWysiwyg(List<String> disabledFields) {
//		map.put(DISABLED_WYSIWYG_KEY, disabledFields);
//	}
	
	public void setDisabledWysiwyg(String disabledWysiwyg) {
		if(disabledWysiwyg != null){
			String[] s = disabledWysiwyg.split(",");
			List<String> l = new ArrayList<String>();
			for (String inode : s) {
				l.add(inode);
			}
	//		setDisabledWysiwyg(l);
			map.put(DISABLED_WYSIWYG_KEY, l);
		}
	}
	
	public String getStringProperty(String fieldVarName) throws DotRuntimeException {
		try{
			return (String)map.get(fieldVarName);
		}catch (Exception e) {
			 throw new DotRuntimeException(e.getMessage(), e);
		}
	}
	
	public void setStringProperty(String fieldVarName,String stringValue) throws DotRuntimeException {
		map.put(fieldVarName, stringValue);
	}
	
	public void setLongProperty(String fieldVarName, long longValue) throws DotRuntimeException {
		map.put(fieldVarName, longValue);
	}
	
	public long getLongProperty(String fieldVarName) throws DotRuntimeException {
		try{
			return (Long)map.get(fieldVarName);
		}catch (Exception e) {
			 throw new DotRuntimeException("Unable to retrive field value", e);
		}
	}
	
	public void setBoolProperty(String fieldVarName, boolean boolValue) throws DotRuntimeException {
		map.put(fieldVarName, boolValue);
	}
	
	public boolean getBoolProperty(String fieldVarName) throws DotRuntimeException {
		try{
			return (Boolean)map.get(fieldVarName);
		}catch (Exception e) {
			 throw new DotRuntimeException("Unable to retrive field value", e);
		}
	}
	
	public void setDateProperty(String fieldVarName, Date dateValue) throws DotRuntimeException {
		map.put(fieldVarName, dateValue);
	}
	
	public Date getDateProperty(String fieldVarName) throws DotRuntimeException {
		try{
			return (Date)map.get(fieldVarName);
		}catch (Exception e) {
			 throw new DotRuntimeException("Unable to retrive field value", e);
		}
	}
	
	public void setFloatProperty(String fieldVarName, float floatValue) throws DotRuntimeException {
		map.put(fieldVarName, floatValue);
	}
	
	public float getFloatProperty(String fieldVarName) throws DotRuntimeException {
		try{
			return (Float)map.get(fieldVarName);
		}catch (Exception e) {
			 throw new DotRuntimeException("Unable to retrive field value", e);
		}
	}

	
	public void setProperty(String fieldVarName, Object value) throws DotRuntimeException {
		map.put(fieldVarName, value);
	}
	
	public Object getProperty(String fieldVarName) throws DotRuntimeException {
		try{
			return map.get(fieldVarName);
		}catch (Exception e) {
			 throw new DotRuntimeException("Unable to retrive field value", e);
		}
	}	
	
	/**
	 * Returns a map of the contentlet properties based on the fields of the structure
	 * The keys used in the map will be the velocity variables names
	 */
	public Map<String, Object> getMap() throws DotRuntimeException {
		return new HashMap<String, Object>(map);
	}

	/**
	 * Returns the deleted.
	 * @return boolean
	 */
	public boolean isArchived() {
		return (Boolean)map.get(ARCHIVED_KEY);
	}

	/**
	 * Returns the live.
	 * @return boolean
	 */
	public boolean isLive() {
		return (Boolean)map.get(LIVE_KEY);
	}

	/**
	 * Returns the locked.
	 * @return boolean
	 */
	public boolean isLocked() {
		return (Boolean)map.get(LOCKED_KEY);
	}

	/**
	 * Returns the modDate.
	 * @return java.util.Date
	 */
	public Date getModDate() {
		return (Date)map.get(MOD_DATE_KEY);
	}

	/**
	 * Returns the modUser.
	 * @return String
	 */
	public String getModUser() {
		return (String)map.get(MOD_USER_KEY);	
	}

	/**
	 * Returns the working.
	 * @return boolean
	 */
	public boolean isWorking() {
		return (Boolean)map.get(WORKING_KEY);
	}

	/**
	 * Sets the deleted.
	 * @param deleted The deleted to set
	 */
	public void setArchived(boolean archived) {
		map.put(ARCHIVED_KEY, archived);
	}

	/**
	 * Sets the live.
	 * @param live The live to set
	 */
	public void setLive(boolean live) {
		map.put(LIVE_KEY, live);
	}

	/**
	 * Sets the locked.
	 * @param locked The locked to set
	 */
	public void setLocked(boolean locked) {
		map.put(LOCKED_KEY, locked);
	}

	/**
	 * Sets the modDate.
	 * @param modDate The modDate to set
	 */
	public void setModDate(Date modDate) {
		map.put(MOD_DATE_KEY, modDate);
	}

	/**
	 * Sets the modUser.
	 * @param modUser The modUser to set
	 */
	public void setModUser(String modUser) {
		map.put(MOD_USER_KEY, modUser);
	}

	/**
	 * Sets the working.
	 * @param working The working to set
	 */
	public void setWorking(boolean working) {
		map.put(WORKING_KEY, working);
	}
	
	
	/**
	 * Sets the owner.
	 * 
	 * @param owner
	 *            The owner to set
	 */
	public void setOwner(String owner) {
		map.put(OWNER_KEY, owner);
	}


	/**
	 * Returns the owner.
	 * 
	 * @return String owner
	 */
	public String getOwner() {
		return (String)map.get(OWNER_KEY);
	}
	
	/**
	 * @return Returns the identifier.
	 */
	public String getIdentifier() {
		return (String)map.get(IDENTIFIER_KEY);
	}

	/**
	 * @param identifier
	 *            The identifier to set.
	 */
	public void setIdentifier(String identifier) {
		map.put(IDENTIFIER_KEY, identifier);
	}
	
	/**
	 * Sets the sort_order.
	 * @param sort_order The sort_order to set
	 */
	public void setSortOrder(long sortOrder) {
		map.put(SORT_ORDER_KEY, sortOrder);
	}
	
	public long getSortOrder(){
		return (Long)map.get(SORT_ORDER_KEY);
	}
	
	public String getPermissionId() {
		return getIdentifier();
	}
    
    

    public String getTaskComments() {
        return taskComments;
    }


    public void setTaskComments(String taskComments) {
        this.taskComments = taskComments;
    }


    public String getTaskAssignment() {
        return taskAssignment;
    }


    public void setTaskAssignment(String taskRole) {
        this.taskAssignment = taskRole;
    }


    public boolean isReviewContent() {
        return reviewContent;
    }


    public void setReviewContent(boolean reviewContent) {
        this.reviewContent = reviewContent;
    }

   

    public String getReviewIntervalNum() {
        return reviewIntervalNum;
    }


    public void setReviewIntervalNum(String reviewIntervalNum) {
        this.reviewIntervalNum = reviewIntervalNum;
    }


    public String getReviewIntervalSelect() {
        return reviewIntervalSelect;
    }


    public void setReviewIntervalSelect(String reviewIntervalSelect) {
        this.reviewIntervalSelect = reviewIntervalSelect;
    }


    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if(request.getParameter("cmd")!=null && request.getParameter("cmd").equals(Constants.ADD)) {
            Logger.debug(this, "Contentlet validation!!!!!!");
            return super.validate(mapping, request);
        }
        return null;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    //Auto-generated getter and setter methods
	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

	

	public List getAllStructures() {
		return AllStructures;
	}

	public void setAllStructures(List allStructures) {
		AllStructures = allStructures;
	}
	
	//END auto-generated getter and setter method
	
	public Date getDate(String dateName)
	{
		try
		{	
			Object oDate = PropertyUtils.getProperty(this,dateName);
            if (oDate instanceof Date) {
                return (Date)oDate;
            } else {
                String sDate = oDate.toString();
                SimpleDateFormat dateFormatter = new SimpleDateFormat(WebKeys.DateFormats.LONGDBDATE);
                try {
                    return dateFormatter.parse(sDate);
                } catch (Exception e) { }
                dateFormatter = new SimpleDateFormat(WebKeys.DateFormats.DBDATE);
                try {
                    return dateFormatter.parse(sDate);
                } catch (Exception e) { }
                dateFormatter = new SimpleDateFormat(WebKeys.DateFormats.SHORTDATE);
                try {
                    return dateFormatter.parse(sDate);
                } catch (Exception e) { }
            }
		}
		catch(Exception ex)
		{			
			Logger.debug(this,ex.toString());
		}
		return new Date();
	}
	
	   /**
	 * This method returns the value for any of the generic fields
	 * of the contentlet, given a fieldName using reflection, invoking the
	 * getter of the field.
	 * @param fieldName
	 * @return
	 */

	public Object getFieldValueByVar(String velocityVariableName) {

		Object value = null;
		try {
			value = map.get(velocityVariableName);
		} catch (Exception e) {
			Logger.error(this,"An error has ocurred trying to get the value for the field: " + velocityVariableName);
		}
		return value;
	}

	public String[] getCategories() {
		return categories;
	}

	public void setCategories(String[] categories) {
		this.categories = categories;
	}

	public boolean isAllowChange() {
		return allowChange;
	}

	public void setAllowChange(boolean allowChange) {
		this.allowChange = allowChange;
	}

	public void setMap(Map<String, Object> map) {
		this.map = map;
	}
	//http://jira.dotmarketing.net/browse/DOTCMS-3232
	public String getHostOrFolder() {
		return (String) map.get(HOST_FOLDER_KEY);
	}

	public void setHostOrFolder(String hostOrFolder) {
		map.put(HOST_FOLDER_KEY, hostOrFolder);
	}
    
}
