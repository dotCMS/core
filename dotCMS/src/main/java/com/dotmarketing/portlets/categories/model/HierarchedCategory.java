package com.dotmarketing.portlets.categories.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents a {@link Category} with its hierarchy calculated.
 * This means traversing from the current category all the way up to the first-level category that you encounter.
 *
 * For example:
 *
 * | Name        | Parent         | hierarchy               |
 * |-------------|----------------|------------------------ |
 * | Top Category| null           | []                      |
 * | Child       | Top Category   | [Top Category]          |
 * | Grand Child | Child          | [Top Category, Child]   |
 *
 */
public class HierarchedCategory extends Category{

    private List<ShortCategory> parentList;

    public void setParentList(final List<ShortCategory> parentList) {
        this.parentList = parentList;
    }

    public List<ShortCategory> getParentList() {
        return parentList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        HierarchedCategory that = (HierarchedCategory) o;
        return Objects.equals(parentList, that.parentList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), parentList);
    }
}
