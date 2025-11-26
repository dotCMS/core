package com.dotmarketing.portlets.structure.model;

import static com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_ONE;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.publisher.util.PusheableAsset;
import com.dotcms.publishing.manifest.ManifestItem;
import com.dotmarketing.beans.LegacyInode;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.TreeFactory;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.liferay.util.StringPool;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 * <p>Defines a Content Relationship in dotCMS.</p>
 * <p>Relationships represent a pre-defined hierarchical relationship among different Content Types. Individual
 * pieces of Content may then be related to other content in a parent-child relationship. When certain types of
 * content need to be found by related content in a hierarchy (e.g. you need to find children of an item, or the
 * parent or siblings of an item), then Relationships are usually the best solution.</p>
 *
 * @author root
 * @since Mar 22, 2012
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class Relationship extends LegacyInode implements Serializable, ManifestItem {
	
	private static final long serialVersionUID = 1L;
	
	private String parentStructureInode;
	private String childStructureInode;
	private String parentRelationName;
	private String childRelationName;
	private String relationTypeValue;
	private int cardinality;
	private boolean parentRequired;
    private boolean childRequired;
    private boolean fixed=false;
    private String inode;
    private Date modDate;

    /**
     * Default class constructor.
     */
    public Relationship(){
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
		this.parentStructureInode = parentStructure.getInode();
		this.childStructureInode = childStructure.getInode();
		this.parentRelationName = parentRelationName;
		this.childRelationName = childRelationName;
		this.cardinality = cardinality;
		this.parentRequired = parentRequired;
		this.childRequired = childRequired;

		this.relationTypeValue = parentRelationName.replaceAll(" ", "_") + "-" + childRelationName
				.replaceAll(" ", "_");

		final RelationshipConstructionStrategy strategy = new RelationshipStrategyFactory()
				.getRelationshipConstructionStrategy(cardinality == MANY_TO_ONE.ordinal());
		strategy.apply(this);
	}

    /**
     * Creates a new instance of the {@link Relationship} class.
     *
     * @param parentContentType The parent {@link ContentType}.
     * @param childContentType The child {@link ContentType}.
     * @param field The {@link Field} defining this relationship.
     */
	public Relationship(final ContentType parentContentType, final ContentType childContentType,
			final Field field) {
		super();
		final int cardinality = Integer.parseInt(field.values());

		this.parentStructureInode = parentContentType.id();
		this.childStructureInode = childContentType.id();
		this.parentRelationName = null;
		this.childRelationName = field.variable();
		this.cardinality = Integer.parseInt(field.values());
		this.parentRequired = false;
		this.childRequired = field.required();
		this.relationTypeValue =
				parentContentType.variable() + StringPool.PERIOD + field.variable();

		final RelationshipConstructionStrategy strategy = new RelationshipStrategyFactory()
				.getRelationshipConstructionStrategy(cardinality == MANY_TO_ONE.ordinal());
		strategy.apply(this);
	}

	/**
	 * @return Returns the cardinality.
	 */
	public int getCardinality() {
		return cardinality;
	}

	/**
	 * Sets the cardinality of the relationship (One to One, One to Many, Many to One, or Many to Many).
     *
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

    /**
     * Retrieves the child Content Type in this Relationship.
     *
     * @return The {@link Structure} object representing the child Content Type.
     */
	public Structure getChildStructure() {
		try {
			return UtilMethods.isSet(childStructureInode) ? new StructureTransformer(
					APILocator.getContentTypeAPI(APILocator.systemUser()).find(childStructureInode))
					.asStructure() : null;
		} catch (final DotStateException | DotDataException | DotSecurityException e) {
            throw new DotStateException(String.format("Child Content Type with ID '%s' was not found in Relationship " +
                    "'%s'. Make sure such a Content Type exists.", childStructureInode,
                    getRelationTypeValue()), e);
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

    /**
     * Retrieves the parent Content Type in this Relationship.
     *
     * @return The {@link Structure} object representing the parent Content Type.
     */
	public Structure getParentStructure() {
		try {
			return UtilMethods.isSet(parentStructureInode) ? new StructureTransformer(
					APILocator.getContentTypeAPI(APILocator.systemUser())
							.find(parentStructureInode))
					.asStructure() : null;
		} catch (final DotStateException | DotDataException | DotSecurityException e) {
            throw new DotStateException(String.format("Parent Content Type with ID '%s' was not found in Relationship" +
                    " '%s'. Make sure such a Content Type exists.", parentStructureInode,
                    getRelationTypeValue()), e);
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
		return false;
	}

	/**
	 * Returns true if the relationship is the new kind of relationship (Relationship Field)
	 * @return
	 */
	public boolean isRelationshipField() {
		return this.relationTypeValue != null && this.relationTypeValue
				.matches("[a-zA-z0-9]+\\.[a-zA-Z0-9]+");
	}

	public void setFixed(boolean fixed) {
	    
	}

	public String getInode() {
		return inode;
	}

	public String getIdentifier() { return getInode(); }

	public void setInode(String inode) {
		this.inode = inode;
	}

	public String getTitle() {
		return getRelationTypeValue();
	}

	public boolean hasParents() {
		return TreeFactory.getTreesByChild(this.inode).size()>0;
	}

	public boolean isLive() {
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Relationship that = (Relationship) o;
		return Objects.equals(inode, that.inode);
	}

	@Override
	public int hashCode() {
		return Objects.hash(inode);
	}

	public Date getModDate() {
		return modDate;
	}

	public void setModDate(Date modDate) {
		this.modDate = modDate;
	}

	@Override
	public String toString() {
		return "Relationship{" +
				"inode='" + inode + '\'' +
				", parentStructureInode='" + parentStructureInode + '\'' +
				", childStructureInode='" + childStructureInode + '\'' +
				", parentRelationName='" + parentRelationName + '\'' +
				", childRelationName='" + childRelationName + '\'' +
				", relationTypeValue='" + relationTypeValue + '\'' +
				", cardinality=" + cardinality +
				", parentRequired=" + parentRequired +
				", childRequired=" + childRequired +
				", fixed=" + fixed +
				", modDate=" + modDate +
				'}';
	}

	@Override
	public ManifestInfo getManifestInfo(){
		return new ManifestInfoBuilder()
			.objectType(PusheableAsset.RELATIONSHIP.getType())
			.id(this.inode)
			.title(this.getTitle())
			.build();
	}
}
