package com.dotcms.rest.api.v1.categories;

import java.util.Date;

public class CategoryDTO {

    private String categoryName;
    private String description;
    private String key;
    private String sortOrder;
    private boolean active = true;
    private String keywords;
    private String categoryVelocityVarName;
    private Date modDate;

    private String inode;

    public CategoryDTO(String categoryName,
            String categoryVelocityVarName,
            String key,
            String keywords,
            String sortOrder) {

        this.categoryName = categoryName;
        this.categoryVelocityVarName = categoryVelocityVarName;
        this.key = key;
        this.keywords = keywords;
        this.sortOrder = sortOrder;
    }

    public CategoryDTO(String categoryName,
            String description,
            String key,
            String sortOrder,
            boolean active,
            String keywords,
            String categoryVelocityVarName,
            Date modDate,
            String inode) {

        this.categoryName = categoryName;
        this.description = description;
        this.key = key;
        this.sortOrder = sortOrder;
        this.active = active;
        this.keywords = keywords;
        this.categoryVelocityVarName = categoryVelocityVarName;
        this.modDate = modDate;
        this.inode = inode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getCategoryVelocityVarName() {
        return categoryVelocityVarName;
    }

    public void setCategoryVelocityVarName(String categoryVelocityVarName) {
        this.categoryVelocityVarName = categoryVelocityVarName;
    }

    public Date getModDate() {
        return modDate;
    }

    public void setModDate(Date modDate) {
        this.modDate = modDate;
    }

    public String getInode() {
        return inode;
    }

    public void setInode(String inode) {
        this.inode = inode;
    }
}
