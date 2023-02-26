package com.dotcms.browser;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class BrowserAPIImplTest {
    private static BrowserAPIImpl browserAPI;

    @BeforeClass
    public static void init(){
        browserAPI = mock(BrowserAPIImpl.class);
    }

    @Test
    public void getAssetNameColumn_providedBaseQuery_shouldGenerateCorrectSQLForDB() throws DotDataException, DotSecurityException {

        final String sql = browserAPI.getAssetNameColumn("LOWER(%s) LIKE ? ");

        assertNotNull(sql);
        if (DbConnectionFactory.isPostgres()) {
            assertTrue(sql.contains("-> 'fields' -> 'asset' -> 'metadata' ->> 'name'"));
        }
        else{
            assertTrue(sql.contains("$.fields.asset.metadata.name"));
        }
    }
}
