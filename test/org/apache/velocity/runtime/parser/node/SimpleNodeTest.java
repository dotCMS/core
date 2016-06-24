package org.apache.velocity.runtime.parser.node;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.ObjectOutputStream;

import org.apache.velocity.app.VelocityEngine;

import org.junit.Test;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.VelocityUtil;

public class SimpleNodeTest {
    
    
    @Test
    public void serializeTest() throws Exception {
        CacheLocator.getCacheAdministrator().flushAlLocalOnly();
        
        VelocityEngine engine=VelocityUtil.getEngine();
        
        StringBuilder code=new StringBuilder();
        
        for(int i=0;i<10000;i++) {
            code.append("#set($x=").append(i).append(")\n");
        }
        
        SimpleNode node=engine.getRuntimeServices().parse(code.toString(),"test.vtl");
        
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(out);
        objectOut.writeObject(node);
        objectOut.close();
        out.close();
        
        String path=Config.CONTEXT.getRealPath("/WEB-INF/velocity/VM_global_library.vm");
        node=engine.getRuntimeServices().parse(new FileReader(path),"VM_global_library.vm");
        
        out=new ByteArrayOutputStream();
        objectOut = new ObjectOutputStream(out);
        objectOut.writeObject(node);
        objectOut.close();
        out.close();
        
        
    }
}
