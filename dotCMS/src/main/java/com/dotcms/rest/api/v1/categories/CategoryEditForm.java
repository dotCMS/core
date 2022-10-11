package com.dotcms.rest.api.v1.categories;

import com.dotcms.rest.api.Validated;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.HashMap;

/**
 * Category Input Form
 *
 * @author Hassan Mustafa Baig
 */
@JsonDeserialize(builder = CategoryEditForm.Builder.class)
public class CategoryEditForm extends Validated {

    private String filter;
    private int page;
    private int perPage;
    private String direction;
    private String orderBy;
    private String siteId;
    private String parentInode;
    private HashMap<String, Integer> categoryData;

    private CategoryEditForm(final Builder builder) {
        this.filter = builder.filter;
        this.page = builder.page;
        this.perPage = builder.perPage;
        this.direction = builder.direction;
        this.orderBy = builder.orderBy;
        this.siteId = builder.siteId;
        this.parentInode = builder.parentInode;
        this.categoryData = builder.categoryData;
    }

    public String getFilter() {
        return filter;
    }

    public int getPage() {
        return page;
    }

    public int getPerPage() {
        return perPage;
    }

    public String getDirection() {
        return direction;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getParentInode() {
        return parentInode;
    }

    public HashMap<String, Integer> getCategoryData() {
        return categoryData;
    }

    public static final class Builder {

        @JsonProperty
        private String filter;
        @JsonProperty
        private int page;
        @JsonProperty
        private int perPage;
        @JsonProperty
        private String direction;
        @JsonProperty
        private String orderBy;
        @JsonProperty
        private String siteId;
        @JsonProperty
        private String inode;
        @JsonProperty
        private int sortOrder;
        @JsonProperty
        private String parentInode;
        @JsonProperty
        private HashMap<String, Integer> categoryData;


        public Builder filter(final String filter) {
            this.filter = filter;
            return this;
        }

        public Builder page(final int page) {
            this.page = page;
            return this;
        }

        public Builder perPage(final int perPage) {
            this.perPage = perPage;
            return this;
        }

        public Builder direction(final String direction) {
            this.direction = direction;
            return this;
        }

        public Builder orderBy(final String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        public Builder siteId(final String siteId) {
            this.siteId = siteId;
            return this;
        }

        public Builder inode(final String inode) {
            this.inode = inode;
            return this;
        }

        public Builder sortOrder(final int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder parentInode(final String parentInode) {
            this.parentInode = parentInode;
            return this;
        }

        public Builder categoryData(final HashMap<String, Integer> categoryData) {
            this.categoryData = categoryData;
            return this;
        }

        public CategoryEditForm build() {

            return new CategoryEditForm(this);
        }
    }
}