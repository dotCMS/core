package com.dotcms.rendering.js;

import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents the key value Form Data in a post request, in a Js context
 * @author jsanca
 *
 */
public class JsFormData  implements Serializable {

    private final Map<String, String> entries = new HashMap<>();

    public JsFormData(final Map<String, String> entries) {
        this.entries.putAll(entries);
    }

    @HostAccess.Export
    public void append(final String name, final String value) {
        this.entries.put(name, value);
    }

    @HostAccess.Export
    public void set(final String name, final String value) {
        this.entries.put(name, value);
    }

    @HostAccess.Export
    public void delete(final String name) {
        this.entries.remove(name);
    }

    @HostAccess.Export
    public String get(final String name) {
        return this.entries.get(name);
    }

    @HostAccess.Export
    public boolean has(final String name) {
        return this.entries.containsKey(name);
    }

    @HostAccess.Export
    public Set<String> getKeys(final String name) {
        return this.entries.keySet();
    }

    @HostAccess.Export
    public Set<Map.Entry<String, String>> getEntries() {
        return this.entries.entrySet();
    }

    @HostAccess.Export
    public Collection<String> getValues() {
        return this.entries.values();
    }
}
