package com.dotcms.content.elasticsearch.business;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dotcms.repackage.junit_4_8_1.org.junit.Assert;
import com.dotcms.repackage.junit_4_8_1.org.junit.Test;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

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
}
