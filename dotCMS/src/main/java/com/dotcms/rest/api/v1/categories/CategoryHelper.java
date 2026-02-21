package com.dotcms.rest.api.v1.categories;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.BulkResultView;
import com.dotcms.util.pagination.OrderDirection;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.StringUtils;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * Helper for categories
 *
 * @author Hassan Mustafa Baig
 */
public class CategoryHelper {

    private final CategoryAPI categoryAPI;

    public CategoryHelper() {
        this(APILocator.getCategoryAPI());
    }

    @VisibleForTesting
    public CategoryHelper(final CategoryAPI categoryAPI) {

        this.categoryAPI = categoryAPI;
    }

    public Host getHost(final String hostId, final Supplier<Host> hostSupplier) {

        if (UtilMethods.isSet(hostId)) {

            return Try.of(
                            () -> APILocator.getHostAPI().find(hostId, APILocator.systemUser(), false))
                    .getOrElse(hostSupplier);
        }

        return hostSupplier.get();
    }

    public CategoryView toCategoryView(final Category category, final User user) throws DotDataException, DotSecurityException {

       return new CategoryView.Builder()
                .inode(category.getInode())
                .description(category.getDescription())
                .keywords(category.getKeywords())
                .key(category.getKey())
                .categoryName(category.getCategoryName())
                .active(category.isActive())
                .sortOrder(category.getSortOrder())
                .categoryVelocityVarName(category.getCategoryVelocityVarName())
                .build();
    }

    public CategoryWithChildCountView toCategoryWithChildCountView(final Category category, final User user) throws DotDataException, DotSecurityException {

        final Integer childrenCategoriesCount = this.categoryAPI.findChildren(user, category.getInode(),
                    false, StringPool.BLANK).size();

       return new CategoryWithChildCountView.Builder()
                .inode(category.getInode())
                .description(category.getDescription())
                .keywords(category.getKeywords())
                .key(category.getKey())
                .categoryName(category.getCategoryName())
                .active(category.isActive())
                .sortOrder(category.getSortOrder())
                .categoryVelocityVarName(category.getCategoryVelocityVarName())
                .childrenCount(childrenCategoriesCount)
                .build();
    }

    public long addOrUpdateCategory(final User user, final String contextInode,
            final BufferedReader bufferedReader, final Boolean merge)
            throws IOException, Exception {

        long successCount = 0;
        for (final CategoryDTO categoryDTO : CategoryImporter.from(bufferedReader)) {
            if (addOrUpdateCategory(user, true, contextInode, categoryDTO.getCategoryName(),
                    categoryDTO.getCategoryVelocityVarName(), categoryDTO.getKey(), null, categoryDTO.getSortOrder(),
                    merge)) {
                successCount++;
            }
        }
        return successCount;
    }

    private boolean addOrUpdateCategory(final User user, final Boolean isSave, final String inode,
            final String name, final String var, final String key, final String keywords,
            final String sort, final boolean isMerge)
            throws Exception {

        Category parent = null;
        Category category = new Category();
        category.setCategoryName(name);
        category.setKey(key);
        category.setCategoryVelocityVarName(var);
        category.setSortOrder(sort);
        category.setKeywords(keywords);

        if (UtilMethods.isSet(inode)) {
            if (!isSave) {//edit
                category.setInode(inode);
                final Category finalCat = category;//this is to be able to use the try.of
                parent = Try.of(() -> categoryAPI.getParents(finalCat, user, false).get(0))
                        .getOrNull();
            } else {//save
                parent = categoryAPI.find(inode, user, false);
            }
        }

        setVelocityVarName(category, var, name);

        if (isMerge) { // add/edit

            if (isSave) { // Importing
                if (UtilMethods.isSet(key)) {
                    category = categoryAPI.findByKey(key, user, false);
                    if (category == null) {
                        category = new Category();
                        category.setKey(key);
                    }

                    category.setCategoryName(name);
                    setVelocityVarName(category, var, name);
                    category.setSortOrder(sort);
                }
            } else { // Editing
                category = categoryAPI.find(inode, user, false);
                category.setCategoryName(name);
                setVelocityVarName(category, var, name);
                category.setKeywords(keywords);
                category.setKey(key);
            }

        } else { // replace
            category.setCategoryName(name);
            setVelocityVarName(category, var, name);
            category.setSortOrder(sort);
            category.setKey(key);
        }

        try {
            categoryAPI.save(parent, category, user, false);
            return true;
        } catch (DotSecurityException e) {
            Logger.error(this,
                    "Error trying to save/update the category " + category.getInode(), e);
            return false;
        }
    }

    private void setVelocityVarName(Category cat, String catvelvar, String catName)
            throws DotDataException, DotSecurityException {
        Boolean Proceed = false;
        if (!UtilMethods.isSet(catvelvar)) {
            catvelvar = StringUtils.camelCaseLower(catName);
            Proceed = true;
        }
        if (!InodeUtils.isSet(cat.getInode()) || Proceed) {
            if (VelocityUtil.isNotAllowedVelocityVariableName(catvelvar)) {
                catvelvar = catvelvar + "Field";
            }
            catvelvar = categoryAPI.suggestVelocityVarName(catvelvar);
            cat.setCategoryVelocityVarName(catvelvar);
        }
    }
}
