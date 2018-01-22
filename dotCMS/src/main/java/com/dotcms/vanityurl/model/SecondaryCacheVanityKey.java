package com.dotcms.vanityurl.model;

import com.dotcms.repackage.org.apache.commons.collections.keyvalue.MultiKey;

import java.io.Serializable;

/**
 * Encapsulates the keys for second cache vanities.
 * @author jsanca
 */
public class SecondaryCacheVanityKey extends MultiKey implements Serializable {

    public SecondaryCacheVanityKey(final String hostId, final long languageId) {

        super(hostId, languageId);
    }

    @Override
    public String toString() {

        return this.getKey(0) + "| lang_" + this.getKey(1);
    }
} // E:O:F:CacheVanityKey.
