package com.dotcms.util.pagination;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.business.CategoryAPI;
import com.dotmarketing.portlets.categories.business.CategorySearchCriteria;
import com.dotmarketing.portlets.categories.business.PaginatedCategories;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import java.util.List;

import static com.dotcms.util.CollectionsUtils.list;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link CategoriesPaginator}
 */
public class CategoriesPaginatorTest {

    @Test
    public void when_orderByIsNotNull_and_ExistsItemsByFilter_should_returnItems()
            throws DotSecurityException, DotDataException {

        final CategoryAPI categoryAPI = mock(CategoryAPI.class);
        final User user = mock(User.class);
        final int offset = 2;
        final int limit = 10;
        final String filter = "filter";

        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("childrenCategories", false);

        final PaginatedCategories topLevelCategories = mock(PaginatedCategories.class);
        final List<Category> categories = list(mock(Category.class), mock(Category.class));

        when(topLevelCategories.getTotalCount()).thenReturn(categories.size());
        when(topLevelCategories.getCategories()).thenReturn(categories);

        final CategorySearchCriteria searchingCriteria = new CategorySearchCriteria.Builder()
                .filter(filter)
                .limit(limit)
                .offset(offset)
                .orderBy("sort_order")
                .direction(OrderDirection.ASC )
                .rootInode("")
                .build();

        when(categoryAPI.findAll(searchingCriteria, user, false))
                .thenReturn(topLevelCategories);

        final CategoriesPaginator categoriesPaginator = new CategoriesPaginator(categoryAPI);
        final PaginatedArrayList<Category> items =
                categoriesPaginator.getItems(user, filter, limit, offset, "sort_order", OrderDirection.ASC, extraParams);

        assertEquals(categories.size(), items.getTotalResults());
        assertEquals(categories.size(), items.size());
        assertEquals(categories.get(0), items.get(0));
        assertEquals(categories.get(1), items.get(1));
    }

    @Test
    public void when_orderByIsNotNull_and_NotexistsItemsByFilter_should_returnItems()
            throws DotSecurityException, DotDataException {

        final CategoryAPI categoryAPI = mock(CategoryAPI.class);
        final User user = mock(User.class);
        final int offset = 2;
        final int limit = 10;
        final String filter = "filter";
        final PaginatedCategories topLevelCategories = mock(PaginatedCategories.class);

        when(topLevelCategories.getTotalCount()).thenReturn(0);
        when(topLevelCategories.getCategories()).thenReturn(null);

        final CategorySearchCriteria searchingCriteria = new CategorySearchCriteria.Builder()
                .filter(filter)
                .limit(limit)
                .offset(offset)
                .orderBy("sort_order")
                .direction(OrderDirection.ASC )
                .rootInode("")
                .build();

        when(categoryAPI.findAll(searchingCriteria, user, false))
                .thenReturn(topLevelCategories);

        final Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("childrenCategories", false);

        final CategoriesPaginator categoriesPaginator = new CategoriesPaginator(categoryAPI);
        final PaginatedArrayList<Category> items =
                categoriesPaginator.getItems(user, filter, limit, offset, "sort_order",  OrderDirection.ASC, extraParams);

        assertEquals(0, items.getTotalResults());
        assertTrue(items.isEmpty());
    }
}
