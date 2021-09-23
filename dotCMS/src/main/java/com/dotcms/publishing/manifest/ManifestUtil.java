package com.dotcms.publishing.manifest;

import com.dotcms.publishing.BundlerUtil;
import com.dotcms.publishing.output.TarGzipBundleOutput;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.Logger;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class ManifestUtil {

    /**
     * Return manifest file's {@link Reader}
     *
     * @param bundleTarGzipFile
     * @return
     * @throws IOException
     */
    public static Optional<Reader> getManifestInputStream(File bundleTarGzipFile)
            throws IOException {

        try (final FileInputStream fileInputStream = new FileInputStream(bundleTarGzipFile);
                BufferedInputStream in = new BufferedInputStream(fileInputStream);
                GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
                TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzIn)) {

            TarArchiveEntry entry;

            while ((entry = (TarArchiveEntry) tarInputStream.getNextEntry())  != null) {
                if (entry.isFile() && entry.getName().equals(ManifestBuilder.MANIFEST_NAME)) {
                    final File tempManifestFile = FileUtil.createTemporaryFile("Manifest_");
                    IOUtils.copy(tarInputStream, new FileOutputStream(tempManifestFile));
                    return Optional.of(new FileReader(tempManifestFile));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Return true if the manifest file exists into the bundle, otherwise return false
     * Alfo return falso if any {@link IOException} is thrown
     *
     * @param bundleId
     * @return
     */
    public static boolean manifestExists(final String bundleId) {
        try {
            if (BundlerUtil.tarGzipExists(bundleId)) {
                final File bundleTarGzipFile = TarGzipBundleOutput.getBundleTarGzipFile(bundleId);

                try (final FileInputStream fileInputStream = new FileInputStream(bundleTarGzipFile);
                        BufferedInputStream in = new BufferedInputStream(fileInputStream);
                        GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
                        TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzIn)) {

                    TarArchiveEntry entry;

                    while ((entry = (TarArchiveEntry) tarInputStream.getNextEntry()) != null) {
                        if (entry.isFile() && entry.getName()
                                .equals(ManifestBuilder.MANIFEST_NAME)) {
                            return true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            Logger.warn(ManifestUtil.class, e.getMessage());
        }

        return false;
    }
}
