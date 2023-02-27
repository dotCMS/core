package com.dotcms.browser;

import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * BrowserAPIImpl Tests
 *
 * @author Hassan Mustafa Baig
 */
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

    @Test
    public void getFolderContent_searchDotAssetWithFilter_shouldReturnNotNull() throws DotDataException, DotSecurityException {
        final String filterText = "company_logo.png";
        final User user = mock(User.class);
        final List<String> mimeTypes = List.of("image");
        final Contentlet contentlet = mock(Contentlet.class);
        final List<Contentlet> contentletList = List.of(contentlet);

        final BrowserQuery browserQuery = BrowserQuery.builder()
                .withUser(user)
                .withHostOrFolderId("SYSTEM_HOST")
                .offset(0)
                .maxResults(1)
                .withFilter(filterText)
                .showMimeTypes(mimeTypes)
                .showImages(mimeTypes.contains(mimeTypes.get(0)))
                .showExtensions(null)
                .showWorking(true)
                .showArchived(false)
                .showFolders(false)
                .showFiles(true)
                .showShorties(false)
                .showContent(true)
                .sortBy("modDate")
                .sortByDesc(true)
                .showLinks(false)
                .withLanguageId(1)
                .showDotAssets(true)
                .build();

        when(browserAPI.getContentUnderParentFromDB(browserQuery)).thenReturn(contentletList);

        final Map<String, Object> result = browserAPI.getFolderContent(browserQuery);
        assertNotNull(result);
    }
}
