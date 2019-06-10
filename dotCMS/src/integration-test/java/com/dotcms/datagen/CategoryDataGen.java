package com.dotcms.datagen;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;

public class CategoryDataGen extends AbstractDataGen<Category> {

    private final long currentTime = System.currentTimeMillis();

    private String categoryName = "CategoryName" + currentTime;
    private String key = "CategoryKey" + currentTime;
    private String velocityVarName = "VelocityVarName" + currentTime;
    private int orderNumber = 1;
    private String keywords;

    /**
     *
     * @param categoryName
     * @return
     */
    public CategoryDataGen setCategoryName(final String categoryName) {
        this.categoryName = categoryName;
        return this;
    }

    /**
     *
     * @param key
     * @return
     */
    public CategoryDataGen setKey(final String key) {
        this.key = key;
        return this;
    }

    /**
     *
     * @param velocityVarName
     * @return
     */
    public CategoryDataGen setCategoryVelocityVarName(final String velocityVarName) {
        this.velocityVarName = velocityVarName;
        return this;
    }

    /**
     *
     * @param orderNumber
     * @return
     */
    public CategoryDataGen setSortOrder(final int orderNumber) {
        this.orderNumber = orderNumber;
        return this;
    }

    /**
     *
     * @param keywords
     * @return
     */
    public CategoryDataGen setKeywords(final String keywords) {
        this.keywords = keywords;
        return this;
    }

    /**
     *
     * @return
     */
    @Override
    public Category next() {

        final Category category = new Category();
        category.setCategoryName(this.categoryName);
        category.setKey(this.key);
        category.setCategoryVelocityVarName(this.velocityVarName);
        category.setSortOrder(this.orderNumber);
        category.setKeywords(this.keywords);
        
        return category;
    }

    @Override
    public Category nextPersisted() {
        return persist(next());
    }


    @Override
    public Category persist(final Category object) {
        try {
            APILocator.getCategoryAPI().save(null, object, APILocator.systemUser(), false);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException("Error persisting Category", e);
        }
        return object;
    }

}
