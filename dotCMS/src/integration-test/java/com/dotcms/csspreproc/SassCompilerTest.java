package com.dotcms.csspreproc;

import java.io.File;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class SassCompilerTest {
    
    protected String baseURL=null;
    
    @Before
    public void prepare() throws Exception {
        HttpServletRequest req=ServletTestRunner.localRequest.get();
        baseURL = "http://"+req.getServerName()+":"+req.getServerPort();
    }
    
    @Test
    public void case01() throws Exception {
    	User systemUser = APILocator.getUserAPI().getSystemUser();
    	
        final String runId =  UUIDGenerator.generateUuid() ;
        final File tmpDir = new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + 
                File.separator + runId + File.separator + "sass01"); 
        tmpDir.mkdirs();
        
        final File screenFile = new File(tmpDir, "screen.scss");
        FileUtils.copyURLToFile(SassCompilerTest.class.getResource("sass01/screen.scss"), screenFile);
        
        final File layoutFile = new File(tmpDir, "_layout.scss");
        FileUtils.copyURLToFile(SassCompilerTest.class.getResource("sass01/_layout.scss"), layoutFile);
        
        final File stylesFile = new File(tmpDir, "_styles.scss");
        FileUtils.copyURLToFile(SassCompilerTest.class.getResource("sass01/_styles.scss"), stylesFile);
        
        final String expectedOutput = IOUtils.toString(SassCompilerTest.class.getResourceAsStream("sass01/screen.css"),"UTF-8");
        
        final User sysuser = APILocator.getUserAPI().getSystemUser();
        final Host demo = APILocator.getHostAPI().findByName("demo.dotcms.com", sysuser, false);
        final Folder folder = APILocator.getFolderAPI().createFolders("/"+runId, demo, sysuser, false);
        
        for(File f : new File[] {screenFile,layoutFile,stylesFile}) {
            Contentlet asset = new Contentlet();
            asset.setHost(demo.getIdentifier());
            asset.setFolder(folder.getInode());
            asset.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
            asset.setStructureInode(CacheLocator.getContentTypeCache().getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).getInode());
            asset.setBinary(FileAssetAPI.BINARY_FIELD, f);
            asset.setStringProperty(FileAssetAPI.TITLE_FIELD, f.getName());
            asset = APILocator.getContentletAPI().checkin(asset,sysuser,false);
            APILocator.getContentletAPI().publish(asset, sysuser, false);
            APILocator.getContentletAPI().isInodeIndexed(asset.getInode(),true);
        }
        
        URL cssURL = new URL(baseURL + "/DOTSASS/" + runId + "/screen.css");
        
        long tt1 = System.currentTimeMillis();
        String response =  IOUtils.toString(cssURL.openStream(),"UTF-8");
        tt1 = System.currentTimeMillis() - tt1;
        
        Assert.assertEquals(expectedOutput.trim(), response.substring(0,response.lastIndexOf("}")+1).trim());
        
        // now it should take less time as its in cache now
        for(int x=0; x<10; x++) {
            long ttx = System.currentTimeMillis();
            response =  IOUtils.toString(cssURL.openStream(),"UTF-8");
            ttx = System.currentTimeMillis() - ttx;
            
            Assert.assertTrue(ttx < (tt1/10));
        }
        
        // now lets modify a bit one of the imported files and check if the resulting file reflects the change 
        final File modStylesFile = new File(tmpDir, "_styles.scss");
        FileUtils.writeStringToFile(modStylesFile, 
                IOUtils.toString(SassCompilerTest.class.getResourceAsStream("sass01/_styles.scss")).replace("blue", "green"));
        Contentlet asset = APILocator.getContentletAPI().search(
                "+conhost:"+demo.getIdentifier()+" +confolder:"+folder.getInode()+" +fileasset.filename:_styles.scss", 
                1, 0, "", sysuser, false).get(0);
        asset = APILocator.getContentletAPI().checkout(asset.getInode(), sysuser, false);
        asset.setBinary(FileAssetAPI.BINARY_FIELD, modStylesFile);
        asset = APILocator.getContentletAPI().checkin(asset, sysuser, false);
        APILocator.getContentletAPI().publish(asset, sysuser, false);
        APILocator.getContentletAPI().isInodeIndexed(asset.getInode(),true);
        
        response = IOUtils.toString(cssURL.openStream(),"UTF-8");
        Assert.assertEquals(expectedOutput.replace("blue", "green").trim(), response.substring(0,response.lastIndexOf("}")+1).trim());
        
        
        // check every asset is in cache
        CachedCSS cc = CacheLocator.getCSSCache().get(demo.getIdentifier(), folder.getPath()+"_layout.scss", true, systemUser);
        Assert.assertNotNull(cc);
        cc = CacheLocator.getCSSCache().get(demo.getIdentifier(), folder.getPath()+"_styles.scss", true, systemUser);
        Assert.assertNotNull(cc);
        cc = CacheLocator.getCSSCache().get(demo.getIdentifier(), folder.getPath()+"screen.scss", true, systemUser);
        Assert.assertNotNull(cc);
        
        // if we unpublish _styles.scss we expect the cache entry to be removed
        APILocator.getContentletAPI().unpublish(asset, sysuser, false);
        cc = CacheLocator.getCSSCache().get(demo.getIdentifier(), folder.getPath()+"_styles.scss", true, systemUser);
        Assert.assertNull(cc);
        
        // now with an archive. it should be wiped too
        asset = APILocator.getContentletAPI().search(
                "+conhost:"+demo.getIdentifier()+" +confolder:"+folder.getInode()+" +fileasset.filename:_layout.scss", 
                1, 0, "", sysuser, false).get(0);
        APILocator.getContentletAPI().archive(asset, sysuser, false);
        cc = CacheLocator.getCSSCache().get(demo.getIdentifier(), folder.getPath()+"_layout.scss", true, systemUser);
        Assert.assertNull(cc);
    }
    

    @Test
    public void pathing() throws Exception {
        final User user=APILocator.getUserAPI().getSystemUser();
        final String runId=UUIDGenerator.generateUuid();
        
        // another host for abs path //host/path/to/file testing
        Host host=new Host();
        host.setHostname("test"+runId+".demo.dotcms.com");
        host.setDefault(false);
        try{
        	HibernateUtil.startTransaction();
            host=APILocator.getHostAPI().save(host, user, false);
        	HibernateUtil.commitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(LessCompilerTest.class, e.getMessage());
        }

        APILocator.getHostAPI().publish(host, user, false);
        APILocator.getContentletAPI().isInodeIndexed(host.getInode());
        APILocator.getContentletAPI().isInodeIndexed(host.getInode(),true);
                
        Host defaultHost=APILocator.getHostAPI().findDefaultHost(user, false);
        
        Folder f1=APILocator.getFolderAPI().createFolders("/"+runId+"/a", defaultHost, user, false);
        Folder f2=APILocator.getFolderAPI().createFolders("/"+runId+"/a/b/c", defaultHost, user, false);
        Folder f3=APILocator.getFolderAPI().createFolders("/"+runId+"/a/b/d", defaultHost, user, false);
        Folder f4=APILocator.getFolderAPI().findFolderByPath("/"+runId+"/a/b", defaultHost, user, false);
        Folder ff=APILocator.getFolderAPI().createFolders("/sass", host, user, false);
        

        File file1=new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + File.separator + runId + File.separator + "_file1.scss"); 
        FileUtils.writeStringToFile(file1, "$file1: 2;");
        Contentlet fileAsset1=newFile(file1, ff, host);
        
        File file2=new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + File.separator + runId + File.separator + "_file2.scss");
        FileUtils.writeStringToFile(file2, "$file2: 4;");
        Contentlet fileAsset2=newFile(file2, f1, defaultHost);
        
        File file3=new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + File.separator + runId + File.separator + "_file3.scss");
        FileUtils.writeStringToFile(file3, "$file3: 8;");
        Contentlet fileAsset3=newFile(file3, f4, defaultHost);
        
        File file4=new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + File.separator + runId + File.separator + "_file4.scss");
        FileUtils.writeStringToFile(file4, "$file4: 16;");
        Contentlet fileAsset4=newFile(file4, f3, defaultHost);
        
        File file5=new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + File.separator + runId + File.separator + "file5.scss");
        FileUtils.writeStringToFile(file5, "@import \"//"+host.getHostname()+"/sass/file1\"; \r\n"+
                                           "@import \"/"+runId+"/a/file2\"; \r\n"+
                                           "@import \"../file3\"; \r\n"+
                                           "@import \"../d/file4\"; \r\n"+
                                           "someclass { width: ($file1 + $file2 + $file3 + $file4); } \r\n");
        Contentlet fileAsset5=newFile(file5,f2,defaultHost);
        
        
        URL cssURL = new URL(baseURL + "/DOTSASS/" + runId + "/a/b/c/file5.css");
        String response =  IOUtils.toString(cssURL.openStream(),"UTF-8");
        Assert.assertEquals("someclass{width:30}", response.substring(0,response.lastIndexOf("}")+1).trim());
        
    }
    
    @Test
    public void includedFilesInIncludedFiles() throws Exception {
        final User user=APILocator.getUserAPI().getSystemUser();
        final String runId=UUIDGenerator.generateUuid();
                
        Host defaultHost=APILocator.getHostAPI().findDefaultHost(user, false);
        
        Folder fa=APILocator.getFolderAPI().createFolders("/"+runId+"/a", defaultHost, user, false);
        Folder fabc=APILocator.getFolderAPI().createFolders("/"+runId+"/a/b/c", defaultHost, user, false);
        Folder fab=APILocator.getFolderAPI().findFolderByPath("/"+runId+"/a/b", defaultHost, user, false);
        
        File file1=new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + File.separator + runId + File.separator + "_fa.scss"); 
        FileUtils.writeStringToFile(file1, ".a { color:green; } ");
        Contentlet fileAsset1=newFile(file1, fa, defaultHost);
        
        File file2=new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + File.separator + runId + File.separator + "_fab.scss");
        FileUtils.writeStringToFile(file2, "@import \"../fa\"; .ab { color:black; }");
        Contentlet fileAsset2=newFile(file2, fab, defaultHost);
        
        File file3=new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + File.separator + runId + File.separator + "fabc.scss");
        FileUtils.writeStringToFile(file3, "@import \"../fab\"; .abc { color:white; }");
        Contentlet fileAsset3=newFile(file3, fabc, defaultHost);
        
        URL cssURL = new URL(baseURL + "/DOTSASS/" + runId + "/a/b/c/fabc.css");
        String response =  IOUtils.toString(cssURL.openStream(),"UTF-8");
        
        Assert.assertEquals(".a{color:green}.ab{color:black}.abc{color:white}", response.substring(0,response.lastIndexOf("}")+1).trim());
    }
    
    protected Contentlet newFile(File file, Folder f, Host host) throws Exception {
        Contentlet fileAsset=new Contentlet();
        fileAsset.setStructureInode(CacheLocator.getContentTypeCache().getStructureByVelocityVarName(FileAssetAPI.DEFAULT_FILE_ASSET_STRUCTURE_VELOCITY_VAR_NAME).getInode());
        fileAsset.setHost(host.getIdentifier());
        fileAsset.setFolder(f.getInode());
        fileAsset.setBinary(FileAssetAPI.BINARY_FIELD, file);
        fileAsset.setStringProperty(FileAssetAPI.TITLE_FIELD, file.getName());
        fileAsset.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, file.getName());
        fileAsset.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        fileAsset=APILocator.getContentletAPI().checkin(fileAsset, APILocator.getUserAPI().getSystemUser(), false);
        APILocator.getContentletAPI().publish(fileAsset, APILocator.getUserAPI().getSystemUser(), false);
        APILocator.getContentletAPI().isInodeIndexed(fileAsset.getInode());
        APILocator.getContentletAPI().isInodeIndexed(fileAsset.getInode(),true);
        return fileAsset;
    }
}
