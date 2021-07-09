package com.dotcms.enterprise.publishing.remote.bundler;

import com.dotcms.publishing.manifest.ManifestBuilder;
import com.dotcms.publishing.manifest.ManifestItem;
import java.io.File;
import java.io.IOException;

public class TestManifestBuilder implements ManifestBuilder {

    @Override
    public void create() {

    }

    @Override
    public <T> void include(ManifestItem manifestItem, String reason) {

    }

    @Override
    public <T> void exclude(ManifestItem manifestItem, String reason) {

    }

    @Override
    public File getManifestFile() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
