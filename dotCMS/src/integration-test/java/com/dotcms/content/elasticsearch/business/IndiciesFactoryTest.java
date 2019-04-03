package com.dotcms.content.elasticsearch.business;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.content.elasticsearch.business.IndiciesAPI.IndiciesInfo;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;

public class IndiciesFactoryTest {

    private static final String INFO_REINDEX_WORKING = "info.reindex_working";
    private static final String INFO_WORKING = "info.working";
    private static final String INFO_LIVE = "info.live";
    private static final String INFO_REINDEX_LIVE = "info.reindex_live";
    static IndiciesFactory indiciesFactory;
    static IndiciesAPI indiciesAPI;


    
    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        indiciesFactory = FactoryLocator.getIndiciesFactory();
        indiciesAPI =APILocator.getIndiciesAPI();

    }
    
    //@Test
    public void test_index_pointing_when_previously_null() throws Exception{
        
        new DotConnect().setSQL("delete from indicies").loadResult();
        CacheLocator.getIndiciesCache().clearCache();
        IndiciesInfo nullInfo = indiciesAPI.loadIndicies();
        assert(nullInfo.live==null);
        assert(nullInfo.working==null);
        assert(nullInfo.reindex_live==null);
        assert(nullInfo.reindex_working==null);
        
        final IndiciesInfo info = new IndiciesInfo();
        info.working= INFO_WORKING;
        info.live= INFO_LIVE;
        info.reindex_live= INFO_REINDEX_LIVE;
        info.reindex_working= INFO_REINDEX_WORKING;
        indiciesFactory.point(info);
        
        
        final IndiciesInfo cachedInfo = indiciesAPI.loadIndicies();
        assertEquals(cachedInfo.live, INFO_LIVE);
        assertEquals(cachedInfo.working, INFO_WORKING);
        assertEquals(cachedInfo.reindex_live, INFO_REINDEX_LIVE);
        assertEquals(cachedInfo.reindex_working,INFO_REINDEX_WORKING);
    }
    
    
    
    //@Test
    public void test_index_repointing_when_previously_set() throws Exception{
        
        new DotConnect().setSQL("delete from indicies").loadResult();
        CacheLocator.getIndiciesCache().clearCache();

        IndiciesInfo info = new IndiciesInfo();
        info.working= INFO_WORKING;
        info.live= INFO_LIVE;
        indiciesFactory.point(info);
        
        
        IndiciesInfo cachedInfo = indiciesAPI.loadIndicies();
        assertEquals(cachedInfo.live, INFO_LIVE);
        assertEquals(cachedInfo.working, INFO_WORKING);
        assertNull(cachedInfo.reindex_live);
        assertNull(cachedInfo.reindex_working);
        
        info = new IndiciesInfo();
        info.reindex_live= INFO_REINDEX_LIVE;
        info.reindex_working=INFO_REINDEX_WORKING;
        indiciesFactory.point(info);
        
        cachedInfo = indiciesAPI.loadIndicies();
        assertNull(cachedInfo.live);
        assertNull(cachedInfo.working);
        assertEquals(cachedInfo.reindex_live, INFO_REINDEX_LIVE);
        assertEquals(cachedInfo.reindex_working, INFO_REINDEX_WORKING);
        
        
    }
}
