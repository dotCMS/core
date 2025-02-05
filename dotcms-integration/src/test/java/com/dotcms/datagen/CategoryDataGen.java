package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.exception.ExceptionUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Logger;
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
    private Category parent = null;

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

    public CategoryDataGen parent(final Category parent) {
        this.parent = parent;
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
             categoryAPI.save(parent, object, APILocator.systemUser(), false);
             if(UtilMethods.isSet(children)){
                for(final Category category : children){
                   categoryAPI.save(object, category, APILocator.systemUser(), false);
                }
             }

            return APILocator.getCategoryAPI().findByKey(object.getKey(), APILocator.systemUser(), false);
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException("Error persisting Category", e);
        }
    }

    /**
     * Deletes the given {@link Category} object.
     *
     * @param category the category to delete
     */
    public static void delete(final Category category) {
        delete(category, true);
    }

    /**
     * Deletes the given {@link Category} object. If {@code failSilently} is {@code true}, the
     * method will log the error message. Otherwise, it will throw a {@link DotRuntimeException}.
     *
     * @param category     the category to delete
     * @param failSilently whether to fail silently
     */
    public static void delete(final Category category, final boolean failSilently) {
        final CategoryAPI categoryAPI = APILocator.getCategoryAPI();
        try {
            categoryAPI.delete(category, APILocator.systemUser(), false);
        } catch (final DotDataException | DotSecurityException e) {
            final String errorMsg = String.format("Error deleting Category " +
                    "'%s': %s", category, ExceptionUtil.getErrorMessage(e));
            if (failSilently) {
                Logger.error(ContentTypeDataGen.class, errorMsg, e);
            } else {
                throw new DotRuntimeException(errorMsg, e);
            }
        }
    }

}