package com.dotmarketing.webdav;


import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.ettrema.httpclient.File;
import com.ettrema.httpclient.Folder;
import com.ettrema.httpclient.Host;
import com.ettrema.httpclient.Resource;

public class WebDavTest extends TestBase {
	@Test
	public void uploadTest() throws Exception {
		final HttpServletRequest req=ServletTestRunner.localRequest.get();
		Host host=new Host(req.getServerName(),"/webdav/autopub",req.getServerPort(),"admin@dotcms.com","admin",null,null);
		
		
		// this is mostly capturing https://github.com/dotCMS/dotCMS/issues/2815
		Folder demo=(Folder)host.child("demo.dotcms.com");
		Folder webdavtest=demo.createFolder("webdavtest_"+System.currentTimeMillis());
		Folder ff1=webdavtest.createFolder("ff1");
		Folder ff2=webdavtest.createFolder("ff2");
	    
		java.io.File tmpDir1=new java.io.File(
	    		APILocator.getFileAPI().getRealAssetPathTmpBinary()+java.io.File.separator+"webdavtest_1_"+System.currentTimeMillis());
	    tmpDir1.mkdirs();
	    java.io.File tmpDir2=new java.io.File(
	    		APILocator.getFileAPI().getRealAssetPathTmpBinary()+java.io.File.separator+"webdavtest_2_"+System.currentTimeMillis());
	    tmpDir2.mkdirs();
	    java.io.File f1=new java.io.File(tmpDir1,"test.txt");
	    java.io.File f2=new java.io.File(tmpDir2,"test.txt");
	    FileWriter w = new FileWriter(f1,true); w.write("text1"); w.close();
	    w = new FileWriter(f2,true); w.write("text2"); w.close();
	    
	    ff1.upload(f1);
	    ff2.upload(f2);
	    
	    List<? extends Resource> children1 = ff1.children();
	    List<? extends Resource> children2 = ff2.children();
	    assertEquals(1,children1.size());
	    assertEquals(1,children2.size());
	    
	    File r1 = (File)children1.get(0);
	    File r2 = (File)children2.get(0);
	    
	    ByteArrayOutputStream sw1=new ByteArrayOutputStream();
	    r1.download(sw1, null);
	    
	    ByteArrayOutputStream sw2=new ByteArrayOutputStream();
	    r2.download(sw2, null);
	    
	    assertEquals("text1",sw1);
	    assertEquals("text2",sw2);
	}
}
