package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.publishing.manifest.ManifestBuilder;
import com.dotcms.publishing.manifest.ManifestItem;
import java.io.File;
import java.io.IOException;

public class TestManifestBuilder implements ManifestBuilder {

    @Override
    public <T> void include(ManifestItem manifestItem, String evaluateReason) {

    }

    @Override
    public <T> void exclude(ManifestItem manifestItem, String evaluateReason, String excludeReason) {

    }

    @Override
    public File getManifestFile() {
        return null;
    }

    @Override
    public void addMetadata(String name, String value) {

    }

    @Override
    public void close() throws IOException {

    }
}
