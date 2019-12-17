package com.dotcms.content.elasticsearch.business;

import static com.dotmarketing.sitesearch.business.SiteSearchAPI.ES_SITE_SEARCH_NAME;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.sitesearch.business.SiteSearchAPI;
import java.io.IOException;
import java.util.Date;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author nollymar
 */
public class ESSiteSearchAPITest {

    private static SiteSearchAPI siteSearchAPI;
    private static ESIndexAPI indexAPI;
    private static IndiciesAPI indiciesAPI;
    private static ContentletIndexAPI contentletIndexAPI;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        siteSearchAPI = APILocator.getSiteSearchAPI();
        indexAPI = APILocator.getESIndexAPI();
        indiciesAPI = APILocator.getIndiciesAPI();
        contentletIndexAPI = APILocator.getContentletIndexAPI();
    }

    @Test
    public void testCreateSiteSearchIndexAndMakeItDefault() throws IOException, DotDataException {

        final String timeStamp = String.valueOf(new Date().getTime());
        final String indexName = ES_SITE_SEARCH_NAME + "_" + timeStamp;
        final String aliasName = "indexAlias" + timeStamp;

        siteSearchAPI.createSiteSearchIndex(indexName, aliasName, 1);

        assertTrue(indexAPI.listIndices().contains(indexName));

        //verifies that there is no a default site search index
        assertTrue(indiciesAPI.loadIndicies().getSiteSearch() == null || !indiciesAPI
                .loadIndicies().getSiteSearch().equals(indexName));
        siteSearchAPI.activateIndex(indexName);

        try {
            CacheLocator.getIndiciesCache().clearCache();
            assertNotNull(indiciesAPI.loadIndicies().getSiteSearch());
            assertTrue(indiciesAPI.loadIndicies().getSiteSearch().equals(indexName));
        } finally {
            siteSearchAPI.deactivateIndex(indexName);
            indexAPI.delete(indexName);
        }
    }


    @Test
    public void testFullReindexKeepsDefaultSiteSearchIndex()
            throws IOException, DotDataException, DotIndexException {

        final String timeStamp = String.valueOf(new Date().getTime());
        final String indexName = ES_SITE_SEARCH_NAME + timeStamp;
        final String aliasName = "indexAlias" + timeStamp;

        String indexTimestamp = null;

        siteSearchAPI.createSiteSearchIndex(indexName, aliasName, 1);
        //sets as default
        siteSearchAPI.activateIndex(indexName);

        try {
            indexTimestamp = contentletIndexAPI.fullReindexStart();
            CacheLocator.getIndiciesCache().clearCache();
            assertNotNull(indiciesAPI.loadIndicies().getSiteSearch());
            assertTrue(indiciesAPI.loadIndicies().getSiteSearch().equals(indexName));
        } finally {
            contentletIndexAPI.stopFullReindexation();
            siteSearchAPI.deactivateIndex(indexName);
            indexAPI.delete(indexName);

            if (indexTimestamp != null){
                indexAPI.delete(IndexType.WORKING.getPrefix() + "_" + indexTimestamp);
                indexAPI.delete(IndexType.LIVE.getPrefix() + "_" + indexTimestamp);
            }

        }
    }

    @Test
    public void testReindexAbortKeepsDefaultSiteSearchIndex()
            throws IOException, DotDataException, DotIndexException {

        final String timeStamp = String.valueOf(new Date().getTime());
        final String indexName = ES_SITE_SEARCH_NAME + timeStamp;
        final String aliasName = "indexAlias" + timeStamp;

        String indexTimestamp = null;

        siteSearchAPI.createSiteSearchIndex(indexName, aliasName, 1);
        //sets as default
        siteSearchAPI.activateIndex(indexName);

        try {
            indexTimestamp = contentletIndexAPI.fullReindexStart();
            contentletIndexAPI.fullReindexAbort();
            CacheLocator.getIndiciesCache().clearCache();
            assertNotNull(indiciesAPI.loadIndicies().getSiteSearch());
            assertTrue(indiciesAPI.loadIndicies().getSiteSearch().equals(indexName));
        } finally {
            contentletIndexAPI.stopFullReindexation();
            siteSearchAPI.deactivateIndex(indexName);
            indexAPI.delete(indexName);

            if (indexTimestamp != null){
                indexAPI.delete(IndexType.WORKING.getPrefix() + "_" + indexTimestamp);
                indexAPI.delete(IndexType.LIVE.getPrefix() + "_" + indexTimestamp);
            }

        }
    }

}
