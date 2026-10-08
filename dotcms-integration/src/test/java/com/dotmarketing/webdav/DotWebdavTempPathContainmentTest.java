package com.dotmarketing.webdav;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.liferay.util.FileUtil;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * WebDAV temp storage must only read and write strictly inside the temp directory
 * ({@link DotWebdavHelper#getTempDir()}), and a name that reaches temp storage must be a single plain
 * path segment. Rejected names surface as {@link BadRequestException} (HTTP 400) and leave the
 * filesystem untouched. In-bounds names, including the dot-prefixed transient files WebDAV clients
 * create, keep working.
 */
public class DotWebdavTempPathContainmentTest {

    private static final byte[] CONTENT = "content".getBytes(StandardCharsets.UTF_8);

    private final DotWebdavHelper helper = new DotWebdavHelper();
    private final List<File> scratchFolders = new ArrayList<>();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @After
    public void cleanUp() {
        scratchFolders.forEach(folder -> FileUtil.deltree(folder, true));
    }

    /** Creates an empty, uniquely named folder inside the temp directory for one test. */
    private File newScratchFolder() {
        final File folder = new File(helper.getTempDir(), "containment-" + UUID.randomUUID());
        Assert.assertTrue(folder.mkdirs());
        scratchFolders.add(folder);
        return folder;
    }

    private static boolean isStrictlyInside(final File root, final File candidate) throws IOException {
        return candidate.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator);
    }

    @Test
    public void loadTempFile_returns_null_for_a_path_outside_the_temp_dir() {
        Assert.assertNull(helper.loadTempFile("/webdav/working/1/../outside.txt"));
    }

    @Test
    public void loadTempFile_returns_null_for_the_temp_dir_itself() {
        Assert.assertNull(helper.loadTempFile("/webdav/working/1/somehost/.."));
    }

    @Test
    public void loadTempFile_allows_an_in_bounds_dot_prefixed_temp_file() throws IOException {
        final File resolved = helper.loadTempFile("/webdav/working/1/demo.dotcms.com/.inflight.txt");
        Assert.assertNotNull(resolved);
        Assert.assertTrue(isStrictlyInside(helper.getTempDir(), resolved));
    }

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

    @Test
    public void createNew_writes_a_plain_name_inside_the_folder() throws Exception {
        final File scratch = newScratchFolder();
        final TempFolderResourceImpl resource = new TempFolderResourceImpl(scratch.getPath(), scratch, false);

        resource.createNew("plain.txt", new ByteArrayInputStream(CONTENT), (long) CONTENT.length, "text/plain");

        final File written = new File(scratch, "plain.txt");
        Assert.assertTrue(written.isFile());
        Assert.assertArrayEquals(CONTENT, Files.readAllBytes(written.toPath()));
    }

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
    }

    @Test
    public void copyTo_rejects_a_name_that_is_not_a_plain_segment() throws Exception {
        assertCopyOrMoveRejected(true);
    }

    @Test
    public void moveTo_rejects_a_name_that_is_not_a_plain_segment() throws Exception {
        assertCopyOrMoveRejected(false);
    }

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

        try (MockedStatic<HttpManager> http = Mockito.mockStatic(HttpManager.class)) {
            final Request request = Mockito.mock(Request.class);
            final Auth auth = Mockito.mock(Auth.class);
            Mockito.when(auth.getTag()).thenReturn(APILocator.getUserAPI().getSystemUser());
            Mockito.when(request.getAuthorization()).thenReturn(auth);
            http.when(HttpManager::request).thenReturn(request);

            Assert.assertThrows(BadRequestException.class, () -> {
                if (copy) {
                    source.copyTo(target, "..");
                } else {
                    source.moveTo(target, "..");
                }
            });
        }

        Assert.assertTrue(sourceFile.isFile());
        Assert.assertTrue(targetFolder.isDirectory());
        Assert.assertArrayEquals(new String[0], targetFolder.list());
    }
}
