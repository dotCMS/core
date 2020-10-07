package com.dotmarketing.business.cache.provider;

import org.junit.Test;
import com.dotmarketing.portlets.contentlet.business.Contentlet;

public class CacheSizingUtilTest {

    
    

    
    CacheSizingUtil cacheSizer = new CacheSizingUtil();
    
    @Test
    public void test_sizeof_works() {
       
        final Contentlet con = new Contentlet();
        
        long contentletSize = cacheSizer.sizeOfMemory(con);
        assert(contentletSize>0);
        
        
        final String fourLetters = "CRAP";
        
        long fourLettersSize = cacheSizer.sizeOfMemory(fourLetters);
        assert(fourLettersSize==48);
        
        
        
        
        
        
        
    }

}
