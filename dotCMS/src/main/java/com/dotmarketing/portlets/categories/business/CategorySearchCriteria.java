package com.dotmarketing.portlets.categories.business;

import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.portlets.categories.model.Category;

/**
 * Represents Search Criteria for {@link Category} searching, you cans set the follow:
 *
 * - filter: Value used to filter the Category by, returning only Categories that contain this value in their key, name, or variable name.
 * - inode: Entry point on the Category tree to start the searching.
 * - orderBy: Field name to order the Category
 * - direction: Order by direction, it can be 'ASC' or 'DESC'
 * - rootInode:  If the root inode is set, the search will be conducted only among the children of this category.
 * Otherwise, the search will include only the top-level categories.
 */
public class CategorySearchCriteria {
    final String rootInode;
    final String filter;
    final String orderBy;
    final OrderDirection direction;
    final int limit;
    final int offset;
    private CategorySearchCriteria (final Builder builder) {
        this.rootInode = builder.rootInode;
        this.filter = builder.filter;
        this.orderBy = builder.orderBy;
        this.direction = builder.direction;
        this.limit = builder.limit;
        this.offset = builder.offset;
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

    public static class Builder {
        private String rootInode;
        private String filter;
        private String orderBy = "category_name";
        private OrderDirection direction = OrderDirection.ASC;
        private int limit = -1;
        private int offset = 0;

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


        public CategorySearchCriteria build() {
            return new CategorySearchCriteria(this);
        }
    }
}
