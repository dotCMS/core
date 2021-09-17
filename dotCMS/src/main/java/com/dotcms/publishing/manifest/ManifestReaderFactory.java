package com.dotcms.publishing.manifest;

import com.dotcms.publishing.output.TarGzipBundleOutput;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.ConfigUtils;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;

public enum ManifestReaderFactory {
    INSTANCE;

    public CSVManifestReader createCSVManifestReader(final String bundleID){
        final File bundleTarGzipFile = TarGzipBundleOutput.getBundleTarGzipFile(bundleID);
        try {
            final Optional<Reader> manifestInputStream = ManifestUtil
                    .getManifestInputStream(bundleTarGzipFile);

            if (!manifestInputStream.isPresent()){
                throw new IllegalArgumentException("Manifest not found for: " + bundleID);
            }

            return new CSVManifestReader(manifestInputStream.get());
        } catch (IOException e) {
            throw new DotRuntimeException(e);
        }

    }
}
