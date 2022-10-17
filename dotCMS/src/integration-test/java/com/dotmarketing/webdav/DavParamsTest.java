package com.dotmarketing.webdav;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.util.FileUtil;

public class DavParamsTest {


    static Host host;
    static Folder child, parent;
    static Contentlet fileAsset;



    static long languageId;

    static long otherLanguageId;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {



        IntegrationTestInitService.getInstance().init();


        languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();

        Language lang= APILocator.getLanguageAPI().getLanguage("ab", "cd");
        
        
        
        otherLanguageId = (lang != null) ? lang.getId() :  new LanguageDataGen().countryCode("cd").languageCode("ab").languageName("ab").country("cd").nextPersisted().getId();
        

        host = new SiteDataGen().name("host-" + UUIDGenerator.shorty()).nextPersisted();
        parent = new FolderDataGen().name("parent-" + UUIDGenerator.shorty()).site(host).nextPersisted();
        child = new FolderDataGen().name("child-" + UUIDGenerator.shorty()).site(host).parent(parent).nextPersisted();

        java.io.File file = java.io.File.createTempFile("file-texto", ".txt");
        FileUtil.write(file, "helloworld");
        fileAsset = new FileAssetDataGen(child, file).nextPersisted();



    }

    @Test
    public void testHostPath() {

        final String path = "/webdav/live/" + languageId + "/" + host.getName();


        DavParams params = new DavParams(path);

        Assert.assertTrue("dav host is set", params.host.equals(host));
        Assert.assertTrue("host path is not a folder", !params.isFolder());
        Assert.assertTrue("this is a host", params.isHost());

    }
    
    @Test
    public void testSystemHost() {

        final String path = "/webdav/live/" + languageId + "/system";


        DavParams params = new DavParams(path);

        Assert.assertTrue("system langauges", !params.isLanguages());
        Assert.assertTrue("system", params.isSystem());

    }
    
    @Test
    public void testLanguagePath() {


        final String path = "/webdav/live/" + languageId + "/system/langauges";

        DavParams params = new DavParams(path);

        Assert.assertTrue("system langauges", params.isLanguages());
        Assert.assertTrue("not system", !params.isSystem());

    }
    
    

    @Test
    public void testLiveAndWorking() {

        final String livePath = "/webdav/live/" + languageId + "/" + host.getName() + "/" + parent.getName();
        final String workingPath = "/webdav/working/" + languageId + "/" + host.getName() + "/" + parent.getName();

        DavParams params = new DavParams(livePath);
        Assert.assertTrue("parent path is a folder", params.isFolder());
        Assert.assertTrue("path is live", params.autoPub);
        
        
        params = new DavParams(workingPath);
        Assert.assertTrue("parent path is a folder", params.isFolder());
        Assert.assertTrue("path is live", !params.autoPub);

    }
    
    @Test
    public void testRootPath() {

        final String livePath = "/webdav/live/" + languageId + "/" + host.getName() + "/" + parent.getName();
        final String workingPath = "/webdav/working/" + languageId + "/" + host.getName() + "/" + parent.getName();

        DavParams params = new DavParams(livePath);
        Assert.assertTrue("parent path is a folder", params.isFolder());
        Assert.assertTrue("path is live", params.autoPub);
        
        
        params = new DavParams(workingPath);
        Assert.assertTrue("parent path is a folder", params.isFolder());
        Assert.assertTrue("path is live", !params.autoPub);

    }
    
    
    @Test
    public void testOtherLanguage() {


        final String otherLangPath = "/webdav/live/" + otherLanguageId + "/" + host.getName() + "/" + parent.getName();
        final String defaultLangPath = "/webdav/working/" + languageId + "/" + host.getName() + "/" + parent.getName();

        DavParams params = new DavParams(otherLangPath);
        Assert.assertTrue("language is correct", params.languageId == otherLanguageId);

        params = new DavParams(defaultLangPath);
        Assert.assertTrue("language is correct", params.languageId == languageId);
        
    }
    
    
    @Test
    public void testParentPath() {
        final String path = "/webdav/live/" + languageId + "/" + host.getName() + "/" + parent.getName();

        DavParams params = new DavParams(path);
        Assert.assertTrue("parent path is a folder", params.isFolder());



    }

    @Test
    public void testChildPath() {

        final String path = "/webdav/live/" + languageId + "/" + host.getName() + "/" + parent.getName() + "/" + child.getName();

        DavParams params = new DavParams(path);
        Assert.assertTrue("parent path is a folder", params.isFolder());
    }

    @Test
    public void testFilePath() {

        final String path = "/webdav/liv" + "/" + languageId + "/" + host.getName() + "/" + parent.getName() + "/"
                        + child.getName() + "/" + fileAsset.getStringProperty("fileName");

        DavParams params = new DavParams(path);
        Assert.assertTrue("path is a file", params.isFile());
    }

    @Test
    public void testUnknownPath() {

        final String path = "/webdav/liv" + "/" + languageId + "/" + host.getName() + "/" + parent.getName() + "/"
                        + UUIDGenerator.generateUuid();

        DavParams params = new DavParams(path);
        Assert.assertTrue("path is Nothing", !params.isFile());
        Assert.assertTrue("path is Nothing", !params.isFolder());
    }



    @AfterClass
    public static void afterClass() throws Exception {

        host = null;
        child = null;
        parent = null;
        fileAsset = null;
    }

}
