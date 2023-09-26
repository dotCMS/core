package com.dotcms.util.pagination;

import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.categories.CategoryListDTO;
import com.dotcms.util.PaginationUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.business.PaginatedCategories;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

import static com.dotmarketing.common.util.SQLUtil._ASC;
import static com.dotmarketing.common.util.SQLUtil._DESC;

/**
 * Category paginator
 */
public class ChildrenCategoryListDTOPaginator implements PaginatorOrdered<CategoryListDTO> {

    private final CategoryAPI categoryAPI;

    @VisibleForTesting
    public ChildrenCategoryListDTOPaginator(final CategoryAPI categoryAPI) {
        this.categoryAPI = categoryAPI;
    }

    public ChildrenCategoryListDTOPaginator() {
        this(APILocator.getCategoryAPI());
    }

    @Override
    public PaginatedArrayList<CategoryListDTO> getItems(final User user, final String filter,
                                                        final int limit, final int offset,
                                                        final String orderby, final OrderDirection direction,
                                                        final Map<String, Object> extraParams) {
        try {
            final PaginatedArrayList<CategoryListDTO> result = getChildrenCategories(user,
                    extraParams.containsKey("inode") ? String.valueOf(extraParams.get("inode")) : StringPool.BLANK, offset, limit, filter,
                    direction, orderby);
            result.setTotalResults(result.size());
            return result;
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    private PaginatedArrayList<CategoryListDTO> getChildrenCategories(final User user,
                                                                      final String inode,
                                                                      final int page, final int perPage,
                                                                      final String filter, final OrderDirection direction, final String orderBy)
            throws DotDataException, DotSecurityException {

        final PaginatedArrayList<CategoryListDTO> result = new PaginatedArrayList<>();
        final PaginatedCategories list = this.categoryAPI.findChildren(user, inode,
                false, page, perPage,
                filter, (direction == OrderDirection.DESC ? "-" + orderBy : orderBy));

        if (list.getCategories() != null) {
            for (var category : list.getCategories()) {
                CategoryListDTO categoryListDTO = new CategoryListDTO(
                        category.getCategoryName(), category.getCategoryVelocityVarName(),
                        category.getKey(),
                        category.getKeywords(), category.getSortOrder(),
                        category.getDescription(), category.isActive(), category.getModDate(),
                        category.getIDate(), category.getType(), category.getOwner(),
                        category.getInode(), category.getIdentifier(),
                        this.categoryAPI.findChildren(user, category.getInode(),
                                false, page, perPage, filter,
                                direction.toString().toLowerCase().equals("asc") ? _ASC : _DESC).getTotalCount());

                result.add(categoryListDTO);
            }
        }

        return result;
    }
}