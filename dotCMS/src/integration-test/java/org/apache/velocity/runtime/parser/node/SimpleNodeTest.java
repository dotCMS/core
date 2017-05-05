package org.apache.velocity.runtime.parser.node;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.VelocityUtil;

import org.apache.velocity.app.VelocityEngine;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.ObjectOutputStream;

public class SimpleNodeTest {

	@BeforeClass
	public static void prepare() throws Exception{
		//Setting web app environment
        IntegrationTestInitService.getInstance().init();
	}

    @Test
    public void serializeTest() throws Exception {
        CacheLocator.getCacheAdministrator().flushAlLocalOnly(true);
        
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

        String path=Config.getStringProperty("VELOCITY_ROOT") + File.separator + "VM_global_library.vm";
        node=engine.getRuntimeServices().parse(new FileReader(path),"VM_global_library.vm");
        
        out=new ByteArrayOutputStream();
        objectOut = new ObjectOutputStream(out);
        objectOut.writeObject(node);
        objectOut.close();
        out.close();
        
        
    }
}
