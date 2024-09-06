package com.dotmarketing.portlets.templates.business;

import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateAsFileDataGen;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.ContentletBaseTest;
import com.dotmarketing.portlets.templates.model.FileAssetTemplate;
import org.junit.Assert;
import org.junit.Test;

public class FileAssetTemplateUtilTest extends ContentletBaseTest {

    /**
     * Method to Test: {@link FileAssetTemplateUtil#getFullPath(FileAssetTemplate)}
     * When: Create a new {@link FileAssetTemplate}
     * should: return the full path
     *
     * @throws Exception
     */
    @Test
    public void testGetFullPathMethod() throws Exception {

        final Host host = new SiteDataGen().nextPersisted();

        final FileAssetTemplate fileAssetTemplate = new TemplateAsFileDataGen()
                .host(host)
                .nextPersisted();

        final FileAssetTemplate template = FileAssetTemplate.class.cast(APILocator.getTemplateAPI()
                .findWorkingTemplate(fileAssetTemplate.getIdentifier(),user,false));

        final String fullPath = FileAssetTemplateUtil.getInstance().getFullPath(template);

        final String expected = "//" + host.getHostname() + template.getPath();

        Assert.assertEquals(expected, fullPath);
    }

    @Test
    public void test_getPathFromFullPath_wrong_host_on_full_path() throws Exception {

        final String relativePath = FileAssetTemplateUtil.getInstance().getPathFromFullPath("demo.dotcms.com","//global.dotcms.com/application/templates/test-template");

        Assert.assertNotNull(relativePath);
        Assert.assertEquals(relativePath, "//global.dotcms.com/application/templates/test-template");
    }

    @Test
    public void test_getPathFromFullPath_right_host_on_full_path() throws Exception {

        final String relativePath = FileAssetTemplateUtil.getInstance().getPathFromFullPath("demo.dotcms.com","//demo.dotcms.com/application/templates/test-template");

        Assert.assertNotNull(relativePath);
        Assert.assertEquals(relativePath, "/application/templates/test-template");
    }

    @Test
    public void test_getPathFromFullPath_any_host_on_relative_path() throws Exception {

        final String relativePath = FileAssetTemplateUtil.getInstance().getPathFromFullPath("demo.dotcms.com","/application/templates/test-template");

        Assert.assertNotNull(relativePath);
        Assert.assertEquals(relativePath, "/application/templates/test-template");
    }

    @Test
    public void test_getHost_null_default_expected() throws Exception {

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Host host        = FileAssetTemplateUtil.getInstance().getHost(null);

        Assert.assertNotNull(host);
        Assert.assertEquals(defaultHost.getHostname(), host.getHostname());
    }

    @Test
    public void test_getHost_blank_default_expected() throws Exception {

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Host host        = FileAssetTemplateUtil.getInstance().getHost("");

        Assert.assertNotNull(host);
        Assert.assertEquals(defaultHost.getHostname(), host.getHostname());
    }

    @Test
    public void test_getHost_ramdom_default_expected() throws Exception {

        final Host defaultHost = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Host host        = FileAssetTemplateUtil.getInstance().getHost("/xxxxxx");

        Assert.assertNotNull(host);
        Assert.assertEquals(defaultHost.getHostname(), host.getHostname());
    }

    @Test
    public void test_getHost_demo_default_demo() throws Exception {

        //Getting the current default host
        final Host currentDefaultHost = APILocator.getHostAPI()
                .findDefaultHost(APILocator.systemUser(), false);

        try {
            //Create test site and make it default
            final Host site = new SiteDataGen().nextPersisted();
            APILocator.getHostAPI().makeDefault(site, APILocator.systemUser(), false);

            final Host host = FileAssetTemplateUtil.getInstance()
                    .getHost("//" + site.getHostname() + "/application/templates/test/");

            Assert.assertNotNull(host);
            Assert.assertEquals(site.getHostname(), host.getHostname());
        } finally {
            APILocator.getHostAPI().makeDefault(currentDefaultHost, APILocator.systemUser(), false);
        }
    }

    /**
     * Method to test: Test the method getThemeIdFromPath, will null
     * Given Scenario: Receives a null theme path
     * ExpectedResult: Expected null
     *
     */
    @Test
    public void test_getThemeIdFromPath_with_null() throws Exception {

        final String theme = null;
        final String themeId = FileAssetTemplateUtil.getInstance().getThemeIdFromPath(theme);

        Assert.assertNull(themeId);
    }

    /**
     * Method to test: Test the method getThemeIdFromPath, will empty string
     * Given Scenario: Receives a empty string theme path
     * ExpectedResult: Expected null
     *
     */
    @Test
    public void test_getThemeIdFromPath_with_empty_string() throws Exception {

        final String theme = "";
        final String themeId = FileAssetTemplateUtil.getInstance().getThemeIdFromPath(theme);

        Assert.assertEquals(theme, themeId);
    }

    /**
     * Method to test: Test the method getThemeIdFromPath, will random string
     * Given Scenario: Receives a random string theme path
     * ExpectedResult: Expected null
     *
     */
    @Test
    public void test_getThemeIdFromPath_with_random_string() throws Exception {

        final String theme = "path/theme/random/";
        final String themeId = FileAssetTemplateUtil.getInstance().getThemeIdFromPath(theme);

        Assert.assertEquals(theme, themeId);
    }

    /**
     * Method to test: Test the method getThemeIdFromPath, will send a full theme path
     * Given Scenario: Creates a theme folder and sends the full path
     * ExpectedResult: Expected the theme id (folder id)
     *
     */
    @Test
    public void test_getThemeIdFromPath_with_full_path_theme() throws Exception {

        // create a folder under application/themes
        final String theme = "/application/themes/dotcms";
        final Host host = APILocator.getHostAPI().findDefaultHost(user, false);
        final Folder themesFolder = APILocator.getFolderAPI().createFolders(theme, host, user, false);
        final String folderId = themesFolder.getInode();
        final String fullPath = "//"+host.getHostname()+theme;
        final String themeId = FileAssetTemplateUtil.getInstance().getThemeIdFromPath(fullPath);

        Assert.assertEquals(folderId, themeId);
    }


    /**
     * Method to test: Test the method getThemeIdFromPath, will send a full theme path that does not exist
     * Given Scenario: Sends a non existing path
     * ExpectedResult: Returns the same path, since can not find it
     *
     */
    @Test
    public void test_getThemeIdFromPath_with_full_path_theme_doesnot_exist() throws Exception {

        // create a folder under application/themes
        final String theme = "//noexists/app/xxxx/yyyy";
        final String themeId = FileAssetTemplateUtil.getInstance().getThemeIdFromPath(theme);

        Assert.assertEquals(theme, themeId);
    }

}
