package com.dotcms.rest.api.v1.content.search.strategies;

import com.dotcms.exception.ExceptionUtil;
import com.dotcms.rest.api.v1.content.search.handlers.FieldContext;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.liferay.util.StringPool.BLANK;
import static com.liferay.util.StringPool.COMMA;

/**
 * This Strategy Field implementation specifies the correct syntax for querying a Category Field via
 * Lucene query in dotCMS.
 *
 * @author Jose Castro
 * @since Jan 29th, 2025
 */
public class CategoryFieldStrategy implements FieldStrategy {

    @Override
    public String generateQuery(final FieldContext fieldContext) {
        final String fieldName = fieldContext.fieldName();
        final String fieldValue = fieldContext.fieldValue().toString();
        final List<String> permissionedCategories = this.extractPermissionedCategories(fieldValue, fieldContext.user());
        final String values = permissionedCategories.stream().map(category -> fieldName + ":" + category + " ")
                .collect(Collectors.joining());
        return UtilMethods.isSet(values) ? "+(" + values.trim() + ")" : BLANK;
    }

    /**
     * Verifies that the User that is generating the Lucene query has the appropriate permissions to
     * access the Category IDs provided in the field value.
     *
     * @param fieldValue The field value containing the Category IDs.
     * @param user       The {@link User} generating the Lucene query.
     *
     * @return A list of Category names that the User has permission to access.
     */
    private List<String> extractPermissionedCategories(final String fieldValue, final User user) {
        final List<String> categoryList = fieldValue.trim().contains(COMMA)
                ? List.of(fieldValue.replaceAll("\\s", BLANK).split(COMMA))
                : List.of(fieldValue.trim());
        final CategoryAPI catAPI = APILocator.getCategoryAPI();
        final List<String> permissionedCategoryNames = new ArrayList<>();
        for (final String categoryId : categoryList) {
            try {
                final Category category = catAPI.find(categoryId, user, false);
                if (null != category && UtilMethods.isSet(category.getKey())) {
                    permissionedCategoryNames.add(category.getCategoryVelocityVarName());
                    continue;
                }
                Logger.warn(this, String.format("Category ID '%s' not found.", categoryId));
            } catch (final DotDataException e) {
                Logger.warnAndDebug(CategoryFieldStrategy.class, String.format("An error occurred when retrieving Category ID " +
                        "'%s': %s", categoryId, ExceptionUtil.getErrorMessage(e)), e);
            } catch (final DotSecurityException e) {
                Logger.warnAndDebug(CategoryFieldStrategy.class, String.format("Failed to verify permissions of User " +
                        "'%s' on Category ID '%s': %s", user.getUserId(), categoryId, ExceptionUtil.getErrorMessage(e)), e);
            }
        }
        return permissionedCategoryNames;
    }

}
