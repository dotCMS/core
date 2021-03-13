package com.dotcms.contenttype.business;

import com.dotcms.util.diff.DiffItem;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class FieldDiffItemsKey {

    private final String fieldVariable;
    private final DiffItem singleDiffItem;
    private final Collection<DiffItem> diffItems;

    public FieldDiffItemsKey(final String fieldVariable, final DiffItem singleDiffItems) {
        this.fieldVariable = fieldVariable;
        this.singleDiffItem = singleDiffItems;
        this.diffItems    = null;
    }

    public FieldDiffItemsKey(final String fieldVariable, final Collection<DiffItem> diffItems) {
        this.fieldVariable = fieldVariable;
        this.diffItems = diffItems;
        this.singleDiffItem = null;
    }

    public boolean isSingleDiff () {

        return null != this.singleDiffItem && null == this.diffItems;
    }

    public String getFieldVariable() {
        return fieldVariable;
    }

    public DiffItem getFirstDiff () {

        return this.isSingleDiff()? this.singleDiffItem : this.getDiffItems(0);
    }

    private DiffItem getDiffItems (final int index) {

        DiffItem diffItem = null;

        if (null != this.diffItems && index < this.diffItems.size()) {

            diffItem = this.diffItems instanceof List?
                    (DiffItem) List.class.cast(this.diffItems).get(index):
                    this.diffItems.stream().iterator().next();
        }

        return diffItem;
    }

    public Collection<DiffItem> getDiffItems () {

        return diffItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldDiffItemsKey that = (FieldDiffItemsKey) o;
        return Objects.equals(fieldVariable, that.fieldVariable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldVariable);
    }
}
