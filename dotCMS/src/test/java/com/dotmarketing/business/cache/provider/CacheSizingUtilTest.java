package com.dotmarketing.business.cache.provider;

import org.junit.Test;
import com.dotmarketing.portlets.contentlet.business.Contentlet;

public class CacheSizingUtilTest {

    
    

    
    CacheSizingUtil cacheSizer = new CacheSizingUtil();
    
    @Test
    public void test_sizeof_works() {
       
        final Contentlet con = new Contentlet();
        
        long contentletSize = cacheSizer.sizeOf(con);
        assert(contentletSize>0);
        
        
        final String fourLetters = "CRAP";
        
        long fourLettersSize = cacheSizer.sizeOf(fourLetters);
        assert(fourLettersSize==48);
        
        
    }
    
    @Test
    public void test_sizeof_null_works() {

        long contentletSize = cacheSizer.sizeOf(null);
        assert(contentletSize==0);

        
    }
    
    

}
