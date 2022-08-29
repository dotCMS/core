package com.dotmarketing.portlets.variant.model;

import java.io.Serializable;

/**
 * Variants represent branches or workspaces for {@link com.dotmarketing.portlets.contentlet.model.Contentlet}.
 *
 * In the future it should include also: {@link com.dotmarketing.portlets.templates.model.Template}
 * and {@link com.dotmarketing.portlets.containers.model.Container}.
 *
 * They are a new dimension in addition to languages which means you can have a content version in
 * a language and in a variation.
 */
public class Variant {
    private final String identifier;
    private final String name;
    private final boolean archived;

    public Variant(final String identifier, final String name, final boolean deleted) {
        this.identifier = identifier;
        this.name = name;
        this.archived = deleted;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public boolean isArchived() {
        return archived;
    }

    @Override
    public String toString() {
        return "Variant{" +
                "identifier='" + identifier + '\'' +
                ", name='" + name + '\'' +
                ", archived=" + archived +
                '}';
    }
}
