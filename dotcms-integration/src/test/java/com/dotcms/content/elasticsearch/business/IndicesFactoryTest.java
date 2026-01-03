package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.dotcms.content.elasticsearch.business.LegacyIndicesInfo.Builder;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class IndicesFactoryTest {

    private static final String INFO_REINDEX_WORKING = "info.reindex_working";
    private static final String INFO_WORKING = "info.working";
    private static final String INFO_LIVE = "info.live";
    private static final String INFO_REINDEX_LIVE = "info.reindex_live";
    private static String ORIGINAL_WORKING_INDEX;
    private static String ORIGINAL_LIVE_INDEX;
    static IndicesFactory indiciesFactory;
    static IndicesAPI indicesAPI;
    
    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        indiciesFactory = FactoryLocator.getIndiciesFactory();
        indicesAPI = APILocator.getIndiciesAPI();

        IndicesInfo legacyIndicesInfo = indicesAPI.loadLegacyIndices();
        ORIGINAL_WORKING_INDEX    = legacyIndicesInfo.getWorking();
        ORIGINAL_LIVE_INDEX       = legacyIndicesInfo.getLive();
    }
    
    @Test
    public void test_index_pointing_when_previously_null() throws Exception{
        new DotConnect().setSQL("delete from indicies").loadResult();
        CacheLocator.getIndiciesCache().clearCache();
        IndicesInfo nullInfo = indicesAPI.loadLegacyIndices();
        assert(nullInfo.getLive()==null);
        assert(nullInfo.getWorking()==null);
        assert(nullInfo.getReindexLive()==null);
        assert(nullInfo.getReindexWorking()==null);

        final Builder builder = new Builder();
        builder.setWorking(INFO_WORKING).setLive(INFO_LIVE).setReindexLive(INFO_REINDEX_LIVE)
                .setReindexWorking(INFO_REINDEX_WORKING);

        indiciesFactory.point(builder.build());

        final IndicesInfo cachedInfo = indicesAPI.loadLegacyIndices();
        assertEquals(cachedInfo.getLive(), INFO_LIVE);
        assertEquals(cachedInfo.getWorking(), INFO_WORKING);
        assertEquals(cachedInfo.getReindexLive(), INFO_REINDEX_LIVE);
        assertEquals(cachedInfo.getReindexWorking(),INFO_REINDEX_WORKING);
    }
    
    
    
    @Test
    public void test_index_repointing_when_previously_set() throws Exception{
        new DotConnect().setSQL("delete from indicies").loadResult();
        CacheLocator.getIndiciesCache().clearCache();

        Builder builder = new Builder();
        builder.setWorking(INFO_WORKING).setLive(INFO_LIVE);
        indiciesFactory.point(builder.build());

        IndicesInfo cachedInfo = indicesAPI.loadLegacyIndices();
        assertEquals(cachedInfo.getLive(), INFO_LIVE);
        assertEquals(cachedInfo.getWorking(), INFO_WORKING);
        assertNull(cachedInfo.getReindexLive());
        assertNull(cachedInfo.getReindexWorking());

        builder = new Builder();
        builder.setReindexLive(INFO_REINDEX_LIVE).setReindexWorking(INFO_REINDEX_WORKING);
        indiciesFactory.point(builder.build());
        
        cachedInfo = indicesAPI.loadLegacyIndices();
        assertNull(cachedInfo.getLive());
        assertNull(cachedInfo.getWorking());
        assertEquals(cachedInfo.getReindexLive(), INFO_REINDEX_LIVE);
        assertEquals(cachedInfo.getReindexWorking(), INFO_REINDEX_WORKING);
    }

    @AfterClass
    public static void restoreIndex() throws Exception {

        final Builder builder = new Builder();
        builder.setWorking(ORIGINAL_WORKING_INDEX).setLive(ORIGINAL_LIVE_INDEX);

        indiciesFactory.point(builder.build());
    }
}
