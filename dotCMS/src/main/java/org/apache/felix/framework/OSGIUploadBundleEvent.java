package org.apache.felix.framework;

import java.io.File;
import java.time.Instant;

/**
 * This event is fired when a new bundle is being upload to the OSGI upload folder
 * @author jsanca
 */
public class OSGIUploadBundleEvent {

    private final Instant instant;
    private final File    uploadFolderFile;

    public OSGIUploadBundleEvent(final Instant instant, final File uploadFolderFile) {
        this.instant = instant;
        this.uploadFolderFile = uploadFolderFile;
    }

    public Instant getInstant() {
        return instant;
    }

    public File getUploadFolderFile() {
        return uploadFolderFile;
    }
}
