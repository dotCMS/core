package com.dotmarketing.portlets.contentlet.transform;

import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.IconType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class DotFolderTransformerTest {

    static Host site;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        final SiteDataGen siteDataGen = new SiteDataGen();
        site = siteDataGen.nextPersisted();
    }

    /**
     * Given scenario: This is a simple test that expects Folder
     * Expected results:  A simple view must be returned.
     */
    @Test
    public void Test_GraphQL_View(){

        final FolderDataGen folderDataGen = new FolderDataGen().site(site);
        final Folder folder = folderDataGen.nextPersisted();

        final DotMapViewTransformer transformer = new DotFolderTransformerBuilder().withFolders(folder).build();
        final List<Map<String, Object>> maps = transformer.toMaps();
        Assert.assertEquals("Expecting 1 folder transformed",1, maps.size());
        final Map<String, Object> map = maps.get(0);

        final Map<String,Object> folderMap = (Map<String,Object>)map.get("folderMap");
        Assert.assertEquals(folderMap.get("folderId"), folder.getIdentifier());
        Assert.assertEquals(folderMap.get("folderFileMask"), folder.getFilesMasks());
        Assert.assertEquals(folderMap.get("folderSortOrder"), folder.getSortOrder());
        Assert.assertEquals(folderMap.get("folderName"), folder.getName());
        Assert.assertEquals(folderMap.get("folderPath"), folder.getPath());
        Assert.assertEquals(folderMap.get("folderTitle"), folder.getTitle());
        Assert.assertEquals(folderMap.get("folderDefaultFileType"), folder.getDefaultFileType());
     }

    /**
     * Given scenario: This is a simple test that expects a user and set of given roles
     * Expected results: If the User has read permissions over a folder then you'll get a view.
     */
    @Test
    public void Test_SiteBrowse_View() throws Exception{

        final FolderDataGen folderDataGen = new FolderDataGen().site(site);
        final Folder folder = folderDataGen.nextPersisted();

        final User admin = TestUserUtils.getAdminUser();
        final Role adminRole = APILocator.getRoleAPI().loadCMSAdminRole();

        final DotMapViewTransformer transformer = new DotFolderTransformerBuilder().withFolders(folder).withUserAndRoles(admin, adminRole).build();
        final List<Map<String, Object>> maps = transformer.toMaps();
        Assert.assertEquals("Expecting 1 folder transformed",1, maps.size());
        final Map<String, Object> folderMap = maps.get(0);

        Assert.assertEquals(folderMap.get("parent"), folder.getInode());
        Assert.assertEquals(folderMap.get("name"), folder.getName());
        Assert.assertEquals(folderMap.get("title"), folder.getName());
        Assert.assertEquals(folderMap.get("description"), folder.getTitle());
        Assert.assertEquals(folderMap.get("extension"), "folder");
        Assert.assertEquals(folderMap.get("hasTitleImage"), StringPool.BLANK);
        Assert.assertEquals(folderMap.get("__icon__"), IconType.FOLDER.iconName());

    }

}
