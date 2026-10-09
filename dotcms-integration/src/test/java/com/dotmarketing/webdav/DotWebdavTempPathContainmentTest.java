package com.dotmarketing.webdav;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.ConfigUtils;
import com.liferay.util.FileUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * WebDAV temp storage must only read and write strictly inside its own temp directory
 * ({@link DotWebdavHelper#getTempDir()}), a dedicated sub-folder of the asset temp path. Paths with a
 * {@code .} or {@code ..} segment are rejected, and a name that reaches temp storage must be a single
 * plain path segment. Rejected names surface as {@link BadRequestException} (HTTP 400) and leave the
 * filesystem untouched. In-bounds names, including the dot-prefixed transient files WebDAV clients
 * create, keep working.
 */
public class DotWebdavTempPathContainmentTest {

    private static final byte[] CONTENT = "content".getBytes(StandardCharsets.UTF_8);

    private final DotWebdavHelper helper = new DotWebdavHelper();
    private final List<File> scratchFolders = new ArrayList<>();

    /** Starts the integration environment that {@link DotWebdavHelper} depends on. */
    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    /** Removes every scratch folder the test created. */
    @After
    public void cleanUp() {
        scratchFolders.forEach(folder -> FileUtil.deltree(folder, true));
    }

    /** Creates an empty, uniquely named folder inside the WebDAV temp directory for one test. */
    private File newScratchFolder() {
        return newFolder(helper.getTempDir(), "containment-");
    }

    /**
     * Creates an empty, uniquely named folder under {@code parent}, removed after the test.
     *
     * @param parent the folder that receives the new folder
     * @param prefix the start of the new folder's name
     * @return the new folder
     */
    private File newFolder(final File parent, final String prefix) {
        final File folder = new File(parent, prefix + UUID.randomUUID());
        Assert.assertTrue(folder.mkdirs());
        scratchFolders.add(folder);
        return folder;
    }

    /**
     * Tells whether {@code candidate} resolves to a descendant of {@code root} once {@code ..} and
     * symlinks are resolved. {@code root} itself does not count as inside.
     */
    private static boolean isStrictlyInside(final File root, final File candidate) throws IOException {
        return candidate.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator);
    }

    /**
     * Runs {@code action} as if it were handling a WebDAV request authenticated as the system user,
     * which is what the resource classes read the current user from.
     *
     * @param action the code to run
     * @return what {@code action} returns
     */
    private static <T> T asWebdavRequest(final Callable<T> action) throws Exception {
        try (MockedStatic<HttpManager> http = Mockito.mockStatic(HttpManager.class)) {
            final Request request = Mockito.mock(Request.class);
            final Auth auth = Mockito.mock(Auth.class);
            Mockito.when(auth.getTag()).thenReturn(APILocator.getUserAPI().getSystemUser());
            Mockito.when(request.getAuthorization()).thenReturn(auth);
            http.when(HttpManager::request).thenReturn(request);
            return action.call();
        }
    }

    /**
     * Asserts that {@code action}, run inside a WebDAV request, fails with
     * {@link BadRequestException}.
     */
    private static void assertBadRequest(final String message, final Callable<?> action) throws Exception {
        asWebdavRequest(() -> Assert.assertThrows(message, BadRequestException.class, action::call));
    }

    /** Returns the names of every file and folder under {@code root}, at any depth. */
    private static List<String> namesUnder(final File root) throws IOException {
        if (!root.exists()) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(root.toPath())) {
            return paths.filter(path -> !path.equals(root.toPath()))
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        }
    }

    /** WebDAV temp storage lives in its own sub-folder of the asset temp path, not in the path itself. */
    @Test
    public void getTempDir_is_a_dedicated_sub_folder_of_the_asset_temp_path() throws IOException {
        final File assetTemp = new File(ConfigUtils.getAssetTempPath());
        Assert.assertTrue(isStrictlyInside(assetTemp, helper.getTempDir()));
    }

    /** A request path that climbs out of the temp directory with {@code ..} resolves to nothing. */
    @Test
    public void loadTempFile_returns_null_for_a_path_outside_the_temp_dir() {
        Assert.assertNull(helper.loadTempFile("/webdav/working/1/../outside.txt"));
    }

    /** A request path that resolves to the temp directory itself is not a valid temp resource. */
    @Test
    public void loadTempFile_returns_null_for_the_temp_dir_itself() {
        Assert.assertNull(helper.loadTempFile("/webdav/working/1/somehost/.."));
    }

    /**
     * A request path whose {@code ..} segment would land on another folder that still sits inside
     * the temp directory resolves to nothing: the segment alone is enough to reject it.
     */
    @Test
    public void loadTempFile_returns_null_for_a_dot_segment_that_stays_inside_the_temp_dir() {
        final File other = newScratchFolder();
        Assert.assertNull(helper.loadTempFile("/webdav/working/1/somehost/../" + other.getName() + "/.x"));
        Assert.assertNull(helper.loadTempFile("/webdav/working/1/somehost/./.x"));
    }

    /**
     * A folder another feature keeps in the asset temp path cannot be reached through WebDAV, with
     * or without a {@code ..} segment: WebDAV paths resolve under its own sub-folder.
     */
    @Test
    public void loadTempFile_never_resolves_a_folder_of_another_feature() throws IOException {
        final File otherFeature = newFolder(new File(ConfigUtils.getAssetTempPath()), "temp_");
        final File otherFile = new File(otherFeature, ".x");
        Files.write(otherFile.toPath(), CONTENT);

        Assert.assertNull(helper.loadTempFile("/webdav/working/1/somehost/../" + otherFeature.getName() + "/.x"));
        Assert.assertNull(helper.loadTempFile("/webdav/working/1/../../" + otherFeature.getName() + "/.x"));

        final File direct = helper.loadTempFile("/webdav/working/1/" + otherFeature.getName() + "/.x");
        Assert.assertNotNull(direct);
        Assert.assertTrue(isStrictlyInside(helper.getTempDir(), direct));
        Assert.assertNotEquals(otherFile.getCanonicalPath(), direct.getCanonicalPath());
        Assert.assertFalse(direct.exists());
    }

    /**
     * Dot-prefixed transient files that WebDAV clients create while saving still resolve, inside the
     * temp directory.
     */
    @Test
    public void loadTempFile_allows_an_in_bounds_dot_prefixed_temp_file() throws IOException {
        final File resolved = helper.loadTempFile("/webdav/working/1/demo.dotcms.com/.inflight.txt");
        Assert.assertNotNull(resolved);
        Assert.assertTrue(isStrictlyInside(helper.getTempDir(), resolved));
    }

    /**
     * A symlink inside the temp directory that points outside it does not make its target reachable:
     * reads resolve to nothing and writes are refused.
     */
    @Test
    public void a_symlink_pointing_outside_the_temp_dir_is_not_followed() throws IOException {
        final File outside = newFolder(new File(ConfigUtils.getAssetTempPath()), "outside-");
        final File scratch = newScratchFolder();
        Files.createSymbolicLink(new File(scratch, "link").toPath(), outside.toPath());

        Assert.assertNull(helper.loadTempFile("/webdav/working/1/" + scratch.getName() + "/link/.x"));
        Assert.assertThrows(IOException.class,
                () -> helper.createTempFile("/" + scratch.getName() + "/link/.x"));
        Assert.assertThrows(RuntimeException.class,
                () -> helper.createTempFolder("/webdav/working/1/" + scratch.getName() + "/link/.dir"));
        Assert.assertArrayEquals(new String[0], outside.list());
    }

    /** A folder path that climbs out of the temp directory is rejected and no folder is created outside. */
    @Test
    public void createTempFolder_rejects_a_path_outside_the_temp_dir() {
        final File outside = new File(helper.getTempDir().getParentFile(), "outside-dir");
        try {
            Assert.assertThrows(RuntimeException.class,
                    () -> helper.createTempFolder("/webdav/working/1/../outside-dir"));
            Assert.assertFalse(outside.exists());
        } finally {
            if (outside.exists()) {
                FileUtil.deltree(outside, true);
            }
        }
    }

    /** A file path that climbs out of the temp directory is rejected and no file is created outside. */
    @Test
    public void createTempFile_rejects_a_path_outside_the_temp_dir() {
        final File outside = new File(helper.getTempDir().getParentFile(), "outside-file.txt");
        try {
            Assert.assertThrows(IOException.class, () -> helper.createTempFile("/../outside-file.txt"));
            Assert.assertFalse(outside.exists());
        } finally {
            if (outside.exists()) {
                Assert.assertTrue(outside.delete());
            }
        }
    }

    /**
     * Uploading into a temp folder with a name that is not a single path segment fails with HTTP 400
     * and writes nothing.
     */
    @Test
    public void createNew_rejects_a_name_that_is_not_a_plain_segment() {
        final File scratch = newScratchFolder();
        final File folder = new File(scratch, "folder");
        Assert.assertTrue(folder.mkdirs());
        final TempFolderResourceImpl resource = new TempFolderResourceImpl(folder.getPath(), folder, false);

        for (final String name : new String[]{"..", ".", "a\\b"}) {
            Assert.assertThrows("name: " + name, BadRequestException.class,
                    () -> resource.createNew(name, new ByteArrayInputStream(CONTENT),
                            (long) CONTENT.length, "text/plain"));
        }
        Assert.assertArrayEquals(new String[0], folder.list());
    }

    /** Uploading into a temp folder with a plain name still writes the content into that folder. */
    @Test
    public void createNew_writes_a_plain_name_inside_the_folder() throws Exception {
        final File scratch = newScratchFolder();
        final TempFolderResourceImpl resource = new TempFolderResourceImpl(scratch.getPath(), scratch, false);

        resource.createNew("plain.txt", new ByteArrayInputStream(CONTENT), (long) CONTENT.length, "text/plain");

        final File written = new File(scratch, "plain.txt");
        Assert.assertTrue(written.isFile());
        Assert.assertArrayEquals(CONTENT, Files.readAllBytes(written.toPath()));
    }

    /** Creating a temp sub-folder named {@code ..} or {@code .} fails with HTTP 400 and creates nothing. */
    @Test
    public void createCollection_rejects_a_name_that_is_not_a_plain_segment() {
        final File scratch = newScratchFolder();
        final File folder = new File(scratch, "folder");
        Assert.assertTrue(folder.mkdirs());
        final TempFolderResourceImpl resource = new TempFolderResourceImpl(folder.getPath(), folder, false);

        for (final String name : new String[]{"..", "."}) {
            Assert.assertThrows("name: " + name, BadRequestException.class,
                    () -> resource.createCollection(name));
        }
        Assert.assertArrayEquals(new String[0], folder.list());
        Assert.assertArrayEquals(new String[]{"folder"}, scratch.list());
    }

    /** Copying a temp file to a {@code ..} destination name fails with HTTP 400 and writes nothing. */
    @Test
    public void copyTo_rejects_a_name_that_is_not_a_plain_segment() throws Exception {
        assertCopyOrMoveRejected(true);
    }

    /**
     * Moving a temp file to a {@code ..} destination name fails with HTTP 400 and leaves the source
     * in place.
     */
    @Test
    public void moveTo_rejects_a_name_that_is_not_a_plain_segment() throws Exception {
        assertCopyOrMoveRejected(false);
    }

    /**
     * Runs a copy or a move of a temp file to the destination name {@code ..} and checks that it is
     * rejected with {@link BadRequestException}, the source file is untouched, and the destination
     * folder stays empty.
     *
     * @param copy {@code true} to copy, {@code false} to move
     */
    private void assertCopyOrMoveRejected(final boolean copy) throws Exception {
        final File scratch = newScratchFolder();
        final File sourceFolder = new File(scratch, "source");
        final File targetFolder = new File(new File(scratch, "target"), "inner");
        Assert.assertTrue(sourceFolder.mkdirs());
        Assert.assertTrue(targetFolder.mkdirs());
        final File sourceFile = new File(sourceFolder, "file.txt");
        Files.write(sourceFile.toPath(), CONTENT);

        final TempFileResourceImpl source = new TempFileResourceImpl(sourceFile, sourceFile.getPath(), false);
        final TempFolderResourceImpl target = new TempFolderResourceImpl(targetFolder.getPath(), targetFolder, false);

        assertBadRequest("copy: " + copy, () -> {
            if (copy) {
                source.copyTo(target, "..");
            } else {
                source.moveTo(target, "..");
            }
            return null;
        });

        Assert.assertTrue(sourceFile.isFile());
        Assert.assertTrue(targetFolder.isDirectory());
        Assert.assertArrayEquals(new String[0], targetFolder.list());
    }

    /**
     * Creates a site for one test and registers its WebDAV temp folder for removal afterwards.
     *
     * @return the new site
     */
    private Host newSite() {
        final Host site = new SiteDataGen().nextPersisted();
        scratchFolders.add(new File(helper.getTempDir(), site.getHostname()));
        return site;
    }

    /**
     * In a site folder, a temp-style folder name that is not a single segment fails with HTTP 400,
     * and a valid one creates the folder in temp storage and returns that folder.
     */
    @Test
    public void folderResource_createCollection_validates_the_name_and_returns_the_created_folder()
            throws Exception {
        final Host site = newSite();
        final Folder folder = new FolderDataGen().site(site).nextPersisted();
        final FolderResourceImpl resource = new FolderResourceImpl(folder,
                "/webdav/working/1/" + site.getHostname() + folder.getPath());
        final File siteTemp = new File(helper.getTempDir(), site.getHostname());

        for (final String name : new String[]{"..", "."}) {
            assertBadRequest("name: " + name, () -> resource.createCollection(name));
        }
        Assert.assertEquals(List.of(), namesUnder(siteTemp));

        final CollectionResource created = asWebdavRequest(() -> resource.createCollection(".tmpdir"));
        assertIsCreatedTempFolder(created, ".tmpdir", siteTemp);
    }

    /**
     * At a site root, a temp-style folder name that is not a single segment fails with HTTP 400, and
     * a valid one creates the folder in temp storage and returns that folder.
     */
    @Test
    public void hostResource_createCollection_validates_the_name_and_returns_the_created_folder()
            throws Exception {
        final Host site = newSite();
        final HostResourceImpl resource = new HostResourceImpl("/webdav/working/1/" + site.getHostname());
        final File siteTemp = new File(helper.getTempDir(), site.getHostname());

        for (final String name : new String[]{"..", "."}) {
            assertBadRequest("name: " + name, () -> resource.createCollection(name));
        }
        Assert.assertEquals(List.of(), namesUnder(siteTemp));

        final CollectionResource created = asWebdavRequest(() -> resource.createCollection(".tmpdir"));
        assertIsCreatedTempFolder(created, ".tmpdir", siteTemp);
    }

    /**
     * At the language root, a temp-style folder name that is not a single segment fails with HTTP
     * 400, and a valid one creates the folder in temp storage and returns that folder.
     */
    @Test
    public void languageFolderResource_createCollection_validates_the_name_and_returns_the_created_folder()
            throws Exception {
        final LanguageFolderResourceImpl resource = new LanguageFolderResourceImpl("");
        final File languagesTemp = new File(new File(helper.getTempDir(), "system"), "languages");
        final String name = ".tmpdir-" + UUID.randomUUID();
        scratchFolders.add(new File(languagesTemp, name));
        final List<String> before = namesUnder(languagesTemp);

        for (final String invalid : new String[]{"..", "."}) {
            Assert.assertThrows("name: " + invalid, BadRequestException.class,
                    () -> resource.createCollection(invalid));
        }
        Assert.assertEquals(before, namesUnder(languagesTemp));

        assertIsCreatedTempFolder(resource.createCollection(name), name, languagesTemp);
    }

    /**
     * Checks that {@code created} wraps the folder named {@code name} that now exists inside
     * {@code parent}, not the parent folder or a path outside temp storage.
     */
    private void assertIsCreatedTempFolder(final CollectionResource created, final String name,
            final File parent) throws IOException {
        Assert.assertTrue(created instanceof TempFolderResourceImpl);
        final File folder = ((TempFolderResourceImpl) created).getFolder();
        Assert.assertEquals(name, folder.getName());
        Assert.assertTrue(folder.isDirectory());
        Assert.assertTrue(isStrictlyInside(parent, folder));
        Assert.assertTrue(isStrictlyInside(helper.getTempDir(), folder));
    }

    /**
     * Uploading a temp-style file at a site root with a name that is not a single segment fails with
     * HTTP 400 and writes nothing; a valid name is written inside temp storage.
     */
    @Test
    public void basicFolderResource_createNew_validates_the_name_and_writes_inside_temp_storage()
            throws Exception {
        final Host site = newSite();
        final HostResourceImpl resource = new HostResourceImpl("/webdav/working/1/" + site.getHostname());
        final File siteTemp = new File(helper.getTempDir(), site.getHostname());

        for (final String name : new String[]{"..", "."}) {
            assertBadRequest("name: " + name, () -> resource.createNew(name,
                    new ByteArrayInputStream(CONTENT), (long) CONTENT.length, "text/plain"));
        }
        Assert.assertEquals(List.of(), namesUnder(siteTemp));

        final Resource created = asWebdavRequest(() -> resource.createNew(".inflight.txt",
                new ByteArrayInputStream(CONTENT), (long) CONTENT.length, "text/plain"));
        Assert.assertTrue(created instanceof TempFileResourceImpl);
        final File written = ((TempFileResourceImpl) created).getFile();
        Assert.assertTrue(written.isFile());
        Assert.assertTrue(isStrictlyInside(siteTemp, written));
        Assert.assertArrayEquals(CONTENT, Files.readAllBytes(written.toPath()));
    }
}
