package com.dotmarketing.beans;


import com.dotcms.variant.VariantAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.util.UtilMethods;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;

/**
 *
 * @author maria
 */
public class MultiTree implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String LEGACY_RELATION_TYPE = "LEGACY_RELATION_TYPE";
    public static final String LEGACY_INSTANCE_ID = LEGACY_RELATION_TYPE;
    public static final String DOT_PERSONALIZATION_DEFAULT = "dot:default";

    /** identifier field for pages */
    private String parent1;

    /** identifier field for container */
    private String parent2;

    /** identifier field for content */
    private String child;

    /** nullable persistent field */
    private String relationType = LEGACY_RELATION_TYPE;

    /** nullable persistent field */
    private int treeOrder;

    private String personalization;

    private String variantId;

    private Map<String, Object> styleProperties;

    /**
     * @param htmlPage page's id
     * @param container container's id
     * @param child contentlet's id
     * @param instanceId container UI ID
     * @param treeOrder order to be shown into the page
     * @param personalization persona's tag
     * @param variantId variant's name
     * @param styleProperties JSON properties for styling
     */
    /** full constructor */
    public MultiTree(final String htmlPage,
                     final String container,
                     final String child,
                     final String instanceId,
                     final int treeOrder,
                     final String personalization,
                     final String variantId,
                     final Map<String, Object> styleProperties) {

        this.parent1      = htmlPage;
        this.parent2      = container;
        this.child        = child;
        this.relationType = (instanceId == null) ? LEGACY_INSTANCE_ID : instanceId;
        this.treeOrder    = Math.max(treeOrder, 0);
        this.personalization = personalization;
        this.variantId = variantId;
        this.styleProperties = UtilMethods.isSet(styleProperties) ? Map.copyOf(styleProperties) : null;
    }

    /** full constructor */
    public MultiTree(final String htmlPage,
                     final String container,
                     final String child,
                     final String instanceId,
                     final int treeOrder) {

        this(htmlPage, container, child, instanceId, treeOrder, DOT_PERSONALIZATION_DEFAULT,
                VariantAPI.DEFAULT_VARIANT.name(), null);
    }

    public MultiTree(final String htmlPage,
            final String container,
            final String child,
            final String instanceId,
            final int treeOrder,
            final String personalization) {

        this (htmlPage, container, child, instanceId, treeOrder, personalization, VariantAPI.DEFAULT_VARIANT.name(), null);
    }

    /** default constructor */
    public MultiTree() {
        this(null, null, null, LEGACY_INSTANCE_ID, 0);
    }

    /** minimal constructor */
    public MultiTree(String htmlPage, String container, String child) {
        this(htmlPage, container, child, LEGACY_INSTANCE_ID, 0);
    }

    private MultiTree(MultiTree tree) {
        this(tree.parent1, tree.parent2, tree.child, tree.relationType, tree.treeOrder, tree.getPersonalization(), tree.getVariantId(), tree.getStyleProperties());
    }

    public static MultiTree buildMultitreeWithVariant(final MultiTree multiTree, final String newVariant) {
        final MultiTree newMultiTree = new MultiTree(multiTree);
        newMultiTree.setVariantId(newVariant);
        return newMultiTree;
    }

    public static MultiTree buildMultitree(final MultiTree multiTree,
            final String newVariant, final String newPersonalization) {
        MultiTree newMultiTree = buildMultitreeWithVariant(multiTree, newVariant);
        newMultiTree.setPersonalization(newPersonalization);

        return newMultiTree;
    }

    public String getVariantId() {
        return variantId != null ? variantId : VariantAPI.DEFAULT_VARIANT.name();
    }

    public MultiTree setVariantId(final String variantId) {
        this.variantId = variantId;
        return new MultiTree(this);
    }

    public Map<String, Object> getStyleProperties() {
        return styleProperties;
    }

    public MultiTree setStyleProperties(final Map<String, Object> styleProperties) {
        this.styleProperties = styleProperties;
        return new MultiTree(this);
    }

    /**
     * Personalized an existing multitree with a new personalization
     * @param multiTree {@link MultiTree}
     * @param personalization {@link String}
     * @return MultiTree
     */
    public static MultiTree personalized (final MultiTree multiTree, final String personalization) {

        final MultiTree newMultiTree = new MultiTree(multiTree);

        newMultiTree.setPersonalization(personalization);

        return newMultiTree;
    }

    /**
     * The {@link #getContainer()} could be a path or uuid, this method will get always the id no matter what is in {@link #getContainer()}
     * @return String
     */
    @JsonIgnore
    public String getContainerAsID () {

        final String containerId = this.getContainer();

        if (FileAssetContainerUtil.getInstance().isFolderAssetContainerId(containerId)) {

            try {
                return FileAssetContainerUtil.getInstance().getContainerIdFromPath(containerId);
            } catch (DotDataException e) {
                /** quiet */
            }
        }

        return containerId;
    }
    
    public String getPersonalization() {
        return personalization;
    }

    public void setPersonalization(String personalization) {
        this.personalization = personalization;
    }

    /**
     * @deprecated
     * {@link #getContentlet()}
     */
    @JsonIgnore
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

    public String getRelationType() {
        return this.relationType;
    }

    public MultiTree setContentlet(final String contentlet) {
        this.child = contentlet;
        return new MultiTree(this);
    }
    public MultiTree setContentlet(final Contentlet contentlet) {
        return this.setContentlet(contentlet.getIdentifier());
    }
    
    @Deprecated
    public MultiTree setRelationType(String relationType) {
        return setInstanceId(relationType);
    }
    
    public MultiTree setInstanceId(String relationType) {
        this.relationType = (relationType == null) ? LEGACY_INSTANCE_ID : relationType;
        return new MultiTree(this);
    }

    public int getTreeOrder() {
        return this.treeOrder;
    }

    public MultiTree setTreeOrder(int treeOrder) {
        treeOrder = Math.max(treeOrder, 0);
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
            .append(this.personalization, castOther.personalization)
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
            .append(this.personalization, castOther.personalization)
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
    @JsonIgnore
    @Deprecated
    public String getParent1() {
        return parent1;
    }

    /**
     * @deprecated
     * {@link #getContainer()}
     */
    @JsonIgnore
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
