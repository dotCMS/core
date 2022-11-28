package com.dotcms.publishing.manifest;

import static org.junit.Assert.*;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.ConfigUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class ManifestUtilTest {

    @Test
    public void TestConfigFilePath(){
        final String bundlePathStr = ConfigUtils.getBundlePath();
        final File bundlePath = Path.of(bundlePathStr).toFile();
        Assert.assertTrue(bundlePathStr.startsWith(APILocator.getFileAssetAPI().getRealAssetsRootPath()));
        Assert.assertTrue(bundlePath.exists());
    }

    /**
     * Basically here we test that we can only read Manifest from under the bundle path
     * This is the positive path
     * @throws IOException
     */
    @Test
    public void getManifestInputStreamExpectSuccess() throws IOException {
        final long time = java.lang.System.nanoTime();
        final File gzip = Path.of(ConfigUtils.getBundlePath(), "any"+time+"").toFile();
        final File file =  Path.of(System.getProperty("java.io.tmpdir"), ManifestBuilder.MANIFEST_NAME).toFile()   ;
        if(!file.exists()) {
            FileUtils.writeStringToFile(file, " ::: " + System.currentTimeMillis(),
                    Charset.defaultCharset());
        }
        compressGzipFile(file, gzip);
        final Optional<Reader> reader = ManifestUtil.getManifestInputStream(gzip);
        Assert.assertTrue(reader.isPresent());
    }

    /**
     * Basically here we test that we can only read Manifest from under the bundle path
     * This is the negative scenario
     * @throws IOException
     */
    @Test
    public void getManifestInputStreamExpectFail() throws IOException {
        final long time = java.lang.System.nanoTime();
        final File gzip = Path.of(System.getProperty("java.io.tmpdir"), "any"+time+"").toFile();
        final File file =  Path.of(System.getProperty("java.io.tmpdir"), ManifestBuilder.MANIFEST_NAME).toFile()   ;
        if(!file.exists()) {
            FileUtils.writeStringToFile(file, " ::: " + System.currentTimeMillis(),
                    Charset.defaultCharset());
        }
        compressGzipFile(file, gzip);
        final Optional<Reader> reader = ManifestUtil.getManifestInputStream(gzip);
        Assert.assertTrue(reader.isEmpty());
    }

    private static void compressGzipFile(final File src, final File gzipFile) throws IOException {
          try(
            FileOutputStream fos = new FileOutputStream(gzipFile);
            TarArchiveOutputStream taos = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(fos))) ;
          ) {
              taos.putArchiveEntry(new TarArchiveEntry(src,  src.getName()));
              try(BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(src.toPath()))) {
                  IOUtils.copy(bis, taos);
                  taos.closeArchiveEntry();
              }
          }
    }


}