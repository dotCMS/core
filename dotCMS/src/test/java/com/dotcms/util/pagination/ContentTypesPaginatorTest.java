package com.dotcms.util.pagination;

import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.util.PaginatedArrayList;
import com.google.common.collect.ImmutableMap;
import com.liferay.portal.model.User;
import org.junit.Test;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * Test of {@link ContentTypesPaginator}
 */
public class ContentTypesPaginatorTest {

    @Test
    public void getItems(){
        final StructureAPI structureAPI = mock(StructureAPI.class);
        final ContentTypesPaginator contentTypesPaginator = new ContentTypesPaginator(structureAPI);

        final User user = mock(User.class);
        final String filter = "filter";
        final int limit = 10;
        final int offset = 2;
        final String orderby = "name";
        final OrderDirection direction = OrderDirection.ASC;
        final Map<String, Object> extraParams = ImmutableMap.<String, Object> builder()
                .put(ContentTypesPaginator.TYPE_PARAMETER_NAME, "FORM")
                .build();

        PaginatedArrayList<Map<String, Object>> items =
                contentTypesPaginator.getItems(user, filter, limit, offset, orderby, direction, extraParams);
    }
}
