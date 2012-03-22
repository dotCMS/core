package com.dotmarketing.beans;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author  maria
 */
public class MultiTree implements Serializable {

    private static final long serialVersionUID = 1L;

	/** identifier field */
    private String parent1;

    /** identifier field */
    private String parent2;

    /** identifier field */
    private String child;

    /** nullable persistent field */
    private String relationType;

    /** nullable persistent field */
    private int treeOrder;

    /** full constructor */

    public MultiTree(String parent1, String parent2, String child, java.lang.String relationType, int treeOrder) {

        this.parent1 = parent1;
        this.parent2 = parent2;
        this.child = child;
        this.relationType = relationType;
        this.treeOrder = treeOrder;
    }

    /** default constructor */
    public MultiTree() {

        this.parent1 = "";
        this.parent2 = "";
        this.child = "";
    }

    /** minimal constructor */

    public MultiTree(String parent1, String parent2,  String child) {
        this.parent1 = parent1;
        this.parent2 = parent2;
        this.child = child;
    }

    public String getChild() {
        return this.child;
    }

    public void setChild(String child) {
        this.child = child;
    }
    public java.lang.String getRelationType() {
        return this.relationType;
    }

    public void setRelationType(java.lang.String relationType) {
        this.relationType = relationType;
    }
    public int getTreeOrder() {
        return this.treeOrder;
    }

    public void setTreeOrder(int treeOrder) {
        this.treeOrder = treeOrder;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public boolean equals(Object other) {
        if ( !(other instanceof MultiTree) ) return false;
        MultiTree castOther = (MultiTree) other;
        return new EqualsBuilder()
            .append(this.parent1, castOther.parent1)
            .append(this.parent2, castOther.parent2)
            .append(this.child, castOther.child)
            .isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder()
            .append(parent1)
            .append(parent2)
            .append(child)
            .toHashCode();
    }
	/**
	 * @return Returns the parent1.
	 */
	public String getParent1() {
		return parent1;
	}
	/**
	 * @param parent1 The parent1 to set.
	 */
	public void setParent1(String parent1) {
		this.parent1 = parent1;
	}
	/**
	 * @return Returns the parent2.
	 */
	public String getParent2() {
		return parent2;
	}
	/**
	 * @param parent2 The parent2 to set.
	 */
	public void setParent2(String parent2) {
		this.parent2 = parent2;
	}
}