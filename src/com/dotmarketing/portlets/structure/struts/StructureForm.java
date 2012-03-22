package com.dotmarketing.portlets.structure.struts;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;

public class StructureForm extends ValidatorForm {
	
	private static final long serialVersionUID = 1L;
	private String inode;
	private String name;
	private String description;
    private boolean reviewContent;
    private String reviewIntervalNum;
    private String reviewIntervalSelect;
    private String reviewerRole;
    private String detailPage;
    private boolean content = false;
    private boolean fixed = false;
    private boolean system = false;
    private int structureType = Structure.STRUCTURE_TYPE_CONTENT;
    private String velocityVarName;
    private String urlMapPattern = "";
    private String folder;
    private String host;
    
	private List fields;

	public String getInode() {
		if(InodeUtils.isSet(inode))
			return inode;
		
		return "";
	}
	public void setInode(String inode) {
		this.inode = inode;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List getFields() {
		return fields;
	}
	public void setFields(List fields) {
		this.fields = fields;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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
    public boolean isReviewContent() {
        return reviewContent;
    }
    public void setReviewContent(boolean reviewContent) {
        this.reviewContent = reviewContent;
    }
    
    public ActionErrors validate(ActionMapping arg0, HttpServletRequest arg1) {
        ActionErrors errors =  super.validate(arg0,arg1);
        if (errors == null) errors = new ActionErrors ();
        if (isReviewContent() && ! InodeUtils.isSet(reviewerRole)) {
            errors.add("reviewerRole", new ActionMessage ("structure.reviewerRole.required"));
        }
        if(!UtilMethods.isSet(host) && (!UtilMethods.isSet(folder) || folder.equals("SYSTEM_FOLDER"))){
       	   errors.add("host", new ActionMessage ("Host-or-folder-is-required"));
		}
        return errors;
	}
	public String getDetailPage() {
		return detailPage;
	}
	public void setDetailPage(String pagedetail) {
		this.detailPage = pagedetail;
	}
	public boolean isContent() {
		return content;
	}
	public void setContent(boolean content) {
		this.content = content;
	}
	public boolean isFixed() {
		return fixed;
	}
	public void setFixed(boolean fixed) {
		this.fixed = fixed;
	}
	
	public boolean isSystem() {
		return system;
	}
	public void setSystem(boolean system) {
		this.system = system;
	}
	
	public int getStructureType() {
		return structureType;
	}
	public void setStructureType(int structureType) {
		this.structureType = structureType;
	}
	public String getReviewerRole() {
		return reviewerRole;
	}
	public void setReviewerRole(String reviewerRole) {
		this.reviewerRole = reviewerRole;
	}
	public void setVelocityVarName(String velocityVarName) {
		this.velocityVarName = velocityVarName;
	}
	public String getVelocityVarName() {
		return velocityVarName;
	}
	/**
	 * @return the urlMapPattern
	 */
	public String getUrlMapPattern() {
		return urlMapPattern;
	}
	/**
	 * @param urlMapPattern the urlMapPattern to set
	 */
	public void setUrlMapPattern(String urlMapPattern) {
		
		if(urlMapPattern == null){
			urlMapPattern = "";
		}
		urlMapPattern = urlMapPattern.trim();
		this.urlMapPattern = urlMapPattern;
	}
	public String getFolder() {
		return folder;
	}
	public void setFolder(String folder) {
		this.folder = folder;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	
	
		
}
