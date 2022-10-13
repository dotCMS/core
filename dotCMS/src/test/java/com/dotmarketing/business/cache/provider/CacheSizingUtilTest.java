package com.dotmarketing.business.cache.provider;

import org.junit.Test;
import com.dotcms.repackage.com.google.common.base.Optional;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

public class CacheSizingUtilTest {

    
    final String fourLetters = "CRAP";

    
    CacheSizingUtil cacheSizer = new CacheSizingUtil();
    
    @Test
    public void test_sizeof_works() {
       
        final Contentlet con = new Contentlet();
        
        long contentletSize = cacheSizer.sizeOf(con);
        assert(contentletSize>0);
        
        

        
        long fourLettersSize = cacheSizer.sizeOf(fourLetters);
        assert(fourLettersSize==44);
        
        
    }
    
    @Test
    public void test_sizeof_null_works() {

        long cacheSize = cacheSizer.sizeOf(null);
        assert(cacheSize==0);

        
    }
    
    
    @Test
    public void test_sizeof_optional_works() {

        
        
        
        long cacheSize = cacheSizer.sizeOf(Optional.fromNullable(null));
        assert(cacheSize==16);
        
        cacheSize = cacheSizer.sizeOf(Optional.of(fourLetters));
        assert(cacheSize==60);

        
    }

}
