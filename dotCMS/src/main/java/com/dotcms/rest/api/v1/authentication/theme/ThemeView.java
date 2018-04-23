package com.dotcms.rest.api.v1.authentication.theme;

import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;

/**
 * It is a Theme view representation
 */
public class ThemeView {
    private final Folder folder;
    private final Host host;

    public ThemeView(final Folder folder, final Host host) {
        this.folder = folder;
        this.host = host;
    }

    public Folder getFolder() {
        return folder;
    }

    public Host getHost() {
        return host;
    }
}
