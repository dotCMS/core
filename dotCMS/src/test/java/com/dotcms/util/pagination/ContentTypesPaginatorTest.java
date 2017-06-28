package com.dotcms.util.pagination;

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
}
