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
    
    static IndiciesFactory ifac =null;
    static IndiciesAPI iapi =null;
    
    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        ifac= FactoryLocator.getIndiciesFactory();
        iapi=APILocator.getIndiciesAPI();

    }
    
    @Test
    public void test_index_pointing_when_previously_null() throws Exception{
        
        new DotConnect().setSQL("delete from indicies").loadResult();
        CacheLocator.getIndiciesCache().clearCache();
        IndiciesInfo nullInfo = iapi.loadIndicies();
        assert(nullInfo.live==null);
        assert(nullInfo.working==null);
        assert(nullInfo.reindex_live==null);
        assert(nullInfo.reindex_working==null);
        
        IndiciesInfo info = new IndiciesInfo();
        info.working="info.working";
        info.live="info.live";
        info.reindex_live="info.reindex_live";
        info.reindex_working="info.reindex_working";
        ifac.point(info);
        
        
        IndiciesInfo cachedInfo = iapi.loadIndicies();
        assertEquals(cachedInfo.live,"info.live");
        assertEquals(cachedInfo.working,"info.working");
        assertEquals(cachedInfo.reindex_live,"info.reindex_live");
        assertEquals(cachedInfo.reindex_working,"info.reindex_working");
    }
    
    
    
    @Test
    public void test_index_repointing_when_previously_set() throws Exception{
        
        new DotConnect().setSQL("delete from indicies").loadResult();
        CacheLocator.getIndiciesCache().clearCache();

        IndiciesInfo info = new IndiciesInfo();
        info.working="info.working";
        info.live="info.live";
        ifac.point(info);
        
        
        IndiciesInfo cachedInfo = iapi.loadIndicies();
        assertEquals(cachedInfo.live,"info.live");
        assertEquals(cachedInfo.working,"info.working");
        assertNull(cachedInfo.reindex_live);
        assertNull(cachedInfo.reindex_working);
        
        info = new IndiciesInfo();
        info.reindex_live="info.reindex_live";
        info.reindex_working="info.reindex_working";
        ifac.point(info);
        
        cachedInfo = iapi.loadIndicies();
        assertNull(cachedInfo.live);
        assertNull(cachedInfo.working);
        assertEquals(cachedInfo.reindex_live,"info.reindex_live");
        assertEquals(cachedInfo.reindex_working,"info.reindex_working");
        
        
    }
}
