package com.dotmarketing.business;

import com.dotcms.repackage.junit_4_8_1.org.junit.Assert;
import com.dotcms.repackage.junit_4_8_1.org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;

public class IdentifierAPITest {
    
    protected static final String id404 = "$$__404__CACHE_MISS__$$";
    final IdentifierAPI api = APILocator.getIdentifierAPI();
    final IdentifierCache cache = CacheLocator.getIdentifierCache();
    
    @Test
    public void testing404() throws Exception {
        
        final Host syshost = APILocator.getHostAPI().findSystemHost();
        
        // fake not yet created id and asset
        String fakeId=UUIDGenerator.generateUuid();
        Contentlet fakeCont=new Contentlet();
        fakeCont.setInode(UUIDGenerator.generateUuid());
        fakeCont.setStructureInode(StructureCache.getStructureByVelocityVarName("Host").getInode());
        
        // if we find a fake id it should end up in 404 cache
        api.find(fakeId);
        Assert.assertEquals(id404, cache.getIdentifier(fakeId).getAssetType());
        api.find(syshost, "/"+fakeId+".content");
        Assert.assertEquals(id404, cache.getIdentifier(syshost.getIdentifier(),"/"+fakeId+".content").getAssetType());
        
        // now if we create an asset with that ID it should be cleared
        api.createNew(fakeCont, syshost, fakeId);
        Assert.assertNull(cache.getIdentifier(fakeId));
        Assert.assertNull(cache.getIdentifier(syshost.getIdentifier(), "/"+fakeId+".content"));
        
        // this should load the identifier in both cache entries (by url and by id)
        api.find(fakeId);
        Assert.assertEquals(fakeId, cache.getIdentifier(fakeId).getId());
        Assert.assertEquals(fakeId, cache.getIdentifier(syshost.getIdentifier(), "/"+fakeId+".content").getId());
        
        
    }
}
