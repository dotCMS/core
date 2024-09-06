package com.dotmarketing.portlets.categories.business;

import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.common.util.SQLUtil;
import com.dotmarketing.portlets.categories.model.Category;

import java.util.Objects;

/**
 * Represents Search Criteria for {@link Category} searching, you cans set the follow:
 *
 * - filter: Value used to filter the Category by, returning only Categories that contain this value in their key, name, or variable name.
 * - inode: Entry point on the Category tree to start the searching.
 * - orderBy: Field name to order the Category
 * - direction: Order by direction, it can be 'ASC' or 'DESC'
 * - rootInode:  If the root inode is set, the search will be conducted only among the children of this category.
 * - parentList: If this is true, the parentList is calculated, which means a collection is created containing
 * the categories from the direct parent and each parent recursively, all the way up to the top-level category.
 * - countChildren: If set to true, the number of children for each {@link Category} will be counted,
 * and an additional attribute childrenCount will be returned. If set to false, the childrenCount attribute will always be 0.
 *
 * Otherwise, the search will include only the top-level categories.
 */
public class CategorySearchCriteria {
    final boolean searchAllLevels;
    final String rootInode;
    final String filter;
    final String orderBy;
    final OrderDirection direction;
    final int limit;
    final int offset;
    private boolean parentList;
    private boolean countChildren;
    private CategorySearchCriteria (final Builder builder) {
        this.rootInode = builder.rootInode;
        this.filter = builder.filter;
        this.orderBy = builder.orderBy;
        this.direction = builder.direction;
        this.limit = builder.limit;
        this.offset = builder.offset;
        this.searchAllLevels = builder.searchAllLevels;
        this.parentList = builder.parentList;
        this.countChildren = builder.countChildren;
    }

    public boolean isCountChildren() {
        return countChildren;
    }

    public String getRootInode() {
        return rootInode;
    }

    public String getFilter() {
        return filter;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public OrderDirection getDirection() {
        return direction;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public boolean isParentList() {
        return parentList;
    }

    @Override
    public String toString() {
        return "CategorySearchCriteria{" +
                "searchAllLevels=" + searchAllLevels +
                ", rootInode='" + rootInode + '\'' +
                ", filter='" + filter + '\'' +
                ", orderBy='" + orderBy + '\'' +
                ", direction=" + direction +
                ", limit=" + limit +
                ", offset=" + offset +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategorySearchCriteria that = (CategorySearchCriteria) o;
        return searchAllLevels == that.searchAllLevels && limit == that.limit && offset == that.offset && parentList == that.parentList && countChildren == that.countChildren && Objects.equals(rootInode, that.rootInode) && Objects.equals(filter, that.filter) && Objects.equals(orderBy, that.orderBy) && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(searchAllLevels, rootInode, filter, orderBy, direction, limit, offset, parentList, countChildren);
    }

    public static class Builder {
        private boolean searchAllLevels = false;
        private String rootInode;
        private String filter;
        private String orderBy = "category_name";
        private OrderDirection direction = OrderDirection.ASC;
        private int limit = -1;
        private int offset = 0;

        private boolean parentList;
        private boolean countChildren;

        public Builder rootInode(String rootInode) {
            this.rootInode = rootInode;
            return this;
        }

        public Builder filter(String filter) {
            this.filter = filter;
            return this;
        }

        public Builder orderBy(String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        public Builder direction(OrderDirection direction) {
            this.direction = direction;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public Builder searchAllLevels(final boolean searchAllLevels) {
            this.searchAllLevels = searchAllLevels;
            return this;
        }

        public CategorySearchCriteria build() {
            if (this.orderBy != null) {
                final String categoriesSort = SQLUtil.sanitizeSortBy(this.direction == OrderDirection.DESC
                        ? "-" + this.orderBy : this.orderBy);

                this.direction = categoriesSort.startsWith("-") ? OrderDirection.DESC : OrderDirection.ASC;
                this.orderBy = categoriesSort.startsWith("-") ? categoriesSort.substring(1) : categoriesSort;
            }

            return new CategorySearchCriteria(this);
        }

        public Builder parentList(boolean parentList) {
            this.parentList = parentList;
            return this;
        }

        public Builder setCountChildren(boolean countChildren) {
            this.countChildren = countChildren;
            return this;
        }
    }
}
