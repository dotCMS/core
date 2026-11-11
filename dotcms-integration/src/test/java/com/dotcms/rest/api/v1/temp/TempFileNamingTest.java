package com.dotcms.rest.api.v1.temp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.PathContainmentUtil;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.liferay.util.FileUtil;
import java.io.File;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Checks how {@link TempFileAPI#createEmptyTempFile} names and places temp files: only the file
 * name part of what the caller sends is used, every file lands in its own folder under the temp
 * upload directory, and nothing is created elsewhere on the way. The location guard is also
 * exercised on its own, since the name handling before it never produces a path it would refuse.
 */
public class TempFileNamingTest {

    private static final String MARKER_DIR = "outside-marker";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @After
    public void removeMarkerDirectories() {
        File parent = tmpRoot().getParentFile();
        for (int depth = 0; depth < 3 && parent != null; depth++) {
            final File marker = new File(parent, MARKER_DIR);
            if (marker.exists()) {
                FileUtil.deltree(marker);
            }
            parent = parent.getParentFile();
        }
    }

    /**
     * Given a name that carries {@code ../} directory segments, only the file name is kept, the
     * temp file lands inside the temp upload directory and no directory is created outside it.
     */
    @Test
    public void test_createEmptyTempFile_nameWithParentSegments_keepsLeafInsideTempDir() throws Exception {
        final DotTempFile tempFile = new TempFileAPI().createEmptyTempFile(
                "../../" + MARKER_DIR + "/x.txt", TempFileAPITest.mockHttpServletRequest());

        assertTrue("temp file must be inside the temp upload directory: " + tempFile.file,
                PathContainmentUtil.isContainedIn(tmpRoot(), tempFile.file));
        assertEquals("x.txt", tempFile.file.getName());
        assertNoMarkerDirectoryOutsideTempDir();
    }

    /**
     * Given a name with Windows-style {@code ..\} segments, the result is the same.
     */
    @Test
    public void test_createEmptyTempFile_nameWithBackslashSegments_staysInsideTempDir() throws Exception {
        final DotTempFile tempFile = new TempFileAPI().createEmptyTempFile(
                "..\\..\\" + MARKER_DIR + "\\x.txt", TempFileAPITest.mockHttpServletRequest());

        assertTrue("temp file must be inside the temp upload directory: " + tempFile.file,
                PathContainmentUtil.isContainedIn(tmpRoot(), tempFile.file));
        assertNoMarkerDirectoryOutsideTempDir();
    }

    /**
     * Given a plain file name, the temp file keeps that name under its own temp folder.
     */
    @Test
    public void test_createEmptyTempFile_plainName_keepsName() throws Exception {
        final DotTempFile tempFile = new TempFileAPI().createEmptyTempFile(
                "report.txt", TempFileAPITest.mockHttpServletRequest());

        assertEquals("report.txt", tempFile.file.getName());
        assertEquals(tempFile.id, tempFile.file.getParentFile().getName());
        assertTrue(PathContainmentUtil.isContainedIn(tmpRoot(), tempFile.file));
    }

    /**
     * The location guard, taken on its own: a candidate that resolves outside the temp upload
     * directory is refused, and the refusal creates nothing on disk.
     */
    @Test
    public void test_requireInsideTempUploadDir_outsideCandidate_isRefused() {
        final File outside = new File(tmpRoot(), "../" + MARKER_DIR + "/x.txt");
        try {
            TempFileAPI.requireInsideTempUploadDir(tmpRoot(), outside);
            fail("a candidate outside the temp upload directory must be refused");
        } catch (final DotRuntimeException expected) {
            assertNoMarkerDirectoryOutsideTempDir();
        }
    }

    /**
     * The location guard accepts a candidate directly below the temp upload directory, even when
     * neither the file nor its folder exists yet.
     */
    @Test
    public void test_requireInsideTempUploadDir_insideCandidate_isAccepted() {
        TempFileAPI.requireInsideTempUploadDir(tmpRoot(), new File(tmpRoot(), "temp_x/report.txt"));
    }

    private static File tmpRoot() {
        return new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary());
    }

    private static void assertNoMarkerDirectoryOutsideTempDir() {
        File parent = tmpRoot().getParentFile();
        for (int depth = 0; depth < 3 && parent != null; depth++) {
            final File marker = new File(parent, MARKER_DIR);
            assertFalse("directory created outside the temp upload directory: " + marker, marker.exists());
            parent = parent.getParentFile();
        }
    }
}
