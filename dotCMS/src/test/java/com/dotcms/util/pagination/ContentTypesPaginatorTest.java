package com.dotcms.util.pagination;

import com.dotcms.rest.api.v1.site.SiteResource;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.structure.business.StructureAPI;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ContentTypesPaginator} test
 */
public class ContentTypesPaginatorTest {

    private ContentTypesPaginator contentTypesPaginator;
    private StructureAPI structureAPI;

    @Before
    public void initTest(){
        structureAPI = mock( StructureAPI.class );
        contentTypesPaginator = new ContentTypesPaginator( structureAPI );
    }

    @Test
    public void testGetTotalRecords(){
        String filter = "filter";
        String queryCondition = String.format("(name like '%%%s%%')", filter);
        int resultExpected = 5;

        when(structureAPI.countStructures( queryCondition )).thenReturn( resultExpected );
        long totalRecords = contentTypesPaginator.getTotalRecords( filter );
        assertEquals(resultExpected, totalRecords);
    }

    @Test
    public void testGetItems() throws DotDataException {
        User user = new User();
        String filter = "filter";
        boolean showArchived = false;
        int limit = 2;
        int offset = 3;
        String orderby = "order";
        OrderDirection direction = OrderDirection.ASC;
        String queryCondition = String.format("(name like '%%%s%%')", filter);

        List<Structure> structures = getStructures();
        when(structureAPI.find(user, showArchived, false, queryCondition,
                orderby, limit, offset, direction.toString().toLowerCase())).thenReturn( structures );

        contentTypesPaginator.getItems( user, filter, showArchived,  limit, offset, orderby, direction);
    }

    private List<Structure> getStructures() {
        Structure st = new Structure();
        st.setHost("1");
        st.setDescription("Testing");
        st.setName("Structure");
        st.setVelocityVarName("Structure");
        st.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
        st.setFixed(false);
        st.setOwner("2");
        st.setExpireDateVar("");
        st.setPublishDateVar("");

        Structure st2 = new Structure();
        st2.setHost("2");
        st2.setDescription("Testing 2");
        st2.setName("Structure_2");
        st2.setVelocityVarName("Structure_2");
        st2.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
        st2.setFixed(false);
        st2.setOwner("3");
        st2.setExpireDateVar("");
        st2.setPublishDateVar("");

        return CollectionsUtils.list( st, st2 );
    }
}
