package com.dotcms.rest.api.v1.temp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.util.PathContainmentUtil;
import com.dotmarketing.business.APILocator;
import com.liferay.util.FileUtil;
import java.io.File;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Checks that {@link TempFileAPI#createEmptyTempFile} keeps every temp file inside the temp upload
 * directory, whatever path the caller puts in the file name, and that it creates nothing elsewhere
 * on the way.
 */
public class TempFileAPITraversalTest {

    private static final String PROBE_DIR = "qa992probe";

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
    }

    @After
    public void removeProbeDirectories() {
        File parent = tmpRoot().getParentFile();
        for (int depth = 0; depth < 3 && parent != null; depth++) {
            final File probe = new File(parent, PROBE_DIR);
            if (probe.exists()) {
                FileUtil.deltree(probe);
            }
            parent = parent.getParentFile();
        }
    }

    /**
     * Given a file name with {@code ../} segments, the temp file still lands inside the temp
     * upload directory and no directory is created outside it.
     */
    @Test
    public void test_createEmptyTempFile_traversalName_staysInsideTmpUpload() throws Exception {
        final DotTempFile tempFile = new TempFileAPI().createEmptyTempFile(
                "../../" + PROBE_DIR + "/x.txt", TempFileAPITest.mockHttpServletRequest());

        assertTrue("temp file must be inside tmp_upload: " + tempFile.file,
                PathContainmentUtil.isContainedIn(tmpRoot(), tempFile.file));
        assertEquals("x.txt", tempFile.file.getName());
        assertNoProbeDirectoryOutsideTmpUpload();
    }

    /**
     * Given a file name with Windows-style {@code ..\} segments, the result is the same.
     */
    @Test
    public void test_createEmptyTempFile_backslashTraversalName_staysInsideTmpUpload() throws Exception {
        final DotTempFile tempFile = new TempFileAPI().createEmptyTempFile(
                "..\\..\\" + PROBE_DIR + "\\x.txt", TempFileAPITest.mockHttpServletRequest());

        assertTrue("temp file must be inside tmp_upload: " + tempFile.file,
                PathContainmentUtil.isContainedIn(tmpRoot(), tempFile.file));
        assertNoProbeDirectoryOutsideTmpUpload();
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

    private static File tmpRoot() {
        return new File(APILocator.getFileAssetAPI().getRealAssetPathTmpBinary());
    }

    private static void assertNoProbeDirectoryOutsideTmpUpload() {
        File parent = tmpRoot().getParentFile();
        for (int depth = 0; depth < 3 && parent != null; depth++) {
            final File probe = new File(parent, PROBE_DIR);
            assertFalse("directory created outside tmp_upload: " + probe, probe.exists());
            parent = parent.getParentFile();
        }
    }
}
