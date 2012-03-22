package com.dotmarketing.beans;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.dotmarketing.util.InodeUtils;

/**
 *
 * @author  maria
 */
public class Tree implements Serializable, Comparable {

    private static final long serialVersionUID = 1L;

	/** identifier field */
    private String parent;

    /** identifier field */
    private String child;

    /** nullable persistent field */
    private String relationType;

    /** nullable persistent field */
    private int treeOrder = 0;

    /** full constructor */
    public Tree(String parent, String child, java.lang.String relationType, int treeOrder) {
        this.parent = parent;
        this.child = child;
        this.relationType = relationType;
        this.treeOrder = treeOrder;
    }

    /** default constructor */
    public Tree() {
    }

    /** minimal constructor */
    public Tree(String parent, String child) {
        this.parent = parent;
        this.child = child;
        this.relationType = "child";
    }

    public String getParent() {
    	if(InodeUtils.isSet(this.parent))
    		return this.parent;
    	
    	return "";
    }

    public void setParent(String parent) {
        this.parent = parent;
    }
    public String getChild() {
    	if(InodeUtils.isSet(this.child))
    		return this.child;
    	
    	return "";
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
        if ( !(other instanceof Tree) ) return false;
        Tree castOther = (Tree) other;
        return new EqualsBuilder()
            .append(this.parent, castOther.parent)
            .append(this.child, castOther.child)
            .append(this.relationType, castOther.relationType)
            .append(this.treeOrder, castOther.treeOrder)
            .isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder()
            .append(parent)
            .append(child)
            .append(this.relationType)
            .append(this.treeOrder)
            .toHashCode();
    }

	public int compareTo(Object o) {
		Tree castOther = (Tree) o;
		if(this.equals(castOther) ){
			return 0;
		}else{
			return (this.treeOrder - castOther.treeOrder);
		}
	}
}