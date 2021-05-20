package com.dotmarketing.business;

import static com.dotcms.datagen.TestDataUtils.getMultipleImageBinariesContent;

import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.util.FileUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.UUIDGenerator;

public class IdentifierAPITest {
    
    protected static final String id404 = "$$__404__CACHE_MISS__$$";
    private static IdentifierAPI api;
    private static IdentifierCache cache;
    
	@BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        
        api = APILocator.getIdentifierAPI();
        cache = CacheLocator.getIdentifierCache();
	}
    
    @Test
    public void testing404() throws Exception {
        
        final Host syshost = APILocator.getHostAPI().findSystemHost();
        
        // fake not yet created id and asset
        String fakeId=UUIDGenerator.generateUuid();
        Contentlet fakeCont=new Contentlet();
        fakeCont.setInode(UUIDGenerator.generateUuid());
        fakeCont.setStructureInode(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Host").getInode());
        
        // if we find a fake id it should end up in 404 cache
        api.find(fakeId);
        Assert.assertEquals(id404, cache.getIdentifier(fakeId).getAssetType());
        api.find(syshost, "/content."+fakeCont.getInode());
        Assert.assertEquals(id404, cache.getIdentifier(syshost.getIdentifier(),"/content."+fakeCont.getInode()).getAssetType());
        
        // now if we create an asset with that ID it should be cleared
        api.createNew(fakeCont, syshost, fakeId);
        Assert.assertNull(cache.getIdentifier(fakeId));
        Assert.assertNull(cache.getIdentifier(syshost.getIdentifier(), "/content."+fakeCont.getInode()));
        
        // this should load the identifier in both cache entries (by url and by id)
        api.find(fakeId);
        Assert.assertEquals(fakeId, cache.getIdentifier(fakeId).getId());
        Assert.assertEquals(fakeId, cache.getIdentifier(syshost.getIdentifier(), "/content."+fakeCont.getInode()).getId());
        
        
    }

    @Test
    public void test_Resolve_Name_Method() throws Exception {
        final IdentifierAPIImpl imp = new IdentifierAPIImpl();

        final int english = 1;

        final Folder folder = APILocator.getFolderAPI().findSystemFolder();
        java.io.File file = java.io.File.createTempFile("file", ".txt");
        FileUtil.write(file, "helloworld");

        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(folder,file);
        Contentlet fileAsset = fileAssetDataGen.languageId(english).nextPersisted();
        final String resolvedAssetName = imp.resolveAssetName(fileAsset);
        System.out.println(resolvedAssetName);

        final String resolvedFolder = imp.resolveAssetName(folder);
        System.out.println(resolvedFolder);
        final Contentlet multipleBinariesContent = getMultipleImageBinariesContent(true, english, null);
        final String multipleBinAssetName = imp.resolveAssetName(multipleBinariesContent);
        System.out.println(multipleBinAssetName);

// new template
        Template template = new TemplateDataGen().nextPersisted();
        final String templateAssetName = imp.resolveAssetName(template);
        // new test folder
        Folder testFolder = new FolderDataGen().nextPersisted();
        final String folderAssetName = imp.resolveAssetName(template);

        //new html page
        final HTMLPageAsset pageAsset = new HTMLPageDataGen(testFolder, template)
                .languageId(english).nextPersisted();

        final String folderAssetResolvedName = imp.resolveAssetName(template);
        System.out.println(pageAsset);

    }
}
