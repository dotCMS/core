package com.dotcms.rest.api.v1.categories;

import java.util.Date;

/**
 * Category listing data transfer object
 *
 * @author Hassan Mustafa Baig
 */

public class CategoryListDTO {

    private final String categoryName;
    private final String key;
    private final Integer sortOrder;
    private final String keywords;
    private final String categoryVelocityVarName;
    private final String description;

    private final boolean active;
    private final java.util.Date modDate;
    private final java.util.Date iDate;
    private final String type;
    private final String owner;
    private final String inode;
    private final String identifier;

    private final Integer childrenCount;

    public CategoryListDTO(final String categoryName,
            final String categoryVelocityVarName,
            final String key,
            final String keywords,
            final Integer sortOrder,
            final String description,
            final boolean active,
            final  java.util.Date modDate,
            final  java.util.Date iDate,
            final String type,
            final String owner,
            final String inode,
            final String identifier,
            final Integer childrenCount) {

        this.categoryName = categoryName;
        this.categoryVelocityVarName = categoryVelocityVarName;
        this.key = key;
        this.keywords = keywords;
        this.sortOrder = sortOrder;
        this.description = description;
        this.active = active;
        this.modDate = modDate;
        this.iDate = iDate;
        this.type = type;
        this.owner = owner;
        this.inode = inode;
        this.identifier = identifier;
        this.childrenCount = childrenCount;
    }


    public String getCategoryName() {
        return categoryName;
    }

    public String getKey() {
        return key;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getCategoryVelocityVarName() {
        return categoryVelocityVarName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public Date getModDate() {
        return modDate;
    }

    public Date getiDate() {
        return iDate;
    }

    public String getType() {
        return type;
    }

    public String getOwner() {
        return owner;
    }

    public String getInode() {
        return inode;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Integer getChildrenCount() {
        return childrenCount;
    }
}
