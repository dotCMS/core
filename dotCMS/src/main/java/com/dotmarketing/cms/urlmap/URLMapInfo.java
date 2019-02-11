package com.dotmarketing.cms.urlmap;

import com.dotmarketing.beans.Identifier;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

public class URLMapInfo {
    final Contentlet contentlet;
    final Identifier identifier;

    URLMapInfo(final Contentlet contentlet, final Identifier identifier) {
        this.contentlet = contentlet;
        this.identifier = identifier;
    }

    public Contentlet getContentlet() {
        return contentlet;
    }

    public String getDetailtPageUri() {
        return identifier.getURI();
    }

    public Identifier getIdentifier() {
        return identifier;
    }
}
