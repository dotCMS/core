package com.dotmarketing.webdav;

import static com.dotcms.unittest.TestUtil.upperCaseRandom;
import static org.mockito.Mockito.when;

import com.bradmcevoy.http.FileResource;
import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.util.FileUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class DotWebdavHelperTest {

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    private final DotWebdavHelper helper = new DotWebdavHelper();

    @Test
    public void completedTemporaryUploadsAreDiscoverableAndSurviveInterruptedReplacement() throws Exception {
        org.junit.Assume.assumeTrue(com.dotcms.storage.AssetStorageFeature.isEnabled());
        final Host host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Folder folder = new FolderDataGen().site(host).name("Mixed-Dav-" + java.util.UUID.randomUUID()).nextPersisted();
        final String base = "/webdav/working/1/" + host.getHostname() + "/" + folder.getName() + "/";
        final var temporaryDirectory = helper.loadTempFile(base).toPath();
        try (var requests = Mockito.mockStatic(HttpManager.class)) {
            final var request = Mockito.mock(Request.class);
            final var auth = Mockito.mock(com.bradmcevoy.http.Auth.class);
            when(auth.getTag()).thenReturn(APILocator.systemUser());
            when(request.getAuthorization()).thenReturn(auth);
            requests.when(HttpManager::request).thenReturn(request);
            final var normalFolder = (FolderResourceImpl) helper.getResourceFromURL("http://localhost:8080" + base);
            Assert.assertNotNull(normalFolder);
            final var complete = (TempFileResourceImpl) normalFolder.createNew(".Mixed-Name.TXT",
                    new ByteArrayInputStream("complete".getBytes()), 8L, "text/plain");
            Assert.assertEquals(".Mixed-Name.TXT", complete.getName());
            java.nio.file.Files.delete(complete.getFile().toPath());
            java.nio.file.Files.delete(helper.loadTempFile(base + ".Mixed-Name.TXT").toPath());
            Assert.assertEquals(Long.valueOf(8), complete.getContentLength());
            Assert.assertEquals("complete", java.nio.file.Files.readString(complete.getFile().toPath()));
            Assert.assertNotNull(helper.getResourceFromURL("http://localhost:8080" + base + ".Mixed-Name.TXT"));
            Assert.assertNotNull(helper.getResourceFromURL("http://localhost:8080" + base.toLowerCase(java.util.Locale.ROOT) + ".Mixed-Name.TXT"));
            Assert.assertTrue(helper.getChildrenOfFolder(folder, APILocator.systemUser(), false, 1).stream()
                    .anyMatch(resource -> resource.getName().equals(".Mixed-Name.TXT")));
            Assert.assertNotNull(normalFolder.child(".Mixed-Name.TXT"));
            Assert.assertNull(normalFolder.child(".mixed-name.txt"));
            final var routedDirectory = (TempFolderResourceImpl) normalFolder.createCollection("(Mixed-Staging)");
            Assert.assertEquals("(Mixed-Staging)", routedDirectory.getFolder().getName());
            final var routedChild = (TempFileResourceImpl) routedDirectory.createNew("Nested-Mixed.TXT",
                    new ByteArrayInputStream("routed".getBytes()), 6L, "text/plain");
            java.nio.file.Files.delete(routedChild.getFile().toPath());
            FileUtil.deltree(routedDirectory.getFolder(), true);
            final var resolvedChild = (TempFileResourceImpl) helper.getResourceFromURL(
                    "http://localhost:8080" + base.toLowerCase(java.util.Locale.ROOT) + "(Mixed-Staging)/Nested-Mixed.TXT");
            Assert.assertNotNull(resolvedChild);
            Assert.assertEquals("routed", java.nio.file.Files.readString(resolvedChild.getFile().toPath()));
            Assert.assertNull(helper.getResourceFromURL("http://localhost:8080" + base + "(Mixed-Staging)/nested-mixed.txt"));
            Assert.assertTrue(helper.getChildrenOfFolder(folder, APILocator.systemUser(), false, 1).stream()
                    .anyMatch(resource -> resource.getName().equals("(Mixed-Staging)")));
            routedDirectory.delete();
            Assert.assertThrows(com.dotmarketing.exception.DotRuntimeException.class, () ->
                    normalFolder.createNew(".Mixed-Name.TXT", failingUpload(), 10L, "text/plain"));
            Assert.assertEquals("complete", java.nio.file.Files.readString(complete.getFile().toPath()));
            Assert.assertThrows(com.dotmarketing.exception.DotRuntimeException.class, () ->
                    normalFolder.createNew(".Absent.TXT", failingUpload(), 10L, "text/plain"));
            Assert.assertFalse(helper.loadTempFile(base + ".Absent.TXT").exists());

            final var staging = (TempFolderResourceImpl) normalFolder.createCollection("(Staging)");
            final var nested = (TempFileResourceImpl) staging.createNew("Nested-Mixed.TXT",
                    new ByteArrayInputStream("nested".getBytes()), 6L, "text/plain");
            Assert.assertThrows(IOException.class, () ->
                    staging.createNew("Nested-Mixed.TXT", failingUpload(), 10L, "text/plain"));
            final var output = new java.io.ByteArrayOutputStream();
            nested.sendContent(output, null, java.util.Map.of(), null);
            Assert.assertEquals("nested", output.toString(java.nio.charset.StandardCharsets.UTF_8));
            java.nio.file.Files.delete(nested.getFile().toPath());
            FileUtil.deltree(temporaryDirectory.toFile(), true);
            Assert.assertNotNull(staging.child("Nested-Mixed.TXT"));
            Assert.assertNull(staging.child("nested-mixed.txt"));
            final var empty = (TempFolderResourceImpl) staging.createCollection("Empty");
            Assert.assertTrue(empty.getChildren().isEmpty());
            nested.copyTo(empty, "Copied.TXT");
            final var copied = (TempFileResourceImpl) empty.child("Copied.TXT");
            Assert.assertEquals("nested", java.nio.file.Files.readString(copied.getFile().toPath()));
            copied.moveTo(empty, "Moved.TXT");
            Assert.assertNull(empty.child("Copied.TXT"));
            final var moved = (TempFileResourceImpl) empty.child("Moved.TXT");
            moved.moveTo(empty, "Moved.TXT");
            Assert.assertNotNull(empty.child("Moved.TXT"));
            empty.copyTo(staging, "Folder-Copy");
            Assert.assertNotNull(((TempFolderResourceImpl) staging.child("Folder-Copy")).child("Moved.TXT"));
            empty.moveTo(staging, "Folder-Moved");
            Assert.assertNull(staging.child("Empty"));
            final var folderMoved = (TempFolderResourceImpl) staging.child("Folder-Moved");
            folderMoved.moveTo(staging, "Folder-Moved");
            Assert.assertNotNull(staging.child("Folder-Moved"));
            folderMoved.delete();
            Assert.assertNull(staging.child("Folder-Moved"));
            staging.delete();
            complete.delete();
            Assert.assertNull(helper.getResourceFromURL("http://localhost:8080" + base + ".Mixed-Name.TXT"));
        } finally {
            com.dotcms.storage.WebdavTemporaryStorage.getInstance().delete(temporaryDirectory.toFile());
            FileUtil.deltree(temporaryDirectory.toFile(), true);
            FolderDataGen.remove(folder);
        }
    }

    private static InputStream failingUpload() {
        return new InputStream() {
            private boolean first = true;
            @Override public int read() throws IOException {
                if (first) { first = false; return 'x'; }
                throw new IOException("interrupted WebDAV upload");
            }
        };
    }

    @Test
    public void siteAndLanguageRootsDiscoverRemoteTemporaryChildren() throws Exception {
        org.junit.Assume.assumeTrue(com.dotcms.storage.AssetStorageFeature.isEnabled());
        final Host host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final String name = "(Mixed-Staging-" + java.util.UUID.randomUUID() + ")";
        final String sitePath = "/webdav/working/1/" + host.getHostname();
        final String languagePath = "/webdav/live/1/system/languages";
        try (var requests = Mockito.mockStatic(HttpManager.class)) {
            final var request = Mockito.mock(Request.class);
            final var auth = Mockito.mock(com.bradmcevoy.http.Auth.class);
            when(auth.getTag()).thenReturn(APILocator.systemUser());
            when(request.getAuthorization()).thenReturn(auth);
            requests.when(HttpManager::request).thenReturn(request);
            final var site = new HostResourceImpl(sitePath);
            final var languages = new LanguageFolderResourceImpl("");
            final var siteTemp = (TempFolderResourceImpl) site.createCollection(name);
            final var languageTemp = (TempFolderResourceImpl) languages.createCollection(name);
            try {
                for (var temporary : java.util.List.of(siteTemp, languageTemp)) {
                    final var child = (TempFileResourceImpl) temporary.createNew("Mixed.TXT",
                            new ByteArrayInputStream("remote bytes".getBytes()), 12L, "text/plain");
                    java.nio.file.Files.delete(child.getFile().toPath());
                    FileUtil.deltree(temporary.getFolder(), true);
                }
                Assert.assertTrue(site.getChildren().stream().anyMatch(resource -> resource.getName().equals(name)));
                Assert.assertNotNull(site.child(name));
                Assert.assertNull(site.child(name.toLowerCase(java.util.Locale.ROOT)));
                Assert.assertTrue(languages.getChildren().stream().anyMatch(resource -> resource.getName().equals(name)));
                Assert.assertNotNull(languages.child(name));
                Assert.assertNull(languages.child(name.toLowerCase(java.util.Locale.ROOT)));
                for (String parent : java.util.List.of(sitePath, languagePath)) {
                    final var child = (TempFileResourceImpl) helper.getResourceFromURL(
                            "http://localhost:8080" + parent + "/" + name + "/Mixed.TXT");
                    Assert.assertNotNull(child);
                    Assert.assertEquals("remote bytes", java.nio.file.Files.readString(child.getFile().toPath()));
                }
                siteTemp.delete();
                languageTemp.delete();
                Assert.assertNull(site.child(name));
                Assert.assertNull(languages.child(name));
            } finally {
                siteTemp.delete();
                languageTemp.delete();
            }
        }
    }

    @Test
    public void uploadColdReadCopyAndOverwriteRetainHistoricalBytes() throws Exception {
        final boolean enabled = com.dotcms.storage.AssetStorageFeature.isEnabled();
        Assert.assertEquals(Boolean.getBoolean("s3.cms.enabled"), enabled);
        final Host host = APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final var user = APILocator.systemUser();
        final var binaries = APILocator.getBinaryAssetStorageAPI();
        final String base = "/webdav/working/1/" + host.getHostname() + "/" + folder.getName() + "/";
        final String source = base + "Mixed-Case.TXT";
        final byte[] original = "original WebDAV asset".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (var requests = Mockito.mockStatic(HttpManager.class)) {
            final var request = Mockito.mock(Request.class);
            when(request.getUserAgentHeader()).thenReturn("WebDAV S3 acceptance");
            requests.when(HttpManager::request).thenReturn(request);
            try (var upload = new ByteArrayInputStream(original)) {
                helper.setResourceContent(source, upload, "text/plain", null, new java.util.Date(), user, false);
            }
            final var first = helper.loadFile(source, user);
            Assert.assertNotNull(first);
            final var firstContent = (com.dotmarketing.portlets.contentlet.model.Contentlet) first;
            final String firstInode = firstContent.getInode();
            final var firstFile = firstContent.getBinary(com.dotmarketing.portlets.fileassets.business.FileAssetAPI.BINARY_FIELD);
            Assert.assertEquals("Mixed-Case.TXT", firstFile.getName());
            if (enabled) Assert.assertTrue(binaries.evictLocalFile(firstFile));
            final var output = new java.io.ByteArrayOutputStream();
            new FileResourceImpl(first, source).sendContent(output, null, java.util.Map.of(), null);
            Assert.assertArrayEquals(original, output.toByteArray());

            if (enabled) Assert.assertTrue(binaries.evictLocalFile(firstFile));
            helper.copyResource(source, base + "Copy-Mixed.TXT", user, enabled);
            final var copy = (com.dotmarketing.portlets.contentlet.model.Contentlet) helper.loadFile(base + "Copy-Mixed.TXT", user);
            Assert.assertNotNull(copy);
            if (enabled) Assert.assertTrue(copy.isLive());
            final var copyFile = copy.getBinary(com.dotmarketing.portlets.fileassets.business.FileAssetAPI.BINARY_FIELD);
            if (enabled) Assert.assertTrue(binaries.evictLocalFile(copyFile));
            try (var input = copy.getBinaryStream(com.dotmarketing.portlets.fileassets.business.FileAssetAPI.BINARY_FIELD)) {
                Assert.assertArrayEquals(original, input.readAllBytes());
            }

            if (enabled) Assert.assertTrue(binaries.evictLocalFile(firstContent.getBinary(com.dotmarketing.portlets.fileassets.business.FileAssetAPI.BINARY_FIELD)));
            try (var upload = new ByteArrayInputStream("replacement".getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                helper.setResourceContent(source, upload, "text/plain", null, new java.util.Date(), user, false);
            }
            final var historical = APILocator.getContentletAPI().find(firstInode, user, false);
            Assert.assertNotNull(historical);
            try (var input = historical.getBinaryStream(com.dotmarketing.portlets.fileassets.business.FileAssetAPI.BINARY_FIELD)) {
                Assert.assertArrayEquals(original, input.readAllBytes());
            }
            if (enabled) {
                try (var upload = new ByteArrayInputStream(original)) {
                    org.junit.Assert.assertThrows(IOException.class, () -> helper.setResourceContent(
                            "/webdav/working/1/" + host.getHostname() + "/missing-" + java.util.UUID.randomUUID() + "/file.txt",
                            upload, "text/plain", null, new java.util.Date(), user, false));
                }
            }
        } finally {
            FolderDataGen.remove(folder);
        }
    }

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

    /**
     * Method to test: DotWebdavHelper.setResourceContent
     * <p>
     * Given Scenario: A file is created with no content, then the same file is updated also with no
     * content.
     * <p>
     * ExpectedResult: The file is created and updated without exceptions.
     *
     * @throws Exception if an error occurs while updating the file with no content.
     */
    @Test
    public void Test_publishing_existing_file_with_empty_file_should_not_fail() throws Exception {

        final SiteDataGen siteDataGen = new SiteDataGen();
        final Host host = siteDataGen.nextPersisted();
        final var user = APILocator.getUserAPI().getSystemUser();

        final var resourceUri = String.format("/webdav/live/1/%s/test-file.vtl", host.getName());

        try (MockedStatic<HttpManager> mocked = Mockito.mockStatic(HttpManager.class)) {

            // Create a mocked request object
            Request mockedRequest = Mockito.mock(Request.class);
            when(mockedRequest.getUserAgentHeader()).thenReturn("Cyberduck");
            mocked.when(HttpManager::request).thenReturn(mockedRequest);

            try (InputStream emptyFileInputStream =
                    new ByteArrayInputStream(new byte[0])) {// Empty file

                // First publish, the contentlet should be created
                helper.setResourceContent(resourceUri, emptyFileInputStream, "",
                        null, java.util.Calendar.getInstance().getTime(), user, true);

                // Second publish, the contentlet should be updated without exceptions
                helper.setResourceContent(resourceUri, emptyFileInputStream, "",
                        null, java.util.Calendar.getInstance().getTime(), user, true);

                // Third publish, the contentlet should be updated without exceptions
                helper.setResourceContent(resourceUri, emptyFileInputStream, "",
                        null, java.util.Calendar.getInstance().getTime(), user, true);
            }
        }
    }

}
