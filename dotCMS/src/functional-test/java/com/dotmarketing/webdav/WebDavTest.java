package com.dotmarketing.webdav;


import com.dotcms.enterprise.PasswordFactoryProxy;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.ettrema.httpclient.*;
import com.liferay.portal.model.User;
import java.util.Optional;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WebDavTest {
	
	@Before
	public void before () {
		Config.setProperty("WEBDAV_LEGACY_PATHING", true);
	}
	
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
	    		APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()+java.io.File.separator+"webdavtest_1_"+System.currentTimeMillis());
	    tmpDir1.mkdirs();
	    java.io.File tmpDir2=new java.io.File(
	    		APILocator.getFileAssetAPI().getRealAssetPathTmpBinary()+java.io.File.separator+"webdavtest_2_"+System.currentTimeMillis());
	    tmpDir2.mkdirs();
	    java.io.File f1=new java.io.File(tmpDir1,"test.txt");
	    java.io.File f2=new java.io.File(tmpDir2,"test.txt");
	    FileWriter w = new FileWriter(f1,true); w.write("text1"); w.close();
	    w = new FileWriter(f2,true); w.write("text2"); w.close();
	    
	    ff1.upload(f1);
	    ff2.upload(f2);
	    
	    Thread.sleep(1000); // wait for the index
	    
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
	    
	    assertEquals("text1",sw1.toString());
	    assertEquals("text2",sw2.toString());
	}

	/**
	 * https://github.com/dotCMS/dotCMS/issues/3656
	 * @throws Exception
	 */
	@Test
	public void copy_under_host() throws Exception {
	    User user=APILocator.getUserAPI().getSystemUser();
	    String hostid=APILocator.getHostAPI().findByName("demo.dotcms.com", user, false).getIdentifier();
	    String filename="test_"+UUIDGenerator.generateUuid()+".txt";
	    String copyfilename="copy_test_"+UUIDGenerator.generateUuid()+".txt";
	    java.io.File tmp=java.io.File.createTempFile("filetest", "folder");
	    tmp.delete();
	    tmp.mkdirs();
	    tmp = new java.io.File(tmp,filename);
	    FileUtils.writeStringToFile(tmp, "this is a test text");
	    
	    Contentlet file = new Contentlet();
	    file.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, filename);
	    file.setStringProperty(FileAssetAPI.TITLE_FIELD, filename);
	    file.setStringProperty(FileAssetAPI.HOST_FOLDER_FIELD,hostid);
	    file.setBinary(FileAssetAPI.BINARY_FIELD, tmp);
	    file.setStructureInode(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("fileAsset").getInode());
	    file.setLanguageId(1);
	    file.setHost(hostid);
	    file.setFolder("SYSTEM_FOLDER");
	    file = APILocator.getContentletAPI().checkin(file, user, false);
	    APILocator.getContentletAPI().isInodeIndexed(file.getInode());
	    
	    
	    final HttpServletRequest req=ServletTestRunner.localRequest.get();
        Host hh=new Host(req.getServerName(),"/webdav/autopub",req.getServerPort(),"admin@dotcms.com","admin",null,null);
        
        Folder demo=(Folder)hh.child("demo.dotcms.com");
        File f1=(File)demo.child(filename);
        f1.copyTo(demo, copyfilename);
        Thread.sleep(1000);
        File f2=(File)demo.child(copyfilename);
        ByteArrayOutputStream out1=new ByteArrayOutputStream(),out2=new ByteArrayOutputStream();
        f1.download(out1, null);
        f2.download(out2, null);
        Assert.assertEquals("this is a test text", out1.toString());
        Assert.assertEquals("this is a test text", out2.toString());
        
        f1.delete();
        f2.delete();
	}
	
	/**
	 * https://github.com/dotCMS/dotCMS/issues/3654
	 * @throws Exception
	 */
	@Test
	public void delete_under_host() throws Exception {

		HibernateUtil.setAsyncCommitListenersFinalization(false);

	    User user=APILocator.getUserAPI().getSystemUser();
        String hostid=APILocator.getHostAPI().findByName("demo.dotcms.com", user, false).getIdentifier();
        String filename="test_"+UUIDGenerator.generateUuid()+".txt";
        java.io.File tmp=java.io.File.createTempFile("filetest", "folder");
        tmp.delete();
        tmp.mkdirs();
        tmp = new java.io.File(tmp,filename);
        FileUtils.writeStringToFile(tmp, "this is a test text");
        
        Contentlet file = new Contentlet();
        file.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, filename);
        file.setStringProperty(FileAssetAPI.TITLE_FIELD, filename);
        file.setStringProperty(FileAssetAPI.HOST_FOLDER_FIELD,hostid);
        file.setBinary(FileAssetAPI.BINARY_FIELD, tmp);
        file.setStructureInode(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("fileAsset").getInode());
        file.setLanguageId(1);
        file.setHost(hostid);
        file.setFolder("SYSTEM_FOLDER");
		file.setIndexPolicy(IndexPolicy.FORCE);
		file.setIndexPolicyDependencies(IndexPolicy.FORCE);
		file.setBoolProperty(Contentlet.IS_TEST_MODE, true);
        file = APILocator.getContentletAPI().checkin(file, user, false);
        APILocator.getContentletAPI().isInodeIndexed(file.getInode());
        
        
        final HttpServletRequest req=ServletTestRunner.localRequest.get();
        Host hh=new Host(req.getServerName(),"/webdav/autopub",req.getServerPort(),"admin@dotcms.com","admin",null,null);
        
        Folder demo=(Folder)hh.child("demo.dotcms.com");
        File f1=(File)demo.child(filename);
        f1.delete();
        
        for(Resource rr: demo.children()) {
            if(rr instanceof File) {
                Assert.assertNotSame(filename, ((File)rr).name);
            }
        }
        
        Optional<ContentletVersionInfo> versionInfo = APILocator.getVersionableAPI()
				.getContentletVersionInfo(file.getIdentifier(), 1);

        assertTrue(versionInfo.isPresent());
        assertTrue(versionInfo.get().isDeleted());
	}
	
	/**
	 * https://github.com/dotCMS/dotCMS/issues/3650
	 * @throws Exception
	 */
	@Test(expected = InternalServerError.class)
	public void autoput_without_pub_permissions() throws Exception {
	    User user=APILocator.getUserAPI().getSystemUser();
	    com.dotmarketing.beans.Host demo=APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
	    com.dotmarketing.portlets.folders.model.Folder folder=
	            APILocator.getFolderAPI().createFolders("/wt/"+System.currentTimeMillis(), demo, user, false);
	    
	    User limited=APILocator.getUserAPI().createUser(System.currentTimeMillis()+"", System.currentTimeMillis()+"@dotcms.com");
	    limited.setPassword(PasswordFactoryProxy.generateHash("123"));
	    APILocator.getUserAPI().save(limited, user, false);
	    Role role=APILocator.getRoleAPI().getUserRole(limited);
	    
	    HibernateUtil.startTransaction();
	    try {
	        APILocator.getPermissionAPI().save(
	            new Permission(demo.getIdentifier(),role.getId(),
	            PermissionAPI.PERMISSION_CAN_ADD_CHILDREN | PermissionAPI.PERMISSION_EDIT | PermissionAPI.PERMISSION_USE),demo,user,false);
	    
	        APILocator.getPermissionAPI().save(
	            new Permission(PermissionAPI.permissionTypes.get("FOLDERS"),demo.getIdentifier(),role.getId(),
	            PermissionAPI.PERMISSION_CAN_ADD_CHILDREN|PermissionAPI.PERMISSION_EDIT|PermissionAPI.PERMISSION_USE),demo,user,false);

	        APILocator.getPermissionAPI().save(
	            new Permission(PermissionAPI.permissionTypes.get("CONTENTLETS"),demo.getIdentifier(),role.getId(),
                PermissionAPI.PERMISSION_EDIT|PermissionAPI.PERMISSION_USE),demo,user,false);
	        
	        HibernateUtil.closeAndCommitTransaction();
	    }
	    catch(Exception ex) {
	        HibernateUtil.rollbackTransaction();
	        throw ex;
	    }
	    
	        
	    final HttpServletRequest req=ServletTestRunner.localRequest.get();
        Folder hh=new Host(req.getServerName(),"/webdav/autopub/demo.dotcms.com/wt/"+folder.getName(),
                req.getServerPort(),limited.getEmailAddress(),"123",null,null);
        
        java.io.File tmp=java.io.File.createTempFile("filetest", ".txt");
        FileUtils.writeStringToFile(tmp, "this is a test text 888");
        
       
        File uploaded = hh.uploadFile(tmp);

	}
	
	/**
	 * https://github.com/dotCMS/dotCMS/issues/3640
	 * @throws Exception
	 */
	@Test
	public void rename_under_host() throws Exception {
	    User user=APILocator.getUserAPI().getSystemUser();
        String hostid=APILocator.getHostAPI().findByName("demo.dotcms.com", user, false).getIdentifier();
        String filename="test_"+UUIDGenerator.generateUuid()+".txt";
        String renamefilename="rename_test_"+UUIDGenerator.generateUuid()+".txt";
        java.io.File tmp=java.io.File.createTempFile("filetest", "folder");
        tmp.delete();
        tmp.mkdirs();
        tmp = new java.io.File(tmp,filename);
        FileUtils.writeStringToFile(tmp, "this is a test text");
        
        Contentlet file = new Contentlet();
        file.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, filename);
        file.setStringProperty(FileAssetAPI.TITLE_FIELD, filename);
        file.setStringProperty(FileAssetAPI.HOST_FOLDER_FIELD,hostid);
        file.setBinary(FileAssetAPI.BINARY_FIELD, tmp);
        file.setStructureInode(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("fileAsset").getInode());
        file.setLanguageId(1);
        file.setHost(hostid);
        file.setFolder("SYSTEM_FOLDER");
        file.setIndexPolicy(IndexPolicy.FORCE);
		file.setIndexPolicyDependencies(IndexPolicy.FORCE);
        file = APILocator.getContentletAPI().checkin(file, user, false);
        APILocator.getContentletAPI().isInodeIndexed(file.getInode());

        
        final HttpServletRequest req=ServletTestRunner.localRequest.get();
        Host hh=new Host(req.getServerName(),"/webdav/autopub",req.getServerPort(),"admin@dotcms.com","admin",null,null);
        
        Folder demo=(Folder)hh.child("demo.dotcms.com");
        File f1=(File)demo.child(filename);
        f1.rename(renamefilename);
        Thread.sleep(1000);
        File f2=(File)demo.child(renamefilename);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        f2.download(out, null);
        Assert.assertEquals("this is a test text", out.toString());
        f2.delete();
	}
}












