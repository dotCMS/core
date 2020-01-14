package com.dotmarketing.webdav;

import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.repackage.com.bradmcevoy.http.FileResource;
import com.dotcms.repackage.com.bradmcevoy.http.FolderResource;
import com.dotcms.repackage.com.bradmcevoy.http.Resource;
import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.util.FileUtil;
import java.io.IOException;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

public class DotWebdavHelperTest {

    private final DotWebdavHelper helper = new DotWebdavHelper();

    @Test
    public void Test_Get_Folder_Resource_Then_Get_File_Resource() throws IOException, DotDataException, DotSecurityException {
        final SiteDataGen siteDataGen = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host host = siteDataGen.nextPersisted();
        final Folder parent = folderDataGen.site(host).nextPersisted();
        final Folder child = folderDataGen.parent(parent).nextPersisted();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");
        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(child, file);
        fileAssetDataGen.nextPersisted();
        final String folderPath = String.format("http://localhost:8080/webdav/live/1/%s/%s/",host.getName(),parent.getName());
        final Resource folderResource = helper.getResourceFromURL(folderPath);
        Assert.assertNotNull(folderResource);
        Assert.assertTrue(folderResource instanceof FolderResource);
        final String fileResourcePath = String.format("http://localhost:8080/webdav/live/1/%s/%s/%s/%s",host.getName(),parent.getName(),child.getName(),file.getName());
        final Resource fileResource = helper.getResourceFromURL(fileResourcePath);
        Assert.assertNotNull(fileResource);
        Assert.assertTrue(fileResource instanceof FileResource);
    }


    @Test
    public void Test_Get_Folder_Resource_Then_Get_File_Resource_Shuffled_Casing() throws IOException, DotDataException, DotSecurityException {
        final SiteDataGen siteDataGen = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host host = siteDataGen.nextPersisted();
        final Folder parent = folderDataGen.site(host).nextPersisted();
        final Folder child = folderDataGen.parent(parent).nextPersisted();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");
        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(child, file);
        fileAssetDataGen.nextPersisted();
        final String folderPath = upperCaseRandom(String.format("http://localhost:8080/webdav/live/1/%s/%s/",host.getName(),parent.getName()), 8);

        final Resource folderResource = helper.getResourceFromURL(folderPath);
        Assert.assertNotNull(folderResource);
        Assert.assertTrue(folderResource instanceof FolderResource);
        final String fileResourcePath = upperCaseRandom(String.format("http://localhost:8080/webdav/live/1/%s/%s/%s/%s",host.getName(),parent.getName(),child.getName(),file.getName()),8);
        final Resource fileResource = helper.getResourceFromURL(fileResourcePath);
        Assert.assertNotNull(fileResource);
        Assert.assertTrue(fileResource instanceof FileResource);
    }

    @Test
    public void Test_Get_Folder_Resource_For_Non_Existing_Path() throws IOException, DotDataException, DotSecurityException {
        String path = "http://localhost:8080/webdav/live/1/demo.dotcms.com/images/black.png";
        final Resource folderResource = helper.getResourceFromURL(path);
        Assert.assertNull(folderResource);
    }

    @Test
    public void Test_Same_Resource_For_Paths_With_Different_Casing() throws IOException, DotDataException, DotSecurityException {
        final SiteDataGen siteDataGen = new SiteDataGen();
        final FolderDataGen folderDataGen = new FolderDataGen();
        final Host host = siteDataGen.nextPersisted();
        final Folder parent = folderDataGen.site(host).nextPersisted();
        final Folder child = folderDataGen.parent(parent).nextPersisted();
        java.io.File file = java.io.File.createTempFile("texto", ".txt");
        FileUtil.write(file, "helloworld");
        FileAssetDataGen fileAssetDataGen = new FileAssetDataGen(child, file);
        fileAssetDataGen.nextPersisted();
        final String path = String.format("http://localhost:8080/webdav/live/1/%s/%s/%s/%s",host.getName(),parent.getName(),child.getName(),file.getName());
        final String fileResourcePath1 = upperCaseRandom(path,8);
        final String fileResourcePath2 = upperCaseRandom(path,10);
        Assert.assertNotEquals(fileResourcePath1,fileResourcePath2);
        Assert.assertTrue(helper.isSameResourceURL(fileResourcePath1,fileResourcePath2, file.getName()));
    }

    private static String upperCaseRandom(final String input, final int n) {
        final int length = input.length();
        final StringBuilder output = new StringBuilder(input);
        final boolean[] alreadyChecked = new boolean[length];
        final Random random = new Random();

        for (int i = 0, checks = 0; i < n && checks < length; i++) {
            // Pick a place
            final int position = random.nextInt(length);

            // Check if lowercase alpha
            if (!alreadyChecked[position]) {
                if (Character.isLowerCase(output.charAt(position))) {
                    output.setCharAt(position, Character.toUpperCase(output.charAt(position)));
                } else {
                    i--;
                }
                checks++;
                alreadyChecked[position] = true;
            } else {
                i--;
            }
        }
        return output.toString();
    }

}
