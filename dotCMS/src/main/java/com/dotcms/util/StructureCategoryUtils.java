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

/**
 * Utility to load categories associated with a Structure type.
 * Only the first level is loaded
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

    /**
     * Default constructor
     */
    public StructureCategoryUtils() {
        this(APILocator.getCategoryAPI(), APILocator.getPermissionAPI());
    }

    /**
     * This method will look for all the fields of type 'Category' within a Structure and will get you all the associated Category types available for a given a user.
     * @param structure
     * @param user
     * @return
     */
    public ImmutableList<Category> findCategories(final Structure structure, final User user) {
        return findCategoryFields(structure).stream().map(field -> findCategory(field, user))
                .filter(category -> hasPermission(category, user))
                .collect(CollectionsUtils.toImmutableList());
    }

    /**
     * given a field previously determined to be of type Category this method will look up the respective Category type.
     * @param categoryField
     * @param user
     * @return
     */
    private Category findCategory(final Field categoryField, final User user) {
        if(!categoryField.getFieldType().equals(CATEGORY)){
            throw new IllegalArgumentException(String.format("Field %s can isn't of the expected type 'Category'.",categoryField.getFieldName()));
        }
        Category category = null;
        try {
            category = categoryAPI.find(categoryField.getValues(), user, false);
        } catch (DotSecurityException | DotDataException e) {
            Logger.error(getClass(),
                    String.format("Unable to get category for field '%s' ", categoryField), e);
        }
        return category;
    }

    /**
     * Permission check helper method
     * @param category
     * @param user
     * @return
     */
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

    /**
     * Given a structure this method will look into the fields and get you all the ones of type Category
     * @param structure
     * @return
     */
    private ImmutableList<Field> findCategoryFields(final Structure structure) {
        return structure.getFields()
                .stream().filter(field -> field.getFieldType().equals(CATEGORY))
                .collect(CollectionsUtils.toImmutableList());
    }


    /**
     * Given a structure this method will look into the fields and tells you if there are at least one of type Category
     * @param structure
     * @return
     */
    public boolean hasCategoryFields(final Structure structure) {
        return structure.getFields()
                .stream().anyMatch(field -> field.getFieldType().equals(CATEGORY));

    }

}
