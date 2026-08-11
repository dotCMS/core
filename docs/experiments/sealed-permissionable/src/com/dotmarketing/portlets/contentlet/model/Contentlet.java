package com.dotmarketing.portlets.contentlet.model;

import com.dotcms.keyvalue.business.KeyValue404;
import com.dotcms.keyvalue.model.DefaultKeyValue;
import com.dotcms.vanityurl.model.DefaultVanityUrl;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.portlets.calendar.model.Event;
import com.dotmarketing.portlets.categories.business.Categorizable;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.personas.model.Persona;

/**
 * Not an Inode. This is the single fact that breaks the intuitive picture of the hierarchy: pages,
 * file assets, personas, hosts and events are all Contentlets, and Contentlet answers to
 * Permissionable directly.
 *
 * <p>Eight direct subclasses, in six packages.</p>
 */
public sealed class Contentlet implements Permissionable, Categorizable, Treeable, Ruleable permits

        Host, HTMLPageAsset, FileAsset, Persona, Event, DefaultKeyValue, KeyValue404,
        DefaultVanityUrl {

    /**
     * The variant that is data rather than type. A Site normally reaches the resolver as a plain
     * Contentlet whose content type is <em>named</em> Host — a row in the database, not a Java class.
     * Nothing about sealing this hierarchy makes that variant checkable.
     */
    public String contentTypeVariable() {
        return null;
    }
}
