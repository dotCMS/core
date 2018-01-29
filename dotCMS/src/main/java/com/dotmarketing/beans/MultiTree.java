package com.dotmarketing.beans;


import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author maria
 */
public class MultiTree implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String LEGACY_RELATION_TYPE = "LEGACY_RELATION_TYPE";

    /** identifier field */
    private String parent1;

    /** identifier field */
    private String parent2;

    /** identifier field */
    private String child;

    /** nullable persistent field */
    private String relationType = LEGACY_RELATION_TYPE;

    /** nullable persistent field */
    private int treeOrder;

    /** full constructor */
    public MultiTree(String htmlPage, String container, String child, String relationType, int treeOrder) {
        this.parent1 = htmlPage;
        this.parent2 = container;
        this.child = child;
        this.relationType = (relationType == null) ? LEGACY_RELATION_TYPE : relationType;
        this.treeOrder = (treeOrder < 0) ? 0 : treeOrder;
    }

    /** default constructor */
    public MultiTree() {
        this(null, null, null, LEGACY_RELATION_TYPE, 0);
    }
    

    private MultiTree(MultiTree tree) {
        this(tree.parent1, tree.parent2, tree.child, tree.relationType, tree.treeOrder);
    }
    
    /** minimal constructor */
    public MultiTree(String htmlPage, String container, String child) {
        this(htmlPage, container, child, LEGACY_RELATION_TYPE, 0);
    }

    /**
     * @deprecated
     * {@link #getContentlet()}
     */
    @Deprecated
    public String getChild() {
        return getContentlet();
    }

    /**
     * returns the contentlet Id of the multitree
     * @return
     */
    public String getContentlet() {
        return this.child;
    }

    /**
     * @deprecated 
     * {@link #setContentlet(String)}
     */
    @Deprecated
    public MultiTree setChild(String child) {
        return setContentlet(child);
    }

    public java.lang.String getRelationType() {
        return this.relationType;
    }

    public MultiTree setContentlet(final String contentlet) {
        this.child = contentlet;
        return new MultiTree(this);
    }
    public MultiTree setContentlet(final Contentlet contentlet) {
        return this.setContentlet(contentlet.getIdentifier());
    }
    public MultiTree setRelationType(java.lang.String relationType) {
        this.relationType = (relationType == null) ? LEGACY_RELATION_TYPE : relationType;
        return new MultiTree(this);
    }

    public int getTreeOrder() {
        return this.treeOrder;
    }

    public MultiTree setTreeOrder(int treeOrder) {
        treeOrder = (treeOrder < 0) ? 0 : treeOrder;
        this.treeOrder = treeOrder;
        return new MultiTree(this);
    }
    public MultiTree setOrder(final int treeOrder) {
        return this.setTreeOrder(treeOrder);
    }
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public boolean equals(Object other) {
        if (!(other instanceof MultiTree))
            return false;
        MultiTree castOther = (MultiTree) other;
        return new EqualsBuilder().append(this.parent1, castOther.parent1)
            .append(this.parent2, castOther.parent2)
            .append(this.child, castOther.child)
            .append(this.relationType, castOther.relationType)
            .isEquals();
    }
    
    public boolean equalsWithOrder(Object other) {
        if (!(other instanceof MultiTree))
            return false;
        MultiTree castOther = (MultiTree) other;
        return new EqualsBuilder().append(this.parent1, castOther.parent1)
            .append(this.parent2, castOther.parent2)
            .append(this.child, castOther.child)
            .append(this.relationType, castOther.relationType)
            .append(this.treeOrder, castOther.treeOrder)
            .isEquals();
    }
    public int hashCode() {
        return new HashCodeBuilder().append(parent1)
            .append(parent2)
            .append(child)
            .toHashCode();
    }

    /**
     * @deprecated 
     * {@link #getHtmlPage()}
     */
    @Deprecated
    public String getParent1() {
        return parent1;
    }

    /**
     * @deprecated
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
     * @deprecated
     * {@link #setHtmlPage(String)}
     */
    @Deprecated
    public MultiTree setParent1(String htmlPage) {
        return setHtmlPage(htmlPage);
    }

    /**
     * @deprecated
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
        this.parent1 = htmlPage;
        return new MultiTree(this);
    }
    public MultiTree setHtmlPage(final HTMLPageAsset page) {
        return this.setHtmlPage(page.getIdentifier());
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
    public MultiTree setContainer(final String container) {
        this.parent2 = container;
        return new MultiTree(this);
    }
    /**
     * @param container The container to set.
     */
    public MultiTree setContainer(final Container container) {
        return this.setContainer(container.getIdentifier());
    }


}
