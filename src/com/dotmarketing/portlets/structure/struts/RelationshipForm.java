package com.dotmarketing.portlets.structure.struts;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.validator.ValidatorForm;

import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UtilMethods;



public class RelationshipForm extends ValidatorForm 
{
	private static final long serialVersionUID = 1L;

	private String inode;
	private String parentStructureInode;
	private String childStructureInode;
	private String parentRelationName = "";
	private String childRelationName = "";
	private String relationTypeValue;
	private int cardinality = com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal();
	private boolean parentRequired;
    private boolean childRequired;
    private boolean fixed = false;
	
	public boolean isFixed() {
		return fixed;
	}
	public void setFixed(boolean fixed) {
		this.fixed = fixed;
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
	 * @return Returns the cardinality.
	 */
	public int getCardinality() {
		return cardinality;
	}
	/**
	 * @param cardinality The cardinality to set.
	 */
	public void setCardinality(int cardinality) {
		this.cardinality = cardinality;
	}
	/**
	 * @return Returns the childRelationName.
	 */
	public String getChildRelationName() {
		return childRelationName;
	}
	/**
	 * @param childRelationName The childRelationName to set.
	 */
	public void setChildRelationName(String childRelationName) {
		this.childRelationName = UtilMethods.isSet(childRelationName)?childRelationName.trim():"";
	}
	/**
	 * @return Returns the childStructureInode.
	 */
	public String getChildStructureInode() {
		return childStructureInode;
	}
	/**
	 * @param childStructureInode The childStructureInode to set.
	 */
	public void setChildStructureInode(String childStructureInode) {
		this.childStructureInode = childStructureInode;
	}
	/**
	 * @return Returns the parentRelationName.
	 */
	public String getParentRelationName() {
		return parentRelationName;
	}
	/**
	 * @param parentRelationName The parentRelationName to set.
	 */
	public void setParentRelationName(String parentRelationName) {
		this.parentRelationName = UtilMethods.isSet(parentRelationName)?parentRelationName.trim():"";
	}
	/**
	 * @return Returns the parentStructureInode.
	 */
	public String getParentStructureInode() {
		return parentStructureInode;
	}
	/**
	 * @param parentStructureInode The parentStructureInode to set.
	 */
	public void setParentStructureInode(String parentStructureInode) {
		this.parentStructureInode = parentStructureInode;
	}
	/**
	 * @return Returns the relationTypeValue.
	 */
	public String getRelationTypeValue() {
		return relationTypeValue;
	}
	/**
	 * @param relationTypeValue The relationTypeValue to set.
	 */
	public void setRelationTypeValue(String relationTypeValue) {
		this.relationTypeValue = relationTypeValue;
	}
    public boolean isChildRequired() {
        return childRequired;
    }
    public void setChildRequired(boolean childRequired) {
        this.childRequired = childRequired;
    }
    public boolean isParentRequired() {
        return parentRequired;
    }
    public void setParentRequired(boolean parentRequired) {
        this.parentRequired = parentRequired;
    }
	@Override
	public ActionErrors validate(ActionMapping arg0, HttpServletRequest arg1) {
		ActionErrors errors = super.validate(arg0, arg1);
		if(errors == null)
			errors = new ActionErrors();
		if(UtilMethods.isSet(getChildRelationName()) && UtilMethods.isSet(getParentRelationName()) && 
				getChildRelationName().equals(getParentRelationName())) {
			errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("error.relationship.same.parent.child.name"));
		}
		return errors;
	}

	
}
