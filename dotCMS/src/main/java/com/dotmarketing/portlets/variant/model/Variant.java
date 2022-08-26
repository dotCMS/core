package com.dotmarketing.portlets.variant.model;

import java.io.Serializable;

/**
 * Represent a new set of versions to a {@link com.dotmarketing.portlets.contentlet.model.Contentlet}.
 *
 * We cann think a <code>Variant</code> as a {@link com.dotmarketing.portlets.contentlet.model.Contentlet}
 * branches so into each Variant we can have several {@link com.dotmarketing.portlets.contentlet.model.Contentlet}'s
 * versions with different languages and in WORKING or LIVE mode.
 *
 * Then we can have something like:
 *
 * **Default variant**
 * - English Contentlet version
 * - Spanish Contentlet version
 *
 * **Redesign 2022 variant**
 * - English Contentlet version
 * - Spanish Contentlet version
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
