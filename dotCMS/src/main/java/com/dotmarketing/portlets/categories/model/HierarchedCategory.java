package com.dotmarketing.portlets.categories.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.*;

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

    private final String PARENT_LIST_EXTRA_PARAMETER = "parentList";
    private final String CHILDREN_COUNT_EXTRA_PARAMETER = "childrenCount";

    @JsonAnyGetter
    final Map<String, Object> extraParameters = new HashMap<>();

    public void setParentList(final List<ShortCategory> parentList) {
        extraParameters.put(PARENT_LIST_EXTRA_PARAMETER, parentList);
    }

    @JsonIgnore
    public List<ShortCategory> getParentList() {

        return extraParameters.containsKey(PARENT_LIST_EXTRA_PARAMETER) ?
                (List<ShortCategory>) extraParameters.get(PARENT_LIST_EXTRA_PARAMETER) : null;
    }

    public void setChildrenCount(int childrenCount) {
        extraParameters.put(CHILDREN_COUNT_EXTRA_PARAMETER, childrenCount);
    }

    @JsonIgnore
    public int getChildrenCount() {

        return extraParameters.containsKey(CHILDREN_COUNT_EXTRA_PARAMETER) ?
                (int) extraParameters.get(CHILDREN_COUNT_EXTRA_PARAMETER) : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        HierarchedCategory that = (HierarchedCategory) o;
        return Objects.equals(getParentList(), that.getParentList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getParentList());
    }
}
