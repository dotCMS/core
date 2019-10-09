package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CategoryDataGen extends AbstractDataGen<Category> {

    private final long currentTime = System.currentTimeMillis();

    private String categoryName = "CategoryName" + currentTime;
    private String key = "CategoryKey" + currentTime;
    private String velocityVarName = "VelocityVarName" + currentTime;
    private int orderNumber = 1;
    private String keywords;
    private List<Category> children = new ArrayList<>();

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

    public CategoryDataGen children(Category... categories) {
        this.children.addAll(Arrays.asList(categories));
        return this;
    }

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

    @WrapInTransaction
    @Override
    public Category persist(final Category object) {
        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        try {
             categoryAPI.save(null, object, APILocator.systemUser(), false);
             if(UtilMethods.isSet(children)){
                for(final Category category : children){
                   categoryAPI.save(object, category, APILocator.systemUser(), false);
                }
             }

        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException("Error persisting Category", e);
        }
        return object;
    }

}