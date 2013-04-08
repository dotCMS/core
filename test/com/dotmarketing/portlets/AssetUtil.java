package com.dotmarketing.portlets;

import static org.junit.Assert.assertEquals;

import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.util.UtilMethods;

public class AssetUtil {
    public static void assertDeleted(String inode, String identifier, String type) throws Exception {
        DotConnect dc=new DotConnect();
        dc.setSQL("select * from identifier where id=?");
        dc.addParam(identifier);
        assertEquals(0, dc.loadObjectResults().size());
        
        dc.setSQL("select * from inode where inode=?");
        dc.addParam(inode);
        assertEquals(0, dc.loadObjectResults().size());
        
        String vinfo=UtilMethods.getVersionInfoTableName(type);
        dc.setSQL("select * from "+vinfo+" where identifier=? or working_inode=? or live_inode=?");
        dc.addParam(identifier);
        dc.addParam(inode);
        dc.addParam(inode);
        assertEquals(0, dc.loadObjectResults().size());
        
        dc.setSQL("select * from "+type+" where inode=? or identifier=?");
        dc.addParam(inode);
        dc.addParam(identifier);
        assertEquals(0, dc.loadObjectResults().size());
        
        dc.setSQL("select * from tree where child=? or child=? or parent=? or parent=?");
        dc.addParam(inode);
        dc.addParam(identifier);
        dc.addParam(inode);
        dc.addParam(identifier);
        assertEquals(0, dc.loadObjectResults().size());
        
        dc.setSQL("select * from multi_tree where child=? or child=? or parent1=? or parent2=?");
        dc.addParam(inode);
        dc.addParam(identifier);
        dc.addParam(inode);
        dc.addParam(identifier);
        assertEquals(0, dc.loadObjectResults().size());
        
        dc.setSQL("select * from template_containers where container_id=? or container_id=? or template_id=? or template_id=?");
        dc.addParam(inode);
        dc.addParam(identifier);
        dc.addParam(inode);
        dc.addParam(identifier);
        assertEquals(0, dc.loadObjectResults().size());
        
        dc.setSQL("select * from tag_inode where inode=? or inode=?");
        dc.addParam(inode);
        dc.addParam(identifier);
        assertEquals(0, dc.loadObjectResults().size());
        
        dc.setSQL("select * from permission where inode_id=? or inode_id=?");
        dc.addParam(inode);
        dc.addParam(identifier);
        assertEquals(0, dc.loadObjectResults().size());
        
        dc.setSQL("select * from permission_reference where asset_id=? or asset_id=? or reference_id=? or reference_id=?");
        dc.addParam(inode);
        dc.addParam(identifier);
        dc.addParam(inode);
        dc.addParam(identifier);
        assertEquals(0, dc.loadObjectResults().size());
    }
}
