package com.dotcms.rest.api.v1.categories;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.function.Supplier;

/**
 * Helper for templates
 *
 * @author jsanca
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

    public CategoryView toCategoryView(final Category category, final User user) {

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
}
