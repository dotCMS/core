package com.dotcms.publishing.manifest;

import com.dotcms.publishing.manifest.ManifestItem.ManifestInfo;
import com.dotmarketing.exception.DotRuntimeException;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public interface ManifestBuilder extends Closeable {
     void create() throws IOException;
     <T> void include(final ManifestItem manifestItem, final String reason);
     <T> void exclude(final ManifestItem manifestItem, final String reason);
    File getManifestFile();
}
