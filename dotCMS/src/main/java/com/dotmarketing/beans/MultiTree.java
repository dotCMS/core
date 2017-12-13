package com.dotmarketing.beans;

import com.dotcms.repackage.org.apache.commons.lang.builder.EqualsBuilder;
import com.dotcms.repackage.org.apache.commons.lang.builder.HashCodeBuilder;
import com.dotcms.repackage.org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;

/**
 *
 * @author maria
 */
public class MultiTree implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String LEGACY_RELATION_TYPE="LEGACY_RELATION_TYPE";
    
    /** identifier field */
    private String parent1;

    /** identifier field */
    private  String parent2;

    /** identifier field */
    private  String child;

    /** nullable persistent field */
    private  String relationType;

    /** nullable persistent field */
    private  int treeOrder;

    /** full constructor */
    public MultiTree(String htmlPage, String container, String child, String relationType, int treeOrder) {
        this.parent1 = htmlPage;
        this.parent2 = container;
        this.child = child;
        this.relationType = (relationType==null) ? LEGACY_RELATION_TYPE : relationType;
        this.treeOrder =  (treeOrder<0)? 0  : treeOrder;
    }

    /** default constructor */
    public MultiTree() {
        this(null, null, null, LEGACY_RELATION_TYPE,0);
    }

    /** minimal constructor */

    public MultiTree(String htmlPage, String container, String child) {
        this(htmlPage, container, child, LEGACY_RELATION_TYPE,0);
    }
    /**
     * 
     * {@link #getContentlet()}
     */
    @Deprecated
    public String getChild() {
        return  getContentlet();
    }

    public String getContentlet() {
        return this.child;
    }


    @Deprecated
    public MultiTree setChild(String child) {
        return setContentlet(child);
    }

    public java.lang.String getRelationType() {
        return this.relationType;
    }
    public MultiTree setContentlet(String contentlet) {
        this.child = contentlet;
        return new MultiTree(this.parent1, this.parent2, contentlet, this.relationType, this.treeOrder);
    }

    public MultiTree setRelationType(java.lang.String relationType) {
        this.relationType = (relationType==null) ? LEGACY_RELATION_TYPE : relationType;
        return new MultiTree(this.parent1, this.parent2, this.child, relationType, this.treeOrder);
    }

    public int getTreeOrder() {
        return this.treeOrder;
    }

    public MultiTree setTreeOrder(int treeOrder) {
        treeOrder = (treeOrder<0)? 0  : treeOrder;
        this.treeOrder = treeOrder;
        return new MultiTree(this.parent1, this.parent2, this.child, this.relationType, treeOrder);
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public boolean equals(Object other) {
        if (!(other instanceof MultiTree))
            return false;
        MultiTree castOther = (MultiTree) other;
        return new EqualsBuilder().append(this.parent1, castOther.parent1).append(this.parent2, castOther.parent2)
                .append(this.child, castOther.child).isEquals();
    }

    public int hashCode() {
        return new HashCodeBuilder().append(parent1).append(parent2).append(child).toHashCode();
    }

    /**
     * 
     * {@link #getHtmlPage()}
     */
    @Deprecated
    public String getParent1() {
        return parent1;
    }
    /**
     * 
     * {@link #getContainer()}
     */
    @Deprecated
    public String getParent2() {
        return parent2;
    }

    /**
     * @return Returns the htmlPage.
     */
    public String getHtmlPage() {
        return parent1;
    }
    /**
     * 
     * {@link #setHtmlPage(String)}
     */
    @Deprecated
    public MultiTree setParent1(String htmlPage) {
        return setHtmlPage(htmlPage);
    }
    /**
     * 
     * {@link #setContainer(String)}
     */
    @Deprecated
    public MultiTree setParent2(String container) {
        return setContainer(container);
    }

    /**
     * @param htmlPage The htmlPage to set.
     */
    public MultiTree setHtmlPage(String htmlPage) {
        return new MultiTree(htmlPage, this.parent2, this.child, this.relationType, this.treeOrder);
    }

    /**
     * @return Returns the container.
     */
    public String getContainer() {
        return parent2;
    }

    /**
     * @param container The container to set.
     */
    public MultiTree setContainer(String container) {
        this.parent2 = container;
        return new MultiTree(this.parent1, container, this.child, this.relationType, this.treeOrder);
    }



}
