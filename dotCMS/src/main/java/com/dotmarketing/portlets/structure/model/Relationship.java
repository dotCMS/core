package com.dotmarketing.portlets.structure.model;

import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.InodeFactory;
import com.dotmarketing.util.Logger;



public class Relationship extends Inode 
{
	
	private static final long serialVersionUID = 1L;
	
	private String parentStructureInode;
	private String childStructureInode;
	private String parentRelationName;
	private String childRelationName;
	private String relationTypeValue;
	private int cardinality;
	private boolean parentRequired;
    private boolean childRequired;
    private boolean fixed;
	
    
    
    public Relationship(){
    	super.setType("relationship");	
    }


    public Relationship(Structure parentStructure, Structure childStructure, String parentRelationName, String childRelationName, int cardinality, boolean parentRequired, boolean childRequired){
    	super.setType("relationship");	
    	this.parentStructureInode = parentStructure.getInode();
    	this.childStructureInode = childStructure.getInode();
    	this.parentRelationName = parentRelationName;
    	this.childRelationName = childRelationName;
    	this.cardinality = cardinality;
    	this.parentRequired = parentRequired;
    	this.childRequired = childRequired;
    	//TODO: Add field.velocityVarName
    	this.relationTypeValue = parentRelationName.replaceAll(" ", "_") + "-" + childRelationName.replaceAll(" ", "_");
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
		this.childRelationName = childRelationName;
	}
	/**
	 * @return Returns the childStructureInode.
	 */
	public String getChildStructureInode() {
		return childStructureInode;
	}
	
	public Structure getChildStructure () {
	  try {
        return new StructureTransformer(APILocator.getContentTypeAPI(APILocator.systemUser()).find(childStructureInode)).asStructure();
      } catch (DotStateException | DotDataException | DotSecurityException e) {
        throw new DotStateException("getChildStructure Struc not found, childStructureInode:" + childStructureInode, e);

      }
	  
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
		this.parentRelationName = parentRelationName;
	}

	public Structure getParentStructure () {
      try {
        return new StructureTransformer(APILocator.getContentTypeAPI(APILocator.systemUser()).find(parentStructureInode)).asStructure();
      } catch (DotStateException | DotDataException | DotSecurityException e) {
        throw new DotStateException("getParentStructure Struc not found, parentStructureInode:" + parentStructureInode, e);

      }
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
    
	public boolean isFixed() {
		return fixed;
	}

	public void setFixed(boolean fixed) {
		this.fixed = fixed;
	}

	
}
