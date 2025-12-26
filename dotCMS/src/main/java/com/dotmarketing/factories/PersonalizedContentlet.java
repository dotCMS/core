package com.dotmarketing.factories;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Encapsulates a contentlet identifier + personalization for this content
 * It has been created to keep both values on the Table for the @{@link MultiTreeAPI#getPageMultiTrees}
 * @see com.dotmarketing.beans.MultiTree
 * @see com.google.common.collect.Table
 * @author jsanca
 */
public class PersonalizedContentlet implements Serializable{

    private final String contentletId;
    private final String personalization;
    private final int treeOrder;
    private final Map<String, Object> styleProperties;

    public PersonalizedContentlet(final String contentletId, final String personalization, final int treeOrder, final Map<String, Object> styleProperties) {
        this.contentletId    = contentletId;
        this.personalization = personalization;
        this.treeOrder = treeOrder;
        this.styleProperties = styleProperties;
    }

    public PersonalizedContentlet(final String contentletId, final String personalization) {
        this.contentletId    = contentletId;
        this.personalization = personalization;
        this.treeOrder = 0;
        this.styleProperties = null;
    }

    public String getContentletId() {
        return contentletId;
    }

    public String getPersonalization() {
        return personalization;
    }

    public Object getTreeOrder() {
        return treeOrder;
    }

    public Map<String, Object> getStyleProperties() {
        return styleProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonalizedContentlet that = (PersonalizedContentlet) o;
        return Objects.equals(contentletId, that.contentletId)
                && Objects.equals(personalization, that.personalization)
                && Objects.equals(styleProperties, that.styleProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentletId, personalization, styleProperties);
    }
}
