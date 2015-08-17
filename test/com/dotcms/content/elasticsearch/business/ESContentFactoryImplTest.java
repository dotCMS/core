package com.dotcms.content.elasticsearch.business;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.repackage.org.junit.Assert;
import com.dotcms.repackage.org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.model.Structure;

public class ESContentFactoryImplTest {
    
    final ESContentFactoryImpl instance = new ESContentFactoryImpl();
    
    @Test
    public void findContentlets() throws Exception {
        DotConnect dc=new DotConnect();
        dc.setSQL("select inode from contentlet");
        List<String> inodes=new ArrayList<String>();
        for(Map<String,Object> r : dc.loadObjectResults()) {
            inodes.add((String)r.get("inode"));
        }
        
        List<Contentlet> contentlets = instance.findContentlets(inodes);
        
        Assert.assertEquals(inodes.size(), contentlets.size());
        
        Set<String> inodesSet=new HashSet<String>(inodes);
        for(Contentlet cc : contentlets) {
            Assert.assertTrue(inodesSet.remove(cc.getInode()));
        }
        Assert.assertEquals(0, inodesSet.size());
    }

    @Test
    public void saveContentlets() throws Exception {
        try {
            // Insert without language id
            Host systemHost = new Host();
            systemHost.setDefault(false);
            systemHost.setHostname("dummy-system");
            systemHost.setSystemHost(true);
            systemHost.setHost(null);
            instance.save(systemHost);

            Assert.fail("Saving a contentlet without language must throw an exception.");
        } catch (Exception e) {
        }

        try {
            // Insert with an invalid language id
            Host systemHost = new Host();
            systemHost.setDefault(false);
            systemHost.setHostname("dummy-system");
            systemHost.setSystemHost(true);
            systemHost.setHost(null);
            systemHost.setLanguageId(9999);
            instance.save(systemHost);

            Assert.fail("Saving a contentlet with unexisting language must throw an exception.");
        } catch (Exception e) {
        }
    }
}
