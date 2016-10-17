package com.dotcms.csspreproc;

import java.io.File;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.LicenseTestUtil;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class CSSPreProcessServletTest {
	
	@Before
    public void prepare() throws Exception {
        LicenseTestUtil.getLicense();
    }
    
    @Test
    public void checkExternalResource() throws Exception {
        User user = APILocator.getUserAPI().getSystemUser();
        Host demo = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
        Folder folder = APILocator.getFolderAPI().createFolders("/"+UUIDGenerator.generateUuid(), demo, user, false);
        
        File file=new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + File.separator + 
                UUIDGenerator.generateUuid() + File.separator + "hello.txt");
        file.getParentFile().mkdirs();
        FileUtils.writeStringToFile(file, "hello there!");
        
        Contentlet asset = new Contentlet();
        asset.setHost(demo.getIdentifier());
        asset.setFolder(folder.getInode());
        asset.setStructureInode(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("FileAsset").getInode());
        asset.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        asset.setStringProperty(FileAssetAPI.TITLE_FIELD, "hello");
        asset.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, "hello.txt");
        asset.setBinary(FileAssetAPI.BINARY_FIELD, file);
        asset = APILocator.getContentletAPI().checkin(asset, user, false);
        APILocator.getContentletAPI().publish(asset, user, false);
        APILocator.getContentletAPI().isInodeIndexed(asset.getInode());
        APILocator.getContentletAPI().isInodeIndexed(asset.getInode(),true);
        
        HttpServletRequest req = ServletTestRunner.localRequest.get();
        String uri = "http://" + req.getServerName() + ":" + req.getServerPort() + 
                "/DOTLESS/" + folder.getName() + "/hello.txt";
        Assert.assertEquals("hello there!", IOUtils.toString(new URL(uri).openStream()));
    }
}
