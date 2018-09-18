package com.dotcms.util;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Check StructureAjax and DependecyManager 
 */
public class StructureCategoryUtils {

    private CategoryAPI categoryAPI;

    private PermissionAPI permissionAPI;

    public static final String CATEGORY = Field.FieldType.CATEGORY.toString();

    @VisibleForTesting
    public StructureCategoryUtils(final CategoryAPI categoryAPI,
            final PermissionAPI permissionAPI) {
        this.categoryAPI = categoryAPI;
        this.permissionAPI = permissionAPI;
    }

    public StructureCategoryUtils() {
        this(APILocator.getCategoryAPI(), APILocator.getPermissionAPI());
    }

    public ImmutableList<Category> getCategories(final Structure structure, final User user) {
        return getCategoryFields(structure).stream().map(field -> getCategory(field, user))
                .filter(category -> hasPermission(category, user))
                .collect(CollectionsUtils.toImmutableList());
    }

    private Category getCategory(final Field categoryField, final User user) {
        Category category = null;
        try {
            category = categoryAPI.find(categoryField.getValues(), user, false);
        } catch (DotSecurityException | DotDataException e) {
            Logger.error(getClass(),
                    String.format("Unable to get category for field '%s' ", categoryField), e);
        }
        return category;
    }

    private boolean hasPermission(final Category category, final User user) {
        boolean hasPermission = false;
        try {
            hasPermission = permissionAPI
                    .doesUserHavePermission(category, permissionAPI.PERMISSION_READ, user);
        } catch (DotDataException e) {
            Logger.error(getClass(), String.format(
                    "Unable to check read permission on category '%s' for userId '%s' ", category,
                    user.getUserId()), e);
        }
        return hasPermission;

    }

    public static List<Field> getCategoryFields(final Structure structure) {
        return structure.getFields()
                .stream().filter(field -> field.getFieldType().equals(CATEGORY))
                .collect(Collectors.toList());
    }

}
