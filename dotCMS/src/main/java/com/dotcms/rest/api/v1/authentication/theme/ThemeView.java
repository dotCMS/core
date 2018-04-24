package com.dotcms.rest.api.v1.authentication.theme;

import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.dotmarketing.beans.Host;

/**
 * It is a Theme view representation
 */
public class ThemeView {
    private final String name;
    private final Host host;

    public ThemeView(final String name, final Host host) {
        this.name = name;
        this.host = host;
    }

    public String getName() {
        return name;
    }

    public Host getHost() {
        return host;
    }
}
