package com.dotmarketing.factories;

import java.io.Serializable;
import java.util.Objects;

/**
 * Encapsulates a contentlet identifier + personalization for this content
 * It has been created to keep both values on the Table for the @{@link MultiTreeAPI#getPageMultiTrees}
 * @see com.dotmarketing.beans.MultiTree
 * @see com.google.common.collect.Table
 * @author jsanca
 */
public class PersonalizedContentlet implements Serializable {

    private final String contentletId;
    private final String personalization;

    public PersonalizedContentlet(final String contentletId, final String personalization) {
        this.contentletId    = contentletId;
        this.personalization = personalization;
    }

    public String getContentletId() {
        return contentletId;
    }

    public String getPersonalization() {
        return personalization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonalizedContentlet that = (PersonalizedContentlet) o;
        return Objects.equals(contentletId, that.contentletId) &&
                Objects.equals(personalization, that.personalization);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentletId, personalization);
    }
}
