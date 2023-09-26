package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.api.v1.categories.CategoryListDTO;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.business.PaginatedCategories;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;

import static com.dotmarketing.common.util.SQLUtil._ASC;
import static com.dotmarketing.common.util.SQLUtil._DESC;

/**
 * Category paginator
 */
public class CategoryListDTOPaginator implements PaginatorOrdered<CategoryListDTO> {

    private final CategoryAPI categoryAPI;

    @VisibleForTesting
    public CategoryListDTOPaginator(final CategoryAPI categoryAPI){
        this.categoryAPI = categoryAPI;
    }

    public CategoryListDTOPaginator(){
        this(APILocator.getCategoryAPI());
    }

    @Override
    public PaginatedArrayList<CategoryListDTO> getItems(final User user, final String filter, final int limit, final int offset,
                                                 final String orderby, final OrderDirection direction, final Map<String, Object> extraParams) {
        try {
            String categoriesSort = null;

            if (orderby != null) {
                categoriesSort = direction == OrderDirection.DESC ? "-" + orderby : orderby;
            }

            final PaginatedArrayList<CategoryListDTO> result = new PaginatedArrayList<>();
            final PaginatedCategories topLevelCategories = categoryAPI.findTopLevelCategories(user, false, offset, limit, filter, categoriesSort);
            result.setTotalResults(topLevelCategories.getTotalCount());

            final List<Category> categories = topLevelCategories.getCategories();

            if (categories != null) {
                for(var category : categories) {
                    CategoryListDTO categoryListDTO = new CategoryListDTO(category.getCategoryName(),category.getCategoryVelocityVarName(), category.getKey(),
                            category.getKeywords(), category.getSortOrder(), category.getDescription(),category.isActive(),category.getModDate(),
                            category.getIDate(),category.getType(),category.getOwner(),category.getInode(),category.getIdentifier(),
                            this.categoryAPI.findChildren(user, category.getInode(), false, offset, limit,filter,
                                    direction.toString().toLowerCase().equals("asc") ? _ASC : _DESC).getTotalCount());

                    result.add(categoryListDTO);
                }
            }

            return result;
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }
}
