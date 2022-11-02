package com.dotcms.rest.api.v1.categories;

import java.util.Date;

/**
 * Category import data transfer object
 *
 * @author Hassan Mustafa Baig
 */
public class CategoryDTO {

    private final String categoryName;
    private final String key;
    private final String sortOrder;
    private final String keywords;
    private final String categoryVelocityVarName;

    public CategoryDTO(final String categoryName,
            final String categoryVelocityVarName,
            final String key,
            final String keywords,
            final String sortOrder) {

        this.categoryName = categoryName;
        this.categoryVelocityVarName = categoryVelocityVarName;
        this.key = key;
        this.keywords = keywords;
        this.sortOrder = sortOrder;
    }


    public String getCategoryName() {
        return categoryName;
    }

    public String getKey() {
        return key;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getCategoryVelocityVarName() {
        return categoryVelocityVarName;
    }
}
