package com.dotcms.util;

import com.dotcms.util.DbExporterUtil.PG_ARCH;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Optional;

/**
 * Tests for {@link DbExporterUtil} class.
 *
 * @author vic
 */
public class DbExporterUtilTest {
    /**
     * Tests executable finding.
     *
     * @throws Exception
     */
    @Test
    public void test_finding_executables() throws Exception {
        Optional<String> sh = DbExporterUtil.findExecutable("sh");
        assert(sh.isPresent());
        assert(sh.get().equals("/bin/sh"));
        assert(!DbExporterUtil.findExecutable("thisDoesNotExist").isPresent());
    }

    /**
     * Tests pg_dump distribution lookup.
     */
    @Test
    public void test_arch_lookup() {
        // testing on OS-X
        assert(PG_ARCH.pgDump().equals(PG_ARCH.pgDump()));
    }

    /**
     * Tests copying of files.
     *
     * @throws Exception
     */
    @Test
    public void test_copy_pgdump() throws Exception {
        final File tempFile = File.createTempFile("testing", "temp");
        DbExporterUtil.copyPgDump(tempFile);
        assert(tempFile.exists());
        assert(tempFile.canExecute());
        tempFile.delete();
    }

    /**
     * Tests locating of pg_dump binary.
     */
    @Test
    public void test_getting_pg_dumps_path() {
        final String pgDumpPath = DbExporterUtil.pgDumpPath.get();
        Assert.assertNotNull(pgDumpPath);
        assert(new File(pgDumpPath).exists());
        assert(new File(pgDumpPath).canExecute());
    }
}
