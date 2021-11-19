package com.dotcms.util;

import com.dotcms.util.DbExporterUtil.PG_ARCH;
import org.junit.Test;

import java.io.File;
import java.util.Optional;

public class DbExporterUtilTest {
    @Test
    public void test_finding_executables() throws Exception {
        Optional<String> sh = DbExporterUtil.findExecutable("sh");
        assert(sh.isPresent());
        assert(sh.get().equals("/bin/sh"));
        assert(!DbExporterUtil.findExecutable("thisDoesNotExist").isPresent());
    }
    
    @Test
    public void test_arch_lookup() {
        // testing on OS-X
        assert(PG_ARCH.pgdump().equals(PG_ARCH.pgdump()));
    }
    
    @Test
    public void test_copy_pgdump() throws Exception {
        final File tempFile = File.createTempFile("testing", "temp");
        DbExporterUtil.copyPgDump(tempFile);
        assert(tempFile.exists());
        assert(tempFile.canExecute());
        tempFile.delete();
    }
    
    @Test
    public void test_getting_pg_dumps_path() throws Exception {
        final String pgDumpPath = DbExporterUtil.pgDumpPath.get();
        assert(pgDumpPath!=null);
        assert(new File(pgDumpPath).exists());
        assert(new File(pgDumpPath).canExecute());
    }
}
