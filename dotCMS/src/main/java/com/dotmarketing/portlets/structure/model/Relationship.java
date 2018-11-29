package com.dotmarketing.portlets.structure.model;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotmarketing.beans.Inode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;


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

	/**
	 * Use {@link Relationship#Relationship(ContentType, ContentType, Field)} instead.
	 * Otherwise, use at your own risk
	 *
	 * @param parentStructure
	 * @param childStructure
	 * @param parentRelationName
	 * @param childRelationName
	 * @param cardinality
	 * @param parentRequired
	 * @param childRequired
	 */
    @Deprecated
	public Relationship(final Structure parentStructure, final Structure childStructure,
			final String parentRelationName, final String childRelationName, final int cardinality,
			final boolean parentRequired, final boolean childRequired) {
		super();
		this.setType("relationship");
		this.parentStructureInode = parentStructure.getInode();
		this.childStructureInode = childStructure.getInode();
		this.parentRelationName = parentRelationName;
		this.childRelationName = childRelationName;
		this.cardinality = cardinality;
		this.parentRequired = parentRequired;
		this.childRequired = childRequired;
		this.relationTypeValue = parentRelationName.replaceAll(" ", "_") + "-" + childRelationName
				.replaceAll(" ", "_");

	}

	public Relationship(final ContentType parentContentType, final ContentType childContentType,
			final Field field) {
		super();
		this.setType("relationship");
		this.parentStructureInode = parentContentType.id();
		this.childStructureInode = childContentType.id();
		this.parentRelationName = null;
		this.childRelationName = field.variable();
		this.cardinality = Integer.parseInt(field.values());
		this.parentRequired = false;
		this.childRequired = field.required();
		this.relationTypeValue = parentContentType.variable() + StringPool.PERIOD + field.variable();
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

	public Structure getChildStructure() {
		try {
			return UtilMethods.isSet(childStructureInode) ? new StructureTransformer(
					APILocator.getContentTypeAPI(APILocator.systemUser()).find(childStructureInode))
					.asStructure() : null;
		} catch (DotStateException | DotDataException | DotSecurityException e) {
			throw new DotStateException(
					"getChildStructure Struc not found, childStructureInode:" + childStructureInode,
					e);

		}

	}
	
	/**
	 * Use at your own risk. This property should not be modified once the object is created through
	 * the constructor {@link Relationship#Relationship(ContentType, ContentType, Field)}
	 * @param childStructureInode The childStructureInode to set.
	 */
	@Deprecated
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

	public Structure getParentStructure() {
		try {
			return UtilMethods.isSet(parentStructureInode) ? new StructureTransformer(
					APILocator.getContentTypeAPI(APILocator.systemUser())
							.find(parentStructureInode))
					.asStructure() : null;
		} catch (DotStateException | DotDataException | DotSecurityException e) {
			throw new DotStateException("getParentStructure Struc not found, parentStructureInode:"
					+ parentStructureInode, e);

		}
	}
	
	/**
	 *  @return Returns the parentStructureInode.
	 */
	public String getParentStructureInode() {
		return parentStructureInode;
	}
	/**
	 * Use at your own risk. This property should not be modified once the object is created through
	 * the constructor {@link Relationship#Relationship(ContentType, ContentType, Field)}
	 * @param parentStructureInode The parentStructureInode to set.
	 */
	@Deprecated
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
	 * Use at your own risk. This property should not be modified once the object is created through
	 * the constructor {@link Relationship#Relationship(ContentType, ContentType, Field)}
	 * @param relationTypeValue The relationTypeValue to set.
	 */
	@Deprecated
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
